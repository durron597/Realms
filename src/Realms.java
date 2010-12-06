import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.lang.Integer;

public class Realms extends Plugin
{
	private final RealmsListener listener = new RealmsListener(this);
	public static final Logger log = Logger.getLogger("Minecraft");
	public static final String name = "Realms";
	public static final String version = "v2.0.2.1";
	private PropertiesFile config = new PropertiesFile("Realms.txt");
	int wandItem = config.getInt("wandItem", 280);
	int pylonType = config.getInt("pylonType", 7);
	int pylonHeight = config.getInt("pylonHeight", 3);
	int sanctuaryTimeout = config.getInt("sanctuaryTimeout", 1);
	boolean grantbyDefault = config.getBoolean("grantbyDefault", true);
	boolean grantOverrulesDeny = config.getBoolean("grantOverrulesDeny", true);
	Server server = etc.getServer();
	public Zone everywhere;
	public List<Zone> zones = new ArrayList<Zone>();
	public List<Wand> wands = new ArrayList<Wand>();
	public Hashtable<Zone,ArrayList<Permission>> zonePermissions = new Hashtable<Zone,ArrayList<Permission>>();
	public Hashtable<Player,ArrayList<Zone>> playerZoneList = new Hashtable<Player,ArrayList<Zone>>();
	private boolean sanctuaryEnabled = true;
	private Thread sanctuaryThread = null;
	
	RealmsData data = new RealmsData(this);

	public enum RealmsCommands {
		GIVEMEPERMISSION ("givemepermission", "") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(player.isInGroup("admins")) theRealms.setPermission(player.getName(), Permission.PermType.ALL, theRealms.everywhere, true, false);
				player.sendMessage("Granted you all permissions to zone 'everywhere'");
				return true;
			}
		},
		DELETEZONE ("deletezone", "<zone name>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(3, command, player)) return true;
				String zoneName = command[2];
				if(zoneName.equalsIgnoreCase("everywhere"))  return playerError(player, "Error: You cannot delete the 'everywhere' zone!");
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(zone.getParent() == null) return playerError(player, "Error: The zone '" + zoneName + "' does not have a parent zone!");
				if(!theRealms.permissionCheck(player, Permission.PermType.ZONING, zone.getParent())) return playerError(player, "Error: You do not have permission to delete this zone!");
				zone.delete();
				player.sendMessage("Deleted " + zoneName + ", moved all children to parent zone.");
				return true;
			}
		},
		SETGREETING ("setgreeting", "<zone> [greeting]") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(3, command, player)) return true;
				String zoneName = command[2];
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(!theRealms.permissionCheck(player, Permission.PermType.MESSAGE, zone)) return playerError(player, "Error: You do not have permission to set the greeting of this zone!");
				String greeting = "";
				for(int i = 3; i < command.length; i++) {
					if(command[i].contains(",")) return playerError(player, "Error: Greetings cannot contain commas!");
					greeting += " " + command[i];
				}
				zone.setGreeting(greeting.trim());
				player.sendMessage("Greeting set to " + greeting.trim());
				return true;
			}
		},
		SETFAREWELL ("setfarewell", "<zone> [greeting]") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(3, command, player)) return true;
				String zoneName = command[2];
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(!theRealms.permissionCheck(player, Permission.PermType.MESSAGE, zone)) return playerError(player, "Error: You do not have permission to set the farewell message of this zone!");
				String farewell = "";
				for(int i = 3; i < command.length; i++) {
					if(command[i].contains(",")) return playerError(player, "Error: Greetings cannot contain commas!");
					farewell += " " + command[i];
				}
				zone.setFarewell(farewell.trim());
				player.sendMessage("Farewell set to " + farewell.trim());
				return true;
			}
		},	
		LIST ("list", "<zone>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(3, command, player)) return true;
				String zoneName = command[2];
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				player.sendMessage("List all permissions attached to " + zoneName);
				for(Permission p : theRealms.zonePermissions.get(zone)) player.sendMessage(p.toString());
				return true;
			}
		},
		DELETE ("delete", "<playername> <permissiontype> <zone>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(5, command, player)) return true;
				String playerName = command[2];
				
				Permission.PermType type = Permission.PermType.getTypeFromString(command[3]);
				if (type.equals(Permission.PermType.NULL)) {
					return playerError(player, "Error: The type '" + command[3] + "' is not valid!");
				}
				
				String zoneName = command[4];
				
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(!theRealms.delegateCheck(player, type, zone))
					return playerError(player, "Error: You do not have permission to delete " + type + " permissions in the zone " + zone.getName());
				theRealms.deletePermission(playerName, type, zone);
				player.sendMessage("Deleted all of " + playerName + "'s permissions to " + type + " in " + zone.getName());
				return true;
			}
		},
		GRANT ("grant", "<playername> <permissiontype> <zone> [override]") {
			
			public boolean execute(Player player, String[] command, Realms theRealms) {
				return this.doGrantDeny(player, command, theRealms);
			}
		},
		DENY ("deny", "<playername> <permissiontype> <zone> [override]") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				return this.doGrantDeny(player, command, theRealms);
			}
		},
		CREATEZONE ("createzone", "<zone> <parentzone>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(4, command, player)) return true;
				String zoneName = command[2];
				String parentZoneName = command[3];
				Zone parentZone = theRealms.getZoneByName(parentZoneName);
				if(parentZone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				for(Zone zone : theRealms.zones) if(zone.getName().equalsIgnoreCase(zoneName))
					return playerError(player, "Error: A zone with the name '" + zoneName + "' already exists!");
				if(!theRealms.permissionCheck(player, Permission.PermType.ZONING, parentZone))
					return playerError(player, "Error: You do not have permission to create zones within " + parentZone.getName());
				if (zoneName.contains(","))
					return playerError(player, "Error: Zone names cannot contain commas!");
				if (zoneName.equalsIgnoreCase("null"))
					return playerError(player, "Error: Zones cannot be named null!");
				
				// Made it past all the checks!
				Zone zone = new Zone(theRealms, zoneName, parentZone);
				theRealms.setPermission(player.getName(), Permission.PermType.ALL, zone, true, true);
				player.sendMessage("Created zone " + zoneName + " within " + parentZoneName);
				player.sendMessage("Use '/wand edit " + zoneName + "' to edit this zone's area.");
				return true;
			}
		},
		PVP ("pvp", "<zone> <on|off|inherit>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(4, command, player)) return true;
				
				String zoneName = command[2];
				
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
					return playerError(player, "Error: You do not have permission set pvp in " + zone.getName());
				
				try {
					Zone.ZoneType theType = Zone.ZoneType.getZoneType(command[3].toUpperCase());
					
					if (theType.equals(Zone.ZoneType.NULL)) {
						return playerError(player, "Error: Invalid combat type!");
					}
					
					if (zone.getName().equals("everywhere") && theType.equals(Zone.ZoneType.INHERIT)) {
						return playerError(player, "Error: You cannot set the type to inherit in zone everywhere!");
					}
					zone.setPVP(theType);
					
					player.sendMessage("Setting pvp to " + theType.toString() + " in zone: " + zoneName);
				} catch (Exception e) {
					return playerError(player, "Error: Invalid pvp mode!");
				}
				
				return true;
			}
		},
		SANCTUARY ("sanctuary", "<zone> <on|off|inherit>") {		
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(4, command, player)) return true;
				
				String zoneName = command[2];
				
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
					return playerError(player, "Error: You do not have permission set sanctuary in " + zone.getName());
				
				try {
					Zone.ZoneType theType = Zone.ZoneType.getZoneType(command[3].toUpperCase());
					
					if (theType.equals(Zone.ZoneType.NULL)) {
						return playerError(player, "Error: Invalid combat type!");
					}
					
					if (zone.getName().equals("everywhere") && theType.equals(Zone.ZoneType.INHERIT)) {
						return playerError(player, "Error: You cannot set the combat type to inherit in zone everywhere!");
					}
					
					zone.setSanctuary(theType);
					
					player.sendMessage("Setting sanctuary to " + theType.toString() + " in zone: " + zoneName);
				} catch (Exception e) {
					return playerError(player, "Error: Invalid sanctuary mode!");
				}
				
				return true;
			}
		},
		CREEPER ("creeper", "<zone> <on|off|inherit>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(4, command, player)) return true;
				
				String zoneName = command[2];
				
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
					return playerError(player, "Error: You do not have permission set sanctuary in " + zone.getName());
				
				try {
					Zone.ZoneType theType = Zone.ZoneType.getZoneType(command[3].toUpperCase());
					
					if (theType.equals(Zone.ZoneType.NULL)) {
						return playerError(player, "Error: Invalid combat type!");
					}
					
					if (zone.getName().equals("everywhere") && theType.equals(Zone.ZoneType.INHERIT)) {
						return playerError(player, "Error: You cannot set the combat type to inherit in zone everywhere!");
					}
					
					zone.setCreeper(theType);
					
					player.sendMessage("Setting creeper to " + theType.toString() + " in zone: " + zoneName);
				} catch (Exception e) {
					return playerError(player, "Error: Invalid creeper mode!");
				}
				
				return true;
			}
		},
		HEALING ("healing", "<zone> <percentage|-1 for inherit>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(4, command, player)) return true;
				
				String zoneName = command[2];
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				
				int newHealing = zone.getAbsoluteHealing();
				
				try {
					newHealing = Integer.parseInt(command[3]);
				} catch (NumberFormatException ex) {
					player.sendMessage("Error: the number was not valid. Please use 0-100 (percentage) or -1 to inherit from parent zone");
				}
				
				if (newHealing < 0 || newHealing > 100) {
					if (newHealing != -1) return playerError(player, "Error: the number was not valid. Please use 0-100 (percentage) or -1 to inherit from parent zone");
					else if (zone.equals(theRealms.everywhere)) return playerError(player, "Error: You cannot set the healing to inherit in zone everywhere!");
					else {
						zone.setHealing(newHealing);
						player.sendMessage("Healing percentage set to inherit in zone " + zoneName);
					}
				} else {
					zone.setHealing(newHealing);
					player.sendMessage("Healing percentage set to " + newHealing + " in zone " + zoneName);
				}
				
				return true;
			}
		},
		COMBAT ("combat", "<zone>") {
			@Override
			public boolean execute(Player player, String[] command, Realms theRealms) {
				if(argumentCountCheck(3, command, player)) return true;
				String zoneName = command[2];
				Zone zone = theRealms.getZoneByName(zoneName);
				if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
				player.sendMessage("List all combat settings attached to " + zoneName);
				player.sendMessage("PVP: " + (zone.getPVP() ? "ON" : "OFF") + (zone.getAbsolutePVP().equals(Zone.ZoneType.INHERIT) ? " (inherited)" : ""));
				player.sendMessage("Sanctuary: " + (zone.getSanctuary() ? "ON" : "OFF") + (zone.getAbsoluteSanctuary().equals(Zone.ZoneType.INHERIT) ? " (inherited)" : ""));
				player.sendMessage("Creeper: " + (zone.getCreeper()  ? "ON" : "OFF") + (zone.getAbsoluteCreeper().equals(Zone.ZoneType.INHERIT) ? " (inherited)" : ""));
				int healing = zone.getHealing();
				player.sendMessage("Healing: " + (healing == 0 ? "OFF" : new StringBuffer().append(healing).append(" percentage").toString()) + (zone.getAbsoluteHealing() == -1 ? " (inherited)" : ""));
				return true;
			}
		},
		INVALID ("", "<command> <arguments>");
		
		private String commandName;
		protected final String usage;
		
		private RealmsCommands(String commandName, String usage) {
			this.commandName = commandName;
			this.usage = usage;
		}

		public String getCommandName() {
			return commandName;
		}

		public boolean execute(Player player, String[] command, Realms theRealms) {
			return playerError(player, "/realms command not understood.");
		}
		
		public String getUsage() {
			return "Usage: /realms " + commandName + " " + usage;
		}
		
		public boolean argumentCountCheck(int argumentsRequired, String[] command, Player player) {
			if(command.length < argumentsRequired)
				return playerError(player, getUsage());
			return false;
		}
		
		protected boolean doGrantDeny(Player player, String[] command, Realms theRealms) {
			if(argumentCountCheck(5, command, player)) return true;
			boolean allowed = command[1].equalsIgnoreCase("grant");
			String playerName = command[2];
			
			Permission.PermType type = Permission.PermType.getTypeFromString(command[3]);
			if (type.equals(Permission.PermType.NULL)) {
				return playerError(player, "Error: The type '" + command[3] + "' is not valid!");
			}
				
			String zoneName = command[4];

			Zone zone = theRealms.getZoneByName(zoneName);
			if(zone == null) return playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.delegateCheck(player, type, zone))
				return playerError(player, "Error: You do not have permission to delegate " + type + " permissions in the zone " + zone.getName());
			boolean override = (command.length == 6 && command[5].equalsIgnoreCase("override"));
			if (playerName.contains(","))
				return playerError(player, "Error: Player names cannot contain commas!");
			
			// Made it past all the checks!
			theRealms.setPermission(playerName, type, zone, allowed, override);
			String p = "";
			if(allowed) p = "Granted ";
			else p = "Denied ";
			player.sendMessage(p + playerName + " " + type + " permission within zone " + command[4]);
			return true;
		}
	}
	
	public Realms() {}

	@Override
	public void enable() {
		zones = new ArrayList<Zone>();
		wands = new ArrayList<Wand>();
		data.initialize();
		if(everywhere == null) {
			log.info("Realms Mod detected first launch. Creating default permissions.");
			everywhere = new Zone(this, "everywhere", null);
			setPermission("admins", Permission.PermType.ALL, everywhere, true, false);
			setPermission("everyone", Permission.PermType.DELEGATE, everywhere, false, false);
			setPermission("everyone", Permission.PermType.ZONING, everywhere, false, false);
		}
		log.info("Realms Mod Version " + version + " Enabled.");
	}

	@Override
	public void disable() {
		log.info("Realms Mod Disabled.");
	}

	@Override
	public void initialize() {
		//Here we add the hook we're going to use. In this case it's the arm swing event.
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_USE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DAMAGE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.EXPLODE, listener, this, RealmsListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.MOB_SPAWN, listener, this, RealmsListener.Priority.CRITICAL);
		
		sanctuaryThread = new Thread(new SanctuaryThread(this, sanctuaryTimeout));
		sanctuaryThread.start();
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

	/**
	 * @param sanctuaryEnabled the sanctuaryEnabled to set
	 */
	public void setSanctuaryEnabled(boolean sanctuaryEnabled) {
		this.sanctuaryEnabled = sanctuaryEnabled;
	}

	/**
	 * @return the sanctuaryEnabled
	 */
	public boolean isSanctuaryEnabled() {
		return sanctuaryEnabled;
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