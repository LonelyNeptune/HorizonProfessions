package com.gmail.Rhisereld.Horizon_Professions;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfessionCommandExecutor implements CommandExecutor
{
	Main main;
	
    public ProfessionCommandExecutor(Main main) 
    {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		Player player = (Player) sender;
		
		if (commandLabel.equalsIgnoreCase("professions"))
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
				if (!player.hasPermission("horizon_professions.view"))
				{
					player.sendMessage(ChatColor.RED + "You don't have permission to view these commands.");
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
		
		player.sendMessage("-----<" + ChatColor.GOLD + " Horizon Professions " + ChatColor.WHITE + ">-----");
		
		for (int i = 0; i < 5; i++)
		{
			tier = main.getTier(player, main.PROFESSIONS[i]);
			level = main.getMetadataInt(player, "level_" + main.PROFESSIONS[i], Main.plugin);
			experience = main.getMetadataInt(player, "experience_" + main.PROFESSIONS[i], Main.plugin);
			maxLevel = main.getMaxLevel(player, main.PROFESSIONS[i]);
			player.sendMessage(ChatColor.GOLD + tier + " " + main.PROFESSIONS[i] + ": " + ChatColor.WHITE + "|||||||||||||||||||| " + ChatColor.GOLD + "[" + level + "/" + maxLevel + "]");
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
