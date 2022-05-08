# MicrotubuleTracking  
  
## Main Plugin script
```Track_Microtubules.java```  

## Other classes  
```Denoiser.java``` contains functions to denoise the image;  
```SpotDetector.java``` contains functions to find spots;  
```SpotTrackor.java``` contains functions to track and link spots;  
```Spot.java``` is the spot class;  
```Spots.java``` is the arrayList of spots.

## NOTE  
I get out-of-memory problems when trying to run the script on the whole dataset. To solve this, I downscaled the image with a factor of 2 in X and Y direction.
