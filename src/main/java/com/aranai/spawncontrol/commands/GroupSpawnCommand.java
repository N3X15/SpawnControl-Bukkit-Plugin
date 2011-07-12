package com.aranai.spawncontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class GroupSpawnCommand implements CommandExecutor {
	private SpawnControl plugin;

	public GroupSpawnCommand(SpawnControl plugin) {
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
        if(plugin.getSetting("enable_groupspawn") == SpawnControl.Settings.YES)
    	{
    		// Check cooldown exemption and status
    		long cooldown = plugin.cooldownLeft(p, "groupspawn");
    		if(cooldown > 0)
    		{
    			p.sendMessage("Cooldown is in effect. You must wait " + cooldown + " seconds.");
    			return true;
    		}
    		
    		// Set cooldown
    		plugin.setCooldown(p, "groupspawn");
    		
    		// Send player to group spawn
    		if(!plugin.canUseGroupSpawn(p))
    		{
    			// User doesn't have access to this command
    			p.sendMessage("You don't have permission to do that.");
    		}
    		else
    		{
    			// Get group spawn for player
    			String group = plugin.permissionHandler.getGroup(p.getWorld().getName(), p.getName());
	    		SpawnControl.log.info("[SpawnControl] Attempting to send player "+p.getName()+" to group spawn.");
	        	plugin.sendToGroupSpawn(group, p);
    		}
        	return true;
    	}
		return false;
	}

}
