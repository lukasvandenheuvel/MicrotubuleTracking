package MicrotubuleTracking.scr;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;

public class SpotTracker {
	
	public double betaDist;
	public double betaIntensity;
	public double betaPred; 
	
	public SpotTracker(double betaDist, double betaIntensity, double betaPred) {
		this.betaDist = betaDist;
		this.betaIntensity = betaIntensity;
		this.betaPred = betaPred;
	}
	
	public double cost(Spot current, Spot next, ArrayList<Spot> spots[], ImagePlus imp, 
					   double dMax, double fMax, double pMax,
					   int numberOfFramesInPast) {
		// Implementation of cost function.
		// This function gets the cost for particles current and next.
		// Note that I added a method intensityDifference to the Spot class,
		// which calculates the difference in mean intensity between the spots current and next.
		double dist = current.distance(next);
		double distToPred = current.distanceToPredicted(next, spots, numberOfFramesInPast);
		double intensityDiff = current.intensityDifference(next, imp);
		if (Double.isNaN(distToPred)) { // in case velocity is not available, replace it by distance
			return (this.betaDist+this.betaPred) * dist/dMax + this.betaIntensity * intensityDiff/fMax ;
		}
		return distToPred * this.betaPred/pMax + dist *this.betaDist/dMax + this.betaIntensity * intensityDiff/fMax;

	}
	
	public double[][] getDistanceMatrix(ArrayList<Spot> spots[], int t){
		// Get max distance
		double dMax = -1;
		for (Spot current: spots[t]) {
			for (Spot next: spots[t+1]) {
				dMax = Math.max(dMax, current.distance(next));
			}
		}
		// Fill distance matrix
		double[][] D = new double[spots[t].size()][spots[t+1].size()];
		for (int i = 0; i < spots[t].size(); i++) {
			Spot current = spots[t].get(i);
			for (int j = 0; j < spots[t+1].size(); j++) {
				Spot next = spots[t+1].get(j);
				D[i][j] = current.distance(next) / dMax;
			}
		}
		return D;
	}
	
	public double[][] getCostMatrix(ArrayList<Spot> spots[], ImagePlus imp,
									int numberOfFramesInPast){
		// This function returns an AxB matrix, where A is the number of particles on timeframe t
		// and B is the number of particles on timeframe t+1.
		// C_ij is the cost function for particle i (on frame t) and particle j (on frame t+1).
		// spots is an array of list of spots, such that spots[t] is the list of spots at time t
		
		int t = imp.getCurrentSlice() - 1;		
		// Get max distance, delta_intensity, delta_speed and delta_theta (used for normalization afterwards)
		double dMax = -1;
		double fMax = -1;
		double pMax = -1;
		for (Spot current: spots[t]) {
			for (Spot next: spots[t+1]) {
				dMax = Math.max(dMax, current.distance(next));
				fMax = Math.max(fMax, current.intensityDifference(next, imp));
				double p = current.distanceToPredicted(next, spots, numberOfFramesInPast);
				pMax = Math.max(pMax, Double.isNaN(p)?-1:p ); // if speed is not defined, just skip
			}
		}
		// Fill cost matrix
		double[][] C = new double[spots[t].size()][spots[t+1].size()];
		for (int i = 0; i < spots[t].size(); i++) {
			Spot current = spots[t].get(i);
			for (int j = 0; j < spots[t+1].size(); j++) {
				Spot next = spots[t+1].get(j);
				C[i][j] = cost(current, next, spots, imp, 
						dMax, fMax, pMax,  
					   numberOfFramesInPast);
			}
		}
		return C;
	}
	
	public void thresholdLinking(double[][] C, ArrayList<Spot> spots[], int t, double costThreshold) {
		// This function links 2 spots if the cost function between them is smaller than  costThreshold.
		for (int i = 0; i < spots[t].size(); i++) {
			for (int j = 0; j < spots[t+1].size(); j++) {
				if (C[i][j] < costThreshold) {
					spots[t].get(i).link(spots[t+1].get(j));
				}
			}
		}
	}
	
	public void nearestNeighbourLinking(double[][] C, ArrayList<Spot> spots[], int t, double distanceThreshold) {
		// Implementation of nearest neighbor linking, and
		// BONUS QUESTION: track dividing cells.
		// ------
		// Inputs:
		//
		// C -> A cost matrix made with the method getCostMatrix().
		//		Rows are spots in the current timeframe t, columns are spots in the next timeframe t+1.
		//		C_ij is the cost between current spot i and next spot j.
		//
		// spots -> Arraylist of spots in all timeframes.
		//
		// t -> Integer indicating the timeframe.
		// ------
		//
		// ------
		// How the code works:
		// example: The 3 particles in timeframe t are A0, A1, A2.
		// 			The 2 particles in timeframe t+1 are B0, B1.
		//
		// We make a directed bipartite graph by creating 2 lists:
		// closestToCurrent: connects particles in timeframe t to closest particles in timeframe t+1.
		// 		example: (A0, B0), (A1, B1), (A2, B1).
		// closestToNext: connects particles in timeframe t+1 to closest particles in timeframe t.
		// 		example: (B0, A0), (B1, A2).
		// 
		// Then, we link A_i to B_j if (A_i, B_j) exists in closestToCurrent AND if (B_j, A_i) exists in closestToNext.
		// 		example: link(A0,B0) and link(A2,B1).
		//
		// For the BONUS question, we give spots the same color if an edge between them exists in closestToNext but NOT in closestToCurrent,
		// which means that a divided (daughter) spot must have its mother as the closest spot, but the mother must have another spot closer to it.
		// Also, the distance between mother and daughter spots can be at most 30 pixels.
		// ------
		
		int[] closestToCurrent = new int[spots[t].size()];
		for (int i = 0; i < spots[t].size(); i++) {
			int closest = 0;
			for (int j = 0; j < spots[t+1].size(); j++) {
				if (C[i][j] <= C[i][closest]) 
					closest = j;
			}
			closestToCurrent[i] = closest;
		}
//		IJ.log(""+t);
		int[] closestToNext = new int[spots[t+1].size()];
		for (int j = 0; j < spots[t+1].size(); j++) {
			int closest = 0;
			for (int i = 0; i < spots[t].size(); i++) {
//				IJ.log(""+C[i][j]);
				if (C[i][j] <= C[closest][j]) 
					closest = i;
			}
			closestToNext[j] = closest;
		}
		// Link spots if an edge between them exists in both closestToCurrent and closestToNext.
		// and if they are close enough together
		for (int i = 0; i < spots[t].size(); i++) {
			if (closestToNext[closestToCurrent[i]]==i) {
				Spot current = spots[t].get(i);
				Spot next = spots[t+1].get(closestToCurrent[i]);
				if (current.distance(next) < distanceThreshold) {
					next.link(current);
					next.trace.addAll(current.trace);
					next.trace.add(i);
				}
			}
		}
	}
}
