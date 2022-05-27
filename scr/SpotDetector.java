package MicrotubuleTracking.scr; // not sure what this does

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;

public class SpotDetector {
	
	public ImagePlus dog(ImagePlus imp, double sigma) {
		// Perform difference of Gaussian
		ImageProcessor ip = imp.getProcessor();
		ImagePlus g1 = imp.crop("whole-slice"); // duplicate only one slice
		ImagePlus g2 = imp.crop("whole-slice"); // duplicate only one slice
		IJ.run(g1, "Gaussian Blur...", "sigma=" + sigma);
		IJ.run(g2, "Gaussian Blur...", "sigma=" + (Math.sqrt(2) * sigma));
		ImagePlus dog = ImageCalculator.run(g1, g2, "Subtract create 32-bit stack");
		g1.close();
		g2.close();
		ip = dog.getProcessor();
		return dog;
	}
	
	public Spots localMax(ImagePlus dog, int nbh_size, double threshold, int t) {
		// Find local maxima. Pixel values of maxima must be larger than threshold,
		// and maxima must be maxima on a neighborhood of size nbh_size
		int nx = dog.getWidth();
		int ny = dog.getHeight();
		Spots spots = new Spots();
		ImageProcessor ip = dog.getProcessor();
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
						spots.add(new Spot(x, y, t));
					}
				}
			}
		}
		return spots;
	}
	
	public ArrayList<Spot> filter(ArrayList<Spot> spotList, int nbh_size) {
		// Remove spots that have a neighbor closer than nbh_size away from it.
		ArrayList<Spot> out = new Spots();
		for (Spot spot : spotList) {
			if (spot.closestDistance(out) > nbh_size)
				out.add(spot);
		}		
		return out;
	}
}
