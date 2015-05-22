package com.gmail.Rhisereld.Horizon_Professions;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfessionCommandExecutor implements CommandExecutor
{
	private final int PROGRESS_BAR_BLOCKS = 20;
	
	Main main;
	
    public ProfessionCommandExecutor(Main main) 
    {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		Player player = (Player) sender;
		
		if (commandLabel.equalsIgnoreCase("profession"))
		{
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
					//Administrator
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

	private void viewStats(Player player) 
	{
		String tier;
		int level;
		int experience;
		int maxLevel;
		int whitespace;
		String message;
		String profession_cap;
		int practiceFatigue;
		int instructionFatigue;
		
		player.sendMessage("------<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">------");
		
		for (int i = 0; i < 5; i++)
		{
			tier = main.getTier(player, main.PROFESSIONS[i]);
			level = main.getMetadataInt(player, main.PROFESSIONS[i] + "_level", Main.plugin);
			experience = main.getMetadataInt(player, main.PROFESSIONS[i] + "_exp", Main.plugin);
			maxLevel = main.getMaxLevel(player, main.PROFESSIONS[i]);
			profession_cap = main.PROFESSIONS[i].substring(0, 1).toUpperCase() + main.PROFESSIONS[i].substring(1);
			practiceFatigue = main.getMetadataInt(player, main.PROFESSIONS[i] + "_practicefatigue", Main.plugin);
			instructionFatigue = main.getMetadataInt(player, main.PROFESSIONS[i] + "_instructionfatigue", Main.plugin);
			
			//Figure out how many spaces to add to make everything line up all pretty.
			whitespace = 15 - main.PROFESSIONS[i].length()*2;
			
			if (whitespace < 0)
				whitespace = 0;
			
			//Build profession header for each profession.
			message = ChatColor.YELLOW + "    " + tier + " " + profession_cap;
			
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
	
	private void viewStatsAdmin(Player player, String string) 
	{
		// TODO Auto-generated method stub
		
	}

	private void giveCommandsGuide(Player player) 
	{
		player.sendMessage("-----<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">-----");
		player.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		player.sendMessage(ChatColor.YELLOW + "/professions view");
		player.sendMessage("View your professions.");
	}
	
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
