package me.bintanq.qBlackjack.game;

import me.bintanq.qBlackjack.QBlackjack;
import me.bintanq.qBlackjack.core.SettingsManager;
import me.bintanq.qBlackjack.core.MessageManager;
import me.bintanq.qBlackjack.managers.ChipManager;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackjackGame {

    private final QBlackjack plugin;
    private final Player player;
    private final double currentBet;

    private GameState gameState;
    private Deck deck;
    private Inventory gameGUI;
    private List<Card> playerHand;
    private List<Card> dealerHand;
    private boolean isProcessingAction = false;

    private final int DEALER_START_SLOT = 20;

    public BlackjackGame(QBlackjack plugin, Player player, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.currentBet = bet;
        this.gameState = GameState.DEALING;
        this.deck = new Deck();
        this.playerHand = new ArrayList<>();
        this.dealerHand = new ArrayList<>();
    }

    public void startGame() {
        MessageManager messages = plugin.getMessageManager();
        String guiTitle = messages.getGUITitlePrefix() + player.getName();
        this.gameGUI = Bukkit.createInventory(null, 54, guiTitle);

        updateActionButtons(true);

        long initialDelay = 5L;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> dealCard(dealerHand, DEALER_START_SLOT, true, false), initialDelay * 1);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> dealCard(playerHand, 38, false, false), initialDelay * 2);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> dealCard(dealerHand, DEALER_START_SLOT + 1, false, false), initialDelay * 3);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            dealCard(playerHand, 39, false, false);
            this.gameState = GameState.PLAYING;
            checkGameStatus(playerHand, true);
            player.openInventory(gameGUI);
        }, initialDelay * 4);
    }

    public void hitAction() {
        if (gameState != GameState.PLAYING) return;

        isProcessingAction = true;

        int nextSlot = 38 + playerHand.size();
        if (nextSlot > 42){
            isProcessingAction = false;
            return;
        }

        updateActionButtons(false);
        dealCard(playerHand, nextSlot, false, false);
    }

    public void standAction() {
        if (gameState != GameState.PLAYING) return;

        this.gameState = GameState.DEALER_TURN;
        updateActionButtons(false);

        revealDealerHoleCard(true);

        plugin.getServer().getScheduler().runTaskLater(plugin, this::continueDealerTurn, 25L);
    }

    public void forfeitAction() {
        if (gameState == GameState.PLAYING) {
            forfeit();
        } else if (gameState == GameState.ENDED) {
            plugin.getBlackjackManager().endGame(player.getUniqueId());
        }
    }

    private void continueDealerTurn() {
        int dealerValue = calculateValue(dealerHand);

        if (dealerValue >= 17 || dealerValue > 21 || dealerHand.size() >= 5) {
            plugin.getServer().getScheduler().runTaskLater(plugin, this::processPayouts, 20L);
            return;
        }

        final int slotToDeal = DEALER_START_SLOT + dealerHand.size();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            dealCard(dealerHand, slotToDeal, false, true);

            plugin.getServer().getScheduler().runTaskLater(this.plugin, this::continueDealerTurn, 20L);

        }, 20L);
    }

    public void processPayouts() {
        if (gameState == GameState.ENDED) return;
        this.gameState = GameState.ENDED;

        SettingsManager settings = plugin.getSettingsManager();
        MessageManager messages = plugin.getMessageManager();

        double multiplier = 0.0;
        String messagePath = "messages.result.lose";

        revealDealerHoleCard(true);

        int pValue = calculateValue(playerHand);
        int dValue = calculateValue(dealerHand);

        if (pValue > 21) {
            multiplier = 0.0; messagePath = "messages.result.bust";
        } else if (dValue > 21) {
            multiplier = settings.getWinMultiplier(); messagePath = "messages.result.win";
        } else if (isBlackjack(playerHand) && !isBlackjack(dealerHand)) {
            multiplier = settings.getBlackjackMultiplier(); messagePath = "messages.result.blackjack";
        } else if (pValue == dValue) {
            multiplier = 1.0; messagePath = "messages.result.push";
        } else if (pValue > dValue) {
            multiplier = settings.getWinMultiplier(); messagePath = "messages.result.win";
        } else {
            multiplier = 0.0; messagePath = "messages.result.lose";
        }

        double totalWinnings = currentBet * multiplier;

        if (settings.getEconomyMode().equalsIgnoreCase("VAULT")) {
            Economy econ = plugin.getEconomy();
            if (totalWinnings > 0 && econ != null) {
                econ.depositPlayer(player, totalWinnings);
            }
        } else {
            ChipManager chipManager = plugin.getChipManager();
            if (totalWinnings > 0) {
                chipManager.deposit(player.getUniqueId(), totalWinnings);
            }
        }

        player.sendMessage(messages.getMessage(messagePath)
                .replace("%winnings%", MessageManager.formatAmount(totalWinnings))
                .replace("%bet%", MessageManager.formatAmount(currentBet)));

        updateActionButtons(false);
    }

    public void forfeit() {
        if (gameState == GameState.ENDED) return;
        this.gameState = GameState.ENDED;
        player.sendMessage(plugin.getMessageManager().getMessage("messages.result.forfeit")
                .replace("%bet%", MessageManager.formatAmount(currentBet)));
        updateActionButtons(false);
    }

    public int calculateValue(List<Card> hand) {
        int value = 0;
        int aceCount = 0;

        for (Card card : hand) {
            value += card.getValue();
            if (card.getRank().equals("Ace")) {
                aceCount++;
            }
        }

        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }
        return value;
    }

    private boolean isBlackjack(List<Card> hand) {
        return hand.size() == 2 && calculateValue(hand) == 21;
    }

    private void checkGameStatus(List<Card> hand, boolean isInitialDeal) {
        int value = calculateValue(hand);

        if (value > 21) {
            processPayouts();
        } else if (isBlackjack(hand) && isInitialDeal) {
            standAction();
        }
        updateScoreboard();
    }

    private void dealCard(List<Card> targetHand, int slot, boolean isHidden, boolean isDealerTurn) {
        Card card = deck.drawCard();
        targetHand.add(card);

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);

        ItemStack placeholder = isHidden ? createHiddenCardItem() : createPlaceholderItem();
        gameGUI.setItem(slot, placeholder);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack finalItem = isHidden ? createHiddenCardItem() : card.getItemStack();
            gameGUI.setItem(slot, finalItem);

            if (!isDealerTurn) {
                checkGameStatus(targetHand, false);
                updateActionButtons(calculateValue(playerHand) <= 21);
                isProcessingAction = false;
            }
            updateScoreboard();
        }, 5L);
    }

    private void revealDealerHoleCard(boolean isFinalReveal) {
        if (dealerHand.isEmpty()) return;

        Card hiddenCard = dealerHand.get(0);
        int slot = DEALER_START_SLOT;

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            gameGUI.setItem(slot, hiddenCard.getItemStack());
            if (isFinalReveal) {
                updateScoreboard();
            }
        }, 5L);
    }

    public void updateActionButtons(boolean enableHitStand) {

        if (gameState == GameState.ENDED) {

            gameGUI.setItem(47, createDisabledButton());

            gameGUI.setItem(51, createDisabledButton());

        } else {

            ItemStack hitButton = enableHitStand
                    ? createButton(Material.LIME_STAINED_GLASS_PANE, "HIT", ChatColor.GREEN, "take another card")
                    : createDisabledButton();
            gameGUI.setItem(47, hitButton);

            ItemStack standButton = enableHitStand
                    ? createButton(Material.RED_STAINED_GLASS_PANE, "STAND", ChatColor.RED, "end your turn")
                    : createDisabledButton();
            gameGUI.setItem(51, standButton);
        }

        setupExitForfeitButton();

        setupBackground();
    }

    private void setupExitForfeitButton() {
        MessageManager messages = plugin.getMessageManager();
        Material material = gameState == GameState.ENDED ? Material.NETHER_STAR : Material.BARRIER;
        ChatColor color = gameState == GameState.ENDED ? ChatColor.YELLOW : ChatColor.DARK_RED;
        String name = gameState == GameState.ENDED ? "EXIT GAME" : "FORFEIT (SURRENDER)";

        String loreKey = gameState == GameState.ENDED ? "messages.gui-lore.exit" : "messages.gui-lore.forfeit";

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + "" + ChatColor.BOLD + name);
        meta.setLore(Collections.singletonList(messages.getMessage(loreKey)));
        item.setItemMeta(meta);
        gameGUI.setItem(49, item);
    }

    public void updateScoreboard() {
        int pValue = calculateValue(playerHand);
        int dValue = calculateValue(dealerHand);

        gameGUI.setItem(3, createConfigItem("player-score", String.valueOf(pValue)));

        ItemStack betInfo = createConfigItem("current-bet", MessageManager.formatAmount(currentBet));
        ItemMeta betMeta = betInfo.getItemMeta();

        SettingsManager settings = plugin.getSettingsManager();
        String statusLore = settings.getConfig().getString("gui-items.current-bet.lore", "&7Status: %status%");
        betMeta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', statusLore)
                .replace("%status%", gameState.toString())));
        betInfo.setItemMeta(betMeta);
        gameGUI.setItem(4, betInfo);

        int visibleDValue = (gameState == GameState.PLAYING || gameState == GameState.DEALING)
                ? calculateVisibleDealerValue() : dValue;
        gameGUI.setItem(5, createConfigItem("dealer-score", String.valueOf(visibleDValue)));
    }

    private ItemStack createConfigItem(String configPath, String value) {
        SettingsManager settings = plugin.getSettingsManager();

        String materialName = settings.getConfig().getString("gui-items." + configPath + ".material", "PAPER");
        String nameFormat = settings.getConfig().getString("gui-items." + configPath + ".name", "&fName Missing");
        String loreFormat = settings.getConfig().getString("gui-items." + configPath + ".lore", "&7Lore Missing");

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String finalName = ChatColor.translateAlternateColorCodes('&', nameFormat) + ChatColor.WHITE + ": " + value;

        meta.setDisplayName(finalName);

        if (!configPath.equals("current-bet")) {
            meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', loreFormat)));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material material, String name, ChatColor color, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + "" + ChatColor.BOLD + name);
        List<String> lore = Collections.singletonList(ChatColor.GRAY + "Click to " + action + ".");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDisabledButton() {
        return createButton(Material.GRAY_DYE, "Action Locked", ChatColor.DARK_GRAY, "Wait for your turn");
    }

    private int calculateVisibleDealerValue() {
        if (dealerHand.size() <= 1) return 0;
        List<Card> visibleHand = dealerHand.subList(1, dealerHand.size());
        return calculateValue(visibleHand);
    }

    private ItemStack createHiddenCardItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Dealer Card (Hidden)");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlaceholderItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLACK + "" + ChatColor.MAGIC + "CARD");
        item.setItemMeta(meta);
        return item;
    }

    private void setupBackground() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        separator.setItemMeta(meta);

        final int DEALER_END_SLOT = DEALER_START_SLOT + 4;

        for (int i = 0; i < gameGUI.getSize(); i++) {
            if ((i >= 3 && i <= 5) || (i >= DEALER_START_SLOT && i <= DEALER_END_SLOT) || (i >= 38 && i <= 42) || i == 47 || i == 49 || i == 51) {
                continue;
            }

            if ((i >= 9 && i <= 17) || (i >= 27 && i <= 35) || (i >= 18 && i <= DEALER_START_SLOT - 1) || (i >= DEALER_END_SLOT + 1 && i <= 26)) {
                gameGUI.setItem(i, separator);
            } else {
                gameGUI.setItem(i, filler);
            }
        }
    }

    public Inventory getGameGUI() { return gameGUI; }
    public GameState getGameState() { return gameState; }
    public Player getPlayer() { return player; }
    public double getCurrentBet() { return currentBet; }
}