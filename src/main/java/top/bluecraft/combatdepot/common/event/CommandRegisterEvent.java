package top.bluecraft.combatdepot.common.event;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.common.command.CombatDepotCommand;
import top.bluecraft.combatdepot.common.command.DebugCommand;


@Mod.EventBusSubscriber
public class CommandRegisterEvent {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CombatDepotCommand.register(event.getDispatcher());
        DebugCommand.register(event.getDispatcher());
    }
}
