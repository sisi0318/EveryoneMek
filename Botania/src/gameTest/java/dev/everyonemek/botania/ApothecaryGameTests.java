package dev.everyonemek.botania;

import java.util.*;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.item.BotaniaItems;

import static dev.everyonemek.botania.BotanicalGameTests.*;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class ApothecaryGameTests {
    static void withUsername(net.minecraft.server.level.ServerPlayer player, Runnable action) {
        // GameTestServer has no profile service; real Mek placement still needs the owner's name.
        try {
            var field = net.neoforged.neoforge.common.UsernameCache.class.getDeclaredField("map"); field.setAccessible(true);
            @SuppressWarnings("unchecked") var names = (Map<UUID, String>) field.get(null);
            String previous = names.put(player.getUUID(), player.getGameProfile().getName());
            try { action.run(); } finally { if (previous == null) names.remove(player.getUUID()); else names.put(player.getUUID(), previous); }
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }
    private static MechanicalApothecary machine(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos.below(), Blocks.STONE); h.setBlock(pos, ApothecaryContent.BLOCK.get());
        return (MechanicalApothecary) h.getBlockEntity(pos);
    }
    @GameTest(template = "empty", timeoutTicks = 450)
    public static void nativeFlowersUseHoppersCableWaterAndAutomaticOutput(GameTestHelper h) {
        BlockPos pos = new BlockPos(5, 2, 5);
        var tile = machine(h, pos);
        Direction outputSide = RelativeSide.RIGHT.getDirection(tile.getDirection()), powerSide = outputSide.getOpposite();
        h.setBlock(pos.relative(outputSide), Blocks.CHEST); var chest = (ChestBlockEntity) h.getBlockEntity(pos.relative(outputSide));
        h.setBlock(pos.above(), Blocks.HOPPER);
        ((HopperBlockEntity) h.getBlockEntity(pos.above())).setItem(0, new ItemStack(BotaniaItems.WHITE_MYSTICAL_PETAL, 8));
        h.setBlock(pos.south(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        ((HopperBlockEntity) h.getBlockEntity(pos.south())).setItem(0, new ItemStack(Items.WHEAT_SEEDS, 2));
        h.setBlock(pos.relative(powerSide, 2), MekanismBlocks.BASIC_ENERGY_CUBE.get());
        var cube = (TileEntityEnergyCube) h.getBlockEntity(pos.relative(powerSide, 2));
        long charge = EnergyUnit.FORGE_ENERGY.convertFrom(20000L);
        cube.getEnergyContainers(null).getFirst().setEnergy(charge);
        cube.getConfig().getConfig(TransmissionType.ENERGY).setDataType(DataType.OUTPUT, RelativeSide.fromDirections(cube.getDirection(), powerSide.getOpposite()));
        cube.getConfig().getConfig(TransmissionType.ENERGY).setEjecting(true);
        h.setBlock(pos.relative(powerSide), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tile.getBlockPos(), Direction.NORTH);
        check(fluid != null && fluid.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.SIMULATE) == 2000 && tile.water().isEmpty(), "Water simulation changed the tank");
        check(fluid.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Apothecary accepted lava");
        check(fluid.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) == 2000, "Water input capability missing");
        h.startSequence().thenWaitUntil(() -> {
            int made = 0; for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(BotaniaBlocks.PURE_DAISY.asItem())) made += chest.getItem(i).getCount();
            check(made == 2, "Native production: chest=" + made + ", status=" + tile.status() + ", inputs=" + tile.inputs.stream().mapToInt(s -> s.getCount()).sum()
                  + ", reagent=" + tile.reagent.getCount() + ", energy=" + tile.energy().getEnergy() + ", outputs=" + tile.outputs.stream().mapToInt(s -> s.getCount()).sum()
                  + ", right=" + RelativeSide.RIGHT.getDirection(tile.getDirection()));
        }).thenExecute(() -> {
            try {
                check(tile.inputs.stream().allMatch(s -> s.getStack().isEmpty()) && tile.reagent.getStack().isEmpty() && tile.water().isEmpty(), "Native recipe lost repeated-petal or water accounting");
                check(cube.getEnergyContainers(null).getFirst().getEnergy() < charge, "Real cable did not supply energy");
            } finally { cube.getEnergyContainers(null).getFirst().setEnergy(0); h.setBlock(pos, Blocks.AIR); }
        }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 450)
    public static void bionicRecipesAreExclusiveAtomicAndSurviveMachineDrops(GameTestHelper h) {
        var player = player(h, "mechanical-bionic"); BlockPos pos = new BlockPos(8, 2, 8); var tile = machine(h, pos);
        var recipes = h.getLevel().getRecipeManager();
        check(recipes.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "mechanical_apothecary")).isPresent(), "Machine crafting recipe failed to load");
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, "mana_lotus");
        var recipe = (MechanicalFlowerRecipe) recipes.byKey(id).orElseThrow().value();
        check(recipes.getAllRecipesFor(ApothecaryContent.RECIPE_TYPE.get()).size() == Content.bionics().length + 1, "Mechanical flower recipes failed to load");
        check(recipes.getAllRecipesFor(BotaniaRecipeTypes.PETAL_APOTHECARY_TYPE).stream().noneMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(Content.LOTUS.asItem())
              || r.value().getResultItem(h.getLevel().registryAccess()).is(Content.AMARANTHUS.asItem())), "Native basin can craft a bionic flower");
        List<ItemStack> nativeInput = new ArrayList<>();
        for (int i = 0; i < recipe.materials().size(); i++) {
            ItemStack material = recipe.materials().get(i).getItems()[0].copy(); nativeInput.add(material.copyWithCount(1));
            tile.inputs.get(i).setStack(material.copyWithCount(2));
        }
        check(recipes.getRecipeFor(BotaniaRecipeTypes.PETAL_APOTHECARY_TYPE, new ApothecaryWork.Input(nativeInput), h.getLevel()).isEmpty(), "Native basin matched bionic ingredients");
        tile.reagent.setStack(new ItemStack(Items.WHEAT_SEEDS));
        for (var output : tile.outputs) output.setStack(new ItemStack(Items.COBBLESTONE, 64));
        var power = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, tile.getBlockPos(), Direction.NORTH);
        check(power != null, "Mechanical FE input capability missing");
        int accepted = power.receiveEnergy(60000, false);
        check(accepted == 60000, "Mechanical FE accepted=" + accepted + ", canReceive=" + power.canReceive() + ", max=" + power.getMaxEnergyStored()
              + ", internalMax=" + tile.energy().getMaxEnergy());
        var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tile.getBlockPos(), Direction.NORTH);
        fluid.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        long initial = tile.energy().getEnergy();
        h.runAfterDelay(15, () -> {
            check(tile.status() == MechanicalApothecary.NO_REAGENT && tile.progressTicks() == 0 && tile.energy().getEnergy() == initial, "Wrong reagent consumed resources");
            tile.reagent.setStack(recipe.reagent().getItems()[0].copyWithCount(2));
        });
        h.runAfterDelay(30, () -> {
            check(tile.status() == MechanicalApothecary.OUTPUT_FULL && tile.progressTicks() == 0 && tile.water().getFluidAmount() == 1000
                  && tile.energy().getEnergy() == initial, "Blocked output consumed resources");
            tile.outputs.forEach(slot -> slot.setStack(ItemStack.EMPTY));
        });
        h.startSequence().thenWaitUntil(() -> check(tile.outputs.stream().anyMatch(s -> s.getStack().is(Content.LOTUS.asItem())), "Bionic recipe has not completed"))
              .thenExecute(() -> {
                  try {
                      check(tile.water().isEmpty() && tile.reagent.getCount() == 1 && tile.inputs.stream().mapToInt(s -> s.getCount()).sum() == recipe.materials().size(),
                            "Bionic crafting did not consume exactly one batch");
                      check(initial - tile.energy().getEnergy() == EnergyUnit.FORGE_ENERGY.convertFrom((long) recipe.ticks() * recipe.fePerTick()), "Bionic FE cost was not conserved");
                      fluid.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE);
                      tile.getConfig().getConfig(TransmissionType.ITEM).setDataType(DataType.EXTRA, RelativeSide.FRONT);
                      long energy = tile.energy().getEnergy();
                      var saved = (MechanicalApothecary) BlockEntity.loadStatic(tile.getBlockPos(), tile.getBlockState(), tile.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
                      check(saved != null && saved.water().getFluidAmount() == 5000 && saved.energy().getEnergy() == energy, "Machine world save lost resources");
                      ItemStack drop = breakAndPick(h, tile.getBlockPos(), ApothecaryContent.BLOCK.get());
                      var attached = drop.get(mekanism.common.registries.MekanismDataComponents.ATTACHED_ITEMS);
                      check(attached != null && attached.size() == 26, "Machine drop item layout is incomplete");
                      // Exercise the known pre-bucket-slot prototype layout as well.
                      drop.set(mekanism.common.registries.MekanismDataComponents.ATTACHED_ITEMS,
                            new mekanism.common.attachments.containers.item.AttachedItems(new ArrayList<>(attached.containers().subList(0, 24))));
                      withUsername(player, () -> placeItem(player, tile.getBlockPos(), drop)); var restored = (MechanicalApothecary) h.getBlockEntity(pos);
                      check(restored.water().getFluidAmount() == 5000 && restored.energy().getEnergy() == energy
                            && restored.reagent.getCount() == 1 && restored.outputs.stream().anyMatch(s -> s.getStack().is(Content.LOTUS.asItem()))
                            && restored.getConfig().getConfig(TransmissionType.ITEM).getDataType(RelativeSide.FRONT) == DataType.EXTRA,
                            "Machine drop lost water, energy, output, reagent or side settings");
                  } finally { h.setBlock(pos, Blocks.AIR); }
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void bucketSlotsAndRealFluidPipeShareTheSameTank(GameTestHelper h) {
        var player = player(h, "mechanical-water"); BlockPos pos = new BlockPos(8, 2, 8); var tile = machine(h, pos);
        var previousMenu = player.containerMenu; var previousPos = player.position(); var previousItem = player.getInventory().getItem(0).copy();
        try {
            player.setPos(tile.getBlockPos().getCenter()); var menu = new ApothecaryMenu(97, player.getInventory(), tile); player.containerMenu = menu;
            player.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
            var slot = menu.slots.stream().filter(s -> s.container == player.getInventory() && s.getItem().is(Items.WATER_BUCKET)).findFirst().orElseThrow();
            check(!menu.quickMoveStack(player, slot.index).isEmpty() && tile.waterInput.getStack().is(Items.WATER_BUCKET)
                  && tile.inputs.stream().allMatch(s -> s.getStack().isEmpty()) && tile.reagent.getStack().isEmpty(), "Shift-click did not route a water bucket to its fluid slot");
        } finally { player.getInventory().setItem(0, previousItem); player.containerMenu = previousMenu; player.setPos(previousPos); }
        h.setBlock(pos.above(), Blocks.HOPPER); var hopper = (HopperBlockEntity) h.getBlockEntity(pos.above());
        for (int i = 0; i < 3; i++) hopper.setItem(i, new ItemStack(Items.WATER_BUCKET));
        Direction out = RelativeSide.RIGHT.getDirection(tile.getDirection());
        h.setBlock(pos.relative(out), Blocks.CHEST); var chest = (ChestBlockEntity) h.getBlockEntity(pos.relative(out));
        int[] stage = {0}, remaining = {2000};
        h.onEachTick(() -> {
            if (stage[0] == 1 && remaining[0] > 0) {
                var pipe = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos.north()), Direction.SOUTH);
                if (pipe != null) remaining[0] -= pipe.fill(new FluidStack(Fluids.WATER, remaining[0]), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        h.startSequence().thenWaitUntil(() -> {
            int buckets = 0; for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.BUCKET)) buckets += chest.getItem(i).getCount();
            check(tile.water().getFluidAmount() == 4000 && buckets == 4, "Repeated water buckets did not fill 4000 mB and eject four empty buckets");
        }).thenExecute(() -> {
            h.setBlock(pos.north(), MekanismBlocks.BASIC_MECHANICAL_PIPE.get()); stage[0] = 1;
        }).thenWaitUntil(() -> check(tile.water().getFluidAmount() == 6000 && remaining[0] == 0, "Real mechanical pipe did not fill the shared water tank"))
              .thenExecute(() -> {
                  stage[0] = 2; h.setBlock(pos.north(), Blocks.AIR);
                  tile.getConfig().getConfig(TransmissionType.ITEM).setEjecting(false);
                  var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tile.getBlockPos(), Direction.NORTH);
                  check(fluid.fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE) == 10000, "Could not fill tank to its 16-bucket capacity");
                  tile.waterInput.setStack(new ItemStack(Items.WATER_BUCKET));
              }).thenIdle(10).thenExecute(() -> {
                  check(tile.water().getFluidAmount() == 16000 && tile.waterInput.getStack().is(Items.WATER_BUCKET) && tile.bucketOutput.getStack().isEmpty(), "Full tank consumed a water bucket");
                  tile.water().extract(1000, mekanism.api.Action.EXECUTE, mekanism.api.AutomationType.INTERNAL);
              }).thenWaitUntil(() -> check(tile.water().getFluidAmount() == 16000 && tile.waterInput.getStack().isEmpty() && tile.bucketOutput.getCount() == 1, "Bucket filling did not resume after tank space was freed"))
              .thenExecute(() -> {
                  tile.water().extract(2000, mekanism.api.Action.EXECUTE, mekanism.api.AutomationType.INTERNAL);
                  tile.bucketOutput.setStack(new ItemStack(Items.BUCKET, 16)); tile.waterInput.setStack(new ItemStack(Items.WATER_BUCKET));
              }).thenIdle(10).thenExecute(() -> {
                  check(tile.water().getFluidAmount() == 14000 && tile.waterInput.getStack().is(Items.WATER_BUCKET), "Blocked empty-bucket output consumed water or a bucket");
                  tile.bucketOutput.extractItem(16, mekanism.api.Action.EXECUTE, mekanism.api.AutomationType.MANUAL);
              }).thenWaitUntil(() -> check(tile.water().getFluidAmount() == 15000 && tile.waterInput.getStack().isEmpty() && tile.bucketOutput.getCount() == 1, "Bucket output unblocking did not resume filling"))
              .thenExecute(() -> {
                  try {
                      // Both appended slots must survive the same drop path as the other inventory.
                      tile.waterInput.setStack(new ItemStack(Items.WATER_BUCKET));
                      var drop = breakAndPick(h, tile.getBlockPos(), ApothecaryContent.BLOCK.get());
                      withUsername(player, () -> placeItem(player, tile.getBlockPos(), drop));
                      var restored = (MechanicalApothecary) h.getBlockEntity(pos);
                      check(restored.waterInput.getStack().is(Items.WATER_BUCKET) && restored.bucketOutput.getCount() == 1
                            && restored.water().getFluidAmount() == 15000, "Machine drop lost either bucket slot or tank water");
                  } finally { stage[0] = 2; h.setBlock(pos, Blocks.AIR); }
              }).thenSucceed();
    }
    @GameTest(template = "empty", timeoutTicks = 50)
    public static void directNetworkControlsAndPlacementRespectSavedSettings(GameTestHelper h) {
        var owner = player(h, "network-controls"); var stranger = player(h, "network-controls-stranger");
        BlockPos pos = new BlockPos(9, 2, 8); pool(h, pos.west(), 0); h.setBlock(pos.below(), Blocks.STONE);
        placeItem(owner, h.absolutePos(pos), new ItemStack(Content.NODE.get())); var node = (NetworkPlant) h.getBlockEntity(pos);
        check(node.direction() == Direction.WEST, "Fresh bud did not identify its unique adjacent pool");
        var core = core(h, owner, new BlockPos(16, 2, 8));
        action(owner, node, FlowerMenu.CONNECT_AS, core.network + ",0");
        check(core.network.equals(node.network) && node.mode == NetworkPlant.SUPPLY && node.direction() == Direction.WEST, "Connect-as did not apply network and mode together while retaining the pool direction");
        action(owner, node, FlowerMenu.SET_MODE, "0"); check(node.mode == NetworkPlant.SUPPLY, "Direct supply button was not applied");
        action(owner, node, FlowerMenu.SET_PRIORITY, "2"); check(node.priority == 1, "Supply bud accepted receive priority");
        action(owner, node, FlowerMenu.SET_MODE, "1"); action(owner, node, FlowerMenu.SET_PRIORITY, "2");
        action(owner, node, 3, "1"); action(owner, node, FlowerMenu.FILL_TARGET, "");
        check(node.priority == 2 && node.target == 1000000, "Direct priority/fill-target controls failed");
        var inaccessible = core(h, stranger, new BlockPos(24, 2, 8));
        action(owner, node, FlowerMenu.CONNECT_AS, inaccessible.network + ",2");
        check(core.network.equals(node.network) && node.mode == NetworkPlant.RECEIVE, "Denied connect-as partially changed the network or mode");
        action(owner, node, FlowerMenu.CONNECT_AS, core.network + ",99");
        check(core.network.equals(node.network) && node.mode == NetworkPlant.RECEIVE, "Invalid connect-as mode changed a node");
        var crowdedCore = core(h, owner, new BlockPos(32, 2, 8)); var crowded = ManaNetworks.get(h.getLevel()).find(crowdedCore.network);
        try {
            for (int i = 1; i <= Balance.NODE_LIMIT; i++) crowded.nodes.add(crowdedCore.getBlockPos().south(i));
            action(owner, node, FlowerMenu.CONNECT_AS, crowdedCore.network + ",2");
            check(core.network.equals(node.network) && node.mode == NetworkPlant.RECEIVE, "Full network partially applied connect-as");
        } finally { crowded.nodes.clear(); ManaNetworks.get(h.getLevel()).invalidate(); }
        var previousMenu = owner.containerMenu; var previousPosition = owner.position();
        try {
            owner.setPos(node.getBlockPos().getCenter()); var menu = new FlowerMenu(93, owner.getInventory(), node.getBlockPos()); owner.containerMenu = menu;
            for (int revision = 1; revision <= 2; revision++) {
                FlowerPackets.handleSettings(new FlowerPackets.Settings(93, FlowerMenu.FILL_TARGET, ""), context(owner));
                var acknowledged = menu.snapshot();
                check(acknowledged.getInt("settingsRevision") == revision && acknowledged.getInt("settingsAction") == FlowerMenu.FILL_TARGET
                      && acknowledged.getString("settingsValue").isEmpty() && acknowledged.getInt("limit") == 1000000,
                      "Unchanged pool-capacity requests did not acknowledge their confirmed value");
            }
        } finally { owner.containerMenu = previousMenu; owner.setPos(previousPosition); }
        action(stranger, node, FlowerMenu.SET_MODE, "2"); check(node.mode == NetworkPlant.RECEIVE, "Stranger changed node mode");
        action(owner, node, FlowerMenu.SET_DIRECTION, "99"); check(node.direction() == Direction.WEST, "Invalid direction packet changed a node");
        pool(h, pos.east(), 0); action(owner, node, FlowerMenu.DETECT_POOL, "");
        check(node.direction() == Direction.WEST, "Ambiguous detection replaced the selected pool");
        h.setBlock(pos.east(), Blocks.AIR); action(owner, node, FlowerMenu.SET_DIRECTION, "5");
        ItemStack item = breakAndPick(h, node.getBlockPos(), Content.NODE.get()); placeItem(owner, node.getBlockPos(), item);
        var restored = (NetworkPlant) h.getBlockEntity(pos);
        check(restored.direction() == Direction.EAST && restored.priority == 2 && core.network.equals(restored.network), "Auto-detection overwrote a saved bud's settings");
        action(owner, restored, 5, core.network.toString());
        var data = ManaNetworks.get(h.getLevel()); var network = data.find(core.network);
        network.members.put(stranger.getUUID(), "Member"); action(owner, core, FlowerMenu.REMOVE_MEMBER, stranger.getUUID().toString());
        check(!network.members.containsKey(stranger.getUUID()), "Explicit member removal failed");
        var snapshot = new FlowerMenu(90, owner.getInventory(), core.getBlockPos()).snapshot();
        check(snapshot.getList("connections", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 1, "Core did not expose its connection list");
        h.succeed();
    }
}
