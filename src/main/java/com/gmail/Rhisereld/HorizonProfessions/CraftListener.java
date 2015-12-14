package com.gmail.Rhisereld.HorizonProfessions;

import haveric.recipeManager.api.events.RecipeManagerCraftEvent;
import haveric.recipeManager.recipes.WorkbenchRecipe;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * ProfessionListener contains all the methods that are called when certain crafting events happen in game.
 */
public class CraftListener implements Listener 
{
	FileConfiguration data;
	FileConfiguration config;
	
	//Constructor passing a reference to main.
	public CraftListener(FileConfiguration data, FileConfiguration config) 
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
				if (config.getConfigurationSection("recipes." + p + "." + t).contains(recipe.getName()))
				{
					profession = p;
					exp = config.getInt("recipes." + p + "." + t + "." + recipe.getName());
				}

		if (profession != null & exp != 0)
			prof.addExperience(profession, exp);
	}
}