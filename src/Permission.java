import java.io.IOException;

public class Permission {

	private Realms realm;
	private String owner;
	private PermType type;
	
	public enum PermType {
		DELEGATE ("delegate"),
		ZONING ("zoning"),
		ENTER ("enter"),
		ALL ("all"),
		CREATE ("create"),
		DESTROY ("destroy"),
		TELEPORT ("teleport"),
		MESSAGE ("message"),
		COMBAT ("combat"),
		NULL ("");
		
		private String type;
		
		private PermType (String type) {
			this.type = type;
		}
		
		public String toString() {
			return this.type;
		}
		
		public static PermType getTypeFromString (String myType) {
			PermType rValue = null;
			
			try {
				rValue = PermType.valueOf(myType.toUpperCase());
			} catch (Exception e) {
				return PermType.valueOf("");
			}
			
			return rValue;
		}
	}
	
	private Zone zone;
	private boolean allowed;
	private boolean override;

	public Permission(Realms realm, String owner, PermType type, Zone zone, boolean allowed, boolean override) {
		this.realm = realm;
		this.owner = owner;
		this.type = type;
		this.zone = zone;
		this.allowed = allowed;
		this.override = override;
		realm.zonePermissions.get(zone).add(this);
		realm.data.modifyFileLine(RealmsData.permissionFile, owner + "," + type + "," + zone + ",", this.toString(), false);
	}
	
	public Permission(Realms realm, String owner, String type, Zone zone, boolean allowed, boolean override)
	{
		this(realm, owner, PermType.valueOf(type.toUpperCase()), zone, allowed, override);
	}

	// CSV File Constructor
	public Permission(Realms realm, String[] split) throws Exception
	{
		this.realm = realm;
		this.owner = split[0];
		this.type = PermType.valueOf(split[1].toUpperCase());
		this.zone = realm.getZoneByName(split[2]);
		if(zone == null) throw new IOException("Zone provided to contructor is null. Zone name: " + split[2]);
		this.allowed = Integer.parseInt(split[3]) == 1;
		this.override = Integer.parseInt(split[4]) == 1;
		if(!realm.zonePermissions.get(zone).contains(this)) realm.zonePermissions.get(zone).add(this);
	}

	/*
	 * Accessor Methods
	 */

	public String getOwnerName() {return owner;}
	public PermType getType() {return type;}
	public Zone getZone() {return zone;}
	public boolean getAllowed() {return allowed;}
	public boolean getOverride() {return override;}


	/*
	 * Other Methods
	 */

	// Delete this permission
	public void delete() {
		realm.zonePermissions.get(zone).remove(this);
		realm.data.modifyFileLine(RealmsData.permissionFile, owner + "," + type + "," + zone.getName() + ",", null, true);
	}

	// Is this permission applicable to the player?
	public boolean applicableToPlayer(Player player) {
		if(owner.equalsIgnoreCase("everyone")) return true;
		if(owner.startsWith("p:")) return owner.replaceAll("p:","").equalsIgnoreCase(player.getName());
		if(owner.startsWith("g:")) return player.isInGroup(owner.replaceAll("g:",""));
		if(owner.equalsIgnoreCase(player.getName())) return true;
		if(player.isInGroup(owner)) return true;
		return false;
	}

	// Is this permission applicable to the type?
	public boolean applicableToType(PermType type) {
		if(this.type.equals(PermType.ALL) || this.type.equals(type)) return true;
		else return false;
	}

	// Is this permission applicable to the player & type?
	public boolean applicable(Player player, PermType type) {
		return applicableToPlayer(player) && applicableToType(type);
	}

	// Returns a CSV representation of the permission.
	public String toString() {
		StringBuffer builder = new StringBuffer();
		builder.append(owner);
		builder.append(",");
		builder.append(type);
		builder.append(",");
		builder.append(zone.getName());
		builder.append(",");
		if(allowed) builder.append("1");
		else builder.append("0");
		builder.append(",");
		if(override) builder.append("1");
		else builder.append("0");
		return builder.toString();
	}
}