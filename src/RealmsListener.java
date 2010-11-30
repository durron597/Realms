//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.logging.Logger;
//import java.util.logging.FileHandler;

//import org.perf4j.javalog.JavaLogStopWatch;

public class RealmsListener extends PluginListener {

	private Realms realm;
//	private Logger perfLogger;

	public RealmsListener(Realms realm) {
		this.realm = realm;
//		perfLogger = Logger.getLogger("Perf4j");
//		try {
//			perfLogger.addHandler(new FileHandler("realms/realmsPerfLog.log"));
//		} catch (IOException e) {
//			System.err.println("Unable to open perfLog: " + e);
//		}
	}

	public void onPlayerMove(Player player, Location from, Location to) {
//		JavaLogStopWatch theWatch = new JavaLogStopWatch(perfLogger);
		
		if(!realm.permissionCheck(player, Permission.PermType.ENTER, realm.server.getBlockAt((int)to.x, (int)to.y, (int)to.z))) {
			player.sendMessage("You do not have permission to enter that zone!");
			player.teleportTo(from);
		}

//		if(realm.playerZoneList.get(player) == null) realm.playerZoneList.put(player, realm.getZones(realm.everywhere, player));
//		ArrayList<Zone> oldZoneList = realm.playerZoneList.get(player);
//		ArrayList<Zone> newZoneList = realm.getZones(realm.everywhere, player);
//		for(Zone zone : oldZoneList) if(!newZoneList.contains(zone)) zone.farewell(player);
//		for(Zone zone : newZoneList) if(!oldZoneList.contains(zone)) zone.greet(player);
//		realm.playerZoneList.put(player, newZoneList);
		
//		theWatch.stop("onPlayerMove");
	}

	public boolean onCommand(Player player, String[] split) {
		if(split[0].toLowerCase().startsWith("/realms") && player.canUseCommand("/realms")) return realm.realmsCommand(split, player);
		if(split[0].equalsIgnoreCase("/Wand") && player.canUseCommand("/wand")) return realm.getPlayerWand(player).wandCommand(split);
		return false;
	}

	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
		if(itemInHand == realm.wandItem) return realm.getPlayerWand(player).wandClick(player, blockClicked);
		return !realm.permissionCheck(player, Permission.PermType.CREATE, blockPlaced);
	}

	public boolean onBlockDestroy(Player player, Block block) {
		return !realm.permissionCheck(player, Permission.PermType.DESTROY, block);
	}

	public void onDisconnect(Player player) {
		Wand wand = realm.getPlayerWand(player);
		wand.reset();
	}

	public boolean onDamage(BaseEntity attacker, BaseEntity defender){
		if ( defender.isPlayer() ){
			Player target = defender.getPlayer();
			if ( attacker.isPlayer() ){
				Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) target.getX(), (int) target.getY(), (int) target.getZ()));
				return !zone.getPVP();
			}
			else if ( attacker.isMob() ){
				Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int)target.getX(), (int)target.getY(), (int)target.getZ()));
				return zone.getSanctuary();
			}
		}
        return false;
    }
	
	public boolean onMobSpawn(Mob mob) {
		Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) mob.getX(), (int) mob.getY(), (int) mob.getZ()));
		if (mob.getName().equals("Creeper")) {
			return !zone.getCreeper() || zone.getSanctuary();
		} else {
			return zone.getSanctuary();
		}
	}
	
	public boolean onExplode(Block block) {
		if ( block.getStatus()==2 ){
			Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) block.getX(), (int) block.getY(), (int) block.getZ()));
			return !zone.getCreeper();
		}
		return false;
    }
}