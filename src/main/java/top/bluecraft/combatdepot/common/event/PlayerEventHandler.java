package top.bluecraft.combatdepot.common.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;

@Mod.EventBusSubscriber
public class PlayerEventHandler {
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            GlobalCardStorage storage = GlobalCardStorage.get(level);
            storage.setDirty(); // 强制保存数据
        }
    }
}
