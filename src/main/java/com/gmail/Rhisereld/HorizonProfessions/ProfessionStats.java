package com.gmail.Rhisereld.HorizonProfessions;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ProfessionStats 
{
	ConfigAccessor data;
	ConfigAccessor config;
	String path;
	List<String> professions;
	HashMap<String, Integer> experience;
	HashMap<String, Integer> levels;
	HashMap<String, Integer> tiers;
	HashMap<String, Integer> instructionFatigue;
	HashMap<String, Integer> practiceFatigue;
	int claimed;
	
	/**
	 * Constructor for fetching existing stats from the data file.
	 * 
	 * @param data
	 * @param uuid
	 */
	public ProfessionStats(ConfigAccessor data, ConfigAccessor config, UUID uuid)
	{		
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
	
}
