package com.gmail.Rhisereld.HorizonProfessions;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

public class ProfessionAPI 
{
	static Main main;			//Reference to main.
	
	public ProfessionAPI()
	{
		main = new Main();
	}
	
	/*
	 * getProfessions() gets the professions names that Horizon Professions is currently configured to use.
	 * @return - a list of strings of the professions.
	 */
	public List<String> getProfessions()
	{
		return main.PROFESSIONS;
	}
	
	/*
	 * getTiers() gets the tier names that Horizon Professions is currently configured to use.
	 * @return - an array of strings of the tiers.
	 */
	public String[] getTiers()
	{
		return main.TIERS;
	}
	
	/*
	 * gainExperience() increases the experience of a player, unless they have reached the maximum tier, reached the
	 * tier cap, or haven't allocated all their free tiers. They will gain a level if they reach maximum experience.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player who is gaining the experience
	 * @param profession - the profession for which the player is gaining experience.
	 * @param exp - the amount of experience the player is gaining.
	 */
	public void gainExperience(Player player, String profession, int exp)
	{		
		main.gainExperience(player,  profession,  exp);
	}
	
	/*
	 * gainLevel() increases the level of the player, unless they have reached the maximum tier, or reached the
	 * tier cap. They will gain a tier if they reach maximum level.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player who is gaining the levels.
	 * @param profession - the profession for which the player is gaining the levels.
	 * @param level - the number of levels the player is gaining.
	 */
	public void gainLevel(Player player, String profession, int level)
	{		
		main.gainLevel(player, profession, level);
	}
	
	/*
	 * gainTier() increases the tier of the player (unskilled -> novice -> adept -> expert)
	 * Note that permissions is used rather than metadata to keep track of tiers.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player who is gaining the tier.
	 * @param profession - the profession for which the player is gaining the tier.
	 * @return - new tier of the player.
	 */
	public int gainTier(Player player, String profession)
	{		
		return main.gainTier(player, profession);
	}
	
	/*
	 * gainTier() increases the tier of the player (unskilled -> novice -> adept -> expert)
	 * Note that permissions is used rather than metadata to keep track of tiers.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param player - the player who is gaining the tier.
	 * @param profession - the profession for which the player is gaining the tier.
	 * @return - new tier of the player.
	 */
	public int gainTier(UUID uuid, String profession)
	{		
		return main.gainTier(uuid, profession);
	}
	
	/*
	 * forgetTier() reduces the tier of the player (expert -> adept -> novice -> unskilled)
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player who is losing the tier.
	 * @param profession - the profession for which the player is losing the tier.
	 * @return  - new tier of the player.
	 */
	public int forgetTier(Player player, String profession)
	{
		return main.forgetTier(player, profession);
	}
	
	/*
	 * forgetTier() reduces the tier of the player (expert -> adept -> novice -> unskilled)
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of player who is losing the tier.
	 * @param profession - the profession for which the player is losing the tier.
	 * @return  - new tier of the player.
	 */
	public int forgetTier(UUID uuid, String profession)
	{
		return main.forgetTier(uuid, profession);
	}
	
	/*
	 * setExp() sets the experience of a player for the specified profession.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the experience is being set.
	 * @param profession - the profession for which the experience is being set.
	 */
	public void setExp(Player player, String profession, int exp)
	{
		main.setExp(player, profession, exp);
	}
	
	/*
	 * setExp() sets the experience of an offline player for the specified profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the experience is being set.
	 * @param profession - the profession for which the experience is being set.
	 */
	public void setExp(UUID uuid, String profession, int exp)
	{
		main.setExp(uuid, profession, exp);
	}
	
	/*
	 * setLevel() sets the level of a player for the specified profession.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(Player player, String profession, int level)
	{
		main.setLevel(player, profession, level);
	}
	
	/*
	 * setLevel() sets the level of a player for the specified profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the level is being set.
	 * @param profession - the profession for which the level is being set.
	 */
	public void setLevel(UUID uuid, String profession, int level)
	{
		main.setLevel(uuid, profession, level);
	}
		
	/*
	 * setPracticeFatigue() sets the practice fatigue value of a player for the specified profession in their metadata.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(Player player, String profession, int fatigue)
	{
		main.setPracticeFatigue(player, profession, fatigue);
	}
	
	/*
	 * setPracticeFatigue() sets the practice fatigue value of an offline player for the specified profession in their 
	 * metadata.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setPracticeFatigue(UUID uuid, String profession, int fatigue)
	{
		main.setPracticeFatigue(uuid, profession, fatigue);
	}
	
	/*
	 * setInstructionFatigue() sets the practice fatigue value of a player for the specified profession in their metadata.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(Player player, String profession, int fatigue)
	{
		main.setInstructionFatigue(player, profession, fatigue);
	}
	
	/*
	 * setInstructionFatigue() sets the practice fatigue value of an offline player for the specified profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the fatigue value is being set.
	 * @param profession - the profession for which the fatigue value is being set.
	 */
	public void setInstructionFatigue(UUID uuid, String profession, int fatigue)
	{
		main.setInstructionFatigue(uuid, profession, fatigue);
	}
	
	/*
	 * setClaimed() sets the number of free tiers a player has claimed.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player
	 * @param claimed - the number of free tiers a player has claimed.
	 */
	public void setClaimed(Player player, int claimed)
	{
		main.setClaimed(player, claimed);
	}
	
	/*
	 * setClaimed() sets the number of free tiers an offline player has claimed.
	 * USE ONYL FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player.
	 * @param claimed - the number of free tiers a player has claimed.
	 */
	public void setClaimed(UUID uuid, int claimed)
	{
		main.setClaimed(uuid, claimed);
	}
	
	/*
	 * setTier() sets the tier that a player has in the profession specified.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param uuid - the uuid of the player.
	 * @param profession - the profession for which to set the tier.
	 * @param tier - the value of the tier.
	 */
	public static void setTier(Player player, String profession, int tier)
	{	
		main.setTier(player, profession, tier);
	}
	
	/*
	 * setTier() sets the tier that an offline player has in the profession specified.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player.
	 * @param profession - the profession for which to set the tier.
	 * @param tier - the value of the tier.
	 */
	public void setTier(UUID uuid, String profession, int tier)
	{
		main.setTier(uuid, profession, tier);
	}
	
	/*
	 * getExp() retrieves the experience of a player for the specified profession.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(Player player, String profession)
	{
		return main.getExp(player, profession);
	}
	
	/*
	 * getExp() retrieves the experience of an offline player for the specified profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the experience is being retrieved.
	 * @param profession - the profession for which the experience is being retrieved.
	 */
	public int getExp(UUID uuid, String profession)
	{
		return main.getExp(uuid, profession);
	}
	
	/*
	 * getLevel() retrieves the level of a player for the specified profession.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(Player player, String profession)
	{		
		return main.getLevel(player, profession);
	}
	
	/*
	 * getLevel() retrieves the level of an offline player for the specified profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the level is being retrieved.
	 * @param profession - the profession for which the level is being retrieved.
	 */
	public int getLevel(UUID uuid, String profession)
	{
		return main.getLevel(uuid, profession);
	}

	/*
	 * getPracticeFatigue() retrieves the practice fatigue value of a player for the specified profession.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getPracticeFatigue(Player player, String profession)
	{
		return main.getPracticeFatigue(player, profession);
	}
	
	/*
	 * getPracticeFatigue() retrieves the practice fatigue value of an offline player for the specified profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getPracticeFatigue(UUID uuid, String profession)
	{
		return main.getPracticeFatigue(uuid, profession);
	}

	/*
	 * getInstructionFatigue() retrieves the instruction fatigue value of a player for the specified profession.
	 * USE ONYL FOR ONLINE PLAYERS.
	 * @param player - the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getInstructionFatigue(Player player, String profession) 
	{
		return main.getInstructionFatigue(player, profession);
	}
	
	/*
	 * getInstructionFatigue() retrieves the instruction fatigue value of an offline player for the specified profession.
	 * @param uuid - the uuid of the player for whom the fatigue value is being retrieved.
	 * @param profession - the profession for which the fatigue value is being retrieved.
	 */
	public int getInstructionFatigue(UUID uuid, String profession)
	{
		return main.getInstructionFatigue(uuid, profession);
	}
	
	/*
	 * getClaimed() returns the number of free tiers a player has claimed.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player.
	 * @return the number of the free tiers a player has claimed.
	 */
	public int getClaimed(Player player)
	{
		return main.getClaimed(player);
	}
	
	/*
	 * getClaimed() returns the number of free tiers an offline player has claimed.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player.
	 * @return the number of the free tiers a player has claimed.
	 */
	public int getClaimed(UUID uuid)
	{
		return main.getClaimed(uuid);
	}
	
	/*
	 * getTier() gets the tier the player currently has in a profession.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the tier the player has in the profession.
	 */
	public int getTier(Player player, String profession) 
	{
		return main.getTier(player, profession);
	}
	
	/*
	 * getTier() gets the tier the offline player currently has in a profession.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom to get the tier name.
	 * @param profession - the profession for which to get the tier name.
	 * @return - the tier the player has in the profession.
	 */
	public int getTier(UUID uuid, String profession) 
	{
		return getTier(uuid, profession);
	}
	
	/*
	 * getTotalTiers() gets the total number of tiers a player has in all professions.
	 * USE ONLY FOR ONLINE PLAYERS.
	 * @param player - the player for whom to get the total number of tiers
	 * @return - the total number of tiers a player has in all professions.
	 */
	public int getTotalTiers(Player player)
	{
		return main.getTotalTiers(player);
	}
	
	/*
	 * getTotalTiers() gets the total number of tiers an offline player has in all professions.
	 * USE ONLY FOR OFFLINE PLAYERS.
	 * @param uuid - the uuid of the player for whom to get the total number of tiers
	 * @return - the total number of tiers a player has in all professions.
	 */
	public int getTotalTiers(UUID uuid)
	{
		return main.getTotalTiers(uuid);
	}

}
