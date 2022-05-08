import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;

public class Denoiser {
	
	public Denoiser() {
	}
	
	public void gaussianBlur3D(ImagePlus imp, double sigmaX, double sigmaY, double sigmaT) {
		IJ.run("Gaussian Blur 3D...", "x="+sigmaX+" y="+sigmaY+" z="+sigmaT);
	}
	
	public ImagePlus subtractBackground(ImagePlus imp) {
		// Subtract the median value over time from every timeframe on the image.
		ImagePlus medProjection = ZProjector.run(imp,"median");
		ImagePlus result = ImageCalculator.run(imp, medProjection, "Subtract create 32-bit stack");
		imp.close();
		return result;
	}
}
