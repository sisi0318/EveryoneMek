package dev.everyonemek.botania;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;
import vazkii.botania.common.block.block_entity.mana.ManaSpreaderBlockEntity;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.WandOfTheForestItem;
import vazkii.botania.common.lib.BotaniaTags;

@GameTestHolder(BotanicalMekanism.ID)
@PrefixGameTestTemplate(false)
public final class BotanicalGameTests {
    static void check(boolean value, String message) { if (!value) throw new GameTestAssertException(message); }
    static ServerPlayer player(GameTestHelper h, String test) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(test.getBytes(StandardCharsets.UTF_8)), "[BotanicalTest]"));
    }
    static BlockEntity plant(GameTestHelper h, ServerPlayer player, BlockPos relative, Block block) {
        h.setBlock(relative.below(), Blocks.STONE); h.setBlock(relative, block);
        BlockEntity tile = h.getBlockEntity(relative); Flowers.claim(tile, player);
        check(tile.getBlockState().canSurvive(h.getLevel(), tile.getBlockPos()), "Plant rejected stone support");
        return tile;
    }
    static ManaPoolBlockEntity pool(GameTestHelper h, BlockPos relative, int mana) {
        h.setBlock(relative.below(), Blocks.STONE); h.setBlock(relative, BotaniaBlocks.MANA_POOL);
        var pool = (ManaPoolBlockEntity) h.getBlockEntity(relative); pool.receiveMana(mana); return pool;
    }
    static IPayloadContext context(ServerPlayer player) {
        return (IPayloadContext) java.lang.reflect.Proxy.newProxyInstance(BotanicalGameTests.class.getClassLoader(), new Class[]{IPayloadContext.class},
              (proxy, method, args) -> {
                  if (method.getName().equals("player")) return player;
                  if (method.getName().equals("enqueueWork")) { ((Runnable) args[0]).run(); return CompletableFuture.completedFuture(null); }
                  throw new UnsupportedOperationException(method.getName());
              });
    }
    static void action(ServerPlayer player, BlockEntity tile, int action, String value) {
        var previous = player.containerMenu; var position = player.position();
        try {
            player.setPos(tile.getBlockPos().getCenter());
            var menu = new FlowerMenu(73, player.getInventory(), tile.getBlockPos()); player.containerMenu = menu;
            FlowerPackets.handleSettings(new FlowerPackets.Settings(menu.containerId, action, value), context(player));
        } finally { player.containerMenu = previous; player.setPos(position); }
    }
    static NetworkPlant node(GameTestHelper h, ServerPlayer player, BlockPos position, NetworkPlant core, int mode, Direction direction, int limit) {
        var node = (NetworkPlant) plant(h, player, position, Content.NODE.get());
        for (int i = 0; i < 3 && node.mode != mode; i++) action(player, node, 1, "");
        for (int i = 0; i < 6 && node.direction() != direction; i++) action(player, node, 2, "");
        if (mode != NetworkPlant.RELAY) action(player, node, 3, Integer.toString(limit));
        action(player, node, 5, core.network.toString());
        check(core.network.equals(node.network) && node.mode == mode && node.direction() == direction, "Real settings packet did not configure node");
        return node;
    }
    static NetworkPlant core(GameTestHelper h, ServerPlayer player, BlockPos position) {
        var core = (NetworkPlant) plant(h, player, position, Content.CORE.get());
        check(ManaNetworks.get(h.getLevel()).activate(core), "Core did not create network"); return core;
    }
    static ItemStack breakAndPick(GameTestHelper h, BlockPos pos, Block block) {
        h.getLevel().destroyBlock(pos, true);
        var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        var item = items.stream().map(ItemEntity::getItem).filter(stack -> stack.is(block.asItem())).findFirst().orElseThrow(() -> new GameTestAssertException("Flower drop missing"));
        ItemStack copy = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) item.save(h.getLevel().registryAccess()));
        items.forEach(ItemEntity::discard); return copy;
    }
    static void placeItem(ServerPlayer player, BlockPos pos, ItemStack stack) {
        var hand = player.getMainHandItem().copy(); var position = player.position();
        try {
            player.setPos(pos.above(2).getCenter()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
            check(((BlockItem) stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit)).consumesAction(), "Flower item could not be placed");
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, hand); player.setPos(position); }
    }
    static void stop(BlockEntity tile) { Flowers.data(tile).putBoolean("paused", true); }
    static void checkWorldSave(GameTestHelper h, BlockEntity flower) {
        var provider = h.getLevel().registryAccess();
        var restored = BlockEntity.loadStatic(flower.getBlockPos(), flower.getBlockState(), flower.saveWithFullMetadata(provider), provider);
        check(restored != null && Objects.equals(Flowers.owner(restored), Flowers.owner(flower))
              && Flowers.storedFE(restored) == Flowers.storedFE(flower) && Flowers.enabled(restored) == Flowers.enabled(flower),
              "World save lost bionic owner, FE or pause state");
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void realCablePowersNonSoilLotusAndNativeSpreader(GameTestHelper h) {
        ServerPlayer player = player(h, "lotus");
        BlockPos pos = new BlockPos(5, 2, 5);
        var lotus = (ManaLotus) plant(h, player, pos, Content.LOTUS.get());
        h.setBlock(new BlockPos(15, 1, 15), BotaniaBlocks.RED_STRINGED_SPOOFER);
        check(!lotus.getBlockState().canSurvive(h.getLevel(), h.absolutePos(new BlockPos(15, 2, 15))), "Bionic support allowed remote-position spoofing");
        for (String recipe : List.of("mana_lotus", "bionic_amaranthus", "resonance_flower", "resonance_bud"))
            check(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BotanicalMekanism.ID, recipe)).isPresent(), "Prototype crafting recipe missing: " + recipe);
        check(Content.LOTUS.get().useItemOn(new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), lotus.getBlockState(), h.getLevel(), lotus.getBlockPos(),
              player, InteractionHand.MAIN_HAND, new BlockHitResult(lotus.getBlockPos().getCenter(), Direction.UP, lotus.getBlockPos(), false))
              == net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, "Flower menu intercepted the native wand");
        h.setBlock(pos.below(), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        check(lotus.getBlockState().canSurvive(h.getLevel(), lotus.getBlockPos()), "Lotus could not stand on real Mek cable");
        h.setBlock(new BlockPos(7, 2, 5), BotaniaBlocks.MANA_SPREADER);
        var spreader = (ManaSpreaderBlockEntity) h.getBlockEntity(new BlockPos(7, 2, 5));
        var pool = pool(h, new BlockPos(10, 2, 5), 0);
        check(spreader.bindTo(player, new ItemStack(BotaniaItems.WAND_OF_THE_FOREST), pool.getBlockPos(), Direction.UP), "Spreader aiming failed");
        // First reproduce the automatic link, then use the actual wand to select and rebind.
        BlockPos otherPos = pos.north(2);
        h.setBlock(otherPos, BotaniaBlocks.MANA_SPREADER);
        lotus.tickFlower();
        check(lotus.getBindingPos() != null, "Lotus failed to auto-bind");
        var replica = new ManaLotus(lotus.getBlockPos(), lotus.getBlockState());
        replica.handleUpdateTag(lotus.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        var wand = new ItemStack(BotaniaItems.WAND_OF_THE_FOREST);
        var stranger = player(h, "lotus-stranger");
        check(replica.canSelect(player, wand, Direction.UP), "Update tag omitted ownership required by client wand selection");
        check(!replica.canSelect(stranger, wand, Direction.UP), "Client update lost the owner restriction");
        var oldHand = player.getMainHandItem(); boolean oldShift = player.isShiftKeyDown();
        try {
            WandOfTheForestItem.setBindMode(wand, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand); player.setShiftKeyDown(true);
            var wandItem = (WandOfTheForestItem) wand.getItem();
            for (BlockPos target : List.of(h.absolutePos(otherPos), spreader.getBlockPos())) {
                var select = new BlockHitResult(lotus.getBlockPos().getCenter(), Direction.UP, lotus.getBlockPos(), false);
                check(wandItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, select)).consumesAction()
                      && WandOfTheForestItem.getBindingAttempt(wand).map(p -> p.pos().equals(lotus.getBlockPos())).orElse(false),
                      "Sneak-use did not select the lotus in the real wand");
                var hit = new BlockHitResult(target.getCenter(), Direction.UP, target, false);
                check(wandItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                      && target.equals(lotus.getBindingPos()) && WandOfTheForestItem.getBindingAttempt(wand).isEmpty(),
                      "Wand did not complete/reassign the lotus binding");
            }
            check(!lotus.bindTo(stranger, wand, h.absolutePos(otherPos), Direction.UP)
                  && spreader.getBlockPos().equals(lotus.getBindingPos()), "Other player changed a private binding");
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, oldHand); player.setShiftKeyDown(oldShift); h.setBlock(otherPos, Blocks.AIR); }
        // The preliminary auto-binding tick already consumed this tick's generation budget.
        lotus.restoreProductionTick(Long.MIN_VALUE);
        var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, lotus.getBlockPos(), Direction.DOWN);
        CompoundTag previous = lotus.getPersistentData().copy();
        check(energy != null && energy.receiveEnergy(1000, true) == 1000 && previous.equals(lotus.getPersistentData()), "FE simulation mutated flower data");
        check(energy.receiveEnergy(1000, false) == 1000, "Lotus rejected FE");
        lotus.tickFlower(); lotus.tickFlower();
        check(Flowers.storedFE(lotus) == 800 && lotus.getMana() + spreader.getCurrentMana() == 4, "Repeated tick changed FE/mana rate");
        action(player, lotus, 0, ""); lotus.tickFlower();
        check(Flowers.storedFE(lotus) == 800, "Paused lotus consumed FE"); action(player, lotus, 0, "");
        h.setBlock(new BlockPos(2, 1, 5), MekanismBlocks.BASIC_ENERGY_CUBE.get());
        var cube = (TileEntityEnergyCube) h.getBlockEntity(new BlockPos(2, 1, 5));
        cube.getEnergyContainers(null).getFirst().setEnergy(1_000_000);
        cube.getConfig().getConfig(TransmissionType.ENERGY).setDataType(DataType.OUTPUT, RelativeSide.fromDirections(cube.getDirection(), Direction.EAST));
        cube.getConfig().getConfig(TransmissionType.ENERGY).setEjecting(true);
        h.setBlock(new BlockPos(3, 1, 5), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.setBlock(new BlockPos(4, 1, 5), MekanismBlocks.BASIC_UNIVERSAL_CABLE.get());
        h.runAfterDelay(110, () -> {
            try {
                check(pool.getCurrentMana() > 0 && Flowers.storedFE(lotus) > 0, "Cable → lotus → native spreader → pool did not operate");
                check(cube.getEnergyContainers(null).getFirst().getEnergy() < 1_000_000, "Cube supplied no real energy");
                stop(lotus); int mana = lotus.getMana(), stored = Flowers.storedFE(lotus); long tick = lotus.lastProductionTick();
                checkWorldSave(h, lotus);
                ItemStack drop = breakAndPick(h, lotus.getBlockPos(), Content.LOTUS.get());
                h.setBlock(pos.below(), Blocks.GLASS); placeItem(player, lotus.getBlockPos(), drop);
                var restored = (ManaLotus) h.getBlockEntity(pos);
                check(Flowers.storedFE(restored) == stored && restored.getMana() == mana && restored.lastProductionTick() == tick,
                      "Lotus drop lost FE, mana or production tick");
                check(energy.receiveEnergy(100, false) == 0, "Old FE handler still accepted power after replacement");
                h.succeed();
            } finally { cube.getEnergyContainers(null).getFirst().setEnergy(0); h.setBlock(pos, Blocks.AIR); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void bionicFlowerUsesFEAndKeepsNativePoolAndItemState(GameTestHelper h) {
        var player = player(h, "amaranthus"); BlockPos pos = new BlockPos(10, 2, 10);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) h.setBlock(pos.offset(x, -1, z), Blocks.GRASS_BLOCK);
        var flower = (FunctionalFlowerBlockEntity) plant(h, player, pos, Content.AMARANTHUS.get());
        var pool = pool(h, pos.east(6), 10_000);
        h.setBlock(pos.west(6), BotaniaBlocks.JADED_AMARANTHUS);
        check(h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos.west(6)), Direction.UP) == null, "Ordinary flower received the bionic FE capability");
        h.setBlock(pos.west(6), Blocks.AIR);
        var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, flower.getBlockPos(), Direction.UP);
        check(energy != null && energy.receiveEnergy(5000, false) == 5000, "Bionic flower rejected FE");
        h.startSequence().thenWaitUntil(() -> {
            boolean produced = false;
            for (BlockPos p : BlockPos.betweenClosed(flower.getBlockPos().offset(-4, 0, -4), flower.getBlockPos().offset(4, 0, 4)))
                if (h.getLevel().getBlockState(p).is(BotaniaTags.Blocks.SMALL_MYSTICAL_FLOWERS)) produced = true;
            check(produced, "Bionic flower has not grown a mystical flower");
        }).thenExecute(() -> {
            try {
                check(pool.getCurrentMana() == 10_000 && Flowers.storedFE(flower) == 0 && flower.getMana() == 0, "Bionic work consumed native pool mana or charged incorrectly");
                energy.receiveEnergy(7500, false); Flowers.supplyAmaranthus(flower); stop(flower);
                checkWorldSave(h, flower);
                int stored = Flowers.storedFE(flower), mana = flower.getMana();
                ItemStack drop = breakAndPick(h, flower.getBlockPos(), Content.AMARANTHUS.get());
                placeItem(player, flower.getBlockPos(), drop);
                var restored = (FunctionalFlowerBlockEntity) h.getBlockEntity(pos);
                check(Flowers.storedFE(restored) == stored && restored.getMana() == mana && !Flowers.enabled(restored), "Bionic item lost paid work storage or pause setting");
            } finally { h.setBlock(pos, Blocks.AIR); }
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void settingsPacketsTransferManaWithFeesAndRejectOtherPlayers(GameTestHelper h) {
        var player = player(h, "wireless"); var core = core(h, player, new BlockPos(20, 2, 20));
        var source = pool(h, new BlockPos(7, 2, 20), 1000); var target = pool(h, new BlockPos(33, 2, 20), 0);
        node(h, player, new BlockPos(8, 2, 20), core, NetworkPlant.SUPPLY, Direction.WEST, 200);
        var receiver = node(h, player, new BlockPos(32, 2, 20), core, NetworkPlant.RECEIVE, Direction.EAST, 100);
        var stranger = player(h, "stranger"); var other = (NetworkPlant) plant(h, stranger, new BlockPos(22, 2, 24), Content.NODE.get());
        action(stranger, other, 5, core.network.toString());
        check(other.network == null, "Unauthorized packet joined private network");
        other.network = core.network;
        check(!new FlowerMenu(84, stranger.getInventory(), other.getBlockPos()).snapshot().contains("name"), "Stale unauthorized node exposed private network metadata");
        other.network = null;
        action(stranger, receiver, 3, "9999"); check(receiver.target == 100, "Another player changed the receiver limit");
        action(player, receiver, 3, "-1"); check(receiver.target == 100, "Negative receiver target accepted");
        h.runAfterDelay(15, () -> {
            try {
                check(target.getCurrentMana() == 100 && source.getCurrentMana() == 896, "Two-hop transfer lost mana or used incorrect fee: " + source.getCurrentMana() + "/" + target.getCurrentMana());
                var data = ManaNetworks.get(h.getLevel());
                var loaded = ManaNetworks.load(data.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
                check(loaded.find(core.network).credit == data.find(core.network).credit && loaded.find(core.network).owner.equals(player.getUUID()), "Network persistence changed credit or owner");
                action(player, core, 0, ""); action(player, receiver, 3, "200");
                h.runAfterDelay(10, () -> {
                    try { check(target.getCurrentMana() == 100 && source.getCurrentMana() == 896, "Paused core still transferred mana"); h.succeed(); }
                    finally { stop(core); }
                });
            } catch (RuntimeException error) { stop(core); throw error; }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void relayStopsAfterBreakAndRestoresFromItsDrop(GameTestHelper h) {
        var player = player(h, "relay"); var core = core(h, player, new BlockPos(6, 2, 6));
        var source = pool(h, new BlockPos(8, 2, 9), 5000); var target = pool(h, new BlockPos(63, 2, 6), 0);
        node(h, player, new BlockPos(7, 2, 9), core, NetworkPlant.SUPPLY, Direction.EAST, 0);
        var relay = node(h, player, new BlockPos(34, 2, 6), core, NetworkPlant.RELAY, Direction.DOWN, 0);
        var receiver = node(h, player, new BlockPos(62, 2, 6), core, NetworkPlant.RECEIVE, Direction.EAST, 100);
        h.runAfterDelay(15, () -> {
            check(target.getCurrentMana() == 100 && source.getCurrentMana() == 4894, "Three-hop route or fee failed");
            ItemStack dropped = breakAndPick(h, relay.getBlockPos(), Content.NODE.get());
            action(player, receiver, 3, "200");
            var duplicate = (NetworkPlant) plant(h, player, new BlockPos(6, 2, 9), Content.CORE.get()); duplicate.network = core.network;
            check(!ManaNetworks.get(h.getLevel()).activate(duplicate), "Duplicate core activated the same network");
            h.runAfterDelay(10, () -> {
                check(target.getCurrentMana() == 100 && source.getCurrentMana() == 4894, "Broken relay route kept transferring");
                placeItem(player, relay.getBlockPos(), dropped);
                h.runAfterDelay(15, () -> {
                    try {
                        check(target.getCurrentMana() == 200 && source.getCurrentMana() == 4788, "Replaced relay did not restore its route and fee"); h.succeed();
                    } finally { stop(core); stop(duplicate); }
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void sharedBudgetsAndPriorityServeAllConsumersOncePerBatch(GameTestHelper h) {
        var player = player(h, "bandwidth"); var core = core(h, player, new BlockPos(20, 2, 20));
        var a = pool(h, new BlockPos(9, 2, 10), 100_000); var b = pool(h, new BlockPos(9, 2, 30), 100_000);
        node(h, player, new BlockPos(10, 2, 10), core, NetworkPlant.SUPPLY, Direction.WEST, 0);
        node(h, player, new BlockPos(10, 2, 30), core, NetworkPlant.SUPPLY, Direction.WEST, 0);
        var pools = new ArrayList<ManaPoolBlockEntity>();
        for (int i = 0; i < 3; i++) {
            pools.add(pool(h, new BlockPos(31, 2, 10 + i * 10), 0));
            var node = node(h, player, new BlockPos(30, 2, 10 + i * 10), core, NetworkPlant.RECEIVE, Direction.EAST, 1_000_000);
            for (int j = 0; j < 3 && node.priority != 2 - i; j++) action(player, node, 4, "");
        }
        try {
            core.tick(); int delivered = pools.stream().mapToInt(ManaPoolBlockEntity::getCurrentMana).sum();
            check(delivered == 640 && pools.stream().allMatch(p -> p.getCurrentMana() <= 320), "Network or endpoint batch budget incorrect");
            check(pools.get(0).getCurrentMana() > pools.get(1).getCurrentMana() && pools.get(1).getCurrentMana() > pools.get(2).getCurrentMana()
                  && pools.get(2).getCurrentMana() > 0, "Weighted scheduling starved or reordered consumers");
            core.tick(); check(pools.stream().mapToInt(ManaPoolBlockEntity::getCurrentMana).sum() == delivered, "Repeated core tick repeated the batch");
            check(200_000 - a.getCurrentMana() - b.getCurrentMana() == delivered + 26, "Batch fee was not conserved");
            h.succeed();
        } finally { stop(core); }
    }
}
