package io.github.LonelyNeptune.HorizonProfessions;

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

class ProfessionStats
{
	private Permission perms;
	private FileConfiguration data;
	private FileConfiguration config;
	private String path;
	private List<String> professions;
	private UUID uuid;
	private HashMap<String, Integer> experience = new HashMap<>();
	private HashMap<String, Integer> levels = new HashMap<>();
	private HashMap<String, Integer> tiers = new HashMap<>();
	private HashMap<String, Long> instructionFatigue = new HashMap<>();
	private HashMap<String, Long> practiceFatigue = new HashMap<>();
	private int claimed;

	ProfessionStats(Permission perms, FileConfiguration data, FileConfiguration config, UUID uuid)
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
			instructionFatigue.put(p, data.getLong(path + "." + p + ".instructionFatigue"));
			practiceFatigue.put(p, data.getLong(path + "." + p + ".practiceFatigue"));
		}
		claimed = data.getInt(path + ".claimed");
	}
	
	// getProfessions() returns a string list containing the names of all the professions they have.
	List<String> getProfessions()
	{
		return professions;
	}
	
	// getClaimed() returns how many free tiers the player has claimed.
	int getClaimed()
	{
		return claimed;
	}
	
	// setClaimed() sets the number of free tiers the player has claimed.
	void setClaimed(int claimed)
	{
		data.set(path + ".claimed", claimed);
		this.claimed = claimed;
	}
	
	// getExperience() returns the amount of experience the player has in the profession given.
	int getExperience(String profession)
	{
		return experience.get(profession);
	}
	
	// resetExperience() sets the amount of experience the player has in the profession given to zero.
	void resetExperience(String profession)
	{		
		experience.put(profession, 0);
	}
	
	/**
	 * addExperience() adds the amount of experience given to the profession given. If the experience is over the
	 * maximum experience, the player gains a level and the experience is set to zero.
	 * 
	 * @param profession: The profession to add experience to
	 * @param exp: The amount of experience to add
	 * @return Returns a value which reflects the result.
	 * 		0 - the experience was added
	 * 		1 - the added experience resulted in a level-up
	 * 		2 - the experience could not be added because the player still has unclaimed tiers.
	 * 		3 - the experience could not be added because the player has reached the maximum tier in that profession.
	 * 		4 - the experience could not be added because the player has reached the maximum number of permitted tiers.
	 *      5 - the experience could not be added because the player is on cooldown.
	 */
	int addExperience(String profession, int exp)
	{
		//If the player is on cooldown, they cannot progress
		if (isPracticeFatigued(profession))
			return 5;
		
		//If the player has reach the maximum possible tiers, they cannot progress.
		if (getTotalTiers() >= config.getInt("tier_cap"))
			return 4;
		
		//If the player is the top tier in this profession, they cannot progress
		if (getTier(profession) >= getTiers().size() - 1)
			return 3;
		
		//If the player has any unclaimed tiers, they cannot progress
		if (getClaimed() < config.getInt("claimable_tiers"))
			return 2;
		
		int newExp = exp + experience.get(profession);
		if (newExp >= config.getInt("max_exp"))
		{
			newExp = newExp - config.getInt("max_exp");
			data.set(path + "." + profession + ".exp", newExp);
			experience.put(profession, newExp);
			setPracticeFatigue(profession);
			addLevel(profession, 1);
			notifyLevelUp(profession);
			return 1;
		}
		else
		{
			data.set(path + "." + profession + ".exp", newExp);
			experience.put(profession, newExp);
			return 0;
		}		
	}
	
	// notifyLevelUp() sends the player a message notifying them that they have levelled up in a profession.
	private void notifyLevelUp(String profession)
	{
		Player player = Bukkit.getPlayer(uuid);
		
		player.sendMessage(ChatColor.YELLOW + "You feel more knowledgeable as a " + profession + ". You will need" +
				" to rest and reflect on what you have learned, as you cannot benefit from any more practice today.");
	}
	
	// getLevel() returns the level progress towards the next tier in the profession given.
	int getLevel(String profession)
	{
		return levels.get(profession);
	}
	
	// resetLevel() set the level progress towards the next tier in the profession given to zero.
	void resetLevel(String profession)
	{
		data.set(path + "." + profession + ".level", 0);
		levels.put(profession,  0);
	}

	/**
	 * addLevel() adds level progress towards the next tier in the profession given.
	 * 
	 * @param profession: The profession to add a level in
	 * @param level: The number of levels to add
	 * @return Returns true if adding levels resulted in gaining a tier, false otherwise.
	 */
	boolean addLevel(String profession, int level)
	{
		int newLevel = levels.get(profession) + level;
		
		if (newLevel >= config.getInt("tiers." + getTier(profession) + ".maxLevel"))
		{
			newLevel = newLevel - config.getInt("tiers." + getTier(profession) + ".maxLevel");
			addTier(profession);
			data.set(path + "." + profession + ".level", newLevel);
			levels.put(profession, newLevel);
			return true;
		}
		else
		{
			data.set(path + "." + profession + ".level", newLevel);
			levels.put(profession, newLevel);
			return false;
		}	
	}
	
	// getTier() returns the tier in the profession given.
	int getTier(String profession)
	{
		return tiers.get(profession);
	}
	
	// resetTier() resets the tier in the profession given to zero.
	void resetTier(String profession)
	{
		data.set(path + "." + profession + ".tier", 0);
		tiers.put(profession,  0);
		
		//Set permissions for the tier.
		Player player = Bukkit.getPlayer(uuid);
		
		if (player == null)
			for (String t: getTiers())
				perms.playerRemove(
					null,
					Bukkit.getOfflinePlayer(uuid),
					config.getString("permission_prefix") + "." + profession + "." + t
				);
		else		
			for (String t: getTiers())
				perms.playerRemove(
					null,
					Bukkit.getPlayer(uuid),
					config.getString("permission_prefix") + "." + profession + "." + t
				);
	}

	// addTier() adds a tier to the profession given.
	void addTier(String profession)
	{
		int oldTier = getTier(profession);
		int newTier = oldTier + 1;
		
		//Make sure the tier doesn't go over the maximum.
		if (newTier >= getTiers().size())
			newTier = getTiers().size() - 1;
		
		data.set(path + "." + profession + ".tier", newTier);
		tiers.put(profession,  newTier);
		
		//Set permissions for the tier.
		for (String t: getTiers())
			perms.playerRemove(null, Bukkit.getPlayer(uuid), config.getString("permission_prefix") + "." + profession + "." + t);
		
		perms.playerAdd(null, Bukkit.getPlayer(uuid), config.getString("permission_prefix") + "." + profession + "."
				+ getTierName(newTier));
	}
	
	// loseTier() removes a tier from the profession given.
	void loseTier(String profession)
	{
		int oldTier = getTier(profession);
		int newTier = oldTier - 1;
		
		//Make sure the tier doesn't go under zero.
		if (newTier < 0)
			newTier = 0;
		
		data.set(path + "." + profession + ".tier", newTier);
		tiers.put(profession,  newTier);
		
		//Set permissions for the tier.
		for (String t: getTiers())
			perms.playerRemove(null, Bukkit.getPlayer(uuid), config.getString("permission_prefix") + "." + profession + "." + t);
		
		perms.playerAdd(null, Bukkit.getPlayer(uuid), config.getString("permission_prefix") + "." + profession + "."
				+ getTierName(newTier));
	}
	
	// getTierName() converts an integer to the name of the tier to which it corresponds.
	String getTierName(int tier)
	{
		return config.getString("tiers." + tier + ".name");
	}
	
	// getTiers() returns the names of all the tiers as specified in configuration.
	List<String> getTiers()
	{
		List<String> tierNames = new ArrayList<>();
		
		for (String t: config.getConfigurationSection("tiers").getKeys(false))
			tierNames.add(config.getString("tiers." + t + ".name"));
		
		return tierNames;
	}
	
	// getTotalTiers() returns the total number of tiers that the player holds in all professions.
	int getTotalTiers()
	{
		int total = 0;
		
		for (String p: getProfessions())
			total += tiers.get(p);
		
		return total;
	}

	// hasTier() returns true if the player has a tier that is equal or higher in the profession given, and false
	// otherwise.
	boolean hasTier(String profession, int tierNum)
	{
		return tiers.get(profession) >= tierNum;
	}
	
	// hasTier() returns true of the player has a tier that is equal or higher in the profession given, and false
	// otherwise.
	boolean hasTier(String profession, String tierName)
	{
		int tierNum = 0;
		Set<String> configTiers = config.getConfigurationSection("tiers").getKeys(false);
		
		for (String t: configTiers)
			if (config.getString("tiers." + t + ".name").equalsIgnoreCase(tierName))
				tierNum = Integer.valueOf(t);
		
		return tiers.get(profession) >= tierNum;
	}
	
	// setInstructionFatigue() sets the instruction fatigue of the player in the given profession.
	void setInstructionFatigue(String profession)
	{
		data.set(path + "." + profession + ".instructionFatigue", System.currentTimeMillis());
		instructionFatigue.put(profession,  System.currentTimeMillis());
	}
	
	// isInstructionFatigued() returns true if the instruction cooldown has not yet been met, and false otherwise.
	boolean isInstructionFatigued(String profession)
	{
		return System.currentTimeMillis() - instructionFatigue.get(profession) < config.getLong("fatigue_time");
	}
	
	// setPracticeFatigue() sets the practice fatigue of the player in the given profession.
	void setPracticeFatigue(String profession)
	{
		data.set(path + "." + profession + ".practiceFatigue", System.currentTimeMillis());
		practiceFatigue.put(profession,  System.currentTimeMillis());
	}
	
	// isPracticeFatigued() returns true if the practice cooldown has not yet been met, and false otherwise.
	boolean isPracticeFatigued(String profession)
	{
		return System.currentTimeMillis() - practiceFatigue.get(profession) < config.getLong("fatigue_time");
	}
	
	// reset() sets all of the player's experience, levels and tiers to 0 in all professions.
	void reset()
	{
		for (String p: getProfessions())
		{
			resetExperience(p);
			resetLevel(p);
			resetTier(p);
		}
	}
}
