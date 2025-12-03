package me.bintanq.qBlackjack.core;

import me.bintanq.qBlackjack.QBlackjack;
import me.bintanq.qBlackjack.managers.BlackjackManager;
import me.bintanq.qBlackjack.managers.ChipManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter { // Tambahkan TabCompleter

    private final QBlackjack plugin;

    public CommandManager(QBlackjack plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        MessageManager messages = plugin.getMessageManager();

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("chip")) {
                handleChipCommand(sender, args);
                return true;
            }
            if (args[0].equalsIgnoreCase("exchange")) {
                handleExchangeCommand(sender, args);
                return true;
            }
        }

        // --- LOGIKA GAME START ---
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("messages.error.console"));
            return true;
        }

        Player player = (Player) sender;
        SettingsManager settings = plugin.getSettingsManager();
        BlackjackManager blackjackManager = plugin.getBlackjackManager();

        if (blackjackManager.isPlayerInGame(player.getUniqueId())) {
            player.sendMessage(messages.getMessage("messages.error.already-playing"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { // Tambahkan help
            player.sendMessage(messages.getMessage("messages.help.usage"));
            return true;
        }

        double amount;
        try { amount = Double.parseDouble(args[0]); } catch (NumberFormatException e) {
            player.sendMessage(messages.getMessage("messages.error.invalid-format")); return true;
        }

        double min = settings.getMinBet(); double max = settings.getMaxBet(); double increment = settings.getIncrement();

        if (amount < min || amount > max) {
            player.sendMessage(messages.getMessage("messages.error.invalid-bet-range").replace("%min%", String.valueOf(min)).replace("%max%", String.valueOf(max))); return true;
        }
        if (amount % increment != 0) {
            player.sendMessage(messages.getMessage("messages.error.invalid-bet-increment").replace("%increment%", String.valueOf(increment))); return true;
        }

        if (settings.getEconomyMode().equalsIgnoreCase("VAULT")) {
            if (!handleVaultTransaction(player, amount)) return true;
        } else {
            if (!handleChipTransaction(player, amount)) return true;
        }

        blackjackManager.startNewGame(player, amount);
        return true;
    }

    // --- SUB-COMMAND HANDLERS ---

    private void handleChipCommand(CommandSender sender, String[] args) {
        MessageManager messages = plugin.getMessageManager();
        SettingsManager settings = plugin.getSettingsManager();
        ChipManager chipManager = plugin.getChipManager();

        if (!settings.getEconomyMode().equalsIgnoreCase("CHIP")) {
            sender.sendMessage(messages.getMessage("messages.error.general").replace("Terjadi kesalahan saat memproses transaksi.", "Error: Chip mode is not enabled."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(messages.getMessage("messages.chip.usage"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        if (subCommand.equals("check")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messages.getMessage("messages.error.console"));
                return;
            }
            Player player = (Player) sender;
            double balance = chipManager.getBalance(player.getUniqueId());
            sender.sendMessage(messages.getMessage("messages.chip.balance").replace("%balance%", String.valueOf(balance)));
            return;
        }

        if (subCommand.equals("give")) {
            if (!sender.hasPermission("qblackjack.admin.chip")) {
                sender.sendMessage(messages.getMessage("messages.chip.error-no-permission"));
                return;
            }
            // /blackjack chip give <player> <amount>
            if (args.length < 4) { sender.sendMessage(messages.getMessage("messages.chip.usage")); return; }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) { sender.sendMessage(messages.getMessage("messages.chip.error-player-offline").replace("%player%", args[2])); return; }

            double amount;
            try { amount = Double.parseDouble(args[3]); if (amount <= 0) throw new NumberFormatException(); } catch (NumberFormatException e) {
                sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return;
            }

            chipManager.deposit(target.getUniqueId(), amount);

            String amountStr = String.valueOf(amount);
            sender.sendMessage(messages.getMessage("messages.chip.success").replace("%amount%", amountStr).replace("%player%", target.getName()));
            target.sendMessage(messages.getMessage("messages.chip.given").replace("%amount%", amountStr));
            return;
        }

        sender.sendMessage(messages.getMessage("messages.chip.usage"));
    }

    private void handleExchangeCommand(CommandSender sender, String[] args) {
        MessageManager messages = plugin.getMessageManager();
        SettingsManager settings = plugin.getSettingsManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("messages.error.console")); return;
        }
        Player player = (Player) sender;

        if (!settings.getEconomyMode().equalsIgnoreCase("CHIP")) {
            sender.sendMessage(messages.getMessage("messages.exchange.vault-mode-required"));
            return;
        }

        Economy econ = plugin.getEconomy();
        if (econ == null) {
            sender.sendMessage(messages.getMessage("messages.exchange.vault-not-available"));
            return;
        }

        // /blackjack exchange <amount>
        if (args.length < 2) { sender.sendMessage(messages.getMessage("messages.exchange.usage")); return; }

        double amount;
        try { amount = Double.parseDouble(args[1]); if (amount <= 0) throw new NumberFormatException(); } catch (NumberFormatException e) {
            sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return;
        }

        if (econ.getBalance(player) < amount) {
            sender.sendMessage(messages.getMessage("messages.exchange.insufficient-vault"));
            return;
        }

        // Lakukan Pertukaran (1:1 Rate Sederhana)
        if (!econ.withdrawPlayer(player, amount).transactionSuccess()) {
            sender.sendMessage(messages.getMessage("messages.error.general")); return;
        }

        plugin.getChipManager().deposit(player.getUniqueId(), amount);

        sender.sendMessage(messages.getMessage("messages.exchange.success")
                .replace("%vault_amount%", econ.format(amount))
                .replace("%chip_amount%", String.valueOf(amount)));
    }


    // --- TRANSAKSI UTILITY (Tidak Berubah) ---

    private boolean handleVaultTransaction(Player player, double amount) {
        Economy econ = plugin.getEconomy();
        MessageManager messages = plugin.getMessageManager();
        if (econ == null || econ.getBalance(player) < amount) {
            player.sendMessage(messages.getMessage("messages.error.insufficient-funds")); return false;
        }
        if (!econ.withdrawPlayer(player, amount).transactionSuccess()) {
            player.sendMessage(messages.getMessage("messages.error.general")); return false;
        }
        return true;
    }

    private boolean handleChipTransaction(Player player, double amount) {
        ChipManager chipManager = plugin.getChipManager();
        MessageManager messages = plugin.getMessageManager();
        if (!chipManager.has(player.getUniqueId(), amount)) {
            player.sendMessage(messages.getMessage("messages.error.insufficient-funds")); return false;
        }
        chipManager.withdraw(player.getUniqueId(), amount);
        return true;
    }

    // --- TAB COMPLETER IMPLEMENTATION (BARU) ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("chip", "exchange"));

            // Contoh taruhan cepat (jika bukan command admin)
            if (sender instanceof Player && !plugin.getBlackjackManager().isPlayerInGame(((Player) sender).getUniqueId())) {
                completions.add("100");
                completions.add("500");
            }
            return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("chip")) {
                List<String> chipCompletions = new ArrayList<>(Collections.singletonList("check"));
                if (sender.hasPermission("qblackjack.admin.chip")) {
                    chipCompletions.add("give");
                }
                return chipCompletions.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            // Untuk /blackjack exchange <amount>
            if (args[0].equalsIgnoreCase("exchange")) {
                return Arrays.asList("100", "500", "1000").stream().filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("chip") && args[1].equalsIgnoreCase("give") && sender.hasPermission("qblackjack.admin.chip")) {
            // /blackjack chip give <player>
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.startsWith(args[2])).collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("chip") && args[1].equalsIgnoreCase("give") && sender.hasPermission("qblackjack.admin.chip")) {
            // /blackjack chip give <player> <amount>
            return Arrays.asList("100", "1000", "5000").stream().filter(s -> s.startsWith(args[3])).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}