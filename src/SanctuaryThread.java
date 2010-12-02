import java.util.List;

public class SanctuaryThread implements Runnable {
	private Realms realm = null;

	public SanctuaryThread(Realms realm) {
		this.realm = realm;
	}

	public void run() {
		while (realm.isSanctuaryEnabled()) {
			try {
				Thread.sleep(1000L);
			} catch (Exception localException1) {
				break;
			}
			
			List<LivingEntity> entityList = realm.server.getLivingEntityList();
			for (LivingEntity theEntity : entityList) {
				if (theEntity.isMob()) {
					Mob theMob = (Mob) theEntity;
					Zone myZone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) theEntity.getX(), (int) theEntity.getY(), (int) theEntity.getZ()));
					
					if (myZone.getSanctuary()) killMob(theMob);
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
