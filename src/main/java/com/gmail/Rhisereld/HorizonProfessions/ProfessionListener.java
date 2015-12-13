package com.gmail.Rhisereld.HorizonProfessions;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

/**
 * ProfessionListener contains all the methods that are called when certain events happen in game.
 */
public class ProfessionListener implements Listener
{
	Plugin plugin;
	ConfigAccessor data;
	ConfigAccessor config;
	boolean isHealingOther;				//Used to cancel healing self if the player is healing another.
	
	public ProfessionListener(Plugin plugin, ConfigAccessor data, ConfigAccessor config) 
	{
		this.plugin = plugin;
		this.data = data;
		this.config = config;
	}
	
	//Called when a monster or player dies.
	@EventHandler(priority = EventPriority.MONITOR)
	void onMonsterDeath(EntityDeathEvent event)
	{
		Entity entity = event.getEntity();
		EntityDamageByEntityEvent dEvent;
		Player player;
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
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		
		for (String p: prof.getProfessions())
		{
			list = config.getConfig().getConfigurationSection("slaying." + p).getKeys(false);
			if (list.isEmpty())
				continue;
			
			for (String monster: list)
				//If found, award experience for it.
				if (entity.getType().toString().equalsIgnoreCase(monster))
				{
		    		prof.addExperience(p, config.getConfig().getInt("slaying." + p + "." + monster));
					return;
				}
		}
	}
	
	//Called when a player right clicks something
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerInteract(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		isHealingOther = true;
		
		//Check that the player right-clicked on another player.
		if (!(event.getRightClicked() instanceof Player))
			return;

    	//Check if the item in hand fits any of the items specified in the configuration file.		
    	Set <String> items = config.getConfig().getConfigurationSection("healing.").getKeys(false);
    	String item = null;
    	for (String i: items)
    		if (player.getItemInHand().getType().toString().equalsIgnoreCase(i))
    			item = i;
    	
    	//If the item isn't found, it's not a healing item.
    	if (item == null)
    		return;
    	
		//Check if the amount to heal is in the config
    	String professionRequired = config.getConfig().getString("healing." + item + ".profession");
    	ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
    	
    	int amountToHeal = config.getConfig().getInt("healing." + item + ".tier." + prof.getTierName(professionRequired));
    	if (amountToHeal == 0)
    	{
    		player.sendMessage(ChatColor.RED + "You do not have the skill required to do this!");
    		return;
    	}
    	
    	//Check that the recipient has missing health.
		Player recipient = (Player) event.getRightClicked();
		if (recipient.getHealth() >= recipient.getMaxHealth())
		{
			player.sendMessage(ChatColor.YELLOW + recipient.getName() + " does not need bandaging!");
			return;
		}  
		
		player.sendMessage(ChatColor.YELLOW + "Bandaging...");
		
		//Schedule the task in one second.
		makeDelayedTask(player, recipient, amountToHeal, item, professionRequired, player.getLocation(), recipient.getLocation());
	}
	
	//Called when a player right-clicks
	@EventHandler(priority = EventPriority.MONITOR)
	void onRightClick(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		//If the player is healing another person, they're not healing themself.
		if (isHealingOther)
		{
			isHealingOther = false;
			return;
		}
		
		//Check if the item in hand fits any of the items specified in the configuration file.		
    	Set <String> items = config.getConfig().getConfigurationSection("healing.").getKeys(false);
    	String item = null;
    	for (String i: items)
    		if (player.getItemInHand().getType().toString().equalsIgnoreCase(i))
    			item = i;
    	
    	//If the item isn't found, it's not a healing item.
    	if (item == null)
    		return;
    	
		//Check if the amount to heal is in the config
    	String professionRequired = config.getConfig().getString("healing." + item + ".profession");
    	ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
    	
    	int amountToHeal = config.getConfig().getInt("healing." + item + ".tier." + prof.getTierName(professionRequired));
    	if (amountToHeal == 0)
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
    	makeDelayedTask(player, player, amountToHeal, item, professionRequired, player.getLocation(),  player.getLocation());
	}
	
	//Called when a block is broken
	@EventHandler(priority = EventPriority.HIGH)
	void onBreakBlock(BlockBreakEvent event)
	{
		Player player = event.getPlayer();
		
		//If the player is in creative mode don't mess with the event
		if (player.getGameMode().equals(GameMode.CREATIVE))
			return;
		
		//Check if the block is contained within the config
		Set<String> configBlocks;
		int exp = 0;
		String professionReq = null;
		String tierReq = null;
		
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		
		for(String p: prof.getProfessions())
			for (String t: prof.getTiers())
			{
				configBlocks = config.getConfig().getConfigurationSection("breakblocks." + p + "." + t).getKeys(false);
				
				for (String b: configBlocks)
					if (event.getBlock().getType().toString().equalsIgnoreCase(b))
					{
						exp = config.getConfig().getInt("breakblocks." + p + "." + t + "." + b);
						professionReq = p;
						tierReq = t;
						break;
					}
			}
		
		//If not found, nothing to do here.
		if (professionReq == null || tierReq == null)
			return;
		
		//If the player doesn't have at least the tier, cancel the event.
		long place_cooldown = config.getConfig().getLong("place_cooldown");
		
		if (!prof.hasTier(professionReq, tierReq))
		{
			player.sendMessage(ChatColor.RED + "You aren't skilled enough to break that!");
			event.setCancelled(true);
		}
		//Otherwise award some experience
		//But only do it if the block wasn't placed recently.		
		else if (event.getBlock().hasMetadata("timeplaced") 
				&& getMetadataLong(event.getBlock(), "timeupdated") - System.currentTimeMillis() > place_cooldown)
			prof.addExperience(professionReq, exp);
	}
	
	//Called when a block is placed.
	@EventHandler (priority = EventPriority.MONITOR)
	void onBlockPlace(BlockPlaceEvent event)
	{
		Player player = event.getPlayer();
		
		//Record the time placed so that placing cooldowns can be implemented.
		event.getBlock().setMetadata("timeplaced", new FixedMetadataValue(plugin, System.currentTimeMillis()));
		
		//Check if the block is contained within the config
		Set<String> configBlocks;
		int exp = 0;
		String professionReq = null;
		String tierReq = null;
		
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		
		for(String p: prof.getProfessions())
			for (String t: prof.getTiers())
			{
				configBlocks = config.getConfig().getConfigurationSection("placeblocks." + p + "." + t).getKeys(false);
				
				for (String b: configBlocks)
					if (event.getBlock().getType().toString().equalsIgnoreCase(b))
					{
						exp = config.getConfig().getInt("placeblocks." + p + "." + t + "." + b);
						professionReq = p;
						tierReq = t;
						break;
					}
			}
		
		//If not found, nothing to do here.
		if (professionReq == null || tierReq == null)
			return;
		
		//If the player doesn't have at least the tier, cancel the event.		
		if (!prof.hasTier(professionReq, tierReq))
		{
			player.sendMessage(ChatColor.RED + "You aren't skilled enough to place that!");
			event.setCancelled(true);
		}
		//Otherwise award some experience
		else
			prof.addExperience(professionReq, exp);
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
				
				//Check that the player is still in roughly the same location
				if (player.getLocation().getBlockX() == playerLoc.getBlockX()
						|| player.getLocation().getBlockY() == playerLoc.getBlockY()
						|| player.getLocation().getBlockZ() == playerLoc.getBlockZ())
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
	    		addHp = config.getConfig().getDouble("healing." + item + ".tier" + playerTier);
	    		recipient.setHealth(recipient.getHealth() + addHp);

	    		//Award experience.
	    		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
	    		
	    		if (prof.getPracticeFatigue(profession) <= 0)
	    			prof.addExperience(profession, config.getConfig().getInt("healing." + item + ".exp"));
	    			
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
	

	/**
	 * getMetadataLong() retrieves metadata from an object using a key.
	 * @param object - the object the metadata is attached to.
	 * @param key - the key the metadata is under.
	 * @return
	 */
	private long getMetadataLong(Metadatable object, String key) 
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
