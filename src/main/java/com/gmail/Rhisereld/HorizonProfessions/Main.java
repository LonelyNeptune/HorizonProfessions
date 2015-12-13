package com.gmail.Rhisereld.HorizonProfessions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin
{
	static Plugin plugin;						//Some functions require a reference to the plugin in args.
	static JavaPlugin javaPlugin;
	public static Permission perms = null;		//Reference to permission object from Vault.
	int FATIGUE_TIME;							//Daily cooldown for level-up in milliseconds.
	List<String> PROFESSIONS = null;			//Names of professions.
	String TIERS[];								//Names of tiers.
	int[] MAX_LEVEL;							//Maximum level before progressing to the next tier
	int MAX_EXP;								//Maximum experience before level-up.
	int CLAIMABLE_TIERS;						//The number of free tiers a new player may claim.
	int TIER_CAP;								//The total nubmer of tiers a player may have in all professions
	long PLACE_COOLDOWN;						//Cooldown preventing experience gained from recently placed blocks.
	
	long time = 0;								//Time of last fatigue update.
	
	ConfigAccessor config;						//Configuration file.
	ConfigAccessor data;						//Data file.
	
	Set<UUID> claimNotified;
	
	/*
	 * onEnable() is called when the server is started or the plugin is enabled.
	 * It should contain everything that the plugin needs for its initial setup.
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
	 */
    @Override
    public void onEnable() 
    {
    	plugin = this;
    	claimNotified = new HashSet<UUID>();
    	
    	//Setup files for configuration and data storage.
    	config = new ConfigAccessor(this, "config.yml");
    	data = new ConfigAccessor(this, "data.yml");
    	
    	//Load configuration
    	config.saveDefaultConfig();
    	
    	//Load all profession names.
    	if (config.getConfig().getStringList("professions") == null)
    		getLogger().severe("No professions specified in configuration - plugin have unexpected behavior.");
    	else
        	PROFESSIONS = config.getConfig().getStringList("professions");
    	
    	//Load all tier names.
    	if (config.getConfig().getConfigurationSection("tiers") == null)
    		getLogger().severe("No tiers specified in configuration - plugin have unexpected behavior.");
    	else
    	{
    		Set <String> configTiers = config.getConfig().getConfigurationSection("tiers").getKeys(false);
    		
			TIERS = new String[configTiers.size()];
			MAX_LEVEL = new int[configTiers.size()];
    		
    		for (String configTier: configTiers)
    		{
    			TIERS[Integer.parseInt(configTier)] = config.getConfig().getString("tiers." + configTier + ".name");
    			MAX_LEVEL[Integer.parseInt(configTier)] = config.getConfig().getInt("tiers." + configTier + ".maxlevel");
    		}
    	}
    	
    	//Load other options.
    	FATIGUE_TIME = config.getConfig().getInt("fatigue_time");
    	MAX_EXP = config.getConfig().getInt("max_exp");
    	CLAIMABLE_TIERS = config.getConfig().getInt("claimable_tiers");
    	TIER_CAP = config.getConfig().getInt("tier_cap");
    	PLACE_COOLDOWN = config.getConfig().getInt("place_cooldown");

    	//Vault integration for permissions
        if (!setupPermissions()) 
        {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        //RecipeManager integration for recipes.
        if (getServer().getPluginManager().isPluginEnabled("RecipeManager"))
        {
        	getLogger().info("RecipeManager hooked, recipe support enabled.");
            getServer().getPluginManager().registerEvents(new CraftListener(data, config), this);
        }
        else
        	getLogger().severe(String.format("Recipe support disabled due to no RecipeManager dependency found!", getDescription().getName()));
        
        //Listeners and commands.
        getServer().getPluginManager().registerEvents(new ProfessionListener(this, data, config), this);
    	this.getCommand("profession").setExecutor(new ProfessionCommandExecutor(this, data, config));
    	
    	//Save every 30 minutes.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			public void run() 
			{
				data.saveConfig();
			}			
		} , 36000, 36000);
		
		//Reduce fatigue time.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			public void run() 
			{
				updateFatigue();
			}			
		} , 20, 12000);
    }

	/*
     * onDisable() is called when the server shuts down or the plugin is disabled.
     * It should contain all the cleanup and data saving that the plugin needs to do before it is disabled.
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
	@Override
    public void onDisable() 
    {
    	data.saveConfig();
    	PROFESSIONS = null;
    	MAX_LEVEL = null;
    	config = null;
    	data = null;
    	claimNotified = null;
    	perms = null;
    	javaPlugin = null;
    	plugin = null;
    }
    
	/*
	 * setupPermissions() sets up Vault permissions integration which allows this plugin to communicate with
	 * permissions plugins in a standardised fashion.
	 */
    private boolean setupPermissions() 
    {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    
	/*
	 * updateFatigue() is called periodically (every 10 minutes) to update the fatigue values of all players.
	 * Fatigue begins at FATIGUE_TIME (milliseconds) and decreases over time until it reaches zero.
	 * Until fatigue reaches zero, players are prevented from gaining any experience.
	 * For online players the fatigue is updated in their metadata. For offline players the fatigue is updated
	 * in the configuration file.
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
			ProfessionStats prof = new ProfessionStats(data, config, UUID.fromString(savedPlayer));
			
			for (String profession: PROFESSIONS)
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


