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
	static Plugin plugin;
	public static Permission perms = null;
	private final int MAX_EXP = 100;
	private final int MAX_LEVEL_UNSKILLED = 1;
	private final int MAX_LEVEL_NOVICE = 20;
	private final int MAX_LEVEL_ADEPT = 40;
	private final int UNSKILLED = 0;
	private final int NOVICE = 1;
	private final int ADEPT = 2;
	private final int EXPERT = 3;
	private final int FATIGUE_TIME = 86400000;
	final String[] PROFESSIONS = {"medic", "hunter", "labourer", "engineer", "pilot"};
	final String[] TIERS = {"", ".novice", ".adept", ".expert"};
	
	long time = 0;
	
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

	@Override
    public void onDisable() 
    {
    	saveAllStats();
    	removeAllStats();
    	plugin = null;
    }
    
    //Vault integration for permissions
    private boolean setupPermissions() 
    {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    
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

	public void removePlayerStats(Player player) 
	{
		for (String profession: PROFESSIONS)
		{
			player.removeMetadata(profession + "_exp", this);
			player.removeMetadata(profession + "_level", this);
			player.removeMetadata(profession + "_practicefatigue", this);
		}
	}
	
	public void loadAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			loadPlayerStats(player);
	}
	
	public void saveAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			savePlayerStats(player);
	}
	
	public void removeAllStats()
	{
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection)
			removePlayerStats(player);
	}

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
	
	//Calls gainLevel if max experience is reached.
	public void gainExperience(Player player, String profession, int exp)
	{
		//Expert is the maximum tier.
		if (player.hasPermission("horizon_profession." + profession + ".expert"))
			return;
		
		//Practice Fatigue
		if (getMetadataInt(player, profession + "_playerfatigue", plugin) > 0)
			return;
		
		int newExp = exp + getMetadataInt(player, profession + "_exp", plugin);

		if (newExp >= MAX_EXP)
		{
			player.sendMessage("You feel more knowledgeable as a " + profession + ". You will need to rest and "
					+ "reflect on what you have learned, as you cannot benefit from any more practice today.");
			player.setMetadata(profession + "_practicefatigue", new FixedMetadataValue(plugin, FATIGUE_TIME));
			gainLevel(player, profession, 1);
			newExp = 0;
		}
		
		player.setMetadata(profession + "_exp", new FixedMetadataValue(plugin, newExp));
	}
	
	public void giveInstruction(Player trainer, Player trainee, String profession)
	{
		
	}
	
	//Calls gainTier if max level is reached.
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
	
	public void resetAll(Player player)
	{		
		for (String profession: PROFESSIONS)
		{
			for (String tier: TIERS)
			{
				perms.playerRemove(null, player, "horizon_professions." + profession + tier);
			}
			
			player.setMetadata(profession + "_level", new FixedMetadataValue(plugin, 0));
		}
	}

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
	
    protected void updateFatigue() 
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
}


