package dev.everyonemek.ars;

import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.common.block.tile.DrygmyTile;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.WhirlisprigTile;
import com.hollingsworth.arsnouveau.common.entity.EntityDrygmy;
import com.hollingsworth.arsnouveau.common.entity.Whirlisprig;
import com.hollingsworth.arsnouveau.common.items.RitualTablet;
import com.hollingsworth.arsnouveau.common.ritual.RitualOvergrowth;
import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.RelativeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

final class WorldControllers {
    private static boolean supported(AbstractRitual ritual) {
        return ritual != null && RitualRegistry.getRitualMap().containsKey(ritual.getRegistryName());
    }

    static boolean acceptsTablet(ItemStack stack) { return stack.getItem() instanceof RitualTablet tablet && supported(tablet.ritual); }

    static boolean acceptsCollected(SourceMachine m, ItemStack stack) {
        if (!m.kind().creatureController() || m.inputs == null || m.inputs.isEmpty()) return true;
        ItemStack filter = m.inputs.getFirst().getStack();
        return filter.isEmpty() || ItemStack.isSameItemSameComponents(filter, stack) == (m.mode() == 0);
    }

    static AdvancedRecipes.Work plan(SourceMachine m) {
        BlockPos targetPos = m.getBlockPos().relative(RelativeSide.FRONT.getDirection(m.getDirection()));
        if (!m.getLevel().hasChunkAt(targetPos)) { m.observe(-1, -1, -1); m.status(SourceMachine.NO_TARGET); return null; }
        BlockEntity target = m.getLevel().getBlockEntity(targetPos);
        if (m.kind() == MachineKind.RITUAL_CONTROLLER) {
            if (!(target instanceof RitualBrazierTile brazier)) { m.status(SourceMachine.NO_TARGET); return null; }
            return ritual(m, brazier);
        }
        if (m.kind() == MachineKind.DRYGMY_STATION && target instanceof DrygmyTile hive) {
            m.observe(hive.bonus, hive.progress, hive.getMaxProgress());
            if (hive.isOff) { m.status(SourceMachine.TARGET_DISABLED); return null; }
            boolean creature = !m.getLevel().getEntitiesOfClass(EntityDrygmy.class, new AABB(targetPos).inflate(12),
                  entity -> entity.isAlive() && entity.isTamed() && targetPos.equals(entity.homePos)).isEmpty();
            if (!hive.converted || !creature) { m.status(SourceMachine.NO_CREATURE); return null; }
        } else if (m.kind() == MachineKind.WHIRLISPRIG_STATION && target instanceof WhirlisprigTile flower) {
            m.observe(flower.moodScore, flower.diversityScore, flower.progress);
            if (flower.isOff) { m.status(SourceMachine.TARGET_DISABLED); return null; }
            boolean creature = !m.getLevel().getEntitiesOfClass(Whirlisprig.class, new AABB(targetPos).inflate(12),
                  entity -> entity.isAlive() && entity.isTamed() && targetPos.equals(entity.flowerPos)).isEmpty();
            if (!flower.converted || !creature) { m.status(SourceMachine.NO_CREATURE); return null; }
        } else { m.observe(-1, -1, -1); m.status(SourceMachine.NO_TARGET); return null; }
        return supplyAndCollect(m, target, 1);
    }

    private static CompoundTag targetSignature(SourceMachine m, BlockEntity target) {
        var tag = new CompoundTag();
        tag.putLong("target", target.getBlockPos().asLong());
        tag.putString("block", BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()).toString());
        if (target instanceof RitualBrazierTile brazier && brazier.ritual != null) {
            tag.putString("ritual", brazier.ritual.getRegistryName().toString());
            tag.putBoolean("running", brazier.ritual.isRunning());
            var consumed = new ListTag();
            for (ItemStack stack : brazier.ritual.getConsumedItems()) consumed.add(stack.save(m.getLevel().registryAccess()));
            tag.put("augments", consumed);
        }
        return tag;
    }

    private static AdvancedRecipes.Work supplyAndCollect(SourceMachine m, BlockEntity target, int firstItemSlot) {
        int[] consume = new int[m.inputs.size()];
        var products = new ArrayList<ItemStack>();
        for (int i = firstItemSlot; i < m.inputs.size(); i++) {
            ItemStack stack = m.inputs.get(i).getStack();
            if (stack.isEmpty() || !acceptsCollected(m, stack)) continue;
            var candidate = new ArrayList<>(products); candidate.add(stack.copy());
            if (m.mergeOutputs(candidate) == null) continue;
            products = candidate; consume[i] = stack.getCount();
        }
        var external = targetSignature(m, target);
        BlockPos jarPos = m.getBlockPos().above();
        var source = m.getLevel().hasChunkAt(jarPos) && m.getLevel().getBlockEntity(jarPos) instanceof SourceJarTile
              ? m.getLevel().getCapability(CapabilityRegistry.SOURCE_CAPABILITY, jarPos, Direction.DOWN) : null;
        int available = (int) Math.min(200, m.sourceTank().getStored());
        int transfer = source == null ? 0 : Math.clamp(source.receiveSource(available, true), 0, available);
        if (products.isEmpty() && transfer == 0) {
            if (m.inputs.subList(firstItemSlot, m.inputs.size()).stream().anyMatch(slot -> !slot.isEmpty() && acceptsCollected(m, slot.getStack())))
                m.status(SourceMachine.OUTPUT_FULL);
            else if (target instanceof WhirlisprigTile flower && flower.moodScore <= 0) m.status(SourceMachine.NO_ENVIRONMENT);
            else if (!hasNativeSupply(m, target)) m.status(source == null && !m.sourceTank().isEmpty()
                  ? SourceMachine.NO_CONTAINER : SourceMachine.NEED_SOURCE);
            else m.status(SourceMachine.IDLE);
            return null;
        }
        external.putLong("source_jar", jarPos.asLong());
        var outputs = List.copyOf(products);
        return AdvancedRecipes.work(m, "arsmekanism:supply_and_collect", consume, outputs, () -> outputs,
              transfer, 0, 0, 0, external, () -> {
                  if (transfer > 0 && source.receiveSource(transfer, false) != transfer)
                      throw new IllegalStateException("Source jar changed during a controller transaction");
              });
    }

    private static boolean hasNativeSupply(SourceMachine m, BlockEntity target) {
        int cost, range;
        if (target instanceof DrygmyTile hive) {
            if (!hive.needsMana) return true;
            cost = Config.DRYGMY_MANA_COST.get(); range = 7;
        } else if (target instanceof WhirlisprigTile) {
            cost = Config.WHIRLISPRIG_SOURCE_COST.get(); range = 5;
        } else if (target instanceof RitualBrazierTile brazier && brazier.ritual != null) {
            if (!brazier.ritual.consumesSource() || !brazier.ritual.needsSourceNow()) return true;
            cost = brazier.ritual.getSourceCost(); range = 6;
        } else return false;
        if (cost <= 0) return true;
        long now = m.getLevel().getGameTime();
        if (m.checkedSourceTarget != target || m.checkedSourceCost != cost || now >= m.nextSourceCheck) {
            m.checkedSourceTarget = target; m.checkedSourceCost = cost; m.nextSourceCheck = now + 20;
            // Some addon providers inherit ISourceTile's mutating default for simulate=true.
            // A status preview must only read stores, never invoke a removal method.
            long available = 0;
            for (var provider : SourceUtil.canTakeSource(target.getBlockPos(), m.getLevel(), range)) {
                var store = provider.getSource();
                if (store instanceof com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile) { available = cost; break; }
                available += Math.max(0, store.getSource());
                if (available >= cost) break;
            }
            m.nativeSourceAvailable = available >= cost;
        }
        return m.nativeSourceAvailable;
    }

    private static AdvancedRecipes.Work ritual(SourceMachine m, RitualBrazierTile brazier) {
        m.observe(brazier.ritual == null ? 0 : brazier.ritual.isRunning() ? 2 : 1,
              brazier.ritual == null ? 0 : brazier.ritual.getProgress(), -1);
        if (brazier.isOff || brazier.isDecorative) { m.status(SourceMachine.TARGET_DISABLED); return null; }
        if (brazier.ritual == null) {
            ItemStack input = m.inputs.getFirst().getStack();
            if (!(input.getItem() instanceof RitualTablet tablet)) { m.status(SourceMachine.MISSING_INPUT); return null; }
            if (!supported(tablet.ritual) || RitualRegistry.getRitual(tablet.ritual.getRegistryName()) == null) {
                m.status(SourceMachine.UNSUPPORTED_RITUAL); return null;
            }
            int[] consume = new int[m.inputs.size()]; consume[0] = 1;
            return AdvancedRecipes.work(m, "arsmekanism:load_ritual", consume, List.of(), List::of, 0, 0, 0, 0,
                  targetSignature(m, brazier), () -> brazier.setRitual(tablet.ritual.getRegistryName()));
        }
        AbstractRitual ritual = brazier.ritual;
        if (!supported(ritual)) { m.status(SourceMachine.UNSUPPORTED_RITUAL); return null; }
        if (ritual.isDone()) { m.status(SourceMachine.IDLE); return null; }
        if (!ritual.isRunning()) {
            if (m.mode() == 1 && !(ritual instanceof RitualOvergrowth)) { m.status(SourceMachine.UNSUPPORTED_RITUAL); return null; }
            ItemStack augment = m.inputs.get(1).getStack();
            boolean accepted = !augment.isEmpty() && ritual.canConsumeItem(augment.copyWithCount(1));
            if (m.mode() == 1 && !ritual.didConsumeItem(Items.BONE_BLOCK) && (!augment.is(Items.BONE_BLOCK) || !accepted)) {
                m.status(SourceMachine.MISSING_MATERIALS); return null;
            }
            if (accepted) {
                int[] consume = new int[m.inputs.size()]; consume[1] = 1;
                return AdvancedRecipes.work(m, "arsmekanism:augment_ritual", consume, List.of(), List::of, 0, 0, 0, 0,
                      targetSignature(m, brazier), () -> {
                          ItemStack offered = augment.copyWithCount(1);
                          if (!brazier.tryBurnStack(offered) || !offered.isEmpty())
                              throw new IllegalStateException("Ritual did not consume its prepared augment");
                      });
            }
            var player = m.requestedRitual == ritual && m.ritualPlayer != null ? m.getLevel().getPlayerByUUID(m.ritualPlayer) : null;
            if (m.mode() == 2 && player == null) { m.status(SourceMachine.WAITING_START); return null; }
            if (!ritual.canStart(player)) { m.status(SourceMachine.RITUAL_CONDITIONS); return null; }
            return AdvancedRecipes.work(m, "arsmekanism:start_ritual", new int[m.inputs.size()], List.of(), List::of,
                  0, 0, 0, 0, targetSignature(m, brazier), () -> {
                      brazier.startRitual(player); brazier.updateBlock(); m.requestedRitual = null; m.ritualPlayer = null;
                  });
        }
        return supplyAndCollect(m, brazier, 2);
    }

    private WorldControllers() { }
}
