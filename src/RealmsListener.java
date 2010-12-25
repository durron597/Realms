import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

public class RealmsListener extends PluginListener {

	private Realms realm;
	private Random rand;
	
	
	public RealmsListener(Realms realm) {
		this.realm = realm;

		rand = new Random();
	}

	@Override
	public void onPlayerMove(Player player, Location from, Location to) {
		if(!realm.permissionCheck(player, Permission.PermType.ENTER, realm.server.getBlockAt((int) Math.floor(to.x), (int) Math.floor(to.y), (int) Math.floor(to.z)))) {
			player.sendMessage("You do not have permission to enter that zone!");
			player.teleportTo(from);
		}
		
		playerMessage(player);
		heal(player);
	}

	public void playerMessage(Player player) {
		if(realm.playerZoneList.get(player.getName()) == null) realm.playerZoneList.put(player.getName(), realm.getZones(realm.everywhere, player));
		ArrayList<Zone> oldZoneList = realm.playerZoneList.get(player.getName());
		ArrayList<Zone> newZoneList = realm.getZones(realm.everywhere, player);
		if (oldZoneList.hashCode() != newZoneList.hashCode()) {
			for(Zone zone : oldZoneList) if(!newZoneList.contains(zone)) zone.farewell(player, realm);
			for(Zone zone : newZoneList) if(!oldZoneList.contains(zone)) zone.greet(player, realm);
		}
		realm.playerZoneList.put(player.getName(), newZoneList);
	}
	
	private void heal(Player player) {
		Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ())));
		
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
		realm.log(Level.INFO, String.format("[DEBUG] onBlockPlace: Player-%s; bP-%s; bC-%s; Item-%s", player.getName(),
				blockPlaced, blockClicked, itemInHand.itemType.toString()));
		return !realm.permissionCheck(player, Permission.PermType.CREATE, blockPlaced);
	}

	@Override
	public void onBlockRightClicked(Player player, Block blockClicked, Item item) {
		if(item.getItemId() == realm.wandItem) realm.getPlayerWand(player).wandClick(player, blockClicked);
	}
	
	@Override
	public boolean onItemUse(Player player, Block blockPlaced, Block blockClicked, Item item) {
		realm.log(Level.INFO, String.format("[DEBUG] onItemUse: Player-%s; bP-%s; bC-%s; Item-%s", player.getName(),
				blockPlaced, blockClicked, item.itemType.toString()));
		if (item != null) {
			if (item.itemType == null) {
				realm.log(Level.WARNING, "item wasn't null, but item.ItemType was. getItemId(): " + item.getItemId());
				return false;
			}
			switch (item.itemType) {
			case GoldRecord:
			case GreenRecord:
			case IronDoor:
			case LavaBucket:
			case MilkBucket:
			case Painting:
			case RedStone:
			case Reed: 
			case Saddle:
			case Seeds:
			case Sign:
			case WaterBucket:
			case WoodDoor:
				if (blockPlaced != null) {
					return !realm.permissionCheck(player, Permission.PermType.CREATE, blockPlaced);
				} else if (blockClicked != null) { 
					return !realm.permissionCheck(player, Permission.PermType.CREATE, blockClicked);
				} else {
					// they didn't click on anything anyway, ignore it.
					return false;
				}
			case Bucket:
			case WoodHoe:
			case StoneHoe:
			case IronHoe:
			case DiamondHoe:
			case GoldHoe:
			case FlintAndSteel:
				if (blockPlaced != null) {
					return !realm.permissionCheck(player, Permission.PermType.DESTROY, blockPlaced);
				} else if (blockClicked != null) { 
					return !realm.permissionCheck(player, Permission.PermType.DESTROY, blockClicked);
				} else {
					// they didn't click on anything anyway, ignore it.
					return false;
				}
			default:
				return false;
			}
		}
		return false;
	}
	
	@Override
	public boolean onBlockDestroy(Player player, Block block) {
		realm.log(Level.INFO, String.format("[DEBUG] onBlockDestroy: Player-%s; Block-%s", player.getName(),
				block));
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
					Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) Math.floor(target.getX()), (int) Math.floor(target.getY()), (int) Math.floor(target.getZ())));
					return !zone.getPVP();
				}
				else if ( attacker.isMob() ){
					Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) Math.floor(target.getX()), (int) Math.floor(target.getY()), (int) Math.floor(target.getZ())));
					return zone.getSanctuary();
				}
			}
		}
        return false;
    }
	
	@Override
	public boolean onMobSpawn(Mob mob) {
		boolean rValue;

		Zone zone = realm.getZone(realm.everywhere, realm.server.getBlockAt((int) Math.floor(mob.getX()), (int) Math.floor(mob.getY()), (int) Math.floor(mob.getZ())));
		
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
			Zone zone = realm.getZone(realm.everywhere, block);
			return !zone.getCreeper();
		}
		return false;
    }
}