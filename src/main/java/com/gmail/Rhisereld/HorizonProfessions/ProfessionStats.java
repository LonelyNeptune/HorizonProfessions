package com.gmail.Rhisereld.HorizonProfessions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProfessionStats 
{
	ConfigAccessor data;
	ConfigAccessor config;
	String path;
	List<String> professions;
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
	public ProfessionStats(ConfigAccessor data, ConfigAccessor config, UUID uuid)
	{		
		this.data = data;
		this.config = config;
		
		path = "data." + uuid.toString();
		professions = config.getConfig().getStringList("professions");
		
		for (String p: professions)
		{
			experience.put(p, data.getConfig().getInt(path + "." + p + ".exp"));
			levels.put(p, data.getConfig().getInt(path + "." + p + ".level"));
			tiers.put(p, data.getConfig().getInt(path + "." + p + ".tier"));
			instructionFatigue.put(p, data.getConfig().getInt(path + "." + p + ".instructionFatigue"));
			practiceFatigue.put(p, data.getConfig().getInt(path + "." + p + ".practiceFatigue"));
		}

		claimed = data.getConfig().getInt(path + ".claimed");
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
		data.getConfig().set(path + ".claimed", claimed);
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
	 * setExperience() sets the amount of experience the player has in the profession given.
	 * 
	 * @param profession
	 * @param exp
	 */
	public void setExperience(String profession, int exp)
	{
		data.getConfig().set(path + "." + profession + ".exp", exp);
		experience.put(profession, exp);
	}
	
	/**
	 * addExperience() adds the amount of experience given to the profession given.
	 * 
	 * @param profession
	 * @param exp
	 */
	public void addExperience(String profession, int exp)
	{
		int newExp = exp + experience.get(profession);
		data.getConfig().set(path + "." + profession + ".exp", newExp);
		experience.put(profession, newExp);
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
	 */
	public void setLevel(String profession, int level)
	{
		data.getConfig().set(path + "." + profession + ".level", level);
		levels.put(profession,  level);
	}
	
	/**
	 * addLevel() adds level progress towards the next tier in the profession given.
	 * 
	 * @param profession
	 * @param level
	 */
	public void addLevel(String profession, int level)
	{
		int newLevel = levels.get(profession) + level;
		data.getConfig().get(path + "." + profession + ".level", newLevel);
		levels.put(profession, newLevel);
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
	 */
	public void setTier(String profession, int tier)
	{
		data.getConfig().set(path + "." + profession + ".tier", tier);
		tiers.put(profession,  tier);
	}
	
	/**
	 * getTiers() returns the names of all the tiers as specified in configuration.
	 * 
	 * @return
	 */
	public List<String> getTiers()
	{
		List<String> tierNames = new ArrayList<String>();
		
		for (String t: config.getConfig().getConfigurationSection("tiers").getKeys(false))
			tierNames.add(config.getConfig().getString("tiers." + t + ".name"));
		
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
		Set<String> configTiers = config.getConfig().getConfigurationSection("tiers").getKeys(false);
		
		for (String t: configTiers)
			if (config.getConfig().getString("tiers." + t + ".name").equalsIgnoreCase(tierName))
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
		data.getConfig().set(path + "." + profession + ".instructionFatigue", fatigue);
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
		data.getConfig().set(path + "." + profession + ".practiceFatigue", fatigue);
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
