package com.gmail.Rhisereld.HorizonProfessions;

import java.util.ArrayList;
import java.util.List;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin
{
	public Permission perms = null;				//Reference to permission object from Vault.
	long time = 0;								//Time of last fatigue update.
	JavaPlugin plugin;
	
	ConfigAccessor config;						//Configuration file.
	ConfigAccessor data;						//Data file.
	
	/**
	 * onEnable() is called when the server is started or the plugin is enabled.
	 * It should contain everything that the plugin needs for its initial setup.
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
	 */
    @Override
    public void onEnable() 
    {    	
    	plugin = this;
    	
    	//Setup files for configuration and data storage.
    	config = new ConfigAccessor(this, "config.yml");
    	config.getConfig().options().copyDefaults(true);
    	data = new ConfigAccessor(this, "data.yml");
    	
    	//Write header.
    	config.getConfig().options().copyHeader(true);
    	
    	//Save configuration
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
		
		ProfessionStats prof;
		for (Player pl: Bukkit.getOnlinePlayers())
		{
			prof = new ProfessionStats(perms, data.getConfig(), config.getConfig(), pl.getUniqueId());
			for (String pr: professions)
				perms.playerAdd((String) null, pl, config.getConfig().getString("permission_prefix") + "." + pr + "." 
			+ prof.getTierName(prof.getTier(pr)));
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
    	
    	//Setup API.
    	new ProfessionAPI(perms, data.getConfig(), config.getConfig());
    	
    	//Save every 30 minutes.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public void run() 
			{
				data.saveConfig();
			}			
		} , 36000, 36000);
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
					perms.playerRemove((String) null, pl, config.getConfig().getString("permission_prefix") + "." + pr + "." + t);
		
    	data.saveConfig();
    	config = null;
    	data = null;
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
}


