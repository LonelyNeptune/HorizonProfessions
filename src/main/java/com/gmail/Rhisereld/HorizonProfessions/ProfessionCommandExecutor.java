package com.gmail.Rhisereld.HorizonProfessions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ProfessionCommandExecutor implements CommandExecutor
{	
	JavaPlugin plugin;
	Permission perms;
	FileConfiguration data;
	FileConfiguration config;
	
	HashMap<String, String> confirmForget = new HashMap<String, String>();	//Used to confirm commands
	HashMap<String, String> confirmReset = new HashMap<String, String>();
	
    public ProfessionCommandExecutor(JavaPlugin plugin, Permission perms, FileConfiguration data, FileConfiguration config) 
    {
    	this.plugin = plugin;
    	this.perms = perms;
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
		if (commandLabel.equalsIgnoreCase("profession") || commandLabel.equalsIgnoreCase("prof"))
		{
			//profession
			if (args.length == 0)
			{
				if (sender instanceof ConsoleCommandSender || sender.hasPermission("horizonprofessions.help.admin") 
						|| sender.hasPermission("horizonprofessions.help"))
					return giveCommandsGuide(sender);
				else
					sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
				return true;
			}	
			
			//profession reload
			if (args[0].equalsIgnoreCase("reload"))
				if (!(sender instanceof Player) || sender.hasPermission("horizonprofessions.reload"))
				{
					config = new ConfigAccessor(plugin, "config.yml").getConfig();
					ProfessionListener.updateConfig(config);
					CraftListener.updateConfig(config);
					ProfessionAPI.updateConfig(config);
					
					sender.sendMessage(ChatColor.YELLOW + "Horizon Professions config reloaded.");
					return true;
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "You don't have permission to use this commend!");
					return false;
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
					return confirmForgetTier(sender, arguments[0], arguments[1]);
				}
				//profession confirm reset
				else if (args[1].equalsIgnoreCase("reset"))
				{
					if (confirmReset.get(name) == null)
					{
						sender.sendMessage(ChatColor.YELLOW + "There is nothing for you to confirm.");
						return true;
					}
					
					return confirmResetStats(sender, confirmReset.get(name));
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
					return viewStatsAdmin(args[1], sender);
					
				//Player is attempting to view their own stats.
				if (args.length == 1)
					return viewStats(sender);
			}
			
			//profession forget [profession] [player] 
			if (args[0].equalsIgnoreCase("forget"))
			{			
				//Player provided an incorrect number of arguments.
				if (args.length > 3 || args.length < 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /profession forget [profession] "
							+ "[optional:player]");
					return false;
				}
				
				//Player is attempting to force another player to forget a tier.
				if (args.length == 3)
					return forgetTier(sender, args[1].toLowerCase(), args[2]);
				
				//Player is attempting to forget a tier.
				if (args.length == 2)
					return forgetTier(sender, args[1].toLowerCase(), sender.getName());
			}
			
			//profession givetier [profession] [player] 
			if (args[0].equalsIgnoreCase("givetier"))
			{
				//Player provided an incorrect number of arguments.
				if (args.length > 3 || args.length < 3)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /profession givetier [profession] [player]");
					return false;
				}
				
				if (args.length == 3)
					return giveTier(sender, args[1].toLowerCase(), args[2]);
			}
			
			//profession claim [profession]
			if (args[0].equalsIgnoreCase("claim"))
			{
				//Player provided too many arguments
				if (args.length != 2)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /profession claim [profession]");
					return false;
				}
				
				if (args.length == 2)
					return claimTier(sender, args[1].toLowerCase());
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
					return resetStats(sender, args[1]);
				
				//Player is attempting to reset.
				if (args.length == 1)
					return resetStats(sender, sender.getName());
			}
			
			//profession train [profession] [player]
			if (args[0].equalsIgnoreCase("train"))
			{
				if (args.length != 3)
				{
					sender.sendMessage(ChatColor.RED + "Incorrect number of arguments! Correct usage: /profession train [profession] [player]");
					return false;
				}
				
				return trainPlayer(sender, args[1].toLowerCase(), args[2]);
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
	private boolean viewStats(CommandSender sender) 
	{
		//Check that the sender is a player
		if (!(sender instanceof Player))
		{
			sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
			return false;
		}
		
		//Check that the player has permission
		if (!sender.hasPermission("horizonprofessions.view"))
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to view your professions.");
			return false;
		}
		
		Player player = (Player) sender;
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		profHandler.displayStats(player.getUniqueId(), player.getName(), sender);
		
		return true;
	}
	
	/**
	 * viewStatsAdmin() displays all current stats to the player including tiers, levels, experience and fatigue level
	 * for each profession.
	 * @param player - the player for whom the stats are being displayed.
	 * @param sender - the viewer to display the stats to.
	 */
	private boolean viewStatsAdmin(String name, CommandSender sender) 
	{
		//Check that the player has permission OR is the console
		if (!sender.hasPermission("horizonprofessions.view.admin") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to view another player's professions.");
			return false;
		}
		
		Player player = Bukkit.getPlayer(name);
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		if (player == null)
		{
			UUID uuid = profHandler.getUUID(name);
			if (uuid == null)
			{
				sender.sendMessage(ChatColor.RED + "That player does not exist!");
				return false;
			}
			profHandler.displayStats(profHandler.getUUID(name), name, sender);
		}
		else
			profHandler.displayStats(player.getUniqueId(), name, sender);
		
		return true;
	}

	/**
	 * forgetTier() checks that the command is valid, asks for confirmation and sets the command to await for confirmation.
	 * @param sender - the sender to return messages to.
	 * @param profession - the profession for which to reduce a tier.
	 * @param playerString - the player of whom to reduce the tier of.
	 */
	private boolean forgetTier(CommandSender sender, String profession, String name) 
	{
		//If the sender is the target
		if (sender.getName().equalsIgnoreCase(name))
		{
			//Console cannot use the command like this
			if (!(sender instanceof Player))
			{
				sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
				return false;
			}
			
			//Player must have permission
			if (!sender.hasPermission("horizonprofessions.forget"))
			{
				sender.sendMessage(ChatColor.RED + "You do not have permission to forget a tier.");
				return false;
			}			
			
			//Ask for confirmation
			sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to forget a tier?"
					+ " Type '/profession confirm forget' to confirm.");
		}
		//If the sender is NOT the target
		else
		{
			//Sender must have permission OR be the console
			if (sender instanceof Player && !sender.hasPermission("horizonprofessions.forget.admin"))
			{
				sender.sendMessage(ChatColor.RED + "You don't have permission to force another player to forget a tier.");
				return false;
			}
			
			//Ask for confirmation
			sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to force " + name + " to forget a tier?"
					+ " Type '/profession confirm forget' to confirm.");
		}

		//Await confirmation
		confirmForget.put(sender.getName(), profession + " " + name);
		confirmForgetTimeout(sender, sender.getName());
		return true;
	}
	
	/**
	 * confirmForgetTier() performs the confirmed command of reducing the tier of the player in a certain profession by one.
	 * @param sender - the sender to return messages to.
	 * @param profession - the profession for which to reduce a tier.
	 * @param playerString - the player of whom to reduce the tier of.
	 */
	private boolean confirmForgetTier(CommandSender sender, String profession, String name)
	{
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		Player player = Bukkit.getPlayer(name);
		int newTier;
		UUID uuid;
		
		//Stop waiting for confirmation
		confirmForget.remove(sender.getName());
		
		//Perform the action
		if (player == null)
			uuid = profHandler.getUUID(name);
		else
			uuid = player.getUniqueId();
		
		//If uuid is null then player is not saved.
		if (uuid == null)
		{
			sender.sendMessage(ChatColor.RED + "That player does not exist!");
			return false;
		}
		
		try {newTier = profHandler.forgetTier(uuid, profession);}
		catch (IllegalArgumentException e) 
		{
			sender.sendMessage(ChatColor.RED + e.getMessage());
			return false;
		}
		
		String tierName = profHandler.getTierName(newTier);

		//Notify sender.
		sender.sendMessage(ChatColor.YELLOW + name + " has forgotten some knowledge. They are now " + getDeterminer(tierName) 
							+ " " + tierName + " " + profession + ".");
				
		//If the sender isn't the receiver, notify the receiver too.
		if (sender instanceof Player && (Player) sender != player && player != null)
		{
			player.sendMessage(ChatColor.YELLOW + name + " has forgotten some knowledge. They are now " + 
					getDeterminer(tierName) + " " + tierName + " " + profession + ".");
		}
		else
			return true;

		String message = ChatColor.GOLD + sender.getName() + " has forced " + name + " to forget a level in " 
						+ profession + ".";
				
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
				
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizonprofessions.forget.admin"))
				onlinePlayer.sendMessage(message);
			
		createLog(message, "log.txt");
		
		return true;
	}
	
	/**
	 * giveTier() increases the tier of the player in a certain profession by one.
	 * 
	 * @param sender
	 * @param profession
	 * @param playerString
	 */
	private boolean giveTier(CommandSender sender, String profession, String name) 
	{
		//Check that the player has permission OR is the console
		if (!sender.hasPermission("horizonprofessions.givetier") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to give tiers.");
			return false;
		}
		
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		Player player = Bukkit.getPlayer(name);
		int newTier;
		UUID uuid;
		
		if (player == null)
			uuid = profHandler.getUUID(name);
		else
			uuid = player.getUniqueId();
		
		//If uuid is null then player is not saved.
		if (uuid == null)
		{
			sender.sendMessage(ChatColor.RED + "That player does not exist!");
			return false;
		}
		
		try { newTier = profHandler.giveTier(uuid, profession);}
		catch (IllegalArgumentException e)
		{
			sender.sendMessage(ChatColor.RED + e.getMessage());
			return false;
		}
		
		String tierName = profHandler.getTierName(newTier);

		//Notify sender.
		sender.sendMessage(ChatColor.YELLOW + name + " has gained some knowledge. They are now " + getDeterminer(tierName) 
							+ " " + tierName + " " + profession + ".");
				
		//If the sender isn't the receiver, notify the receiver too.
		if (sender instanceof Player && (Player) sender != player && player != null)
		{
			player.sendMessage(ChatColor.YELLOW + name + " has gained some knowledge. They are now "  + 
					getDeterminer(tierName) + " " + tierName + " " + profession + ".");
		}
		else
			return true;

		String message = ChatColor.GOLD + sender.getName() + " has given a tier in " + profession + " to " + name;
				
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
				
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizonprofessions.givetier"))
				onlinePlayer.sendMessage(message);
			
		createLog(message, "log.txt");
		return true;
	}
	
	/**
	 * claimTier() increases the tier of the player in a certain profession by one and updates the number of tiers the 
	 * player has claimed.
	 * @param player - the player who is claiming a tier
	 * @param profession - the profession in which a player wishes to claim a tier.
	 */
	private boolean claimTier(CommandSender sender, String profession) 
	{
		//Check that the sender is a player
		if (!(sender instanceof Player))
		{
			sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
			return false;
		}
		
		//Check that the player has permission
		if (!sender.hasPermission("horizonprofessions.claimtier") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to claim a tier.");
			return false;
		}
		
		Player player = (Player) sender;
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		String newTier;
		
		try {newTier = profHandler.getTierName(profHandler.claimTier(player.getUniqueId(), profession));}
		catch (IllegalArgumentException e)
		{
			sender.sendMessage(ChatColor.RED + e.getMessage());
			return false;
		}
		
		player.sendMessage(ChatColor.YELLOW + player.getName() + " has gained some knowledge. They are now " + 
							getDeterminer(newTier) + " " + newTier + " " + profession + ".");
		
		return true;
	}
	
	/**
	 * resetStats() checks that the command is valid, asks for confirmation and sets the command to await for confirmation.
	 * @param sender
	 * @param playerString - the player who is having their stats reset to 0.
	 * @return 
	 */
	private boolean resetStats(CommandSender sender, String playerName) 
	{		
		//If the sender is the target
		if (sender.getName().equalsIgnoreCase(playerName))
		{
			//Console cannot use the command like this
			if (!(sender instanceof Player))
			{
				sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
				return false;
			}
			
			//Player must have permission
			if (!sender.hasPermission("horizonprofessions.reset"))
			{
				sender.sendMessage(ChatColor.RED + "You do not have permission to reset your professions.");
				return false;
			}			
			
			//Ask for confirmation
			sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to reset your professions? You will lose all your progress!"
					+ " Type '/profession confirm reset' to confirm.");
		}
		//If the sender is NOT the target
		else
		{
			//Sender must have permission OR be the console
			if (sender instanceof Player && !sender.hasPermission("horizonprofessions.reset.admin"))
			{
				sender.sendMessage(ChatColor.RED + "You don't have permission to force another player to reset.");
				return false;
			}
			
			//Ask for confirmation
			sender.sendMessage(ChatColor.YELLOW + "Are you sure you want to force " + playerName + " to reset?"
					+ " Type '/profession confirm reset' to confirm.");
		}

		//Await confirmation
		confirmReset.put(sender.getName(), playerName);
		confirmResetTimeout(sender, sender.getName());
		return true;
	}
	
	/**
	 * confirmResetStats() removes all experience, levels and tiers from the player.
	 * @param sender
	 * @param playerString - the player who is having their stats reset to 0.
	 * @return 
	 */
	private boolean confirmResetStats(CommandSender sender, String name)
	{
		Player player = Bukkit.getPlayer(name);
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		
		//Stop waiting for confirmation
		confirmReset.remove(sender.getName());
		
		//Reset all stats
		UUID uuid;
		if (player == null)
			uuid = profHandler.getUUID(name);
		else
			uuid = player.getUniqueId();
		
		//If uuid is null then player is not saved.
		if (uuid == null)
		{
			sender.sendMessage(ChatColor.RED + "That player does not exist!");
			return false;
		}
		
		profHandler.reset(uuid);

		//Notify sender.
		sender.sendMessage(ChatColor.YELLOW + name + " has lost all their knowledge.");
		//If the sender is not the receiver, notify the receiver too.
		if (sender instanceof Player && (Player) sender != player && player != null)
		{
			player.sendMessage(ChatColor.YELLOW + name + " has lost all their knowledge.");
		}
		else
			return true;

		String message = ChatColor.GOLD + sender.getName() + " has forced " + name + " to reset.";
		
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizonprofessions.reset.admin"))
				onlinePlayer.sendMessage(message);
		
		createLog(message, "log.txt");
		return true;
	}
	
	/**
	 * trainPlayer() allows one player to train another in a specified profession. The trainer must be the top tier in that
	 * profession and the trainee must not be. The trainee will gain two levels in the profession, but will suffer 
	 * instruction fatigue which serves as a cooldown.
	 * @param sender
	 * @param profession
	 * @param traineeString
	 * @return 
	 */
	private boolean trainPlayer(CommandSender sender, String profession, String traineeName) 
	{		
		//Check that the sender is a player
		if (!(sender instanceof Player))
		{
			sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
			return false;
		}
		
		//Check that the trainee is online
		Player trainee = Bukkit.getPlayer(traineeName);
		if (trainee == null)
		{
			sender.sendMessage(ChatColor.YELLOW + "You cannot train " + traineeName + " because they are not online!");
			return false;
		}
		
		//Perform the training.
		ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
		String tierUp;
		try { tierUp = profHandler.train(sender, traineeName, profession); }
		catch (IllegalArgumentException e)
		{
			sender.sendMessage(ChatColor.YELLOW + e.getMessage());
			return false;
		}
		
		//Notify trainer, trainee and any moderators.
		Player trainer = (Player) sender;
		String trainerName = trainer.getCustomName();
		if (trainerName == null)
			trainerName = trainer.getName();
		
		sender.sendMessage(ChatColor.YELLOW + "You have trained " + traineeName + " in the " + profession + " profession.");
		trainee.sendMessage(ChatColor.YELLOW + trainerName + " has trained you in the " + profession + " profession.");
		
		//If the trainee gained a tier, let them know about that too.
		if (tierUp != null)
			trainee.sendMessage(ChatColor.YELLOW + tierUp);
		
		String message = ChatColor.YELLOW + trainer.getName() + " just trained " + traineeName + " in the " + profession + " profession!";
		
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		
		for (Player player : onlinePlayers) 
			if (player.hasPermission("horizonprofessions.train.admin"))
				player.sendMessage(message);
		
		createLog(message, "trainlog.txt");
		return true;
	}

	/**
	 * giveCommandsGuide() displays a list of the available commands.
	 * @param sender - the sender to display the commands to.
	 */
	private boolean giveCommandsGuide(CommandSender sender) 
	{
		sender.sendMessage("------------<" + ChatColor.GOLD + " Horizon Profession Commands " + ChatColor.WHITE + ">------------");
		sender.sendMessage(ChatColor.GOLD + "Horizon Professions allows you to keep track of your trade skills!");
		if (sender.hasPermission("horizonprofessions.view.admin"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession view [optional:player]");
			sender.sendMessage("View the professions of a player.");
		}
		else if (sender.hasPermission("horizonprofessions.view"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession view");
			sender.sendMessage("View your professions.");
		}
		if (sender.hasPermission("horizonprofessions.forget.admin"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession forget [profession] [optional:player]");
			sender.sendMessage("Force a player to lose a tier in a profession.");
		}
		else if (sender.hasPermission("horizonprofessions.forget") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession forget [profession]");
			sender.sendMessage("Lose a tier in one profession.");
		}
		if (sender.hasPermission("horizonprofessions.givetier"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession givetier [profession] [player]");
			sender.sendMessage("Give a tier to a player in the specified profession.");
		}
		if (sender.hasPermission("horizonprofessions.claimtier") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession claim [profession]");
			sender.sendMessage("Claim a free tier in a profession.");
		}
		if (sender.hasPermission("horizonprofessions.reset.admin"))
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession reset [optional:player]");
			sender.sendMessage("Resets all of the player's progress to zero.");
		}
		else if (sender.hasPermission("horizonprofessions.reset") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession reset");
			sender.sendMessage("Resets all of your progress to zero.");
		}
		if (sender.hasPermission("horizonprofessions.train") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.YELLOW + "/profession train [profession] [player]");
			sender.sendMessage("Trains another player in a specified profession. They will gain two levels.");
		}
		
		return true;
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
		File saveTo = new File("plugins" + File.separator + "horizonprofessions" + File.separator + filename);
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
	
	private void confirmForgetTimeout(final CommandSender sender, final String name)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{
			public void run() 
			{
				if (confirmForget.containsKey(name))
				{
					confirmForget.remove(name);
					sender.sendMessage(ChatColor.YELLOW + "You timed out.");
				}
			}			
		} , 200);
	}
	
	private void confirmResetTimeout(final CommandSender sender, final String name)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		{
			public void run() 
			{
				if (confirmReset.containsKey(name))
				{
					confirmReset.remove(name);
					sender.sendMessage(ChatColor.YELLOW + "You timed out.");
				}
			}			
		} , 200);
	}
}
