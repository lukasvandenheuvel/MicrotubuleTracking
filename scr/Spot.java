package MicrotubuleTracking.scr; // not sure what this does

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.TextRoi;
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
		// If no angle could be computed, return NaN as the angle difference.
		double[] stats = this.trajectoryStatistics(spots, numberOfFramesInPast);
		double difference = Double.NaN;
		if (!(Double.isNaN(stats[2]))) {
			double angle = Math.atan2(spot.y - this.y, spot.x - this.x);
			difference = Math.abs(stats[2] - angle);
			difference = Math.min(difference, 2*Math.PI - difference); // deal with case where difference is greater than pi
		}
		return difference/(Math.PI);
	}
	
	public double distanceToPredicted(Spot spot, ArrayList<Spot> spots[], int numberOfFramesInPast) {
		if (this.trace.size() == 0) { // if spot has no trace, speed is not defined
			return Double.NaN;
		}
		double [] v = this.computeSpeed(spots, numberOfFramesInPast);
		double dist = Math.pow(this.x + v[0] - spot.x, 2) + Math.pow(this.y + v[1] - spot.y, 2);
		return dist;
	}
	
	public double[] computeSpeed( ArrayList<Spot> spots[], int numberOfFramesInPast) {
		// return average speed of spot over numberOfFramesInPast past frames.
		double vx = 0;
		double vy = 0;
		numberOfFramesInPast =  Math.min(this.trace.size(), numberOfFramesInPast);
		Spot last = spots[this.t - numberOfFramesInPast]
				.get(this.trace.get(this.trace.size() - numberOfFramesInPast));
		vx = (this.x - last.x) / (double) numberOfFramesInPast;
		vy = (this.y - last.y) / (double) numberOfFramesInPast;
		double[] v = {vx, vy};
		return v;
	}
	
	public double[] trajectoryStatistics(ArrayList<Spot> spots[], int numberOfFramesInPast) {
		// Calculate length, speed and angle of spot trajectory over the past numberOfFramesInPast frames.
		double traceLength = this.trace.size();
		numberOfFramesInPast = Math.min((int)traceLength, numberOfFramesInPast);
		double speed = Double.NaN;
		double theta = Double.NaN;
		
		//track all distances and angles
		ArrayList<Double> distances = new ArrayList<Double>();
		ArrayList<Double> angles = new ArrayList<Double>();
		Spot current = this;
		Spot previous = null;
		// Go back in time and trace back the spot
		for (int b = 1; b <= numberOfFramesInPast; b++) {
			int time = this.t - b;
			int index_previous = this.trace.get(this.trace.size() - b);
			previous = spots[time].get(index_previous);
			distances.add(current.distance(previous));
			angles.add(Math.atan2(current.y - previous.y, current.x - previous.x)); //angle of segment with horizontal
			current = previous;
		}
		//trajectoryLength = distances.size();
		if (numberOfFramesInPast > 0) {
			// The angle is the average of the angles over trajectory
			theta = 0;
			// The speed is the average of the distances
			speed = 0;
			for (int i = 0; i < numberOfFramesInPast; i++) {
				speed += distances.get(i) / numberOfFramesInPast;
				theta += angles.get(i) / numberOfFramesInPast;
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

	public void draw(Overlay overlay, ArrayList<Spot>  spots[]) {
		double xp = x + 0.5;
		double yp = y + 0.5;
		int radius = 5;
		OvalRoi roi = new OvalRoi(xp - radius, yp - radius, 2 * radius, 2 * radius);
		roi.setPosition(t+1); // display roi in one frqme
		roi.setStrokeColor(color);
		roi.setStrokeWidth(1);
		overlay.add(roi);
		// uncomment this part to overlay the angle next to each spot
		Font font = new Font("SansSerif", Font.PLAIN, 6);
		double d = -1;
		if (this.trace.size() > 0){
			d = this.distanceToPredicted(
					spots[this.t-1].get(this.trace.get(this.trace.size()-1)),
					spots, 10);
		}
		TextRoi troi = new TextRoi(xp, yp, ""+ d, font);
		troi.setPosition(t+1); // display roi in one frqme
		troi.setStrokeColor(color);
		troi.setStrokeWidth(1);
		overlay.add(troi);
		
//		if (next != null) {
//			Line line = new Line(x, y, next.x, next.y);
//			line.setStrokeColor(color);
//			line.setStrokeWidth(2);
//			overlay.add(line);
//		}
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
