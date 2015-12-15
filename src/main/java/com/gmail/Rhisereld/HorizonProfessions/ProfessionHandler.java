package com.gmail.Rhisereld.HorizonProfessions;

import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ProfessionHandler 
{
	Permission perms;
	FileConfiguration data;
	FileConfiguration config;
	
	private final int PROGRESS_BAR_BLOCKS = 30; //The number of blocks that appear in the progress bar for command /profession view
	private final int HEADER_WIDTH = 30; 		//The width of the header for each profession when viewing stats.
	private final int CONSOLE_HEADER_WIDTH = 25;//The width of the header for the console.
	
	public ProfessionHandler(Permission perms, FileConfiguration data, FileConfiguration config)
	{
		this.perms = perms;
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
		boolean practiceFatigue;
		boolean instructionFatigue;
		String message = null;
		ProfessionStats prof = new ProfessionStats(perms, data, config, player);

		sender.sendMessage("--------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">--------------");
		sender.sendMessage(ChatColor.GOLD + "  Viewing " + name);
		
		for (String profession: prof.getProfessions())
		{
			tier = getTierName(prof.getTier(profession));
			maxLevel = config.getInt("tiers." + prof.getTier(profession) + ".maxLevel");
			practiceFatigue = prof.isPracticeFatigued(profession);
			instructionFatigue = prof.isInstructionFatigued(profession);
			
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
			if (practiceFatigue && instructionFatigue)
				message = "" + ChatColor.RED;
			else if (practiceFatigue || instructionFatigue)
					message = "" + ChatColor.GOLD;
			else 
				message = "" + ChatColor.GREEN;
			
			for (int i = 0; i < PROGRESS_BAR_BLOCKS; i++)
			{
				if (practiceFatigue)
					message += "█";
				else if (i < prof.getExperience(profession) / (config.getInt("max_exp")/PROGRESS_BAR_BLOCKS))
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
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
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
		prof.resetExperience(profession);
		prof.resetLevel(profession);
		int newTier = prof.getTier(profession) - 1;
		prof.loseTier(profession);
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
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		boolean professionFound = false;
		for (String existingProfession: prof.getProfessions())
			if (profession.equalsIgnoreCase(existingProfession))
				professionFound = true;
		
		if (!professionFound)
			throw new IllegalArgumentException("That profession does not exist!");
		
		//Check that the player is not at the highest tier.
		if (prof.getTier(profession) >= prof.getTiers().size() - 1)
			throw new IllegalArgumentException("That player is already the highest tier in that profession.");
		
		//Check that the player hasn't exceeded the total number of players
		if (prof.getTotalTiers() >= config.getInt("tier_cap"))
			throw new IllegalArgumentException("That player already has the maximum number of tiers allowed.");
		
		//Add the tier
		prof.resetExperience(profession);
		prof.resetLevel(profession);
		prof.addTier(profession);
		return prof.getTier(profession);
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
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		int claimed = prof.getClaimed();
		
		//Check if they have reached the maximum number of claimable tiers.
		if (claimed >= config.getInt("claimable_tiers"))
			throw new IllegalArgumentException("You do not have any claimable tiers left!");
		
		//Check if they are already maximum tier in that profession.
		if (prof.getTier(profession) == 3)
			throw new IllegalArgumentException("You are already the maximum tier in that profession!");
		
		//Check if they have reached the maximum number of allowed tiers.
		if (prof.getTotalTiers() >= config.getInt("tier_cap"))
			throw new IllegalArgumentException("You have already reached the maximum number of tiers allowed.");	
		
		//Give them a tier.
		int newTier = prof.getTier(profession) + 1;
		prof.addTier(profession);
		
		//Reset level and exp
		prof.resetExperience(profession);
		prof.resetLevel(profession);
		
		//Increment the number of tiers they have claimed.
		prof.setClaimed(claimed + 1);
		
		return newTier;
	}
	
	/**
	 * train() performs checks to ensure the training can be done, then increases the trainee's levels by two and sets instruction fatigue
	 * which serves as a cooldown for training.
	 * 
	 * @param sender
	 * @param traineeName
	 * @param profession
	 * @return Returns true if the training resulted in a gained tier, false otherwise.
	 */
	public String train(CommandSender sender, String traineeName, String profession) throws IllegalArgumentException
	{
		Player trainee = Bukkit.getPlayer(traineeName);
		Player trainer = (Player) sender;

		ProfessionStats profTrainer = new ProfessionStats(perms, data, config, trainer.getUniqueId());
		ProfessionStats profTrainee = new ProfessionStats(perms, data, config, trainee.getUniqueId());
		
		//Check that the trainer is the top tier
		List<String> tiers = profTrainer.getTiers();
		
		if (profTrainer.getTier(profession) < tiers.size() - 1)
			throw new IllegalArgumentException("You cannot train yet because you are not " 
					+ getDeterminer(tiers.get(tiers.size()-1)) + " " + tiers.get(tiers.size()-1) + " " + profession + "!");
		
		//Trying to train yourself is funny, but not allowed.
		if (trainer.getUniqueId() == trainee.getUniqueId())
			throw new IllegalArgumentException("You cannot train yourself, silly!");
		
		//Check that the trainee is not already at the top tier
		if (profTrainee.getTier(profession) >= 3)
			throw new IllegalArgumentException("You cannot train " + traineeName + " because they are already an " 
					+ getDeterminer(tiers.get(tiers.size()-1)) + " " + tiers.get(tiers.size()-1) + ".");
		
		//Check that the trainee has not already got the maximum number of tiers
		if (profTrainee.getTotalTiers() >= config.getInt("tier_cap"))
			throw new IllegalArgumentException("You cannot train " + traineeName + " because they have already reached "
					+ "the maximum number of tiers allowed.");
		
		//Check that the trainee is not suffering from instruction fatigue.
		if (profTrainee.isInstructionFatigued(profession))
			throw new IllegalArgumentException(traineeName + " has already benefitted from instruction today.");
		
		//Check that the trainer and trainee are reasonably close together and in the same world.
		if (!trainer.getWorld().equals(trainee.getWorld()))
			throw new IllegalArgumentException("You are too far away to train " + traineeName + "!");
		
		double distance = trainer.getLocation().distance(trainee.getLocation());
		if (Double.isNaN(distance) || distance > 20)
			throw new IllegalArgumentException("You are too far away to train " + traineeName + "!");
		
		//Set fatigue
		profTrainee.setInstructionFatigue(profession);
		
		//Give levels
		String message = null;
		if (profTrainee.addLevel(profession, 2))
			message = "Thanks to your training, you have become " + getDeterminer(profTrainee.getTierName(profTrainee.getTier(profession)))
					+ " " + profTrainee.getTierName(profTrainee.getTier(profession)) + " " + profession + ".";
		
		return message;
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
		return config.getString("tiers." + tier + ".name");
	}
	
	/** 
	 * getDeterminer() returns the determiner that should occur before a noun.
	 * @param string - the noun.
	 * @return - "an" if the noun begins with a vowel, "a" otherwise.
	 */
	private String getDeterminer(String string)
	{
		if (string.charAt(0) == 'a' || string.charAt(0) == 'e' || string.charAt(0) == 'i' || string.charAt(0) == 'o'
				|| string.charAt(0) == 'u')
			return "an";
		else
			return "a";
	}
}
