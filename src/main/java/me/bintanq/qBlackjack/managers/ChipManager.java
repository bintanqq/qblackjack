package me.bintanq.qBlackjack.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.bintanq.qBlackjack.QBlackjack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChipManager {
    private final QBlackjack plugin;
    private final File dataFile;
    private Map<UUID, Double> chipBalances;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ChipManager() {
        this.plugin = QBlackjack.getInstance();
        this.dataFile = new File(plugin.getDataFolder(), "chip_balances.json");
        this.chipBalances = new HashMap<>();
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().severe("Could not create chip_balances.json: " + e.getMessage());
            }
            return;
        }
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Double>>() {}.getType();
            Map<String, Double> jsonMap = gson.fromJson(reader, type);
            if (jsonMap != null) {
                this.chipBalances = jsonMap.entrySet().stream()
                        .collect(Collectors.toMap(entry -> UUID.fromString(entry.getKey()), Map.Entry::getValue));
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            plugin.getLogger().severe("Failed to load chip balances from JSON: " + e.getMessage());
            this.chipBalances = new HashMap<>();
        }
    }

    public void saveData() {
        Map<String, Double> jsonMap = chipBalances.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(jsonMap, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chip balances to JSON: " + e.getMessage());
        }
    }

    public double getBalance(UUID playerUUID) { return chipBalances.getOrDefault(playerUUID, 0.0); }
    public boolean has(UUID playerUUID, double amount) { return getBalance(playerUUID) >= amount; }

    public void deposit(UUID playerUUID, double amount) {
        if (amount <= 0) return;
        chipBalances.put(playerUUID, getBalance(playerUUID) + amount);
        saveData();
    }

    public boolean withdraw(UUID playerUUID, double amount) {
        if (amount <= 0 || !has(playerUUID, amount)) return false;
        chipBalances.put(playerUUID, getBalance(playerUUID) - amount);
        saveData();
        return true;
    }

    public void setBalance(UUID playerUUID, double amount) {
        if (amount < 0) amount = 0;
        chipBalances.put(playerUUID, amount);
        saveData();
    }
}