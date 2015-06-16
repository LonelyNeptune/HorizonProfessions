package com.gmail.Rhisereld.Horizon_Professions;

import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

/*
 * ProfessionListener contains all the methods that are called when certain events happen in game.
 */
public class ProfessionListener implements Listener
{
	static Plugin plugin = Main.plugin;	//A reference to this plugin.
	Main main;							//A reference to main.
	
	//Constructor passing a reference to main.
	public ProfessionListener(Main main) 
	{
		this.main = main;
	}

	//Called when a player logs onto the server.
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		//Attempt to load player stats.
		main.loadPlayerStats(player);
	}
	
	//Called when a player logs off the server.
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		//Save player stats
		main.savePlayerStats(player);
		main.removePlayerStats(player);
	}
	
	//Called when a monster or player dies.
	@EventHandler(priority = EventPriority.MONITOR)
	void onMonsterDeath(EntityDeathEvent event)
	{
		Entity entity = event.getEntity();
		EntityDamageByEntityEvent dEvent;
		Player player;
		String profession = null;
		int exp = 0;
		Set<String> list;
		
		//Check that it was killed by another entity
		if(!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return;
		
		dEvent = (EntityDamageByEntityEvent) entity.getLastDamageCause();
		
		//Check that it was killed by a player
		if(!(dEvent.getDamager() instanceof Player))
			return;
		
		player = (Player) dEvent.getDamager();
		
		//Check if the monster is contained within the config
		for (String professionConfig: main.PROFESSIONS)
		{
			if (main.config.getConfig().getConfigurationSection("slaying." + professionConfig) != null)
			{
				list = main.config.getConfig().getConfigurationSection("slaying." + professionConfig).getKeys(false);
				for (String monster: list)
				{
					if (entity.getType().toString().equalsIgnoreCase(monster))
					{
		    			profession = professionConfig;
		    			exp = main.config.getConfig().getInt("slaying." + profession + "." + monster);
						break;
					}	
				}
			}
		}
		
    	main.gainExperience(player, profession, exp);
	}
	
	//Called when a player right clicks something
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerInteract(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		Player recipient;
		String profession;
		int playerTier = -1;
		
		//Check that the player right-clicked on another player.
		if (!(event.getRightClicked() instanceof Player))
			return;
		
		recipient = (Player) event.getRightClicked();
		
		//See if options are specified in the configuration file.
    	Set <String> items = main.config.getConfig().getConfigurationSection("healing.").getKeys(false);

    	for (final String item: items)
    	{
        	
    		//Check if the item in hand fits any of the items specified in the configuration file.
    		if (player.getItemInHand().getType().toString().equalsIgnoreCase(item))
    		{
    			profession = main.config.getConfig().getString("healing." + item + ".profession");
    			
    			//Check if the amount to heal is in the config
    	    	Set <String> tiers = main.config.getConfig().getConfigurationSection("healing." + item + ".tier").getKeys(false);
    	    	
    	    	for (String tier: tiers)
    	    		if (main.TIERS[main.getTier(player, profession)].equalsIgnoreCase(tier))
    	    			playerTier = main.config.getConfig().getInt("healing." + item + ".tier." + tier);

    	    	//If it isn't in the config just stop.
    	    	if (playerTier == -1)
    	    	{
    	    		player.sendMessage(ChatColor.RED + "You do not have the skill required to do this!");
    	    		return;
    	    	}
    	    	
    			//Check that the recipient has missing health.
    			if (recipient.getHealth() >= recipient.getMaxHealth())
    			{
    				player.sendMessage(ChatColor.YELLOW + recipient.getName() + " does not need bandaging!");
    				return;
    			}  
    			
    			player.sendMessage(ChatColor.YELLOW + "Bandaging...");
    			
    			//Schedule the task in one second.
    			makeDelayedTask(player, recipient, playerTier, item, profession, player.getLocation(),  recipient.getLocation());
    		}
    	}
	}
	
	//Called when a player right-clicks
	@EventHandler(priority = EventPriority.MONITOR)
	void onRightClick(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		String profession;
		int playerTier = -1;
		
		//See if options are specified in the configuration file.
    	Set <String> items = main.config.getConfig().getConfigurationSection("healing.").getKeys(false);

    	for (final String item: items)
    	{
        	
    		//Check if the item in hand fits any of the items specified in the configuration file.
    		if (player.getItemInHand().getType().toString().equalsIgnoreCase(item))
    		{
    			profession = main.config.getConfig().getString("healing." + item + ".profession");
    			
    			//Check if the amount to heal is in the config
    	    	Set <String> tiers = main.config.getConfig().getConfigurationSection("healing." + item + ".tier").getKeys(false);
    	    	
    	    	for (String tier: tiers)
    	    		if (main.TIERS[main.getTier(player, profession)].equalsIgnoreCase(tier))
    	    		{
    	    			playerTier = main.config.getConfig().getInt("healing." + item + ".tier." + tier);
    	    		}

    	    	//If it isn't in the config just stop.
    	    	if (playerTier == -1)
    	    	{
    	    		player.sendMessage(ChatColor.RED + "You do not have the skill required to do this!");
    	    		return;
    	    	}
    	    	
    			//Check that the player has missing health.
    			if (player.getHealth() >= player.getMaxHealth())
    			{
    				player.sendMessage(ChatColor.YELLOW + "You do not need bandaging!");
    				return;
    			}  
    			
    			player.sendMessage(ChatColor.YELLOW + "Bandaging...");
    			
    			//Schedule the task in one second.
    			makeDelayedTask(player, player, playerTier, item, profession, player.getLocation(),  player.getLocation());
    		}
    	}
	}
	
	//Called when a block is broken
	@EventHandler(priority = EventPriority.HIGH)
	void onBreakBlock(BlockBreakEvent event)
	{
		Player player = event.getPlayer();
		Set<String> list;
		int exp = 0;
		String professionReq = null;
		String tierReq = null;
		int tierInt = -1;
		
		//If the player is in creative mode don't mess with the event
		if (player.getGameMode().equals(GameMode.CREATIVE))
			return;
		
		//Check if the block is contained within the config
		for (String profession: main.PROFESSIONS)
		{
			for (String tier: main.TIERS)
				if (main.config.getConfig().getConfigurationSection("blocks." + profession + "." + tier) != null)
				{
					list = main.config.getConfig().getConfigurationSection("blocks." + profession + "." + tier).getKeys(false);
					for (String block: list)
					{
						if (event.getBlock().getType().toString().equalsIgnoreCase(block))
						{
							exp = main.config.getConfig().getInt("blocks." + profession + "." + tier + "." + block);
							professionReq = profession;
							tierReq = tier;
							break;
						}	
					}
				}
		}
		
		//If not found, don't mess with the event.
		if (professionReq == null || tierReq == null)
			return;
		
		//Check that the tier requirement is an actual tier.
		for (int i = 0; i < main.TIERS.length - 1; i++)
		{
			if (tierReq.equalsIgnoreCase(main.TIERS[i]))
				tierInt = i;
		}
		
		//Be nice and let the administrator know that they messed up the config.
		if (tierInt == -1)
		{
			main.getLogger().severe("Tier not found - " + tierReq);
			return;
		}

		//If the player doesn't have at least the tier, cancel the event.
		if (main.getTier(player, professionReq) < tierInt)
		{
			player.sendMessage(ChatColor.RED + "You aren't skilled enough to break that!");
			event.setCancelled(true);
		}
		//Otherwise award some experience
		//But only do it if the block wasn't placed recently.
		else if (event.getBlock().hasMetadata("timeplaced") 
				&& (getMetadataLong(event.getBlock(), "timeupdated", plugin) - System.currentTimeMillis()) > main.PLACE_COOLDOWN)
			main.gainExperience(player, professionReq, exp);
	}
	
	//Called when a block is placed.
	@EventHandler (priority = EventPriority.MONITOR)
	void onBlockPlace(BlockPlaceEvent event)
	{
		event.getBlock().setMetadata("timeplaced", new FixedMetadataValue(plugin, System.currentTimeMillis()));
	}
	
	void makeDelayedTask(final Player player, final Player recipient, final int playerTier, final String item, 
			final String profession, final Location playerLoc, final Location recipientLoc)
	{
		//After a second, perform the action.
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() 
		{
			public void run() 
			{
				double addHp;  
				int exp;
				
				//Check that the player is still in roughly the same location
				if (Math.abs(player.getLocation().getX() - playerLoc.getX()) > 1
						|| Math.abs(player.getLocation().getY() - playerLoc.getY()) > 1
						|| Math.abs(player.getLocation().getZ() - playerLoc.getZ()) > 1)
				{
					player.sendMessage(ChatColor.YELLOW + "You cannot move while bandaging!");
					return;
				}
				
				//Check that the recipient is still in roughly the same location
				//Skip if self-heal
				if (!player.equals(recipient))
					if (Math.abs(recipient.getLocation().getX() - recipientLoc.getX()) > 1
						|| Math.abs(recipient.getLocation().getY() - recipientLoc.getY()) > 1
						|| Math.abs(recipient.getLocation().getZ() - recipientLoc.getZ()) > 1)
					{
						player.sendMessage(ChatColor.YELLOW + "You cannot bandage your patient while they are moving!");
						return;
					}
				
				//Remove item from player's inventory.
	    		player.getInventory().removeItem(new ItemStack(Material.getMaterial(item.toUpperCase()), 1));
	    		player.updateInventory();
	    			
	    		//Heal the other player.
	    		addHp = main.config.getConfig().getDouble("healing." + item + ".tier" + playerTier);
	    		recipient.setHealth(recipient.getHealth() + addHp);

	    		//Award experience.
	    		exp = main.config.getConfig().getInt("healing." + item + ".exp");
	    		
	    		if (main.getPracticeFatigue(player, profession) <= 0)
	    			main.gainExperience(player,  profession, exp);
	    			
	    		//Notify both parties.
	    		if (!player.equals(recipient))
	    		{	    		
	    			player.sendMessage(ChatColor.YELLOW + "You bandaged " + recipient.getName() + "'s wounds.");
	    			recipient.sendMessage(ChatColor.YELLOW + player.getName() + " bandaged your wounds.");
	    		}
	    		else
	    			player.sendMessage(ChatColor.YELLOW + "You bandaged your wounds.");
			  }
			}, 20);
	}
	
	/*
	 * getMetadataLong() retrieves metadata from an object using a key.
	 * @param object - the object the metadata is attached to.
	 * @param key - the key the metadata is under.
	 * @param plugin - a reference to this plugin
	 * @return - the metadata attached to the player that is associated with the key given.
	 */
	private long getMetadataLong(Metadatable object, String key, Plugin plugin) 
	{
		List<MetadataValue> values = object.getMetadata(key);  
		for (MetadataValue value : values) 
		{
			// Plugins are singleton objects, so using == is safe here
			if (value.getOwningPlugin() == plugin) 
			{
				return value.asLong();
			}
		}
		return 0;
	}
}
