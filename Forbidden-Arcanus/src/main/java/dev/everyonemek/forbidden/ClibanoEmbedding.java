package dev.everyonemek.forbidden;

import com.stal111.forbidden_arcanus.common.block.ModBlockPatterns;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoFrameBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import com.stal111.forbidden_arcanus.common.item.mundabitur.TransformPatternInteraction.TransformPatternContext;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import dev.everyonemek.forbidden.mixin.ClibanoAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** A wall replacement retains the original furnace entity, inventory and processing engine. */
public final class ClibanoEmbedding {
    private record Installation(Level level, BlockPos wall, BlockPos main) { }
    private static final ThreadLocal<Installation> INSTALLING = new ThreadLocal<>();

    public static boolean isController(BlockState state) {
        return state.getBlock() instanceof MachineBlock block && !block.kind.forge();
    }
    public static boolean isPart(BlockState state) { return isController(state) || state.getBlock() instanceof ClibanoPortBlock; }
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            allow(ModBlockPatterns.CLIBANO_COMBUSTION_BASE, 0, 1);
            allow(ModBlockPatterns.CLIBANO_COMBUSTION_BASE, 1, 0);
            allow(ModBlockPatterns.CLIBANO_COMBUSTION_BASE, 1, 2);
            var base = ModBlockPatterns.CLIBANO_COMBUSTION_BASE.getPattern();
            base[2][1][1] = base[2][1][1].or(block -> isController(block.getState()));
            for (int row : new int[] {0, 2}) base[1][row][1] = base[1][row][1].or(block -> block.getState().getBlock() instanceof ClibanoPortBlock);
            for (int[] cell : new int[][] {{0, 1}, {1, 0}, {1, 2}, {2, 1}})
                allow(ModBlockPatterns.CLIBANO_COMBUSTION, cell[0], cell[1]);
            var formed = ModBlockPatterns.CLIBANO_COMBUSTION.getPattern();
            for (int row : new int[] {0, 2}) formed[1][row][1] = formed[1][row][1].or(block -> block.getState().getBlock() instanceof ClibanoPortBlock);
        });
    }
    private static void allow(BlockPattern pattern, int depth, int x) {
        var cells = pattern.getPattern();
        cells[depth][1][x] = cells[depth][1][x].or(block -> isPart(block.getState()));
    }
    public static boolean controllerPosition(BlockPos wall, BlockPos center) {
        return wall.getY() == center.getY() && wall.distManhattan(center) == 1;
    }
    public static boolean validWalls(Level level, BlockPos center, Direction front) {
        int controllers = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!level.hasChunkAt(pos)) return false;
            if (isController(level.getBlockState(pos)) && (!controllerPosition(pos, center) || ++controllers > 1)) return false;
            if (level.getBlockState(pos).getBlock() instanceof ClibanoPortBlock
                  && (pos.distManhattan(center) != 1 || pos.equals(center.relative(front)))) return false;
        }
        return true;
    }
    public static boolean validBase(TransformPatternContext context) {
        Direction front = context.clickedFace();
        if (!front.getAxis().isHorizontal()) return false;
        BlockPos center = context.pos().relative(front.getOpposite());
        return validWalls(context.level(), center, front) && ModBlockPatterns.CLIBANO_COMBUSTION_BASE.matches(context.level(),
              center.relative(front.getOpposite()).relative(front.getCounterClockWise()).above(), front, Direction.UP) != null;
    }
    public static boolean validWalls(ClibanoMainBlockEntity main) {
        return validWalls(main.getLevel(), main.getBlockPos(), ((ClibanoAccess) main).forbiddenmekanism$front());
    }
    public static boolean belongs(Controller controller, ClibanoMainBlockEntity main) {
        return controllerPosition(controller.getBlockPos(), main.getBlockPos());
    }
    public static void autoConnect(Controller controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide || controller.kind().forge()) return;
        if (controller.binding.target != null && !controllerPosition(controller.getBlockPos(), controller.binding.target)) controller.binding.release();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var block = Binding.nativeAt(level, controller.getBlockPos().relative(direction));
            if (!(block instanceof ClibanoMainBlockEntity main) || !belongs(controller, main) || !Binding.structure(main)) continue;
            if (controller.binding.embedded && controller.binding.resolve() == main) return;
            var owner = controller.getSecurity().getOwnerUUID();
            if (owner != null) controller.binding.owner = owner;
            var actor = controller.binding.actor();
            if (actor != null && controller.binding.bind(actor, main.getBlockPos())) {
                if (!controller.binding.embedded) {
                    controller.binding.embedded = true;
                    Direction outward = Direction.fromDelta(controller.getBlockPos().getX() - main.getBlockPos().getX(), 0,
                          controller.getBlockPos().getZ() - main.getBlockPos().getZ());
                    controller.setFacing(outward);
                    controller.markForSave();
                }
            }
            return;
        }
    }
    public static boolean preserveMain(Level level, BlockPos wall, BlockPos main, BlockState replacement) {
        Installation install = INSTALLING.get();
        return install != null && install.level == level && install.wall.equals(wall)
              && install.main.equals(main) && isPart(replacement);
    }
    public static net.minecraft.world.InteractionResult openFromPart(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (level.isClientSide) return null;
        ClibanoMainBlockEntity main = Binding.nativeAt(level, pos) instanceof ClibanoMainBlockEntity furnace ? furnace : null;
        if (main == null && level.getBlockEntity(pos) instanceof ClibanoPort) {
            for (Direction side : Direction.values()) {
                var candidate = pos.relative(side);
                if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof ClibanoMainBlockEntity furnace) { main = furnace; break; }
            }
        }
        if (main == null) return null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            var candidate = main.getBlockPos().relative(side);
            if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof Controller controller && !controller.kind().forge()) {
                autoConnect(controller);
                return controller.openGui(player);
            }
        }
        return null;
    }
    public static void remove(Controller controller) {
        if (controller.kind().forge()) return;
        // A wall can be broken in the same tick as formation, before autoConnect first runs.
        controller.binding.release();
        removePart(controller.getLevel(), controller.getBlockPos());
    }
    public static void removePart(Level level, BlockPos wall) {
        for (Direction direction : Direction.values()) {
            BlockPos pos = wall.relative(direction);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ClibanoMainBlockEntity) {
                level.removeBlock(pos, false);
                return;
            }
        }
    }
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem item) || !isPart(item.getBlock().defaultBlockState())
              || !(event.getLevel().getBlockEntity(event.getPos()) instanceof ClibanoFrameBlockEntity)
              && !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.CLIBANO_CORE.get())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean success = install(player, event.getHand(), event.getHitVec());
        if (!success) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.forbiddenmekanism.install_wall"), true);
    }
    public static boolean install(ServerPlayer player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        Level level = player.level(); BlockPos wall = hit.getBlockPos(); ItemStack held = player.getItemInHand(hand);
        boolean controllerItem = held.is(Content.MACHINES.get(MachineKind.CLIBANO).asItem());
        if (!(held.getItem() instanceof BlockItem item) || !isPart(item.getBlock().defaultBlockState()) || !level.hasChunkAt(wall)
              || !player.mayInteract(level, wall) || !player.mayUseItemAt(wall, hit.getDirection(), held)
              || !player.canInteractWithBlock(wall, 0)) return false;
        BlockState original = level.getBlockState(wall), returned;
        ClibanoMainBlockEntity main = null;
        if (original.is(ModBlocks.CLIBANO_CORE.get())) {
            if (!controllerItem) return false;
            returned = original;
        } else {
            if (!(level.getBlockEntity(wall) instanceof ClibanoFrameBlockEntity frame)
                  || !(Binding.nativeAt(level, wall) instanceof ClibanoMainBlockEntity furnace)
                  || wall.distManhattan(furnace.getBlockPos()) != 1
                  || !controllerItem && wall.equals(furnace.getBlockPos().relative(((ClibanoAccess) furnace).forbiddenmekanism$front()))
                  || controllerItem && wall.getY() != furnace.getBlockPos().getY()
                  || !Binding.structure(furnace) || !furnace.canOpen(player) || !Binding.canModify(furnace, player)
                  || controllerItem && !Binding.available(furnace, null)) return false;
            if (controllerItem) for (Direction direction : Direction.Plane.HORIZONTAL)
                if (isController(level.getBlockState(furnace.getBlockPos().relative(direction)))) return false;
            main = furnace;
            returned = frame.getFrameData().replaceState();
        }
        BlockPlaceContext context = new BlockPlaceContext(player, hand, held, hit) {
            @Override public BlockPos getClickedPos() { return wall; }
            @Override public boolean canPlace() { return true; }
            @Override public boolean replacingClickedOnBlock() { return true; }
        };
        InteractionResult result;
        if (main != null) INSTALLING.set(new Installation(level, wall.immutable(), main.getBlockPos()));
        try { result = ((BlockItem) held.getItem()).place(context); }
        finally { INSTALLING.remove(); }
        if (!result.consumesAction() || !isPart(level.getBlockState(wall))) return false;
        // Placement applies Mek attachments and ownership before this automatic connection.
        if (level.getBlockEntity(wall) instanceof Controller controller) {
            controller.binding.embedded = false;
            autoConnect(controller);
        }
        if (!player.getAbilities().instabuild) {
            var refund = new ItemStack(returned.getBlock());
            if (!player.getInventory().add(refund)) player.drop(refund, false);
        }
        return true;
    }
    public static net.minecraft.world.ItemInteractionResult activate(Controller controller, net.minecraft.world.entity.player.Player player,
          net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        Level level = controller.getLevel();
        if (!player.mayBuild() || player.distanceToSqr(controller.getBlockPos().getCenter()) > 64
              || !mekanism.api.security.IBlockSecurityUtils.INSTANCE.canAccess(player, level, controller.getBlockPos(), controller)
              || !validBase(new TransformPatternContext(level, controller.getBlockPos(), hand, hit.getDirection())))
            return net.minecraft.world.ItemInteractionResult.FAIL;
        if (level.isClientSide) return net.minecraft.world.ItemInteractionResult.SUCCESS;
        var stack = player.getItemInHand(hand);
        var result = stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
        if (!result.consumesAction()) return net.minecraft.world.ItemInteractionResult.FAIL;
        autoConnect(controller);
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }
    private ClibanoEmbedding() { }
}
