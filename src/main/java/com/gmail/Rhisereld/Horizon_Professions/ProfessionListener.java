package com.gmail.Rhisereld.Horizon_Professions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
}
