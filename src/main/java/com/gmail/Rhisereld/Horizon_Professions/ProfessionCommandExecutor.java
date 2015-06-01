package com.gmail.Rhisereld.Horizon_Professions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
		Player player = (Player) sender;
		
		//All commands that fall under /profession [additional arguments]
		if (commandLabel.equalsIgnoreCase("profession"))
		{
			//profession
			if (args.length == 0)
			{
				if (player.hasPermission("horizon_profession.help.admin"))
					giveCommandsGuideAdmin(player);
				else 
					giveCommandsGuide(player);
				return false;
			}	
			
			//profession view [player]
			if (args[0].equalsIgnoreCase("view"))
			{
				//Permission required
				if (!player.hasPermission("horizon_professions.view"))
				{
					player.sendMessage(ChatColor.RED + "You don't have permission to view these commands.");
					return false;
				}
				
				//Player-only command
				if (!(player instanceof Player))
				{
					player.sendMessage(ChatColor.RED + "This command can only be used by players.");
					return false;
				}
				
				//Player is attempting to view another player's stats.
				if (args.length == 2)
				{
					//Administrators only
					if (player.hasPermission("horizon_professions.view.admin"))
						viewStatsAdmin(player, args[1]);
					//Nope
					else
						player.sendMessage(ChatColor.RED + "You don't have permission to view another player's trade skills.");
				}
					
				//Player is attempting to view their own stats.
				if (args.length == 1)
				{
					viewStats(player);
				}
				
				return true;
			}
		}
		return false;
	}

	/*
	 * viewStats() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession.
	 * @param player - the player to display the stats for and to.
	 */
	private void viewStats(Player player) 
	{
		String tier;
		int level;
		int experience;
		int maxLevel;
		int whitespace;
		String message;
		String professionCap;
		int practiceFatigue;
		int instructionFatigue;
		
		player.sendMessage("------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">------");
		
		for (int i = 0; i < 5; i++)
		{
			tier = main.getTier(player, main.PROFESSIONS[i]);
			level = main.getLevel(player, main.PROFESSIONS[i]);
			experience = main.getExp(player, main.PROFESSIONS[i]);
			maxLevel = main.getMaxLevel(player, main.PROFESSIONS[i]);
			professionCap = main.PROFESSIONS[i].substring(0, 1).toUpperCase() + main.PROFESSIONS[i].substring(1);
			practiceFatigue = main.getPracticeFatigue(player, main.PROFESSIONS[i]);
			instructionFatigue = main.getInstructionFatigue(player, main.PROFESSIONS[i]);
			
			//Figure out how many spaces to add to make everything line up all pretty.
			whitespace = 15 - main.PROFESSIONS[i].length()*2;
			
			if (whitespace < 0)
				whitespace = 0;
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "    " + tier + " " + professionCap;
			
			for (int i1 = 0; i1 <= whitespace; i1++)
				message = message + " ";
			
			message = message + "    Level " + "[" + level + "/" + maxLevel + "]";
			
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
					message = message + "█";
				else if (i2 < experience / 5)
					message = message + "█";
				else
					message = message + ChatColor.DARK_GRAY + "█";
			}
			
			message = message + ChatColor.YELLOW + " XP";
			
			//Send it.
			player.sendMessage(message);
		}
	}
	
	/*
	 * viewStatsAdmin() is the admin version of viewStats(). It displays all current stats of any player including 
	 * tiers, levels, experience and fatigue level for each profession.
	 * @param mod - the player to display the stats to.
	 * @param player - the player to display the stats of.
	 */
	private void viewStatsAdmin(Player player, String string) 
	{
		// TODO Auto-generated method stub
		
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
	 * giveCommandsGuide() displays a list of the available commands to moderators.
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
}
