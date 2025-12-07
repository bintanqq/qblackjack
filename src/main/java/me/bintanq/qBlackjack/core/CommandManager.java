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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final QBlackjack plugin;

    public CommandManager(QBlackjack plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        MessageManager messages = plugin.getMessageManager();

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                handleReloadCommand(sender);
                return true;
            }
            if (args[0].equalsIgnoreCase("chip")) {
                handleChipCommand(sender, args);
                return true;
            }
            if (args[0].equalsIgnoreCase("exchange")) {
                handleExchangeCommand(sender, args);
                return true;
            }
        }

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

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
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

    private void handleReloadCommand(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        if (!sender.hasPermission("qblackjack.admin")) {
            sender.sendMessage(messages.getMessage("messages.error.no-permission"));
            return;
        }

        if (plugin.getSettingsManager().getEconomyMode().equalsIgnoreCase("CHIP") && plugin.getChipManager() != null) {
            plugin.getChipManager().saveData();
        }

        plugin.getSettingsManager().reloadConfig();
        plugin.getMessageManager().reloadMessages();

        if (plugin.getSettingsManager().getEconomyMode().equalsIgnoreCase("CHIP") && plugin.getChipManager() != null) {
            plugin.getChipManager().loadData();
        }

        sender.sendMessage(messages.getMessage("messages.reload.success"));
    }

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
            Player targetPlayer = null;
            if (args.length == 3 && sender.hasPermission("qblackjack.admin")) {
                targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) { sender.sendMessage(messages.getMessage("messages.chip.error-player-offline").replace("%player%", args[2])); return; }
            } else if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage(messages.getMessage("messages.chip.usage")); return;
            }

            double balance = chipManager.getBalance(targetPlayer.getUniqueId());
            String msg = (targetPlayer == sender) ? messages.getMessage("messages.chip.balance") : messages.getMessage("messages.chip.balance-other").replace("%player%", targetPlayer.getName());
            sender.sendMessage(msg.replace("%balance%", String.valueOf(balance)));
            return;
        }

        if (subCommand.equals("give") || subCommand.equals("take") || subCommand.equals("set")) {
            if (!sender.hasPermission("qblackjack.admin")) {
                sender.sendMessage(messages.getMessage("messages.error.no-permission"));
                return;
            }

            if (args.length < 4) { sender.sendMessage(messages.getMessage("messages.chip.usage")); return; }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) { sender.sendMessage(messages.getMessage("messages.chip.error-player-offline").replace("%player%", args[2])); return; }

            double amount;
            try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) {
                sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return;
            }

            String amountStr = String.valueOf(amount);

            if (subCommand.equals("give")) {
                if (amount <= 0) { sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return; }
                chipManager.deposit(target.getUniqueId(), amount);
                sender.sendMessage(messages.getMessage("messages.chip.success-give").replace("%amount%", amountStr).replace("%player%", target.getName()));
                target.sendMessage(messages.getMessage("messages.chip.given").replace("%amount%", amountStr));
            } else if (subCommand.equals("take")) {
                if (amount <= 0) { sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return; }
                chipManager.withdraw(target.getUniqueId(), amount);
                sender.sendMessage(messages.getMessage("messages.chip.success-take").replace("%amount%", amountStr).replace("%player%", target.getName()));
                target.sendMessage(messages.getMessage("messages.chip.taken").replace("%amount%", amountStr));
            } else if (subCommand.equals("set")) {
                if (amount < 0) { sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return; }
                chipManager.setBalance(target.getUniqueId(), amount);
                sender.sendMessage(messages.getMessage("messages.chip.success-set").replace("%amount%", amountStr).replace("%player%", target.getName()));
                target.sendMessage(messages.getMessage("messages.chip.set").replace("%amount%", amountStr));
            }
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

        if (args.length < 2) { sender.sendMessage(messages.getMessage("messages.exchange.usage")); return; }

        double amount;
        try { amount = Double.parseDouble(args[1]); if (amount <= 0) throw new NumberFormatException(); } catch (NumberFormatException e) {
            sender.sendMessage(messages.getMessage("messages.error.invalid-format")); return;
        }

        if (econ.getBalance(player) < amount) {
            sender.sendMessage(messages.getMessage("messages.exchange.insufficient-vault"));
            return;
        }

        if (!econ.withdrawPlayer(player, amount).transactionSuccess()) {
            sender.sendMessage(messages.getMessage("messages.error.general")); return;
        }

        plugin.getChipManager().deposit(player.getUniqueId(), amount);

        sender.sendMessage(messages.getMessage("messages.exchange.success")
                .replace("%vault_amount%", econ.format(amount))
                .replace("%chip_amount%", String.valueOf(amount)));
    }

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("chip");
            completions.add("exchange");
            completions.add("help");
            if (sender.hasPermission("qblackjack.admin")) {
                completions.add("reload");
            }

            if (sender instanceof Player && !plugin.getBlackjackManager().isPlayerInGame(((Player) sender).getUniqueId())) {
                completions.addAll(getRawBetSuggestions(plugin.getSettingsManager(), true));
            }
            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("chip")) {
            completions.add("check");
            if (sender.hasPermission("qblackjack.admin")) {
                completions.add("give");
                completions.add("take");
                completions.add("set");
            }
            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("exchange")) {
            return getRawBetSuggestions(plugin.getSettingsManager(), false).stream().filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("chip")) {
            String subCommand = args[1].toLowerCase();
            if (sender.hasPermission("qblackjack.admin") && (subCommand.equals("give") || subCommand.equals("take") || subCommand.equals("set"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
            if (subCommand.equals("check")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("chip") && sender.hasPermission("qblackjack.admin")) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("take") || subCommand.equals("set")) {
                return getRawBetSuggestions(plugin.getSettingsManager(), false).stream().filter(s -> s.startsWith(args[3])).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> getRawBetSuggestions(SettingsManager settings, boolean useMinMaxOnly) {
        double minBet = settings.getMinBet();
        double maxBet = settings.getMaxBet();
        double increment = settings.getIncrement();

        List<String> suggestions = new ArrayList<>();

        java.util.function.Function<Double, String> toRawString = amount -> String.valueOf((long) amount.doubleValue());

        suggestions.add(toRawString.apply(minBet));
        suggestions.add(toRawString.apply(maxBet));

        if (!useMinMaxOnly) {
            if (minBet < 100) {
                suggestions.add(toRawString.apply(100.0));
            }
            if (maxBet > 1000) {
                suggestions.add(toRawString.apply(1000.0));
            }
            if (maxBet > 5000) {
                suggestions.add(toRawString.apply(5000.0));
            }

            if (minBet + increment * 2 <= maxBet) {
                suggestions.add(toRawString.apply(minBet + increment * 2));
            }
        } else {
            if (minBet < 500 && maxBet >= 500) {
                suggestions.add(toRawString.apply(500.0));
            }
            if (minBet < 1000 && maxBet >= 1000) {
                suggestions.add(toRawString.apply(1000.0));
            }
        }

        return suggestions.stream().distinct().collect(Collectors.toList());
    }
}