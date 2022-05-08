import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;

public class SpotDetector {
	
	public ImagePlus dog(ImagePlus imp, double sigma) {
		// Perform difference of Gaussian
		ImagePlus g1 = imp.duplicate();
		ImagePlus g2 = imp.duplicate();
		IJ.run(g1, "Gaussian Blur...", "sigma=" + sigma + " stack");
		IJ.run(g2, "Gaussian Blur...", "sigma=" + (Math.sqrt(2) * sigma) + " stack");
		ImagePlus dog = ImageCalculator.run(g1, g2, "Subtract create 32-bit stack");
		dog.show();
		return dog;
	}
	
	public Spots[] localMax(ImagePlus imp, int nbh_size, double threshold) {
		// Find local maxima. Pixel values of maxima must be larger than threshold,
		// and maxima cannot be further than nbh_size away from each other.
		int nt = imp.getNFrames();
		int nx = imp.getWidth();
		int ny = imp.getHeight();
		Spots spots[] = new Spots[nt];
		for (int t = 0; t < nt; t++) {
			imp.setPosition(1, 1, t + 1);
			ImageProcessor ip = imp.getProcessor();
			spots[t] = new Spots();
			for (int x = nbh_size; x < nx - nbh_size; x++) {
				for (int y = nbh_size; y < ny - nbh_size; y++) {
					double v = ip.getPixelValue(x, y);
					// Maxima must be larger than threshold
					if (v > threshold) {
						double max = -1;
						// Maxima must be max in their neighborhood.
						for (int k = -nbh_size; k <= nbh_size; k++)
							for (int l = -nbh_size; l <= nbh_size; l++)
								max = Math.max(max, ip.getPixelValue(x + k, y + l));
						if (v == max) {
							spots[t].add(new Spot(x, y, t));
						}
					}
				}
			}
		}
		return spots;
	}
	
	public ArrayList<Spot>[] filter(ArrayList<Spot> spots[], int nbh_size) {
		// Remove spots that have a neighbor closer than nbh_size away from it.
		int nt = spots.length;
		ArrayList<Spot> out[] = new Spots[nt];
		for (int t = 0; t < nt; t++) {
			out[t] = new Spots();
			for (Spot spot : spots[t]) {
				if (spot.closestDistance(out[t]) > nbh_size)
					out[t].add(spot);
			}
		}
		
		return out;
	}

}
