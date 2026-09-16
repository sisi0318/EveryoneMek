package dev.everyonemek.overloadcore;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

public final class WardCommands {
    private static BlockEntity target(ServerPlayer p) {
        var hit = p.pick(8,0,false);
        return hit instanceof BlockHitResult block && p.level().hasChunkAt(block.getBlockPos()) ? p.level().getBlockEntity(block.getBlockPos()) : null;
    }
    private static int result(CommandSourceStack source, boolean ok) {
        if (ok) source.sendSuccess(() -> CoreContent.text("ward.updated"), false);
        else source.sendFailure(CoreContent.text("ward.update_failed"));
        return ok ? 1 : 0;
    }
    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        var root = Commands.literal("ward")
              .then(Commands.literal("status").executes(c -> { WardRuntime.report(c.getSource().getPlayerOrException(),c.getSource()); return 1; }))
              .then(Commands.literal("extreme").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c ->
                    result(c.getSource(),WardRuntime.setExtreme(c.getSource().getPlayerOrException(),BoolArgumentType.getBool(c,"enabled"))))))
              .then(Commands.literal("source").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c -> {
                  var p=c.getSource().getPlayerOrException(); return result(c.getSource(),WardSources.enabled(p,target(p),BoolArgumentType.getBool(c,"enabled")));
              })))
              .then(Commands.literal("reserve").then(Commands.argument("percent",IntegerArgumentType.integer(0,100)).executes(c -> {
                  var p=c.getSource().getPlayerOrException(); return result(c.getSource(),WardSources.reserve(p,target(p),IntegerArgumentType.getInteger(c,"percent")));
              })))
              .then(Commands.literal("inspect").requires(s -> s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).executes(c -> {
                  WardRuntime.report(EntityArgument.getPlayer(c,"player"),c.getSource()); return 1;
              })));
        for (boolean repair : new boolean[]{true,false}) root.then(Commands.literal(repair?"repair":"release").requires(s -> s.hasPermission(2))
              .then(Commands.argument("player",EntityArgument.player()).executes(c -> {
                  var p=EntityArgument.getPlayer(c,"player"); boolean ok=repair?WardCustody.repair(p):WardCustody.release(p);
                  WardRuntime.sync(p); c.getSource().sendSuccess(() -> CoreContent.text(ok?"ward.admin_ok":"ward.admin_failed",p.getDisplayName()),true);
                  return ok?1:0;
              })));
        for (boolean allow : new boolean[]{true,false}) root.then(Commands.literal(allow?"share":"unshare")
              .then(Commands.argument("player",EntityArgument.player()).executes(c -> {
                  var p=c.getSource().getPlayerOrException(); return result(c.getSource(),WardSources.share(p,target(p),EntityArgument.getPlayer(c,"player").getUUID(),allow));
              })));
        return root;
    }
    private WardCommands() { }
}
