Horizon Professions - Version 1.0 15/6/2015

DEPENDENCIES
------------

-Vault-
This plugin has a hard dependency on Vault and will not load without it. Vault is
middleware designed to centralise the many different permission and economy 
plugins out there, and allows Horizon Professions to function with any of them.

-RecipeManager-
This plugin has a soft dependency on RecipeManager and is designed to work 
alongside it, but will work pefectly fine without. With RecipeManager loaded you 
will be able to configure Horizon Professions to award experience for custom 
recipes thatare specified in RecipeManager. See CONFIGURATION and RECIPEMANAGER 
INTEGRATION for more information.

INSTALLATION
------------

To install this plugin on your server, simply copy "Horizon_Professions.jar" 
into your "/plugins" directory. Upon the next server restart the plugin will be 
loaded automatically and a default configuration file will be generated. It is 
recommended that the plugin be configured for your individual needs.

FEATURES
--------
- Each player begins at the lowest tier in each profession and must progress by 
	earning experience
- Fully configurable profession names and tiers
- Configurable tier cap
- Configurable daily cooldown for level-ups
- Award experience for custom recipes
- Award experience for slaying monsters
- Award experience for block breaking
- Prevent/allow block breaking based on profession and tier
- Heal other players
- Train other players in a profession you are skilled at
- Player commands for viewing stats, undoing progress and resetting progress
- Admin commands for viewing others' stats, undoing others' progress, resetting 
	others' progress and awarding tiers
- Awards permissions to players based on their profession and tier for easy 
	interaction with other plugins
- Support for UUIDS

CONFIGURATION
-------------
To configure this plugin, modify the contents of "config.yml" in your 
"/plugins/Horizon_Professions" directory. Keep in mind this will not be 
generated until the server is started for the first time.

All configuration files for Bukkit use YAML format: http://yaml.org/
-Do not use tabs for indentation, use two spaces instead.
-Capitalisation matters
-Indentation matters

The following options in the configuration file are explained:

professions:            < ---   List of profession names that you want on 
  - engineer                    the server
  - medic
  - labourer
  - pilot
  - hunter
tiers:                  < ---   List of tiers and their names that you want
  0:                    < ---   Ensure that they are numbered 0 to maximum tier
    name: unskilled     < ---   Name of the tier
    maxlevel: 1         < ---   The number of levels a player will gain in this
  1:                    < ---   tier before they progress to the next one.
    name: novice
    maxlevel: 20
  2:
    name: adept
    maxlevel: 40
  3:
    name: expert
fatigue_time: 86400000  < ---   Time of the level-up cooldown in ticks 
                                (20 ticks = 1 second)
max_exp: 100            < ---   The amount of experience a player will gain in 
                                any level before levelling up
claimable_tiers: 3      < ---   The amount of free tiers a new player can claim. 
                                Set to 0 to disable this.
tier_cap: 6             < ---   The total amount of tiers a player may have in 
                                all professions.
slaying:                < ---   Configuration for slaying monsters
  hunter:               < ---   The profession that you want these creatures to 
                                apply to.
    chicken: 1          < ---   The creature that you want this value to apply 
                                to (chicken) and the experience to be awarded (1)
    pig: 1
  pilot:                < ---   Multiple professions can be specified
    sheep: 1            < ---   Multiple creatures too
    cow: 1
healing:                < ---   Configuration for healing other players
  paper:                < ---   The item you want players to use to heal (this 
                                will consume the item)
    profession: medic   < ---   The profession you want to be required for this 
                                item
    exp: 5              < ---   The experience to be awarded
    tier:				
      novice: 2         < ---   The amount of HP to heal for each tier.
      adept: 4          < ---   (Note: unlisted tiers will NOT be able to heal!)
      expert: 6
recipes:                < ---   Configuration for RecipeManager integration
  engineer:             < ---   Profession you want these recipes to apply to.
    diode: 5            < ---   The name of the custom recipe (diode) and the 
                                amount of experience to award (5).
blocks:                 < ---   Configuration for block breaking
  labourer:             < ---   The profession you want to be required to break 
                                the following blocks
    novice:             < ---   The tier you want to be required to break the 
                                following blocks
      dirt: 1           < ---   The block you want to restrict (dirt) and the 
                                amount of experience to award (1).
    adept:
      stone: 2
      
RECIPEMANAGER INTEGRATION
-------------------------

In order to award experience for custom made recipes the following steps should 
be followed.
- Create a custom recipe in RecipeManager's configuration
- Name it something meaningful
- Specify the name in Horizon Professions configuration

Example: RecipeManager

CRAFT Diode
@displayresult first
redstone + stained_clay:10 + redstone
= diode:0:1

This is a recipe with the name Diode.
Note: follow RecipeManager's instructions for creating your recipes in that 
plugin.

Example: Horizon Professions

recipes:
  engineer:
    diode: 5	
    
This specifies that crafting the "Diode" recipe will award 5 experience to 
Engineers.

Example 2: RecipeManager

CRAFT Diode
@permission horizon_professions.engineer | <yellow>You're not sure how to make 
this. Perhaps a Novice Engineer could help you.
@displayresult first
redstone + stained_clay:10 + redstone
= diode:0:1
  @permission horizon_professions.engineer.novice
= diode:0:2
  @permission horizon_professions.engineer.adept
= diode:0:3
  @permission horizon_professions.engineer.expert
  
In this example, the recipe will only be craftable by Engineers. A Novice 
Engineer will recieve 1 diode, an Adept Engineer will recieve 2 diodes and an 
Expert Engineer will recieve 2 diodes.

This is made possible because Horizon Professions gives players permission nodes 
based on their tiers in each profession. The permission node for your 
configuration will always be horizon_professions.[profession name].[tier name]. 
All players with any tier above 0 will have the permission node 
horizon_professions.[profession name].

This format is recommended if you wish to restrict recipes or provide extra 
boons to certain professions.

DEVELOPER API
-------------

It is possible for other plugins to interact with Horizon Professions. 

- Add Horizon_Professions.jar to your build path as an external jar and import 
	"com.gmail.Rhisereld.Horizon_Professions".
- If your plugin depends on Horizon Professions to function, you should add 
	"depend: [Horizon_Professions]" to your plugin.yml.
	Otherwise you may want to add softdepend: [Horizon_Professions] to ensure 
	your plugin loads second.
- Create a reference to Main of Horizon_Professions.
- Use the public methods available.

Methods available are documented at: 
https://github.com/Rhisereld/HorizonProfessions

THE FOLLOWING IS AN EXAMPLE OF A PLUGIN THAT INTERACTS WITH HORIZON_PROFESSIONS.

package com.gmail.Rhisereld.Driver;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.Rhisereld.Horizon_Professions.Main;

public class Driver extends JavaPlugin implements CommandExecutor
{
	Main professions;
	
	@Override
	public void onEnable()
	{
        if (getServer().getPluginManager().isPluginEnabled("Horizon_Professions"))
        {
        	getLogger().info("RecipeManager hooked, recipe support enabled.");
        	professions = new Main();
        }
        else
        	getLogger().severe(String.format("Support disabled due to no 
        		Horizon_Professions dependency found!", 
        			getDescription().getName()));
	}
	
	@Override
	public void onDisable()
	{
		professions = null;
	}
	
	public boolean testAPI(CommandSender sender, Command cmd, String commandLabel, 
						String[] args)
	{
		Player player;
		
		if (commandLabel.equalsIgnoreCase("testprofession"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage("Only players can do this!");
				return true;
			}
			
			if (args.length != 1)
			{
				sender.sendMessage("Use one argument");
				return true;
			}
			
				player = (Player) sender;
				
				professions.setExp(player, args[0], 50);
				player.sendMessage("" + professions.getExp(player, args[0]));
		}
		
		return false;
	}
}