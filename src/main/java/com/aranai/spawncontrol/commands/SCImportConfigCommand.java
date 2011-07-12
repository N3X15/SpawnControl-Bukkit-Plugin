package com.aranai.spawncontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aranai.spawncontrol.SpawnControl;

public class SCImportConfigCommand implements CommandExecutor {
	private SpawnControl plugin;

	public SCImportConfigCommand(SpawnControl plugin) {
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

    	if(p.isOp())
    	{
    		SpawnControl.log.info("[SpawnControl] Attempting to import player configuration file.");
    		plugin.importConfig();
    		return true;
    	}
		return false;
	}

}
