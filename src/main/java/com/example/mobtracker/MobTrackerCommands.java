package com.example.mobtracker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class MobTrackerCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("tracker")
            .then(literal("toggle").executes(ctx -> { MobTrackerMod.toggleActive(); return 1; }))
            .then(literal("add").then(argument("name", StringArgumentType.string())
                .executes(ctx -> { 
                    MobTrackerMod.addNametag(StringArgumentType.getString(ctx, "name")); 
                    return 1; 
                })))
            .then(literal("remove").then(argument("name", StringArgumentType.string())
                .executes(ctx -> { 
                    MobTrackerMod.removeNametag(StringArgumentType.getString(ctx, "name")); 
                    return 1; 
                })))
            .then(literal("range").then(argument("blocks", IntegerArgumentType.integer(10, 500))
                .executes(ctx -> { 
                    MobTrackerMod.setRange(IntegerArgumentType.getInteger(ctx, "blocks")); 
                    return 1; 
                })))
            .then(literal("clear").executes(ctx -> { MobTrackerMod.clearTracked(); return 1; }))
            .then(literal("list").executes(ctx -> { 
                if (ctx.getSource().getPlayer() != null) 
                    MobTrackerMod.listNametags(ctx.getSource().getPlayer()); 
                return 1; 
            }))
            .then(literal("status").executes(ctx -> { 
                if (ctx.getSource().getPlayer() != null) 
                    MobTrackerMod.showStatus(ctx.getSource().getPlayer()); 
                return 1; 
            }))
            .then(literal("help").executes(ctx -> {
                ctx.getSource().sendFeedback(() -> Text.literal("""
                    §6MobTracker Commands:
                    §e/tracker toggle §7- Enable/disable
                    §e/tracker add <name> §7- Add nametag
                    §e/tracker remove <name> §7- Remove nametag
                    §e/tracker range <10-500> §7- Set range
                    §e/tracker clear §7- Clear tracked
                    §e/tracker list §7- List nametags
                    §e/tracker status §7- Show status
                    §6Keybinds: §7H(toggle), J(config), K(add)
                    """), false);
                return 1;
            }))
        );
    }
}
