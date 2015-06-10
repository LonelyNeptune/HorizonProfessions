package com.gmail.Rhisereld.Horizon_Professions;

import net.milkbowl.vault.permission.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.crypto.Data;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
	final int MAX_EXP = 100;					//Maximum experience before level-up.
	private final int UNSKILLED = 0;			
	private final int NOVICE = 1;				//Tiers progress as unskilled -> novice -> adept -> expert
	private final int ADEPT = 2;				//Tiers correlate with numbers 0-3 for simplicity and fetching 
	private final int EXPERT = 3;				//from TIERS String array.
	final int FATIGUE_TIME = 86400000;	//Daily cooldown for level-up in milliseconds.
	final String[] PROFESSIONS = {"medic", "hunter", "labourer", "engineer", "pilot"}; 	//Names of professions.
	final String[] TIERS = {"unskilled", "novice", "adept", "expert"};						//Names of tiers.
	final int[] MAX_LEVEL = {1, 20, 40, 0};		//Maximum level before progressing to the next tier
	final int CLAIMABLE_TIERS = 3;				//The number of free tiers a new player may claim.
	
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
	 * updateFatigue() is called periodically (every 10 minutes) to update the fatigue values of all players.
	 * Fatigue begins at FATIGUE_TIME (milliseconds) and decreases over time until it reaches zero.
	 * Until fatigue reaches zero, players are prevented from gaining any experience.
	 * For online players the fatigue is updated in their metadata. For offline players the fatigue is updated
	 * in the configuration file.
	 */
	private void updateFatigue() 
    {    	
    	UUID uuid;
    	long timeDifference;
    	int practiceFatigue, instructionFatigue;
    	
    	//Try loading from config
    	if (time == 0)
    		time = getConfig().getLong("lasttimeupdated");
    	
    	//If no previous time available set to current time.
    	if (time == 0)
    		time = System.currentTimeMillis();
    	
    	timeDifference = System.currentTimeMillis() - time;
    	
    	//Get all saved players
		Set<String> savedPlayers = plugin.getConfig().getConfigurationSection("data.").getKeys(false);
		
    	//Update fatigue for players
		for (String playerString: savedPlayers)
		{
			Player player = getServer().getPlayer(playerString);		
			
			if (player == null)
			{
				uuid = Bukkit.getServer().getOfflinePlayer(playerString).getUniqueId();
			}
			else
				uuid = player.getUniqueId();
			
			for (String profession: PROFESSIONS)
			{
				practiceFatigue = (int) (getPracticeFatigue(uuid, profession) - timeDifference);
				instructionFatigue = (int) (getInstructionFatigue(uuid, profession) - timeDifference);
					
				if (practiceFatigue < 0)
					setPracticeFatigue(uuid, profession, 0);
				else
					setPracticeFatigue(uuid, profession, practiceFatigue);
				
				if (instructionFatigue < 0)
					setInstructionFatigue(uuid, profession, 0);
				else
					setInstructionFatigue(uuid, profession, instructionFatigue);
			}
		}
    	
    	//New time
		time = System.currentTimeMillis();
		getConfig().set("lasttimeupdated", time);
	}
    
	/*
	 * loadAllStats() loads data for all online players.
	 */
	private void loadAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			loadPlayerStats(player);
	}
	
    /*
     * loadPlayerStats() loads saved data from the configuration file and stores it on the player as metadata.
     * @param player - the player for whom the data is loaded.
     */
	void loadPlayerStats(Player player)
    {
		for (int i = 0; i < 5; i++)
		{
			loadExp(player, PROFESSIONS[i]);
			loadLevel(player, PROFESSIONS[i]);
			loadPracticeFatigue(player, PROFESSIONS[i]);
			loadInstructionFatigue(player, PROFESSIONS[i]);
			loadTier(player, PROFESSIONS[i]);
		}
		
		loadClaimed(player);
    }

	/*
     * loadExp() retrieves the experience from the configuration file, for the player and profession specified, and 
     * stores it in the player's metadata.
     * @param player - the player for whom the experience is loaded.
     * @param profession - the profession for which the experience is loaded.
     */
	private void loadExp(Player player, String profession)
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
	private void loadLevel(Player player, String profession)
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
	private void loadPracticeFatigue(Player player, String profession)
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
	private void loadInstructionFatigue(Player player, String profession)
	{
		int fatigue = getConfig().getInt("data." + player.getUniqueId() + "." + profession + ".instructionfatigue");
		player.setMetadata(profession + "_instructionfatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * loadClaimed() retrieves the number of claimed tiers from the configuration file, for the player specified, and 
	 * stores it in the player's metadata.
	 * @param player - the player for whom the claimed tiers is loaded.
	 */
	private void loadClaimed(Player player) 
	{
		 int claims = getConfig().getInt("data." + player.getUniqueId() + ".claimed");
		 player.setMetadata("claimed", new FixedMetadataValue(plugin, claims));	
	}
	
	/*
	 * loadTier() retrieves the tier for a player in the profession specified, and stores it in the player's metadata.
	 * @param player - the player for whom the tier is loaded.
	 * @param profession - the profession for which the tier is loaded.
	 */
	private void loadTier(Player player, String profession) 
	{
		 int tier = getConfig().getInt("data." + player.getUniqueId() + "." + profession + ".tier");
		 player.setMetadata(profession + "_tier", new FixedMetadataValue(plugin, tier));	
	}
	
	/*
	 * saveAllStats() saves data for all online players.
	 */
	 private void saveAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			savePlayerStats(player);
	}
	
    /*
     * savePlayerStats() retrieves metadata from the player and stores it in a configuration file.
     * @param player - the player for whom the data is saved.
     */
	 void savePlayerStats(Player player) 
	{
		for (String profession: PROFESSIONS)
		{
			saveExp(player, profession);
			saveLevel(player, profession);
			savePracticeFatigue(player, profession);
			saveInstructionFatigue(player, profession);
			saveTier(player, profession);
		}

		saveClaimed(player);
    	saveConfig();
	}
	
	/*
	 * saveExp() retrieves the metadata for experience from the player and stores it in a configuration file.
	 * @param player - the player for whom the experience is saved.
	 * @param profession - the profession for which the experience is saved.
	 */
	private void saveExp(Player player, String profession)
	{
		getConfig().set("data." + player.getUniqueId() + "." + profession + ".exp", getExp(player.getUniqueId(), profession));
	}

	/*
	 * saveLevel() retrieves the metadata for level from the player and stores it in a configuration file.
	 * @param player - the player for whom the level is saved.
	 * @param profession - the profession for which the level is saved.
	 */
	private void saveLevel(Player player, String profession)
	{
		getConfig().set("data." + player.getUniqueId() + "." + profession + ".level", getLevel(player.getUniqueId(), profession));
	}
	
	/*
	 * savePracticeFatigue() retrieves the metadata for practice fatigue from the player and stores it in a configuration 
	 * file.
	 * @param player - the player for whom the fatigue value is saved.
	 * @param profession - the profession for which the fatigue value is saved.
	 */
	private void savePracticeFatigue(Player player, String profession)
	{
		UUID uuid = player.getUniqueId();
		
		getConfig().set("data." + uuid + "." + profession + ".practicefatigue", getPracticeFatigue(uuid, profession));
	}
	
	/*
	 * saveInstructionFatigue() retrieves the metadata for instruction fatigue from the player and stores it in a 
	 * configuration file.
	 * @param player - the player for whom the fatigue value is saved.
	 * @param profession - the profession for which the fatigue value is saved.
	 */
	private void saveInstructionFatigue(Player player, String profession)
	{
		UUID uuid = player.getUniqueId();
		
		getConfig().set("data." + uuid + "." + profession + ".instructionfatigue", getInstructionFatigue(uuid, profession));
	}
	
	/*
	 * saveClaimed() retrieves the metadata for number of claimed tiers from the player and stores it in a 
	 * configuration file.
	 * @param player - the player for whom the claimed tiers is saved.
	 */
	private void saveClaimed(Player player)
	{
		UUID uuid = player.getUniqueId();
		
		getConfig().set("data." + uuid + ".claimed", getClaimed(uuid));
	}
	
	/*
	 * saveTier() retrieves the tier for the player in the profession specified and stores it in a configuration file.
	 * @param player - the player for whom the tier is saved.
	 * @param profession - the profession for which the tier is saved.
	 */
	private void saveTier(Player player, String profession)
	{
		UUID uuid = player.getUniqueId();
		
		getConfig().set("data." + uuid + "." + profession + ".tier", getTier(uuid, profession));
	}

	/*
	 * removePlayerStats() removes metadata from the player when it is no longer needed (such as when the plugin
	 * is disabled or the player logs off).
	 * @param player - the player for whom the data is removed.
	 */
	void removePlayerStats(Player player) 
	{
		for (String profession: PROFESSIONS)
		{
			player.removeMetadata(profession + "_exp", this);
			player.removeMetadata(profession + "_level", this);
			player.removeMetadata(profession + "_practicefatigue", this);
			player.removeMetadata(profession + "_instructionfatigue", this);
			player.removeMetadata(profession + "_claimed", this);
			player.removeMetadata(profession + "_tier", this);
		}
	}
	
	/*
	 * removeAllStats() removes the metadata of all players.
	 */
	private void removeAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection)
			removePlayerStats(player);
	}
	
	/*
	 * resetPlayerStats() removes all tiers from the player (by removing relevant permissions) and sets the 
	 * level and experience to zero for each profession.
	 * @param player - the player who is having their stats reset to 0.
	 */
	void resetPlayerStats(UUID uuid)
	{
		for (String profession: PROFESSIONS)
		{
			setExp(uuid, profession, 0);
			setLevel(uuid, profession, 0);
			setPracticeFatigue(uuid, profession, 0);
			setInstructionFatigue(uuid, profession, 0);
			setTier(uuid, profession, 0);
		}
	}
	
	/*
	 * getMetadataInt() retrieves metadata from a player using a key.
	 * @param object - the object the metadata is attached to.
	 * @param key - the key the metadata is under.
	 * @param plugin - a reference to this plugin
	 * @return - the metadata attached to the player that is associated with the key given.
	 */
	private int getMetadataInt(Metadatable object, String key, Plugin plugin) 
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
	 * gainExperience() increases the experience of a player, detects whether the player has reached MAX_EXP
	 * and calls gainLevel() and sets daily cap if this is the case. Also guards against daily cap (player will not
	 * gain experience if they are "fatigued").
	 * @param player - the player who is gaining the experience
	 * @param profession - the profession for which the player is gaining experience.
	 * @param exp - the amount of experience the player is gaining.
	 */
	public void gainExperience(Player player, String profession, int exp)
	{
		UUID uuid = player.getUniqueId();
		
		//Expert is the maximum tier, player cannot progress past that point.
		if (getTier(uuid, profession) == 3)
			return;
		
		//If player is fatigued, return.
		if (getPracticeFatigue(uuid, profession) > 0)
			return;
		
		int newExp = exp + getExp(uuid, profession);

		//If player has reached maximum experience, level them up, set the daily cap and set exp to 0.
		if (newExp >= MAX_EXP)
		{
			player.sendMessage("You feel more knowledgeable as a " + profession + ". You will need to rest and "
					+ "reflect on what you have learned, as you cannot benefit from any more practice today.");
			setPracticeFatigue(uuid, profession, FATIGUE_TIME);
			gainLevel(uuid, profession, 1);
			newExp = 0;
		}
		
		//Set new experience.
		setExp(player.getUniqueId(), profession, newExp);
	}
	
	/*
	 * gainLevel() increases the level of the player and calls gainTier() if the player has reached the maximum
	 * level for their tier.
	 * @param player - the player who is gaining the levels.
	 * @param profession - the profession for which the player is gaining the levels.
	 * @param level - the number of levels the player is gaining.
	 */
	public void gainLevel(UUID uuid, String profession, int level)
	{		
		int newLevel = level + getLevel(uuid, profession);
		setLevel(uuid, profession, level + newLevel);
		
		if (getTier(uuid, profession) == 0)
		{
			gainTier(uuid, profession);
			setLevel(uuid, profession, level - MAX_LEVEL[0]);
		}
		else if (getTier(uuid, profession) == 1 && newLevel >= MAX_LEVEL[1])
		{
			gainTier(uuid, profession);
			setLevel(uuid, profession, level - MAX_LEVEL[1]);	
		}
		else if	(getTier(uuid, profession) == 2 && newLevel >= MAX_LEVEL[2])
		{
			gainTier(uuid, profession);
			setLevel(uuid, profession, level - MAX_LEVEL[2]);
		}
	}
	
	/*
	 * gainTier() increases the tier of the player (unskilled -> novice -> adept -> expert)
	 * Note that permissions is used rather than metadata to keep track of tiers.
	 * @param player - the player who is gaining the tier.
	 * @param profession - the profession for which the player is gaining the tier.
	 * @return  - new tier of the player.
	 */
	public int gainTier(UUID uuid, String profession)
	{		
		int tier = getTier(uuid, profession);
		int newTier;
		
		if ((newTier = tier + 1) > 3)
			newTier = 3;

		setTier(uuid, profession, newTier);
		return newTier;
	}
	
	/*
	 * forgetTier() reduces the tier of the player (expert -> adept -> novice -> unskilled)
	 * @param player - the player who is losing the tier.
	 * @param profession - the profession for which the player is losing the tier.
	 * @return  - new tier of the player.
	 */
	public int forgetTier(UUID uuid, String profession)
	{
		int newTier;
		
		if ((newTier = getTier(uuid, profession) - 1) < 0)
			newTier = 0;
		
		setExp(uuid, profession, 0);
		setLevel(uuid, profession, 0);
		setTier(uuid, profession, newTier);
		return newTier;
	}
	
	/*
	 * setExp() sets the experience of a player for the specified profession.
	 * @param player - the player for whom the experience is being set.
	 * @param profession - the profession for which the experience is being set.
	 */
	public void setExp(UUID uuid, String profession, int exp)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			getConfig().set("data." + uuid + "." + profession + ".exp", exp);
		//Player is online.
		else
			player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, exp));
	}
	
	/*
	 * setLevel() sets the level of a player for the specified profession.
	 * @param player - the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(UUID uuid, String profession, int level)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			getConfig().set("data." + uuid + "." + profession + ".level", level);
		//Player is online.
		else
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level));
	}
		
	/*
	 * setPracticeFatigue() sets the practice fatigue value of a player for the specified profession in their metadata.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(UUID uuid, String profession, int fatigue)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			getConfig().set("data." + uuid + "." + profession + ".practicefatigue", fatigue);
		//Player is online.
		else
			player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * setInstructionFatigue() sets the practice fatigue value of a player for the specified profession in their metadata.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(UUID uuid, String profession, int fatigue)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			getConfig().set("data." + uuid + "." + profession + ".instructionfatigue", fatigue);
		//Player is online.
		else
			player.setMetadata(profession + "_instructionfatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * setClaimed() sets the number of free tiers a player has claimed.
	 * @param uuid - the uuid of the player.
	 * @param claimed - the number of free tiers a player has claimed.
	 */
	public void setClaimed(UUID uuid, int claimed)
	{
		Player player = getServer().getPlayer(uuid);
		
		player.setMetadata("claimed", new FixedMetadataValue(plugin, claimed));
	}
	
	/*
	 * setTier() sets the tier that a player has in the profession specified.
	 * @param uuid - the uuid of the player.
	 * @param profession - the profession for which to set the tier.
	 * @param tier - the value of the tier.
	 */
	public void setTier(UUID uuid, String profession, int tier)
	{
		Player player = getServer().getPlayer(uuid);
		OfflinePlayer offlinePlayer;
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = getServer().getOfflinePlayer(uuid);
			
			//Remove all previously held permissions.
			for (String previousTier: TIERS)
				perms.playerRemove(null, offlinePlayer, "horizon_professions." + profession + "." + previousTier);
			perms.playerRemove(null, offlinePlayer, "horizon_professions." + profession);
			
			//Re-add relevant ones.
			if (tier != 0)
			{
				perms.playerAdd(null, offlinePlayer, "horizon_professions." + profession);
				perms.playerAdd(null, offlinePlayer, "horizon_professions." + profession + "." + TIERS[tier]);
			}

			getConfig().set("data." + uuid + "." + profession + ".tier", tier);
		}
		//Player is online.
		else
		{
			//Remove all previously held permissions.
			for (String previousTier: TIERS)
				perms.playerRemove(null, player, "horizon_professions." + profession + "." + previousTier);
			perms.playerRemove(null, player, "horizon_professions." + profession);
			
			//Re-add relevant ones.
			if (tier != 0)
			{
				perms.playerAdd(null, player, "horizon_professions." + profession);
				perms.playerAdd(null, player, "horizon_professions." + profession + "." + TIERS[tier]);
			}
			player.setMetadata(profession + "_tier", new FixedMetadataValue(plugin, tier));
		}
	}
	
	/*
	 * getExp() retrieves the experience of a player for the specified profession.
	 * @param player - the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(UUID uuid, String profession)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			return getConfig().getInt("data." + uuid + "." + profession + ".exp");
		//Player is online.
		else
			return getMetadataInt(player, profession + "_exp", plugin);
	}
	
	/*
	 * getLevel() retrieves the level of a player for the specified profession.
	 * @param player - the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(UUID uuid, String profession)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			return getConfig().getInt("data." + uuid + "." + profession + ".level");
		//Player is online.
		else
			return getMetadataInt(player, profession + "_level", plugin);
	}

	/*
	 * getPracticeFatigue() retrieves the practice fatigue value of a player for the specified profession from their 
	 * metadata.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getPracticeFatigue(UUID uuid, String profession)
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			return getConfig().getInt("data." + uuid + "." + profession + ".practicefatigue");
		//Player is online.
		else	
			return getMetadataInt(player, profession + "_practicefatigue", plugin);
	}

	/*
	 * getInstructionFatigue() retrieves the instruction fatigue value of a player for the specified profession from their 
	 * metadata.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getInstructionFatigue(UUID uuid, String profession) 
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			return getConfig().getInt("data." + uuid + "." + profession + ".instructionfatigue");
		//Player is online.
		else	
			return getMetadataInt(player, profession + "_instructionfatigue", plugin);
	}
	
	/*
	 * getClaimed() returns the number of free tiers a player has claimed.
	 * @param uuid - the uuid of the player.
	 * @return the number of the free tiers a player has claimed.
	 */
	public int getClaimed(UUID uuid)
	{
		Player player = getServer().getPlayer(uuid);
		
		return getMetadataInt(player, "claimed", plugin);
	}
	
	/*
	 * getTier() gets the tier the player currently has in a profession.
	 * @param player - the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the tier the player has in the profession.
	 */
	public int getTier(UUID uuid, String profession) 
	{
		Player player = getServer().getPlayer(uuid);
		
		//Player is offline.
		if (player == null)
			return getConfig().getInt("data." + uuid + "." + profession + ".tier");
		//Player is online.
		else
			return getMetadataInt(player, profession + "_tier", plugin);
	}
}


