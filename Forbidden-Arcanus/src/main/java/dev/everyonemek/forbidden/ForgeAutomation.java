package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.HephaestusForgeBlock;
import com.stal111.forbidden_arcanus.common.block.entity.PedestalBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.circle.MagicCircleController;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.*;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.UpgradeTierResult;
import com.stal111.forbidden_arcanus.common.block.pedestal.effect.PedestalEffectTrigger;
import com.stal111.forbidden_arcanus.common.item.component.RitualStarter;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.init.other.ModPOITypes;
import dev.everyonemek.forbidden.mixin.ForgeAccess;
import dev.everyonemek.forbidden.mixin.RitualAccess;
import java.util.*;
import mekanism.api.Action;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;

public final class ForgeAutomation {
    public static final String RECEIPT = "forbiddenmekanism_batch";
    private static final int[][] POSITIONS = {{0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}};
    public static List<PedestalBlockEntity> pedestals(HephaestusForgeBlockEntity forge) {
        var result = new ArrayList<PedestalBlockEntity>();
        var level = (ServerLevel) forge.getLevel();
        for (int[] offset : POSITIONS) {
            BlockPos pos = forge.getBlockPos().offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)) continue;
            // The native pedestal effect updates the first nearby forge. Use exactly the same association.
            var owner = level.getPoiManager().getInRange(type -> type.value() == ModPOITypes.HEPHAESTUS_FORGE.get(), pos, 4,
                  PoiManager.Occupancy.ANY).findFirst();
            if (owner.isPresent() && owner.get().getPos().equals(forge.getBlockPos())) result.add(pedestal);
        }
        return result;
    }
    public static boolean tick(Controller controller, HephaestusForgeBlockEntity forge) {
        var manager = forge.getRitualManager();
        if (controller.phase == 2) return finish(controller, forge);
        if (manager.isRitualActive()) { controller.status = Controller.RUNNING; return false; }
        if (controller.phase == 1) return start(controller, forge);
        var pedestals = pedestals(forge);
        if (!forge.getStack(4).isEmpty() || pedestals.stream().anyMatch(PedestalBlockEntity::hasStack)) {
            var valid = manager.getValidRitual().orElse(null);
            if (valid == null) { controller.status = Controller.MANUAL_ITEMS; return false; }
            String id = valid.unwrapKey().orElseThrow().location().toString();
            if (!controller.recipeLock.isEmpty() && !controller.recipeLock.equals(id)
                  || controller.recipeLock.isEmpty() && valid.value().result() instanceof UpgradeTierResult) {
                controller.status = Controller.RECIPE_CONFLICT; return false;
            }
            if (!controller.canStore(valid.value().result().getResultItem(forge.getStack(4)))) { controller.status = Controller.OUTPUT_FULL; return false; }
            prepare(controller, forge, valid);
            controller.preparedSignature = signature(forge);
            return start(controller, forge);
        }
        if (!controller.infiniteHammer() && controller.hammer.isEmpty()) { controller.status = Controller.NEED_HAMMER; return false; }
        var stock = controller.stock.stream().map(s -> s.getStack().copy()).toList();
        controller.status = Controller.NEED_MATERIALS;
        for (var holder : Recipes.rituals(forge.getLevel())) {
            String id = holder.key().location().toString(); Ritual ritual = holder.value();
            if (!controller.recipeLock.isEmpty() && !controller.recipeLock.equals(id)
                  || controller.recipeLock.isEmpty() && ritual.result() instanceof UpgradeTierResult) continue;
            int[] allocation = Recipes.allocate(Recipes.ingredients(ritual), stock);
            if (allocation == null) continue;
            if (pedestals.size() < allocation.length - 1) { controller.status = Controller.STRUCTURE; continue; }
            var entries = new ArrayList<ForgeDataCache.IngredientEntry>();
            for (int i = 1; i < allocation.length; i++) entries.add(new ForgeDataCache.IngredientEntry(pedestals.get(i - 1).getBlockPos(), stock.get(allocation[i]).copyWithCount(1)));
            var cache = new ForgeDataCache(entries, stock.get(allocation[0]).copyWithCount(1), ((ForgeAccess) forge).forbiddenmekanism$data().enhancers());
            var preview = new RitualManager(new MagicCircleController(HephaestusForgeBlockEntity.UPDATE_MAGIC_CIRCLE), tier(forge), cache);
            if (!((RitualAccess) preview).forbiddenmekanism$canStart(ritual, forge.getEssences())) { controller.status = Controller.CONDITIONS; continue; }
            // Verify the same native registry order before placing anything, so an overlapping recipe cannot steal this batch.
            preview.updateValidRitual(forge.getEssences(), forge.getLevel().registryAccess());
            if (!preview.getValidRitual().map(r -> r.equals(holder)).orElse(false)) { controller.status = Controller.RECIPE_CONFLICT; continue; }
            ItemStack output = ritual.result().getResultItem(cache.mainIngredient());
            if (!controller.canStore(output)) { controller.status = Controller.OUTPUT_FULL; continue; }
            prepare(controller, forge, holder);
            controller.expected = output.copy();
            // Remove ownership from each buffer before invoking a native placement effect.
            controller.stock.get(allocation[0]).shrinkStack(1, Action.EXECUTE);
            forge.setStack(4, cache.mainIngredient().copy());
            for (int i = 1; i < allocation.length; i++) {
                controller.stock.get(allocation[i]).shrinkStack(1, Action.EXECUTE);
                var pedestal = pedestals.get(i - 1);
                pedestal.setStack(entries.get(i - 1).stack().copy(), controller.binding.actor(), PedestalEffectTrigger.PLAYER_PLACE_ITEM);
            }
            forge.setChanged(); controller.markForSave();
            controller.preparedSignature = signature(forge);
            start(controller, forge);
            return true;
        }
        return false;
    }
    private static int tier(HephaestusForgeBlockEntity forge) { return ((HephaestusForgeBlock) forge.getBlockState().getBlock()).getLevel().getAsInt(); }
    private static void prepare(Controller controller, HephaestusForgeBlockEntity forge, Holder<Ritual> ritual) {
        controller.batch = UUID.randomUUID(); controller.phase = 1;
        controller.batchRecipe = ritual.unwrapKey().orElseThrow().location().toString();
        controller.selectedRecipe = controller.batchRecipe;
        controller.expected = ritual.value().result().getResultItem(forge.getStack(4)).copy();
        controller.startingTier = tier(forge); controller.markForSave();
    }
    private static boolean start(Controller controller, HephaestusForgeBlockEntity forge) {
        if (forge.getRitualManager().isRitualActive()) { controller.status = Controller.RUNNING; return false; }
        if (controller.preparedSignature == null || !controller.preparedSignature.equals(signature(forge))) {
            controller.status = Controller.INTERRUPTED; return false;
        }
        var valid = forge.getRitualManager().getValidRitual().orElse(null);
        if (valid == null) { controller.status = Controller.CONDITIONS; return false; }
        if (!valid.unwrapKey().orElseThrow().location().toString().equals(controller.batchRecipe)) { controller.status = Controller.RECIPE_CONFLICT; return false; }
        if (!controller.canStore(controller.expected)) { controller.status = Controller.OUTPUT_FULL; return false; }
        boolean infinite = controller.infiniteHammer();
        if (!infinite && controller.hammer.isEmpty()) { controller.status = Controller.NEED_HAMMER; return false; }
        var player = controller.binding.actor();
        ItemStack previous = player.getMainHandItem();
        ItemStack tool = infinite ? new ItemStack(ModItems.DIAMOND_BLACKSMITH_GAVEL.get()) : controller.hammer.getStack().copy();
        if (infinite) {
            RitualStarter original = tool.get(ModDataComponents.RITUAL_STARTER);
            tool.set(ModDataComponents.RITUAL_STARTER, new RitualStarter(0, original.soundEvent()));
        }
        var receipt = new CompoundTag(); receipt.putUUID("id", controller.batch);
        forge.getPersistentData().put(RECEIPT, receipt);
        boolean started;
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            BlockPos pos = forge.getBlockPos();
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            player.gameMode.useItemOn(player, player.level(), tool, InteractionHand.MAIN_HAND,
                  new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            started = forge.getRitualManager().isRitualActive();
            if (started && !infinite) controller.hammer.setStackUnchecked(player.getMainHandItem().copy());
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
        if (!started) { forge.getPersistentData().remove(RECEIPT); controller.status = Controller.CONDITIONS; return false; }
        controller.phase = 2; controller.status = Controller.RUNNING;
        forge.setChanged(); controller.markForSave(); return true;
    }
    private static boolean finish(Controller controller, HephaestusForgeBlockEntity forge) {
        if (forge.getRitualManager().isRitualActive()) { controller.status = Controller.RUNNING; return false; }
        var receipt = forge.getPersistentData().getCompound(RECEIPT);
        if (!receipt.hasUUID("id") || !receipt.getUUID("id").equals(controller.batch) || !receipt.getBoolean("done") || !receipt.getBoolean("success")) {
            controller.status = Controller.INTERRUPTED; return false;
        }
        if (controller.expected.isEmpty()) {
            if (tier(forge) <= controller.startingTier) { controller.status = Controller.INTERRUPTED; return false; }
            controller.enabled = false; controller.status = Controller.UPGRADED;
        } else {
            ItemStack output = forge.getStack(4);
            if (!ItemStack.matches(controller.expected, output)) { controller.status = Controller.INTERRUPTED; return false; }
            if (!controller.storeOutput(output)) { controller.status = Controller.OUTPUT_FULL; return false; }
            forge.setStack(4, ItemStack.EMPTY); controller.status = Controller.IDLE;
        }
        forge.getPersistentData().remove(RECEIPT); controller.clearBatch(); forge.setChanged();
        return true;
    }
    public static void completed(ServerLevel level, BlockPos pos, boolean success) {
        if (level == null || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof HephaestusForgeBlockEntity forge)) return;
        var receipt = forge.getPersistentData().getCompound(RECEIPT);
        if (!receipt.hasUUID("id") || receipt.getBoolean("done")) return;
        receipt.putBoolean("done", true); receipt.putBoolean("success", success);
        // Native cancellation can be called from a slot-change callback as well as serverTick. Its item was already ejected.
        if (!success) forge.setStack(4, ItemStack.EMPTY);
        forge.setChanged();
    }
    private static CompoundTag signature(HephaestusForgeBlockEntity forge) {
        var tag = new CompoundTag(); var provider = forge.getLevel().registryAccess();
        tag.put("main", forge.getStack(4).saveOptional(provider));
        var inputs = new net.minecraft.nbt.ListTag();
        ((ForgeAccess) forge).forbiddenmekanism$data().cachedIngredients().stream()
              .sorted(Comparator.comparingLong(entry -> entry.pos().asLong())).forEach(entry -> {
                  var input = new CompoundTag(); input.putLong("pos", entry.pos().asLong()); input.put("item", entry.stack().saveOptional(provider)); inputs.add(input);
              });
        tag.put("pedestals", inputs);
        var enhancers = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 4; i++) enhancers.add(forge.getStack(i).saveOptional(provider));
        tag.put("enhancers", enhancers); return tag;
    }
    public static boolean reset(Controller controller) {
        var block = controller.binding.resolve();
        if (!(block instanceof HephaestusForgeBlockEntity forge) || forge.getRitualManager().isRitualActive()) return false;
        forge.getPersistentData().remove(RECEIPT); controller.clearBatch(); forge.setChanged(); return true;
    }
    private ForgeAutomation() { }
}
