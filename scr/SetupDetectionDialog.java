package MicrotubuleTracking.scr;

import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Overlay;

public class SetupDetectionDialog implements ActionListener {
	
	ImagePlus crop;
	JButton previewBtn = new JButton("Preview");
	GenericDialog  gd = new GenericDialog("Enter spot detection parameters");
	
	public GenericDialog showDialog( ImagePlus imp )
	{
		// Initiate a 'crop' ImagePlus (one timeframe) 
		crop = new ImagePlus();
		
		gd.addMessage("Spot detection parameters:");
        gd.addNumericField("DOG sigma", 3);
        gd.addNumericField("DOG threshold", 1);
        gd.addNumericField("Maximal distance between neigbouring spots", 5);
        // Add listener to button and add button to GenericDialog
     	previewBtn.addActionListener(this);
        gd.add(previewBtn);
        
        gd.addMessage("Spot tracking parameters:");
        gd.addNumericField("Maximal spot movement (in one timestep)", 15);
        gd.addNumericField("Maximal number of frames in past considered for speed calculation", 10);
        
        gd.addMessage("Cost function parameters (must sum up to 1):");
        gd.addNumericField("Distance cost", 0.25);
        gd.addNumericField("Intensity cost", 0.25);
        gd.addNumericField("Speed cost", 0.25);
        gd.addNumericField("Angle cost", 0.25);
        
        gd.showDialog();
        
        if (gd.wasCanceled()){
        	crop.changes = false; // this avoids "Save changes?" message
        	crop.close();
        	return gd;
        }
        crop.changes = false; // this avoids "Save changes?" message
        crop.close();
        gd.dispose();
        return gd;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		// Close the previous crop
		crop.changes = false; // this avoids "Save changes?" message
		crop.close();
		
		ImagePlus imp = IJ.getImage();
		SpotDetector detector = new SpotDetector();
		
		// Get parameters
		Vector<TextField> params = gd.getNumericFields();
		double sigmaDOG = Double.parseDouble( params.get(0).getText() );
		double DOGthreshold = Double.parseDouble( params.get(1).getText() );
		int maxSpotDistance = (int) Double.parseDouble( params.get(2).getText() );
		
		// Do spot detection on one frame only to save time
		crop = imp.crop("whole-slice");
		
		// Detect spots by detecting local maxima in DoG
		ImagePlus dog = detector.dog(crop, sigmaDOG);
		ArrayList<Spot> localmax = detector.localMax(dog, maxSpotDistance, DOGthreshold, 0);
		ArrayList<Spot> spots = detector.filter(localmax, maxSpotDistance);
		
		// Draw spot detections as overlay
		Overlay overlay = new Overlay();
		draw(overlay, spots);
		crop.setOverlay(overlay);			
		crop.show();
	}
	
	private void draw(Overlay overlay, ArrayList<Spot> spots) {
		for (Spot s : spots)
			s.draw(overlay);
	}
}
