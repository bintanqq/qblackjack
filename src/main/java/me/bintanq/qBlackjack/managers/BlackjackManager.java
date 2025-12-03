package me.bintanq.qBlackjack.managers;

import me.bintanq.qBlackjack.QBlackjack;
import me.bintanq.qBlackjack.game.BlackjackGame;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlackjackManager {

    private final QBlackjack plugin;
    private final Map<UUID, BlackjackGame> activeGames;

    public BlackjackManager() {
        this.plugin = QBlackjack.getInstance();
        this.activeGames = new HashMap<>();
    }

    public void startNewGame(Player player, double bet) {
        UUID playerUUID = player.getUniqueId();

        if (activeGames.containsKey(playerUUID)) return;

        BlackjackGame game = new BlackjackGame(plugin, player, bet);
        activeGames.put(playerUUID, game);
        game.startGame();

        player.sendMessage(plugin.getMessageManager().getMessage("messages.game.bet-placed")
                .replace("%bet%", String.valueOf(bet)));
    }

    public void endGame(UUID playerUUID) {
        if (!activeGames.containsKey(playerUUID)) return;

        BlackjackGame game = activeGames.remove(playerUUID);

        if (game.getPlayer().getOpenInventory().getTopInventory().equals(game.getGameGUI())) {
            game.getPlayer().closeInventory();
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (activeGames.containsKey(playerUUID)) {
            BlackjackGame game = activeGames.get(playerUUID);

            game.forfeit();
            endGame(playerUUID);

            plugin.getLogger().info("Player " + player.getName() + " forfeited Blackjack due to disconnect.");
        }
    }

    public boolean isPlayerInGame(UUID playerUUID) { return activeGames.containsKey(playerUUID); }
    public BlackjackGame getPlayerGame(UUID playerUUID) { return activeGames.get(playerUUID); }
}