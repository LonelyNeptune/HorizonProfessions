package com.gmail.Rhisereld.Horizon_Professions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class ProfessionListener implements Listener
{
	static Plugin plugin = Main.plugin;
	Main main;
	
	public ProfessionListener(Main main) 
	{
		this.main = main;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		//Attempt to load player stats.
		main.loadPlayerStats(player);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		
		//Save player stats
		main.savePlayerStats(player);
		main.removePlayerStats(player);
	}
}
