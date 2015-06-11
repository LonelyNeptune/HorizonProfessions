package com.gmail.Rhisereld.Horizon_Professions;

import haveric.recipeManager.api.events.RecipeManagerCraftEvent;
import haveric.recipeManager.flags.FlagPermission;

import java.util.Map;

import org.bukkit.entity.Player;
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
	String[] splitPermission;			//Permission string split into base/profession/tier.
	FlagPermission flag;
	
	//Constructor passing a reference to main.
	public CraftListener(Main main) 
	{
		this.main = main;
	}

	//Called when a player crafts a custom recipe.
	@EventHandler(priority = EventPriority.MONITOR)
	public void RecipeManagerCraftEvent(final RecipeManagerCraftEvent event)
	{
		Player player = event.getPlayer();
		
		//No permissions required for recipe.
		if ((flag = event.getRecipe().getFlag(FlagPermission.class)) == null)
			return;
		
		//List of permissions where at least one is required to craft the recipe.
		Map<String, Boolean> Permissions = flag.getPermissions();

		//Find the profession of the permission required, award experience for that profession.
		for (Map.Entry <String, Boolean> permission : Permissions.entrySet())
		{
			splitPermission = permission.getKey().split("\\.");
			
			//Check that the permission being examined belongs to this plugin.
			if (splitPermission[0].equalsIgnoreCase("horizon_professions"))

			for (int i = 0; i < main.PROFESSIONS.length; i++)
				if (main.PROFESSIONS[i].equalsIgnoreCase(splitPermission[1]))
					main.gainExperience(player, main.PROFESSIONS[i], main.EXP_REWARD_CRAFT[i]);					
		}
	}
}