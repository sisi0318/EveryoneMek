package dev.everyonemek.natures;

import com.mojang.authlib.GameProfile;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.items.ModItems;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mekanism.api.Action;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.util.MekanismUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Automates feeding; Nature's Aura remains the sole source of birth spirits and their Aura cost. */
public final class IndustrialBreederLogic {
    public static final int SPIRIT_THRESHOLD = 1_200_000;
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("c839763a-b163-4a79-8a25-c4d37daaa571"), "[NatureBreeder]");
    private final AuraMachine machine;
    private boolean breedOnly;
    private int progress, duration = 1, nearby;
    private CompoundTag signature;

    public IndustrialBreederLogic(AuraMachine machine) { this.machine = machine; }
    public boolean breedOnly() { return breedOnly; }
    public String modeKey() { return "gui.naturesmekanism.breeder_mode." + (breedOnly ? "breed" : "spirit"); }
    public double progress() { return progress / (double) duration; }
    public int nearby() { return nearby; }
    public void setBreedOnly(boolean value) {
        if (breedOnly != value) { breedOnly = value; reset(); machine.markForSave(); }
    }
    private void reset() { progress = 0; signature = null; }
    private record Pair(Animal first, Animal second, int[] consume, List<ItemStack> remainders, CompoundTag signature) { }

    private boolean eligible(Animal animal) {
        return animal.isAlive() && animal.getAge() == 0 && !animal.isInLove() && animal.canFallInLove()
              && (breedOnly || !(animal instanceof Turtle) && !(animal instanceof Frog));
    }

    private static boolean compatible(Animal first, Animal second) {
        // canMate also tests love state. Probe only its timer, without hearts or changing player attribution.
        int a = first.getInLoveTime(), b = second.getInLoveTime();
        first.setInLoveTime(600); second.setInLoveTime(600);
        try { return first.canMate(second) && second.canMate(first); }
        finally { first.setInLoveTime(a); second.setInLoveTime(b); }
    }

    private Pair findPair() {
        var parents = machine.getLevel().getEntitiesOfClass(Animal.class, machine.area().bounds(), this::eligible)
              .stream().sorted(Comparator.comparing(Animal::getUUID)).toList();
        int[] available = machine.inputs.stream().mapToInt(s -> s.getCount()).toArray();
        for (int a = 0; a < parents.size(); a++) for (int b = a + 1; b < parents.size(); b++) {
            Animal first = parents.get(a), second = parents.get(b);
            if (first.distanceToSqr(second) >= 9 || !first.hasLineOfSight(second) || !compatible(first, second)) continue;
            boolean[][] accepts = new boolean[2][available.length];
            for (int s = 0; s < available.length; s++) {
                accepts[0][s] = first.isFood(machine.inputs.get(s).getStack());
                accepts[1][s] = second.isFood(machine.inputs.get(s).getStack());
            }
            int[] consume = IngredientAssignment.matchQuantities(accepts, available, new int[]{1, 1});
            if (consume == null) continue;
            if (!breedOnly && IAuraChunk.getAuraInArea(machine.getLevel(), first.blockPosition(), 30) < SPIRIT_THRESHOLD) continue;
            CompoundTag tag = new CompoundTag();
            tag.putUUID("first", first.getUUID()); tag.putUUID("second", second.getUUID());
            tag.putIntArray("consume", consume); tag.put("area", machine.area().save());
            tag.putBoolean("breed_only", breedOnly); tag.putInt("ticks", MachineConfig.BREEDER_TICKS.get());
            ListTag items = new ListTag();
            for (var slot : machine.inputs) items.add(slot.isEmpty() ? new CompoundTag()
                  : slot.getStack().copyWithCount(1).save(machine.getLevel().registryAccess()));
            tag.put("inputs", items);
            var remainders = new ArrayList<ItemStack>();
            for (int s = 0; s < consume.length; s++) for (int count = 0; count < consume[s]; count++) {
                var food = machine.inputs.get(s).getStack();
                // Axolotl#usePlayerItem returns water, not an empty bucket. Also preserve modded food containers.
                var remainder = food.is(Items.TROPICAL_FISH_BUCKET) ? new ItemStack(Items.WATER_BUCKET) : food.getCraftingRemainingItem();
                if (!remainder.isEmpty()) remainders.add(remainder);
            }
            return new Pair(first, second, consume, remainders, tag);
        }
        return null;
    }

    public void tick() {
        nearby = machine.area().count(null);
        if (collectSpirit()) return;
        if (nearby >= machine.area().cap()) { machine.setStatus(14); return; }
        Pair pair = findPair();
        if (pair == null) { reset(); machine.setStatus(breedOnly ? 17 : 18); return; }
        int environment = IAuraChunk.getAuraInArea(machine.getLevel(), pair.first().blockPosition(), 30);
        // Reserve the native maximum without manufacturing a guaranteed drop.
        var reserved = new ArrayList<>(pair.remainders());
        if (environment >= SPIRIT_THRESHOLD && !(pair.first() instanceof Turtle) && !(pair.first() instanceof Frog))
            reserved.add(new ItemStack(ModItems.BIRTH_SPIRIT, 3));
        if (!machine.canFit(reserved, false)) { machine.setStatus(3); return; }
        if (!pair.signature().equals(signature)) {
            signature = pair.signature(); progress = 0;
            duration = Math.max(1, MekanismUtils.getTicks(machine, MachineConfig.BREEDER_TICKS.get()));
        }
        if (!machine.payEnergy()) return;
        machine.setActive(true); machine.setStatus(0);
        if (++progress >= duration) {
            // Food precedes the native attempt, just as player feeding does. Cancellation keeps vanilla cooldowns.
            for (int i = 0; i < pair.consume().length; i++) machine.inputs.get(i).shrinkStack(pair.consume()[i], Action.EXECUTE);
            machine.insertOutputs(pair.remainders());
            breed(pair.first(), pair.second());
            reset();
        }
        machine.markForSave();
    }

    private void breed(Animal first, Animal second) {
        ServerLevel level = (ServerLevel) machine.getLevel();
        var player = FakePlayerFactory.get(level, PROFILE);
        // Animal#getLoveCause resolves through ServerLevel#players. A cached FakePlayer is not registered
        // there. Expose it only for this synchronous native call, without entity registration or login.
        boolean added = !level.players().contains(player);
        if (added) level.players().add(player);
        try {
            first.setInLove(player); second.setInLove(player);
            if (first instanceof Turtle turtle) {
                // TurtleBreedGoal uses pregnancy instead of Animal#spawnChildFromBreeding.
                turtle.setHasEgg(true);
                first.finalizeSpawnChildFromBreeding(level, second, null);
            } else first.spawnChildFromBreeding(level, second);
        } finally {
            if (added) level.players().remove(player);
        }
    }

    private boolean collectSpirit() {
        for (var entity : machine.getLevel().getEntitiesOfClass(ItemEntity.class, machine.area().bounds(), e -> e.isAlive()
              && e.getItem().is(ModItems.BIRTH_SPIRIT) && !e.hasPickUpDelay()
              && (e.getTarget() == null || e.getTarget().equals(PROFILE.getId())))) {
            var items = List.of(entity.getItem().copy());
            if (!machine.canFit(items, false)) { machine.setStatus(3); return true; }
            if (!machine.payEnergy()) return true;
            if (machine.insertOutputs(items)) entity.discard();
            machine.setStatus(19); machine.setActive(true);
            return true;
        }
        return false;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("breed_only", breedOnly); tag.putInt("progress", progress); tag.putInt("duration", duration);
        if (signature != null) tag.put("signature", signature);
        return tag;
    }
    public void load(CompoundTag tag) {
        breedOnly = tag.getBoolean("breed_only");
        duration = Math.clamp(tag.getInt("duration"), 1, 1_000_000);
        progress = Math.clamp(tag.getInt("progress"), 0, duration);
        signature = tag.contains("signature") ? tag.getCompound("signature") : null;
    }
    public void track(MekanismContainer c) {
        c.track(SyncableBoolean.create(() -> breedOnly, v -> breedOnly = v));
        c.track(SyncableInt.create(() -> progress, v -> progress = v));
        c.track(SyncableInt.create(() -> duration, v -> duration = Math.max(1, v)));
        c.track(SyncableInt.create(() -> nearby, v -> nearby = v));
    }
}
