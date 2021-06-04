/**Class Link.java Models a directed edge to send Agents on
 * 
 * @author Madison Pickering
 * (Copyright 2020 Madison Pickering)
 * 
 * This file is part of NetworkSIR/EnvironmentalSIR.

    NetworkSIR/EnvironmentalSIR is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetworkSIR/EnvironmentalSIR is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetworkSIR/EnvironmentalSIR.  If not, see <https://www.gnu.org/licenses/>.
 */
package util;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import impl.Node;

public class Link {
	private double probLow; //s.t. messages are sent when they meet [probLow, probHigh]
	private double probHigh;
	private Node dest; //destination
	
	/** "Default" constructor... sets probLow and probHigh to what is specified */
	public Link(double probLow, double probHigh, Node destination)
	{
		this.probLow = probLow;
		this.probHigh = probHigh;
		dest = destination;
	}
	
	/** Constructor override... sets probLow and probHigh to -1 */
	public Link(Node destination)
	{
		dest = destination;
		probLow = -1;
		probHigh = -1;
	}
	
	public double getProbLow()
	{
		return probLow;
	}
	
	public double getProbHigh()
	{
		return probHigh;
	}
	
	public Node getDestination()
	{
		return dest;
	}
	
	private void setProbLow(double prob)
	{
		probLow = prob;
	}
	
	private void setProbHigh(double prob)
	{
		probHigh = prob;
	}
	
	/**Converts a given input generated by nextDouble()
	 * into a CPI by feeding it through the equation (0.06/prob)^(1/1.7)
	 * @param prob the result of calling nextDouble()
	 */
	private static double probToCPI(double prob)
	{
		double scaledProb = prob / 0.06;
		double result = Math.pow(scaledProb, 1.0/1.7);
		return result;
	}
	
	/** Weights the edges by doing the following:
	 * 
	 * @param outEdges the links to weight
	 * @param stay the probability that an agent will stay within a node
	 */
	public static void weightEdges(ArrayList<Link> outEdges, double stay)
	{	
		//generate a probability for each outEdge.. ea prob corresponds to the prob --> that edge
		double[] probabilities = new double[outEdges.size()];
		double normalizer = 0;
		for (int i = 0; i < outEdges.size(); i++)
		{
			double weight = ThreadLocalRandom.current().nextDouble();
			//convert to probability to some CPI duration
			weight = probToCPI(weight);
			
			//prevent underflow
			if (weight < 0)
				weight = 0.01;		
			
			normalizer += weight;
			probabilities[i] = weight;
		}
		
		double bound = 0; //for setting lower bounds
		//normalize the generated probabilities s.t. generated probabilities U stay probability sums to 1
		stay = 1 - stay;
		for (int i = 0; i < probabilities.length; i++)
		{
			double normalized = (probabilities[i] / normalizer) * stay;
			Link link = outEdges.get(i);
			link.setProbLow(bound); //lower bound is mostly for human use
			link.setProbHigh(bound + normalized);
			bound += normalized; 
		}

	}
	
}