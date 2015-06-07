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
	private final int PROGRESS_BAR_BLOCKS = 20; //The number of blocks that appear in the progress bar for command /profession view
	
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
				if (sender instanceof ConsoleCommandSender)
					giveCommandsGuideConsole((ConsoleCommandSender) sender);
				else if (sender.hasPermission("horizon_profession.help.admin"))
					giveCommandsGuideAdmin((Player) sender);
				else 
					giveCommandsGuide((Player) sender);
				return false;
			}	
			
			//profession view [player]
			if (args[0].equalsIgnoreCase("view"))
			{				
				//Permission required
				if (!sender.hasPermission("horizon_professions.view"))
				{
					sender.sendMessage(ChatColor.RED + "You don't have permission to view these commands.");
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
					//Player-only command
					if (!(sender instanceof Player))
					{
						sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
						return false;
					}
					
					player = (Player) sender;
					viewStats(player.getUniqueId(), sender);
				}
				
				return true;
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
	private void viewStats(UUID uuid, CommandSender sender) 
	{
		int tier;
		int level;
		int experience;
		int maxLevel;
		int whitespace;
		int practiceFatigue;
		int instructionFatigue;
		String professionCapitalised;
		String tierCapitalised;
		String message;
		
		sender.sendMessage("------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">------");
		
		for (int i = 0; i < 5; i++)
		{
			tier = main.getTier(uuid, main.PROFESSIONS[i]);
			level = main.getLevel(uuid, main.PROFESSIONS[i]);
			experience = main.getExp(uuid, main.PROFESSIONS[i]);
			maxLevel = main.MAX_LEVEL[tier];
			practiceFatigue = main.getPracticeFatigue(uuid, main.PROFESSIONS[i]);
			instructionFatigue = main.getInstructionFatigue(uuid, main.PROFESSIONS[i]);
			professionCapitalised = main.PROFESSIONS[i].substring(0, 1).toUpperCase() + main.PROFESSIONS[i].substring(1);
			tierCapitalised = main.TIERS[tier].substring(0, 1).toUpperCase() + main.TIERS[tier].substring(1);
			
			//Figure out how many spaces to add to make everything line up all pretty.
			whitespace = 15 - main.PROFESSIONS[i].length()*2;
			
			if (whitespace < 0)
				whitespace = 0;
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "    " + tierCapitalised + " " + professionCapitalised;
			
			for (int i1 = 0; i1 <= whitespace; i1++)
				message = message + " ";
			
			message = message + "    Level " + "[" + level + "/" + maxLevel + "]";
			
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
					message = message + "█";
				else if (i2 < experience / 5)
					message = message + "█";
				else
					message = message + ChatColor.DARK_GRAY + "█";
			}
			
			message = message + ChatColor.YELLOW + " XP";
			
			//Send it.
			sender.sendMessage(message);
		}
	}
	
	/*
	 * viewStatsAdmin() is the admin version of viewStats(). It displays all current stats of any player including 
	 * tiers, levels, experience and fatigue level for each profession.
	 * @param mod - the player to display the stats to.
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
			viewStats(offlinePlayer.getUniqueId(), admin);
		}

		//Player is online.
		else
		{
			player = Bukkit.getServer().getPlayer(playerString);
			viewStats(player.getUniqueId(), admin);
		}
	}

	/*
	 * giveCommandsGuide() displays a list of the available commands.
	 * @param player - the player to display the commands to.
	 */
	private void giveCommandsGuide(Player player) 
	{
		player.sendMessage("-----<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">-----");
		player.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		player.sendMessage(ChatColor.YELLOW + "/professions view");
		player.sendMessage("View your professions.");
	}
	
	/*
	 * giveCommandsGuideAdmin() displays a list of the available commands to administrators.
	 * @param player - the player to display the commands to.
	 */
	private void giveCommandsGuideAdmin(Player player) 
	{
		player.sendMessage("-----<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">-----");
		player.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		player.sendMessage(ChatColor.YELLOW + "/professions view");
		player.sendMessage("View your professions.");
		player.sendMessage(ChatColor.YELLOW + "/professions view [player]");
		player.sendMessage("View the professions of another player.");
	}
	
	/*
	 * giveCommandsGuideConsole() displays a list of the available commands to the console.
	 * @param console - the console.
	 */
	private void giveCommandsGuideConsole(ConsoleCommandSender console)
	{
		console.sendMessage("-----<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">-----");
		console.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		console.sendMessage(ChatColor.YELLOW + "/professions view [player]");
		console.sendMessage("View the professions of another player.");
	}
}
