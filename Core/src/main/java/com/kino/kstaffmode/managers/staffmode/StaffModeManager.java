package com.kino.kstaffmode.managers.staffmode;

import com.kino.kore.utils.items.KMaterial;
import com.kino.kore.utils.items.builder.ItemBuilder;
import com.kino.kore.utils.messages.MessageUtils;
import com.kino.kstaffmode.factory.UtilsFactory;
import fr.minuskube.netherboard.Netherboard;
import fr.minuskube.netherboard.bukkit.BPlayerBoard;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@SuppressWarnings("deprecation")
public class StaffModeManager {

    private List<UUID> inStaffMode;
    private List<UUID> vanished;
    private List<UUID> inStaffChat;
    private List<UUID> fly;
    private List<UUID> frozen;
    private Map<UUID, ItemStack[]> armorItems;
    private Map<UUID, ItemStack[]> inventoryItems;
    private Map<ItemStack, Integer> staffModeItems;
    private ItemStack vanishedItem;
    private ItemStack notVanishedItem;

    private FileConfiguration messages;
    private FileConfiguration config;
    private FileConfiguration scoreboardFile;

    
    public StaffModeManager(FileConfiguration config, FileConfiguration messages,FileConfiguration scoreboardFile){
        this.config = config;
        this.messages = messages;
        this.scoreboardFile = scoreboardFile;
        this.inStaffMode = new ArrayList<>();
        this.vanished = new ArrayList<>();
        this.inStaffChat = new ArrayList<>();
        this.fly = new ArrayList<>();
        this.frozen = new ArrayList<>();
        this.armorItems = new HashMap<>();
        this.inventoryItems = new HashMap<>();
        this.staffModeItems = new HashMap<>();
        this.registerItems();
    }

    public void clearInventory(Player p){
        p.getInventory().clear();
        p.getInventory().setHelmet(new ItemStack(Material.AIR));
        p.getInventory().setChestplate(new ItemStack(Material.AIR));
        p.getInventory().setLeggings(new ItemStack(Material.AIR));
        p.getInventory().setBoots(new ItemStack(Material.AIR));
    }

    public void giveStaffItems(Player p){
        this.saveItems(p);
        this.clearInventory(p);
        for(ItemStack item : this.staffModeItems.keySet()){
            if(item.getType() == Material.getMaterial(config.getInt("staffItems.vanish.vanished.id"))){
                if(!isVanished(p)){
                    item = notVanishedItem;
                }
            }
            p.getInventory().setItem(this.staffModeItems.get(item), item);
        }
    }

    public void givePlayerItems(Player p){
        this.clearInventory(p);
        p.getInventory().setContents(this.inventoryItems.get(p.getUniqueId()));
        p.getInventory().setArmorContents(this.armorItems.get(p.getUniqueId()));
        this.inventoryItems.remove(p.getUniqueId());
        this.armorItems.remove(p.getUniqueId());
    }

    public void toogleFreeze (Player staff, Player p) {
        if(frozen.contains(p.getUniqueId())) {
            frozen.remove(p.getUniqueId());
            MessageUtils.sendMessage(p, messages.getString("unfrozen").replace("<player>", staff.getName()));
            MessageUtils.sendMessage(staff, messages.getString("unfreeze").replace("<player>", p.getName()));
        } else {
            if (!p.hasPermission("kstaffmode.bypass.freeze")) {
                frozen.add(p.getUniqueId());
                MessageUtils.sendMessage(p, messages.getString("frozen").replace("<player>", staff.getName()));
                MessageUtils.sendMessage(staff, messages.getString("freeze").replace("<player>", p.getName()));
            } else {
                MessageUtils.sendMessage(staff, messages.getString("noPerms"));
            }
        }
    }

    public void toogleStaffMode(Player p){
        if(p.hasPermission("kstaffmode.staffmode")){
            if(!this.inStaffMode.contains(p.getUniqueId())){
                this.inStaffMode.add(p.getUniqueId());
                this.vanished.add(p.getUniqueId());
                for(Player player : Bukkit.getOnlinePlayers()){
                    if(player.hasPermission("kstaffmode.bypass.vanish")){
                        player.showPlayer(p);
                    }else{
                        player.hidePlayer(p);
                    }
                }
                this.fly.add(p.getUniqueId());
                p.setAllowFlight(true);
                p.setFlying(true);
                this.giveStaffItems(p);
                createScoreboard(p);
                MessageUtils.sendMessage(p, messages.getString("enabledStaffMode"));
            }else{
                this.inStaffMode.remove(p.getUniqueId());
                this.vanished.remove(p.getUniqueId());
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.showPlayer(p);
                }
                this.fly.remove(p.getUniqueId());
                p.setAllowFlight(false);
                p.setFlying(false);
                this.givePlayerItems(p);
                board.delete();
                board = null;
                MessageUtils.sendMessage(p, messages.getString("disabledStaffMode"));
            }
        }
    }

    public void toogleStaffChat(Player p){
        if(p.hasPermission("kstaffmode.staffchat")){
            if(!inStaffChat.contains(p.getUniqueId())){
                inStaffChat.add(p.getUniqueId());
                MessageUtils.sendMessage(p, messages.getString("enabledStaffChat"));
            }else{
                inStaffChat.remove(p.getUniqueId());
                MessageUtils.sendMessage(p, messages.getString("disabledStaffChat"));
            }
            setStaffChatScore(p);
        }

    }

    public void toogleFly(Player p){
        if(p.hasPermission("kstaffmode.fly")){
            if(!fly.contains(p.getUniqueId())){
                fly.add(p.getUniqueId());
                p.setAllowFlight(true);
                p.setFlying(true);
                MessageUtils.sendMessage(p, messages.getString("enabledFly"));
            }else{
                fly.remove(p.getUniqueId());
                p.setAllowFlight(false);
                p.setFlying(false);
                MessageUtils.sendMessage(p, messages.getString("disabledFly"));
            }
        }
    }

    public void toogleVanish(Player p){
        if(p.hasPermission("kstaffmode.vanish")){
            if(!vanished.contains(p.getUniqueId())){
                vanished.add(p.getUniqueId());
                if(inStaffMode.contains(p.getUniqueId())) {
                    p.getInventory().setItem(config.getInt("staffItems.vanish.slot"), vanishedItem);
                }
                for(Player player : Bukkit.getOnlinePlayers()){
                    if(player.hasPermission("kstaffmode.bypass.vanish")){
                        player.showPlayer(p);
                    }else{
                        player.hidePlayer(p);
                    }
                }
                MessageUtils.sendMessage(p, messages.getString("enabledVanish"));
            }else{
                vanished.remove(p.getUniqueId());
                if(inStaffMode.contains(p.getUniqueId())) {
                    p.getInventory().setItem(config.getInt("staffItems.vanish.slot"), notVanishedItem);
                }
                for(Player player : Bukkit.getOnlinePlayers()){
                    player.showPlayer(p);
                }
                MessageUtils.sendMessage(p, messages.getString("disabledVanish"));
            }
            setVanishScore(p);
        }
    }

    public void teleportToRandomplayer(Player p, boolean multiworld) {
        Random r = new Random();

        ArrayList<Player> players = new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
        players.remove(p);

        if(players.size() > 0) {
            int index = r.nextInt(players.size());
            Player player = players.get(index);
            if(multiworld) {
                p.teleport(player.getLocation());
                MessageUtils.sendMessage(p, messages.getString("teleportedTo").replace("<player>", player.getName()));
            } else {
                if(player.getWorld().equals(p.getWorld())) {
                    p.teleport(player.getLocation());
                    MessageUtils.sendMessage(p, messages.getString("teleportedTo").replace("<player>", player.getName()));
                }
            }
        } else {
            MessageUtils.sendMessage(p, messages.getString("errorRandomTp"));
        }
    }

    public void saveItems(Player p){
        this.armorItems.put(p.getUniqueId(), p.getInventory().getArmorContents());
        this.inventoryItems.put(p.getUniqueId(), p.getInventory().getContents());
    }
    

    public boolean isInStaffMode(Player p){
        return inStaffMode.contains(p.getUniqueId());
    }

    public boolean isVanished(Player p){
        return vanished.contains(p.getUniqueId());
    }

    public boolean isInStaffChat(Player p){
        return inStaffChat.contains(p.getUniqueId());
    }

    public boolean canFly(Player p){
        return fly.contains(p.getUniqueId());
    }

    public List<UUID> getFrozen() {
        return frozen;
    }

    public boolean isFrozen(Player p){
        return frozen.contains(p.getUniqueId());
    }

    public List<UUID> getInStaffMode() {
        return inStaffMode;
    }

    public List<UUID> getFly() {
        return fly;
    }

    public List<UUID> getVanished() {
        return vanished;
    }

    public List<UUID> getInStaffChat() {
        return inStaffChat;
    }

    public Map<ItemStack, Integer> getStaffModeItems() {
        return staffModeItems;
    }

    public Map<UUID, ItemStack[]> getArmorItems() {
        return armorItems;
    }

    public Map<UUID, ItemStack[]> getInventoryItems() {
        return inventoryItems;
    }

    public void registerItems(){
        staffModeItems.clear();
        if(config.getBoolean("staffItems.navigator.enabled")) {
            ItemStack item = buildItem("navigator");
            staffModeItems.put(item, config.getInt("staffItems.navigator.slot"));
        }

        if(config.getBoolean("staffItems.staffList.enabled")) {
            ItemStack item = buildItem("staffList");
            staffModeItems.put(item, config.getInt("staffItems.staffList.slot"));
        }

        if(config.getBoolean("staffItems.worldEdit.enabled")) {
            ItemStack item = buildItem("worldEdit");
            staffModeItems.put(item, config.getInt("staffItems.worldEdit.slot"));
        }

        if(config.getBoolean("staffItems.inspect.enabled")) {
            ItemStack item = buildItem("inspect");
            staffModeItems.put(item, config.getInt("staffItems.inspect.slot"));
        }

        if(config.getBoolean("staffItems.freeze.enabled")) {
            ItemStack item = buildItem("freeze");
            staffModeItems.put(item, config.getInt("staffItems.freeze.slot"));
        }

        if(config.getBoolean("staffItems.fly.enabled")) {
            ItemStack item = buildItem("fly");
            staffModeItems.put(item, config.getInt("staffItems.fly.slot"));
        }

        if(config.getBoolean("staffItems.randomtp.enabled")) {
            ItemStack item = buildItem("randomtp");
            staffModeItems.put(item, config.getInt("staffItems.randomtp.slot"));
        }

        this.notVanishedItem = buildItem("vanish.visible");

        this.vanishedItem = buildItem("vanish.vanished");

        if(config.getBoolean("staffItems.vanish.enabled")) {
            staffModeItems.put(vanishedItem, config.getInt("staffItems.vanish.slot"));
        }
    }

    @Getter
    @Setter
    private BPlayerBoard board;

    public void createScoreboard(Player p){
        if(scoreboardFile.getBoolean("scoreboard.enabled")) {
            board = Netherboard.instance().createBoard(p, ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.title")));
            if (scoreboardFile.getBoolean("scoreboard.header.enabled")) {
                setHeader();
            }

            if (scoreboardFile.getBoolean("scoreboard.staffmode.enabled")) {
                setStaffModeScore();
            }

            if (scoreboardFile.getBoolean("scoreboard.vanish.enabled")) {
                setStaffChatScore(p);
            }

            if (scoreboardFile.getBoolean("scoreboard.staffchat.enabled")) {
                setVanishScore(p);
            }

            if (scoreboardFile.getBoolean("scoreboard.online.enabled")) {
                setOnline();
            }

            if (scoreboardFile.getBoolean("scoreboard.tps.enabled")) {
                setTPS();
            }

            if (scoreboardFile.getBoolean("scoreboard.footer.enabled")) {
                setFooter();
            }
        }

    }

    public void setHeader () {
        if(board !=null) {
            board.set(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.header.line")), 14);
        }
    }

    public void setStaffModeScore () {
        if(board !=null) {
            board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.staffmode.line")), 13);
        }
    }

    public void setVanishScore (Player p) {
        if(board !=null) {
            boolean b = isVanished(p);
            if (b) {
                board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.vanish.line").replace(
                        "<vanished>", "Vanished"
                ).replace("<color>", getColor("vanish", true))), 12);
            } else {
                board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.vanish.line").replace(
                        "<vanished>", "Visible"
                ).replace("<color>", getColor("vanish", false))), 12);
            }
        }
    }

    public void setStaffChatScore (Player p) {
        if(board !=null) {
            boolean b = isInStaffChat(p);
            if (b) {
                board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.staffchat.line").replace(
                        "<staffchat>", "StaffChat"
                ).replace("<color>", getColor("staffchat", true))), 11);
            } else {
                board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.staffchat.line").replace(
                        "<staffchat>", "Global"
                ).replace("<color>", getColor("staffchat", false))), 11);
            }
        }
    }

    public void setOnline () {
        if(board !=null) {
            board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.online.line").replace(
                    "<online>", Bukkit.getServer().getOnlinePlayers().size() + ""
            )), 10);
        }
    }

    public void setTPS() {
        if(board !=null) {
            board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.tps.line").replace(
                    "<tps>", UtilsFactory.getTPS() + ""
            )), 9);
        }
    }

    public void setFooter () {
        board.set(ChatColor.translateAlternateColorCodes('&', scoreboardFile.getString("scoreboard.footer.line")), 8);
    }



    private String getColor(String key, boolean enabled){
        String lettere = scoreboardFile.getString("scoreboard." + key + ".enabled-color");
        String letterd = scoreboardFile.getString("scoreboard." + key + ".disabled-color");

        return enabled ? "&" + lettere : "&" + letterd;
    }

    private ItemStack buildItem (String key) {
        return config.getString("staffItems." + key + ".skull.type").equalsIgnoreCase("OWNER") ?
                ItemBuilder.newSkullBuilder(KMaterial.PLAYER_HEAD.name(), config.getInt("staffItems." + key + ".amount"))
                        .owner(config.getString("staffItems." + key + ".skull.owner"))
                        .name(config.getString("staffItems." + key + ".name"))
                        .lore(config.getStringList("staffItems." + key + ".lore")).build()
                : config.getString("staffItems." + key + ".skull.type").equalsIgnoreCase("URL") ?

                ItemBuilder.newSkullBuilder(KMaterial.PLAYER_HEAD.name(), config.getInt("staffItems." + key + ".amount"))
                        .url(config.getString("staffItems." + key + ".skull.owner"))
                        .name(config.getString("staffItems." + key + ".name"))
                        .lore(config.getStringList("staffItems." + key + ".lore")).build()

                : ItemBuilder.newBuilder(config.getString("staffItems." + key + ".id"), config.getInt("staffItems." + key + ".amount"))
                .name(config.getString("staffItems." + key + ".name"))
                .lore(config.getStringList("staffItems." + key + ".lore"))
                .build();
    }
}
