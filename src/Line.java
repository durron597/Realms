import java.awt.geom.Line2D;

public class Line {
	public Point p1;
	public Point p2;

	public Line(Point p1, Point p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public boolean intersects2DIgnorePoints(Line line) {
		if(p1.equals2D(line.p1) || p1.equals2D(line.p2) || p2.equals2D(line.p1) || p2.equals2D(line.p2)) return false;
		else return new Line2D.Double(p1.x, p1.z, p2.x, p2.z).intersectsLine(new Line2D.Double(line.p1.x, line.p1.z, line.p2.x, line.p2.z));
	}
}