package com.aranai.spawncontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class SCConfigCommand implements CommandExecutor {
	private SpawnControl plugin;

	public SCConfigCommand(SpawnControl plugin) {
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
        if(plugin.canUseScConfig(p))
    	{
    		if(cmd.length < 2)
    		{
    			// Command format is wrong
    			p.sendMessage("Command format: /sc_config [setting] [value]");
    		}
    		else
    		{
	    		// Verify setting
	    		if(!SpawnControl.validSettings.contains(cmd[0]))
	    		{
	    			// Bad setting key
	    			p.sendMessage("Unknown configuration value.");
	    		}
	    		else
	    		{
	    			// Parse value
	    			try
	    			{
	    				int tmpval = Integer.parseInt(cmd[1]);
	    				
	    				if(tmpval < 0)
	    				{
	    					p.sendMessage("Value must be >= 0.");
	    				}
	    				else
	    				{
	    					// Save
	    					if(!plugin.setSetting(cmd[0], tmpval, p.getName()))
	    					{
	    						p.sendMessage("Could not save value for '"+cmd[0]+"'!");
	    					}
	    					else
	    					{
	    						p.sendMessage("Saved value for '"+cmd[0]+"'.");
	    					}
	    				}
	    			}
	    			catch(Exception ex)
	    			{
	    				// Bad number
	    				p.sendMessage("Couldn't read value.");
	    			}
	    		}
    		}
    		return true;
    	}
		return false;
	}

}
