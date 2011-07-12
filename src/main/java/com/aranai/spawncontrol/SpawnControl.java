package com.aranai.spawncontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.aranai.spawncontrol.commands.GroupSpawnCommand;
import com.aranai.spawncontrol.commands.HomeCommand;
import com.aranai.spawncontrol.commands.SCConfigCommand;
import com.aranai.spawncontrol.commands.SCImportConfigCommand;
import com.aranai.spawncontrol.commands.SCImportGroupConfigCommand;
import com.aranai.spawncontrol.commands.SetGroupSpawnCommand;
import com.aranai.spawncontrol.commands.SetHomeCommand;
import com.aranai.spawncontrol.commands.SetSpawnCommand;
import com.aranai.spawncontrol.commands.SpawnCommand;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * SpawnControl for Bukkit
 *
 * @author Timberjaw
 */
public class SpawnControl extends JavaPlugin {
    private final SCPlayerListener playerListener = new SCPlayerListener(this);
    private final SCWorldListener worldListener = new SCWorldListener(this);
    protected Connection conn;
    public static Logger log;
    public final static String directory = "plugins/SpawnControl";
    public final static String db = "jdbc:sqlite:" + SpawnControl.directory + File.separator + "spawncontrol.db";
    
    // Schema version
    public static final int SchemaVersion = 1;
    
    // SQL Strings
    protected static String SQLCreatePlayersTable = "CREATE TABLE `players` (`id` INTEGER PRIMARY KEY, `name` varchar(32) NOT NULL, "
		+"`world` varchar(50), `x` REAL, `y` REAL, `z` REAL, `r` REAL, `p` REAL, "
		+"`updated` INTEGER, `updated_by` varchar(32));";
    protected static String SQLCreatePlayersIndex = "CREATE UNIQUE INDEX playerIndex on `players` (`name`,`world`);";
    protected static String SQLCreateGroupsTable = "CREATE TABLE `groups` (`id` INTEGER PRIMARY KEY, `name` varchar(32) NOT NULL, "
		+"`world` varchar(50), `x` REAL, `y` REAL, `z` REAL, `r` REAL, `p` REAL, "
		+"`updated` INTEGER, `updated_by` varchar(32));";
    protected static String SQLCreateGroupsIndex = "CREATE UNIQUE INDEX groupIndex on `groups` (`name`,`world`);";
    
    // Permissions
    public PermissionHandler permissionHandler = null;
    public boolean usePermissions = false;
    
    // Cache variables
    private Hashtable<String,Integer> activePlayerIds;
    private Hashtable<Integer,Location> homes;
    private Hashtable<String,Integer> activeGroupIds;
    private Hashtable<Integer,Location> groupSpawns;
    private Hashtable<String,Boolean> respawning;
    private Hashtable<String,Long> cooldowns;
    private String lastSetting;
    private int lastSettingValue;
    
    // Settings
    public static final class Settings {
    	public static final int UNSET = -1;
    	public static final int NO = 0;
    	public static final int YES = 1;
    	public static final int DEATH_NONE = 0;
    	public static final int DEATH_HOME = 1;
    	public static final int DEATH_GROUPSPAWN = 2;
    	public static final int DEATH_GLOBALSPAWN = 3;
    	public static final int JOIN_NONE = 0;
    	public static final int JOIN_HOME = 1;
    	public static final int JOIN_GROUPSPAWN = 2;
    	public static final int JOIN_GLOBALSPAWN = 3;
    	public static final int GLOBALSPAWN_DEFAULT = 0;
    	public static final int GLOBALSPAWN_OVERRIDE = 1;
    	public static final int SPAWN_GLOBAL = 0;
    	public static final int SPAWN_GROUP = 1;
    	public static final int SPAWN_HOME = 2;
    }
    
    public static final List<String> validSettings = Arrays.asList(
    		"enable_home", "enable_groupspawn", "enable_globalspawn",
    		"behavior_join", "behavior_death", "behavior_globalspawn", "behavior_spawn",
    		"cooldown_home", "cooldown_sethome", "cooldown_spawn", "cooldown_groupspawn" 
    );

    public SpawnControl()
    {
    	super();
    }
    
    // Initialize database
    private void initDB()
    {
    	ResultSet rs = null;
    	Statement st = null;
    	
    	try
        {
    		Class.forName("org.sqlite.JDBC");
        	conn = DriverManager.getConnection(db);
        	
        	DatabaseMetaData dbm = conn.getMetaData();
        	
        	// Check players table
            rs = dbm.getTables(null, null, "players", null);
            if (!rs.next())
            {
            	// Create table
            	log.info("[SpawnControl]: Table 'players' not found, creating.");
            	
            	conn.setAutoCommit(false);
                st = conn.createStatement();
                st.execute(SpawnControl.SQLCreatePlayersTable);
                st.execute(SpawnControl.SQLCreatePlayersIndex);
                conn.commit();
                
                log.info("[SpawnControl]: Table 'players' created.");
            }
            
            // Check groups table
            rs = dbm.getTables(null, null, "groups", null);
            if (!rs.next())
            {
            	// Create table
            	log.info("[SpawnControl]: Table 'groups' not found, creating.");
            	
            	conn.setAutoCommit(false);
                st = conn.createStatement();
                st.execute(SpawnControl.SQLCreateGroupsTable);
                st.execute(SpawnControl.SQLCreateGroupsIndex);
                conn.commit();
                
                log.info("[SpawnControl]: Table 'groups' created.");
            }
            
            // Check settings table
            boolean needSettings = false;
            rs = dbm.getTables(null, null, "settings", null);
            if (!rs.next())
            {
            	// Create table
            	needSettings = true;
            	System.out.println("[SpawnControl]: Table 'settings' not found, creating.");
            	
            	conn.setAutoCommit(false);
                st = conn.createStatement();
                st.execute("CREATE TABLE `settings` (`setting` varchar(32) PRIMARY KEY, `value` INT, "
                		+"`updated` INTEGER, `updated_by` varchar(32));");
                conn.commit();
                
                log.info("[SpawnControl]: Table 'settings' created.");
            }
        	
	        rs.close();
	        conn.close();
	        
	        if(needSettings)
	        {
	            // Insert default settings
		        this.setSetting("enable_home", Settings.YES, "initDB");
		        this.setSetting("enable_groupspawn", Settings.YES, "initDB");
		        this.setSetting("enable_globalspawn", Settings.YES, "initDB");
		        this.setSetting("behavior_death", Settings.DEATH_GLOBALSPAWN, "initDB");
		        this.setSetting("behavior_join", Settings.JOIN_NONE, "initDB");
		        this.setSetting("behavior_globalspawn", Settings.GLOBALSPAWN_DEFAULT, "initDB");
		        this.setSetting("behavior_spawn", Settings.SPAWN_GLOBAL, "initDB");
		        this.setSetting("schema_version", SpawnControl.SchemaVersion, "initDB");
		        this.setSetting("cooldown_home", 0, "initDB");
		        this.setSetting("cooldown_sethome", 0, "initDB");
		        this.setSetting("cooldown_groupspawn", 0, "initDB");
		        this.setSetting("cooldown_spawn", 0, "initDB");
	        }
	        
	        // Check schema version
	    	int sv = this.getSetting("schema_version");
	    	if(sv < SpawnControl.SchemaVersion)
	    	{
	    		SCUpdater.run(sv, this);
	    	}
        }
        catch(SQLException e)
        {
        	// ERROR
        	System.out.println("[initDB] DB ERROR - " + e.getMessage() + " | SQLState: " + e.getSQLState() + " | Error Code: " + e.getErrorCode());
        }
        catch(Exception e)
        {
        	// Error
        	System.out.println("Error: " + e.getMessage());
        	e.printStackTrace();
        }
    }

    public void onEnable() {
    	log = Logger.getLogger("Minecraft");
    	
    	// Initialize active player ids and homes
        this.activePlayerIds = new Hashtable<String,Integer>();
        this.homes = new Hashtable<Integer,Location>();
        
        // Initialize active group ids and group spawns
        this.activeGroupIds = new Hashtable<String,Integer>();
        this.groupSpawns = new Hashtable<Integer,Location>();
        
        // Initialize respawn list
        this.respawning = new Hashtable<String,Boolean>();
        
        // Initialize cooldown list
        this.cooldowns = new Hashtable<String,Long>();
        
        // Initialize last setting info
        this.lastSetting = "";
        this.lastSettingValue = -1;
    	
    	// Make sure we have a local folder for our database and such
        if (!new File(directory).exists()) {
            try {
                (new File(directory)).mkdir();
            } catch (Exception e) {
                SpawnControl.log.log(Level.SEVERE, "[SpawnControl]: Unable to create spawncontrol/ directory.");
            }
        }
        
        // Initialize the database
        this.initDB();
        
        // Initialize permissions system
        setupPermissions();
        
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        
        // Get player join
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        
        // Get player respawn
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Highest, this);
        
        // Get world load
        pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);
        
        // Enable message
        PluginDescriptionFile pdfFile = this.getDescription();
        log.info( "[SpawnControl] version [" + pdfFile.getVersion() + "] loaded" );

        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setgroupspawn").setExecutor(new SetGroupSpawnCommand(this));
        getCommand("groupspawn").setExecutor(new GroupSpawnCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sc_config").setExecutor(new SCConfigCommand(this));
        getCommand("scimportconfig").setExecutor(new SCImportConfigCommand(this));
        getCommand("scimportgroupconfig").setExecutor(new SCImportGroupConfigCommand(this));
    }
    
    public void onDisable() {
        // Disable message
    	PluginDescriptionFile pdfFile = this.getDescription();
    	log.info( "[SpawnControl] version [" + pdfFile.getVersion() + "] unloaded" );
    }
    

    private boolean setupPermissions() { 
        Plugin permissionPlugin = getServer().getPluginManager().getPlugin("Permissions");

        if (permissionHandler == null) {
            if (permissionPlugin != null) {
                permissionHandler = ((Permissions) permissionPlugin).getHandler();
                return true;
            } else {
                log.warning("[SpawnControl] Permission system not detected, defaulting to OP");
            }
        }
        return false;
    }
    
    // Get timestamp
    public int getTimeStamp()
    {
    	return (int) (System.currentTimeMillis() / 1000L);
    }
    
    // Mark as respawning
    public void markPlayerRespawning(String name) { this.markPlayerDoneRespawning(name); this.respawning.put(name, true); }
    // Mark as done respawning
    public void markPlayerDoneRespawning(String name) { this.respawning.remove(name); }
    // Check to see if the player is respawning
    public boolean isPlayerRespawning(String name) { return this.respawning.containsKey(name); }
    
    
    // Get setting
    public int getSetting(String name)
    {
    	Connection conn = null;
    	PreparedStatement ps = null;
        ResultSet rs = null;
        int value = -1;
        
        if(this.lastSetting.equals(name))
        {
        	return this.lastSettingValue;
        }
		
		// Get from database
		try
        {
    		Class.forName("org.sqlite.JDBC");
        	conn = DriverManager.getConnection(db);
        	ps = conn.prepareStatement("SELECT * FROM `settings` WHERE `setting` = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
             
            while (rs.next()) { value = rs.getInt("value"); this.lastSetting = name; this.lastSettingValue = value; }
        }
        catch(Exception e)
        {
        	// Error
        	SpawnControl.log.warning("[SpawnControl] Could not get setting '"+name+"': " + e.getMessage());
        }
        finally
        {
        	if(conn != null) { try { conn.close(); } catch(Exception e) { e.printStackTrace(); } }
        }
        
        return value;
    }
    
    // Set setting
    public boolean setSetting(String name, int value, String setter)
    {
        boolean success = true;
        
        try
        {
	    	Class.forName("org.sqlite.JDBC");
	    	Connection conn = DriverManager.getConnection(db);
	    	conn.setAutoCommit(false);
	        PreparedStatement ps = conn.prepareStatement("REPLACE INTO `settings` (`setting`,`value`,`updated`,`updated_by`) VALUES (?, ?, ?, ?);");
	        ps.setString(1, name);
	        ps.setInt(2, value);
	        ps.setInt(3, this.getTimeStamp());
	        ps.setString(4, setter);
	        ps.execute();
	        conn.commit();
	        conn.close();
	        
	        if(this.lastSetting.equals(name))
	        {
	        	this.lastSetting = "";
	        	this.lastSettingValue = -1;
	        }
        }
        catch(Exception e)
        {
        	SpawnControl.log.severe("[SpawnControl] Failed to save setting '"+name+"' with value '"+value+"'");
        	success = false;
        }
        
    	return success;
    }
    
    // Spawn
    public void sendToSpawn(Player p)
    {
    	this.sendToGroupSpawn("scglobal", p);
    }
    
    // Set spawn
    public boolean setSpawn(Location l, String setter)
    {
    	return this.setGroupSpawn("scglobal", l, setter);
    }
    
    // Get spawn
    public Location getSpawn(World world)
    {
    	return this.getGroupSpawn("scglobal", world);
    }
    
    // Home
    public void sendHome(Player p)
    {
    	// Check for home
    	String nameHash = p.getName() + "-" + p.getWorld().getName();
    	if(!this.activePlayerIds.contains(nameHash))
    	{
    		if(!this.getPlayerData(p.getName(), p.getWorld()))
    		{
    			// No home available, use global
    			this.sendToSpawn(p);
    			return;
    		}
    	}
    	
    	// Teleport to home
    	p.teleport(this.homes.get(this.activePlayerIds.get(nameHash)));
    }
    
    // Get home
    public Location getHome(String name, World world)
    {
    	// Check for home
    	String nameHash = name + "-" + world.getName();
    	if(!this.activePlayerIds.contains(nameHash))
    	{
    		if(this.getPlayerData(name, world))
    		{
    			// Found home!
    			return this.homes.get(this.activePlayerIds.get(nameHash));
    		}
    	}
    	
    	return null;
    }
    
    // Sethome
    public boolean setHome(String name, Location l, String updatedBy)
    {
    	Connection conn = null;
    	PreparedStatement ps = null;
        Boolean success = false;
		
		// Save to database
		try
        {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(db);
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("REPLACE INTO `players` (id, name, world, x, y, z, r, p, updated, updated_by) VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			ps.setString(1, name);
			ps.setString(2, l.getWorld().getName());
			ps.setDouble(3, l.getX());
			ps.setDouble(4, l.getY());
			ps.setDouble(5, l.getZ());
			ps.setFloat(6, l.getYaw());
			ps.setFloat(7, l.getPitch());
			ps.setInt(8, this.getTimeStamp());
			ps.setString(9, updatedBy);
			ps.execute();
			conn.commit();
        	conn.close();
        	
        	success = true;
        }
        catch(SQLException e)
        {
        	// ERROR
        	System.out.println("[setHome] DB ERROR - " + e.getMessage() + " | SQLState: " + e.getSQLState() + " | Error Code: " + e.getErrorCode());
        }
        catch(Exception e)
        {
        	// Error
        	System.out.println("Error: " + e.getMessage());
        	e.printStackTrace();
        }
        
        if(success)
        {
        	// Update local cache
        	this.getPlayerData(name, l.getWorld());
        }
        
        return success;
    }
    
    // Group spawn
    public void sendToGroupSpawn(String group, Player p)
    {
    	// Check for spawn
    	String groupHash = group + "-" + p.getWorld().getName();
    	if(!this.activeGroupIds.contains(groupHash))
    	{
    		if(!this.getGroupData(group, p.getWorld()))
    		{
    			if(group.equals("scglobal"))
    			{
    				// No global spawn found, set one
    				this.setGroupSpawn("scglobal", p.getWorld().getSpawnLocation(), "sendToGroupSpawn");
    			}
    			else
    			{
	    			// No group spawn available, use global
	    			this.sendToSpawn(p);
	    			return;
    			}
    		}
    	}
    	
    	// Teleport to home
    	p.teleport(this.groupSpawns.get(this.activeGroupIds.get(groupHash)));
    }
    
    // Set group spawn
    public boolean setGroupSpawn(String group, Location l, String updatedBy)
    {
    	Connection conn = null;
    	PreparedStatement ps = null;
        Boolean success = false;
		
		// Save to database
		try
        {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(db);
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("REPLACE INTO `groups` (id, name, world, x, y, z, r, p, updated, updated_by) VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			ps.setString(1, group);
			ps.setString(2, l.getWorld().getName());
			ps.setDouble(3, l.getX());
			ps.setDouble(4, l.getY());
			ps.setDouble(5, l.getZ());
			ps.setFloat(6, l.getYaw());
			ps.setFloat(7, l.getPitch());
			ps.setInt(8, this.getTimeStamp());
			ps.setString(9, updatedBy);
			ps.execute();
			conn.commit();
        	conn.close();
        	
        	success = true;
        }
        catch(SQLException e)
        {
        	// ERROR
        	System.out.println("[setGroupSpawn] DB ERROR - " + e.getMessage() + " | SQLState: " + e.getSQLState() + " | Error Code: " + e.getErrorCode());
        }
        catch(Exception e)
        {
        	// Error
        	System.out.println("Error: " + e.getMessage());
        	e.printStackTrace();
        }
        
        if(success)
        {
        	// Update local cache
        	this.getGroupData(group, l.getWorld());
        }
        
        return success;
    }
    
    // Get group spawn
    public Location getGroupSpawn(String group, World world)
    {
    	// Check for spawn
    	if(group.equals("Default"))
    	{
    		group = "scglobal";
    	}
    	
    	// Include group world in key
    	String groupHash = group + "-" + world.getName();
    	
    	if(this.activeGroupIds.contains(groupHash) || this.getGroupData(group, world))
    	{
    		return this.groupSpawns.get(this.activeGroupIds.get(groupHash));
    	}
    	
    	SpawnControl.log.warning("[SpawnControl] Could not find or load group spawn for '"+group+"'!");
    	
    	return null;
    }
    
    // Utility
    private boolean getPlayerData(String name, World world)
    {
    	Connection conn = null;
    	PreparedStatement ps = null;
        ResultSet rs = null;
        Boolean success = false;
        Integer id = 0;
		
		// Get from database
		try
        {
    		Class.forName("org.sqlite.JDBC");
        	conn = DriverManager.getConnection(db);
        	//conn.setAutoCommit(false);
        	ps = conn.prepareStatement("SELECT * FROM `players` WHERE `name` = ? AND `world` = ?");
            ps.setString(1, name);
            ps.setString(2, world.getName());
            rs = ps.executeQuery();
            //conn.commit();
             
             while (rs.next()) {
                 success = true;
                 String nameHash = name + "-" + world.getName();
                 this.activePlayerIds.put(nameHash, id);
                 Location l = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("r"), rs.getFloat("p"));
                 this.homes.put(id, l);
             }
        	conn.close();
        }
        catch(SQLException e)
        {
        	// ERROR
        	System.out.println("[getPlayerData] DB ERROR - " + e.getMessage() + " | SQLState: " + e.getSQLState() + " | Error Code: " + e.getErrorCode());
        }
        catch(Exception e)
        {
        	// Error
        	System.out.println("Error: " + e.getMessage());
        	e.printStackTrace();
        }
        
        return success;
    }
    
    public boolean getGroupData(String name, World world)
    {
    	Connection conn = null;
    	PreparedStatement ps = null;
        ResultSet rs = null;
        Boolean success = false;
        Integer id = 0;
		
		// Get from database
		try
        {
    		Class.forName("org.sqlite.JDBC");
        	conn = DriverManager.getConnection(db);
        	ps = conn.prepareStatement("SELECT * FROM `groups` WHERE `name` = ? AND `world` = ?");
            ps.setString(1, name);
            ps.setString(2, world.getName());
            rs = ps.executeQuery();
             
             while (rs.next()) {
                 success = true;
                 String nameHash = name + "-" + world.getName();
                 this.activeGroupIds.put(nameHash, id);
                 Location l = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("r"), rs.getFloat("p"));
                 this.groupSpawns.put(id, l);
             }
        	conn.close();
        }
        catch(SQLException e)
        {
        	// ERROR
        	System.out.println("[getGroupData] DB ERROR - " + e.getMessage() + " | SQLState: " + e.getSQLState() + " | Error Code: " + e.getErrorCode());
        }
        catch(Exception e)
        {
        	// Error
        	System.out.println("Error: " + e.getMessage());
        	e.printStackTrace();
        }
        
        return success;
    }
    
    public void setCooldown(Player p, String cooldown)
    {
    	if(!this.isExemptFromCooldowns(p, cooldown)) {
    		String key = p.getName()+"."+cooldown;
    		long cooldownAmount = this.getSetting("cooldown_"+cooldown);

    		if(cooldownAmount > 0)
    		{
    			cooldowns.put(key, System.currentTimeMillis());
    		}
    	}
    }
    
    public long getCooldownRemaining(Player p, String cooldown)
    {
    	String key = p.getName()+"."+cooldown;
    	long cooldownAmount = this.getSetting("cooldown_"+cooldown);
    	
    	if(cooldowns.containsKey(key))
    	{
    		// Compare time
    		long timeElapsed = (System.currentTimeMillis() - cooldowns.get(key))/1000;
    		
    		if(timeElapsed > cooldownAmount)
    		{
    			// Remove cooldown
    			cooldowns.remove(key);
    		}
    		else
    		{
    			// Return number of seconds left
    			return cooldownAmount-timeElapsed;
    		}
    	}
    	
    	return 0;
    }
    
    public void importConfig()
    {
    	File cf = new File(directory+"/spawncontrol-players.properties");
    	
    	if(cf.exists())
    	{
    		// Attempt import
            BufferedReader reader = null;

            try
            {
                reader = new BufferedReader(new FileReader(cf));
                String text = null;

                // Read a line
                while ((text = reader.readLine()) != null)
                {
                	// Skip if comment
                	if(!text.startsWith("#"))
                	{
                		// Format: Timberjaw=-86.14281646837361\:75.0\:233.43342838872454\:168.00002\:17.40001
                		text = text.replaceAll("\\\\", "");
                		String[] parts = text.split("=");
                		String name = parts[0];
                		String[] coords = parts[1].split(":");
                		Location l = new Location(null,
                				Double.parseDouble(coords[0]),
                				Double.parseDouble(coords[1]),
                				Double.parseDouble(coords[2]),
                				Float.parseFloat(coords[3]),
                				Float.parseFloat(coords[4]));
                		
                		// Set home
                		this.setHome(name, l, "ConfigImport");
                		
                		log.info("[SpawnControl] Found home for '"+name+"' at: "+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch());
                	}
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (reader != null)
                    {
                        reader.close();
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
    	}
    }
    
    public void importGroupConfig()
    {
    	File cf = new File(directory+"/spawncontrol-groups.properties");
    	
    	if(cf.exists())
    	{
    		// Attempt import
            BufferedReader reader = null;

            try
            {
                reader = new BufferedReader(new FileReader(cf));
                String text = null;

                // Read a line
                while ((text = reader.readLine()) != null)
                {
                	// Skip if comment
                	if(!text.startsWith("#"))
                	{
                		// Format: admins=-56.50158762045817:12.0:265.4291449731157
                		text = text.replaceAll("\\\\", "");
                		String[] parts = text.split("=");
                		String name = parts[0];
                		String[] coords = parts[1].split(":");
                		Location l = new Location(this.getServer().getWorlds().get(0),
                				Double.parseDouble(coords[0]),
                				Double.parseDouble(coords[1]),
                				Double.parseDouble(coords[2]),
                				0.0f,
                				0.0f);
                		
                		// Set home
                		this.setGroupSpawn(name, l, "ConfigImport");
                		
                		log.info("[SpawnControl] Found group spawn for '"+name+"' at: "+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch());
                	}
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (reader != null)
                    {
                        reader.close();
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
    	}
    }

    
    public boolean isExemptFromCooldowns(Player p, String cooldown)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.CooldownExempt."+cooldown);
    	}
    	
    	return p.isOp();
    }
    
    public long cooldownLeft(Player p, String cooldown)
    {
    	// Check cooldown setting
    	int cooldownAmount = this.getSetting("cooldown_"+cooldown);
    	
    	if(cooldownAmount > 0 && !this.isExemptFromCooldowns(p, cooldown))
    	{
    		// Check cooldown status for player
    		return this.getCooldownRemaining(p, cooldown);
    	}
    	
    	return 0;
    }
    
    public boolean canUseSpawn(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.spawn.use");
    	}
    	
    	return true;
    }
    
    public boolean canUseSetSpawn(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.spawn.set");
    	}
    	
    	return p.isOp();
    }
    
    public boolean canUseSetGroupSpawn(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.groupspawn.set");
    	}
    	
    	// Disabled without group support
    	return false;
    }
    
    public boolean canUseGroupSpawn(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.groupspawn.use");
    	}
    	
    	// Disabled without group support
    	return false;
    }
    
    public boolean canUseHomeBasic(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.home.basic");
    	}
    	
    	return true;
    }
    
    public boolean canUseSetHomeBasic(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.sethome.basic");
    	}
    	
    	return true;
    }
    
    public boolean canUseSetHomeProxy(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.sethome.proxy");
    	}
    	
    	return p.isOp();
    }
    
    public boolean canUseScConfig(Player p)
    {
    	if(this.usePermissions)
    	{
    		return this.permissionHandler.has(p, "SpawnControl.config");
    	}
    	
    	return p.isOp();
    }
}