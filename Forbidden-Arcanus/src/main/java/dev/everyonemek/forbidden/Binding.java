package dev.everyonemek.forbidden;

import com.mojang.authlib.GameProfile;
import com.stal111.forbidden_arcanus.common.block.ModBlockPatterns;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoFrameBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import java.util.*;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.registries.MekanismItems;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.valhelsia.valhelsia_core.api.common.block.entity.neoforge.ValhelsiaContainerBlockEntity;

/** No cached handler or chunk ticket can outlive the binding's identity and structure checks. */
public final class Binding {
    private static final String CLAIM = "forbiddenmekanism_claim", PENDING = "forbiddenmekanism_target";
    private final Controller controller;
    public UUID id = UUID.randomUUID(), owner, targetId;
    public BlockPos target;
    public String clientLabel = "";
    public boolean embedded;
    public Binding(Controller controller) { this.controller = controller; }
    public FakePlayer actor() {
        if (!(controller.getLevel() instanceof ServerLevel level) || owner == null) return null;
        var player = FakePlayerFactory.get(level, new GameProfile(owner, "[ForbiddenMek]"));
        player.setPos(controller.getBlockPos().getX() + .5, controller.getBlockPos().getY() + .5, controller.getBlockPos().getZ() + .5);
        return player;
    }
    public static ValhelsiaContainerBlockEntity<?> nativeAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockEntity block = level.getBlockEntity(pos);
        if (block instanceof ClibanoFrameBlockEntity frame) {
            var main = frame.getFrameData().mainPos();
            if (!level.hasChunkAt(main) || pos.distSqr(main) > 3) return null;
            block = level.getBlockEntity(main);
        }
        return block instanceof ClibanoMainBlockEntity
              ? (ValhelsiaContainerBlockEntity<?>) block : null;
    }
    public static boolean structure(ValhelsiaContainerBlockEntity<?> block) {
        Level level = block.getLevel(); BlockPos pos = block.getBlockPos();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (!level.hasChunkAt(pos.offset(x, 0, z))) return false;
        return block instanceof ClibanoMainBlockEntity main && ClibanoEmbedding.validWalls(main)
              && ModBlockPatterns.CLIBANO_COMBUSTION.matches(level, pos.offset(1, 1, -1), Direction.SOUTH, Direction.UP) != null;
    }
    public ValhelsiaContainerBlockEntity<?> resolve() {
        Level level = controller.getLevel();
        if (controller.kind().forge() || level == null || level.isClientSide || controller.isRemoved() || target == null || targetId == null || owner == null
              || controller.getBlockPos().distSqr(target) > 64 || !level.hasChunkAt(controller.getBlockPos())
              || level.getBlockEntity(controller.getBlockPos()) != controller) return null;
        var block = nativeAt(level, target);
        if (block == null || !block.getBlockPos().equals(target) || block.isRemoved()) return null;
        CompoundTag claim = block.getPersistentData().getCompound(CLAIM);
        if (!claim.hasUUID("target") || !targetId.equals(claim.getUUID("target")) || !claim.hasUUID("id")
              || !id.equals(claim.getUUID("id")) || claim.getLong("pos") != controller.getBlockPos().asLong()) return null;
        return (!embedded || block instanceof ClibanoMainBlockEntity main && ClibanoEmbedding.belongs(controller, main))
              && structure(block) && block.canOpen(actor()) ? block : null;
    }
    public boolean bind(ServerPlayer player, BlockPos pos) {
        if (controller.kind().forge() || controller.getBlockPos().distSqr(pos) > 64) return false;
        var block = nativeAt(player.level(), pos);
        if (embedded && (!(block instanceof ClibanoMainBlockEntity main) || !ClibanoEmbedding.belongs(controller, main))) return false;
        if (block == null || !structure(block) || !block.canOpen(player)) { controller.status = Controller.STRUCTURE; return false; }
        CompoundTag old = block.getPersistentData().getCompound(CLAIM);
        if (!available(block, controller)) { controller.status = Controller.OCCUPIED; return false; }
        release();
        target = block.getBlockPos(); owner = player.getUUID();
        targetId = old.hasUUID("target") ? old.getUUID("target") : UUID.randomUUID();
        var claim = new CompoundTag(); claim.putUUID("id", id); claim.putUUID("target", targetId);
        claim.putLong("pos", controller.getBlockPos().asLong());
        block.getPersistentData().put(CLAIM, claim); block.setChanged(); controller.markForSave();
        controller.status = Controller.IDLE;
        return true;
    }
    public void release() {
        if (target != null && controller.getLevel() != null && controller.getLevel().hasChunkAt(target)) {
            var block = nativeAt(controller.getLevel(), target);
            if (block != null) {
                var claim = block.getPersistentData().getCompound(CLAIM);
                if (claim.hasUUID("id") && id.equals(claim.getUUID("id"))) {
                    claim.remove("id"); block.setChanged();
                }
            }
        }
        target = null; targetId = null;
    }
    public static boolean available(ValhelsiaContainerBlockEntity<?> block, Controller claimant) {
        var claim = block.getPersistentData().getCompound(CLAIM);
        if (!claim.hasUUID("id")) return true;
        BlockPos previous = BlockPos.of(claim.getLong("pos"));
        return block.getLevel().hasChunkAt(previous) && (!(block.getLevel().getBlockEntity(previous) instanceof Controller other)
              || other == claimant || !other.binding.id.equals(claim.getUUID("id")));
    }
    public static boolean canModify(ValhelsiaContainerBlockEntity<?> block, ServerPlayer player) {
        var claim = block.getPersistentData().getCompound(CLAIM);
        if (!claim.hasUUID("id")) return true;
        BlockPos previous = BlockPos.of(claim.getLong("pos"));
        return block.getLevel().hasChunkAt(previous) && (!(block.getLevel().getBlockEntity(previous) instanceof Controller other)
              || !other.binding.id.equals(claim.getUUID("id")) || IBlockSecurityUtils.INSTANCE.canAccess(player, block.getLevel(), previous, other));
    }
    public boolean bindNearby(ServerPlayer player) {
        if (controller.kind().forge()) return false;
        if (embedded) { ClibanoEmbedding.autoConnect(controller); return resolve() != null; }
        var found = new LinkedHashMap<BlockPos, ValhelsiaContainerBlockEntity<?>>();
        BlockPos pos = controller.getBlockPos();
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-8, -8, -8), pos.offset(8, 8, 8))) {
            if (candidate.distSqr(pos) > 64) continue;
            var block = nativeAt(player.level(), candidate);
            if (block != null) found.put(block.getBlockPos(), block);
        }
        if (found.size() == 1) return bind(player, found.keySet().iterator().next());
        player.displayClientMessage(Component.translatable("gui.forbiddenmekanism.bind_hint"), true);
        return false;
    }
    public String label() { return controller.getLevel() != null && controller.getLevel().isClientSide ? clientLabel : target == null ? "" : target.toShortString(); }
    public void save(CompoundTag tag) {
        tag.putBoolean("embedded", embedded);
        tag.putUUID("controller_id", id);
        if (owner != null) tag.putUUID("owner", owner);
        if (target != null && targetId != null) { tag.putLong("target_pos", target.asLong()); tag.putUUID("target_id", targetId); }
    }
    public void load(CompoundTag tag) {
        embedded = tag.getBoolean("embedded");
        if (tag.hasUUID("controller_id")) id = tag.getUUID("controller_id");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        targetId = tag.hasUUID("target_id") ? tag.getUUID("target_id") : null;
        target = targetId == null ? null : BlockPos.of(tag.getLong("target_pos"));
    }
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown() || !event.getItemStack().is(MekanismItems.CONFIGURATOR)) return;
        var nativeBlock = nativeAt(player.level(), event.getPos());
        if (nativeBlock != null && nativeBlock.canOpen(player)) {
            var tag = new CompoundTag(); tag.putLong("pos", nativeBlock.getBlockPos().asLong());
            tag.putString("dimension", player.level().dimension().location().toString());
            player.getPersistentData().put(PENDING, tag);
            player.displayClientMessage(Component.translatable("gui.forbiddenmekanism.target_selected"), true);
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        } else if (player.level().getBlockEntity(event.getPos()) instanceof Controller controller
              && !controller.kind().forge()
              && IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), event.getPos(), controller)
              && player.getPersistentData().contains(PENDING)) {
            var tag = player.getPersistentData().getCompound(PENDING);
            if (tag.getString("dimension").equals(player.level().dimension().location().toString())
                  && controller.binding.bind(player, BlockPos.of(tag.getLong("pos")))) {
                player.getPersistentData().remove(PENDING);
                player.displayClientMessage(Component.translatable("gui.forbiddenmekanism.bound"), true);
            }
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
