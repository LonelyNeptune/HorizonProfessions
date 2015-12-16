package com.gmail.Rhisereld.HorizonProfessions;

import haveric.recipeManager.api.events.RecipeManagerCraftEvent;
import haveric.recipeManager.recipes.WorkbenchRecipe;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
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
	Permission perms;
	FileConfiguration data;
	static FileConfiguration config;
	Set<UUID> notified = new HashSet<UUID>();	//Used to ensure players are not spammed with the reason they are not gaining experience.
	
	public CraftListener(Permission perms, FileConfiguration data, FileConfiguration config) 
	{
		this.perms = perms;
		this.data = data;
		CraftListener.config = config;
	}
	
	/**
	 * updateConfig() updates the config file in the event of a configuration reload.
	 * 
	 * @param config
	 */
	public static void updateConfig(FileConfiguration config)
	{
		CraftListener.config = config;
	}

	//Called when a player crafts a custom recipe.
	@EventHandler(priority = EventPriority.MONITOR)
	public void RecipeManagerCraftEvent(final RecipeManagerCraftEvent event)
	{
		Player player = event.getPlayer();
		WorkbenchRecipe recipe = event.getRecipe();
		
		//Go through configuration file, if a recipe matches add the corresponding experience.
		int exp = 0;
		String profession = null;
		ProfessionStats prof = new ProfessionStats(perms, data, config, player.getUniqueId());
		
		for (String p: prof.getProfessions())
		{
			Set<String> recipes;
			try { recipes = config.getConfigurationSection("recipes." + p).getKeys(false); }
			catch (NullPointerException e)
			{ continue; }
			
			for (String r: recipes)
				if (r.equalsIgnoreCase(recipe.getName()))			
				{
					profession = p;
					exp = config.getInt("recipes." + p + "." + r);
				}
		}

		if (profession != null && exp != 0)
			addExperience(player, profession, exp);
	}
	
	
	/**
	 * addExperience() calls ProfessionStats to add experience, and also provides messages to the player
	 * if giving this experience fails.
	 * 
	 * @param prof
	 * @param profession
	 * @param exp
	 */
	private void addExperience(Player player, String profession, int exp)
	{
		UUID uuid = player.getUniqueId();
		ProfessionStats prof = new ProfessionStats(perms, data, config, uuid);
		int result = prof.addExperience(profession, exp);
		
		if (notified.contains(uuid))
			return;
		
		if (result == 4)
		{
			notified.add(uuid);
			player.sendMessage(ChatColor.YELLOW + "You cannot gain any experience because you have reached the maximum number of "
					+ "tiers permitted.");
		}
		if (result == 3)
		{
			notified.add(uuid);
			player.sendMessage(ChatColor.YELLOW + "You cannot gain any experience because you have reached the maximum tier in "
					+ profession);
		}
		if (result == 2)
		{
			notified.add(uuid);
			player.sendMessage(ChatColor.YELLOW + "You cannot gain any experience because you have not yet claimed all your tiers.");
		}
	}
}