import java.lang.Math;

public class Point {
	public int x;
	public int y;
	public int z;

	public Point(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean equals(Point p) {
		return x == p.x && y == p.y && z == p.z;
	}

	public boolean equals2D(Point p) {
		return x == p.x && z == p.z;
	}

	public double distance2D(Point p) {
		return Math.sqrt(Math.pow((x-p.x),2)+Math.pow((z-p.z),2));
	}
	
	public static int isLeft2D(Point p1, Point p2, Point p3) {
		return (p2.x - p1.x) * (p3.z - p1.z) - (p3.x - p1.x) * (p2.z - p1.z);
	}
}