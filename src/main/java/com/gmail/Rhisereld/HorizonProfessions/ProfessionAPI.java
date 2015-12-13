package com.gmail.Rhisereld.HorizonProfessions;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ProfessionAPI 
{
	ConfigAccessor data;
	ConfigAccessor config;
	
	public ProfessionAPI()
	{
    	config = new ConfigAccessor((JavaPlugin) Bukkit.getServer().getPluginManager().getPlugin("HorizonProfessions"), "config.yml");
    	data = new ConfigAccessor((JavaPlugin) Bukkit.getServer().getPluginManager().getPlugin("HorizonProfessions"), "data.yml");
	}

	/*
	 * getProfessions() gets the professions names that Horizon Professions is currently configured to use.
	 * @return - a list of strings of the professions.
	 */
	public List<String> getProfessions()
	{
		UUID uuid = UUID.fromString("null");
		ProfessionStats prof = new ProfessionStats(data, config, uuid);
		return prof.getProfessions();
	}
	
	/*
	 * getTiers() gets the tier names that Horizon Professions is currently configured to use.
	 * @return - an array of strings of the tiers.
	 */
	public List<String> getTiers()
	{
		UUID uuid = UUID.fromString("null");
		ProfessionStats prof = new ProfessionStats(data, config, uuid);
		return prof.getTiers();
	}
	
	/*
	 * gainExperience() increases the experience of a player, unless they have reached the maximum tier, reached the
	 * tier cap, or haven't allocated all their free tiers. They will gain a level if they reach maximum experience.
	 * @param player - the player who is gaining the experience
	 * @param profession - the profession for which the player is gaining experience.
	 * @param exp - the amount of experience the player is gaining.
	 */
	public void gainExperience(Player player, String profession, int exp)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.addExperience(profession, exp);
	}
	
	/*
	 * gainLevel() increases the level of the player, unless they have reached the maximum tier, or reached the
	 * tier cap. They will gain a tier if they reach maximum level.
	 * @param player - the player who is gaining the levels.
	 * @param profession - the profession for which the player is gaining the levels.
	 * @param level - the number of levels the player is gaining.
	 */
	public void gainLevel(Player player, String profession, int level)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.addLevel(profession, level);
	}
	
	/*
	 * gainTier() increases the tier of the player.
	 * Note that permissions is used to keep track of tiers.
	 * @param player - the player who is gaining the tier.
	 * @param profession - the profession for which the player is gaining the tier.
	 * @return - new tier of the player.
	 */
	public int gainTier(Player player, String profession)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		int newTier = prof.getTier(profession) + 1;
		prof.setTier(profession, newTier);
		return newTier;
	}
	
	/*
	 * forgetTier() reduces the tier of the player
	 * @param player - the player who is losing the tier.
	 * @param profession - the profession for which the player is losing the tier.
	 * @return  - new tier of the player.
	 */
	public int forgetTier(Player player, String profession)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		int newTier = prof.getTier(profession) - 1;
		prof.setTier(profession, newTier);
		return newTier;
	}
	
	/*
	 * setExp() sets the experience of a player for the specified profession.
	 * @param player - the player for whom the experience is being set.
	 * @param profession - the profession for which the experience is being set.
	 */
	public void setExp(Player player, String profession, int exp)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.setExperience(profession, exp);
	}

	/*
	 * setLevel() sets the level of a player for the specified profession.
	 * @param player - the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(Player player, String profession, int level)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.setLevel(profession, level);
	}
		
	/*
	 * setPracticeFatigue() sets the practice fatigue value of a player for the specified profession.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(Player player, String profession, int fatigue)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.setPracticeFatigue(profession, fatigue);
	}
	
	/*
	 * setInstructionFatigue() sets the practice fatigue value of a player for the specified profession.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(Player player, String profession, int fatigue)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.setInstructionFatigue(profession, fatigue);
	}
	
	/*
	 * setClaimed() sets the number of free tiers a player has claimed.
	 * @param player - the player
	 * @param claimed - the number of free tiers a player has claimed.
	 */
	public void setClaimed(Player player, int claimed)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.setClaimed(claimed);
	}
	
	/*
	 * setTier() sets the tier that a player has in the profession specified.
	 * @param uuid - the uuid of the player.
	 * @param profession - the profession for which to set the tier.
	 * @param tier - the value of the tier.
	 */
	public void setTier(Player player, String profession, int tier)
	{	
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		prof.setTier(profession, tier);
	}

	/*
	 * getExp() retrieves the experience of a player for the specified profession.
	 * @param player - the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(Player player, String profession)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		return prof.getExperience(profession);
	}
	
	/*
	 * getLevel() retrieves the level of a player for the specified profession.
	 * @param player - the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(Player player, String profession)
	{		
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		return prof.getLevel(profession);
	}

	/*
	 * getPracticeFatigue() retrieves the practice fatigue value of a player for the specified profession.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getPracticeFatigue(Player player, String profession)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		return prof.getPracticeFatigue(profession);
	}

	/*
	 * getInstructionFatigue() retrieves the instruction fatigue value of a player for the specified profession.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getInstructionFatigue(Player player, String profession) 
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		return prof.getInstructionFatigue(profession);
	}
	
	/*
	 * getClaimed() returns the number of free tiers a player has claimed.
	 * @param player - the player.
	 * @return the number of the free tiers a player has claimed.
	 */
	public int getClaimed(Player player)
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		return prof.getClaimed();
	}
	
	/*
	 * getTier() gets the tier the player currently has in a profession.
	 * @param player - the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the tier the player has in the profession.
	 */
	public int getTier(Player player, String profession) 
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		return prof.getTier(profession);
	}
}
