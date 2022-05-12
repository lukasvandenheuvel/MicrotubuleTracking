import java.awt.Color;
import java.util.ArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.plugin.PlugIn;

public class Track_Microtubules implements PlugIn {
	
	public void run(String arg) {
		
			// Denoising parameters
			double sigmaX = 1;				// smoothing sigma in X
			double sigmaY = 1;				// smoothing sigma in Y
			double sigmaT = 1;				// smoothing sigma in time
			// Cost function parameters
			double betaDist = 0;
			double betaIntensity = 0;
			double betaSpeed = 1; 
			double betaAngle = 0;
			// Other parameters 
			double sigmaDOG = 1;			// sigma for DoG
			int maxSpotDistance = 4;    	// maximal distance between neighboring spots
			double DOGthreshold = 1;		// threshold of localmax after DoG filter
			double maxSpotMovement = 6; 	// maximal movement of a spot in one timeframe
			int numberOfFramesInPast = 10; 	// number of frames in the past used to calculate speed of a spot
			
			// Import classes
			Denoiser denoiser = new Denoiser();
			SpotDetector detector = new SpotDetector();
			SpotTracker tracker = new SpotTracker();
			
			IJ.log("Starting the script");
			ImagePlus imp = IJ.getImage();
			
			// Do the preprocessing
			IJ.log("Preprocessing ...");
			denoiser.gaussianBlur3D(imp, sigmaX, sigmaY, sigmaT);
			imp = denoiser.subtractBackground(imp);
			
			// Run difference of Gaussian
			IJ.log("Running DoG ...");
			int nt = imp.getNFrames();
			ImagePlus dog = detector.dog(imp, sigmaDOG);
						
			// Detect spots by detecting local maxima in DoG
			IJ.log("Detecting spots ...");
			ArrayList<Spot> localmax[] = detector.localMax(dog, maxSpotDistance, DOGthreshold);
			ArrayList<Spot> spots[] = detector.filter(localmax, maxSpotDistance);
			
			// Link spots
			IJ.log("Linking spots ...");
			for (int t = 0; t < nt - 1; t++) {
				imp.setSlice(t+1);
				// Do a first linking based on distance, so that we can calculate
				// double[][] D = tracker.getDistanceMatrix(spots, t);
				// tracker.thresholdLinking(D, spots, t, maxSpotMovement);
				// Do a second linking with the full cost function
				double[][] C = tracker.getCostMatrix(spots, imp,
													 betaDist, betaIntensity, betaSpeed, betaAngle, numberOfFramesInPast);
				tracker.nearestNeighbourLinking(C, spots, t, maxSpotMovement);
				
			}
			
			// Calculate trajectory length, speed and angle of all particles
			Overlay overlayTraces = new Overlay();
			int numResults = 0;										// keep track of the number of particles with a valid speed
			ArrayList<Trace> allTraces = new ArrayList<Trace>();
			ArrayList[] trajectoryLengthDistribution = new ArrayList[nt]; 	// List of arraylists with angles
			ArrayList[] speedDistribution = new ArrayList[nt]; 				// List of arraylists with speeds
			ArrayList[] angleDistribution = new ArrayList[nt]; 				// List of arraylists with angles
			for (int t = 0; t < nt; t++) {
				IJ.log("Time = "+t);
				ArrayList<Double> traces = new ArrayList<Double>(); // Arraylist of doubles
				ArrayList<Double> lengths = new ArrayList<Double>(); // Arraylist of doubles
				ArrayList<Double> speeds = new ArrayList<Double>(); // Arraylist of doubles
				ArrayList<Double> angles = new ArrayList<Double>(); // Arraylist of doubles
				for (Spot spot : spots[t]) {
					double[] stats = spot.trajectoryStatistics(spots, numberOfFramesInPast);
					lengths.add( stats[0] );
					speeds.add( stats[1] );
					angles.add( stats[2] );
					
					IJ.log("Spot at time "+t+", is end = "+spot.isTraceEnd()+", trace length = "+stats[0]);
					
					if (spot.isTraceEnd() & (stats[0] > 5)){
						drawTrace(overlayTraces, spot, spots);
					}
					//if (spot.isTraceEnd()) {
					//	Trace trace = new Trace(spot);
					//	trace.colorByAngle(spots);
					//	allTraces.add(trace);
					//	IJ.log("Added trace of length " + spot.trace.size());
					//}
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
			for (int t = 0; t < nt; t++) {
				for (int i = 0; i < speedDistribution[t].size(); i++) {
					resultsLength[count] = (double)trajectoryLengthDistribution[t].get(i);
					resultsSpeed[count] = (double)speedDistribution[t].get(i);
					resultsAngle[count] = (double)angleDistribution[t].get(i);
					xArray[count] = t + 1;
					count += 1;
				}
			}
			// Add results to plots
			Plot plotLength = new Plot("Results traces", "Time", "Trajectory lengts");
			plotLength.add("circle", xArray, resultsLength);
			plotLength.show();
			
			Plot plotSpeed = new Plot("Results", "Time", "Speed");
			plotSpeed.add("circle", xArray, resultsSpeed);
			plotSpeed.show();
			
			Plot plotAngle = new Plot("Results", "Time", "Angle");
			plotAngle.add("circle", xArray, resultsAngle);
			plotAngle.show();
			
			// Draw traces as overlay
			Overlay overlaySpots = new Overlay();
			drawSpots(overlaySpots, spots);
			//drawTraces(overlay, allTraces, spots);
			imp.setOverlay(overlayTraces);
			imp.show();
		
			IJ.log("Success!");

	}
	
	private void drawSpots(Overlay overlay, ArrayList<Spot> spots[]) {
		int nt = spots.length;
		for (int t = 0; t < nt; t++)
			for (Spot spot : spots[t])
				spot.draw(overlay);
	}
	
	private void drawTrace(Overlay overlay, Spot spot, ArrayList<Spot> spots[]) {
		
		// Use angle as color;
		int startTime = spot.t - spot.trace.size(); // timepoint where the trace started
		Spot first = spots[startTime].get(spot.trace.get(0));
		double angle = Math.atan2(spot.y - first.y, spot.x - first.x);
		double rescaledAngle = (angle + Math.PI) / Math.PI;
		Color color = Color.getHSBColor((float)rescaledAngle, 1f, 1f);
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
}
