package com.kino.kstaffmode;

import com.kino.kore.utils.PluginUtils;
import com.kino.kstaffmode.commands.*;
import com.kino.kstaffmode.commands.gamemode.GameModeExecutor;
import com.kino.kstaffmode.listener.ChatListener;
import com.kino.kstaffmode.listener.JoinListener;
import com.kino.kstaffmode.listener.LeaveListener;
import com.kino.kstaffmode.listener.MoveListener;
import com.kino.kstaffmode.listener.staffmode.InventoryListener;
import com.kino.kstaffmode.commands.kstaffmode.KStaffModeExecutor;
import com.kino.kstaffmode.listener.staffmode.ItemsInteractListener;
import com.kino.kstaffmode.listener.staffmode.StaffModeBasicListener;
import com.kino.kstaffmode.managers.files.DataManager;
import com.kino.kstaffmode.managers.files.FilesManager;
import com.kino.kstaffmode.managers.files.PlayerDataManager;
import com.kino.kstaffmode.managers.menus.MenuManager;
import com.kino.kstaffmode.managers.staffmode.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;



public final class KStaffMode extends JavaPlugin {

    private StaffModeManager staffModeManager;
    private MenuManager menuManager;
    private DataManager dataManager;
    private PlayerDataManager playerDataManager;
    private FilesManager filesManager;


    @Override
    public void onEnable() {
        this.registerClasses();
        new TaskLoader(this, staffModeManager).load();
        this.registerListeners();
        this.registerCommands();
        PluginUtils.sendEnableMessages(this);
    }

    @Override
    public void onDisable() {
        this.dataManager.saveData();
        PluginUtils.sendDisableMessages(this);
    }



    private void registerListeners(){
        Bukkit.getServer().getPluginManager().registerEvents(new LeaveListener(staffModeManager, filesManager.getMessages(), getConfig(), playerDataManager), this);
        Bukkit.getServer().getPluginManager().registerEvents(new JoinListener(staffModeManager, playerDataManager), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ChatListener(filesManager.getMessages(), staffModeManager), this);
        Bukkit.getServer().getPluginManager().registerEvents(new StaffModeBasicListener(staffModeManager), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ItemsInteractListener(staffModeManager, menuManager, getConfig(), filesManager.getMessages()), this);
        Bukkit.getServer().getPluginManager().registerEvents(new InventoryListener(getConfig(), menuManager), this);
        Bukkit.getServer().getPluginManager().registerEvents(new MoveListener(staffModeManager, filesManager.getMessages()), this);
    }

    private void registerCommands(){
        getCommand("kstaffmode").setExecutor(new KStaffModeExecutor(this, filesManager, staffModeManager));
        getCommand("gm").setExecutor(new GameModeExecutor(filesManager.getMessages()));
        getCommand("staff").setExecutor(new StaffModeCommand(staffModeManager, filesManager.getMessages()));
        getCommand("vanish").setExecutor(new VanishCommand(staffModeManager, filesManager.getMessages()));
        getCommand("fly").setExecutor(new FlyCommand(staffModeManager, filesManager.getMessages()));
        getCommand("staffchat").setExecutor(new StaffChatCommand(staffModeManager, filesManager.getMessages()));
        getCommand("s").setExecutor(new SCommand(filesManager.getMessages()));
        getCommand("invsee").setExecutor(new InvSeeCommand(filesManager.getMessages(), menuManager));
        getCommand("freeze").setExecutor(new FreezeCommand(staffModeManager, filesManager.getMessages()));
    }


    private void registerClasses(){
        this.filesManager = new FilesManager(this);
        filesManager.start();
        this.staffModeManager = new StaffModeManager(getConfig(), filesManager.getMessages(), filesManager.getScoreboard());
        this.menuManager = new MenuManager(getConfig(), staffModeManager, menuManager);
        this.dataManager = new DataManager(filesManager, getConfig(), staffModeManager);
        dataManager.startManager();
        this.playerDataManager = new PlayerDataManager(dataManager, staffModeManager);
    }

}
