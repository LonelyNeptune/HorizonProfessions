package com.gmail.Rhisereld.HorizonProfessions;

import java.util.List;
import java.util.Set;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

/**
 * ProfessionListener contains all the methods that are called when certain events happen in game.
 */
public class ProfessionListener implements Listener
{
	Plugin plugin;
	Permission perms;
	FileConfiguration data;
	FileConfiguration config;
	boolean isHealingOther;				//Used to cancel healing self if the player is healing another.
	
	public ProfessionListener(Plugin plugin, Permission perms, FileConfiguration data, FileConfiguration config) 
	{
		this.perms = perms;
		this.plugin = plugin;
		this.data = data;
		this.config = config;
	}
	
	//Called when a player joins the server
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerJoin(PlayerJoinEvent event)
	{
		//Add the player to the correct permissions groups for their professions
		ProfessionStats prof = new ProfessionStats(perms, data, config, event.getPlayer().getUniqueId());
		for (String p: prof.getProfessions())
			perms.playerAddGroup((String) null, event.getPlayer(), p + "-" + prof.getTierName(prof.getTier(p))); 
	}
	
	//Called when a player leaves the server
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerLeave(PlayerQuitEvent event)
	{
		//Remove the player from all permission groups for professions
		ProfessionStats prof = new ProfessionStats(perms, data, config, event.getPlayer().getUniqueId());
		for (String p: prof.getProfessions())
			for (String t: prof.getTiers())
			perms.playerRemoveGroup((String) null, event.getPlayer(), p + "-" + t); 
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
		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
		
		for (String p: prof.getProfessions())
		{
			//If there's no configuration for that profession, skip it.
			try {list = config.getConfigurationSection("slaying." + p).getKeys(false);}
			catch (NullPointerException e)
			{ continue; }
			
			for (String monster: list)
				//If found, award experience for it.
				if (entity.getType().toString().equalsIgnoreCase(monster))
				{
					if (!prof.isPracticeFatigued(p))
						prof.addExperience(p, config.getInt("slaying." + p + "." + monster));
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
    	Set <String> items = config.getConfigurationSection("healing.").getKeys(false);
    	String item = null;
    	for (String i: items)
    		if (player.getItemInHand().getType().toString().equalsIgnoreCase(i))
    			item = i;
    	
    	//If the item isn't found, it's not a healing item.
    	if (item == null)
    		return;
    	
		//Check if the amount to heal is in the config
    	String professionRequired = config.getString("healing." + item + ".profession");
    	ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
    	ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
    	
    	double amountToHeal = config.getInt("healing." + item + ".tier." + profHandler.getTierName(prof.getTier(professionRequired)));
    	if (amountToHeal == 0)
    	{
    		player.sendMessage(ChatColor.RED + "You do not have the skill required to do this!");
    		return;
    	}
    	
    	//Check that the recipient has missing health.
		Player recipient = (Player) event.getRightClicked();
		if (recipient.getHealth() >= 20)
		{
			player.sendMessage(ChatColor.YELLOW + recipient.getName() + " does not need bandaging!");
			return;
		}  
		
    	//Check that it won't take you over the maximum amount of health.
    	if (recipient.getHealth() + amountToHeal > 20)
    		amountToHeal = 20 - recipient.getHealth();
		
		player.sendMessage(ChatColor.YELLOW + "Bandaging...");
		String name = player.getCustomName();
		if (name == null)
			name = player.getName();
		recipient.sendMessage(ChatColor.YELLOW + name + " is bandaging you...");
		
		//Schedule the task in one second.
		makeDelayedTask(player, recipient, amountToHeal, item, professionRequired, player.getLocation(), recipient.getLocation());
	}
	
	//Called when a player right-clicks
	@EventHandler(priority = EventPriority.MONITOR)
	void onRightClick(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		//Check that it's a right click
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		//If the player is healing another person, they're not healing themself.
		if (isHealingOther)
		{
			isHealingOther = false;
			return;
		}
		
		//Check if the item in hand fits any of the items specified in the configuration file.		
    	Set <String> items = config.getConfigurationSection("healing.").getKeys(false);
    	String item = null;
    	for (String i: items)
    		if (player.getItemInHand().getType().toString().equalsIgnoreCase(i))
    			item = i;
    	
    	//If the item isn't found, it's not a healing item.
    	if (item == null)
    		return;
    	
		//Check if the amount to heal is in the config
    	String professionRequired = config.getString("healing." + item + ".profession");
    	ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
    	ProfessionHandler profHandler = new ProfessionHandler(perms, data, config);
    	
    	double amountToHeal = config.getInt("healing." + item + ".tier." + profHandler.getTierName(prof.getTier(professionRequired)));
    	if (amountToHeal == 0)
    	{
    		player.sendMessage(ChatColor.RED + "You do not have the skill required to do this!");
    		return;
    	}
    	    	
    	//Check that the player has missing health.
    	if (player.getHealth() >= 20)
    	{
    		player.sendMessage(ChatColor.YELLOW + "You do not need bandaging!");
    		return;
    	}  
    	
    	//Check that it won't take you over the maximum amount of health.
    	if (player.getHealth() + amountToHeal > 20)
    		amountToHeal = 20 - player.getHealth();
    			
    	player.sendMessage(ChatColor.YELLOW + "Bandaging...");
    			
    	//Schedule the task in one second.
    	makeDelayedTask(player, player, amountToHeal, item, professionRequired, player.getLocation(),  player.getLocation());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	void onDamage(EntityDamageByEntityEvent event)
	{
		Set<String> professions;
		
		//If it isn't a player, don't mess with this event.
		if (!(event.getDamager() instanceof Player))
			return;

		//Get the damage and multiply it by the relevant modifiers.
		try {professions = config.getConfigurationSection("damageModifier").getKeys(false);}
		catch (NullPointerException e)
		{ return; }
		
		Player player = (Player) event.getDamager();
		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
		double damage = event.getDamage();

		for (String p: professions)
			damage = damage * config.getInt("damageModifier." + p + "." + prof.getTierName(prof.getTier(p)), 100) / 100;
		
		event.setDamage(damage);
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
		
		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
		
		for(String p: prof.getProfessions())
			for (String t: prof.getTiers())
			{
				if (config.getConfigurationSection("breakBlocks." + p + "." + t) == null)
					continue;
				
				configBlocks = config.getConfigurationSection("breakBlocks." + p + "." + t).getKeys(false);
				
				for (String b: configBlocks)
					if (event.getBlock().getType().toString().equalsIgnoreCase(b))
					{
						exp = config.getInt("breakBlocks." + p + "." + t + "." + b);
						professionReq = p;
						tierReq = t;
						break;
					}
			}
		
		//If not found, nothing to do here.
		if (professionReq == null || tierReq == null)
		{
			return;
		}
		
		//If the player doesn't have at least the tier, cancel the event.
		long place_cooldown = config.getLong("place_cooldown");
		
		if (!prof.hasTier(professionReq, tierReq))
		{
			player.sendMessage(ChatColor.RED + "You aren't skilled enough to break that!");
			event.setCancelled(true);
		}
		//Otherwise award some experience
		//But only do it if the block wasn't placed recently and the player is not currently suffering from fatigue.
		else if ((!event.getBlock().hasMetadata("timeplaced") 
				|| System.currentTimeMillis() - getMetadataLong(event.getBlock(), "timeplaced") > place_cooldown)
				&& !prof.isPracticeFatigued(professionReq))
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
		
		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
		
		for(String p: prof.getProfessions())
			for (String t: prof.getTiers())
			{
				configBlocks = config.getConfigurationSection("placeBlocks." + p + "." + t).getKeys(false);
				
				for (String b: configBlocks)
					if (event.getBlock().getType().toString().equalsIgnoreCase(b))
					{
						exp = config.getInt("placeBlocks." + p + "." + t + "." + b);
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
		else if (!prof.isPracticeFatigued(professionReq))
			prof.addExperience(professionReq, exp);
	}
	
	void makeDelayedTask(final Player player, final Player recipient, final double amountToHeal, final String item, 
			final String profession, final Location playerLoc, final Location recipientLoc)
	{
		//After a second, perform the action.
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() 
		{
			public void run() 
			{				
				//Check that the player is still in roughly the same location
				if (Math.abs(player.getLocation().getX() - playerLoc.getX()) > 1
						|| Math.abs(player.getLocation().getY() - playerLoc.getY()) > 1
						|| Math.abs(player.getLocation().getZ() - playerLoc.getZ()) > 1)
				{
					player.sendMessage(ChatColor.YELLOW + "You cannot move while bandaging!");
					return;
				}
				
				String name = player.getCustomName();
				if (name == null)
					name = player.getName();
				
				//Check that the recipient is still in roughly the same location
				//Skip if self-heal
				if (!player.equals(recipient))
					if (Math.abs(recipient.getLocation().getX() - recipientLoc.getX()) > 1
						|| Math.abs(recipient.getLocation().getY() - recipientLoc.getY()) > 1
						|| Math.abs(recipient.getLocation().getZ() - recipientLoc.getZ()) > 1)
					{
						player.sendMessage(ChatColor.YELLOW + "You cannot bandage your patient while they are moving!");
						recipient.sendMessage(ChatColor.YELLOW + name + " cannot bandage you while you are moving!");
						return;
					}
				
				//Remove item from player's inventory.
	    		player.getInventory().removeItem(new ItemStack(Material.getMaterial(item.toUpperCase()), 1));
	    		player.updateInventory();
	    			
	    		//Heal the other player.
	    		recipient.setHealth(recipient.getHealth() + amountToHeal);

	    		//Award experience.
	    		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
	    		
	    		if (!prof.isPracticeFatigued(profession))
	    			prof.addExperience(profession, config.getInt("healing." + item + ".exp"));
	    			
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
