import java.util.ArrayList;
import java.util.List;

public class Zone {

	private Realms realm;
	private String name;
	private Zone parent;
	private String greeting;
	private String farewell;
	private PolygonArea polygon;
	private ZoneType pvp; // OFF = pvp disabled, ON = pvp enabled
	private ZoneType sanctuary; // OFF = zone is not a sanctuary, ON = zone is a sanctuary
	private ZoneType creeper; // OFF = creepers may not explode, ON = creepers may explode
	private int healing;
	
	public enum ZoneType {
		ON (true), OFF (false), INHERIT (false), NULL (false);
		
		private boolean value;
		
		private ZoneType (boolean value) {
			this.value = value;
		}
		
		public boolean getValue() {
			return value;
		}
		
		public static ZoneType getZoneType(String type) {
			try {
				return ZoneType.valueOf(type.toUpperCase());
			} catch (Exception e) {
				return ZoneType.NULL;
			}
		}
	}
	
	private List<Zone> children = new ArrayList<Zone>();

	// Regular Constructor
	public Zone(Realms realm, String name, Zone parent)
	{
		this.realm = realm;
		this.name = name;
		this.parent = parent;
		this.polygon = new PolygonArea(realm, this);
		this.children = new ArrayList<Zone>();
		this.greeting = null;
		this.farewell = null;

		setDefaults(parent == null);

		if(parent != null && !parent.getChildren().contains(this)) parent.children.add(this);
		if(name.equalsIgnoreCase("everywhere")) realm.everywhere = this;
		realm.zones.add(this);
		realm.zonePermissions.put(this, new ArrayList<Permission>());
		this.saveZoneToFile();
		Realms.log.info("Zone created: " + name);
	}

	// CSV File Constructor
	public Zone(Realms realm, String[] split)
	{
		this.realm = realm;
		this.name = split[0];
		if(split.length < 2 || split[1] == null || split[1].equalsIgnoreCase("null")) this.parent = null;
			else this.parent = realm.getZoneByName(split[1]);
		if(split.length < 3 || split[2] == null || split[2].equalsIgnoreCase("null")) this.greeting = null;
			else this.greeting = split[2];
		if(split.length < 4 || split[3] == null || split[3].equalsIgnoreCase("null")) this.farewell = null;
			else this.farewell = split[3];
		
		if(split.length < 5) {
			setDefaults(name.equals("everywhere")); 
		} else {
			try {
				this.pvp = ZoneType.getZoneType(split[4]);
				if (this.pvp.equals(ZoneType.NULL)) {
					this.pvp = ZoneType.INHERIT;
				}
				this.sanctuary = ZoneType.getZoneType(split[5]);
				if (this.sanctuary.equals(ZoneType.NULL)) {
					this.sanctuary = ZoneType.INHERIT;
				}
				this.creeper = ZoneType.getZoneType(split[6]);
				if (this.creeper.equals(ZoneType.NULL)) {
					this.sanctuary = ZoneType.INHERIT;
				}
				if (split[7] == null) {
					this.healing = -1;
				} else {
					try {
						this.healing = Integer.parseInt(split[7]);
					} catch (NumberFormatException e) {
						this.healing = -1;
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				setDefaults(name.equals("everywhere"));
			}
		}
		
		
		this.polygon = new PolygonArea(realm, this);
		this.children = new ArrayList<Zone>();
		if(parent != null && !parent.getChildren().contains(this)) parent.children.add(this);
		if(name.equalsIgnoreCase("everywhere")) realm.everywhere = this;
		realm.zones.add(this);
		realm.zonePermissions.put(this, new ArrayList<Permission>());
	}

	private void setDefaults(boolean isEverywhere) {
		if (isEverywhere) {
			this.pvp = ZoneType.OFF;
			this.sanctuary = ZoneType.OFF;
			this.creeper = ZoneType.OFF;
			this.healing = 0;
		} else {
			this.pvp = ZoneType.INHERIT;
			this.sanctuary = ZoneType.ON;
			this.creeper = ZoneType.INHERIT;
			this.healing = -1;
		}
		
	}
	
	/*
	 * Accessor Methods
	 */

	public String getName() {return name;}
	public Zone getParent() {return parent;}
	public String getGreeting() {return greeting;}
	public String getFarewell() {return farewell;}
	public List<Zone> getChildren() {return children;}
	public PolygonArea getPolygon() {return polygon;}
	
	public ZoneType getAbsolutePVP() {return pvp;}
	public ZoneType getAbsoluteSanctuary() {return sanctuary;}
	public ZoneType getAbsoluteCreeper() {return creeper;}
	public int getAbsoluteHealing() {return healing;}

	public boolean getPVP() {
		if (this.pvp.equals(ZoneType.INHERIT) && this.parent != null) {
			return parent.getPVP();
		} else {
			return this.pvp.getValue();
		}
	}
	
	public boolean getSanctuary() {
		if (this.sanctuary.equals(ZoneType.INHERIT) && this.parent != null) {
			return parent.getSanctuary();
		} else {
			return this.sanctuary.getValue();
		}
	}
	
	public boolean getCreeper() {
		if (this.creeper.equals(ZoneType.INHERIT) && this.parent != null) {
			return parent.getCreeper();
		} else {
			return this.creeper.getValue();
		}
	}
	
	public int getHealing() {
		if (this.healing == -1 && this.parent != null) {
			return parent.getHealing();
		} else {
			return this.healing;
		}
	}
	
	/*
	 * Mutator Methods
	 */

	public void removeChild(Zone child) {
		children.remove(child);
		saveZone();
	}

	public void setPolygon(PolygonArea polygon) {
		this.polygon = polygon;
	}

	public void setParent(Zone newParent) {
		parent = newParent;
		saveZone();
	}

	public void setGreeting(String greeting) {
		this.greeting = greeting;
		saveZone();
	}

	public void setFarewell(String farewell) {
		this.farewell = farewell;
		saveZone();
	}
	
	public void setPVP(ZoneType pvp) {
		this.pvp = pvp;
		saveZone();
	}
	
	public void setSanctuary(ZoneType sanctuary) {
		this.sanctuary = sanctuary;
		saveZone();
	}
	
	public void setCreeper(ZoneType creeper) {
		this.creeper = creeper;
		saveZone();
	}
	
	public void setHealing(int healing) {
		this.healing = healing;
		saveZone();
	}

	/*
	 * Other Methods
	 */
	public void farewell(Player player) {
		if(farewell != null) player.sendMessage(farewell);
	}

	public void greet(Player player) {
		if(greeting != null) player.sendMessage(greeting);
	}

	// Save the zone after changes
	public void saveZone() {
		this.saveZoneToFile();
	}

	// Write the zone to file
	public void saveZoneToFile() {
		realm.data.modifyFileLine(RealmsData.zoneFile, this.name + ",", this.toString(), false);
		if(!this.isEmpty()) realm.data.modifyFileLine(RealmsData.polygonFile, this.name + ",", polygon.toString(), false);
	}

	// Delete the zone
	public void delete() {
		// Delete polygon
		polygon.delete();

		// Delete all zone permissions!
		@SuppressWarnings("unchecked")
		List<Permission> permissionList = (List<Permission>) realm.zonePermissions.get(this).clone();
		for(Permission p : permissionList) p.delete();
		if(parent != null) parent.removeChild(this);
		for(Zone child : getChildren()) if(parent != null) child.setParent(parent);
		for(ArrayList<Zone> zoneList : realm.playerZoneList.values()) zoneList.remove(this);
		realm.data.modifyFileLine(RealmsData.zoneFile, this.name + ",", null, true);
		realm.zones.remove(this);
	}

	// Does this zone contain zero area?
	public boolean isEmpty() {
		return polygon == null || polygon.isEmpty();
	}

	public boolean contains(Block block) {
		if(name.equalsIgnoreCase("everywhere")) return true;
		return polygon.contains(block);
	}

	public boolean contains(Player player) {
		if(name.equalsIgnoreCase("everywhere")) return true;
		return polygon.contains(player);
	}

	public Zone whichChildContains(Block block) {
		for(Zone child : children) if(child.contains(block)) return child.whichChildContains(block);
		return this;
	}


	public String toString() {
		StringBuffer builder = new StringBuffer();
		builder.append(name);
		builder.append(',');
		if(parent == null) builder.append("null");
			else builder.append(parent.name);
		builder.append(',');
		if(greeting == null) builder.append("null");
			else builder.append(greeting);
		builder.append(',');
		if(farewell == null) builder.append("null");
			else builder.append(farewell);
		builder.append(',');
		builder.append(pvp.toString());
		builder.append(',');
		builder.append(sanctuary.toString());
		builder.append(',');
		builder.append(creeper.toString());
		builder.append(',');
		builder.append(healing);
		return builder.toString();
	}

}