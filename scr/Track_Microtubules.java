import java.util.ArrayList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.plugin.PlugIn;

public class Track_Microtubules implements PlugIn {
	
	public void run(String arg) {
		
			double sigmaX = 1;				// smoothing sigma in X
			double sigmaY = 1;				// smoothing sigma in Y
			double sigmaT = 1;				// smoothing sigma in time
			double lambda = 0.1; 			// for cost function
			double sigmaDOG = 1;			// sigma for DoG
			int maxSpotDistance = 4;    	// maximal distance between neighboring spots
			double DOGthreshold = 1;		// threshold of localmax after DoG filter
			double maxSpotMovement = 7; 	// maximal movement of a spot in one timeframe
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
				double[][] C = tracker.getCostMatrix(spots, imp, lambda);
				tracker.nearestNeighbourLinking(C, spots, t, maxSpotMovement);
			}
			
			// Calculate speed of all particles
			ArrayList[] speedDist = new ArrayList[nt];
			for (int t = 0; t < nt - 1; t++) {
				ArrayList<Double> speeds = new ArrayList<Double>();
				for (int i = 0; i < spots[t].size(); i++) {
					double speed = spots[t].get(i).speed(spots, numberOfFramesInPast);
					if (speed > 0)
						speeds.add( speed );
				}
				speedDist[t] = speeds;
			}
			
			// Draw traces as overlay
			Overlay overlay = new Overlay();
			draw(overlay, spots);
			imp.setOverlay(overlay);
			
			// Plot a histogram of speeds in a specific timeframe
			int timeframeToPlot = 100;
			double[] toPlot = new double[speedDist[timeframeToPlot].size()];
			for (int i = 0; i < toPlot.length; i++){
				toPlot[i] = (double)speedDist[timeframeToPlot].get(i);
			}
			Plot plot = new Plot("Results", "Speed", "Frequency");
			plot.addHistogram(toPlot);
			plot.show();
		
			IJ.log("Success!");

	}
	
	private void draw(Overlay overlay, ArrayList<Spot> spots[]) {
		int nt = spots.length;
		for (int t = 0; t < nt; t++)
			for (Spot spot : spots[t])
				spot.draw(overlay);
	}

}
