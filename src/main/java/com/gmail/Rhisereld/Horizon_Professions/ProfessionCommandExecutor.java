package com.gmail.Rhisereld.Horizon_Professions;

import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ProfessionCommandExecutor implements CommandExecutor
{
	private final int PROGRESS_BAR_BLOCKS = 33; //The number of blocks that appear in the progress bar for command /profession view
	private final int CHATBOX_WIDTH = 44; 	//The number of spaces in one line of the chatbox. May be unreliable for custom
											//fonts
	private final int CONSOLE_WIDTH = 35;	//The number of spaces in one line of the console.
	private final int HEADER_WIDTH = 31; 	//The width of the header for each profession when viewing stats.
	private final int CONSOLE_HEADER_WIDTH = 25;	//The width of the header for the console.
	
	Main main;									//A reference to main.
	
	//Constructor that passes a reference to main.
    public ProfessionCommandExecutor(Main main) 
    {
		this.main = main;
	}

    /*
     * onCommand() is called when a player enters a command recognised by Bukkit to belong to this plugin.
     * After that it is up to the contents of this method to determine what the commands do.
     * 
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		Player player;
		
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
						viewStatsAdmin(sender, args[1].toLowerCase());
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
					
					player = (Player) sender;
					viewStats(player, sender);
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
						forgetTier(sender, args[1].toLowerCase(), args[2].toLowerCase());
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
					
					player = (Player) sender;
					forgetTier(sender, args[1].toLowerCase(), player.getName());
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
						giveTier(sender, args[1].toLowerCase(), args[2].toLowerCase());
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
						resetStats(sender, args[1].toLowerCase());
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
					
					player = (Player) sender;
					resetStats(sender, player.getName());
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

	/*
	 * viewStats() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession. Only use for players that are online! Use viewStatsOffline() for offline players.
	 * @param player - the player for whom the stats are being displayed.
	 * @param sender - the viewer to display the stats to.
	 */
	private void viewStats(Player player, CommandSender sender) 
	{
		String name = player.getName();
		
		int tier;
		int level;
		int experience;
		int maxLevel;
		int practiceFatigue;
		int instructionFatigue;
		String professionCapitalised;
		String tierCapitalised;
		String message = null;
		int chatboxWidth;
		int headerWidth;
		
		if (sender instanceof ConsoleCommandSender)
		{
			chatboxWidth = CONSOLE_WIDTH;
			headerWidth = CONSOLE_HEADER_WIDTH;
		}
		else
		{
			chatboxWidth = CHATBOX_WIDTH;
			headerWidth = HEADER_WIDTH;
		}

		sender.sendMessage("----------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">----------------");
		sender.sendMessage(ChatColor.GOLD + centreText(" Viewing " + name, chatboxWidth));
		
		for (String profession: main.PROFESSIONS)
		{
			if ((tier = main.getTier(player, profession)) == -1)
			{
				sender.sendMessage("Error fetching tier. Please contact an Administrator.");
				return;
			}
			level = main.getLevel(player, profession);
			experience = main.getExp(player, profession);
			maxLevel = main.MAX_LEVEL[tier];
			practiceFatigue = main.getPracticeFatigue(player, profession);
			instructionFatigue = main.getInstructionFatigue(player, profession);
			professionCapitalised = profession.substring(0, 1).toUpperCase() + profession.substring(1);
			tierCapitalised = main.TIERS[tier].substring(0, 1).toUpperCase() + main.TIERS[tier].substring(1);
			
			
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "  " + alignText(tierCapitalised + " " + professionCapitalised, headerWidth);
			
			//If the player has hit max tier, don't even show the level progression
			if (maxLevel == 0)
				message += " Level " + "[Maximum]";
			else
				message += " Level " + "[" + level + "/" + maxLevel + "]";
			
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
				else if (i2 < experience / (main.MAX_EXP/PROGRESS_BAR_BLOCKS))
					message += "█";
				else
					message += ChatColor.DARK_GRAY + "█";
			}
			
			message += ChatColor.YELLOW + " XP";
			
			//Send it.
			sender.sendMessage(message);
		}
	}
	
	/*
	 * viewStatsOffline() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession. Only use for players that are online!
	 * @param player - the player for whom the stats are being displayed.
	 * @param sender - the viewer to display the stats to.
	 */
	private void viewStatsOffline(OfflinePlayer player, CommandSender sender)
	{
		String name = player.getName();
		
		int tier;
		int level;
		int experience;
		int maxLevel;
		int practiceFatigue;
		int instructionFatigue;
		String professionCapitalised;
		String tierCapitalised;
		String message = null;
		int chatboxWidth;
		int headerWidth;
		
		if (sender instanceof ConsoleCommandSender)
		{
			chatboxWidth = CONSOLE_WIDTH;
			headerWidth = CONSOLE_HEADER_WIDTH;
		}
		else
		{
			chatboxWidth = CHATBOX_WIDTH;
			headerWidth = HEADER_WIDTH;
		}
		
		sender.sendMessage("----------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">----------------");
		sender.sendMessage(ChatColor.GOLD + centreText(" Viewing " + name, chatboxWidth));
		
		for (String profession: main.PROFESSIONS)
		{
			if ((tier = main.getTier(player, profession)) == -1)
			{
				sender.sendMessage("Error fetching tier. Please contact an Administrator.");
				return;
			}
			level = main.getLevel(player, profession);
			experience = main.getExp(player, profession);
			maxLevel = main.MAX_LEVEL[tier];
			practiceFatigue = main.getPracticeFatigue(player, profession);
			instructionFatigue = main.getInstructionFatigue(player, profession);
			professionCapitalised = profession.substring(0, 1).toUpperCase() + profession.substring(1);
			tierCapitalised = main.TIERS[tier].substring(0, 1).toUpperCase() + main.TIERS[tier].substring(1);
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "  " + alignText(tierCapitalised + " " + professionCapitalised, headerWidth);
			
			//If the player has hit max tier, don't even show the level progression
			if (maxLevel == 0)
				message += " Level " + "[Maximum]";
			else
				message += " Level " + "[" + level + "/" + maxLevel + "]";
			
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
				else if (i2 < experience / (main.MAX_EXP/PROGRESS_BAR_BLOCKS))
					message += "█";
				else
					message += ChatColor.DARK_GRAY + "█";
			}
			
			message += ChatColor.YELLOW + " XP";
			
			//Send it.
			sender.sendMessage(message);
		}
	}
	
	/*
	 * viewStatsAdmin() controls viewing the stats of another player. It displays all current stats of any player including 
	 * tiers, levels, experience and fatigue level for each profession.
	 * @param admin - the sender to display the stats to.
	 * @param player - the player to display the stats of.
	 */
	@SuppressWarnings("deprecation")
	private void viewStatsAdmin(CommandSender admin, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
			viewStatsOffline(offlinePlayer, admin);
		}

		//Player is online.
		else
			viewStats(player, admin);
	}
	
	/*
	 * alignText() allows for text to be aligned into columns in the chatbox by adding the appropriate number of spaces
	 * to the end of the string.
	 * @param string - the string to modify.
	 * @param size - the ideal size of the column for the string to occupy.
	 * @return - the modified string.
	 */
	private static String alignText(String string, int size) 
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
	
	/*
	 * centreText() allows text to be centred in the middle of the chatbox by adding the appropriate number of spaces to
	 * the beginning and end of the string
	 * @param string - the string to modify
	 * @param size - the ideal size of the column for the string to occupy.
	 * @return - the modified string.
	 */
	private static String centreText(String string, int size) 
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

	/*
	 * forgetTier() reduces the tier of the player in a certain profession by one.
	 * @param sender - the sender to return messages to.
	 * @param profession - the profession for which to reduce a tier.
	 * @param playerString - the player of whom to reduce the tier of.
	 */
	@SuppressWarnings("deprecation")
	private void forgetTier(CommandSender sender, String professionArg, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		String profession = null;
		int newTier;
		
		//Check that the profession argument is one of the professions.
		for (String existingProfession: main.PROFESSIONS)
			if (professionArg.equalsIgnoreCase(existingProfession))
				profession = professionArg;
		
		if (profession == null)
		{
			player.sendMessage(ChatColor.RED + "That profession does not exist!");
			return;
		}
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
			main.setExp(offlinePlayer, profession, 0);
			main.setLevel(offlinePlayer,  profession,  0);
			newTier = main.forgetTier(offlinePlayer, profession);
		}
		//Player is online.
		else 
		{
			main.setExp(player, profession, 0);
			main.setLevel(player,  profession,  0);
			newTier = main.forgetTier(player, profession);
		}
		
		sender.sendMessage(ChatColor.YELLOW + playerString + " has forgotten some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
		if (sender instanceof Player && (Player) sender != player && player != null)
			player.sendMessage(ChatColor.YELLOW + playerString + " has forgotten some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");	
	}
	
	/*
	 * giveTier() increases the tier of the player in a certain profession by one.
	 * @param sender - the sender to return messages to.
	 * @param - player - the player for whom to increase the tier.
	 * @param - profession - the profession for which to increase the tier.
	 */
	@SuppressWarnings("deprecation")
	private void giveTier(CommandSender sender, String profession, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		int newTier;
		
		//Check that the profession argument is one of the professions.
		for (String existingProfession: main.PROFESSIONS)
			if (profession.equalsIgnoreCase(existingProfession))
			{
				//Player is offline.
				if (player == null)
				{
					offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
					main.setExp(offlinePlayer, profession, 0);
					main.setLevel(offlinePlayer, profession, 0);
					if ((newTier = main.gainTier(offlinePlayer, profession)) == -1)
					{
						sender.sendMessage("Error giving tier. Please contact an Administrator.");
						return;
					}
				}
				//Player is online.
				else
				{
					main.setExp(player, profession, 0);
					main.setLevel(player, profession, 0);
					if ((newTier = main.gainTier(player, profession)) == -1)
					{
						sender.sendMessage("Error giving tier. Please contact an Administrator.");
						return;
					}
				}

				sender.sendMessage(ChatColor.YELLOW + playerString + " has gained some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
				if (sender instanceof Player && (Player) sender != player && player != null)
					player.sendMessage(ChatColor.YELLOW + playerString + " has gained some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
		
				return;
			}
		
		sender.sendMessage(ChatColor.YELLOW + "That profession does not exist!");
	}
	
	/*
	 * claimTier() increases the tier of the player in a certain profession by one and updates the number of tiers the 
	 * player has claimed. A new player may claim 3 tiers, and must not already be the maximum tier.
	 * @param player - the player who is claiming a tier
	 * @param profession - the profession in which a player wishes to claim a tier.
	 */
	private void claimTier(Player player, String profession) 
	{
		int newTier;
		int claimed = main.getClaimed(player);
		
		//Check if they have reached the maximum number of claimable tiers.
		if (claimed >= main.CLAIMABLE_TIERS)
		{
			player.sendMessage(ChatColor.RED + "You do not have any claimable tiers left!");
			return;
		}
		
		//Check if they are already maximum tier in that profession.
		if (main.getTier(player, profession) == 3)
		{
			player.sendMessage(ChatColor.RED + "You are already the maximum tier in that profession!");
			return;
		}
			
		//Give them a tier.
		if ((newTier = main.gainTier(player, profession)) == -1)
		{
			player.sendMessage("Error claiming tier. Please contact an Administrator.");
			return;
		}
		
		//Reset level and exp
		main.setExp(player, profession, 0);
		main.setLevel(player, profession, 0);
		
		//Increment the number of tiers they have claimed.
		main.setClaimed(player, claimed + 1);
		
		player.sendMessage(ChatColor.YELLOW + player.getName() + " has gained some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
	}

	/*
	 * resetStats() removes all experience, levels and tiers from the player.
	 * @param playerString - the player who is having their stats reset to 0.
	 */
	@SuppressWarnings("deprecation")
	private void resetStats(CommandSender sender, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
			main.resetPlayerStats(offlinePlayer);
		}
		//Player is online.
		else
			main.resetPlayerStats(player);

		sender.sendMessage(ChatColor.YELLOW + playerString + " has lost all their knowledge.");
		if (sender instanceof Player && (Player) sender != player && player != null)
			player.sendMessage(ChatColor.YELLOW + playerString + " has lost all their knowledge.");
	}
	
	/*
	 * trainPlayer() allows one player to train another in a specified profession. The trainer must be Expert tier in that
	 * profession and the trainee must not be. The trainee will gain two levels in the profession, but will suffer 
	 * instruction fatigue which serves as a cooldown.
	 */
	private void trainPlayer(Player trainer, String profession, String traineeString) 
	{
		Player trainee = Bukkit.getServer().getPlayer(traineeString);
		String trainerString = trainer.getName();
		double distance;
		
		//Check that the trainer is an expert
		if (main.getTier(trainer, profession) < 3)
		{
			trainer.sendMessage(ChatColor.YELLOW + "You cannot train yet because you are not an Expert " + profession + "!");
			return;
		}
		
		//Check that the trainee is online
		if (trainee == null)
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
		
		//Check that the trainee is not already an expert
		if (main.getTier(trainee, profession) >= 3)
		{
			trainer.sendMessage(ChatColor.YELLOW + "You cannot train " + traineeString + " because they are already an Expert!");
			return;
		}
		
		//Check that the trainee is not suffering from instruction fatigue.
		if (main.getInstructionFatigue(trainee, profession) > 0)
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
		main.gainLevel(trainee, profession, 2);
		
		//Set fatigue
		main.setInstructionFatigue(trainee,  profession,  main.FATIGUE_TIME);
		
		//Notify trainer, trainee and any moderators.
		trainer.sendMessage("You have trained " + traineeString + " in the " + profession + " profession.");
		trainee.sendMessage(trainerString + " has trained you in the " + profession + " profession.");
		
		Collection<? extends Player> playerCollection = Bukkit.getServer().getOnlinePlayers();
		
		for (Player player : playerCollection) 
			if (player.hasPermission("horizon_profession.train.admin"))
				player.sendMessage(ChatColor.YELLOW + trainerString + " just trained " + traineeString + " in " + profession + "!");
	}

	/*
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
	
	/*
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
}
