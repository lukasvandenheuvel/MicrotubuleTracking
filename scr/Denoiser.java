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
	
	public void gaussianBlur(ImagePlus imp, double sigmaXY){
		IJ.run(imp, "Gaussian Blur...", "sigma="+sigmaXY+" slice");
	}

	
	public void gaussianBlur3D(ImagePlus imp, double sigmaXY, double sigmaT) {
		IJ.run("Gaussian Blur 3D...", "x="+sigmaXY+" y="+sigmaXY+" z="+sigmaT);
	}
	
	public ImagePlus medianProjection(ImagePlus imp) {
		ImagePlus medProjection = ZProjector.run(imp,"median");
		return medProjection;
	}
	
	public ImagePlus subtractBackground(ImagePlus imp, ImagePlus background) {
		// Subtract the median value over time from every timeframe on the image.
		ImagePlus result = ImageCalculator.run(imp, background, "Subtract create 32-bit stack");
		imp.close();
		return result;
	}
}
