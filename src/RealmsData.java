import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.logging.Level; 
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class RealmsData {

	protected static final Logger log = Logger.getLogger("Minecraft");
	private Realms realm;
	private List<String> fileList = new ArrayList<String>();
	private static final String storageDirectory = "realms";
	static final String polygonFile = "polygons.csv";
	static final String zoneFile = "zones.csv";
	static final String permissionFile = "permissions.csv";
	static final String blockFile = "blocks.csv";
	public static final String zoneFormat = "name,parent,greeting,farewell";
	public static final String permissionFormat = "player/group,type,zone,allowed,override";
	public static final String polygonFormat = "zone,ceiling,floor,x1,y1,z1,x2,y2,z2,x3,y3,z3";
	private File storage = null;

	public RealmsData(Realms realm) {
		this.realm = realm;
		fileList.add(polygonFile);
		fileList.add(zoneFile);
		fileList.add(permissionFile);
		fileList.add(blockFile);
	}

	public void initialize() {
		createStorageDirectory();
		if (storage != null) {
			moveFiles();
			loadFile(zoneFile, "zone");
			loadFile(polygonFile, "polygon");
			loadFile(permissionFile, "permission");
			loadFile(blockFile, "block");
			
			for (Zone zone : realm.zones) {
				zone.saveZone();
			}
		}
	}

	// Creates file storage directory
	private void createStorageDirectory() {
		storage = new File(storageDirectory);
		
		try {
			if (!storage.exists()) storage.mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while trying to create realms directory: ", e);
		}
		
/*		File plugins = new File("plugins");
		try {
			if(!plugins.exists()) plugins.mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while trying to create plugins directory: ", e);
		}
		storage = new File(plugins, "Realms Files");
		try {
			if(!storage.exists()) storage.mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while trying to create realms directory: ", e);
		}*/
	}

	// Files are now stored in a sub directory. This function moves
	// files from the old location to the new location.
	private void moveFiles() {
		File plugins = new File("plugins");
		File oldSubDir = null;
		
		if (plugins.exists()) oldSubDir = new File(plugins, "Realms Files");
		
		for(String fileName : fileList) {
			File rootFile = new File(fileName);
			File oldSubDirFile = null;
			if (oldSubDir != null)
				oldSubDirFile = new File(oldSubDir, fileName);
			try {
				if(oldSubDirFile != null && oldSubDirFile.exists()) {
					log.info("Moving file from " + oldSubDirFile + " to " + new File(storage, fileName));
					oldSubDirFile.renameTo(new File(storage, fileName));
				} else if(rootFile.exists()) {
					log.info("Moving file from " + rootFile + " to " + new File(storage, fileName));
					rootFile.renameTo(new File(storage, fileName));
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while trying to move file from " + fileName + " to " + new File(storage, fileName), e);
			}
		}
		
		try {
			if (oldSubDir.exists()) oldSubDir.delete();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while trying to delete Realms Files subdirectory", e);
		}
	}

	public void loadFile(String location, String type) {
		try {
			File existanceTest = new File(storage,location);
			if(!existanceTest.exists()) {
				existanceTest.createNewFile();
				BufferedWriter bw = new BufferedWriter(new FileWriter(existanceTest, true));
				if(type.equalsIgnoreCase("zone")) bw.append(zoneFormat);
				if(type.equalsIgnoreCase("permission")) bw.append(permissionFormat);
				if(type.equalsIgnoreCase("polygon")) bw.append(polygonFormat);
				bw.close();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while creating new file at " + new File(storage,location), e);
		}

		try {
			Scanner scanner = new Scanner(new File(storage,location));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if(line.startsWith("#") || line.equals("") || line.startsWith("?")) continue;
				if(line.equals(zoneFormat) || line.equals(permissionFormat) || line.equals(polygonFormat)) continue;
				if(type.equalsIgnoreCase("zone")) new Zone(realm, line.split(","));
				if(type.equalsIgnoreCase("permission")) new Permission(realm, line.split(","));
				if(type.equalsIgnoreCase("polygon")) new PolygonArea(realm, line.split(","));
				if(type.equalsIgnoreCase("block")) readBlock(line);
			}
			scanner.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while reading " + new File(storage,location) + " (Are you sure you formatted it correctly?)", e);
		}

		// If we are reading the blockfile, then clear it!

		if(type.equalsIgnoreCase("block")) {
			try {
				File blockFile = new File(storage,location);
				if(!blockFile.delete()) throw new Exception();
				blockFile.createNewFile();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while deleting " + new File(storage,location), e);
			}
		}
	}


	public void readBlock(String line) {
		String[] split = line.split(",");
		Block block = new Block(Integer.parseInt(split[0]),Integer.parseInt(split[1]),Integer.parseInt(split[2]),Integer.parseInt(split[3]));
		log.info("Realms: Reseting orphaned block: " + realm.blockToString(block));
		realm.server.setBlock(block);
	}

	public String blockToString(Block block) {
		StringBuilder builder = new StringBuilder();
		builder.append(block.getType());
		builder.append(",");
		builder.append(block.getX());
		builder.append(",");
		builder.append(block.getY());
		builder.append(",");
		builder.append(block.getZ());
		return builder.toString();
	}

	public void removeSavedBlock(Block block) {
		modifyFileLine(blockFile, blockToString(block), null, true);
	}

	public void addSavedBlock(Block block) {
		String location = blockFile;
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(storage,location), true));
			bw.append(blockToString(block));
			bw.newLine();
			bw.close();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while writing new block to " + location, ex);
		}
	}

	public void modifyFileLine(String location, String match, String newLine, boolean delete) {
		try {
			boolean matchFound = false;
			ArrayList<String> lines = new ArrayList<String>();
			String readline = new String();
			BufferedReader reader = new BufferedReader(new FileReader(new File(storage,location)));
			while ((readline = reader.readLine()) != null) {
				if(readline.startsWith(match)) {
					matchFound = true;
					if(!delete) lines.add(newLine.toString());
				} else {
					lines.add(readline);
				}
			}
			reader.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(storage,location), false));
			for(String writeline : lines) {
				bw.append(writeline);
				bw.newLine();
			}
			if(!delete && !matchFound) {
				bw.append(newLine);
				bw.newLine();
			}
			bw.close();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while modifying " + match + " in " + location, ex);
		}
	}
} 
