public enum RealmsCommands {
	GIVEMEPERMISSION ("givemepermission", "") {
		@Override
		public boolean execute(Player player, String[] command, Realms theRealms) {
			if(player.isAdmin()) theRealms.setPermission(player.getName(), Permission.PermType.ALL, theRealms.everywhere, true, false);
			player.sendMessage("Granted you all permissions to zone 'everywhere'");
			return true;
		}
	},
	DELETEZONE ("deletezone", "<zone name>") {
		@Override
		public boolean execute(Player player, String[] command, Realms theRealms) {
			if(argumentCountCheck(3, command, player)) return true;
			String zoneName = command[2];
			if(zoneName.equalsIgnoreCase("everywhere"))  return Realms.playerError(player, "Error: You cannot delete the 'everywhere' zone!");
			Zone zone = theRealms.getZoneByName(zoneName);
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(zone.getParent() == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' does not have a parent zone!");
			if(!theRealms.permissionCheck(player, Permission.PermType.ZONING, zone.getParent())) return Realms.playerError(player, "Error: You do not have permission to delete this zone!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.permissionCheck(player, Permission.PermType.MESSAGE, zone)) return Realms.playerError(player, "Error: You do not have permission to set the greeting of this zone!");
			String greeting = "";
			for(int i = 3; i < command.length; i++) {
				if(command[i].contains(",")) return Realms.playerError(player, "Error: Greetings cannot contain commas!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.permissionCheck(player, Permission.PermType.MESSAGE, zone)) return Realms.playerError(player, "Error: You do not have permission to set the farewell message of this zone!");
			String farewell = "";
			for(int i = 3; i < command.length; i++) {
				if(command[i].contains(",")) return Realms.playerError(player, "Error: Greetings cannot contain commas!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
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
				return Realms.playerError(player, "Error: The type '" + command[3] + "' is not valid!");
			}
			
			String zoneName = command[4];
			
			Zone zone = theRealms.getZoneByName(zoneName);
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.delegateCheck(player, type, zone))
				return Realms.playerError(player, "Error: You do not have permission to delete " + type + " permissions in the zone " + zone.getName());
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
			if(parentZone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			for(Zone zone : theRealms.zones) if(zone.getName().equalsIgnoreCase(zoneName))
				return Realms.playerError(player, "Error: A zone with the name '" + zoneName + "' already exists!");
			if(!theRealms.permissionCheck(player, Permission.PermType.ZONING, parentZone))
				return Realms.playerError(player, "Error: You do not have permission to create zones within " + parentZone.getName());
			if (zoneName.contains(","))
				return Realms.playerError(player, "Error: Zone names cannot contain commas!");
			if (zoneName.equalsIgnoreCase("null"))
				return Realms.playerError(player, "Error: Zones cannot be named null!");
			
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
				return Realms.playerError(player, "Error: You do not have permission set pvp in " + zone.getName());
			
			try {
				Zone.ZoneType theType = Zone.ZoneType.getZoneType(command[3].toUpperCase());
				
				if (theType.equals(Zone.ZoneType.NULL)) {
					return Realms.playerError(player, "Error: Invalid combat type!");
				}
				
				if (zone.getName().equals("everywhere") && theType.equals(Zone.ZoneType.INHERIT)) {
					return Realms.playerError(player, "Error: You cannot set the type to inherit in zone everywhere!");
				}
				zone.setPVP(theType);
				
				player.sendMessage("Setting pvp to " + theType.toString() + " in zone: " + zoneName);
			} catch (Exception e) {
				return Realms.playerError(player, "Error: Invalid pvp mode!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
				return Realms.playerError(player, "Error: You do not have permission set sanctuary in " + zone.getName());
			
			try {
				Zone.ZoneType theType = Zone.ZoneType.getZoneType(command[3].toUpperCase());
				
				if (theType.equals(Zone.ZoneType.NULL)) {
					return Realms.playerError(player, "Error: Invalid combat type!");
				}
				
				if (zone.getName().equals("everywhere") && theType.equals(Zone.ZoneType.INHERIT)) {
					return Realms.playerError(player, "Error: You cannot set the combat type to inherit in zone everywhere!");
				}
				
				zone.setSanctuary(theType);
				
				player.sendMessage("Setting sanctuary to " + theType.toString() + " in zone: " + zoneName);
			} catch (Exception e) {
				return Realms.playerError(player, "Error: Invalid sanctuary mode!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
				return Realms.playerError(player, "Error: You do not have permission set sanctuary in " + zone.getName());
			
			try {
				Zone.ZoneType theType = Zone.ZoneType.getZoneType(command[3].toUpperCase());
				
				if (theType.equals(Zone.ZoneType.NULL)) {
					return Realms.playerError(player, "Error: Invalid combat type!");
				}
				
				if (zone.getName().equals("everywhere") && theType.equals(Zone.ZoneType.INHERIT)) {
					return Realms.playerError(player, "Error: You cannot set the combat type to inherit in zone everywhere!");
				}
				
				zone.setCreeper(theType);
				
				player.sendMessage("Setting creeper to " + theType.toString() + " in zone: " + zoneName);
			} catch (Exception e) {
				return Realms.playerError(player, "Error: Invalid creeper mode!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
			
	        if(!theRealms.permissionCheck(player, Permission.PermType.COMBAT, zone))
	                return Realms.playerError(player, "Error: You do not have permission set healing in " + zone.getName());
			
			int newHealing = zone.getAbsoluteHealing();
			
			try {
				newHealing = Integer.parseInt(command[3]);
			} catch (NumberFormatException ex) {
				player.sendMessage("Error: the number was not valid. Please use 0-100 (percentage) or -1 to inherit from parent zone");
			}
			
			if (newHealing < 0 || newHealing > 100) {
				if (newHealing != -1) return Realms.playerError(player, "Error: the number was not valid. Please use 0-100 (percentage) or -1 to inherit from parent zone");
				else if (zone.equals(theRealms.everywhere)) return Realms.playerError(player, "Error: You cannot set the healing to inherit in zone everywhere!");
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
			if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
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
		return Realms.playerError(player, "/realms command not understood.");
	}
	
	public String getUsage() {
		return "Usage: /realms " + commandName + " " + usage;
	}
	
	public boolean argumentCountCheck(int argumentsRequired, String[] command, Player player) {
		if(command.length < argumentsRequired)
			return Realms.playerError(player, getUsage());
		return false;
	}
	
	protected boolean doGrantDeny(Player player, String[] command, Realms theRealms) {
		if(argumentCountCheck(5, command, player)) return true;
		boolean allowed = command[1].equalsIgnoreCase("grant");
		String playerName = command[2];
		
		Permission.PermType type = Permission.PermType.getTypeFromString(command[3]);
		if (type.equals(Permission.PermType.NULL)) {
			return Realms.playerError(player, "Error: The type '" + command[3] + "' is not valid!");
		}
			
		String zoneName = command[4];

		Zone zone = theRealms.getZoneByName(zoneName);
		if(zone == null) return Realms.playerError(player, "Error: The zone '" + zoneName + "' could not be found!");
		if(!theRealms.delegateCheck(player, type, zone))
			return Realms.playerError(player, "Error: You do not have permission to delegate " + type + " permissions in the zone " + zone.getName());
		boolean override = (command.length == 6 && command[5].equalsIgnoreCase("override"));
		if (playerName.contains(","))
			return Realms.playerError(player, "Error: Player names cannot contain commas!");
		
		// Made it past all the checks!
		theRealms.setPermission(playerName, type, zone, allowed, override);
		String p = "";
		if(allowed) p = "Granted ";
		else p = "Denied ";
		player.sendMessage(p + playerName + " " + type + " permission within zone " + command[4]);
		return true;
	}
}