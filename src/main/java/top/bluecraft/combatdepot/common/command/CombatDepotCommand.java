package top.bluecraft.combatdepot.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.item.TerminalProvider;

public class CombatDepotCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(CombatDepot.MODID)
                .requires(source -> source.hasPermission(0)) // 所有玩家可执行
                .executes(context -> execute(context.getSource()));
        dispatcher.register(command);
    }

    private static int execute(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            // 主指令逻辑
            player.openMenu(new TerminalProvider());
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
