package com.gmail.Rhisereld.Horizon_Professions;

import net.milkbowl.vault.permission.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements CommandExecutor 
{
	static Plugin plugin;						//Some functions require a reference to the plugin in args.
	public static Permission perms = null;		//Reference to permission object from Vault.
	private final int MAX_EXP = 100;			//Maximum experience before level-up.
	private final int MAX_LEVEL_UNSKILLED = 1;	//Maximum level before progressing to novice tier.
	private final int MAX_LEVEL_NOVICE = 20;	//Maximum level before progressing to adept tier.
	private final int MAX_LEVEL_ADEPT = 40;		//Maximum level before progressing to expert tier.
	private final int UNSKILLED = 0;			
	private final int NOVICE = 1;				//Tiers progress as unskilled -> novice -> adept -> expert
	private final int ADEPT = 2;				//Tiers correlate with numbers 0-3 for simplicity and fetching 
	private final int EXPERT = 3;				//from PROFESSIONS String array.
	private final int FATIGUE_TIME = 86400000;	//Daily cooldown for level-up in milliseconds.
	final String[] PROFESSIONS = {"medic", "hunter", "labourer", "engineer", "pilot"}; 	//Names of professions.
	final String[] TIERS = {"", ".novice", ".adept", ".expert"};						//Names of tiers.
	
	long time = 0;	//Time of last fatigue update.
	
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
    	saveDefaultConfig();

    	//Vault integration for permissions
        if (!setupPermissions()) 
        {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        //Listeners and commands.
        getServer().getPluginManager().registerEvents(new ProfessionListener(this), this);
    	this.getCommand("profession").setExecutor(new ProfessionCommandExecutor(this));
    	
    	loadAllStats();
    	
    	//Save every 30 minutes.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			public void run() 
			{
				getLogger().info("Backing up player stats.");
				saveAllStats();
			}			
		} , 36000, 36000);
		
		//Reduce fatigue time.
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			public void run() 
			{
				getLogger().info("Updating fatigue values.");
				updateFatigue();
			}			
		} , 100, 12000);
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
    	saveAllStats();
    	removeAllStats();
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
     * loadPlayerStats() loads saved data from the configuration file and stores it on the player as metadata.
     * @param player - the player for whom the data is loaded.
     */
    public void loadPlayerStats(Player player)
    {
    	//If stats don't exist these are all set to 0.
    	int exp;
		int level;
		int practiceFatigue;

		for (int i = 0; i < 5; i++)
		{
			exp = getConfig().getInt("data." + player.getUniqueId() + "." + PROFESSIONS[i] + ".exp");
			level = getConfig().getInt("data." + player.getUniqueId() + "." + PROFESSIONS[i] + ".level");
			practiceFatigue = getConfig().getInt("data." + player.getUniqueId() + "." + PROFESSIONS[i] + ".practicefatigue");
			player.setMetadata(PROFESSIONS[i] + "_exp", new FixedMetadataValue(plugin, exp));
			player.setMetadata(PROFESSIONS[i] + "_level", new FixedMetadataValue(plugin, level));
			player.setMetadata(PROFESSIONS[i] + "_practicefatigue", new FixedMetadataValue(plugin, practiceFatigue));
		}
    }

    /*
     * savePlayerStats() retrieves metadata from the player and stores it in a configuration file.
     * @para player - the player for whom the data is saved.
     */
	public void savePlayerStats(Player player) 
	{
		for (String profession: PROFESSIONS)
		{
			getConfig().set("data." + player.getUniqueId() + "." + profession + ".exp", getMetadataInt(player, profession + "_exp", this));
			getConfig().set("data." + player.getUniqueId() + "." + profession + ".level", getMetadataInt(player, profession + "_level", this));
			getConfig().set("data." + player.getUniqueId() + "." + profession + ".practicefatigue", getMetadataInt(player, profession + "_practicefatigue", this));
		}

    	saveConfig();
	}

	/*
	 * removePlayerStats() removes metadata from the player when it is no longer needed (such as when the plugin
	 * is disabled or the player logs off).
	 * @param player - the player for whom the data is removed.
	 */
	public void removePlayerStats(Player player) 
	{
		for (String profession: PROFESSIONS)
		{
			player.removeMetadata(profession + "_exp", this);
			player.removeMetadata(profession + "_level", this);
			player.removeMetadata(profession + "_practicefatigue", this);
		}
	}
	
	/*
	 * loadAllStats() loads data for all online players.
	 */
	public void loadAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			loadPlayerStats(player);
	}
	
	/*
	 * saveAllStats() saves data for all online players.
	 */
	public void saveAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			savePlayerStats(player);
	}
	
	/*
	 * removeAllStats() removes the metadata of all players.
	 */
	public void removeAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection)
			removePlayerStats(player);
	}
	

	/*
	 * gainExperience() increases the experience of a player, detects whether the player has reached MAX_EXP
	 * and calls gainLevel() and sets daily cap if this is the case. Also guards against daily cap (player will not
	 * gain experience if they are "fatigued").
	 * @param player - the player who is gaining the experience
	 * @param profession - the profession for which the player is gaining experience.
	 * @param exp - the amount of experience the player is gaining.
	 */
	public void gainExperience(Player player, String profession, int exp)
	{
		//Expert is the maximum tier, player cannot progress past that point.
		if (player.hasPermission("horizon_profession." + profession + ".expert"))
			return;
		
		//If player is fatigued, return.
		if (getMetadataInt(player, profession + "_playerfatigue", plugin) > 0)
			return;
		
		int newExp = exp + getMetadataInt(player, profession + "_exp", plugin);

		//If player has reached maximum experience, level them up, set the daily cap and set exp to 0.
		if (newExp >= MAX_EXP)
		{
			player.sendMessage("You feel more knowledgeable as a " + profession + ". You will need to rest and "
					+ "reflect on what you have learned, as you cannot benefit from any more practice today.");
			player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, FATIGUE_TIME));
			gainLevel(player, profession, 1);
			newExp = 0;
		}
		
		//Set new experience.
		player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, newExp));
	}
	
	public void giveInstruction(Player trainer, Player trainee, String profession)
	{
		
	}
	
	/*
	 * gainLevel() increases the level of the player and calls gainTier() if the player has reached the maximum
	 * level for their tier.
	 * @param player - the player who is gaining the levels.
	 * @param profession - the profession for which the player is gaining the levels.
	 * @param level - the number of levels the player is gaining.
	 */
	public void gainLevel(Player player, String profession, int level)
	{
		int newLevel = level + getMetadataInt(player, profession + "_level", plugin);
		player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, newLevel));
		
		if (!player.hasPermission("horizon_professions." + profession))
			gainTier(player, profession);
		else if (player.hasPermission("horizon_professions." + profession + ".novice") && newLevel >= MAX_LEVEL_NOVICE)
			gainTier(player, profession);
		else if (player.hasPermission("horizon_professions." + profession + ".adept") && newLevel >= MAX_LEVEL_ADEPT)
			gainTier(player, profession);
	}
	
	/*
	 * gainTier() increases the tier of the player (unskilled -> novice -> adept -> expert)
	 * Note that permissions is used rather than metadata to keep track of tiers.
	 * @param player - the player who is gaining the tier.
	 * @param profession - the profession for which the player is gaining the tier.
	 * @return  - new tier of the player.
	 */
	public int gainTier(Player player, String profession)
	{		
		int level = getMetadataInt(player, profession + "_level", plugin);
		
		if (!player.hasPermission("horizon_professions." + profession))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession);
			perms.playerAdd(null, player, "horizon_professions." + profession + ".novice");
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level - MAX_LEVEL_UNSKILLED));
			return NOVICE;
		}
		else if (player.hasPermission("horizon_professions." + profession + ".novice"))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession + ".adept");
			perms.playerRemove(null, player, "horizon_professions." + profession + ".novice");	
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level - MAX_LEVEL_NOVICE));
			return ADEPT;
		}
		else if (player.hasPermission("horizon_professions." + profession + ".adept"))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession + ".expert");
			perms.playerRemove(null, player, "horizon_professions." + profession + ".adept");
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level - MAX_LEVEL_ADEPT));
			return EXPERT;
		}
		else
			return -1;
	}
	
	/*
	 * forgetTier() reduces the tier of the player (expert -> adept -> novice -> unskilled)
	 * @param player - the player who is losing the tier.
	 * @param profession - the profession for which the player is losing the tier.
	 * @return  - new tier of the player.
	 */
	public int forgetTier(Player player, String profession)
	{
		player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, 0));
		
		if (player.hasPermission("horizon_professions." + profession + ".expert"))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession + ".adept");
			perms.playerRemove(null, player, "horizon_professions." + profession + ".expert");
			return ADEPT;
		}
		else if (player.hasPermission("horizon_professions." + profession + ".adept"))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession + ".novice");
			perms.playerRemove(null, player, "horizon_professions." + profession + ".adept");
			return NOVICE;
		}
		else if (player.hasPermission("horizon_professions." + profession + ".novice"))
		{
			perms.playerRemove(null, player, "horizon_professions." + profession + ".novice");
			perms.playerRemove(null, player, "horizon_professions." + profession);
			return UNSKILLED;
		}
		else
			return 0;
	}
	
	/*
	 * resetPlayerStats() removes all tiers from the player (by removing relevant permissions) and sets the 
	 * level and experience to zero for each profession.
	 * @param player - the player who is having their stats reset to 0.
	 */
	public void resetPlayerStats(Player player)
	{		
		for (String profession: PROFESSIONS)
		{
			for (String tier: TIERS)
			{
				perms.playerRemove(null, player, "horizon_professions." + profession + tier);
			}
			
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, 0));
			player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, 0));
		}
	}

	/*
	 * getMaxLevel() gets the maximum level for the tier that the player is currently in.
	 * @param player - the player for whom to get the maximum level.
	 * @param profession - the profession for which to get the maximum level.
	 * @return - the maximum level for the tier the player is currently in for the profession.
	 */
	public int getMaxLevel(Player player, String profession) 
	{
		if (!player.hasPermission("horizon_professions." + profession))
			return 1;
		else if (player.hasPermission("horizon_professions." + profession + TIERS[1]))
			return 20;
		else if (player.hasPermission("horizon_professions." + profession + TIERS[2]))
			return 40;
		else
			return -1;
	}

	/*
	 * getTier() gets string of the name of the tier the player currently has in a profession.
	 * Doesn't follow good conventions - rework in future?
	 * @param player - the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the name of the tier the player has in the profession in a string.
	 */
	public String getTier(Player player, String profession) 
	{
		if (!player.hasPermission("horizon_professions." + profession))
			return "Unskilled";
		else if (player.hasPermission("horizon_professions." + profession + TIERS[1]))
			return "Novice";
		else if (player.hasPermission("horizon_professions." + profession + TIERS[2]))
			return "Adept";
		else if (player.hasPermission("horizon_professions." + profession + TIERS[3]))
			return "Expert";
		else
			return "Error";
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
    	//Try loading from config
    	if (time == 0)
    		time = getConfig().getLong("lasttimeupdated");
    	
    	//If no previous time available set to current time.
    	if (time == 0)
    		time = System.currentTimeMillis();
    	
    	long timeDifference = System.currentTimeMillis() - time;
    	int newFatigue = 0;
    	
    	//Get all online players
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
    	
    	//Get all saved players
		Set<String> savedPlayers = plugin.getConfig().getConfigurationSection("data.").getKeys(false);
		
    	//Update fatigue for online players
		for (Player player: onlinePlayers)
		{
			for (String profession: PROFESSIONS)
			{
				newFatigue = (int) (getMetadataInt(player, profession + "_practicefatigue", this) - timeDifference);
				
				if (newFatigue < 0)
					player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, 0));
				else
					player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, newFatigue));
			}

		}
		
		//Update fatigue for offline players
		for (String playerUUID: savedPlayers)
		{
			//If player is online, skip.
			if (onlinePlayers.contains(getServer().getPlayer(UUID.fromString(playerUUID))))
				continue;
			
			for (String profession: PROFESSIONS)
			{
				newFatigue = (int) (getConfig().getInt("data." + playerUUID + "." + profession + ".practicefatigue") - timeDifference);
				
				if (newFatigue < 0)
					getConfig().set("data." + playerUUID + "." + profession + ".practicefatigue", 0);
				else
					getConfig().set("data." + playerUUID + "." + profession + ".practicefatigue", newFatigue);
			}
		}
    	
    	//New time
		time = System.currentTimeMillis();
		getConfig().set("lasttimeupdated", time);
	}
    
	/*
	 * getMetadataInt() retrieves metadata from a player using a key.
	 * @param object - the object the metadata is attached to.
	 * @param key - the key the metadata is under.
	 * @param plugin - a reference to this plugin
	 * @return - the metadata attached to the player that is associated with the key given.
	 */
	public int getMetadataInt(Metadatable object, String key, Plugin plugin) 
	{
		List<MetadataValue> values = object.getMetadata(key);  
		for (MetadataValue value : values) 
		{
			// Plugins are singleton objects, so using == is safe here
			if (value.getOwningPlugin() == plugin) 
			{
				return value.asInt();
			}
		}
		return 0;
	}
}


