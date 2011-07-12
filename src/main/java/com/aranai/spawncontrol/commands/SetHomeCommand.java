package com.aranai.spawncontrol.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class SetHomeCommand implements CommandExecutor {
	private SpawnControl plugin;

	public SetHomeCommand(SpawnControl plugin) {
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
        
		if(plugin.getSetting("enable_home") == SpawnControl.Settings.YES)
    	{
    		String setter = p.getName();;
    		String homeowner = setter;
    		Location l = p.getLocation();
    		
    		// Check cooldown exemption and status
    		long cooldown = plugin.cooldownLeft(p, "sethome");
    		if(cooldown > 0)
    		{
    			p.sendMessage("Cooldown is in effect. You must wait " + cooldown + " seconds.");
    			return true;
    		}
    		
    		// Set cooldown
    		plugin.setCooldown(p, "sethome");
    		
    		if(cmd.length > 0 && !plugin.canUseSetHomeProxy(p))
    		{
    			// User is trying to set home for another user but they don't have permission
    			p.sendMessage("You don't have permission to do that.");
    		}
    		else if(!plugin.canUseSetHomeBasic(p))
    		{
    			// User is trying to set home but they don't have permission
    			p.sendMessage("You don't have permission to do that.");
    		}
    		else
    		{
    			if(cmd.length > 0)
    			{
    				// Setting home for different player
    				homeowner = cmd[0];
    			}
    			
	    		if(plugin.setHome(homeowner, l, setter))
	    		{
	    			p.sendMessage("Home set successfully!");
	    		}
	    		else
	    		{
	    			p.sendMessage("Could not set Home!");
	    		}
    		}
    		
    		return true;
    	}
		return false;
	}

}
