package com.gmail.Rhisereld.Horizon_Professions;

import net.milkbowl.vault.permission.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
	static JavaPlugin javaPlugin;
	public static Permission perms = null;		//Reference to permission object from Vault.
	int FATIGUE_TIME;			//Daily cooldown for level-up in milliseconds.
	List<String> PROFESSIONS = null;			//Names of professions.
	String TIERS[];								//Names of tiers.
	int[] MAX_LEVEL;				//Maximum level before progressing to the next tier
	int MAX_EXP = 100;							//Maximum experience before level-up.
	int CLAIMABLE_TIERS = 3;					//The number of free tiers a new player may claim.
	
	long time = 0;								//Time of last fatigue update.
	
	ConfigAccessor config;						//Configuration file.
	ConfigAccessor data;						//Data file.
	
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
            getServer().getPluginManager().registerEvents(new CraftListener(this), this);
        }
        else
        	getLogger().severe(String.format("Recipe support disabled due to no RecipeManager dependency found!", getDescription().getName()));
        
        //Listeners and commands.
        getServer().getPluginManager().registerEvents(new ProfessionListener(this), this);
    	this.getCommand("profession").setExecutor(new ProfessionCommandExecutor(this));
    	
    	//Load player stats
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
    	saveAllStats();
    	removeAllStats();
    	config.saveConfig();
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
    	Player player;
    	UUID uuid;
    	
    	//Try loading from config
    	if (time == 0)
    		time = data.getConfig().getLong("lasttimeupdated");
    	
    	//If no previous time available set to current time and don't update.
    	if (time == 0)
    	{
    		time = System.currentTimeMillis();
    		return;
    	}
    	
    	timeDifference = System.currentTimeMillis() - time;
    	
    	//Get all saved players
    	if (data.getConfig().getConfigurationSection("data") == null)
    		return;
    	
		Set<String> savedPlayers = data.getConfig().getConfigurationSection("data").getKeys(false);
		
    	//Update fatigue for players
		for (String savedPlayer: savedPlayers)
		{
			player = getServer().getPlayer(savedPlayer);
			
			//Player is offline
			if (player == null)
			{				
				uuid = UUID.fromString(savedPlayer);
				
				for (String profession: PROFESSIONS)
				{
					practiceFatigue = (int) (data.getConfig().getInt(savedPlayer + "." + profession + ".practicefatigue") - timeDifference);
					instructionFatigue = (int) (data.getConfig().getInt(savedPlayer + "." + profession + ".instructionfatigue") - timeDifference);
						
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
			//Player is online
			else
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
		}
    	
    	//New time
		time = System.currentTimeMillis();
		data.getConfig().set("lasttimeupdated", time);
		
		getLogger().info("Update successful.");
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
		for (String profession: PROFESSIONS)
		{
			loadExp(player, profession);
			loadLevel(player, profession);
			loadPracticeFatigue(player, profession);
			loadInstructionFatigue(player, profession);
			loadTier(player, profession);
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
		int exp = data.getConfig().getInt(player.getUniqueId() + "." + profession + ".exp");
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
		int level = data.getConfig().getInt(player.getUniqueId() + "." + profession + ".level");
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
		int fatigue = data.getConfig().getInt(player.getUniqueId() + "." + profession + ".practicefatigue");
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
		int fatigue = data.getConfig().getInt(player.getUniqueId() + "." + profession + ".instructionfatigue");
		player.setMetadata(profession + "_instructionfatigue", new FixedMetadataValue(plugin, fatigue));
	}
	
	/*
	 * loadClaimed() retrieves the number of claimed tiers from the configuration file, for the player specified, and 
	 * stores it in the player's metadata.
	 * @param player - the player for whom the claimed tiers is loaded.
	 */
	private void loadClaimed(Player player) 
	{
		 int claims = data.getConfig().getInt(player.getUniqueId() + ".claimed");
		 player.setMetadata("claimed", new FixedMetadataValue(plugin, claims));	
	}
	
	/*
	 * loadTier() retrieves the tier for a player in the profession specified, and stores it in the player's metadata.
	 * @param player - the player for whom the tier is loaded.
	 * @param profession - the profession for which the tier is loaded.
	 */
	private void loadTier(Player player, String profession) 
	{
		 int tier = data.getConfig().getInt(player.getUniqueId() + "." + profession + ".tier");
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
    	data.saveConfig();
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
	}
	
	/*
	 * saveExp() retrieves the metadata for experience from the player and stores it in a configuration file.
	 * @param player - the player for whom the experience is saved.
	 * @param profession - the profession for which the experience is saved.
	 */
	private void saveExp(Player player, String profession)
	{
		data.getConfig().set(player.getUniqueId() + "." + profession + ".exp", getExp(player, profession));
	}

	/*
	 * saveLevel() retrieves the metadata for level from the player and stores it in a configuration file.
	 * @param player - the player for whom the level is saved.
	 * @param profession - the profession for which the level is saved.
	 */
	private void saveLevel(Player player, String profession)
	{
		data.getConfig().set(player.getUniqueId() + "." + profession + ".level", getLevel(player, profession));
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
		
		data.getConfig().set(uuid + "." + profession + ".practicefatigue", getPracticeFatigue(player, profession));
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
		
		data.getConfig().set(uuid + "." + profession + ".instructionfatigue", getInstructionFatigue(player, profession));
	}
	
	/*
	 * saveClaimed() retrieves the metadata for number of claimed tiers from the player and stores it in a 
	 * configuration file.
	 * @param player - the player for whom the claimed tiers is saved.
	 */
	private void saveClaimed(Player player)
	{
		UUID uuid = player.getUniqueId();
		data.getConfig().set(uuid + ".claimed", getClaimed(player));
	}
	
	/*
	 * saveTier() retrieves the tier for the player in the profession specified and stores it in a configuration file.
	 * @param player - the player for whom the tier is saved.
	 * @param profession - the profession for which the tier is saved.
	 */
	private void saveTier(Player player, String profession)
	{
		UUID uuid = player.getUniqueId();
		
		data.getConfig().set(uuid + "." + profession + ".tier", getTier(player, profession));
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
	void resetPlayerStats(Player player)
	{
		for (String profession: PROFESSIONS)
		{
			setExp(player, profession, 0);
			setLevel(player, profession, 0);
			setPracticeFatigue(player, profession, 0);
			setInstructionFatigue(player, profession, 0);
			setTier(player, profession, 0);
		}
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
		//Expert is the maximum tier, player cannot progress past that point.
		if (getTier(player, profession) == 3)
			return;
		
		//If player is fatigued, return.
		if (getPracticeFatigue(player, profession) > 0)
			return;
		
		int newExp = exp + getExp(player, profession);

		//If player has reached maximum experience, level them up, set the daily cap and set exp to 0.
		if (newExp >= MAX_EXP)
		{
			player.sendMessage(ChatColor.YELLOW + "You feel more knowledgeable as a " + profession + ". You will need to rest and "
					+ "reflect on what you have learned, as you cannot benefit from any more practice today.");
			setPracticeFatigue(player, profession, FATIGUE_TIME);
			gainLevel(player, profession, 1);
			newExp = 0;
		}
		
		//Set new experience.
		setExp(player, profession, newExp);
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
		
		for (int i = 0; i < (MAX_LEVEL.length - 1); i++)
			if (getTier(player, profession) == i && newLevel >= MAX_LEVEL[i])
			{
				gainTier(player, profession);
				setLevel(player, profession, level - MAX_LEVEL[i]);
			}
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
		int tier = getTier(player, profession);
		int newTier;
		
		if ((newTier = tier + 1) > 3)
			newTier = 3;

		setTier(player, profession, newTier);
		return newTier;
	}
	
	/*
	 * gainTier() increases the tier of the offline player (unskilled -> novice -> adept -> expert)
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
	public int forgetTier(Player player, String profession)
	{
		int newTier;
		
		if ((newTier = getTier(player, profession) - 1) < 0)
			newTier = 0;
		
		setExp(player, profession, 0);
		setLevel(player, profession, 0);
		setTier(player, profession, newTier);
		return newTier;
	}
	
	/*
	 * forgetTier() reduces the tier of the offline player (expert -> adept -> novice -> unskilled)
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
	public void setExp(Player player, String profession, int exp)
	{
			player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, exp));
	}
	
	/*
	 * setExp() sets the experience of an offline player for the specified profession.
	 * @param player - the player for whom the experience is being set.
	 * @param profession - the profession for which the experience is being set.
	 */
	public void setExp(UUID uuid, String profession, int exp)
	{
		data.getConfig().set(uuid + "." + profession + ".exp", exp);
	}
	
	/*
	 * setLevel() sets the level of a player for the specified profession.
	 * @param player - the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(Player player, String profession, int level)
	{
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, level));
	}
	
	/*
	 * setLevel() sets the level of a player for the specified profession.
	 * @param player - the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(UUID uuid, String profession, int level)
	{
		data.getConfig().set(uuid + "." + profession + ".level", level);
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
	 * setPracticeFatigue() sets the practice fatigue value of an offline player for the specified profession in their metadata.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(UUID uuid, String profession, int fatigue)
	{
		data.getConfig().set(uuid + "." + profession + ".practicefatigue", fatigue);
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
	 * setInstructionFatigue() sets the practice fatigue value of an offline player for the specified profession in their 
	 * metadata. 
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(UUID uuid, String profession, int fatigue)
	{
		data.getConfig().set(uuid + "." + profession + ".instructionfatigue", fatigue);
	}
	
	/*
	 * setClaimed() sets the number of free tiers a player has claimed.
	 * @param uuid - the uuid of the player.
	 * @param claimed - the number of free tiers a player has claimed.
	 */
	public void setClaimed(Player player, int claimed)
	{
		player.setMetadata("claimed", new FixedMetadataValue(plugin, claimed));
	}
	
	/*
	 * setTier() sets the tier that a player has in the profession specified.
	 * @param uuid - the uuid of the player.
	 * @param profession - the profession for which to set the tier.
	 * @param tier - the value of the tier.
	 */
	public void setTier(Player player, String profession, int tier)
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
	
	/*
	 * setTier() sets the tier that an offline player has in the profession specified.
	 * @param uuid - the uuid of the player.
	 * @param profession - the profession for which to set the tier.
	 * @param tier - the value of the tier.
	 */
	public void setTier(UUID uuid, String profession, int tier)
	{
		OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
		
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

		data.getConfig().set(uuid + "." + profession + ".tier", tier);
	}
	
	/*
	 * getExp() retrieves the experience of a player for the specified profession.
	 * @param player - the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(Player player, String profession)
	{
			return getMetadataInt(player, profession + "_exp", plugin);
	}
	
	/*
	 * getExp() retrieves the experience of an offline player for the specified profession.
	 * @param player - the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(UUID uuid, String profession)
	{
		return data.getConfig().getInt(uuid + "." + profession + ".exp");
	}
	
	/*
	 * getLevel() retrieves the level of a player for the specified profession.
	 * @param player - the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(Player player, String profession)
	{		
		return getMetadataInt(player, profession + "_level", plugin);
	}
	
	/*
	 * getLevel() retrieves the level of an offline player for the specified profession.
	 * @param player - the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(UUID uuid, String profession)
	{
		return data.getConfig().getInt(uuid + "." + profession + ".level");
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
	 * getPracticeFatigue() retrieves the practice fatigue value of an offline player for the specified profession from 
	 * their metadata.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getPracticeFatigue(UUID uuid, String profession)
	{
			return data.getConfig().getInt(uuid + "." + profession + ".practicefatigue");
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
	
	/*
	 * getInstructionFatigue() retrieves the instruction fatigue value of an offline player for the specified profession 
	 * from their metadata.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getInstructionFatigue(UUID uuid, String profession)
	{
		return data.getConfig().getInt(uuid + "." + profession + ".instructionfatigue");
	}
	
	/*
	 * getClaimed() returns the number of free tiers a player has claimed.
	 * @param uuid - the uuid of the player.
	 * @return the number of the free tiers a player has claimed.
	 */
	public int getClaimed(Player player)
	{
		return getMetadataInt(player, "claimed", plugin);
	}
	
	/*
	 * getTier() gets the tier the player currently has in a profession. Use getTierOffline() for offline players!
	 * @param player - the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the tier the player has in the profession.
	 */
	public int getTier(Player player, String profession) 
	{
		return getMetadataInt(player, profession + "_tier", plugin);
	}
	
	/*
	 * getTierOffline() gets the tier the offline player currently has in a profession.
	 * @param player - the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the tier the player has in the profession.
	 */
	public int getTier(UUID uuid, String profession) 
	{
		return data.getConfig().getInt(uuid + "." + profession + ".tier");
	}
}


