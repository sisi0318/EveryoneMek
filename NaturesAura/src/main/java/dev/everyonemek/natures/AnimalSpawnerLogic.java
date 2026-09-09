package dev.everyonemek.natures;

import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.recipes.AnimalSpawnerRecipe;
import de.ellpeck.naturesaura.recipes.ModRecipes;
import java.util.Comparator;
import mekanism.api.Action;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;

/** Uses the live Nature's Aura recipe table and its native entity initialization. */
public final class AnimalSpawnerLogic {
    private final AuraMachine machine;
    private int progress, duration = 1, entityId = -1, nearby;
    private CompoundTag signature;
    public AnimalSpawnerLogic(AuraMachine machine) { this.machine = machine; }
    public double progress() { return progress / (double) duration; }
    public int nearby() { return nearby; }
    public EntityType<?> target() { return entityId < 0 ? null : BuiltInRegistries.ENTITY_TYPE.byId(entityId); }
    private record Plan(AnimalSpawnerRecipe recipe, int[] consume, CompoundTag signature) { }

    private Plan find() {
        var level = machine.getLevel();
        for (RecipeHolder<AnimalSpawnerRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ANIMAL_SPAWNER_TYPE)
              .stream().sorted(Comparator.comparing(h -> h.id().toString())).toList()) {
            var r = holder.value();
            if (r.entity == null || r.ingredients.isEmpty() || r.ingredients.size() > 128 || r.aura < 0 || r.time < 1) continue;
            int[] available = machine.inputs.stream().mapToInt(s -> s.getCount()).toArray();
            int[] required = new int[r.ingredients.size()];
            boolean[][] accepts = new boolean[required.length][available.length];
            for (int i = 0; i < required.length; i++) {
                required[i] = Math.max(1, Helper.getIngredientAmount(r.ingredients.get(i)));
                for (int s = 0; s < available.length; s++) accepts[i][s] = r.ingredients.get(i).test(machine.inputs.get(s).getStack());
            }
            int[] consume = IngredientAssignment.matchQuantities(accepts, available, required);
            if (consume == null) continue;
            var tag = new CompoundTag();
            tag.putString("recipe", holder.id().toString());
            tag.putString("entity", BuiltInRegistries.ENTITY_TYPE.getKey(r.entity).toString());
            tag.putInt("aura", r.aura); tag.putInt("time", r.time); tag.putIntArray("consume", consume);
            tag.put("area", machine.area().save());
            var items = new ListTag();
            for (var slot : machine.inputs) items.add(slot.isEmpty() ? new CompoundTag() : slot.getStack().copyWithCount(1).save(level.registryAccess()));
            tag.put("inputs", items);
            return new Plan(r, consume, tag);
        }
        return null;
    }

    public void tick() {
        Plan plan = find();
        if (plan == null) { progress = 0; signature = null; entityId = -1; machine.setStatus(4); return; }
        var r = plan.recipe();
        entityId = BuiltInRegistries.ENTITY_TYPE.getId(r.entity);
        if (!plan.signature().equals(signature)) {
            progress = 0;
            signature = plan.signature();
            duration = Math.max(1, MekanismUtils.getTicks(machine, r.time));
        }
        nearby = machine.area().count(r.entity);
        if (nearby >= machine.area().cap()) { machine.setStatus(14); return; }
        Runnable payAura = machine.prepareAuraPayment(r.aura);
        if (payAura == null) { machine.setStatus(5); return; }
        BlockPos pos = findPosition(r.entity);
        if (pos == null) { machine.setStatus(15); return; }
        if (progress < duration) {
            if (!machine.payEnergy()) return;
            progress++;
        }
        machine.setStatus(0);
        machine.setActive(true);
        if (progress >= duration) {
            var level = machine.getLevel();
            var entity = r.makeEntity(level, pos);
            if (entity == null || !level.noCollision(entity, entity.getBoundingBox()) || !level.isUnobstructed(entity)
                  || !level.addFreshEntity(entity)) {
                machine.setStatus(16); machine.setActive(false); machine.markForSave(); return;
            }
            payAura.run();
            for (int i = 0; i < plan.consume().length; i++) machine.inputs.get(i).shrinkStack(plan.consume()[i], Action.EXECUTE);
            progress = 0; signature = null;
            nearby++;
        }
        machine.markForSave();
    }

    private BlockPos findPosition(EntityType<?> type) {
        var level = machine.getLevel();
        var area = machine.area();
        int radius = area.radius(), width = radius * 2 + 1, cells = width * width;
        int start = level.random.nextInt(cells);
        boolean aquatic = switch (type.getCategory()) {
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> true;
            default -> false;
        };
        for (int i = 0; i < cells; i++) {
            int cell = (start + i) % cells;
            BlockPos pos = area.center().offset(cell % width - radius, 0, cell / width - radius);
            var box = type.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos));
            if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                  || !level.getWorldBorder().isWithinBounds(box)) continue;
            boolean loaded = true;
            for (BlockPos edge : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                if (!level.hasChunkAt(edge)) { loaded = false; break; }
            }
            if (!loaded || !level.noCollision(box)) continue;
            if (aquatic ? !level.getFluidState(pos).is(FluidTags.WATER)
                  : !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) || !level.getFluidState(pos).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("progress", progress); tag.putInt("duration", duration);
        if (signature != null) tag.put("signature", signature);
        return tag;
    }
    public void load(CompoundTag tag) {
        duration = Math.clamp(tag.getInt("duration"), 1, 1_000_000);
        progress = Math.clamp(tag.getInt("progress"), 0, duration);
        signature = tag.contains("signature") ? tag.getCompound("signature") : null;
    }
    public void track(MekanismContainer c) {
        c.track(SyncableInt.create(() -> progress, v -> progress = v));
        c.track(SyncableInt.create(() -> duration, v -> duration = Math.max(1, v)));
        c.track(SyncableInt.create(() -> entityId, v -> entityId = v));
        c.track(SyncableInt.create(() -> nearby, v -> nearby = v));
    }
}
