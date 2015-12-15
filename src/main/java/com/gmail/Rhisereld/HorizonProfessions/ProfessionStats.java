package com.gmail.Rhisereld.HorizonProfessions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ProfessionStats 
{
	Permission perms;
	FileConfiguration data;
	FileConfiguration config;
	String path;
	List<String> professions;
	UUID uuid;
	HashMap<String, Integer> experience = new HashMap<String, Integer>();
	HashMap<String, Integer> levels = new HashMap<String, Integer>();
	HashMap<String, Integer> tiers = new HashMap<String, Integer>();
	HashMap<String, Integer> instructionFatigue = new HashMap<String, Integer>();
	HashMap<String, Integer> practiceFatigue = new HashMap<String, Integer>();
	int claimed;
	
	/**
	 * Constructor for fetching existing stats from the data file.
	 * 
	 * @param data
	 * @param uuid
	 */
	public ProfessionStats(Permission perms, FileConfiguration data, FileConfiguration config, UUID uuid)
	{		
		this.perms = perms;
		this.data = data;
		this.config = config;
		
		this.uuid = uuid;
		
		path = "data." + uuid.toString();
		professions = config.getStringList("professions");
		
		for (String p: professions)
		{
			experience.put(p, data.getInt(path + "." + p + ".exp"));
			levels.put(p, data.getInt(path + "." + p + ".level"));
			tiers.put(p, data.getInt(path + "." + p + ".tier"));
			instructionFatigue.put(p, data.getInt(path + "." + p + ".instructionFatigue"));
			practiceFatigue.put(p, data.getInt(path + "." + p + ".practiceFatigue"));
		}

		claimed = data.getInt(path + ".claimed");
	}
	
	/**
	 * getProfessions() returns a string list containing the names of all the professions they
	 * have.
	 * 
	 * @return
	 */
	public List<String> getProfessions()
	{
		return professions;
	}
	
	/**
	 * getClaimed() returns how many free tiers the player has claimed.
	 * 
	 * @return
	 */
	public int getClaimed()
	{
		return claimed;
	}
	
	/**
	 * setClaimed() sets the number of free tiers the player has claimed.
	 * 
	 * @param claimed
	 */
	public void setClaimed(int claimed)
	{
		data.set(path + ".claimed", claimed);
		this.claimed = claimed;
	}
	
	/**
	 * getExperience() returns the amount of experience the player has in the profession given.
	 * 
	 * @param profession
	 * @return
	 */
	public int getExperience(String profession)
	{
		return experience.get(profession);
	}
	
	/**
	 * setExperience() sets the amount of experience the player has in the profession given. If the experience is over the maximum experience, 
	 * the player gains a level and the experience is set to zero.
	 * 
	 * @param profession
	 * @param exp
	 * @return Returns true if setting the experience resulted in gaining a level, false otherwise.
	 */
	public boolean setExperience(String profession, int exp)
	{
		if (exp >= config.getInt("max_exp"))
		{
			exp = 0;
			addLevel(profession, 1);
			data.set(path + "." + profession + ".exp", exp);
			experience.put(profession, exp);
			setPracticeFatigue(profession, config.getInt("fatigue_time"));
			return true;
		}
		else
		{
			data.set(path + "." + profession + ".exp", exp);
			experience.put(profession, exp);
			return false;
		}
	}
	
	/**
	 * addExperience() adds the amount of experience given to the profession given.If the experience is over the maximum experience, 
	 * the player gains a level and the experience is set to zero.
	 * 
	 * @param profession
	 * @param exp
	 * @return Returns true if the added experience resulted in gaining a level, false otherwise.
	 */
	public boolean addExperience(String profession, int exp)
	{
		int newExp = exp + experience.get(profession);
		if (newExp >= config.getInt("max_exp"))
		{
			newExp = 0;
			addLevel(profession, 1);
			data.set(path + "." + profession + ".exp", newExp);
			experience.put(profession, newExp);
			setPracticeFatigue(profession, config.getInt("fatigue_time"));
			return true;
		}
		else
		{
			data.set(path + "." + profession + ".exp", newExp);
			experience.put(profession, newExp);
			return false;
		}		
	}
	
	/**
	 * notifyLevelUp() sends the player a message notifying them that they have levelled up in a profession.
	 * 
	 * @param profession
	 */
	public void notifyLevelUp(String profession)
	{
		Player player = Bukkit.getPlayer(uuid);
		
		player.sendMessage(ChatColor.YELLOW + "You feel more knowledgeable as a " + profession + ". You will need to rest and "
				+ "reflect on what you have learned, as you cannot benefit from any more practice today.");
	}
	
	/**
	 * getLevel() returns the level progress towards the next tier in the profession given.
	 * 
	 * @param profession
	 * @return
	 */
	public int getLevel(String profession)
	{
		return levels.get(profession);
	}
	
	/**
	 * setLevel() set the level progress towards the next tier in the profession given.
	 * 
	 * @param profession
	 * @param level
	 * @return Returns true if setting the level resulted in gaining a tier, false otherwise.
	 */
	public boolean setLevel(String profession, int level)
	{
		if (level >= config.getInt("tiers." + getTier(profession) + ".maxLevel"))
		{
			level = 0;
			addTier(profession, 1);
			data.set(path + "." + profession + ".level", level);
			levels.put(profession,  level);
			return true;
		}
		else
		{
			data.set(path + "." + profession + ".level", level);
			levels.put(profession,  level);
			return false;
		}
	}
	
	/**
	 * addLevel() adds level progress towards the next tier in the profession given.
	 * 
	 * @param profession
	 * @param level
	 * @return Returns true if adding levels resulted in gaining a tier, false otherwise.
	 */
	public boolean addLevel(String profession, int level)
	{
		int newLevel = levels.get(profession) + level;
		
		if (newLevel >= config.getInt("tiers." + getTier(profession) + ".maxLevel"))
		{
			newLevel = 0;
			addTier(profession, 1);
			data.get(path + "." + profession + ".level", newLevel);
			levels.put(profession, newLevel);
			return true;
		}
		else
		{
			data.get(path + "." + profession + ".level", newLevel);
			levels.put(profession, newLevel);
			return false;
		}	
	}
	
	/**
	 * getTier() returns the tier in the profession given.
	 * 
	 * @param profession
	 * @return
	 */
	public int getTier(String profession)
	{
		return tiers.get(profession);
	}
	
	/**
	 * setTier() sets the tier in the profession given.
	 * 
	 * @param profession
	 * @return Returns true if the tier was set successfully. Returns false if the tier was modified because it was over the maximum tier.
	 */
	public boolean setTier(String profession, int tier)
	{
		boolean returnValue = true;
		
		//Make sure the tier doesn't go over the maximum.
		if (tier >= getTiers().size())
		{
			tier = getTiers().size() - 1;
			returnValue = false;
		}
		
		data.set(path + "." + profession + ".tier", tier);
		tiers.put(profession,  tier);
		
		//Set permissions for the tier.
		for (String t: getTiers())
			perms.playerRemoveGroup((String) null, Bukkit.getPlayer(uuid), profession + "-" + t);
		
		perms.playerAddGroup((String) null, Bukkit.getPlayer(uuid), profession + "-" + getTierName(tier));
		
		return returnValue;
	}
	
	/**
	 * addTier() adds a tier to the profession given.
	 * 
	 * @param profession
	 * @param tier
	 * @return Returns true if the tier was added successfully. Returns false if the tier was modified because it was over the maximum tier.
	 */
	public boolean addTier(String profession, int tier)
	{
		boolean returnValue = true;
		int newTier = getTier(profession) + tier;
		
		//Make sure the tier doesn't go over the maximum.
		if (newTier >= getTiers().size())
		{
			newTier = getTiers().size() - 1;
			returnValue = false;
		}
		
		data.set(path + "." + profession + ".tier", newTier);
		tiers.put(profession,  newTier);
		
		//Set permissions for the tier.
		for (String t: getTiers())
			perms.playerRemoveGroup((String) null, Bukkit.getPlayer(uuid), profession + "-" + t);
		
		perms.playerAddGroup((String) null, Bukkit.getPlayer(uuid), profession + "-" + getTierName(tier));
		
		return returnValue;
	}
	
	/**
	 * getTierName() converts an integer to the name of the tier to which it corresponds.
	 * 
	 * @param profession
	 * @return
	 */
	public String getTierName(int tier)
	{
		return config.getString("tiers." + tier + ".name");
	}
	
	/**
	 * getTiers() returns the names of all the tiers as specified in configuration.
	 * 
	 * @return
	 */
	public List<String> getTiers()
	{
		List<String> tierNames = new ArrayList<String>();
		
		for (String t: config.getConfigurationSection("tiers").getKeys(false))
			tierNames.add(config.getString("tiers." + t + ".name"));
		
		return tierNames;
	}
	
	/**
	 * hasTier() returns true if the player has a tier that is equal or higher in the profession given, and false otherwise.
	 * 
	 * @param profession
	 * @param tierNum
	 * @return
	 */
	public boolean hasTier(String profession, int tierNum)
	{
		if (tiers.get(profession) >= tierNum)
			return true;
		else		
			return false;
	}
	
	/**
	 * hasTier() returns true of the player has a tier that is equal or higher in the profession given, and false otherwise.
	 * 
	 * @param tierName
	 * @return
	 */
	public boolean hasTier(String profession, String tierName)
	{
		int tierNum = 0;
		Set<String> configTiers = config.getConfigurationSection("tiers").getKeys(false);
		
		for (String t: configTiers)
			if (config.getString("tiers." + t + ".name").equalsIgnoreCase(tierName))
				tierNum = Integer.valueOf(t);
		
		if (tiers.get(profession) >= tierNum)
			return true;
		else		
			return false;
	}
	
	/**
	 * getInstructionFatigue() returns the instruction fatigue of the player in the given profession.
	 * 
	 * @param profession
	 * @return
	 */
	public int getInstructionFatigue(String profession)
	{
		return instructionFatigue.get(profession);
	}
	
	/**
	 * setInstructionFatigue() sets the instruction fatigue of the player in the given profession.
	 * 
	 * @param profession
	 * @param fatigue
	 */
	public void setInstructionFatigue(String profession, int fatigue)
	{
		data.set(path + "." + profession + ".instructionFatigue", fatigue);
		instructionFatigue.put(profession,  fatigue);
	}
	
	/**
	 * getPracticeFatigue() returns the practice fatigue of the player in the given profession.
	 * 
	 * @param profession
	 * @return
	 */
	public int getPracticeFatigue(String profession)
	{
		return practiceFatigue.get(profession);
	}
	
	/**
	 * setPracticeFatigue() sets the practice fatigue of the player in the given profession.
	 * 
	 * @param profession
	 * @param fatigue
	 */
	public void setPracticeFatigue(String profession, int fatigue)
	{
		data.set(path + "." + profession + ".practiceFatigue", fatigue);
		practiceFatigue.put(profession,  fatigue);
	}
	
	/**
	 * reset() sets all of the player's experience, levels and tiers to 0 in all professions, and removes all types of fatigue.
	 * 
	 */
	public void reset()
	{
		for (String p: getProfessions())
		{
			setExperience(p, 0);
			setLevel(p, 0);
			setTier(p, 0);
			setPracticeFatigue(p, 0);
			setInstructionFatigue(p, 0);
		}
		
		setClaimed(0);
	}
}
