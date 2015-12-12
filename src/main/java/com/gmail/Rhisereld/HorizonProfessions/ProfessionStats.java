package com.gmail.Rhisereld.HorizonProfessions;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ProfessionStats 
{
	List<String> professions;
	HashMap<String, Integer> experience;
	HashMap<String, Integer> levels;
	HashMap<String, Integer> tiers;
	HashMap<String, Integer> instructionFatigue;
	HashMap<String, Integer> practiceFatigue;
	int claimed;
	
	/**
	 * Constructor for fetching existing stats from the data file.
	 * 
	 * @param data
	 * @param uuid
	 */
	public ProfessionStats(ConfigAccessor data, ConfigAccessor config, UUID uuid)
	{		
		String path = "data." + uuid.toString();
		professions = config.getConfig().getStringList("professions");
		
		for (String p: professions)
		{
			experience.put(p, data.getConfig().getInt(path + "." + p + ".exp"));
			levels.put(p, data.getConfig().getInt(path + "." + p + ".level"));
			tiers.put(p, data.getConfig().getInt(path + "." + p + ".tier"));
			instructionFatigue.put(p, data.getConfig().getInt(path + "." + p + ".instructionFatigue"));
			practiceFatigue.put(p, data.getConfig().getInt(path + "." + p + ".practiceFatigue"));
		}
		
		if (data.getConfig().getConfigurationSection(path).getKeys(false).isEmpty())
			claimed = config.getConfig().getInt("claimable_tiers");
		else
			claimed = data.getConfig().getInt(path + ".claimed");
	}
}
