package dev.everyonemek.botania;

import java.util.*;
import java.util.function.Predicate;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import vazkii.botania.api.recipe.*;
import vazkii.botania.common.crafting.StateIngredients;
import vazkii.botania.common.item.BotaniaItems;

/** Plans against copies, then commits only player and material slots on the server thread. */
public final class MachineRecipeTransfer {
    private record Need(BasicInventorySlot slot, Predicate<ItemStack> accepts) { }
    private record Spec(List<BasicInventorySlot> slots, List<Need> needs) { }
    private static Spec spec(AbstractContainerMenu menu, Recipe<?> recipe) {
        List<BasicInventorySlot> inputs, extras;
        List<Predicate<ItemStack>> materials = new ArrayList<>(); Predicate<ItemStack> extra = null;
        if (menu instanceof ApothecaryMenu apothecary) {
            var tile = apothecary.getTileEntity(); inputs = tile.inputs; extras = List.of(tile.reagent);
            if (recipe instanceof MechanicalFlowerRecipe mechanical) extra = mechanical.reagent()::test;
            else if (recipe instanceof PetalApothecaryRecipe petal) extra = petal.getReagent()::test;
            else return null;
            recipe.getIngredients().forEach(i -> materials.add(i::test));
        } else if (menu instanceof ManaMachineMenu machine) {
            var tile = machine.getTileEntity(); inputs = tile.inputs; extras = tile.extras;
            if (ManaWork.recipes(tile.kind(), tile.getLevel()).stream().noneMatch(h -> h.value() == recipe)) return null;
            if (recipe instanceof PureDaisyRecipe daisy) {
                if (!ManaWork.safeStateRecipe(daisy) || !ManaWork.safeOutput(daisy.getOutput())) return null;
                materials.add(stack -> ManaWork.itemState(stack) != null && daisy.matches(tile.getLevel(), tile.getBlockPos(), ManaWork.itemState(stack)));
            } else if (recipe instanceof ManaInfusionRecipe infusion) {
                materials.add(infusion::matches);
                if (infusion.getRecipeCatalyst() != StateIngredients.NONE) extra = stack -> ManaWork.catalystState(stack) != null
                      && infusion.getRecipeCatalyst().test(ManaWork.catalystState(stack));
            } else {
                recipe.getIngredients().forEach(i -> materials.add(i::test));
                if (recipe instanceof RunicAltarRecipe runic) {
                    runic.getCatalysts().forEach(i -> materials.add(i::test)); extra = runic.getReagent()::test;
                } else if (recipe instanceof BotanicalBreweryRecipe brew) {
                    extra = stack -> (stack.is(BotaniaItems.MANAGLASS_VIAL) || stack.is(BotaniaItems.ALFGLASS_FLASK))
                          && !brew.getOutput(stack.copyWithCount(1)).isEmpty();
                } else if (recipe instanceof ElvenTradeRecipe trade) {
                    if (recipe.getClass() != vazkii.botania.common.crafting.ElvenTradeRecipe.class || trade.isReturnRecipe()) return null;
                } else if (!(recipe instanceof TerrestrialAgglomerationRecipe)) return null;
            }
        } else return null;
        if (materials.isEmpty() || materials.size() > inputs.size() || extra != null && extras.isEmpty()) return null;
        List<Need> needs = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) needs.add(new Need(inputs.get(i), materials.get(i)));
        if (extra != null) needs.add(new Need(extras.getFirst(), extra));
        var slots = new ArrayList<>(inputs); slots.addAll(extras); return new Spec(slots, needs);
    }
    public static String transfer(Player player, AbstractContainerMenu menu, ResourceLocation id, boolean max, boolean execute) {
        if (player.containerMenu != menu || !menu.stillValid(player) || execute && player.level().isClientSide) return "unavailable";
        if (menu instanceof ApothecaryMenu apothecary && !apothecary.canPlayerAccess(player)
              || menu instanceof ManaMachineMenu machine && !machine.canPlayerAccess(player)) return "unavailable";
        var holder = player.level().getRecipeManager().byKey(id).orElse(null);
        var spec = holder == null ? null : spec(menu, holder.value()); if (spec == null) return "unsupported";
        List<ItemStack> pool = new ArrayList<>();
        for (var slot : spec.slots) pool.add(slot.getStack().copy());
        int playerStart = pool.size();
        for (int i = 0; i < 36; i++) pool.add(player.getInventory().getItem(i).copy());
        List<ItemStack> selected = select(spec.needs, pool);
        if (selected == null) return "missing";
        if (max) {
            // Keep the component-aware choices from the first batch and add complete batches only.
            for (int batch = 1; batch < 64; batch++) {
                var copy = pool.stream().map(ItemStack::copy).toList(); boolean fits = true;
                for (int i = 0; i < selected.size(); i++) {
                    var stack = selected.get(i);
                    if (stack.getCount() >= Math.min(stack.getMaxStackSize(), spec.needs.get(i).slot.getLimit(stack))) { fits = false; break; }
                    var source = copy.stream().filter(s -> !s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)).findFirst().orElse(null);
                    if (source == null) { fits = false; break; } source.shrink(1);
                }
                if (!fits) break; pool = new ArrayList<>(copy); selected.forEach(stack -> stack.grow(1));
            }
        }
        var backpack = new ArrayList<>(pool.subList(playerStart, pool.size()));
        for (int i = 0; i < playerStart; i++) if (!merge(backpack, pool.get(i))) return "full";
        if (!execute) return "";
        // All predicates, component choices and return capacity are checked before the first write.
        spec.slots.forEach(slot -> slot.setStackUnchecked(ItemStack.EMPTY));
        for (int i = 0; i < selected.size(); i++) spec.needs.get(i).slot.setStackUnchecked(selected.get(i));
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, backpack.get(i));
        if (menu instanceof ManaMachineMenu machine && !machine.getTileEntity().kind().controller())
            machine.getTileEntity().applySetting(3, id.toString());
        if (menu instanceof ApothecaryMenu apothecary) apothecary.getTileEntity().lastRecipe = id;
        player.getInventory().setChanged(); menu.broadcastChanges(); return "";
    }
    private static List<ItemStack> select(List<Need> needs, List<ItemStack> pool) {
        if (!matches(needs, pool)) return null;
        List<ItemStack> selected = new ArrayList<>();
        for (int i = 0; i < needs.size(); i++) {
            var need = needs.get(i); ItemStack chosen = null;
            for (var stack : pool) if (!stack.isEmpty() && need.accepts.test(stack) && need.slot.isItemValid(stack)) {
                var single = stack.copyWithCount(1); stack.shrink(1);
                if (matches(needs.subList(i + 1, needs.size()), pool)) { chosen = single; break; }
                stack.grow(1);
            }
            if (chosen == null) return null; selected.add(chosen);
        }
        return selected;
    }
    private static boolean matches(List<Need> needs, List<ItemStack> pool) {
        boolean[][] accepts = new boolean[needs.size()][pool.size()]; int[] required = new int[needs.size()], available = new int[pool.size()];
        Arrays.fill(required, 1);
        for (int j = 0; j < pool.size(); j++) {
            var stack = pool.get(j); available[j] = stack.getCount();
            for (int i = 0; i < needs.size(); i++) accepts[i][j] = !stack.isEmpty() && needs.get(i).accepts.test(stack) && needs.get(i).slot.isItemValid(stack);
        }
        return IngredientAssignment.matchQuantities(accepts, available, required) != null;
    }
    private static boolean merge(List<ItemStack> backpack, ItemStack stack) {
        var rest = stack.copy();
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < backpack.size() && !rest.isEmpty(); i++) {
            var current = backpack.get(i);
            if (pass == 0 ? current.isEmpty() : !current.isEmpty()) continue;
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, rest)) continue;
            int move = Math.min(rest.getCount(), rest.getMaxStackSize() - current.getCount());
            if (move > 0) { backpack.set(i, rest.copyWithCount(current.getCount() + move)); rest.shrink(move); }
        }
        return rest.isEmpty();
    }
    private MachineRecipeTransfer() { }
}
