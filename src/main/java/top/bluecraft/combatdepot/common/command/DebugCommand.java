package top.bluecraft.combatdepot.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import top.bluecraft.combatdepot.config.Config;

import static top.bluecraft.combatdepot.CombatDepot.configmanager;

public class DebugCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("debug_cd")
                        .then(
                                Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setDebugValue(context, BoolArgumentType.getBool(context, "value")))
                        )
        );
    }

    private static int setDebugValue(CommandContext<CommandSourceStack> context, boolean value) {
        Config.debug = value;
        configmanager.save();
        context.getSource().sendSuccess(() -> Component.literal("Debug mode set to: " + value), true);
        return 1;
    }
}
