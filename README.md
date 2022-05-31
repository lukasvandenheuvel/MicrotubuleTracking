# MicrotubuleTracking  
  
Bioimage informatics (BIO-410) Miniproject.

Authors : [Brodier Mariane](mailto:mariane.brodier@epfl.ch) , [Guirardel Lucas](mailto:lucas.guirardel@epfl.ch), [van den Heuvel Lukas](mailto:cornelius.vandenheuvel@epfl.ch).

[Github link](github.com/lukasvandenheuvel/MicrotubuleTracking)

This ImageJ Java plugin tracks microtubules on a TIFF image. It overlays the microtubule trajectories on the image, colored according to the microtubule orientation.
It also display scatterplots of the speeds and angles of the microtubules as a function of time.

## Contents

This .zip archive contains the compiled .jar file containing ImageJ and the MicrotubuleTracker.
It also contains the source code in ```src/```.
Finally, the ```slides.pdf``` document contains slides presenting the project in more detail.

## Usage

Open an image with ImageJ and select "Plugins > Tracker".
Choose the denoising parameters on the GUI, then when the image is denoised, choose the tracking parameters.
Default parameters are already in place.

Output : four images, containing the denoised image with the trajectories overlaid, a colorbar for the trajectories, the angle scatterplot, and the speed scatterplot.

### Parameters

#### Denoising parameters

- SigmaXY : spatial Gaussian blur $\sigma$
- SigmaT : temporal Gaussian blur $\sigma$

#### Spot detection parameters

- DOG sigma (pixels) : Difference of Gaussian filter for spot detection $\sigma_1$ ($\sigma_2 = \sqrt{2} \sigma_1$)
- DOG threshold  (pixels) : minimum intensity value to detect spots.
- Maximal distance between neighbouring spots  (pixels) : prevent spots closer than this distance to appear.

#### Spot tracking parameters

- Maximal spot movement (pixels) : spots farther than this distance will not be linked.
- Maximal number of frames in past : number of frames used to compute trajectory statistics.

#### Cost function parameters

Weights given to different components of the cost function.

- Distance cost : weight of the distance between current and next spot.
- Intensity cost : weight of the difference in intensity between current and next spot.
- Distance to predicted cost : weight of the distance between predicted and actual next spot.

## Code structure

### Main Plugin script

```Track_Microtubules.java```  

### Other classes  

```Denoiser.java``` contains functions to denoise the image.

```SpotDetector.java``` contains functions to find spots.

```SpotTracker.java``` contains functions to track and link spots, in particular the cost function.

```Spot.java``` is the spot class. It contains methods to compute trajectory properties and distances between spots.

```Spots.java``` is a synonym for  ```ArrayList<Spot> []```.

```SetupDenoisingDialog.java``` and ```SetupDetectionDialog.java``` are GUI classes.
