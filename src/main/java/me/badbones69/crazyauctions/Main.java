package me.badbones69.crazyauctions;

import me.badbones69.crazyauctions.api.*;
import me.badbones69.crazyauctions.api.FileManager.Files;
import me.badbones69.crazyauctions.api.events.AuctionListEvent;
import me.badbones69.crazyauctions.controllers.GUI;
import me.badbones69.crazyauctions.currency.CurrencyManager;
import me.badbones69.crazyauctions.currency.Vault;
import me.badbones69.crazyauctions.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    public static FileManager fileManager = FileManager.getInstance();
    public static CrazyAuctions crazyAuctions = CrazyAuctions.getInstance();

    @Override
    public void onEnable() {
        fileManager.logInfo(true).setup(this);
        crazyAuctions.loadCrazyAuctions();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getPluginManager().registerEvents(new GUI(), this);
        Methods.updateAuction();
        startCheck();
        if (!Vault.setupEconomy()) {
            saveDefaultConfig();
        }
        Messages.addMissingMessages();
    }

    @Override
    public void onDisable() {
        int file = 0;
        Bukkit.getScheduler().cancelTask(file);
        Files.DATA.saveFile();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!e.getPlayer().isOnline()) return;
            FileConfiguration config = Files.DATA.getFile();
            long amount = config.getLong("pendingDeposit." + e.getPlayer().getUniqueId(), 0);
            if (amount <= 0) return;
            config.set("pendingDeposit." + e.getPlayer().getUniqueId(), null);
            Files.DATA.saveFile();
            CurrencyManager.addMoney(e.getPlayer(), amount);
            getLogger().info("Given $" + amount + " to " + e.getPlayer().getName() + " (trigger: pendingDeposit)");
        }, 400);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLable, String[] args) {
        if (commandLable.equalsIgnoreCase("CrazyAuctions") || commandLable.equalsIgnoreCase("CrazyAuction") || commandLable.equalsIgnoreCase("CA") || commandLable.equalsIgnoreCase("AH") || commandLable.equalsIgnoreCase("HDV")) {
            if (args.length == 0) {
                if (!Methods.hasPermission(sender, "Access")) return true;
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.PLAYERS_ONLY.getMessage());
                    return true;
                }
                Player player = (Player) sender;
                if (Files.CONFIG.getFile().contains("Settings.Category-Page-Opens-First")) {
                    if (Files.CONFIG.getFile().getBoolean("Settings.Category-Page-Opens-First")) {
                        GUI.openCategories(player, ShopType.SELL);
                        return true;
                    }
                }
                if (crazyAuctions.isSellingEnabled()) {
                    GUI.openShop(player, ShopType.SELL, Category.NONE, 1);
                } else if (crazyAuctions.isBiddingEnabled()) {
                    GUI.openShop(player, ShopType.BID, Category.NONE, 1);
                } else {
                    player.sendMessage(Methods.getPrefix() + Methods.color("&cThe bidding and selling options are both disabled. Please contact the admin about this."));
                }
                return true;
            }
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("Help")) {// CA Help
                    if (!Methods.hasPermission(sender, "Access")) return true;
                    sender.sendMessage(Messages.HELP.getMessage());
                    return true;
                }
                if (args[0].equalsIgnoreCase("test")) {// CA test [times]
                    if (!Methods.hasPermission(sender, "test")) return true;
                    int times = 1;
                    if (args.length >= 2) {
                        if (!Methods.isInt(args[1])) {
                            HashMap<String, String> placeholders = new HashMap<>();
                            placeholders.put("%Arg%", args[1]);
                            placeholders.put("%arg%", args[1]);
                            sender.sendMessage(Messages.NOT_A_NUMBER.getMessage(placeholders));
                            return true;
                        }
                        times = Integer.parseInt(args[1]);
                    }
                    int price = 10;
                    int amount = 1;
                    ItemStack item = Methods.getItemInHand((Player) sender);
                    if (item != null && item.getType() != Material.AIR) {
                        // For testing as another player
                        String seller = ((Player) sender).getUniqueId().toString();
                        for (int it = 1; it <= times; it++) {
                            int num = 1;
                            Random r = new Random();
                            for (; Files.DATA.getFile().contains("Items." + num); num++) ;
                            Files.DATA.getFile().set("Items." + num + ".Price", price);
                            Files.DATA.getFile().set("Items." + num + ".Seller", seller);
                            if (args[0].equalsIgnoreCase("Bid")) {
                                Files.DATA.getFile().set("Items." + num + ".Time-Till-Expire", Methods.convertToMill(Files.CONFIG.getFile().getString("Settings.Bid-Time")));
                            } else {
                                Files.DATA.getFile().set("Items." + num + ".Time-Till-Expire", Methods.convertToMill(Files.CONFIG.getFile().getString("Settings.Sell-Time")));
                            }
                            Files.DATA.getFile().set("Items." + num + ".Full-Time", Methods.convertToMill(Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")));
                            int id = r.nextInt(Integer.MAX_VALUE);
                            for (String i : Files.DATA.getFile().getConfigurationSection("Items").getKeys(false))
                                if (Files.DATA.getFile().getInt("Items." + i + ".StoreID") == id) id = r.nextInt(Integer.MAX_VALUE);
                            Files.DATA.getFile().set("Items." + num + ".StoreID", id);
                            ShopType type = ShopType.SELL;
                            Files.DATA.getFile().set("Items." + num + ".Biddable", args[0].equalsIgnoreCase("Bid"));
                            Files.DATA.getFile().set("Items." + num + ".TopBidder", "None");
                            ItemStack I = item.clone();
                            I.setAmount(amount);
                            Files.DATA.getFile().set("Items." + num + ".Item", I);
                        }
                        Files.DATA.saveFile();
                        HashMap<String, String> placeholders = new HashMap<>();
                        placeholders.put("%Price%", price + "");
                        placeholders.put("%price%", price + "");
                        sender.sendMessage(Messages.ADDED_ITEM_TO_AUCTION.getMessage(placeholders));
                        if (item.getAmount() <= 1 || (item.getAmount() - amount) <= 0) {
                            Methods.setItemInHand((Player) sender, new ItemStack(Material.AIR));
                        } else {
                            item.setAmount(item.getAmount() - amount);
                        }
                    } else {
                        sender.sendMessage(Messages.DOSENT_HAVE_ITEM_IN_HAND.getMessage());
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("Reload")) {// CA Reload
                    if (!Methods.hasPermission(sender, "Admin")) return true;
                    fileManager.logInfo(true).setup(this);
                    crazyAuctions.loadCrazyAuctions();
                    sender.sendMessage(Messages.RELOAD.getMessage());
                    return true;
                }
                if (args[0].equalsIgnoreCase("View")) {// CA View <Player>
                    if (!Methods.hasPermission(sender, "View")) return true;
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYERS_ONLY.getMessage());
                        return true;
                    }
                    if (args.length >= 2) {
                        Player player = (Player) sender;
                        GUI.openViewer(player, args[1], 1);
                        return true;
                    }
                    sender.sendMessage(Messages.CRAZYAUCTIONS_VIEW.getMessage());
                    return true;
                }
                if (args[0].equalsIgnoreCase("Expired") || args[0].equalsIgnoreCase("Collect")) {// CA Expired
                    if (!Methods.hasPermission(sender, "Access")) return true;
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYERS_ONLY.getMessage());
                        return true;
                    }
                    Player player = (Player) sender;
                    GUI.openPlayersExpiredList(player, 1);
                    return true;
                }
                if (args[0].equalsIgnoreCase("Listed")) {// CA Listed
                    if (!Methods.hasPermission(sender, "Access")) return true;
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYERS_ONLY.getMessage());
                        return true;
                    }
                    Player player = (Player) sender;
                    GUI.openPlayersCurrentList(player, 1);
                    return true;
                }
                if (args[0].equalsIgnoreCase("Sell") || args[0].equalsIgnoreCase("Bid")) {// /CA Sell/Bid <Price> [Amount of Items]
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYERS_ONLY.getMessage());
                        return true;
                    }
                    if (args.length >= 2) {
                        Player player = (Player) sender;
                        if (args[0].equalsIgnoreCase("Sell")) {
                            if (!crazyAuctions.isSellingEnabled()) {
                                player.sendMessage(Messages.SELLING_DISABLED.getMessage());
                                return true;
                            }
                            if (!Methods.hasPermission(player, "Sell")) return true;
                        }
                        if (args[0].equalsIgnoreCase("Bid")) {
                            if (!crazyAuctions.isBiddingEnabled()) {
                                player.sendMessage(Messages.BIDDING_DISABLED.getMessage());
                                return true;
                            }
                            if (!Methods.hasPermission(player, "Bid")) return true;
                        }
                        ItemStack item = Methods.getItemInHand(player);
                        if (item.getType().name().contains("SHULKER_BOX")) {
                            player.sendMessage(Messages.BOOK_NOT_ALLOWED.getMessage());
                            return true;
                        }
                        int amount = item.getAmount();
                        if (args.length >= 3) {
                            if (!Methods.isInt(args[2])) {
                                HashMap<String, String> placeholders = new HashMap<>();
                                placeholders.put("%Arg%", args[2]);
                                placeholders.put("%arg%", args[2]);
                                player.sendMessage(Messages.NOT_A_NUMBER.getMessage(placeholders));
                                return true;
                            }
                            amount = Integer.parseInt(args[2]);
                            if (amount <= 0) amount = 1;
                            if (amount > item.getAmount()) amount = item.getAmount();
                        }
                        if (Methods.getItemInHand(player).getType() == Material.AIR) {
                            player.sendMessage(Messages.DOSENT_HAVE_ITEM_IN_HAND.getMessage());
                            return false;
                        }
                        long price;
                        try {
                            price = fromFriendlyString(args[1]);
                        } catch (RuntimeException e) {
                            try {
                                price = Long.parseLong(args[1]);
                            } catch (NumberFormatException ex2) {
                                HashMap<String, String> placeholders = new HashMap<>();
                                placeholders.put("%Arg%", args[1]);
                                placeholders.put("%arg%", args[1]);
                                player.sendMessage(Messages.NOT_A_NUMBER.getMessage(placeholders));
                                return true;
                            }
                        }
                        if (args[0].equalsIgnoreCase("Bid")) {
                            if (price < Files.CONFIG.getFile().getLong("Settings.Minimum-Bid-Price")) {
                                player.sendMessage(Messages.BID_PRICE_TO_LOW.getMessage());
                                return true;
                            }
                            if (price > Files.CONFIG.getFile().getLong("Settings.Max-Beginning-Bid-Price")) {
                                player.sendMessage(Messages.BID_PRICE_TO_HIGH.getMessage());
                                return true;
                            }
                        } else {
                            if (price < Files.CONFIG.getFile().getLong("Settings.Minimum-Sell-Price")) {
                                player.sendMessage(Messages.SELL_PRICE_TO_LOW.getMessage());
                                return true;
                            }
                            if (price > Files.CONFIG.getFile().getLong("Settings.Max-Beginning-Sell-Price")) {
                                player.sendMessage(Messages.SELL_PRICE_TO_HIGH.getMessage());
                                return true;
                            }
                        }
                        if (!player.hasPermission("crazyauctions.bypass")) {
                            int SellLimit = 0;
                            int BidLimit = 0;
                            for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
                                String perm = permission.getPermission();
                                if (perm.startsWith("crazyauctions.sell.")) {
                                    perm = perm.replace("crazyauctions.sell.", "");
                                    if (Methods.isInt(perm)) {
                                        if (Integer.parseInt(perm) > SellLimit) {
                                            SellLimit = Integer.parseInt(perm);
                                        }
                                    }
                                }
                                if (perm.startsWith("crazyauctions.bid.")) {
                                    perm = perm.replace("crazyauctions.bid.", "");
                                    if (Methods.isInt(perm)) {
                                        if (Integer.parseInt(perm) > BidLimit) {
                                            BidLimit = Integer.parseInt(perm);
                                        }
                                    }
                                }
                            }
                            for (int i = 1; i < 100; i++) {
                                if (SellLimit < i) {
                                    if (player.hasPermission("crazyauctions.sell." + i)) {
                                        SellLimit = i;
                                    }
                                }
                                if (BidLimit < i) {
                                    if (player.hasPermission("crazyauctions.bid." + i)) {
                                        BidLimit = i;
                                    }
                                }
                            }
                            if (args[0].equalsIgnoreCase("Sell")) {
                                if (crazyAuctions.getItems(player, ShopType.SELL).size() >= SellLimit) {
                                    player.sendMessage(Messages.MAX_ITEMS.getMessage());
                                    return true;
                                }
                            }
                            if (args[0].equalsIgnoreCase("Bid")) {
                                if (crazyAuctions.getItems(player, ShopType.BID).size() >= BidLimit) {
                                    player.sendMessage(Messages.MAX_ITEMS.getMessage());
                                    return true;
                                }
                            }
                        }
                        for (String id : Files.CONFIG.getFile().getStringList("Settings.BlackList")) {
                            if (item.getType() == Methods.makeItem(id, 1).getType()) {
                                player.sendMessage(Messages.ITEM_BLACKLISTED.getMessage());
                                return true;
                            }
                        }
                        if (!Files.CONFIG.getFile().getBoolean("Settings.Allow-Damaged-Items")) {
                            for (Material i : getDamageableItems()) {
                                if (item.getType() == i) {
                                    if (item.getDurability() > 0) {
                                        player.sendMessage(Messages.ITEM_DAMAGED.getMessage());
                                        return true;
                                    }
                                }
                            }
                        }
                        if (!allowBook(item)) {
                            player.sendMessage(Messages.BOOK_NOT_ALLOWED.getMessage());
                            return true;
                        }
                        String seller = player.getUniqueId().toString();
                        // For testing as another player
                        //String seller = "Test-Account";
                        int num = 1;
                        Random r = new Random();
                        for (; Files.DATA.getFile().contains("Items." + num); num++) ;
                        Files.DATA.getFile().set("Items." + num + ".Price", price);
                        Files.DATA.getFile().set("Items." + num + ".Seller", seller);
                        if (args[0].equalsIgnoreCase("Bid")) {
                            Files.DATA.getFile().set("Items." + num + ".Time-Till-Expire", Methods.convertToMill(Files.CONFIG.getFile().getString("Settings.Bid-Time")));
                        } else {
                            Files.DATA.getFile().set("Items." + num + ".Time-Till-Expire", Methods.convertToMill(Files.CONFIG.getFile().getString("Settings.Sell-Time")));
                        }
                        Files.DATA.getFile().set("Items." + num + ".Full-Time", Methods.convertToMill(Files.CONFIG.getFile().getString("Settings.Full-Expire-Time")));
                        int id = r.nextInt(999999);
                        // Runs 3x to check for same ID.
                        for (String i : Files.DATA.getFile().getConfigurationSection("Items").getKeys(false))
                            if (Files.DATA.getFile().getInt("Items." + i + ".StoreID") == id) id = r.nextInt(Integer.MAX_VALUE);
                        for (String i : Files.DATA.getFile().getConfigurationSection("Items").getKeys(false))
                            if (Files.DATA.getFile().getInt("Items." + i + ".StoreID") == id) id = r.nextInt(Integer.MAX_VALUE);
                        for (String i : Files.DATA.getFile().getConfigurationSection("Items").getKeys(false))
                            if (Files.DATA.getFile().getInt("Items." + i + ".StoreID") == id) id = r.nextInt(Integer.MAX_VALUE);
                        Files.DATA.getFile().set("Items." + num + ".StoreID", id);
                        ShopType type = ShopType.SELL;
                        if (args[0].equalsIgnoreCase("Bid")) {
                            Files.DATA.getFile().set("Items." + num + ".Biddable", true);
                            type = ShopType.BID;
                        } else {
                            Files.DATA.getFile().set("Items." + num + ".Biddable", false);
                        }
                        Files.DATA.getFile().set("Items." + num + ".TopBidder", "None");
                        ItemStack stack = item.clone();
                        stack.setAmount(amount);
                        Files.DATA.getFile().set("Items." + num + ".Item", stack);
                        Files.DATA.getFile().set("Items." + num + ".ItemBytes", Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
                        Files.DATA.saveFile();
                        Bukkit.getPluginManager().callEvent(new AuctionListEvent(player, type, stack, price));
                        getLogger().info("Added item to auction by " + player.getName() + " for $" + price);
                        ItemUtil.log(getLogger(), stack);
                        HashMap<String, String> placeholders = new HashMap<>();
                        String priceFormatted = formatPrice(price) + " (" + toFriendlyString(price) + ")";
                        placeholders.put("%Price%", priceFormatted);
                        placeholders.put("%price%", priceFormatted);
                        player.sendMessage(Messages.ADDED_ITEM_TO_AUCTION.getMessage(placeholders));
                        if (item.getAmount() <= 1 || (item.getAmount() - amount) <= 0) {
                            Methods.setItemInHand(player, new ItemStack(Material.AIR));
                        } else {
                            item.setAmount(item.getAmount() - amount);
                        }
                        return false;
                    }
                    sender.sendMessage(Messages.CRAZYAUCTIONS_SELL_BID.getMessage());
                    return true;
                }
            }
        }
        sender.sendMessage(Messages.CRAZYAUCTIONS_HELP.getMessage());
        return false;
    }

    private void startCheck() {
        Bukkit.getScheduler().runTaskTimer(this, Methods::updateAuction, 20, 5 * 20);
    }

    private ArrayList<Material> getDamageableItems() {
        ArrayList<Material> ma = new ArrayList<>();
        if (Version.isNewer(Version.v1_12_R1)) {
            ma.add(Material.matchMaterial("GOLDEN_HELMET"));
            ma.add(Material.matchMaterial("GOLDEN_CHESTPLATE"));
            ma.add(Material.matchMaterial("GOLDEN_LEGGINGS"));
            ma.add(Material.matchMaterial("GOLDEN_BOOTS"));
            ma.add(Material.matchMaterial("WOODEN_SWORD"));
            ma.add(Material.matchMaterial("WOODEN_AXE"));
            ma.add(Material.matchMaterial("WOODEN_PICKAXE"));
            ma.add(Material.matchMaterial("WOODEN_AXE"));
            ma.add(Material.matchMaterial("WOODEN_SHOVEL"));
            ma.add(Material.matchMaterial("STONE_SHOVEL"));
            ma.add(Material.matchMaterial("IRON_SHOVEL"));
            ma.add(Material.matchMaterial("DIAMOND_SHOVEL"));
            ma.add(Material.matchMaterial("WOODEN_HOE"));
            ma.add(Material.matchMaterial("GOLDEN_HOE"));
            ma.add(Material.matchMaterial("CROSSBOW"));
            ma.add(Material.matchMaterial("TRIDENT"));
            ma.add(Material.matchMaterial("TURTLE_HELMET"));
        } else {
            ma.add(Material.matchMaterial("GOLD_HELMET"));
            ma.add(Material.matchMaterial("GOLD_CHESTPLATE"));
            ma.add(Material.matchMaterial("GOLD_LEGGINGS"));
            ma.add(Material.matchMaterial("GOLD_BOOTS"));
            ma.add(Material.matchMaterial("WOOD_SWORD"));
            ma.add(Material.matchMaterial("WOOD_AXE"));
            ma.add(Material.matchMaterial("WOOD_PICKAXE"));
            ma.add(Material.matchMaterial("WOOD_AXE"));
            ma.add(Material.matchMaterial("WOOD_SPADE"));
            ma.add(Material.matchMaterial("STONE_SPADE"));
            ma.add(Material.matchMaterial("IRON_SPADE"));
            ma.add(Material.matchMaterial("DIAMOND_SPADE"));
            ma.add(Material.matchMaterial("WOOD_HOE"));
            ma.add(Material.matchMaterial("GOLD_HOE"));
        }
        ma.add(Material.DIAMOND_HELMET);
        ma.add(Material.DIAMOND_CHESTPLATE);
        ma.add(Material.DIAMOND_LEGGINGS);
        ma.add(Material.DIAMOND_BOOTS);
        ma.add(Material.CHAINMAIL_HELMET);
        ma.add(Material.CHAINMAIL_CHESTPLATE);
        ma.add(Material.CHAINMAIL_LEGGINGS);
        ma.add(Material.CHAINMAIL_BOOTS);
        ma.add(Material.IRON_HELMET);
        ma.add(Material.IRON_CHESTPLATE);
        ma.add(Material.IRON_LEGGINGS);
        ma.add(Material.IRON_BOOTS);
        ma.add(Material.LEATHER_HELMET);
        ma.add(Material.LEATHER_CHESTPLATE);
        ma.add(Material.LEATHER_LEGGINGS);
        ma.add(Material.LEATHER_BOOTS);
        ma.add(Material.BOW);
        ma.add(Material.STONE_SWORD);
        ma.add(Material.IRON_SWORD);
        ma.add(Material.DIAMOND_SWORD);
        ma.add(Material.STONE_AXE);
        ma.add(Material.IRON_AXE);
        ma.add(Material.DIAMOND_AXE);
        ma.add(Material.STONE_PICKAXE);
        ma.add(Material.IRON_PICKAXE);
        ma.add(Material.DIAMOND_PICKAXE);
        ma.add(Material.STONE_AXE);
        ma.add(Material.IRON_AXE);
        ma.add(Material.DIAMOND_AXE);
        ma.add(Material.STONE_HOE);
        ma.add(Material.IRON_HOE);
        ma.add(Material.DIAMOND_HOE);
        ma.add(Material.FLINT_AND_STEEL);
        ma.add(Material.ANVIL);
        ma.add(Material.FISHING_ROD);
        return ma;
    }

    private boolean allowBook(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof BookMeta) {
            getLogger().info("Checking " + item.getType() + " for illegal unicode.");
            try {
                Files.TEST_FILE.getFile().set("Test", item);
                Files.TEST_FILE.saveFile();
                getLogger().info(item.getType() + " has passed unicode checks.");
            } catch (YAMLException e) {
                getLogger().info(item.getType() + " has failed unicode checks and has been denied.");
                return false;
            }
            return ((BookMeta) item.getItemMeta()).getPages().stream().mapToInt(String :: length).sum() < 2000;
        }
        return true;
    }

    public Material getMaterial(String newMaterial, String oldMaterial) {
        return Material.matchMaterial(Version.isNewer(Version.v1_12_R1) ? newMaterial : oldMaterial);
    }

    public static String formatPrice(long l) {
        // this code is very inefficient right?
        String preFormatted = String.format("%,d", l);
        if (l >= 1_000_000_000_000_000_000L) {
            return "§5" + preFormatted;
        } else if (l >= 1_000_000_000_000_000L) {
            return "§6" + preFormatted;
        } else if (l >= 1_000_000_000_000L) {
            return "§d" + preFormatted;
        } else if (l >= 1_000_000_000L) {
            return "§b" + preFormatted;
        } else if (l >= 1_000_000L) {
            return "§a" + preFormatted;
        } else {
            return "§f" + preFormatted;
        }
    }

    public static String toFriendlyString(long number) {
        List<String> suffixes = Arrays.asList("", "", "万", "億", "兆", "京");
        double suffixNum = Math.ceil(("" + number).length() / 4.0);
        double shortValue = Math.floor(number / Math.pow(10000.0, suffixNum - 1) * 100) / 100;
        String suffix = suffixes.get((int) suffixNum);
        if (((long) shortValue) == shortValue) {
            return String.format("%,.0f", shortValue) + suffix;
        }
        return shortValue + suffix;
    }

    public static long fromFriendlyString(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.replace(" ", "");
        long multiplier = 1;
        if (s.endsWith("万")) {
            multiplier = 10000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("億")) {
            multiplier = 100000000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("兆")) {
            multiplier = 1000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("京")) {
            multiplier = 10000000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("k") || s.endsWith("K")) {
            multiplier = 1000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("m") || s.endsWith("M")) {
            multiplier = 1000000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("b") || s.endsWith("B")) {
            multiplier = 1000000000;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("t") || s.endsWith("T")) {
            multiplier = 1000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("q")) {
            multiplier = 1000000000000000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("Q")) {
            multiplier = 1000000000000000000L;
            s = s.substring(0, s.length() - 1);
        }
        return (long) (Double.parseDouble(s) * multiplier);
    }
}
