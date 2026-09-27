package dev.everyonemek.overloadcore.gear;

import java.util.function.Consumer;
import dev.everyonemek.overloadcore.CoreBinding;
import dev.everyonemek.overloadcore.CoreConfig;
import dev.everyonemek.overloadcore.CoreContent;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Charging is dispatched once by CoreBinding, not again by the suit's module tick. */
public final class ResidualCouplingUnit implements ICustomModule<ResidualCouplingUnit> {
    public static int wornLevel(Player player) {
        if (!CoreBinding.active(player) || !CuriosApi.getCuriosInventory(player)
              .flatMap(inv -> inv.getStacksHandler(CoreBinding.SLOT))
              .map(slots -> slots.getStacks().getSlots() > 0 && !slots.getActiveStates().isEmpty()
                    && slots.getActiveStates().get(0) && CoreBinding.matches(player, slots.getStacks().getStackInSlot(0)))
              .orElse(false)) return 0;
        var module = IModuleHelper.INSTANCE.getIfEnabled(player.getItemBySlot(EquipmentSlot.CHEST), EquipmentModules.RESIDUAL_COUPLING);
        return module == null ? 0 : module.getInstalledCount();
    }

    @Override public void addHUDStrings(IModule<ResidualCouplingUnit> module, IModuleContainer container,
          ItemStack stack, Player player, Consumer<Component> adder) {
        adder.accept(CoreBinding.active(player) ? CoreContent.text("coupling.rate", CoreConfig.couplingRateFE(module.getInstalledCount()))
              : CoreContent.text("coupling.no_core"));
    }
}
