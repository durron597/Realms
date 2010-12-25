import java.util.List;
import java.util.logging.Level;

public class SanctuaryThread implements Runnable {
	private Realms realm = null;
	private long sanctuaryTimeout = 1000L;
	

	public SanctuaryThread(Realms realm, int SanctuaryTimeout) {
		this.realm = realm;
		this.sanctuaryTimeout = this.sanctuaryTimeout * 1000L;
	}

	public void run() {
		while (realm != null && realm.isEnabled()) {
			try {
				Thread.sleep(this.sanctuaryTimeout);
			} catch (Exception localException1) {
				break;
			}
			
			List<Mob> mobList = realm.server.getMobList();
			realm.log(Level.INFO, "There are currently " + mobList.size() + " mobs on your server.");
			for (Mob theMob : mobList) {
				if (theMob.isMob()) {
					Block mobLocation = realm.server.getBlockAt((int) Math.floor(theMob.getX()), (int) Math.floor(theMob.getY()), (int) Math.floor(theMob.getZ()));
					Zone myZone = realm.getZone(realm.everywhere, mobLocation);
					
					if (myZone.getSanctuary()) {
						realm.log(Level.INFO, "Trying to kill mob type " + theMob.getName() + " at location " + mobLocation.getX() + "," + mobLocation.getY() + "," + mobLocation.getZ());
						killMob(theMob);
					}
					else if (theMob.getName().equals("Creeper") && !myZone.getCreeper()) killMob(theMob);
				}
			}
		}
	}
	
	private void killMob (Mob theMob) {
		theMob.teleportTo(0, -10, 0, theMob.getRotation(), theMob.getPitch());
		theMob.setHealth(-1);
	}
}
