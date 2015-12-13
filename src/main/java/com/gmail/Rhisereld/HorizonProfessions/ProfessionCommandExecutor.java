package com.gmail.Rhisereld.HorizonProfessions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ProfessionCommandExecutor implements CommandExecutor
{
	private final int PROGRESS_BAR_BLOCKS = 30; //The number of blocks that appear in the progress bar for command /profession view
	private final int HEADER_WIDTH = 30; 		//The width of the header for each profession when viewing stats.
	private final int CONSOLE_HEADER_WIDTH = 25;//The width of the header for the console.
	
	Plugin plugin;
	ConfigAccessor data;
	ConfigAccessor config;
	
	HashMap<String, String> confirmForget = new HashMap<String, String>();	//Used to confirm commands
	HashMap<String, String> confirmReset = new HashMap<String, String>();
	
    public ProfessionCommandExecutor(Plugin plugin, ConfigAccessor data, ConfigAccessor config) 
    {
    	this.plugin = plugin;
    	this.data = data;
    	this.config = config;
	}

    /**
     * onCommand() is called when a player enters a command recognised by Bukkit to belong to this plugin.
     * After that it is up to the contents of this method to determine what the commands do.
     * 
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		String name = sender.getName();
		String[] arguments;

		//All commands that fall under /profession [additional arguments]
		if (commandLabel.equalsIgnoreCase("profession"))
		{
			//profession
			if (args.length == 0)
			{
				if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizon_professions.help.admin"))
					giveCommandsGuideAdmin(sender);
				else if (sender.hasPermission("horizon_professions.help"))
					giveCommandsGuide((Player) sender);
				else
					sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
				return true;
			}	
			
			//profession confirm 
			if (args[0].equalsIgnoreCase("confirm"))
			{
				if (args.length != 2)
				{
					sender.sendMessage(ChatColor.YELLOW + "Incorrect format.");
					return true;
				}
				
				//profession confirm forget
				if (args[1].equalsIgnoreCase("forget"))
				{					
					if (confirmForget.get(name) == null)
					{
						sender.sendMessage(ChatColor.YELLOW + "There is nothing for you to confirm.");
						return true;
					}
					
					arguments = confirmForget.get(name).split(" ");

					forgetTier(sender, arguments[0], arguments[1]);
					confirmForget.remove(name);
					return true;
				}
				//profession confirm reset
				else if (args[1].equalsIgnoreCase("reset"))
				{
					if (confirmReset.get(name) == null)
					{
						sender.sendMessage(ChatColor.YELLOW + "There is nothing for you to confirm?");
						return true;
					}
					
					resetStats(sender, confirmReset.get(name));
					confirmReset.remove(name);
					return true;
				}
			}
			
			//profession view [player]
			if (args[0].equalsIgnoreCase("view"))
			{			
				//Player provided too many arguments
				if (args.length > 2)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession view [optional:player]");
					return false;
				}
				
				//Player is attempting to view another player's stats.
				if (args.length == 2)
				{
					//Console or admin-only command.
					if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizon_professions.view.admin"))
					{
						viewStatsAdmin(args[1], sender);
					}
					//Nope
					else
						sender.sendMessage(ChatColor.RED + "You don't have permission to view another player's professions.");
				}
					
				//Player is attempting to view their own stats.
				if (args.length == 1)
				{
					//Permission required
					if (!sender.hasPermission("horizon_professions.view"))
					{
						sender.sendMessage(ChatColor.RED + "You don't have permission to view professions.");
						return false;
					}
					
					//Player-only command
					if (!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
						return false;
					}
					
					viewStats((Player) sender);
				}
				
				return true;
			}
			
			//profession forget [profession] [player] 
			if (args[0].equalsIgnoreCase("forget"))
			{			
				//Player provided too many arguments
				if (args.length > 3)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession forget [profession] [optional:player]");
					return false;
				}
				
				//Player is attempting to force another player to forget a tier.
				if (args.length == 3)
				{
					//Console or admin-only command.
					if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizon_professions.forget.admin"))
					{
						sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to force " + args[2] + " to forget a tier?"
											+ " Type '/profession confirm forget' to confirm.");
						confirmForget.put(name, args[1] + " " + args[2]);
						confirmForgetTimeout(sender, name);
						return true;
					}
					//Nope
					else
						sender.sendMessage(ChatColor.RED + "You don't have permission to force another player to forget a tier.");
				}
				
				//Player is attempting to forget a tier.
				if (args.length == 2)
				{
					//Permission required
					if (!sender.hasPermission("horizon_professions.forget"))
					{
						sender.sendMessage(ChatColor.RED + "You don't have permission to forget a tier.");
						return false;
					}
					
					//Player-only command
					if (!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
						return false;
					}
					
					sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to forget a tier?"
										+ " Type '/profession confirm forget' to confirm.");
					confirmForget.put(name, args[1] + " " + name);
					confirmForgetTimeout(sender, name);
					return true;
				}
				
				//Player did not provide enough arguments
				if (args.length < 2)
				{
					sender.sendMessage(ChatColor.RED + "Too few arguments! Correct usage: /profession forget [profession] [optional:player]");
					return false;
				}
			}
			
			//profession givetier [profession] [player] 
			if (args[0].equalsIgnoreCase("givetier"))
			{
				//Player provided too many arguments
				if (args.length > 3)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession givetier [profession] [player]");
					return false;
				}
				
				//Player did not provide enough arguments
				if (args.length < 3)
				{
					sender.sendMessage(ChatColor.RED + "Too few arguments! Correct usage: /profession givetier [profession] [player]");
					return false;
				}
				
				if (args.length == 3)
				{
					//Console or admin-only command.
					if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizon_professions.givetier.admin"))
						giveTier(sender, args[1].toLowerCase(), args[2]);
					//Nope
					else
						sender.sendMessage(ChatColor.RED + "You don't have permission to give tiers to players.");
				}
			}
			
			//profession claim [profession]
			if (args[0].equalsIgnoreCase("claim"))
			{
				//Player provided too many arguments
				if (args.length > 2)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession claim [profession]");
					return false;
				}
			
				//Player did not provide enough arguments
				if (args.length < 2)
				{
					sender.sendMessage(ChatColor.RED + "Too few arguments! Correct usage: /profession claim [profession]");
					return false;
				}
				
				if (args.length == 2)
				{
					//Player-only command
					if (!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
						return false;
					}
					
					//Permission required
					if (!sender.hasPermission("horizon_professions.claimtier"))
					{
						sender.sendMessage(ChatColor.RED + "You don't have permission to claim a tier.");
						return false;
					}

					claimTier((Player) sender, args[1].toLowerCase());
				}
			}
			
			//profession reset [player]
			if (args[0].equalsIgnoreCase("reset"))
			{
				//Player provided too many arguments
				if (args.length > 2)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession reset [profession] [optional:player]");
					return false;
				}
				
				//Player is attempting to force another player to reset.
				if (args.length == 2)
				{
					//Console or admin-only command.
					if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizon_professions.reset.admin"))
					{
						sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to force " + args[1] + " to reset?"
								+ " Type '/profession confirm reset' to confirm.");
						confirmReset.put(name, args[1]);
						confirmResetTimeout(sender, name);
						return true;
					}
					//Nope
					else
						sender.sendMessage(ChatColor.RED + "You don't have permission to force another player to reset their professions.");
				}
				
				//Player is attempting to reset.
				if (args.length == 1)
				{
					//Permission required
					if (!sender.hasPermission("horizon_professions.reset"))
					{
						sender.sendMessage(ChatColor.RED + "You don't have permission to reset.");
						return false;
					}
					
					//Player-only command
					if (!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
						return false;
					}
					
					sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to reset?"
										+ " Type '/profession confirm reset' to confirm.");
					confirmReset.put(name, name);
					confirmResetTimeout(sender, name);
					return true;
				}
			}
			
			//profession train [profession] [player]
			if (args[0].equalsIgnoreCase("train"))
			{
				//Player provided too many arguments
				if (args.length > 3)
				{
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession train [profession] [player]");
					return false;
				}
				
				//Player is attempting to train someone
				if (args.length == 3)
				{
					//Player-only command
					if (!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
						return false;
					}

					trainPlayer((Player) sender, args[1].toLowerCase(), args[2].toLowerCase());
				}
				
				//Player did not provide enough arguments
				if (args.length < 3)
				{
					sender.sendMessage(ChatColor.RED + "Too few arguments! Correct usage: /profession train [profession] [player]");
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * viewStats() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession.
	 * @param player - the player for whom the stats are being displayed.
	 * @param sender - the viewer to display the stats to.
	 */
	private void viewStats(Player player) 
	{
		String name = player.getName();
		
		String tier;
		int maxLevel;
		int practiceFatigue;
		int instructionFatigue;
		String message = null;
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());

		player.sendMessage("--------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">--------------");
		player.sendMessage(ChatColor.GOLD + " Viewing " + name);
		
		for (String profession: prof.getProfessions())
		{
			tier = prof.getTierName(profession);
			maxLevel = config.getConfig().getInt("tiers." + prof.getTier(profession) + ".maxlevel");
			practiceFatigue = prof.getPracticeFatigue(profession);
			instructionFatigue = prof.getInstructionFatigue(profession);
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "  " + alignText(tier.substring(0, 1).toUpperCase() + tier.substring(1) + " " 
						+ profession.substring(0, 1).toUpperCase() + profession.substring(1), HEADER_WIDTH);
			
			//If the player has hit max tier, don't even show the level progression
			if (maxLevel == 0)
				message += " Level " + "[Maximum]";
			else
				message += " Level " + "[" + prof.getLevel(profession) + "/" + maxLevel + "]";
			
			//Send it.
			player.sendMessage(message);
			
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
			player.sendMessage(message);
		}
	}
	
	/**
	 * viewStatsAdmin() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession.
	 * @param player - the player for whom the stats are being displayed.
	 * @param sender - the viewer to display the stats to.
	 */
	private void viewStatsAdmin(String player, CommandSender sender) 
	{
		String tier;
		int maxLevel;
		int practiceFatigue;
		int instructionFatigue;
		String tierCapitalised;
		String message = null;
		ProfessionStats prof = new ProfessionStats(data, config, Bukkit.getOfflinePlayer(player).getUniqueId());

		sender.sendMessage("--------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">--------------");
		sender.sendMessage(ChatColor.GOLD + " Viewing " + player);
		
		for (String profession: prof.getProfessions())
		{
			tier = prof.getTierName(profession);
			maxLevel = config.getConfig().getInt("tiers." + prof.getTier(profession) + ".maxlevel");
			practiceFatigue = prof.getPracticeFatigue(profession);
			instructionFatigue = prof.getInstructionFatigue(profession);
			tierCapitalised = tier.substring(0, 1).toUpperCase() + tier.substring(1);
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "  " + alignText(tierCapitalised + " " + profession.substring(0, 1).toUpperCase() 
					+ profession.substring(1), CONSOLE_HEADER_WIDTH);
			
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
	 * centreText() allows text to be centred in the middle of the chatbox by adding the appropriate number of spaces to
	 * the beginning and end of the string
	 * @param string - the string to modify
	 * @param size - the ideal size of the column for the string to occupy.
	 * @return - the modified string.
	 */
	private String centreText(String string, int size) 
	{
	    String alignedString = " ";
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
	        
	        for (int i2 = 0; i2 < numSpaces/2; i2++) 
	        	alignedString += " ";
	        
	        alignedString += string;
	        
	        for (int i2 = 0; i2 < numSpaces/2; i2++) 
	        	alignedString += " ";
	    }
	    
	    return alignedString;
	}

	/**
	 * forgetTier() reduces the tier of the player in a certain profession by one.
	 * @param sender - the sender to return messages to.
	 * @param profession - the profession for which to reduce a tier.
	 * @param playerString - the player of whom to reduce the tier of.
	 */
	private void forgetTier(CommandSender sender, String profession, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		String message;
		
		//Check that the profession argument is one of the professions.
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		for (String existingProfession: prof.getProfessions())
			if (!profession.equalsIgnoreCase(existingProfession))
				{
					player.sendMessage(ChatColor.RED + "That profession does not exist!");
					return;
				}
		
		//Remove the tier
		prof.setExperience(profession, 0);
		prof.setLevel(profession, 0);
		if (prof.getTier(profession) > 0)
			prof.setTier(profession, prof.getTier(profession) - 1);
		
		//Notify sender.
		sender.sendMessage(ChatColor.YELLOW + playerString + " has forgotten some knowledge. They are now " + 
							getDeterminer(prof.getTierName(profession)) + " " + prof.getTierName(profession) + " " + profession + ".");
		
		//If the sender isn't the receiver, notify the receiver too.
		if (sender instanceof Player && (Player) sender != player && player != null)
		{
			player.sendMessage(ChatColor.YELLOW + playerString + " has forgotten some knowledge. They are now " + 
					getDeterminer(prof.getTierName(profession)) + " " + prof.getTierName(profession) + " " + profession + ".");
		}
		else
			return;

		message = ChatColor.GOLD + sender.getName() + " has forced " + playerString + " to forget a level in " 
				+ profession + ".";
		
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
		
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizon_profession.forget.admin"))
				onlinePlayer.sendMessage(message);
		
		createLog(message, "log.txt");
	}
	
	/**
	 * giveTier() increases the tier of the player in a certain profession by one.
	 * 
	 * @param sender
	 * @param profession
	 * @param playerString
	 */
	private void giveTier(CommandSender sender, String profession, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		String message;
		
		//Check that the profession argument is one of the professions.
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		for (String existingProfession: prof.getProfessions())
			if (!profession.equalsIgnoreCase(existingProfession))
				{
					player.sendMessage(ChatColor.RED + "That profession does not exist!");
					return;
				}
		
		//Add the tier
		prof.setExperience(profession, 0);
		prof.setLevel(profession, 0);
		if (prof.getTier(profession) < prof.getTiers().size())
			prof.setTier(profession, prof.getTier(profession) + 1);
		
		//Notify sender.
		sender.sendMessage(ChatColor.YELLOW + playerString + " has gained some knowledge. They are now " + 
							getDeterminer(prof.getTierName(profession)) + " " + prof.getTierName(profession) + " " + profession + ".");
		
		//If the sender isn't the receiver, notify the receiver too.
		if (sender instanceof Player && (Player) sender != player && player != null)
		{
			player.sendMessage(ChatColor.YELLOW + playerString + " has gained some knowledge. They are now " + 
					getDeterminer(prof.getTierName(profession)) + " " + prof.getTierName(profession) + " " + profession + ".");
		}
		else
			//If the sender is the receiver there isn't any need to notify further or create logs.
			return;

		message = ChatColor.GOLD + sender.getName() + " has given a tier in " + profession + " to " + playerString;
		
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
		
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizon_profession.givetier"))
				onlinePlayer.sendMessage(message);
		
		createLog(message, "log.txt");
	}
	
	/**
	 * claimTier() increases the tier of the player in a certain profession by one and updates the number of tiers the 
	 * player has claimed.
	 * @param player - the player who is claiming a tier
	 * @param profession - the profession in which a player wishes to claim a tier.
	 */
	private void claimTier(Player player, String profession) 
	{
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		int claimed = prof.getClaimed();
		
		//Check if they have reached the maximum number of claimable tiers.
		if (claimed >= config.getConfig().getInt("claimable_tiers"))
		{
			player.sendMessage(ChatColor.RED + "You do not have any claimable tiers left!");
			return;
		}
		
		//Check if they are already maximum tier in that profession.
		if (prof.getTier(profession) == 3)
		{
			player.sendMessage(ChatColor.RED + "You are already the maximum tier in that profession!");
			return;
		}
			
		//Give them a tier.
		prof.setTier(profession, prof.getTier(profession) + 1);
		
		//Reset level and exp
		prof.setExperience(profession, 0);
		prof.setLevel(profession, 0);
		
		//Increment the number of tiers they have claimed.
		prof.setClaimed(claimed + 1);
		
		player.sendMessage(ChatColor.YELLOW + player.getName() + " has gained some knowledge. They are now " + 
							getDeterminer(prof.getTierName(profession)) + " " + prof.getTierName(profession) + " " + profession + ".");
	}
	
	/**
	 * resetStats() removes all experience, levels and tiers from the player.
	 * @param sender
	 * @param playerString - the player who is having their stats reset to 0.
	 */
	private void resetStats(CommandSender sender, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		String message;
		
		//Reset all stats
		prof.reset();

		//Notify sender.
		sender.sendMessage(ChatColor.YELLOW + playerString + " has lost all their knowledge.");
		//If the sender is not the receiver, notify the receiver too.
		if (sender instanceof Player && (Player) sender != player && player != null)
		{
			player.sendMessage(ChatColor.YELLOW + playerString + " has lost all their knowledge.");
		}
		else
			return;

		message = ChatColor.GOLD + sender.getName() + " has forced " + playerString + " to reset.";
		
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
		
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizon_profession.reset.admin"))
				onlinePlayer.sendMessage(message);
		
		createLog(message, "log.txt");
	}
	
	/**
	 * trainPlayer() allows one player to train another in a specified profession. The trainer must be the top tier in that
	 * profession and the trainee must not be. The trainee will gain two levels in the profession, but will suffer 
	 * instruction fatigue which serves as a cooldown.
	 * @param trainer
	 * @param profession
	 * @param traineeString
	 */
	private void trainPlayer(Player trainer, String profession, String traineeString) 
	{
		Player trainee = Bukkit.getServer().getPlayer(traineeString);
		ProfessionStats profTrainer = new ProfessionStats(data, config, trainer.getUniqueId());
		ProfessionStats profTrainee = new ProfessionStats(data, config, trainee.getUniqueId());
		double distance;
		String message;
		
		//Check that the trainer is the top tier
		List<String> tiers = profTrainer.getTiers();
		
		if (profTrainer.getTier(profession) < tiers.size() - 1)
		{
			trainer.sendMessage(ChatColor.YELLOW + "You cannot train yet because you are not " 
								+ getDeterminer(tiers.get(tiers.size()-1)) + " " + tiers.get(tiers.size()-1) + " " + profession + "!");
			return;
		}
		
		//Check that the trainee is online
		if (!trainee.isOnline())
		{
			trainer.sendMessage(ChatColor.YELLOW + "You cannot train " + traineeString + " because they are not online!");
			return;
		}
		
		//Trying to train yourself is funny, but not allowed.
		if (trainer.getUniqueId() == trainee.getUniqueId())
		{
			trainer.sendMessage(ChatColor.YELLOW + "You cannot train yourself, silly!");
			return;
		}
		
		//Check that the trainee is not already at the top tier
		if (profTrainee.getTier(profession) >= 3)
		{
			trainer.sendMessage(ChatColor.YELLOW + "You cannot train " + traineeString + " because they are already an Expert!");
			return;
		}
		
		//Check that the trainee is not suffering from instruction fatigue.
		if (profTrainee.getInstructionFatigue(profession) > 0)
		{
			trainer.sendMessage(ChatColor.YELLOW + traineeString + " has already benefitted from instruction today.");
			return;
		}
		
		//Check that the trainer and trainee are reasonably close together and in the same world.
		distance = trainer.getLocation().distance(trainee.getLocation());

		if (!trainer.getWorld().equals(trainee.getWorld()) || Double.isNaN(distance) || distance > 20)
		{
			trainer.sendMessage("You are too far away to train " + traineeString + "!");
			return;
		}
		
		//Give levels
		profTrainee.addLevel(profession, 2);
		
		//Set fatigue
		profTrainee.setInstructionFatigue(profession, config.getConfig().getInt("fatigue_time"));
		
		//Notify trainer, trainee and any moderators.
		trainer.sendMessage("You have trained " + traineeString + " in the " + profession + " profession.");
		trainee.sendMessage(trainer.getCustomName() + " has trained you in the " + profession + " profession.");
		
		message = ChatColor.YELLOW + trainer.getName() + " just trained " + traineeString + " in the " + profession + " profession!";
		
		Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : onlinePlayers) 
			if (player.hasPermission("horizon_profession.train.admin"))
				player.sendMessage(message);
		
		createLog(message, "trainlog.txt");
	}


	/**
	 * giveCommandsGuide() displays a list of the available commands.
	 * @param player - the player to display the commands to.
	 */
	private void giveCommandsGuide(Player player) 
	{
		player.sendMessage("----------------<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">----------------");
		player.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		player.sendMessage(ChatColor.YELLOW + "/profession view");
		player.sendMessage("View your professions.");
		player.sendMessage(ChatColor.YELLOW + "/profession forget [profession]");
		player.sendMessage("Lose a tier in a profession.");
		player.sendMessage(ChatColor.YELLOW + "/profession claim [profession]");
		player.sendMessage("Claim a free tier in a profession.");
		player.sendMessage(ChatColor.YELLOW + "/profession reset");
		player.sendMessage("Resets all of your progress to zero.");
		player.sendMessage(ChatColor.YELLOW + "/profession train [profession] [player]");
		player.sendMessage("Trains another player in a specified profession. They will gain two levels.");
	}
	
	/**
	 * giveCommandsGuideAdmin() displays a list of the available commands to administrators.
	 * @param sender - the sender to display the commands to.
	 */
	private void giveCommandsGuideAdmin(CommandSender sender) 
	{
		sender.sendMessage("------------<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">------------");
		sender.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		sender.sendMessage(ChatColor.YELLOW + "/profession view [optional:player]");
		sender.sendMessage("View the professions of a player.");
		sender.sendMessage(ChatColor.YELLOW + "/profession forget [profession] [optional:player]");
		sender.sendMessage("Force a player to lose a tier in a profession.");
		sender.sendMessage(ChatColor.YELLOW + "/profession givetier [profession] [player]");
		sender.sendMessage("Give a tier to a player in the specified profession.");
		sender.sendMessage(ChatColor.YELLOW + "/profession claim [profession]");
		sender.sendMessage("Claim a free tier in a profession.");
		sender.sendMessage(ChatColor.YELLOW + "/profession reset [optional:player]");
		sender.sendMessage("Resets all of the player's progress to zero.");
		sender.sendMessage(ChatColor.YELLOW + "/profession train [profession] [player]");
		sender.sendMessage("Trains another player in a specified profession. They will gain two levels.");
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
	
	/**
	 * createLog() saves the string provided to the file log.txt.
	 * @param String - the message to be logged.
	 * @param time - the time the message was logged.
	 */
	private void createLog(String message, String filename)
    {
		File saveTo = new File("plugins\\horizon_professions\\" + filename);
		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		PrintWriter out = null;
		
		//Add timestamp to the message.
		message = timestamp.toString() + " - " + message;

		try
		{
			File dataFolder = plugin.getDataFolder();
			if(!dataFolder.exists())
			{
				dataFolder.mkdir();
			}
	 
			if (!saveTo.exists())
			{
				saveTo.createNewFile();
			}
	 
			FileWriter fw = new FileWriter(saveTo, true);
			PrintWriter pw = new PrintWriter(fw);
	 
			pw.println(message);
			pw.close();
	 
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	 
		finally
		{
		    if(out != null)
		    out.close();
		} 
    }
	
	private void confirmForgetTimeout(final CommandSender sender, final String key)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{
			public void run() 
			{
				if (confirmForget.containsKey(key))
				{
					confirmForget.remove(key);
					sender.sendMessage(ChatColor.YELLOW + "You timed out.");
				}
			}			
		} , 200);
	}
	
	private void confirmResetTimeout(final CommandSender sender, final String key)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{
			public void run() 
			{
				if (confirmReset.containsKey(key))
				{
					confirmReset.remove(key);
					sender.sendMessage(ChatColor.YELLOW + "You timed out.");
				}
			}			
		} , 200);
	}
}
