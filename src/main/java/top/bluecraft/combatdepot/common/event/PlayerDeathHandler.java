package top.bluecraft.combatdepot.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.util.BitSet;

@Mod.EventBusSubscriber(modid = CombatDepot.MODID)
public class PlayerDeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            // 当玩家死亡时，获取玩家的GunViewMenu（如果存在）
            if (player.containerMenu instanceof CombatDepotMenu menu) {
                // 获取槽位标记
                BitSet takenSlots = menu.getTakenSlots();
                // 重置所有槽位标记
                takenSlots.clear();
                // 广播更改以同步到客户端
                menu.broadcastChanges();
            }
        }
    }
}
