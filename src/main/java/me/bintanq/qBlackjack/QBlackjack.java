package me.bintanq.qBlackjack;

import me.bintanq.qBlackjack.core.SettingsManager;
import me.bintanq.qBlackjack.core.MessageManager;
import me.bintanq.qBlackjack.core.CommandManager;
import me.bintanq.qBlackjack.events.GameListener;
import me.bintanq.qBlackjack.managers.BlackjackManager;
import me.bintanq.qBlackjack.managers.ChipManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class QBlackjack extends JavaPlugin {

    private static QBlackjack instance;
    private Economy econ = null;

    private SettingsManager settingsManager;
    private MessageManager messageManager;
    private ChipManager chipManager;
    private BlackjackManager blackjackManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        setupMessageFile();

        this.settingsManager = new SettingsManager(this);
        this.messageManager = new MessageManager(this);

        String mode = settingsManager.getEconomyMode();
        if (mode.equalsIgnoreCase("VAULT")) {
            if (!setupVaultEconomy()) {
                getLogger().severe("VAULT NOT FOUND. Switching economy mode to CHIP.");
                settingsManager.setEconomyMode("CHIP");
                this.chipManager = new ChipManager();
            }
        } else {
            this.chipManager = new ChipManager();
        }

        this.blackjackManager = new BlackjackManager();

        registerCommands();
        registerEvents();

        getLogger().info("Enabled");
    }

    @Override
    public void onDisable() {
        if (settingsManager.getEconomyMode().equalsIgnoreCase("CHIP") && this.chipManager != null) {
            this.chipManager.saveData();
        }
        instance = null;

        getLogger().info("Disabled");
    }

    private boolean setupVaultEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void setupMessageFile() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
    }

    private void registerCommands() {
        CommandManager cmdManager = new CommandManager(this);
        getCommand("blackjack").setExecutor(cmdManager);
        getCommand("blackjack").setTabCompleter(cmdManager);
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
    }



    public static QBlackjack getInstance() { return instance; }
    public Economy getEconomy() { return this.econ; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public ChipManager getChipManager() { return chipManager; }
    public BlackjackManager getBlackjackManager() { return blackjackManager; }
}