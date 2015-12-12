package com.gmail.Rhisereld.Horizon_Professions;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class ProfessionStats 
{
	Set<String> professions;
	HashMap<String, Integer> experience;
	HashMap<String, Integer> levels;
	HashMap<String, Integer> tiers;
	HashMap<String, Integer> instructionFatigue;
	HashMap<String, Integer> practiceFatigue;
	int claimed;
	
	public ProfessionStats(ConfigAccessor data, UUID uuid)
	{
		String path = "data." + uuid.toString();
		professions = data.getConfig().getConfigurationSection(path).getKeys(false);
		
		for (String p: professions)
		{
			experience.put(p, data.getConfig().getInt(path + "." + p + ".exp"));
			levels.put(p, data.getConfig().getInt(path + "." + p + ".level"));
			tiers.put(p, data.getConfig().getInt(path + "." + p + ".tier"));
			instructionFatigue.put(p, data.getConfig().getInt(path + "." + p + ".instructionFatigue"));
			practiceFatigue.put(p, data.getConfig().getInt(path + "." + p + ".practiceFatigue"));
		}
		
		claimed = data.getConfig().getInt(path + ".claimed");
	}
	
	public ProfessionStats(ConfigAccessor data, UUID uuid, Set<String> professions, int claimed)
	{
		String path = "data." + uuid.toString();
		
		for (String p: professions)
		{
			experience.put(p, 0);
			levels.put(p, 0);
			tiers.put(p, 0);
			instructionFatigue.put(p, 0);
			practiceFatigue.put(p, 0);
		}
		
		data.getConfig().set(path + ".claimed", claimed);
		this.claimed = claimed;
	}
}
