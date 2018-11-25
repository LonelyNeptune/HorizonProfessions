package io.github.LonelyNeptune.HorizonProfessions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ProfessionAPI 
{
	private static Permission perms;
	private static FileConfiguration data;
	private static FileConfiguration config;
	
	ProfessionAPI(Permission perms, FileConfiguration data, FileConfiguration config)
	{
		ProfessionAPI.perms = perms;
        ProfessionAPI.data = data;
        ProfessionAPI.config = config;
	}
	
	public ProfessionAPI()
	{	}

	// updateConfig() updates the config file in the event of a configuration reload.
	static void updateConfig(FileConfiguration config)
	{
		ProfessionAPI.config = config;
	}

	/**
	 * getProfessions() gets the professions names that Horizon Professions is currently configured to use.
	 * 
	 * @return - a list of strings of the professions.
	 */
	public List<String> getProfessions()
	{
		return config.getStringList("professions");
	}
	
	/**
	 * isValidProfession() returns true if the given profession is a valid profession in current Horizon Professions
     * configuration, and false otherwise.
	 * 
	 * @param profession: The profession to test
	 * @return : True if the profession is a valid profession
	 */
	public boolean isValidProfession(String profession)
	{
		for (String p: getProfessions())
			if (profession.equalsIgnoreCase(p))
				return true;
		return false;
	}
	
	/**
	 * getTiers() gets the tier names that Horizon Professions is currently configured to use.
	 * 
	 * @return - a list of strings of the tiers
	 */
	public List<String> getTiers()
	{
		List<String> tierNames = new ArrayList<>();
		Set<String> configTiers;
		
		try { configTiers = config.getConfigurationSection("tiers").getKeys(false); }
		catch (NullPointerException e)
		{ return null; }
		
		for (String t: configTiers)
			tierNames.add(config.getString("tiers." + t + ".name"));
		
		return tierNames;
	}
	
	/**
	 * getClaimed() returns the number of free tiers a player has claimed.
	 * 
	 * @param uuid - the player
	 * @return the number of the free tiers a player has claimed
	 */
	public int getClaimed(UUID uuid)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.getClaimed();
	}
	
	/**
	 * setClaimed() sets the number of free tiers a player has claimed.
	 * 
	 * @param uuid - the player
	 * @param claimed - the number of free tiers a player has claimed
	 */
	public void setClaimed(UUID uuid, int claimed)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		prof.setClaimed(claimed);
	}
	
	/**
	 * getExperience() retrieves the experience of a player for the specified profession.
	 * 
	 * @param uuid - the player for whom the experience is being retrieved
	 * @param profession - the profession for which the experience is being retrieved
	 * @return The current experience of the player in the given profession
	 */
	public int getExperience(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.getExperience(profession);
	}
	
	/**
	 * resetExperience() resets the experience of a player for the specified profession.
	 * 
	 * @param uuid - the player for whom the experience is being reset
	 * @param profession - the profession for which the experience is being reset
	 */
	public void resetExperience(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		prof.resetExperience(profession);
	}
	
	/**
	 * addExperience() increases the experience of a player, unless they have reached the maximum tier, reached the
	 * tier cap, or haven't allocated all their free tiers. They will gain a level if they reach maximum experience.
	 * They will gain a tier if they reach maximum experience and the maximum level.
	 * 
	 * @param uuid - the player who is gaining the experience
	 * @param profession - the profession for which the player is gaining experience
	 * @param exp - the amount of experience the player is gaining
	 * @return Returns a value which reflects the result
	 * 		0 - the experience was added
	 * 		1 - the added experience resulted in a level-up
	 * 		2 - the experience could not be added because the player still has unclaimed tiers
	 * 		3 - the experience could not be added because the player has reached the maximum tier in that profession
	 * 		4 - the experience could not be added because the player has reached the maximum number of permitted tiers
	 */
	public int addExperience(UUID uuid, String profession, int exp)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.addExperience(profession, exp);
	}
	
	/**
	 * getLevel() retrieves the level of a player for the specified profession
	 * 
	 * @param uuid - the player for whom the level is being retrieved
	 * @param profession - the profession for which the level is being retrieved
	 */
	public int getLevel(UUID uuid, String profession)
	{		
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.getLevel(profession);
	}
	
	/**
	 * resetLevel() the level of a player in for the specified profession.
	 * 
	 * @param uuid - the player for whom the level is being reset
	 * @param profession - the profession for which the level is being reset
	 */
	public void resetLevel(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		prof.resetLevel(profession);
	}
	
	/**
	 * addLevel() increases the level of the player, unless they have reached the maximum tier, or reached the
	 * tier cap. They will gain a tier if they reach maximum level.
	 * @param uuid - the player who is gaining the levels
	 * @param profession - the profession for which the player is gaining the levels
	 * @param level - the number of levels the player is gaining
	 * @return Returns true if adding levels resulted in gaining a tier, false otherwise
	 */
	public boolean addLevel(UUID uuid, String profession, int level)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.addLevel(profession, level);
	}
	
	/**
	 * getTier() gets the tier the player currently has in a profession.
	 * @param uuid - the player for whom to get the tier name
	 * @param profession - the profession for which to get the tier name
	 * @return The tier the player has in the profession
	 */
	public int getTier(UUID uuid, String profession) 
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.getTier(profession);
	}
	
	/**
	 * resetTier() resets the tier of a player for the specified profession.
	 * 
	 * @param uuid - the player for whom the tier is being reset
	 * @param profession - the profession for which the tier is being reset
	 */
	public void resetTier(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		prof.resetTier(profession);
	}
	
	/**
	 * addTier() increases the tier of the player.
	 * Note that permissions is used to keep track of tiers.
	 * @param uuid - the player who is gaining the tier.
	 * @param profession - the profession for which the player is gaining the tier.
	 * @return - new tier of the player.
	 */
	public int addTier(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		int newTier = prof.getTier(profession) + 1;
		prof.addTier(profession);
		return newTier;
	}
	
	/**
	 * loseTier() reduces the tier of the player
	 * @param uuid - the player who is losing the tier.
	 * @param profession - the profession for which the player is losing the tier.
	 * @return  - new tier of the player.
	 */
	public int loseTier(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		int newTier = prof.getTier(profession) - 1;
		prof.loseTier(profession);
		return newTier;
	}
	
	/**
	 * getTierName() returns the string name for a tier number as specified in Horizon Profession's current
     * configuration.
	 * 
	 * @param tier - the number of the tier to find
	 * @return Returns the string name of the tier.
	 */
	public String getTierName(int tier)
	{
		return config.getString("tiers." + tier + ".name");
	}
	
	/**
	 * getTotalTiers() returns the total number of tiers that the player currently holds in all professions.
	 * 
	 * @param uuid - the player for whom the tiers are being counted
	 * @return Returns the total number of tiers that the player currently holds in all professions.
	 */
	public int getTotalTiers(UUID uuid)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.getTotalTiers();
	}
	
	/**
	 * hasTier() returns true if the player has a tier that is equal or higher in the profession given, and false
     * otherwise.
	 * 
	 * @param uuid - the player for whom the tier is being checked
	 * @param profession - the profession for which the tier is being checked
	 * @param tier - the tier to check
	 * @return True if the player has a tier that is equal or higher in the profession given, false otherwise
	 */
	public boolean hasTier(UUID uuid, String profession, int tier)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.hasTier(profession, tier);
	}
	
	/**
	 * hasTier() returns true if the player has a tier that is equal or higher in the profession given, and false
     * otherwise.
	 * 
	 * @param uuid - the player for whom the tier is being checked
	 * @param profession - the profession for which the tier is being checked
	 * @param tier - the tier to check
	 * @return True if the player has a tier that is equal or higher in the profession given, false otherwise
	 */
	public boolean hasTier(UUID uuid, String profession, String tier)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.hasTier(profession, tier);
	}
	
	/**
	 * setInstructionFatigue() sets the player as fatigued and sets the fatigue value to the current time.
	 * Fatigue will expire after an amount of time configured in Horizon Professions configuration.
	 * Until the fatigue expires, the player will not be able to benefit from trainer instruction.
	 * 
	 * @param uuid - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		prof.setInstructionFatigue(profession);
	}
	
	/**
	 * isInstructionFatigued() returns true if the instruction cooldown has not yet been met, and false otherwise.
	 * 
	 * @param uuid
	 * @param profession
	 * @return
	 */
	public boolean isInstructionFatigued(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.isInstructionFatigued(profession);
	}
	
	/**
	 * setPracticeFatigue() ets the player as fatigued and sets the fatigue value to the current time.
	 * Fatigue will expire after an amount of time configured in Horizon Professions configuration.
	 * Until the fatigue expires, the player will not be able to benefit from practice.
	 * 
	 * @param uuid - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(Player player, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
		prof.setPracticeFatigue(profession);
	}
	
	/**
	 * isPracticeFatigued() returns true if the practice cooldown has not yet been met, and false otherwise.
	 * 
	 * @param uuid
	 * @param profession
	 * @return
	 */
	public boolean isPracticeFatigued(UUID uuid, String profession)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		return prof.isPracticeFatigued(profession);
	}
	
	/**
	 * reset() sets all of the player's experience, levels and tiers to 0 in all professions, and removes all types of fatigue.
	 * 
	 * @param uuid - the player for whom the professions are being reset.
	 */
	public void reset(UUID uuid)
	{
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		prof.reset();
	}
}
