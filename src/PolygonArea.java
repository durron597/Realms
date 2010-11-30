import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.lang.Math;


public class PolygonArea {
	private Realms realm;
	private Zone zone;
	private ArrayList<Point> vertices = new ArrayList<Point>();
	private ArrayList<Point> workingVertices = new ArrayList<Point>();
	private Polygon polygon = new Polygon();
	private int ceiling;
	private int floor;
	private int workingCeiling;
	private int workingFloor;
	private String mode = "saved";

	// Normal Constructor
	public PolygonArea(Realms realm, Zone zone) {
		this.realm = realm;
		this.zone = zone;
		this.ceiling = 1000;
		this.floor = 0;
	}

	// CSV File Constructor
	public PolygonArea(Realms realm, String[] split) throws Exception
	{
		this.realm = realm;
		this.zone = realm.getZoneByName(split[0]);
		if(zone == null) throw new IOException("Zone provided to contructor is null. Zone name: " + split[0]);
		this.ceiling = Integer.parseInt(split[1]);
		this.floor = Integer.parseInt(split[2]);
		for(int i = 3; i < split.length; i += 3)
			vertices.add(new Point(Integer.parseInt(split[i]),Integer.parseInt(split[i+1]),Integer.parseInt(split[i+2])));
		this.polygon = toPolygon(vertices);
		zone.setPolygon(this);
	}

	public String toString() {
		StringBuffer builder = new StringBuffer();
		builder.append(zone.getName());
		builder.append(",");
		builder.append(ceiling);
		builder.append(",");
		builder.append(floor);
		builder.append(",");
		for(Point vertex : vertices) {
			builder.append(vertex.x);
			builder.append(",");
			builder.append(vertex.y);
			builder.append(",");
			builder.append(vertex.z);
			builder.append(",");
		}
		builder.deleteCharAt(builder.length()-1);
		return builder.toString();
	}

	/*
	 * Accessor Methods
	 */
	public Polygon getPolygon() {return polygon;}
	public Zone getZone() {return zone;}
	public List<Point> getVertices() {return vertices;}
	public boolean isEmpty() {return polygon == null || polygon.npoints < 3;}
	public int getArea() {return calculateArea(vertices);}
	public String getMode() {return mode;}

	/*
	 * Mutator Methods
	 */
	public void setWorkingCeiling(int ceiling) {
		this.workingCeiling = ceiling;
	}
	public void setWorkingFloor(int floor) {
		this.workingFloor = floor;
	}

	public void save() {
		vertices = new ArrayList<Point>(workingVertices);
		workingVertices = new ArrayList<Point>();
		polygon = toPolygon(vertices);
		floor = workingFloor;
		ceiling = workingCeiling;
		this.mode = "saved";
		realm.data.modifyFileLine(RealmsData.polygonFile, zone.getName() + ",", this.toString(), false);
	}

	public List<Point> edit() {
		this.mode = "edit";
		workingFloor = floor;
		workingCeiling = ceiling;
		workingVertices = new ArrayList<Point>(vertices);
		return workingVertices;
	}

	public void cancelEdit() {
		this.mode = "saved";
		this.workingVertices.clear();
	}

	public void delete() {
		this.mode = "deleted";
		realm.data.modifyFileLine(RealmsData.polygonFile, zone.getName() + ",", null, true);
	}

	// Ignores the Y coordinate
	public void removeWorkingVertex(Block block) {
		Iterator<Point> itr = workingVertices.iterator(); 
		while(itr.hasNext()) {
			Point p = itr.next(); 
			if(p.x == block.getX() && p.z == block.getZ()) itr.remove();
		}
	}

	public boolean contains(Player player) {
		Point p = new Point((int)Math.round(player.getLocation().x), (int)Math.round(player.getLocation().y), (int)Math.round(player.getLocation().z));
		return this.contains(p);
	}

	public boolean contains(Block block) {
		Point p = new Point(block.getX(), block.getY(), block.getZ());
		return this.contains(p);
	}

	public boolean contains(Point p) {
		if(polygon.contains(p.x, p.z) && (p.y >= floor) && (p.y <= ceiling)) return true;
		// Check if the point is on any of the lines
		return false;
	}

	public boolean workingVerticesContain(PolygonArea polygonArea) {
		Polygon poly = toPolygon(workingVertices);
		for(Point p : polygonArea.getVertices())
			if(!poly.contains(p.x, p.z) || !(workingFloor <= p.y) || !(p.y <= workingCeiling)) return false;
		return true;
	}

	public boolean containsWorkingVertex(Block block) {
		for (Point p : workingVertices) if(p.x == block.getX() && p.z == block.getZ()) return true;
		return false;
	}


	// Adds the vertex to the working list
	// Returns a list of removed vertices
	public List<Point> addVertex(Block block) {
		List<Point> removed = new ArrayList<Point>();
		Point newVertex = new Point(block.getX(), block.getY(), block.getZ());

		// Case #1: The vertex list has less than three points
		// Just add the point to the end of the working vertices list
		if(workingVertices.size() < 3) {
			workingVertices.add(newVertex);
			return removed;
		}

		// Case #2: Adding the vertex to the end of the working vertices list creates a valid polygon
		// Just add the point to the end of the working vertices list
		workingVertices.add(newVertex);
		if(validPolygon(null)) {
			return removed;
		}

		// Case #3: Adding the vertex to the end of the working vertices list does not create a valid polygon
		// Insert the polygon into place between the two nearest polygons

		// Remove the new vertex from the end of the working vertices list from case #2
		workingVertices.remove(newVertex);
		// Find the two nearest points (p1 & p2)
		Point p1 = null;
		Point p2 = null;
		for (Point p : workingVertices) {
			if(p1 == null || newVertex.distance2D(p1) > newVertex.distance2D(p)) {
				p2 = p1;
				p1 = p;
			} else if(p2 == null || newVertex.distance2D(p2) > newVertex.distance2D(p)) p2 = p;
		}
		if(p1 == null || p2 == null) workingVertices.add(newVertex);

		// Remove working vertices between p1 and p2
		int start = 0;
		int end = 0;
		if(workingVertices.indexOf(p1) > workingVertices.indexOf(p2)) {
			start = workingVertices.indexOf(p2)+1;
			end = workingVertices.indexOf(p1);
		} else {
			start = workingVertices.indexOf(p1)+1;
			end = workingVertices.indexOf(p2);
		}

		for(Point p : workingVertices.subList(start,end)) removed.add(p);
		workingVertices.subList(start,end).clear();


		// Finally, add the new vertex between p1 and p2
		workingVertices.add(start, newVertex);

		return removed;
	}

	// Tests is a new vertex is valid
	public boolean validVertex(Block block, Player player) {
		// The vertex must be contained by the parent zone
		if(!zone.getParent().contains(block)) return !Realms.playerError(player, "Error: Block not contained within " + zone.getParent().getName());
		// The vertex must not be contained by sibling zones
		for(Zone sibling : zone.getParent().getChildren())
			if(sibling != zone && sibling.contains(block)) return !Realms.playerError(player, "Error: Block already claimed by a sibling zone");
		// The vertex must not already be in the vertex list
		if(containsWorkingVertex(block)) return !Realms.playerError(player, "Error: This column of blocks is already in the vertex list.");
		// All checks passed: test vertex is valid
		return true;
	}

	// Do the working vertices make a valid polygon?
	public boolean validPolygon(Player player) {
		// A polygon must have a least three sides
		if(workingVertices.size() < 3) {
			return !Realms.playerError(player, "Error: A polygon must have a least three vertices");
		}
		// The polygon must not intersect any other sibling zones
		for(Zone sibling : zone.getParent().getChildren()) {
			if(sibling != zone && intersects(sibling.getPolygon().getVertices(), workingVertices)) {
				return !Realms.playerError(player, "Error: A block that would be enclosed by this polygon is already claimed by another zone.");
			}
		}
		// The polygon must contain all zone children
		for(Zone child : zone.getChildren()) {
			if(!workingVerticesContain(child.getPolygon())) {
				return !Realms.playerError(player, "Error: New zone boundries do not contain all zone children!");
			}
		}

		// The polygon must not contain intersecting lines
		for(Line line1 : getLines(workingVertices)) {
			for(Line line2 : getLines(workingVertices)) {
				if(line1.intersects2DIgnorePoints(line2)) return !Realms.playerError(player, "Error: Polygon line intersection!!");
			}
		}

		// All checks passed: vertex is valid
		return true;
	}

	// Calculate the area of a polygon defined by a list of points
	public static int calculateArea(List<Point> points) {
		if(points.size() < 3) return 0;
		int areaCalc = 0;
		Point last = points.get(points.size() - 1);
		for(Point p : points) {
			areaCalc += (p.x*last.z)-(last.x*p.z);
			last = p;
		}
		areaCalc = Math.abs(areaCalc / 2);
		return areaCalc;
	}

	// Calculate the volume of a polygon with a ceiling and floor
	public static int calculateVolume(List<Point> points, int floor, int ceiling) {
		int height = ceiling - floor;
		return calculateArea(points) * height;
	}

	// Tests two lists of points for polygon intersection
	// Should probably use ANY-SEGMENTS-INTERSECT for performance, not a big deal tho
	private static boolean intersects(List<Point> list1, List<Point> list2) {
		List<Line> lines1 = getLines(list1);
		List<Line> lines2 = getLines(list2);
		for(Line line1 : lines1) {
			for(Line line2 : lines2) {
				if(line1.intersects2DIgnorePoints(line2)) return true;
			}
		}
		return false;
	}

	// Return all lines made by the list of points
	private static List<Line> getLines(List<Point> points) {
		List<Line> results = new ArrayList<Line>();
		if(points.size() < 2) return results;
		Point last = points.get(points.size() - 1);
		for (Point p : points) {
			results.add(new Line(p,last));
			last = p;
		}
		return results;
	}

	private static Polygon toPolygon(List<Point> points) {
		Polygon poly = new Polygon();
		for(Point p : points) poly.addPoint(p.x,p.z);
		return poly;
	}
}