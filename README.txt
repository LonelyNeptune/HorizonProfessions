Horizon Professions is a highly configurable professions plugin designed for heavy RP servers. It was originally designed and written for my science-fiction roleplay server, Project Horizon, but I have been moving towards releasing it for other servers as well. The default configuration suits a science-fiction theme, but can be altered to work with medieval and modern servers just fine!

FEATURES

-Each player begins at the lowest tier in each profession and must progress by earning experience
-Intuitive progress display
-Fully configurable profession names and tiers
-Configurable tier cap (the maximum number of tiers a player can have in all professions)
-Configurable daily cooldown for level-ups and training
-Healing (item, profession requirement, tier requirement are all configurable)
-Award experience for healing
-Award experience for custom recipes
-Award experience for slaying monsters (in melee and with arrows)
-Award experience for block breaking
-Award experience for block placing
-Exploit protection against repeatedly breaking and placing blocks
-Prevent/allow block breaking/placing based on profession and tier
-Train other players in a profession you are the top tier in (can be disabled)
-Configurable damage modifiers. Requiring to use a specific weapon may be specified.
-Player commands for viewing stats, undoing progress and resetting progress
-Admin commands for viewing others' stats, undoing others' progress, resetting others' progress and awarding tiers
-Admin notified when a player trains another player, or when another admin manipulates a player's professions.
-All notifications also logged in /plugins/HorizonProfessions/log.txt
-Awards permissions to players based on their profession and tier for easy interaction with other plugins
-Support for UUIDS 

DEPENDENCIES

Vault
This plugin has a hard dependency on Vault and will not load without it. Vault is middleware designed to centralise the many different permission and economy plugins out there, and allows Horizon Professions to function with any of them. You can download the latest version of Vault here: http://dev.bukkit.org/bukkit-plugins/vault/

RecipeManager
This plugin has a soft dependency on RecipeManager and is designed to work alongside it, but will work perfectly fine without. With RecipeManager loaded you will be able to configure Horizon Professions to award experience for custom recipes that are specified in RecipeManager. See the configuration for more information. You can download the latest version of Recipemanager here: http://dev.bukkit.org/bukkit-plugins/recipemanager/ 


INSTALLATION

To install this plugin on your server, simply copy "HorizonProfessions.jar" into your "/plugins" directory. Upon the next server restart the plugin will be loaded automatically and a default configuration file will be generated. It is recommended that the plugin be configured for your individual needs. Once you are finished editing the configuration, you can apply your changes by typing /prof reload. 


CONFIGURATION

To configure this plugin, modify the contents of "config.yml" in your "/plugins/HorizonProfessions" directory. Keep in mind this will not be generated until the server is started with the plugin for the first time.

All configuration files for Bukkit use YAML format: http://yaml.org/

Do not use tabs for indentation, use two spaces instead.
Capitalisation matters
Indentation matters
The following options in the configuration file are explained. This guide is also viewable in the configuration file that is generated.

PROFESSIONS
List of profession names that you want on the server.

professions:
  - engineer
  - medic
  - labourer
  - pilot
  - hunter
  
TIERS
List of tiers and their names that you want
Ensure that they are numbered from 0 to the maximum tier
maxLevel - the number of levels a player gains in that tier before
progressing to the next one.

tiers: 
  0:
    name: unskilled
    maxLevel: 1
  1:
    name: novice
    maxLevel: 20
  2:
    name: adept
    maxLevel: 40
  3:
    name: expert
    
OPTIONS
fatigue_time -  Time of the level-up cooldown in ticks 
(20 ticks = 1 second). Training and grinding are on seperate timers.
permission_prefix - the prefix to all permissions awarded for
professions. Format is [permission_prefix].[profession].[tier].
This is used to interface with other plugins that rely on 
permissions.
enable_training - set to false if you don't want players to be able
to train.
max_exp - the amount of experience earned before level-up.
claimable_tiers - the number of free tiers a new player can claim.
Set to 0 to disable this.
tier_cap - the total number of tiers a player may have in all 
professions. After they reach this point they gain no experience.
place_cooldown - exploit protection against repeatedly breaking and
placing blocks. This is the timer in ticks (20 ticks = 1 second).
Blocks broken before this timer is up yields no experience.

SLAYING
The amount of experience for killing each type of mob is configurable
Multiple professions may be defined. Mobs can appear under more than 
one profession.
Example configuration:
slaying:
  hunter:
    chicken: 1
    pig: 1
  pilot:
    sheep: 1
    cow: 1

This configuration awards 1 experience to hunters for killing
chickens and pigs, and 1 experience to pilots for killing sheep and
cows.
DAMAGE MODIFIER
The amount of damage a player deals is configurable.
Multiple professions and tiers may be defined. It can be required
that the player is holding a specific weapon.
Example configuration:

damageModifier:
  hunter:
    unskilled: 25
    novice: 50
    adept: 100
    expert: 150
    weaponReq:
    - DIAMOND_SWORD
    - IRON_SWORD
    - STONE_SWORD
    - WOOD_SWORD

This configuration will cause unskilled hunters to deal 25% the
damage they usually would, novices 50%, adepts 100% and experts 150%.
However, this modifier only applies if the player is holding any of
the items listed. For a list of items, view here:
https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
HEALING
Certain professions can be configured with the ability to heal
themselves and other players.
The item, profession, and tier required are configurable and there 
may be multiple.
For each profession and tier the amount to heal is configurable.
Any unlisted professions and tiers will be unable to heal.
Example configuration:

healing:
  paper:
    medic:
      novice: 2
      adept: 4
      expert: 6

In this configuration, medics can heal using paper. A novice heals
2HP, an adept heals 4HP, and an expert heals 6HP. 2HP = 1 heart.
Unskilled is not listed, so unskilled medics cannot heal.
RECIPES
The plugin can be configured to award experience when a player
crafts an item.
This feature is ONLY available if RecipeManager is installed.
This feature only applies to recipes specified in RecipeManager
configuration. The name of the custom recipe must match the name
given here.

Example RecipeManager configuration (1):
CRAFT Diode
@displayresult first
redstone + stained_clay:10 + redstone
= diode:0:1

This is a recipe with the name Diode.
Note: follow RecipeManager's instructions for creating your 
recipes in that plugin.

Example HorizonProfessions configuration (1):

recipes:
  engineer:
    diode: 5

This specifies that crafting the "Diode" recipe will award 5 
experience to engineers. Both plugins must be configured for this
to work.

Example RecipeManager configuration (2)

CRAFT Diode
@permission horizonprofessions.engineer.novice, horizonprofessions.engineer.adept, horizonprofessions.engineer.expert | <yellow>You're not sure how to make this. Perhaps a Novice Engineer could help you.
@displayresult first
redstone + stained_clay:10 + redstone
= diode:0:1
@permission horizonprofessions.engineer.novice
= diode:0:2
@permission horizonprofessions.engineer.adept
= diode:0:3
@permission horizonprofessions.engineer.expert

In this example, the recipe will only be craftable by engineers. 
A novice engineer will receive 1 diode, an adept engineer will 
receive 2 diodes and an expert engineer will receive 3 diodes.

This is made possible because Horizon Professions gives players 
permission nodes based on their tiers in each profession. The 
default permission node for your configuration is 
horizonprofessions.[profession name].[tier name]. However, the 
prefix can be changed to anything you want instead of 
"horizonprofessions".

This format is recommended if you wish to restrict recipes or 
provide extra boons to certain professions.
BREAK BLOCKS
Each block can be configured to only be broken by players with a
certain profession and tier. If the player has a tier HIGHER than
the requirement, they are permitted to break that block.
Multiple professions and tiers may be defined.
Each block should only appear once.
The plugin also awards experience for breaking the block.
Example configuration:

breakBlocks:
  labourer:
    novice:
      stone: 1
    unskilled:
      dirt: 1

In this configuration, only novice, adept, and expert labourers can
break stone (unskilled labourers can't). When a player breaks stone
they get 1 experience. Any labourer (unskilled and above) can break
dirt and they get 1 experience for doing so.
PLACE BLOCKS
Identical in concept and configuration to break blocks.
Example configuration:
placeBlocks:
  labourer:
    novice:
      potato: 1

In this configuration, only novice, adept and expert labourers can
plant potatoes. They get 1 experience for doing so.

SUPPORT

Having a problem with the plugin? Want to suggest a new feature? Submit a ticket with a description of your problem/idea and I'll get to it!