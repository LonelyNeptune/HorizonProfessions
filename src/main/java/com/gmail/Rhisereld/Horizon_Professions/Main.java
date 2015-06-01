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
	 * loadAllStats() loads data for all online players.
	 */
	public void loadAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			loadPlayerStats(player);
	}
	
    /*
     * loadPlayerStats() loads saved data from the configuration file and stores it on the player as metadata.
     * @param player - the player for whom the data is loaded.
     */
    public void loadPlayerStats(Player player)
    {
		for (int i = 0; i < 5; i++)
		{
			loadExp(player, PROFESSIONS[i]);
			loadLevel(player, PROFESSIONS[i]);
			loadPracticeFatigue(player, PROFESSIONS[i]);
			loadInstructionFatigue(player, PROFESSIONS[i]);
		}
    }
    
    /*
     * loadExp() retrieves the experience from the configuration file, for the player and profession specified, and 
     * stores it in the player's metadata.
     * @param player - the player for whom the experience is loaded.
     * @param profession - the profession for which the experience is loaded.
     */
	public void loadExp(Player player, String profession)
	{
		int exp = getConfig().getInt("data." + player.getUniqueId() + "." + profession + ".exp");
		player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, exp));
	}
	
    /*
     * loadLevel() retrieves the level from the configuration file, for the player and profession specified, and
     * stores it in the player's metadata.
     * @param player - the player for whom the level is loaded.
     * @param profession - the profession for which the level is loaded.
     */
	public void loadLevel(Player player, String profession)
	{
		int level = getConfig().getInt("data." + player.getUniqueId() + "." + profession + ".level");
		player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level));
	}
	
	/*
	 * loadPracticeFatigue() retrieves the practice fatigue value from the configuration file, for the player and profession
	 * specified, and stores it in the player's metadata.
	 * @param player - the player for whom the fatigue value is loaded.
	 * @param profession - the profession for which the fatigue value is loaded.
	 */
	public void loadPracticeFatigue(Player player, String profession)
	{
		int fatigue = getConfig().getInt("data." + player.getUniqueId() + "." + profession + ".practicefatigue");
		player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * loadInstructionFatigue() retrieves the instruction fatigue value from the configuration file, for the player and profession
	 * specified, and stores it in the player's metadata.
	 * @param player - the player for whom the fatigue value is loaded.
	 * @param profession - the profession for which the fatigue value is loaded.
	 */
	public void loadInstructionFatigue(Player player, String profession)
	{
		int fatigue = getConfig().getInt("data." + player.getUniqueId() + "." + profession + ".instructionfatigue");
		player.setMetadata(profession + "_instructionfatigue", new FixedMetadataValue(plugin, fatigue));
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
     * savePlayerStats() retrieves metadata from the player and stores it in a configuration file.
     * @param player - the player for whom the data is saved.
     */
	public void savePlayerStats(Player player) 
	{
		for (String profession: PROFESSIONS)
		{
			saveExp(player, profession);
			saveLevel(player, profession);
			savePracticeFatigue(player, profession);
			saveInstructionFatigue(player, profession);
		}

    	saveConfig();
	}
	
	/*
	 * saveExp() retrieves the metadata for experience from the player and stores it in a configuration file.
	 * @param player - the player for whom the experience is saved.
	 * @param profession - the profession for which the experience is saved.
	 */
	public void saveExp(Player player, String profession)
	{
		getConfig().set("data." + player.getUniqueId() + "." + profession + ".exp", getExp(player, profession));
	}

	/*
	 * saveLevel() retrieves the metadata for level from the player and stores it in a configuration file.
	 * @param player - the player for whom the level is saved.
	 * @param profession - the profession for which the level is saved.
	 */
	public void saveLevel(Player player, String profession)
	{
		getConfig().set("data." + player.getUniqueId() + "." + profession + ".level", getLevel(player, profession));
	}
	
	/*
	 * savePracticeFatigue() retrieves the metadata for practice fatigue from the player and stores it in a configuration 
	 * file.
	 * @param player - the player for whom the fatigue value is saved.
	 * @param profession - the profession for which the fatigue value is saved.
	 */
	public void savePracticeFatigue(Player player, String profession)
	{
		getConfig().set("data." + player.getUniqueId() + "." + profession + ".practicefatigue", getPracticeFatigue(player, profession));
	}
	
	/*
	 * saveInstructionFatigue() retrieves the metadata for instruction fatigue from the player and stores it in a 
	 * configuration file.
	 * @param player - the player for whom the fatigue value is saved.
	 * @param profession - the profession for which the fatigue value is saved.
	 */
	public void saveInstructionFatigue(Player player, String profession)
	{
		getConfig().set("data." + player.getUniqueId() + "." + profession + ".instructionfatigue", getInstructionFatigue(player, profession));
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
			player.removeMetadata(profession + "_instructionfatigue", this);
		}
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
		if (getPracticeFatigue(player, profession) > 0)
			return;
		
		int newExp = exp + getExp(player, profession);

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
		setExp(player, profession, newExp);
	}
	
	/*
	 * giveInstruction() allows a trainer to train another player in a profession, awarding them 2 levels in the profession
	 * but also activating instruction fatigue.
	 * @param trainer - the player who is doing the training
	 * @param trainee - the player who is being trained
	 * @param profession - the profession that the trainee is being trained in.
	 */
	public void giveInstruction(Player trainer, Player trainee, String profession)
	{
		//TODO
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
		int newLevel = level + getLevel(player, profession);
		setLevel(player, profession, level + newLevel);
		
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
		int level = getLevel(player, profession);
		
		if (!player.hasPermission("horizon_professions." + profession))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession);
			perms.playerAdd(null, player, "horizon_professions." + profession + ".novice");
			setLevel(player, profession, level - MAX_LEVEL_UNSKILLED);
			return NOVICE;
		}
		else if (player.hasPermission("horizon_professions." + profession + ".novice"))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession + ".adept");
			perms.playerRemove(null, player, "horizon_professions." + profession + ".novice");	
			setLevel(player, profession, level - MAX_LEVEL_NOVICE);
			return ADEPT;
		}
		else if (player.hasPermission("horizon_professions." + profession + ".adept"))
		{
			perms.playerAdd(null, player, "horizon_professions." + profession + ".expert");
			perms.playerRemove(null, player, "horizon_professions." + profession + ".adept");
			setLevel(player, profession, level - MAX_LEVEL_ADEPT);
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
		setExp(player, profession, 0);
		setLevel(player, profession, 0);
		
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
			
			setExp(player, profession, 0);
			setLevel(player, profession, 0);
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
    	int practiceFatigue, instructionFatigue = 0;
    	
    	//Get all online players
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
    	
    	//Get all saved players
		Set<String> savedPlayers = plugin.getConfig().getConfigurationSection("data.").getKeys(false);
		
    	//Update fatigue for online players
		for (Player player: onlinePlayers)
		{
			for (String profession: PROFESSIONS)
			{
				practiceFatigue = (int) (getPracticeFatigue(player, profession) - timeDifference);
				instructionFatigue = (int) (getInstructionFatigue(player, profession) - timeDifference);
					
				if (practiceFatigue < 0)
					setPracticeFatigue(player, profession, 0);
				else
					setPracticeFatigue(player, profession, practiceFatigue);
				
				if (instructionFatigue < 0)
					setInstructionFatigue(player, profession, 0);
				else
					setInstructionFatigue(player, profession, instructionFatigue);
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
				practiceFatigue = (int) (getConfig().getInt("data." + playerUUID + "." + profession + ".practicefatigue") - timeDifference);
				instructionFatigue = (int) (getConfig().getInt("data." + playerUUID + "." + profession + ".instructionfatigue") - timeDifference);
				
				if (instructionFatigue < 0)
					getConfig().set("data." + playerUUID + "." + profession + ".instructionfatigue", 0);
				else
					getConfig().set("data." + playerUUID + "." + profession + ".instructionfatigue", instructionFatigue);
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
	
	/*
	 * setExp() sets the experience of a player for the specified profession in their metadata.
	 * @param player - the player for whom the experience is being set.
	 * @param profession - the profession for which the experience is being set.
	 */
	public void setExp(Player player, String profession, int exp)
	{
		player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, exp));
	}
	
	/*
	 * setLevel() sets the level of a player for the specified profession in their metadata.
	 * @param player - the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(Player player, String profession, int level)
	{
		player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level));
	}
		
	/*
	 * setPracticeFatigue() sets the practice fatigue value of a player for the specified profession in their metadata.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(Player player, String profession, int fatigue)
	{
		player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * setInstructionFatigue() sets the practice fatigue value of a player for the specified profession in their metadata.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(Player player, String profession, int fatigue)
	{
		player.setMetadata(profession + "_instructionfatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * getExp() retrieves the experience of a player for the specified profession from their metadata.
	 * @param player - the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(Player player, String profession)
	{
		return getMetadataInt(player, profession + "_exp", plugin);
	}
	
	/*
	 * getLevel() retrieves the level of a player for the specified profession from their metadata.
	 * @param player - the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(Player player, String profession)
	{
		return getMetadataInt(player, profession + "_level", plugin);
	}

	/*
	 * getPracticeFatigue() retrieves the practice fatigue value of a player for the specified profession from their 
	 * metadata.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getPracticeFatigue(Player player, String profession)
	{
		return getMetadataInt(player, profession + "_practicefatigue", plugin);
	}

	/*
	 * getInstructionFatigue() retrieves the instruction fatigue value of a player for the specified profession from their 
	 * metadata.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getInstructionFatigue(Player player, String profession) 
	{
		return getMetadataInt(player, profession + "_instructionfatigue", plugin);
	}
}


