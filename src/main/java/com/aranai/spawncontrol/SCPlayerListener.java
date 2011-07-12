package com.aranai.spawncontrol;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handle events for all Player related events
 * @author Timberjaw
 */
public class SCPlayerListener extends PlayerListener {
    private final SpawnControl plugin;
    

    public SCPlayerListener(SpawnControl instance) {
        plugin = instance;
    }

	public void onPlayerJoin(PlayerJoinEvent e)
    {
    	if(plugin.getHome(e.getPlayer().getName(), e.getPlayer().getWorld()) == null)
    	{
    		// Probably a new player
    		SpawnControl.log.info("[SpawnControl] Sending new player " + e.getPlayer().getName() + " to global spawn.");
    		
    		// Send player to global spawn
    		plugin.sendToSpawn(e.getPlayer());
    		
    		// Set home for player
    		plugin.setHome(e.getPlayer().getName(), plugin.getSpawn(e.getPlayer().getWorld()), "SpawnControl");
    	}
    	
    	int jb = plugin.getSetting("behavior_join");
    	if(jb != SpawnControl.Settings.JOIN_NONE)
    	{
	    	// Get player
	    	Player p = e.getPlayer();
	    	
	    	// Check for home
	    	SpawnControl.log.info("[SpawnControl] Attempting to respawn player "+p.getName()+" (joining).");
	    	
	    	switch(jb)
	    	{
	    		case SpawnControl.Settings.JOIN_HOME:
	    			plugin.sendHome(p);
	    			break;
	    		case SpawnControl.Settings.JOIN_GROUPSPAWN:
	    			if(plugin.usePermissions)
	    			{
	    				plugin.sendToGroupSpawn(plugin.permissionHandler.getGroup(p.getWorld().getName(), p.getName()), p);
	    			}
	    			else
	    			{
	    				plugin.sendToSpawn(p);
	    			}
	    			break;
	    		case SpawnControl.Settings.JOIN_GLOBALSPAWN:
	    		default:
	    			plugin.sendToSpawn(p);
	    			break;
	    	}
    	}
    }
    
    public void onPlayerRespawn(PlayerRespawnEvent e)
    {
    	int db = plugin.getSetting("behavior_death");
    	if(db != SpawnControl.Settings.DEATH_NONE)
    	{
    		// Get player
	    	Player p = e.getPlayer();
	    	
	    	// Check for home
	    	SpawnControl.log.info("[SpawnControl] Attempting to respawn player "+p.getName()+" (respawning).");
	    	
	    	// Build respawn location
	    	Location l;
	    	
    		switch(db)
	    	{
	    		case SpawnControl.Settings.DEATH_HOME:
	    			l = plugin.getHome(p.getName(), p.getWorld());
	    			break;
	    		case SpawnControl.Settings.DEATH_GROUPSPAWN:
	    			if(plugin.usePermissions)
	    			{
	    				l = plugin.getGroupSpawn(plugin.permissionHandler.getGroup(p.getWorld().getName(), p.getName()), p.getWorld());
	    			}
	    			else
	    			{
	    				l = plugin.getGroupSpawn("scglobal", p.getWorld());
	    			}
	    			break;
	    		case SpawnControl.Settings.DEATH_GLOBALSPAWN:
	    		default:
	    			l = plugin.getGroupSpawn("scglobal", p.getWorld());
	    			break;
	    	}
    		
    		if(l == null)
    		{
    			// Something has gone wrong
    			SpawnControl.log.warning("[SpawnControl] Could not find respawn for " + p.getName() + "!");
    			return;
    		}
    		else
    		{
    			// Set world
    			l.setWorld(p.getWorld());
    		}
    		
    		SpawnControl.log.info("[SpawnControl] DEBUG: Respawn Location: " + l.toString());
    		e.setRespawnLocation(l);
    	}
    }
}