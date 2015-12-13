package com.gmail.Rhisereld.HorizonProfessions;

import haveric.recipeManager.api.events.RecipeManagerCraftEvent;
import haveric.recipeManager.recipes.WorkbenchRecipe;

import java.util.Set;

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
	ConfigAccessor data;
	ConfigAccessor config;
	Plugin plugin;					//A reference to this plugin.
	String[] splitPermission;		//Permission string split into base/profession/tier.
	Set <String> configRecipes;		//A list of recipes in the configuration file.
	
	//Constructor passing a reference to main.
	public CraftListener(ConfigAccessor data, ConfigAccessor config) 
	{
		this.data = data;
		this.config = config;
	}

	//Called when a player crafts a custom recipe.
	@EventHandler(priority = EventPriority.MONITOR)
	public void RecipeManagerCraftEvent(final RecipeManagerCraftEvent event)
	{
		Player player = event.getPlayer();
		WorkbenchRecipe recipe = event.getRecipe();
		
		//Go through configuration file, if a recipe matches add the corresponding experience.
		ProfessionStats prof = new ProfessionStats(data, config, player.getUniqueId());
		int exp = 0;
		String profession = null;
		
		for (String p: prof.getProfessions())
			for (String t: prof.getTiers())
				if (config.getConfig().getConfigurationSection("recipes." + p + "." + t).contains(recipe.getName()))
				{
					profession = p;
					exp = config.getConfig().getInt("recipes." + p + "." + t + "." + recipe.getName());
				}

		if (profession != null & exp != 0)
			prof.addExperience(profession, exp);
	}
}