package dev.everyonemek.overloadcore;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.CuriosApi;

/** Ninths of an ingot; supported nested containers are bounded and never query remote storage. */
public final class MetalLoad {
    public static final TagKey<Item> INGOTS = tag("metal_ingots"), NUGGETS = tag("metal_nuggets"), BLOCKS = tag("metal_blocks"), EQUIPMENT = tag("metal_equipment");
    private static TagKey<Item> tag(String name) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(OverloadCore.ID, name)); }
    public static int measure(Player player) {
        int[] budget = {1024}; long mass = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) mass += stack(player.getInventory().getItem(i), 0, budget);
        var curios = CuriosApi.getCuriosInventory(player).map(h -> h.getEquippedCurios()).orElse(null);
        if (curios != null) for (int i = 0; i < curios.getSlots(); i++) mass += stack(curios.getStackInSlot(i), 0, budget);
        return (int) Math.min(Integer.MAX_VALUE, (mass + 8) / 9);
    }
    private static long stack(ItemStack stack, int depth, int[] budget) {
        if (stack.isEmpty()) return 0;
        if (budget[0]-- <= 0 || depth > 4) return 9L * CoreConfig.METAL_LIMIT.get();
        int weight = stack.is(BLOCKS) ? 81 : stack.is(INGOTS) ? 9 : stack.is(NUGGETS) ? 1 : stack.is(EQUIPMENT) ? 72 : 0;
        long total = (long) weight * stack.getCount();
        var container = stack.get(DataComponents.CONTAINER);
        if (container != null) for (var content : container.nonEmptyItems()) {
            total += stack(content, depth + 1, budget) * Math.min(64, stack.getCount());
            if (budget[0] <= 0) break;
        }
        return Math.min(total, Integer.MAX_VALUE);
    }
    public static boolean blocksSprint(Player player) {
        return player.level().isClientSide ? CorePackets.clientState.getBoolean("bound") && CorePackets.clientState.getBoolean("heavy")
              : CoreBinding.bound(player) && CoreBinding.data(player).getBoolean("heavy");
    }
    private MetalLoad() { }
}
