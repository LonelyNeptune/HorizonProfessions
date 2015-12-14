package com.gmail.Rhisereld.HorizonProfessions;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class ProfessionHandler 
{
	ConfigAccessor data;
	ConfigAccessor config;
	
	private final int PROGRESS_BAR_BLOCKS = 30; //The number of blocks that appear in the progress bar for command /profession view
	private final int HEADER_WIDTH = 30; 		//The width of the header for each profession when viewing stats.
	private final int CONSOLE_HEADER_WIDTH = 25;//The width of the header for the console.
	
	public ProfessionHandler(ConfigAccessor data, ConfigAccessor config)
	{
		this.data = data;
		this.config = config;
	}
	
	/**
	 * displayStats() retrieves the profession stats of the UUID provided, and displays it to the sender provided.
	 * 
	 * @param player
	 * @param name
	 * @param sender
	 */
	public void displayStats(UUID player, String name, CommandSender sender)
	{	
		String tier;
		int maxLevel;
		int practiceFatigue;
		int instructionFatigue;
		String message = null;
		ProfessionStats prof = new ProfessionStats(data, config, player);

		sender.sendMessage("--------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">--------------");
		sender.sendMessage(ChatColor.GOLD + "  Viewing " + name);
		
		for (String profession: prof.getProfessions())
		{
			tier = getTierName(prof.getTier(profession));
			maxLevel = config.getConfig().getInt("tiers." + prof.getTier(profession) + ".maxlevel");
			practiceFatigue = prof.getPracticeFatigue(profession);
			instructionFatigue = prof.getInstructionFatigue(profession);
			
			//Build profession header for each profession.
			int headerWidth;
			if (sender instanceof ConsoleCommandSender)
				headerWidth = CONSOLE_HEADER_WIDTH;
			else 
				headerWidth = HEADER_WIDTH;
			message = ChatColor.YELLOW + "  " + alignText(tier.substring(0, 1).toUpperCase() + tier.substring(1) + " " 
						+ profession.substring(0, 1).toUpperCase() + profession.substring(1), headerWidth);
			
			//If the player has hit max tier, don't even show the level progression
			if (maxLevel == 0)
				message += " Level " + "[Maximum]";
			else
				message += " Level " + "[" + prof.getLevel(profession) + "/" + maxLevel + "]";
			
			//Send it.
			sender.sendMessage(message);
			
			//Build progress bar for each profession.
			if (practiceFatigue > 0 && instructionFatigue > 0)
				message = "" + ChatColor.RED;
			else if (practiceFatigue > 0 || instructionFatigue > 0)
					message = "" + ChatColor.GOLD;
			else 
				message = "" + ChatColor.GREEN;
			
			for (int i2 = 0; i2 < PROGRESS_BAR_BLOCKS; i2++)
			{
				if (practiceFatigue > 0)
					message += "█";
				else if (i2 < prof.getExperience(profession) / (config.getConfig().getInt("max_exp")/PROGRESS_BAR_BLOCKS))
					message += "█";
				else
					message += ChatColor.DARK_GRAY + "█";
			}
			
			message += ChatColor.YELLOW + " XP";
			
			//Send it.
			sender.sendMessage(message);
		}
	}
	
	/**
	 * forgetTier() reduces the tier of the player in a certain profession by one.
	 * 
	 * @param uuid
	 * @param profession
	 * @return
	 */
	public int forgetTier(UUID uuid, String profession) throws IllegalArgumentException
	{
		//Check that the profession argument is one of the professions.
		ProfessionStats prof = new ProfessionStats(data, config, uuid);
		String professionFound = null;
		for (String existingProfession: prof.getProfessions())
			if (profession.equalsIgnoreCase(existingProfession))
				professionFound = existingProfession;
		
		if (professionFound == null)
			throw new IllegalArgumentException("That profession does not exist!");
		
		//Check that the player is not at the lowest tier.
		if (prof.getTier(professionFound) == 0)
			throw new IllegalArgumentException("That player is already the lowest tier in that profession.");
		
		//Remove the tier
		prof.setExperience(profession, 0);
		prof.setLevel(profession, 0);
		int newTier = prof.getTier(profession) - 1;
		prof.setTier(profession, newTier);
		return newTier;
	}
	
	/**
	 * giveTier() increases the tier of the player in a certain profession by one.
	 * 
	 * @param uuid
	 * @param profession
	 * @return
	 */
	public int giveTier(UUID uuid, String profession) throws IllegalArgumentException
	{
		//Check that the profession argument is one of the professions.
		ProfessionStats prof = new ProfessionStats(data, config, uuid);
		String professionFound = null;
		for (String existingProfession: prof.getProfessions())
			if (profession.equalsIgnoreCase(existingProfession))
				professionFound = existingProfession;
		
		if (professionFound == null)
			throw new IllegalArgumentException("That profession does not exist!");
		
		//Check that the player is not at the highest tier.
		if (prof.getTier(professionFound) >= prof.getTiers().size() - 1)
			throw new IllegalArgumentException("That player is already the highest tier in that profession.");
		
		//Add the tier
		prof.setExperience(profession, 0);
		prof.setLevel(profession, 0);
		int newTier = prof.getTier(profession) + 1;
		prof.setTier(profession, newTier);
		return newTier;
	}
	
	/**
	 * giveTier() increases the tier of the player in a certain profession by one.
	 * 
	 * @param uuid
	 * @param profession
	 * @return
	 * @throws IllegalArgumentException
	 */
	public int claimTier(UUID uuid, String profession) throws IllegalArgumentException
	{
		ProfessionStats prof = new ProfessionStats(data, config, uuid);
		int claimed = prof.getClaimed();
		
		//Check if they have reached the maximum number of claimable tiers.
		if (claimed >= config.getConfig().getInt("claimable_tiers"))
			throw new IllegalArgumentException("You do not have any claimable tiers left!");
		
		//Check if they are already maximum tier in that profession.
		if (prof.getTier(profession) == 3)
			throw new IllegalArgumentException("You are already the maximum tier in that profession!");
			
		//Give them a tier.
		int newTier = prof.getTier(profession) + 1;
		prof.setTier(profession, newTier);
		
		//Reset level and exp
		prof.setExperience(profession, 0);
		prof.setLevel(profession, 0);
		
		//Increment the number of tiers they have claimed.
		prof.setClaimed(claimed + 1);
		
		return newTier;
	}
	
	/**
	 * alignText() allows for text to be aligned into columns in the chatbox by adding the appropriate number of spaces
	 * to the end of the string.
	 * @param string - the string to modify.
	 * @param size - the ideal size of the column for the string to occupy.
	 * @return - the modified string.
	 */
	private String alignText(String string, int size) 
	{
	    String alignedString = string;
	    int numSpaces;
	 
	    if (string != null) 
	    {
	        numSpaces = (size - string.length())*2;
	        
	        for (int i = 0; i < alignedString.length(); i++)
	        {
	        	if (alignedString.charAt(i) == 'i' || alignedString.charAt(i) == 'l') 
	            	numSpaces++;
	        	if (alignedString.charAt(i) == 'c' || alignedString.charAt(i) == 'p' || alignedString.charAt(i) == 'v')
	        		numSpaces--;
	        }
	        
	        for (int i2 = 0; i2 < numSpaces; i2++) 
	        	alignedString += " ";
	    }
	    
	    return alignedString;
	}
	
	/**
	 * getTierName() converts an integer to the name of the tier to which it corresponds.
	 * 
	 * @param profession
	 * @return
	 */
	public String getTierName(int tier)
	{
		return config.getConfig().getString("tiers." + tier + ".name");
	}
}
