package com.aranai.spawncontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class SetGroupSpawnCommand implements CommandExecutor {
	private SpawnControl plugin;

	public SetGroupSpawnCommand(SpawnControl plugin) {
		this.plugin=plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel,
			String[] args) {
		// Split the command in case it has parameters
    	String[] cmd = args;
        Player p = null;
        
        if(sender instanceof Player)
        {
        	p = (Player)sender;
        }
        if(plugin.getSetting("enable_groupspawn") == SpawnControl.Settings.YES)
    	{    		
    		String group = null;
    		
    		// Set group spawn
    		if(!plugin.canUseSetGroupSpawn(p))
    		{
    			// User doesn't have access to this command
    			p.sendMessage("You don't have permission to do that.");
    		}
    		else if(!(cmd.length > 0))
    		{
    			// User didn't specify a group
    			p.sendMessage("Command format: /setgroupspawn [group]");
    		}
    		else
    		{
    			group = cmd[0];
	    		SpawnControl.log.info("[SpawnControl] Setting group spawn for '"+group+"'.");
	        	if(plugin.setGroupSpawn(group, p.getLocation(), p.getName()))
	        	{
	        		p.sendMessage("Group spawn for "+group+" set successfully!");
	        	}
	        	else
	        	{
	        		p.sendMessage("Could not set group spawn for "+group+".");
	        	}
    		}
        	return true;
    	}
		return false;
	}

}
