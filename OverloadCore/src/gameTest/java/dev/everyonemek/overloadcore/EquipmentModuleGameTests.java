package dev.everyonemek.overloadcore;

import dev.everyonemek.overloadcore.gear.EquipmentModules;
import mekanism.api.Action;
import mekanism.api.gear.config.ModuleConfig;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.content.gear.mekasuit.ModuleChargeDistributionUnit;
import mekanism.common.integration.energy.EnergyCompatUtils;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import mekanism.common.registries.MekanismModules;
import mekanism.common.tile.TileEntityModificationStation;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static dev.everyonemek.overloadcore.CoreGameTests.*;

@GameTestHolder(OverloadCore.ID)
@PrefixGameTestTemplate(false)
public final class EquipmentModuleGameTests {
    private static long joules(long fe) { return EnergyUnit.FORGE_ENERGY.convertFrom(fe); }
    private static long energy(ItemStack stack) { return StorageUtils.getEnergyContainer(stack, 0).getEnergy(); }
    private static void fillCore(ServerPlayer p, long value) {
        var data = CoreBinding.data(p).copy(); data.putLong("energy", value); CoreBinding.save(p, data);
    }
    private static long core(ServerPlayer p) { return CoreBinding.data(p).getLong("energy"); }
    private static ItemStack chest(GameTestHelper h) {
        var stack = new ItemStack(MekanismItems.MEKASUIT_BODYARMOR.get());
        ModuleHelper.get().getModuleContainer(stack).addModule(h.getLevel().registryAccess(), stack, MekanismModules.ENERGY_UNIT, 8);
        return stack;
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void nativeModificationStationInstallsCapsPersistsAndReturnsCoupling(GameTestHelper h) {
        var player = player(h, new BlockPos(20, 4, 20));
        var pos = new BlockPos(20, 4, 23);
        h.setBlock(pos, MekanismBlocks.MODIFICATION_STATION.get());
        var station = (TileEntityModificationStation) h.getBlockEntity(pos);
        station.getEnergyContainer().setEnergy(station.getEnergyContainer().getMaxEnergy());
        long initialEnergy = station.getEnergyContainer().getEnergy();
        var inventory = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), net.minecraft.core.Direction.NORTH);
        check(inventory != null, "Native modification station exposes no inventory");
        check(inventory.insertItem(0, new ItemStack(CoreContent.COUPLING_MODULE.get(), 5), false).isEmpty(), "Station rejected real module item");
        check(inventory.insertItem(1, new ItemStack(MekanismItems.MEKASUIT_BODYARMOR.get()), false).isEmpty(), "Station rejected bodyarmor");
        h.startSequence().thenWaitUntil(() -> check(ModuleHelper.get().getModuleContainer(station.containerSlot.getStack())
              .installedCount(EquipmentModules.RESIDUAL_COUPLING) == 4, "Station did not finish installing four modules"))
              .thenExecute(() -> {
                  try {
                      check(inventory.getStackInSlot(0).getCount() == 1, "Station did not enforce the four-module cap");
                      check(station.getEnergyContainer().getEnergy() < initialEnergy, "Native installation consumed no power");
                      var stack = station.containerSlot.getStack();
                      var restored = ItemStack.parse(h.getLevel().registryAccess(), stack.save(h.getLevel().registryAccess())).orElseThrow();
                      check(ModuleHelper.get().getModuleContainer(restored).installedCount(EquipmentModules.RESIDUAL_COUPLING) == 4, "Item save lost installed modules");
                      for (var item : new net.minecraft.world.level.ItemLike[]{MekanismItems.MEKA_TOOL, MekanismItems.MEKASUIT_PANTS}) {
                          var other = new ItemStack(item);
                          check(!ModuleHelper.get().getModuleContainer(other).canInstall(other, EquipmentModules.RESIDUAL_COUPLING), "Coupling allowed on an unsupported item");
                      }
                      station.removeModule(player, EquipmentModules.RESIDUAL_COUPLING, true);
                      check(!ModuleHelper.get().getModuleContainer(stack).has(EquipmentModules.RESIDUAL_COUPLING), "Native uninstall left the module installed");
                      int returned = player.getInventory().items.stream().filter(s -> s.is(CoreContent.COUPLING_MODULE)).mapToInt(ItemStack::getCount).sum();
                      check(returned == 4 && inventory.getStackInSlot(0).getCount() == 1, "Uninstall duplicated or lost module items");
                  } finally { remove(player); }
              }).thenSucceed();
    }

    @GameTest(template="empty", timeoutTicks=50)
    public static void couplingLevelsRespectEnergyAcceptanceToggleAndEquippedSlot(GameTestHelper h) {
        var player = player(h, new BlockPos(20, 4, 20)); bind(player);
        try {
            var armor = chest(h); var tool = new ItemStack(MekanismItems.MEKA_TOOL.get());
            player.setItemSlot(EquipmentSlot.CHEST, armor); player.getInventory().setItem(0, tool);
            long initial = joules(1_000_000); // Existing above-capacity reserves are preserved, not minted by the module.
            for (int level = 1; level <= 4; level++) {
                ModuleHelper.get().getModuleContainer(armor).addModule(player.registryAccess(), armor, EquipmentModules.RESIDUAL_COUPLING, 1);
                StorageUtils.getEnergyContainer(armor, 0).setEnergy(0); StorageUtils.getEnergyContainer(tool, 0).setEnergy(0); fillCore(player, initial);
                long offered = Math.min(initial, joules(CoreConfig.couplingRateFE(level)));
                long expected = offered - EnergyCompatUtils.getStrictEnergyHandler(armor).insertEnergy(offered, Action.SIMULATE);
                check(expected > joules(CoreConfig.CHARGE_FE.get()), "Coupling level has no usable improvement");
                CoreBinding.charge(player);
                check(energy(armor) == expected && core(player) == initial - expected && energy(tool) == 0, "Coupling failed conservation, level cap or chest priority");
            }
            // Almost-full chest and tiny reserve must retain precise remainders.
            player.getInventory().setItem(0, ItemStack.EMPTY);
            var storage = StorageUtils.getEnergyContainer(armor, 0); storage.setEnergy(storage.getMaxEnergy() - 7); fillCore(player, 10);
            CoreBinding.charge(player);
            check(core(player) == 3 && energy(armor) == storage.getMaxEnergy(), "Full chest lost unused reserve");
            CoreBinding.charge(player); check(core(player) == 3, "Full chest continued consuming reserve");
            storage.setEnergy(0); CoreBinding.charge(player);
            check(energy(armor) == 3 && core(player) == 0, "Small remainder was rounded away or multiplied");
            // Native module configuration disables only the upgrade; the previous base charge remains.
            var container = ModuleHelper.get().getModuleContainer(armor);
            var config = container.get(EquipmentModules.RESIDUAL_COUPLING).<Boolean>getConfigOrThrow(ModuleConfig.ENABLED_KEY);
            container.replaceModuleConfig(player.registryAccess(), armor, EquipmentModules.RESIDUAL_COUPLING, config.with(false));
            player.getInventory().setItem(0, tool); storage.setEnergy(0); fillCore(player, initial);
            CoreBinding.charge(player);
            check(energy(tool) == joules(CoreConfig.CHARGE_FE.get()) && energy(armor) == 0, "Disabled module broke base charging");
            var saved = ItemStack.parse(player.registryAccess(), armor.save(player.registryAccess())).orElseThrow();
            check(!ModuleHelper.get().getModuleContainer(saved).hasEnabled(EquipmentModules.RESIDUAL_COUPLING), "Disabled setting was not persistent");
            container = ModuleHelper.get().getModuleContainer(armor);
            container.replaceModuleConfig(player.registryAccess(), armor, EquipmentModules.RESIDUAL_COUPLING, config.with(true));
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); player.getInventory().setItem(1, armor);
            StorageUtils.getEnergyContainer(tool, 0).setEnergy(0); fillCore(player, initial); CoreBinding.charge(player);
            check(energy(tool) == joules(CoreConfig.CHARGE_FE.get()) && energy(armor) == 0, "Backpack module granted worn bonus");
            player.getPersistentData().remove(CoreBinding.KEY); long before = energy(tool); CoreBinding.charge(player);
            check(energy(tool) == before, "Unbound player gained charging");
            h.succeed();
        } finally { remove(player); }
    }

    @GameTest(template="empty", timeoutTicks=50)
    public static void nativeSuitDistributionReceivesCoupledPowerWithoutDoubleDebiting(GameTestHelper h) {
        var player = player(h, new BlockPos(20, 4, 20)); bind(player);
        var armor = chest(h); var tool = new ItemStack(MekanismItems.MEKA_TOOL.get());
        ModuleHelper.get().getModuleContainer(armor).addModule(player.registryAccess(), armor, EquipmentModules.RESIDUAL_COUPLING, 1);
        ModuleHelper.get().getModuleContainer(armor).addModule(player.registryAccess(), armor, MekanismModules.CHARGE_DISTRIBUTION_UNIT, 1);
        var container = ModuleHelper.get().getModuleContainer(armor);
        var setting = container.get(MekanismModules.CHARGE_DISTRIBUTION_UNIT).<Boolean>getConfigOrThrow(ModuleChargeDistributionUnit.CHARGE_INVENTORY);
        container.replaceModuleConfig(player.registryAccess(), armor, MekanismModules.CHARGE_DISTRIBUTION_UNIT, setting.with(true));
        player.setItemSlot(EquipmentSlot.CHEST, armor); player.getInventory().setItem(0, tool);
        long initial = joules(50_000); fillCore(player, initial);
        h.startSequence().thenIdle(5).thenExecute(() -> {
            try {
                check(energy(tool) > 0 && energy(armor) > 0, "Actual player ticks did not drive native chest distribution");
                check(core(player) + energy(tool) + energy(armor) == initial, "Core/chest/tool distribution did not conserve energy");
            } finally { remove(player); }
        }).thenSucceed();
    }
}
