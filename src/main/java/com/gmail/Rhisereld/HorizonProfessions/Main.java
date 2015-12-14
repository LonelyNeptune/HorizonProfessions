package com.gmail.Rhisereld.HorizonProfessions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin
{
	public static Permission perms = null;		//Reference to permission object from Vault.
	long time = 0;								//Time of last fatigue update.
	
	ConfigAccessor config;						//Configuration file.
	ConfigAccessor data;						//Data file.
	
	Set<UUID> claimNotified;
	
	/**
	 * onEnable() is called when the server is started or the plugin is enabled.
	 * It should contain everything that the plugin needs for its initial setup.
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
	 */
    @Override
    public void onEnable() 
    {
    	claimNotified = new HashSet<UUID>();
    	
    	//Setup files for configuration and data storage.
    	config = new ConfigAccessor(this, "config.yml");
    	data = new ConfigAccessor(this, "data.yml");
    	
    	//Load configuration
    	config.saveDefaultConfig();

    	//Vault integration for permissions
        if (!setupPermissions()) 
        {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        //Setup permission groups
        List<String> professions = config.getConfig().getStringList("professions");
        List<String> tiers = new ArrayList<String>();
		
		for (String t: config.getConfig().getConfigurationSection("tiers").getKeys(false))
			tiers.add(config.getConfig().getString("tiers." + t + ".name"));
        
		for (String p: professions)
			for (String t: tiers)
				perms.groupAdd((String) null, p + "-" + t, config.getConfig().getString("permission_prefix") + "." + p + "." + t);
		
		//Add online players to groups
		ProfessionStats prof;
		for (Player pl: Bukkit.getOnlinePlayers())
		{
			prof = new ProfessionStats(perms, data.getConfig(), config.getConfig(), pl.getUniqueId());
			for (String pr: professions)
				perms.playerAddGroup((String) null, pl, pr + "-" + prof.getTierName(prof.getTier(pr)));
		}
        
        //RecipeManager integration for recipes.
        if (getServer().getPluginManager().isPluginEnabled("RecipeManager"))
        {
        	getLogger().info("RecipeManager hooked, recipe support enabled.");
            getServer().getPluginManager().registerEvents(new CraftListener(perms, data.getConfig(), config.getConfig()), this);
        }
        else
        	getLogger().severe(String.format("Recipe support disabled due to no RecipeManager dependency found!", getDescription().getName()));
        
        //Listeners and commands.
        getServer().getPluginManager().registerEvents(new ProfessionListener(this, perms, data.getConfig(), config.getConfig()), this);
    	this.getCommand("profession").setExecutor(new ProfessionCommandExecutor(this, perms, data.getConfig(), config.getConfig()));
    	
    	//Save every 30 minutes.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public void run() 
			{
				data.saveConfig();
			}			
		} , 36000, 36000);
		
		//Reduce fatigue time.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public void run() 
			{
				updateFatigue();
			}			
		} , 20, 12000);
    }

	/**
     * onDisable() is called when the server shuts down or the plugin is disabled.
     * It should contain all the cleanup and data saving that the plugin needs to do before it is disabled.
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
	@Override
    public void onDisable() 
    {
		//Remove players from groups.
		List<String> professions = config.getConfig().getStringList("professions");
        List<String> tiers = new ArrayList<String>();
		
		for (String t: config.getConfig().getConfigurationSection("tiers").getKeys(false))
			tiers.add(config.getConfig().getString("tiers." + t + ".name"));
		
		for (Player pl: Bukkit.getOnlinePlayers())
			for (String pr: professions)
				for (String t: tiers)
					perms.playerRemoveGroup(pl, pr + "-" + t);
		
    	data.saveConfig();
    	config = null;
    	data = null;
    	claimNotified = null;
    	perms = null;
    }
    
	/**
	 * setupPermissions() sets up Vault permissions integration which allows this plugin to communicate with
	 * permissions plugins in a standardised fashion.
	 * 
	 * @return
	 */
    private boolean setupPermissions() 
    {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    
    /**
     * updateFatigue() is called periodically (every 10 minutes) to update the fatigue values of all players.
	 * Fatigue begins at FATIGUE_TIME (milliseconds) and decreases over time until it reaches zero.
	 * Until fatigue reaches zero, players are prevented from gaining any experience.
     */
	private void updateFatigue() 
    {    	
    	long timeDifference;
    	int practiceFatigue, instructionFatigue;
    	
    	//Try loading from config
    	if (time == 0)
    		time = data.getConfig().getLong("lasttimeupdated");
    	
    	//If no previous time available set to current time and don't update.
    	if (time == 0)
    	{
    		data.getConfig().set("lasttimeupdated", System.currentTimeMillis());
    		return;
    	}
    	
    	timeDifference = System.currentTimeMillis() - time;
    	
    	//Get all saved players
    	if (data.getConfig().getConfigurationSection("data") == null)
    	{
    		return;
    	}
    	
		Set<String> savedPlayers = data.getConfig().getConfigurationSection("data").getKeys(false);
		
    	//Update fatigue for players
		for (String savedPlayer: savedPlayers)
		{
			ProfessionStats prof = new ProfessionStats(perms, data.getConfig(), config.getConfig(), UUID.fromString(savedPlayer));
			
			for (String profession: prof.getProfessions())
			{
				practiceFatigue = (int) (prof.getPracticeFatigue(profession) - timeDifference);
				instructionFatigue = (int) (prof.getInstructionFatigue(profession) - timeDifference);
					
				if (practiceFatigue < 0)
					prof.setPracticeFatigue(profession, 0);
				else
					prof.setPracticeFatigue(profession, practiceFatigue);
				
				if (instructionFatigue < 0)
					prof.setInstructionFatigue(profession, 0);
				else
					prof.setInstructionFatigue(profession, instructionFatigue);
			}
		}
    	
    	//New time
		time = System.currentTimeMillis();
		data.getConfig().set("lasttimeupdated", time);
	}
}


