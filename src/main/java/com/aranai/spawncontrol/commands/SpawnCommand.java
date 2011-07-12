package com.aranai.spawncontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class SpawnCommand implements CommandExecutor {
	private SpawnControl plugin;

	public SpawnCommand(SpawnControl plugin) {
		this.plugin=plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel,
			String[] args) {
		// Split the command in case it has parameters
        String commandName = command.getName().toLowerCase();
        Player p = null;
        
        if(sender instanceof Player)
        {
        	p = (Player)sender;
        }
        if(plugin.getSetting("enable_globalspawn") == SpawnControl.Settings.YES)
    	{
    		// Check cooldown exemption and status
    		long cooldown = plugin.cooldownLeft(p, "spawn");
    		if(cooldown > 0)
    		{
    			p.sendMessage("Cooldown is in effect. You must wait " + cooldown + " seconds.");
    			return true;
    		}
    		
    		// Set cooldown
    		plugin.setCooldown(p, "spawn");
    		
    		// Send player to spawn
    		if(!plugin.canUseSpawn(p))
    		{
    			// User doesn't have access to this command
    			p.sendMessage("You don't have permission to do that.");
    		}
    		else
    		{
    			int spawnBehavior = plugin.getSetting("behavior_spawn");
    			String spawnType = "global";
    			
    			// Check permissions availability for group spawn
    			if(spawnBehavior == SpawnControl.Settings.SPAWN_GROUP && !plugin.usePermissions)
    			{
    				SpawnControl.log.warning("[SpawnControl] Spawn behavior set to 'group' but group support is not available. Using global spawn.");
    				spawnBehavior = SpawnControl.Settings.SPAWN_GLOBAL;
    			}
    			
    			switch(spawnBehavior)
    			{
	    			case SpawnControl.Settings.SPAWN_HOME:
						// Send player to home
						plugin.sendHome(p);
					break;
    				case SpawnControl.Settings.SPAWN_GROUP:
    					// Send player to group spawn
    					plugin.sendToGroupSpawn(plugin.permissionHandler.getGroup(p.getWorld().getName(), p.getName()), p);
    				break;
    				case SpawnControl.Settings.SPAWN_GLOBAL:
    				default:
    					// Send player to global spawn
    					plugin.sendToSpawn(p);
    				break;
    			}
    			
	    		SpawnControl.log.info("[SpawnControl] Sending player "+p.getName()+" to spawn ("+spawnType+").");
    		}
        	return true;
    	}
		return false;
	}

}
