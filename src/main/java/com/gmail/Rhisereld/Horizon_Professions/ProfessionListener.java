package com.gmail.Rhisereld.Horizon_Professions;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		//Attempt to load player stats.
		main.loadPlayerStats(player);
	}
	
	//Called when a player logs off the server.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		
		//Save player stats
		main.savePlayerStats(player);
		main.removePlayerStats(player);
	}
	
	//Called when a monster or player dies.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onMonsterDeath(EntityDeathEvent event)
	{
		Entity entity = event.getEntity();
		EntityDamageByEntityEvent dEvent;
		Player player;
		String profession;
		int exp;
		
		//Check that it was killed by another entity
		if(!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return;
		
		dEvent = (EntityDamageByEntityEvent) entity.getLastDamageCause();
		
		//Check that it was killed by a player
		if(!(dEvent.getDamager() instanceof Player))
			return;
		
		player = (Player) dEvent.getDamager();
		
		//See if options are specified in the configuration file.
    	Set <String> monsters = main.config.getConfig().getConfigurationSection("slaying.").getKeys(false);
    	
    	for (String monster: monsters)
    		//It's in the config
    		if (entity.getType().toString().equalsIgnoreCase(monster))
    		{
    			profession = main.config.getConfig().getString("slaying." + monster + ".profession");
    			exp = main.config.getConfig().getInt("slaying." + monster + ".exp");
    			
    			//Check that the player doesn't have practice fatigue.
    			if (main.getPracticeFatigue(player, profession ) > 0)
    				return;
    			
    			main.gainExperience(player, profession, exp);
    		}
	}
}
