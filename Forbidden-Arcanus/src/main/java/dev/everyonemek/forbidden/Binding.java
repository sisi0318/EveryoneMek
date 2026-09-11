package dev.everyonemek.forbidden;

import com.mojang.authlib.GameProfile;
import com.stal111.forbidden_arcanus.common.block.ModBlockPatterns;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoFrameBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.clibano.ClibanoMainBlockEntity;
import java.util.*;
import mekanism.api.security.IBlockSecurityUtils;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.valhelsia.valhelsia_core.api.common.block.entity.neoforge.ValhelsiaContainerBlockEntity;

/** No cached handler or chunk ticket can outlive the binding's identity and structure checks. */
public final class Binding {
    private static final String CLAIM = "forbiddenmekanism_claim";
    private final Controller controller;
    public UUID id = UUID.randomUUID(), owner, targetId;
    public BlockPos target;
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
              || !ClibanoEmbedding.controllerPosition(controller.getBlockPos(), target) || !level.hasChunkAt(controller.getBlockPos())
              || level.getBlockEntity(controller.getBlockPos()) != controller) return null;
        var block = nativeAt(level, target);
        if (block == null || !block.getBlockPos().equals(target) || block.isRemoved()) return null;
        CompoundTag claim = claim(block);
        if (!claim.hasUUID("target") || !targetId.equals(claim.getUUID("target")) || !claim.hasUUID("id")
              || !id.equals(claim.getUUID("id")) || claim.getLong("pos") != controller.getBlockPos().asLong()) return null;
        return block instanceof ClibanoMainBlockEntity main && ClibanoEmbedding.belongs(controller, main)
              && structure(block) && block.canOpen(actor()) ? block : null;
    }
    public boolean bind(ServerPlayer player, BlockPos pos) {
        if (controller.kind().forge()) return false;
        var block = nativeAt(player.level(), pos);
        if (!(block instanceof ClibanoMainBlockEntity main) || !ClibanoEmbedding.belongs(controller, main)) return false;
        if (block == null || !structure(block) || !block.canOpen(player)) { controller.status = Controller.STRUCTURE; return false; }
        CompoundTag old = claim(block);
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
                var claim = claim(block);
                if (claim.hasUUID("id") && id.equals(claim.getUUID("id"))) {
                    claim.remove("id"); block.setChanged();
                }
            }
        }
        target = null; targetId = null;
    }
    public static boolean available(ValhelsiaContainerBlockEntity<?> block, Controller claimant) {
        var claim = claim(block);
        if (!claim.hasUUID("id")) return true;
        BlockPos previous = BlockPos.of(claim.getLong("pos"));
        return block.getLevel().hasChunkAt(previous) && (!(block.getLevel().getBlockEntity(previous) instanceof Controller other)
              || other == claimant || !other.binding.id.equals(claim.getUUID("id")));
    }
    public static boolean canModify(ValhelsiaContainerBlockEntity<?> block, ServerPlayer player) {
        var claim = claim(block);
        if (!claim.hasUUID("id")) return true;
        BlockPos previous = BlockPos.of(claim.getLong("pos"));
        return block.getLevel().hasChunkAt(previous) && (!(block.getLevel().getBlockEntity(previous) instanceof Controller other)
              || !other.binding.id.equals(claim.getUUID("id")) || IBlockSecurityUtils.INSTANCE.canAccess(player, block.getLevel(), previous, other));
    }
    private static CompoundTag claim(ValhelsiaContainerBlockEntity<?> block) {
        var claim = block.getPersistentData().getCompound(CLAIM);
        if (claim.hasUUID("id") && !ClibanoEmbedding.controllerPosition(BlockPos.of(claim.getLong("pos")), block.getBlockPos())) {
            claim.remove("id"); block.setChanged();
        }
        return claim;
    }
    public static boolean hasClaim(ClibanoMainBlockEntity main) { return claim(main).hasUUID("id"); }
    public static Controller claimedController(ClibanoMainBlockEntity main) {
        var claim = claim(main);
        if (!claim.hasUUID("id")) return null;
        var pos = BlockPos.of(claim.getLong("pos")); var level = main.getLevel();
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Controller controller
              && controller.binding.id.equals(claim.getUUID("id")) && controller.binding.resolve() == main ? controller : null;
    }
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
}
