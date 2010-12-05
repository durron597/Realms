import java.util.ArrayList;
import java.util.Random;

public class RealmsListener extends PluginListener {

	private Realms realm;
	private Random rand;
	
	
	public RealmsListener(Realms realm) {
		this.realm = realm;

		rand = new Random();
	}

	@Override
	public void onPlayerMove(Player player, Location from, Location to) {
		if(!realm.permissionCheck(player, Permission.PermType.ENTER, realm.server.getBlockAt((int)to.x, (int)to.y, (int)to.z))) {
			player.sendMessage("You do not have permission to enter that zone!");
			player.teleportTo(from);
		}
		
//		playerMessage(player);
		heal(player);
	}

	public void playerMessage(Player player) {
		if(realm.playerZoneList.get(player) == null) realm.playerZoneList.put(player, realm.getZones(realm.everywhere, player));
		ArrayList<Zone> oldZoneList = realm.playerZoneList.get(player);
		ArrayList<Zone> newZoneList = realm.getZones(realm.everywhere, player);
		for(Zone zone : oldZoneList) if(!newZoneList.contains(zone)) zone.farewell(player);
		for(Zone zone : newZoneList) if(!oldZoneList.contains(zone)) zone.greet(player);
		realm.playerZoneList.put(player, newZoneList);
	}
	
	private void heal(Player player) {
		Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) player.getX(), (int) player.getY(), (int) player.getZ()));
		
		if (player.getHealth() < 20 && rand.nextInt(500) < zone.getHealing()) player.increaseHealth(1);
	}
	
	@Override
	public boolean onCommand(Player player, String[] split) {
		if(split[0].toLowerCase().startsWith("/realms") && player.canUseCommand("/realms")) return realm.realmsCommand(split, player);
		if(split[0].equalsIgnoreCase("/Wand") && player.canUseCommand("/wand")) return realm.getPlayerWand(player).wandCommand(split);
		return false;
	}
	
	@Override
	public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand) {
		return !realm.permissionCheck(player, Permission.PermType.CREATE, blockPlaced);
	}

	@Override
	public void onBlockRightClicked(Player player, Block blockClicked, Item item) {
		if(item.getItemId() == realm.wandItem) realm.getPlayerWand(player).wandClick(player, blockClicked);
	}
	
	@Override
	public boolean onItemUse(Player player, Block blockPlaced, Block blockClicked, Item item) {
		switch (item.itemType) {
		case Bucket:
		case FlintAndSteel:
		case GoldRecord:
		case GreenRecord:
		case IronDoor:
		case LavaBucket:
		case MilkBucket:
		case Painting:
		case Saddle:
		case Sign:
		case WaterBucket:
		case WoodDoor:
			return !realm.permissionCheck(player, Permission.PermType.CREATE, blockPlaced);
		default:
			return false;
		}
	}
	
	@Override
	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
		return !realm.permissionCheck(player, Permission.PermType.CREATE, blockPlaced);
	}

	@Override
	public boolean onBlockDestroy(Player player, Block block) {
		return !realm.permissionCheck(player, Permission.PermType.DESTROY, block);
	}

	@Override
	public void onDisconnect(Player player) {
		Wand wand = realm.getPlayerWand(player);
		wand.reset();
	}

	@Override
	public boolean onDamage(PluginLoader.DamageType type, BaseEntity attacker, BaseEntity defender, int amount) {
		if (attacker != null && type.equals(PluginLoader.DamageType.ENTITY)) {
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
		}
        return false;
    }
	
	@Override
	public boolean onMobSpawn(Mob mob) {
		boolean rValue;

		Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) mob.getX(), (int) mob.getY(), (int) mob.getZ()));
		
		if (mob.getName().equals("Creeper")) {
			rValue = !zone.getCreeper() || zone.getSanctuary();
		} else {
			rValue = zone.getSanctuary();
		}

		return rValue;
	}
	
	@Override
	public boolean onExplode(Block block) {
		if ( block.getStatus()==2 ){
			Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) block.getX(), (int) block.getY(), (int) block.getZ()));
			return !zone.getCreeper();
		}
		return false;
    }
}