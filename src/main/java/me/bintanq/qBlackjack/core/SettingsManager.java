package me.bintanq.qBlackjack.core;

import me.bintanq.qBlackjack.QBlackjack;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsManager {
    private final QBlackjack plugin;
    private FileConfiguration config; // Diubah menjadi non-final agar bisa di-reload

    public SettingsManager(QBlackjack plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    // --- PENAMBAHAN UNTUK RELOAD ---
    public void reloadConfig() {
        plugin.reloadConfig();
        // Mengambil ulang konfigurasi yang baru dimuat
        this.config = plugin.getConfig();
    }
    // --------------------------------

    public String getEconomyMode() { return config.getString("economy-mode", "VAULT"); }
    public void setEconomyMode(String mode) { config.set("economy-mode", mode); plugin.saveConfig(); }
    public double getBlackjackMultiplier() { return config.getDouble("payout.blackjack-multiplier", 2.5); }
    public double getWinMultiplier() { return config.getDouble("payout.win-multiplier", 2.0); }
    public double getMinBet() { return config.getDouble("betting-limits.min-bet", 100.00); }
    public double getMaxBet() { return config.getDouble("betting-limits.max-bet", 5000.00); }
    public double getIncrement() { return config.getDouble("betting-limits.increment", 50.00); }
}