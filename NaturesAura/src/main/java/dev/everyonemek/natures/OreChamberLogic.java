package dev.everyonemek.natures;

import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.chunk.effect.OreSpawnEffect;
import de.ellpeck.naturesaura.items.ItemEffectPowder;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.api.misc.WeightedOre;
import de.ellpeck.naturesaura.reg.ModRegistry;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** A 3x3x3 shell with its controller in the middle of a horizontal face and one empty center. */
public final class OreChamberLogic {
    private final AuraMachine machine;
    private int progress, duration = 1, targetId = -1, auraCost;
    private CompoundTag signature;
    private ResourceLocation selectedTag;
    private int selectedWeight;
    public OreChamberLogic(AuraMachine machine) { this.machine = machine; }
    public double progress() { return progress / (double) duration; }
    public ItemStack target() { return targetId < 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.byId(targetId)); }
    public int auraCost() { return auraCost; }
    public BlockPos center() { return machine.getBlockPos().relative(machine.getDirection().getOpposite()); }
    public boolean containsShell(BlockPos pos) {
        BlockPos center = center();
        int x = Math.abs(pos.getX() - center.getX()), y = Math.abs(pos.getY() - center.getY()), z = Math.abs(pos.getZ() - center.getZ());
        return x <= 1 && y <= 1 && z <= 1 && (x == 1 || y == 1 || z == 1);
    }
    public boolean formed() {
        var level = machine.getLevel();
        BlockPos center = center();
        if (level == null) return false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!level.hasChunkAt(pos)) return false;
            var state = level.getBlockState(pos);
            if (pos.equals(center)) { if (!state.isAir()) return false; }
            else if (pos.equals(machine.getBlockPos())) { if (level.getBlockEntity(pos) != machine) return false; }
            else if (!state.is(Content.CHAMBER_CASING.get()) && !state.is(Content.CHAMBER_PORT.get())) return false;
        }
        return true;
    }
    public static boolean isPowder(ItemStack stack) {
        // Reject malformed component strings instead of crashing a hopper or recipe search.
        if (!stack.is(ModItems.EFFECT_POWDER)) return false;
        var data = stack.get(ItemEffectPowder.Data.TYPE);
        return data != null && OreSpawnEffect.NAME.toString().equals(data.effect());
    }
    public static Item substrate(Level level) {
        var type = IAuraType.forLevel(level);
        return type.isSimilar(NaturesAuraAPI.TYPE_OVERWORLD) ? Items.STONE
              : type.isSimilar(NaturesAuraAPI.TYPE_NETHER) ? Items.NETHERRACK : Items.AIR;
    }
    public record Ore(ResourceLocation tag, int weight, Item item) {
        public int cost() { return (int) Math.clamp(40_000L - 4L * weight, 1, Integer.MAX_VALUE); }
    }
    public static List<Ore> ores(Level level, BlockPos center) {
        var type = IAuraType.forLevel(level);
        var weighted = type.isSimilar(NaturesAuraAPI.TYPE_OVERWORLD) ? NaturesAuraAPI.OVERWORLD_ORES
              : type.isSimilar(NaturesAuraAPI.TYPE_NETHER) ? NaturesAuraAPI.NETHER_ORES : List.<de.ellpeck.naturesaura.api.misc.WeightedOre>of();
        var player = level instanceof ServerLevel server ? FakePlayerFactory.get(server, ModRegistry.FAKE_PLAYER) : null;
        var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
              new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
        return resolveOres(weighted, context);
    }
    public static List<Ore> displayOres(boolean nether) {
        return resolveOres(nether ? NaturesAuraAPI.NETHER_ORES : NaturesAuraAPI.OVERWORLD_ORES, null);
    }
    private static List<Ore> resolveOres(List<WeightedOre> weighted, BlockPlaceContext context) {
        var result = new ArrayList<Ore>();
        for (var ore : weighted) {
            if (ore == null || ore.tag == null || ore.getWeight().asInt() <= 0) continue;
            var values = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, ore.tag));
            if (values.isEmpty()) continue;
            // Native OreSpawnEffect picks the first valid block in the selected tag.
            for (var holder : values.get()) {
                var block = holder.value();
                if (block == Blocks.AIR || block.asItem() == Items.AIR) continue;
                var state = context == null ? block.defaultBlockState() : block.getStateForPlacement(context);
                if (state == null || OreSpawnEffect.SPAWN_EXCEPTIONS.contains(state)) continue;
                result.add(new Ore(ore.tag, ore.getWeight().asInt(), block.asItem()));
                break;
            }
        }
        return result;
    }
    private void reset() { progress = 0; signature = null; selectedTag = null; targetId = -1; auraCost = 0; }

    public void tick() {
        if (!formed()) { machine.setStatus(20); return; }
        if (!ModConfig.instance.oreEffect.get() || substrate(machine.getLevel()) == Items.AIR) { machine.setStatus(21); return; }
        if (!machine.inputs.get(0).getStack().is(substrate(machine.getLevel())) || !isPowder(machine.inputs.get(1).getStack())) {
            machine.setStatus(22); return;
        }
        int environment = IAuraChunk.getAuraInArea(machine.getLevel(), machine.getBlockPos(), 30);
        machine.setEnvironmentAura(environment);
        if (environment <= 2_000_000) { machine.setStatus(23); return; }
        var ores = ores(machine.getLevel(), center());
        if (ores.isEmpty()) { reset(); machine.setStatus(24); return; }
        Ore selected = ores.stream().filter(o -> o.tag().equals(selectedTag) && o.weight() == selectedWeight
              && BuiltInRegistries.ITEM.getId(o.item()) == targetId).findFirst().orElse(null);
        if (selected == null) {
            // Long totals also handle modpack tables whose positive int weights sum beyond Integer.MAX_VALUE.
            long total = ores.stream().mapToLong(Ore::weight).sum();
            long bits, pick;
            do { bits = machine.getLevel().random.nextLong() >>> 1; pick = bits % total; }
            while (bits - pick + total - 1 < 0);
            for (Ore ore : ores) { if (pick < ore.weight()) { selected = ore; break; } pick -= ore.weight(); }
            reset();
            selectedTag = selected.tag(); selectedWeight = selected.weight();
            targetId = BuiltInRegistries.ITEM.getId(selected.item()); auraCost = selected.cost();
        }
        CompoundTag next = new CompoundTag();
        next.putString("tag", selected.tag().toString()); next.putInt("weight", selected.weight());
        next.putString("item", BuiltInRegistries.ITEM.getKey(selected.item()).toString());
        next.putString("substrate", BuiltInRegistries.ITEM.getKey(substrate(machine.getLevel())).toString());
        next.putInt("ticks", MachineConfig.ORE_TICKS.get());
        if (!next.equals(signature)) {
            signature = next; progress = 0;
            duration = Math.max(1, MekanismUtils.getTicks(machine, MachineConfig.ORE_TICKS.get()));
            machine.markForSave();
        }
        var output = List.of(new ItemStack(selected.item()));
        if (!machine.canFit(output, false)) { machine.setStatus(3); return; }
        Runnable payAura = machine.prepareAuraPayment(selected.cost());
        if (payAura == null) { machine.setStatus(5); return; }
        if (!machine.payEnergy()) return;
        machine.setActive(true); machine.setStatus(0);
        if (++progress >= duration) {
            payAura.run();
            machine.inputs.get(0).shrinkStack(1, Action.EXECUTE);
            machine.insertOutputs(output);
            reset();
        }
        machine.markForSave();
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
        selectedTag = signature == null ? null : ResourceLocation.tryParse(signature.getString("tag"));
        selectedWeight = signature == null ? 0 : signature.getInt("weight");
        var itemId = signature == null ? null : ResourceLocation.tryParse(signature.getString("item"));
        Item item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.get(itemId);
        targetId = item == Items.AIR ? -1 : BuiltInRegistries.ITEM.getId(item);
        auraCost = targetId < 0 ? 0 : (int) Math.clamp(40_000L - 4L * selectedWeight, 1, Integer.MAX_VALUE);
    }
    public void track(MekanismContainer c) {
        c.track(SyncableInt.create(() -> progress, v -> progress = v));
        c.track(SyncableInt.create(() -> duration, v -> duration = Math.max(1, v)));
        c.track(SyncableInt.create(() -> targetId, v -> targetId = v));
        c.track(SyncableInt.create(() -> auraCost, v -> auraCost = v));
    }
}
