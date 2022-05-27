import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.IJ;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SetupDenoisingDialog implements ActionListener {
	
	ImagePlus crop;
	JButton previewBtn = new JButton("Preview");
	GenericDialog  gd = new GenericDialog("Enter denoising parameters");
		
	public GenericDialog showDialog( ImagePlus imp )
	{
		// Initiate a 'crop' ImagePlus (one timeframe) 
		crop = new ImagePlus();
		
        gd.addNumericField("SigmaXY", 1);
        gd.addNumericField("SigmaT", 2);
        
        // Add listener to button
     	previewBtn.addActionListener(this);
        
        gd.add(previewBtn);
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
		
		Denoiser denoiser = new Denoiser();
		
		// Close the previous crop
		crop.changes = false; // this avoids "Save changes?" message
		crop.close();
		
		ImagePlus imp = IJ.getImage();
		
		// Get parameters
		Vector<TextField> params = gd.getNumericFields();
		double sigmaXY = Double.parseDouble( params.get(0).getText() );
		
		// Do denoising on one frame only to save time
		crop = imp.crop("whole-slice");
		denoiser.gaussianBlur(crop, sigmaXY);
		crop.show();
	}
}
