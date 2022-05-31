import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Track_Microtubules implements PlugIn {
	
	public void run(String arg) {
		
			// Import classes for denoising and detection
			Denoiser denoiser = new Denoiser();
			SpotDetector detector = new SpotDetector();
		
			// Get original image
			ImagePlus original = IJ.getImage();
		
			// GUI 1: Denoising GUI
			GenericDialog gd_denoise = new SetupDenoisingDialog().showDialog(original);
			if (gd_denoise.wasCanceled()) {
				return;
			}
			// Get denoising parameters
			double sigmaXY = gd_denoise.getNextNumber();
			double sigmaT = gd_denoise.getNextNumber();
			
			ImagePlus img = original.duplicate();
			original.hide();
			img.show();
			
			// Do the preprocessing (denoising + background subtraction)
			IJ.log("Blurring ...");
			denoiser.gaussianBlur3D(img, sigmaXY, sigmaT);
			ImagePlus medianProj = denoiser.medianProjection(img);
			img = denoiser.subtractBackground(img, medianProj);
			img.show();
			
			// GUI 2: Spot detection GUI
			GenericDialog gd_detection =  new SetupDetectionDialog().showDialog(original);
			if (gd_detection.wasCanceled()) {
				return;
			}
			// Get spot detection parameters
			double sigmaDOG = gd_detection.getNextNumber();					// sigma for DoG
			double DOGthreshold = gd_detection.getNextNumber();				// threshold of localmax after DoG filter
			int maxSpotDistance = (int) gd_detection.getNextNumber();    	// maximal distance between neighboring spots
			
			// Get spot tracking parameters
			double maxSpotMovement = gd_detection.getNextNumber(); 			// maximal movement of a spot in one timeframe
			int numberOfFramesInPast = (int) gd_detection.getNextNumber(); 	// number of frames in the past used to calculate speed of a spot

			// Get cost function parameters
			double betaDist = gd_detection.getNextNumber();
			double betaIntensity = gd_detection.getNextNumber();
			double betaPredict = gd_detection.getNextNumber(); 
			
			// Normalize the cost function parameters so they sum up to 1
			double totalWeight = betaDist + betaIntensity + betaPredict;
			SpotTracker tracker = new SpotTracker(betaDist/totalWeight, betaIntensity/totalWeight, betaPredict/totalWeight);
			
			// Get plot coloring paramters
			String hueBy = gd_detection.getNextChoice();
			String SatBy = gd_detection.getNextChoice();
			String BrightBy = gd_detection.getNextChoice();
			String[] HSB_coloring = {hueBy, SatBy, BrightBy};
			int minimalTraceLengthOnPlot = (int)gd_detection.getNextNumber();
			
									
			// Run difference of Gaussian
			// To save memory, we do this on each slice seperately
			IJ.log("Detect spots with DoG ...");
			int nt = img.getNFrames();
			ArrayList<Spot> spots[] = new Spots[nt];
			for (int t = 0; t < nt; t++) {
				img.setSlice(t + 1);
				ImagePlus dog = detector.dog(img, sigmaDOG);
				// Detect spots by detecting local maxima in DoG
				ArrayList<Spot> localmax = detector.localMax(dog, maxSpotDistance, DOGthreshold, t);
				spots[t] = detector.filter(localmax, maxSpotDistance);
			}
									
			// Link spots
			IJ.log("Linking spots with Nearest Neighbor Linking ...");
			for (int t = 0; t < nt - 1; t++) {
				img.setSlice(t+1);
				double[][] C = tracker.getCostMatrix(spots, img,
													 numberOfFramesInPast);

				tracker.nearestNeighbourLinking(C, spots, t, maxSpotMovement);
				
			}
			
			// Calculate trajectory length, speed and angle of all particles
			Overlay overlayTraces = new Overlay();
			int numResults = 0;										// keep track of the number of particles with a valid speed
			ArrayList[] trajectoryLengthDistribution = new ArrayList[nt]; 	// List of arraylists with angles
			ArrayList[] speedDistribution = new ArrayList[nt]; 				// List of arraylists with speeds
			ArrayList[] angleDistribution = new ArrayList[nt]; 				// List of arraylists with angles
			for (int t = 0; t < nt; t++) {
				ArrayList<Double> lengths = new ArrayList<Double>(); // Arraylist of doubles
				ArrayList<Double> speeds = new ArrayList<Double>(); // Arraylist of doubles
				ArrayList<Double> angles = new ArrayList<Double>(); // Arraylist of doubles
				for (Spot spot : spots[t]) {
					double[] stats = spot.trajectoryStatistics(spots, numberOfFramesInPast);
					lengths.add( stats[0] );
					speeds.add( stats[1] );
					angles.add( stats[2] );
					numResults += 1;
				}
				trajectoryLengthDistribution[t] = lengths;
				speedDistribution[t] = speeds;						// Add arraylist of speeds to speedDistribution list
				angleDistribution[t] = angles;						// Add arraylist of speeds to angleDistribution list
			}
			
			// Plot a distribution of speeds
			double[] resultsLength = new double[numResults];
			double[] resultsSpeed = new double[numResults];
			double[] resultsAngle = new double[numResults];
			double[] xArray = new double[numResults];
			
			int count = 0;
			double maxSpeed = -1;  // keep track of max speed, will be useful for drawing the traces
			double maxLength = -1; // keep track of max trace lentgh, will be useful for drawing the traces
			for (int t = 0; t < nt; t++) {
				for (int i = 0; i < speedDistribution[t].size(); i++) {
					resultsLength[count] = (double)trajectoryLengthDistribution[t].get(i);
					resultsSpeed[count] = (double)speedDistribution[t].get(i);
					resultsAngle[count] = (double)angleDistribution[t].get(i);
					maxLength = Math.max(maxLength, resultsLength[count]);
					if (!(Double.isNaN(resultsSpeed[count])))
						maxSpeed = Math.max(maxSpeed, resultsSpeed[count]);
					xArray[count] = t + 1;
					count += 1;
				}
			}
			
			// If the spot is at the end of the trace, 
			// and if the trace is long enough, then draw its trace.
			int numFinalTraces = 0;
			for (int t = 0; t < nt; t++) {
				for (Spot spot : spots[t]) {
					if (spot.isTraceEnd() & (spot.trace.size() > minimalTraceLengthOnPlot)){
						drawTrace(overlayTraces, spot, spots, numberOfFramesInPast, HSB_coloring, maxSpeed, maxLength);
						numFinalTraces++;
						
					}
				}
			}
			
			// Calculate the statistics of the final traces
			double[] finalSpeeds = new double[numFinalTraces];
			double[] finalLengths = new double[numFinalTraces];
			double[] finalAngles = new double[numFinalTraces];
			count = 0;
			for (int t = 0; t < nt; t++) {
				for (Spot spot : spots[t]) {
					if (spot.isTraceEnd() & (spot.trace.size() > minimalTraceLengthOnPlot)){
						double[] stats = spot.trajectoryStatistics(spots, numberOfFramesInPast);
						finalLengths[count] = stats[0];
						finalSpeeds[count] = stats[1];
						finalAngles[count] = stats[2];
						count++;
					}
				}
			}
			 			
			// Add results to plots
			Plot plotLength = new Plot("Results traces", "Frame", "Trajectory lengts");
			plotLength.add("circle", xArray, resultsLength);
			plotLength.show();
			
			Plot plotSpeed = new Plot("Results speed", "Frame", "Speed");
			plotSpeed.add("circle", xArray, resultsSpeed);
			plotSpeed.show();
			
			Plot plotAngle = new Plot("Results angle", "Frame", "Angle");
			plotAngle.add("circle", xArray, resultsAngle);
			plotAngle.show();
			
			// Draw traces as overlay
			Overlay overlaySpots = new Overlay();
			drawSpots(overlaySpots, spots);
			img.setOverlay(overlayTraces);
			img.show();
			
			// Make color bar
			if (hueBy.equals("angle")) {
				makeColorBar("Color bar angle", 256, 50, "0", "2 pi");
			}else if (hueBy.equals("speed")) {
				makeColorBar("Color bar speed", 256, 50, "0", String.format("%.1f", maxSpeed));
			}else if (hueBy.equals("trace length")) {
				makeColorBar("Color bar trace length", 256, 50, "0", String.format("%.1f", maxLength));
			}
			IJ.log("Success!");

	}
	
	private void drawSpots(Overlay overlay, ArrayList<Spot> spots[]) {
		int nt = spots.length;
		for (int t = 0; t < nt; t++)
			for (Spot spot : spots[t])
				spot.draw(overlay);
	}
	
	private void drawTrace(Overlay overlay, Spot spot, ArrayList<Spot> spots[], int numberOfFramesInPast, String[] HSB_coloring, double maxSpeed, double maxLength) {
		
		double[] stats = spot.trajectoryStatistics(spots, numberOfFramesInPast);
		
		// Fill HSB_values with 3 numbers between 0 and 1 (hue, saturation, brightness)
		double[] HSB_values = new double[3]; 
		for (int i=0; i<3; i++) {
			if (HSB_coloring[i].equals("angle")) {
				HSB_values[i] = (stats[2] + Math.PI) / Math.PI;
			}else if (HSB_coloring[i].equals("speed")) {
				HSB_values[i] = stats[1] / maxSpeed;
			}else if (HSB_coloring[i].equals("trace length")) {
				HSB_values[i] = stats[0] / maxLength;
			}else if (HSB_coloring[i].equals("random")) {
				HSB_values[i] = Math.random();
			}else {
				HSB_values[i] = 1;
			}
		}
		Color color = Color.getHSBColor((float)HSB_values[0], (float)HSB_values[1], (float)HSB_values[2]);
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 120);
		
		// Trace back this trace and draw a line
		for (int i = 0; i < spot.trace.size()-1; i++) {
			int time = spot.t - spot.trace.size() + i;
			Spot current = spots[time].get(spot.trace.get(i));
			Spot next = spots[time+1].get(spot.trace.get(i+1));
			Line line = new Line(current.x, current.y, next.x, next.y);
			line.setStrokeColor(color);
			line.setStrokeWidth(1);
			overlay.add(line);
		}
	}
	
	private void makeColorBar(String title, int width, int height, String startValue, String endValue) {
		// Create HSB colorbar.
		ImagePlus colorbar = IJ.createImage(title, "RGB", width, height, 1);
		colorbar = new CompositeImage(colorbar, IJ.COMPOSITE);
		ImageProcessor ipRed = colorbar.getChannelProcessor();
		ImageProcessor ipGreen = colorbar.getChannelProcessor();
		ImageProcessor ipBlue = colorbar.getChannelProcessor();
		
		// Fill in the pixels
		for (int x = 0; x < width; x++) {
			Color HSBColor = Color.getHSBColor((float)x / width, 1f, 1f);
			int[] columnRed = new int[height];
			int[] columnGreen = new int[height];
			int[] columnBlue = new int[height];
			Arrays.fill(columnRed, HSBColor.getRed());
			Arrays.fill(columnGreen, HSBColor.getGreen());
			Arrays.fill(columnBlue, HSBColor.getBlue());
			// Put pixels as columns
			colorbar.setPosition(1, 1, 1);
			ipRed.putColumn(x, 0, columnRed, height);
			colorbar.setPosition(2, 1, 1);
			ipGreen.putColumn(x, 0, columnGreen, height);
			colorbar.setPosition(3, 1, 1);
			ipBlue.putColumn(x, 0, columnBlue, height);
		}
		IJ.run(colorbar, "RGB Color", "");
		ImageProcessor ip = colorbar.getProcessor();
		ip.setFontSize(14);
		ip.setJustification(ip.LEFT_JUSTIFY);
		ip.drawString("- "+startValue, 0, (int)height/2 + 7);
		ip.setJustification(ip.RIGHT_JUSTIFY);
		ip.drawString(endValue+" -", width, (int)height/2 + 7);
		colorbar.show();
		colorbar.changes = false;
	}
}
