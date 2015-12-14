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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("deprecation")
public class ProfessionCommandExecutor implements CommandExecutor
{	
	Plugin plugin;
	FileConfiguration data;
	FileConfiguration config;
	
	HashMap<String, String> confirmForget = new HashMap<String, String>();	//Used to confirm commands
	HashMap<String, String> confirmReset = new HashMap<String, String>();
	
    public ProfessionCommandExecutor(Plugin plugin, FileConfiguration data, FileConfiguration config) 
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
					return giveCommandsGuideAdmin(sender);
				else if (sender.hasPermission("horizon_professions.help"))
					return giveCommandsGuide((Player) sender);
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
					return forgetTier(sender, args[1], args[2]);
				
				//Player is attempting to forget a tier.
				if (args.length == 2)
					return forgetTier(sender, args[1], sender.getName());
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
					return giveTier(sender, args[1], args[2]);
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
				
				return trainPlayer(sender, args[1], args[2]);
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
		if (!sender.hasPermission("horizon_professions.view"))
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to view your professions.");
			return false;
		}
		
		Player player = (Player) sender;
		ProfessionHandler profHandler = new ProfessionHandler(data, config);
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
		if (!sender.hasPermission("horizon_professions.view.admin") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to view another player's professions.");
			return false;
		}
		
		Player player = Bukkit.getPlayer(name);
		ProfessionHandler profHandler = new ProfessionHandler(data, config);
		if (player == null)
		{
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
			profHandler.displayStats(offlinePlayer.getUniqueId(), offlinePlayer.getName(), sender);
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
			if (!sender.hasPermission("horizon_professions.forget"))
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
			if (sender instanceof Player && !sender.hasPermission("horizon_professions.forget.admin"))
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
		ProfessionHandler profHandler = new ProfessionHandler(data, config);
		Player player = Bukkit.getPlayer(name);
		int newTier;
		
		//Stop waiting for confirmation
		confirmForget.remove(sender.getName());
		
		//Perform the action
		if (player == null)
			try {newTier = profHandler.forgetTier(Bukkit.getOfflinePlayer(name).getUniqueId(), profession);}
			catch (IllegalArgumentException e) 
			{
				sender.sendMessage(ChatColor.RED + e.getMessage());
				return false;
			}
		else
			try {newTier = profHandler.forgetTier(player.getUniqueId(), profession);} 
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
			if (onlinePlayer.hasPermission("horizon_profession.forget.admin"))
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
		if (!sender.hasPermission("horizon_professions.givetier") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to give tiers.");
			return false;
		}
		
		ProfessionHandler profHandler = new ProfessionHandler(data, config);
		Player player = Bukkit.getPlayer(name);
		int newTier;
		
		if (player == null)
			try {newTier = profHandler.giveTier(Bukkit.getOfflinePlayer(name).getUniqueId(), profession);}
			catch (IllegalArgumentException e)
			{
				sender.sendMessage(ChatColor.RED + e.getMessage());
				return false;
			}
		else
			try {newTier = profHandler.giveTier(player.getUniqueId(), profession);}
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
			if (onlinePlayer.hasPermission("horizon_profession.givetier"))
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
		if (!sender.hasPermission("horizon_professions.claimtier") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.RED + "You don't have permission to claim a tier.");
			return false;
		}
		
		Player player = (Player) sender;
		ProfessionHandler profHandler = new ProfessionHandler(data, config);
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
			if (!sender.hasPermission("horizon_professions.reset"))
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
			if (sender instanceof Player && !sender.hasPermission("horizon_professions.reset.admin"))
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
	private boolean confirmResetStats(CommandSender sender, String playerString)
	{
		Player player = Bukkit.getPlayer(playerString);
		ProfessionStats prof;
		
		//Stop waiting for confirmation
		confirmReset.remove(sender.getName());
		
		if (player == null)
			prof = new ProfessionStats(data, config, Bukkit.getOfflinePlayer(playerString).getUniqueId());
		else
			prof = new ProfessionStats(data, config, Bukkit.getOfflinePlayer(playerString).getUniqueId());
		
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
			return true;

		String message = ChatColor.GOLD + sender.getName() + " has forced " + playerString + " to reset.";
		
		//Notify all online moderators.
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		
		for (Player onlinePlayer: onlinePlayers)
			if (onlinePlayer.hasPermission("horizon_profession.reset.admin"))
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
	private boolean trainPlayer(CommandSender sender, String profession, String traineeString) 
	{		
		//Check that the sender is a player
		if (!(sender instanceof Player))
		{
			sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
			return false;
		}
		
		Player trainee = Bukkit.getPlayer(traineeString);
		Player trainer = (Player) sender;
		
		//Check that the trainee is online
		if (trainee == null)
		{
			sender.sendMessage(ChatColor.YELLOW + "You cannot train " + traineeString + " because they are not online!");
			return false;
		}
		
		ProfessionStats profTrainer = new ProfessionStats(data, config, trainer.getUniqueId());
		ProfessionStats profTrainee = new ProfessionStats(data, config, trainee.getUniqueId());
		
		//Check that the trainer is the top tier
		List<String> tiers = profTrainer.getTiers();
		
		if (profTrainer.getTier(profession) < tiers.size() - 1)
		{
			sender.sendMessage(ChatColor.YELLOW + "You cannot train yet because you are not " 
								+ getDeterminer(tiers.get(tiers.size()-1)) + " " + tiers.get(tiers.size()-1) + " " + profession + "!");
			return false;
		}
		
		//Trying to train yourself is funny, but not allowed.
		if (trainer.getUniqueId() == trainee.getUniqueId())
		{
			sender.sendMessage(ChatColor.YELLOW + "You cannot train yourself, silly!");
			return false;
		}
		
		//Check that the trainee is not already at the top tier
		if (profTrainee.getTier(profession) >= 3)
		{
			sender.sendMessage(ChatColor.YELLOW + "You cannot train " + traineeString + " because they are already an Expert!");
			return false;
		}
		
		//Check that the trainee is not suffering from instruction fatigue.
		if (profTrainee.getInstructionFatigue(profession) > 0)
		{
			sender.sendMessage(ChatColor.YELLOW + traineeString + " has already benefitted from instruction today.");
			return false;
		}
		
		//Check that the trainer and trainee are reasonably close together and in the same world.
		double distance = trainer.getLocation().distance(trainee.getLocation());

		if (!trainer.getWorld().equals(trainee.getWorld()) || Double.isNaN(distance) || distance > 20)
		{
			sender.sendMessage("You are too far away to train " + traineeString + "!");
			return false;
		}
		
		//Give levels
		profTrainee.addLevel(profession, 2);
		
		//Set fatigue
		profTrainee.setInstructionFatigue(profession, config.getInt("fatigue_time"));
		
		//Notify trainer, trainee and any moderators.
		sender.sendMessage("You have trained " + traineeString + " in the " + profession + " profession.");
		trainee.sendMessage(trainer.getCustomName() + " has trained you in the " + profession + " profession.");
		
		String message = ChatColor.YELLOW + trainer.getName() + " just trained " + traineeString + " in the " + profession + " profession!";
		
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		
		for (Player player : onlinePlayers) 
			if (player.hasPermission("horizon_profession.train.admin"))
				player.sendMessage(message);
		
		createLog(message, "trainlog.txt");
		return true;
	}


	/**
	 * giveCommandsGuide() displays a list of the available commands.
	 * @param player - the player to display the commands to.
	 */
	private boolean giveCommandsGuide(Player player) 
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
		
		return true;
	}
	
	/**
	 * giveCommandsGuideAdmin() displays a list of the available commands to administrators.
	 * @param sender - the sender to display the commands to.
	 */
	private boolean giveCommandsGuideAdmin(CommandSender sender) 
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
