package com.gmail.Rhisereld.Horizon_Professions;

import java.util.UUID;

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
	private final int HEADER_WIDTH = 32; 	//The width of the header for each profession when viewing stats.
	
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
						viewStatsAdmin(sender, args[1]);
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
					viewStats(player.getName(), player.getUniqueId(), sender);
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
						forgetTier(sender, args[1], args[2]);
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
					forgetTier(sender, args[1], player.getName());
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
					sender.sendMessage(ChatColor.RED + "Too many arguments! Correct usage: /profession givetier [player] [profession]");
					return false;
				}
				
				//Player did not provide enough arguments
				if (args.length < 3)
				{
					sender.sendMessage(ChatColor.RED + "Too few arguments! Correct usage: /profession givetier [player] [profession]");
					return false;
				}
				
				if (args.length == 3)
				{
					//Console or admin-only command.
					if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizon_professions.givetier.admin"))
						giveTier(sender, args[1], args[2]);
					//Nope
					else
						sender.sendMessage(ChatColor.RED + "You don't have permission to give tiers to players.");
				}
			}
		}
		return false;
	}

	/*
	 * viewStats() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession. Only use for players that are online! Use viewStatsOffline() for offline players.
	 * @param uuid - the uuid of the player's stats being displayed.
	 * @param sender - the viewer to display the stats to.
	 */
	private void viewStats(String name, UUID uuid, CommandSender sender) 
	{
		int tier;
		int level;
		int experience;
		int maxLevel;
		int practiceFatigue;
		int instructionFatigue;
		String professionCapitalised;
		String tierCapitalised;
		String message = null;
		
		sender.sendMessage("----------------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">----------------");
		sender.sendMessage(ChatColor.GOLD + centreText(" Viewing " + name, CHATBOX_WIDTH));
		
		for (int i = 0; i < 5; i++)
		{
			if ((tier = main.getTier(uuid, main.PROFESSIONS[i])) == -1)
			{
				sender.sendMessage("Error fetching tier. Please contact an Administrator.");
				return;
			}
			level = main.getLevel(uuid, main.PROFESSIONS[i]);
			experience = main.getExp(uuid, main.PROFESSIONS[i]);
			maxLevel = main.MAX_LEVEL[tier];
			practiceFatigue = main.getPracticeFatigue(uuid, main.PROFESSIONS[i]);
			instructionFatigue = main.getInstructionFatigue(uuid, main.PROFESSIONS[i]);
			professionCapitalised = main.PROFESSIONS[i].substring(0, 1).toUpperCase() + main.PROFESSIONS[i].substring(1);
			tierCapitalised = main.TIERS[tier].substring(0, 1).toUpperCase() + main.TIERS[tier].substring(1);
			
			
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "  " + alignText(tierCapitalised + " " + professionCapitalised, HEADER_WIDTH);
			
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
	 * viewStatsAdmin() is the admin version of viewStats(). It displays all current stats of any player including 
	 * tiers, levels, experience and fatigue level for each profession.
	 * @param admin - the sender to display the stats to.
	 * @param player - the player to display the stats of.
	 */
	private void viewStatsAdmin(CommandSender admin, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
			viewStats(playerString, offlinePlayer.getUniqueId(), admin);
		}

		//Player is online.
		else
			viewStats(playerString, player.getUniqueId(), admin);
	}
	
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
	private void forgetTier(CommandSender sender, String profession, String playerString) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		UUID uuid;
		int newTier;
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
			uuid = offlinePlayer.getUniqueId();
		}
		else 
			uuid = player.getUniqueId();

		newTier = main.forgetTier(uuid, profession);
		
		sender.sendMessage(ChatColor.YELLOW + playerString + " has forgotten some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
		if (sender instanceof Player && (Player) sender != player)
			player.sendMessage(ChatColor.YELLOW + playerString + " has forgotten some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
	}
	
	/*
	 * giveTier() increases the tier of the player in a certain profession by one.
	 * @param sender - the sender to return messages to.
	 * @param - player - the player for whom to increase the tier.
	 * @param - profession - the profession for which to increase the tier.
	 */
	private void giveTier(CommandSender sender, String playerString, String profession) 
	{
		Player player = Bukkit.getServer().getPlayer(playerString);
		OfflinePlayer offlinePlayer;
		UUID uuid;
		int newTier;
		
		//Player is offline.
		if (player == null)
		{
			offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerString);
			uuid = offlinePlayer.getUniqueId();
		}
		//Player is online.
		else
			uuid = player.getUniqueId();
		
		main.setExp(uuid, profession, 0);
		main.setLevel(uuid, profession, 0);
		if ((newTier = main.gainTier(uuid, profession)) == -1)
		{
			sender.sendMessage("Error giving tier. Please contact an Administrator.");
			return;
		}

		sender.sendMessage(ChatColor.YELLOW + playerString + " has gained some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
		if (sender instanceof Player && (Player) sender != player)
			player.sendMessage(ChatColor.YELLOW + playerString + " has gained some knowledge. They are now a " + main.TIERS[newTier] + " " + profession + ".");
	}

	/*
	 * giveCommandsGuide() displays a list of the available commands.
	 * @param player - the player to display the commands to.
	 */
	private void giveCommandsGuide(Player player) 
	{
		player.sendMessage("-----<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">-----");
		player.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		player.sendMessage(ChatColor.YELLOW + "/profession view");
		player.sendMessage("View your professions.");
		player.sendMessage(ChatColor.YELLOW + "/profession forget [profession]");
		player.sendMessage("Lose a tier in a profession.");
	}
	
	/*
	 * giveCommandsGuideAdmin() displays a list of the available commands to administrators.
	 * @param sender - the sender to display the commands to.
	 */
	private void giveCommandsGuideAdmin(CommandSender sender) 
	{
		sender.sendMessage("-----<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">-----");
		sender.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		sender.sendMessage(ChatColor.YELLOW + "/profession view [optional:player]");
		sender.sendMessage("View the professions of a player.");
		sender.sendMessage(ChatColor.YELLOW + "/profession forget [profession] [optional:player]");
		sender.sendMessage("Force a player to lose a tier in a profession.");
	}
}
