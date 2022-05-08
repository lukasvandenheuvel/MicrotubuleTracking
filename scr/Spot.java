import java.awt.Color;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.process.ImageProcessor;

public class Spot {
	public int x;
	public int y;
	public int t;
	private Spot next = null;
	private Color color;

	public Spot(int x, int y, int t) {
		this.x = x;
		this.y = y;
		this.t = t;
		color = Color.getHSBColor((float)Math.random(), 1f, 1f);
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 120);
	}

	public double distance(Spot spot) {
		double dx = x - spot.x;
		double dy = y - spot.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public double closestDistance(ArrayList<Spot> spotList) {
		double minDistance = Double.MAX_VALUE;
		for (Spot spot : spotList) {
			if (this.distance(spot) < minDistance)
				minDistance = this.distance(spot);
		}
		return minDistance;
	}
	
	public double intensityDifference(Spot spot, ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		return Math.abs(ip.getPixelValue(x,y) - ip.getPixelValue(spot.x,spot.y));
	}
	
	public double speed(ArrayList<Spot> spots[], int numberOfFramesInPast) {
		// Calculate speed of spot
		numberOfFramesInPast = Math.min(this.t, numberOfFramesInPast);
		ArrayList<Double> distances = new ArrayList<Double>();
		int currentTime = this.t;
		Spot current = new Spot(this.x, this.y, this.t);
		// Go back in time and trace back the spot
		for (int b = 1; b <= numberOfFramesInPast; b++) {
			int time = currentTime - b;
			Spot previous = null;
			// Find previous spot
			for (Spot spot: spots[time]) {
				if (!(spot.next == null)) {
					if ((spot.next.x == current.x) && (spot.next.y == current.y)) {
						previous = spot;
						break;
					}
				}
			}
			if (previous == null)
				break;
			distances.add(current.distance(previous));
			current = previous;
		}
		// The speed is the average of the distances
		double speed = 0;
		for (int i = 0; i < distances.size(); i++) {
			speed += distances.get(i) / distances.size();
		}
		return speed;
	}
	
	public void giveColor(Spot a) {
		this.color = a.color;
	}

	public void draw(Overlay overlay) {
		double xp = x + 0.5;
		double yp = y + 0.5;
		int radius = 5;
		OvalRoi roi = new OvalRoi(xp - radius, yp - radius, 2 * radius, 2 * radius);
		roi.setPosition(t+1); // display roi in one frqme
		roi.setStrokeColor(color);
		roi.setStrokeWidth(1);
		overlay.add(roi);
		if (next != null) {
			Line line = new Line(x, y, next.x, next.y);
			line.setStrokeColor(color);
			line.setStrokeWidth(2);
			overlay.add(line);
		}
	}
	
	public void link(Spot a) {
		if (a == null)
			return;
		a.next = this;
		a.color = this.color;
	}
	
	public String toString() {
		return "(" + x + ", " + y + ", " + t + ")";
	}
}
