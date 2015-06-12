package com.gmail.Rhisereld.Horizon_Professions;

import haveric.recipeManager.api.events.RecipeManagerCraftEvent;
import haveric.recipeManager.recipes.WorkbenchRecipe;

import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
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
	Set <String> configRecipes;			//A list of recipes in the configuration file.
	FileConfiguration config; //Configuration file.
	
	//Constructor passing a reference to main.
	public CraftListener(Main main) 
	{
		this.main = main;
		config = main.config.getConfig();
	}

	//Called when a player crafts a custom recipe.
	@EventHandler(priority = EventPriority.MONITOR)
	public void RecipeManagerCraftEvent(final RecipeManagerCraftEvent event)
	{
		Player player = event.getPlayer();
		WorkbenchRecipe recipe = event.getRecipe();
		
		//Go through configuration file, if a recipe matches add the corresponding experience.
		for (String profession: main.PROFESSIONS)
		{
			if (config.getConfigurationSection("recipes." + profession) != null)
				configRecipes = config.getConfigurationSection("recipes." + profession).getKeys(false);
			
			if (configRecipes != null)
				for (String configRecipe: configRecipes)
					if (recipe.getName().equalsIgnoreCase(configRecipe))
						main.gainExperience(player, profession, config.getInt("recipes." + profession + "." + configRecipe));			
		}
	}
}