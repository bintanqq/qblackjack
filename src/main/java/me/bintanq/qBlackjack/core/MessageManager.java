package me.bintanq.qBlackjack.core;

import me.bintanq.qBlackjack.QBlackjack;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {
    private final QBlackjack plugin;
    private FileConfiguration messagesConfig;
    private String messagePrefix;
    private String guiTitlePrefix;

    public MessageManager(QBlackjack plugin) {
        this.plugin = plugin;
        loadMessagesConfig();

        this.messagePrefix = ChatColor.translateAlternateColorCodes('&',
                messagesConfig.getString("messages.prefix", "&b[Q-Blackjack] &7"));
        this.guiTitlePrefix = ChatColor.DARK_BLUE + "Blackjack Game: ";
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) return ChatColor.RED + "Error: Message path not found in messages.yml: " + path;

        if (!path.equalsIgnoreCase("messages.prefix")) {
            msg = this.messagePrefix + msg;
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getGUITitlePrefix() {
        return guiTitlePrefix;
    }
}