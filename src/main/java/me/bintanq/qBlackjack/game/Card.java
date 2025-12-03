package me.bintanq.qBlackjack.game;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private final String rank;
    private final String suit;
    private final int value;
    private final ItemStack itemStack;

    public Card(String rank, String suit, int value) {
        this.rank = rank;
        this.suit = suit;
        this.value = value;
        this.itemStack = createCardItem(rank, suit, value);
    }

    private ItemStack createCardItem(String rank, String suit, int value) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();

        // MENGHILANGKAN SIMBOL UNICODE DAN MENGGANTINYA DENGAN KODE WARNA/TEKS
        String color = getSuitColor(suit);
        String suitName = suit.toUpperCase();

        // Nama item hanya menggunakan Rank dan Suit Name, menghilangkan simbol kotak
        meta.setDisplayName(color + rank + " of " + suitName + ChatColor.RESET + ChatColor.GRAY + " (Value: " + value + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Suit: " + suit);
        meta.setLore(lore);

        paper.setItemMeta(meta);
        return paper;
    }

    private String getSuitColor(String suit) {
        if (suit.equalsIgnoreCase("Hearts") || suit.equalsIgnoreCase("Diamonds")) {
            return ChatColor.RED.toString();
        }
        return ChatColor.DARK_GRAY.toString();
    }

    public String getRank() { return rank; }
    public int getValue() { return value; }
    public ItemStack getItemStack() { return itemStack; }
}