package MicrotubuleTracking.scr; // not sure what this does

import java.awt.Color;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.process.ImageProcessor;

public class Spot {
	public int x;
	public int y;
	public int t;
	public ArrayList<Integer> trace;
	private Spot next = null;
	private Color color;

	public Spot(int x, int y, int t) {
		this.x = x;
		this.y = y;
		this.t = t;
		this.trace = new ArrayList<Integer>();
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
	
	public double speedDifference(Spot spot, ArrayList<Spot> spots[], int numberOfFramesInPast) {
		// Calculate difference between the speed of the current spot and the distance to the next spot.
		// If the current spot has no speed, the distance between the 2 spots is returned.
		double difference = this.distance(spot);
		double[] stats = this.trajectoryStatistics(spots, numberOfFramesInPast);
		if (!(Double.isNaN(stats[1]))) {
			difference = Math.abs(stats[1] - this.distance(spot));
		}
		return difference;
	}
	
	public double angleDifference(Spot spot, ArrayList<Spot> spots[], int numberOfFramesInPast) {
		// Calculate difference between the trajectory angle of the current spot and angle with the next spot.
		// If no angle could be computed, return 0 as the angle difference.
		double[] stats = this.trajectoryStatistics(spots, numberOfFramesInPast);
		double difference = 0;
		if (!(Double.isNaN(stats[2]))) {
			double angle = Math.atan2(spot.y - this.y, spot.x - this.x);
			difference = Math.abs(stats[2] - angle);
		}
		return difference;
	}
	
	public double[] trajectoryStatistics(ArrayList<Spot> spots[], int numberOfFramesInPast) {
		// Calculate length, speed and angle of spot trajectory over the past numberOfFramesInPast frames.
		double traceLength = this.trace.size();
		numberOfFramesInPast = Math.min((int)traceLength, numberOfFramesInPast);
		double speed = Double.NaN;
		double theta = Double.NaN;
 
		ArrayList<Double> distances = new ArrayList<Double>();
		Spot current = new Spot(this.x, this.y, this.t);
		Spot previous = null;
		// Go back in time and trace back the spot
		for (int b = 1; b <= numberOfFramesInPast; b++) {
			int time = this.t - b;
			int index_previous = this.trace.get(this.trace.size() - b);
			previous = spots[time].get(index_previous);
			distances.add(current.distance(previous));
			current = previous;
		}
		//trajectoryLength = distances.size();
		if (numberOfFramesInPast > 0) {
			// The angle is measured from this particle to the particle tracked furtherst back in time 
			theta = Math.atan2(this.y - current.y, this.x - current.x);
			// The speed is the average of the distances
			speed = 0;
			for (int i = 0; i < numberOfFramesInPast; i++) {
				speed += distances.get(i) / numberOfFramesInPast;
			}
		}
		// Save statistics
		double[] results = new double[3];
		results[0] = traceLength;
		results[1] = speed;
		results[2] = theta;
		return results;
	}
	
	public boolean isTraceEnd() {
		return (this.next == null);
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
