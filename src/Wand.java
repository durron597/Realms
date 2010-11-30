import java.util.*;

public class Wand {
	int pylonType;
	int pylonHeight;

	// Dynamically generated
	Realms realm;
	// Read from file
	Player player;
	String mode = "default";
	PolygonArea workingPolygon;
	private List<Block> savedBlocks = new ArrayList<Block>();

	public Wand(Realms realm, Player player)
	{
		this.realm = realm;
		this.pylonType = realm.pylonType;
		this.pylonHeight = realm.pylonHeight;
		this.player = player;
	}

	// Reset wand to default mode
	public void reset() {
		if(!mode.equalsIgnoreCase("default")) {
			workingPolygon.cancelEdit();
			workingPolygon = null;
			mode = "default";
		}
		resetAllSavedBlocks();
	}

	// Reset all saved blocks in the column x,z
	public void removePylon(int x, int z) {
		Iterator<Block> itr = savedBlocks.iterator(); 
		while(itr.hasNext()) {
			Block savedBlock = itr.next();
			if(savedBlock.getX() == x && savedBlock.getZ() == z) {
				realm.server.setBlock(savedBlock);
				realm.data.removeSavedBlock(savedBlock);
				itr.remove();
			}
		}
	}

	// Resets all saved blocks
	public void resetAllSavedBlocks() {
		for(Block block : savedBlocks) {
			realm.server.setBlock(block);
			realm.data.removeSavedBlock(block);
		}
		savedBlocks = new ArrayList<Block>();
	}

	// Add saved block
	public void addSavedBlock(Block block) {
		savedBlocks.add(block);
		realm.data.addSavedBlock(block);
	}

	// Creates a pylon above the specified block.
	// Saves the original blocks into wand's savedBlocks list
	public void createPylon(Block block) {
		for (int i = 0; i < pylonHeight; i++) {
			block = realm.server.getBlockAt(block.getX(), block.getY()+1, block.getZ());
			addSavedBlock(block);
			realm.server.setBlockAt(pylonType, block.getX(), block.getY(), block.getZ());
		}
	}

	// WAND COMMANDS
	public boolean wandCommand(String[] command) {
		if(command.length < 2) {
			player.sendMessage("No /wand subcommand provided!");
			return true;
		}

		// Cancel operation
		if(command[1].equalsIgnoreCase("cancel")) {
			this.reset();
			player.sendMessage("Your wand has been reset.");
			return true;
		}

		// Save vertices
		if (command[1].equalsIgnoreCase("save")) {
			if(!mode.equalsIgnoreCase("default")) {
				if(!workingPolygon.validPolygon(player)) return true;
				workingPolygon.save();
				reset();
				player.sendMessage("Zone complete! Wand back in getInfo mode.");
				return true;
			}
			return Realms.playerError(player, "Error: You are not currently editing a zone");
		}

		// Edit zone
		if(command[1].equalsIgnoreCase("edit")) {

			// Wand must be in default mode
			if(!mode.equalsIgnoreCase("default")) return Realms.playerError(player, "Error: You must finish what you are doing first (or cancel it)");

			// Zone name must be provided
			if(command.length < 3) return Realms.playerError(player, "No zone name provided");

			// Cannot edit the "everywhere" zone!
			if(command[2].equalsIgnoreCase("everywhere")) return Realms.playerError(player, "You cannot edit the everywhere zone!");

			// Get zone
			Zone zone = realm.getZoneByName(command[2]);
			if(zone == null) return Realms.playerError(player, "Zone '" + command[2] + "' could not be found");

			// Zone must be in "saved" mode
			if(!zone.getPolygon().getMode().equalsIgnoreCase("saved")) return Realms.playerError(player, "That zone is already being edited!");

			if(zone.getParent() == null) return Realms.playerError(player, "Error: The zone '" + command[2] + "' does not have a parent zone!");

			// Player must have zoning permission
			if(!realm.permissionCheck(player, Permission.PermType.ZONING, zone.getParent()))
				return Realms.playerError(player, "You do not have permission to edit or create zones within " + zone.getParent().getName() + "!");

			// Passed all checks!
			mode = "polygon";
			workingPolygon = zone.getPolygon();
			List<Point> oldVertices = workingPolygon.edit();
			for(Point p : oldVertices) createPylon(realm.server.getBlockAt(p.x,p.y,p.z));
			player.sendMessage("Your wand is now ready to edit " + zone.getName());
			return true;
		}

		// setFloor
		if(command[1].equalsIgnoreCase("setfloor") && mode.equalsIgnoreCase("polygon")) {
			if(command.length == 3) {
				try {
					int floor = Integer.parseInt(command[2]);
					workingPolygon.setWorkingFloor(floor);
					player.sendMessage("Set zone " + workingPolygon.getZone().getName() + " floor to " + floor);
					return true;
				} catch(Exception e) {
					return Realms.playerError(player, "Could not understand floor value: " + command[2]);
				}
			} else {
				mode = "setFloor";
				player.sendMessage("Your wand is now in set zone floor mode for zone " + workingPolygon.getZone().getName());
				return true;
			}
		}

		// setCeiling
		if(command[1].equalsIgnoreCase("setceiling") && mode.equalsIgnoreCase("polygon")) {
			if(command.length == 3) {
				try {
					int ceiling = Integer.parseInt(command[2]);
					workingPolygon.setWorkingCeiling(ceiling);
					player.sendMessage("Set zone " + workingPolygon.getZone().getName() + " ceiling to " + ceiling);
					return true;
				} catch(Exception e) {
					return Realms.playerError(player, "Could not understand ceiling value: " + command[2]);
				}
			} else {
				mode = "setCeiling";
				player.sendMessage("Your wand is now in set zone ceiling mode for zone " + workingPolygon.getZone().getName());
				return true;
			}
		}

		// None of the above
		player.sendMessage("/wand command not understood!");
		return true;
	}

	// WAND CLICK

	public boolean wandClick(Player player, Block block) {

		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		// By default wand is in get info mode
		if(mode.equalsIgnoreCase("default")) {
			for(Zone zone : realm.zones) if(zone.contains(block)) {
				player.sendMessage("Block belongs to " + zone.whichChildContains(block).getName());
				return true;
			}
			player.sendMessage("This block does not belong to any zones. Weird, this shouldn't ever happen!");
			return true;
		}

		// workingPolygon must not be null for remaining wand actions
		if(workingPolygon == null) {
			player.sendMessage("Wand has no zone selected! Reseting wand.");
			reset();
			return true;
		}

		if(mode.equalsIgnoreCase("setCeiling")) {
			workingPolygon.setWorkingCeiling(y);
			mode = "polygon";
			player.sendMessage("Set zone " + workingPolygon.getZone().getName() + " ceiling to " + y);
			player.sendMessage("Wand mode set to define " + workingPolygon.getZone().getName());
			return true;
		}

		if(mode.equalsIgnoreCase("setFloor")) {
			workingPolygon.setWorkingFloor(y);
			mode = "polygon";
			player.sendMessage("Set zone " + workingPolygon.getZone().getName() + " floor to " + y);
			player.sendMessage("Wand mode set to define " + workingPolygon.getZone().getName());
			return true;
		}

		if(mode.equalsIgnoreCase("polygon")) {
			// Remove last vertex
			if(workingPolygon.containsWorkingVertex(block)) {
				workingPolygon.removeWorkingVertex(block);
				removePylon(block.getX(), block.getZ());
				player.sendMessage("Vertex " + x + "," + z + " removed from zone " + workingPolygon.getZone().getName());
				return true;
			}
			// The vertex must be valid
			if(!workingPolygon.validVertex(block, player)) return true;

			List<Point> removedVertices = workingPolygon.addVertex(block);
			for(Point p : removedVertices) {
				player.sendMessage("Removing vertex at " + p.x + "," + p.y + "," + p.z);
				removePylon(p.x, p.z);
			}
			createPylon(block);
			player.sendMessage("Vertex " + x + "," + z + " added to zone " + workingPolygon.getZone().getName());
			return true;
		}

		player.sendMessage("Wand mode invalid. Reseting wand now.");
		reset();
		return true;
	}

}