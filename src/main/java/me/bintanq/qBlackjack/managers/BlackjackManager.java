package me.bintanq.qBlackjack.managers;

import me.bintanq.qBlackjack.QBlackjack;
import me.bintanq.qBlackjack.core.MessageManager;
import me.bintanq.qBlackjack.core.SettingsManager;
import me.bintanq.qBlackjack.game.BlackjackGame;
import net.milkbowl.vault.economy.Economy;
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

    private boolean processBetTransaction(Player player, double amount) {
        SettingsManager settings = plugin.getSettingsManager();
        MessageManager messages = plugin.getMessageManager();

        if (settings.getEconomyMode().equalsIgnoreCase("VAULT")) {
            Economy econ = plugin.getEconomy();
            if (econ == null) return false;

            if (!econ.has(player, amount)) {
                player.sendMessage(messages.getMessage("messages.error.insufficient-funds"));
                return false;
            }
            econ.withdrawPlayer(player, amount);
            return true;
        }
        else {
            ChipManager chipManager = plugin.getChipManager();
            if (chipManager == null) return false;

            if (!chipManager.has(player.getUniqueId(), amount)) {
                player.sendMessage(messages.getMessage("messages.error.insufficient-chips"));
                return false;
            }
            chipManager.withdraw(player.getUniqueId(), amount);
            return true;
        }
    }

    public void startNewGame(Player player, double bet) {
        UUID playerUUID = player.getUniqueId();
        MessageManager messages = plugin.getMessageManager();

        if (activeGames.containsKey(playerUUID)) {
            player.sendMessage(messages.getMessage("messages.error.already-playing"));
            return;
        }

        if (!processBetTransaction(player, bet)) {
            return;
        }

        BlackjackGame game = new BlackjackGame(plugin, player, bet);
        activeGames.put(playerUUID, game);
        game.startGame();

        player.sendMessage(plugin.getMessageManager().getMessage("messages.game.bet-placed")
                .replace("%bet%", MessageManager.formatAmount(bet)));
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