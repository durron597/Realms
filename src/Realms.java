import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.lang.Integer;

public class Realms extends Plugin
{
	private final RealmsListener listener = new RealmsListener(this);
	private static final Logger log = Logger.getLogger("Minecraft");
	public static final String name = "Realms";
	public static final String version = "v2.2.0.2";
	
	private PropertiesFile config = new PropertiesFile("Realms.txt");
	int wandItem = config.getInt("wandItem", 280);
	int pylonType = config.getInt("pylonType", 7);
	int pylonHeight = config.getInt("pylonHeight", 3);
	int sanctuaryTimeout = config.getInt("sanctuaryTimeout", 1);
	boolean grantbyDefault = config.getBoolean("grantbyDefault", true);
	boolean grantOverrulesDeny = config.getBoolean("grantOverrulesDeny", true);
	boolean debug = config.getBoolean("debug", false);
	
	Server server = etc.getServer();
	public Zone everywhere;
	public List<Zone> zones = new ArrayList<Zone>();
	public List<Wand> wands = new ArrayList<Wand>();
	public Hashtable<Zone,ArrayList<Permission>> zonePermissions = new Hashtable<Zone,ArrayList<Permission>>();
	public Hashtable<String,ArrayList<Zone>> playerZoneList = new Hashtable<String,ArrayList<Zone>>();
	private Thread sanctuaryThread = null;
	
	RealmsData data = new RealmsData(this);

	public Realms() {}
	
	public void log(Level arg0, String arg1, Throwable arg2) {
		if (debug || !arg0.equals(Level.INFO)) log.log(arg0, "[" + name + "] " + arg1, arg2);
	}

	public void log(Level arg0, String arg1) {
		if (debug || !arg0.equals(Level.INFO)) log.log(arg0, "[" + name + "] " + arg1);
	}

	public void loginfo(String arg1) {
		log.log(Level.INFO, "[" + name + "] " + arg1);
	}
	
	@Override
	public void enable() {
		etc.getInstance().addCommand("/realms", "Configures your realms zones");
		etc.getInstance().addCommand("/wand", "Works with the shape and location of realms zones");
		
		zones = new ArrayList<Zone>();
		wands = new ArrayList<Wand>();
		data.initialize();
		initProperties();
		if(everywhere == null) {
			log.log(Level.INFO, "[" + name + "] Mod detected first launch. Creating default permissions.");
			everywhere = new Zone(this, "everywhere", null);
			String adminName = "admins";
			for (String groupName : new String[]{"admins", "admin", "mods", "mod", "moderators", "moderator", "su", "superuser"} ) {
				if(etc.getDataSource().getGroup(groupName).Administrator) {
					adminName = groupName;
					break;
				}
			}
			setPermission("g:" + adminName, Permission.PermType.ALL, everywhere, true, false);
			setPermission("everyone", Permission.PermType.DELEGATE, everywhere, false, false);
			setPermission("everyone", Permission.PermType.ZONING, everywhere, false, false);
		}
		loginfo("[" + name + "] Mod Version " + version + " Enabled.");
		if (debug) log(Level.WARNING, ("[DEBUG] Starting with debug enabled! Don't let your logs get bloated!"));
		
		sanctuaryThread = new Thread(new SanctuaryThread(this, sanctuaryTimeout));
		sanctuaryThread.start();
	}

	@Override
	public void disable() {
		loginfo("Realms Mod Disabled.");
		
		etc.getInstance().removeCommand("/realms");
		etc.getInstance().removeCommand("/wand");
	}

	@Override
	public void initialize() {
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener, this, RealmsListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_USE, listener, this, RealmsListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, RealmsListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DAMAGE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.EXPLODE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.MOB_SPAWN, listener, this, RealmsListener.Priority.MEDIUM);
	}
	
	private void initProperties() {
		config = new PropertiesFile("Realms.txt");
		wandItem = config.getInt("wandItem", 280);
		pylonType = config.getInt("pylonType", 7);
		pylonHeight = config.getInt("pylonHeight", 3);
		sanctuaryTimeout = config.getInt("sanctuaryTimeout", 1);
		grantbyDefault = config.getBoolean("grantbyDefault", true);
		grantOverrulesDeny = config.getBoolean("grantOverrulesDeny", true);
		debug = config.getBoolean("debug", false);
	}


	// REALMS COMMANDS
	public boolean realmsCommand(String[] command, Player player) {	
		String[] newCommands;
		
		if (command[0].equalsIgnoreCase("/realms")) {
			if (command.length == 1) {
				StringBuilder builder = new StringBuilder("Please specify one of: ");
				for(RealmsCommands rc : RealmsCommands.values()) {
					builder.append(rc.getCommandName()).append(" ");
				}
				return playerError(player, builder.toString());
			}
			
			newCommands = command;
		} else {
			String theCommand = command[0].substring(7);
			
			newCommands = new String[command.length+1];
			newCommands[0] = "/realms";
			newCommands[1] = theCommand;
			for (int i = 1; i < command.length; i++) {
				newCommands[i+1] = command[i];
			}
		}
		
		if(argumentCountCheck(2, newCommands, player)) return true;
		
		try {
			RealmsCommands myCommand = RealmsCommands.valueOf(newCommands[1].toUpperCase());
			
			if (!player.canUseCommand(newCommands[0] + newCommands[1])) {
				player.sendMessage(Colors.Rose + "You do not have rights to /realms " + command[1]);	
				return true;
			}
			
			return myCommand.execute(player, newCommands, this);
		} catch (IllegalArgumentException e) {
			playerError(player, newCommands[1] + " is not a valid /realms command!");
			return true;
		}
	}

	public Block stringToBlock(String blockString) {
		String[] block = blockString.split(",");
		int type = Integer.parseInt(block[0]);
		int x = Integer.parseInt(block[1]);
		int y = Integer.parseInt(block[2]);
		int z = Integer.parseInt(block[3]);
		return new Block(type, x, y, z);
	}

	public String blockToString(Block block) {
		String type = Integer.toString(block.getType());
		String x = Integer.toString(block.getX());
		String y = Integer.toString(block.getY());
		String z = Integer.toString(block.getZ());
		return type + "," + x + "," + y + "," + z;
	}

	// Verify that the player gave enough arguments
	public boolean argumentCountCheck(int argumentsRequired, String[] command, Player player) {
		if(command.length < argumentsRequired)
			return playerError(player, "Error: This command requires at least " + argumentsRequired + " arguments");
		return false;
	}

	// Gets a list of all zones that the player is currently contained by
	public ArrayList<Zone> getZones(Zone zone, Player player) {
		ArrayList<Zone> newZoneList = new ArrayList<Zone>(5);
		newZoneList.add(zone);
		for(Zone child : zone.getChildren()) {
			if(child.contains(player)) {
				newZoneList.addAll(getZones(child, player));
				return newZoneList;
			}
		}
		return newZoneList;
	}

	// Finds the smallest zone that the block is contained by
	public Zone getZone(Zone zone, Block block) {
		for(Zone child : zone.getChildren()) if(child.contains(block)) return getZone(child, block);
		return zone;
	}

	// Gets a zone
	public Zone getZoneByName(String name) {
		for(Zone zone : zones) if(zone.getName().equalsIgnoreCase(name)) return zone;
		return null;
	}

	// Delegate check
	public boolean delegateCheck(Player player, Permission.PermType type, Zone zone) {
		if(zone.getParent() != null) if(permissionCheck(player, Permission.PermType.ALL, zone.getParent())) return true;
		if(type.equals(Permission.PermType.DELEGATE)) return permissionCheck(player, Permission.PermType.ALL, zone);
		else return permissionCheck(player, Permission.PermType.DELEGATE, zone) && permissionCheck(player, type, zone);
	}
	
	// Block Permission Check
	public boolean permissionCheck(Player player, Permission.PermType type, Block block) {
		return permissionCheck(player, type, getZone(everywhere, block));
	}

	// General permission check
	public boolean permissionCheck(Player player, Permission.PermType type, Zone zone) {
		Permission result = null;
		
		for(Permission p : zonePermissions.get(zone)) {
			if(p.applicable(player, type)) {
				if(result == null) result = p;
				else result = battle(result,p);
			}
		}
		if(result == null) {
			if(zone.getParent() != null) return permissionCheck(player, type, zone.getParent());
			else return grantbyDefault;
		} else {
			return result.getAllowed();
		}
	}
	
	/*
	 * Battle method
	 */
	public Permission battle(Permission p1, Permission p2) {
		// Override permissions always win
		if(p1.getOverride() && !p2.getOverride()) return p1;
		else if(!p1.getOverride() && p2.getOverride()) return p2;
		// Otherwise, return whichever permission overrules the other
		// If both permissions agree, it doesn't matter which we return
		if(grantOverrulesDeny && p2.getAllowed()) return p2;
		else if(!grantOverrulesDeny && !p2.getAllowed()) return p2;
		else return p1;
	}

	// Overrides previous permission if it existed
	// Otherwise creates new permission
	public void setPermission(String ownerName, Permission.PermType type, Zone zone, boolean allowed, boolean override) {
		Permission previous = getSpecificPermission(ownerName, type, zone);
		if(previous != null) previous.delete();
		new Permission(this, ownerName, type, zone, allowed, override);
	}

	public Permission getSpecificPermission(String ownerName, Permission.PermType type, Zone zone) {
		for(Permission p : zonePermissions.get(zone)) 
			if(p.getOwnerName().equalsIgnoreCase(ownerName) && p.getType().equals(type)) return p;
		return null;
	}

	public void deletePermission(String ownerName, Permission.PermType type, Zone zone) {
		Permission permission = getSpecificPermission(ownerName, type, zone);
		if(permission != null) permission.delete();
	}

	public Wand getPlayerWand(Player player) {
		for(Wand wand : wands) if(wand.player == player) return wand;
		Wand wand = new Wand(this, player);
		wands.add(wand);
		return wand;
	}

	public static boolean playerError(Player player, String message) {
		if(player != null) player.sendMessage(message);
		return true;
	}
}