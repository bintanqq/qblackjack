package me.bintanq.qBlackjack.events;

import me.bintanq.qBlackjack.QBlackjack;
import me.bintanq.qBlackjack.managers.BlackjackManager;
import me.bintanq.qBlackjack.game.BlackjackGame;
import me.bintanq.qBlackjack.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class GameListener implements Listener {

    private final QBlackjack plugin;
    private final BlackjackManager manager;

    public GameListener(QBlackjack plugin) {
        this.plugin = plugin;
        this.manager = plugin.getBlackjackManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (!event.getView().getTitle().startsWith(plugin.getMessageManager().getGUITitlePrefix())) return;

        event.setCancelled(true);

        if (!manager.isPlayerInGame(playerUUID)) {
            player.closeInventory();
            return;
        }

        BlackjackGame game = manager.getPlayerGame(playerUUID);
        int slot = event.getSlot();

        // SLOT YANG DIREVISI:
        final int HIT_SLOT = 47;
        final int FORFEIT_EXIT_SLOT = 49;
        final int STAND_SLOT = 51;

        // 1. Logika Klik saat GameState.ENDED / DEALER_TURN
        if (game.getGameState() != GameState.PLAYING && game.getGameState() != GameState.DEALING) {
            // Hanya izinkan klik pada tombol EXIT (Slot 49)
            if (slot == FORFEIT_EXIT_SLOT) {
                game.forfeitAction();
            }
            return;
        }

        // 2. Logika Klik saat GameState.PLAYING
        if (game.getGameState() == GameState.PLAYING) {
            if (slot == HIT_SLOT) {
                game.hitAction();
            } else if (slot == STAND_SLOT) {
                game.standAction();
            } else if (slot == FORFEIT_EXIT_SLOT) {
                game.forfeitAction();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!event.getView().getTitle().startsWith(plugin.getMessageManager().getGUITitlePrefix())) return;

        BlackjackGame game = manager.getPlayerGame(playerUUID);

        if (game == null) return;

        // JIKA MASIH BERMAIN: Buka kembali GUI setelah 1 tick
        if (game.getGameState() == GameState.PLAYING || game.getGameState() == GameState.DEALING) {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> player.openInventory(game.getGameGUI()), 1L);
            return;
        }

        // JIKA GAME SUDAH BERAKHIR (ENDED/DEALER_TURN): Akhiri sesi di manager
        if (game.getGameState() == GameState.ENDED || game.getGameState() == GameState.DEALER_TURN) {
            manager.endGame(playerUUID);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.handlePlayerQuit(event.getPlayer());
    }
}