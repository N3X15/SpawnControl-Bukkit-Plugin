package com.aranai.spawncontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class SetSpawnCommand implements CommandExecutor {
	private SpawnControl plugin;

	public SetSpawnCommand(SpawnControl plugin) {
		this.plugin=plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel,
			String[] args) {
		// Split the command in case it has parameters
        Player p = null;
        
        if(sender instanceof Player)
        {
        	p = (Player)sender;
        }
        if(plugin.getSetting("enable_globalspawn") == SpawnControl.Settings.YES)
    	{
    		// Set global spawn
    		if(!plugin.canUseSetSpawn(p))
    		{
    			// User doesn't have access to this command
    			p.sendMessage("You don't have permission to do that.");
    		}
    		else
    		{
	    		SpawnControl.log.info("[SpawnControl] Attempting to set global spawn.");
	        	if(plugin.setSpawn(p.getLocation(), p.getName()))
	        	{
	        		p.sendMessage("Global spawn set successfully!");
	        	}
	        	else
	        	{
	        		p.sendMessage("Could not set global spawn.");
	        	}
    		}
        	return true;
    	}
		return false;
	}

}
