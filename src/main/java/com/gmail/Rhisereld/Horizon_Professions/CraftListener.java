package com.gmail.Rhisereld.Horizon_Professions;

import haveric.recipeManager.api.events.RecipeManagerCraftEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/*
 * ProfessionListener contains all the methods that are called when certain crafting events happen in game.
 */
public class CraftListener implements Listener 
{
	static Plugin plugin = Main.plugin;	//A reference to this plugin.
	Main main;							//A reference to main.
	
	//Constructor passing a reference to main.
	public CraftListener(Main main) 
	{
		this.main = main;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void eventRMCraftEvent(final RecipeManagerCraftEvent event)
	{
		
	}
}
