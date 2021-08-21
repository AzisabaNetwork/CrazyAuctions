package me.badbones69.crazyauctions.currency;

import me.badbones69.crazyauctions.api.FileManager.Files;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class Vault {
    
    public static Economy econ = null;
    public static EconomyResponse r;
    
    public static boolean hasVault() {
        return Bukkit.getServer().getPluginManager().getPlugin("Vault") != null;
    }
    
    public static boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    
    public static Long getMoney(Player player) {
        if (player != null) {
            try {
                return (long) econ.getBalance(player);
            } catch (NullPointerException ignore) {
            }
        }
        return 0L;
    }
    
    public static void removeMoney(Player player, Long amount) {
        econ.withdrawPlayer(player, amount);
    }
    
    public static void removeMoney(OfflinePlayer player, Long amount) {
        econ.withdrawPlayer(player, amount);
    }
    
    public static void addMoney(Player player, Long amount) {
        if (player.isOnline()) {
            econ.depositPlayer(player, amount);
        } else {
            Files.DATA.getFile().set("pendingDeposit." + player.getUniqueId(), Files.DATA.getFile().getLong("pendingDeposit." + player.getUniqueId(), 0) + amount);
            Files.DATA.saveFile();
        }
    }
    
    public static void addMoney(OfflinePlayer player, Long amount) {
        if (player.isOnline()) {
            econ.depositPlayer(player, amount);
        } else {
            Files.DATA.getFile().set("pendingDeposit." + player.getUniqueId(), Files.DATA.getFile().getLong("pendingDeposit." + player.getUniqueId(), 0) + amount);
            Files.DATA.saveFile();
        }
    }

    public static void addMoney(UUID uuid, Long amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            econ.depositPlayer(player, amount);
        } else {
            Files.DATA.getFile().set("pendingDeposit." + uuid, Files.DATA.getFile().getLong("pendingDeposit." + uuid, 0) + amount);
            Files.DATA.saveFile();
        }
    }
    
}