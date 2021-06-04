/**Class Leader.java receives information from all other nodes
 * once per round, determines when to start and stop the simulation,
 * and logs the results of a run to a file.
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
package impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.DataFormatException;

import util.NodeState;
import util.StatBundle;

public class Leader extends Thread
{
	private final static String OUTPUT_METADATA = "output is characterized by the following 15-tuple:\n"
			+ "Round number, #of nodes that are Suceptible, #of nodes that are Infected, "
			+ "#of nodes that are Recovered/Removed, #of nodes that are Environment nodes, "
			+ "Total number of Agents, #Agents in Infected nodes, Average #Agents per Infected node, "
			+ "#Agents in transit, #Agents in Environment nodes, Average #Agents per Environment node, "
			+ "#Agents removed from Recovered nodes, Average #Agents removed per Recovered node, "
			+ "#Agents removed from Environment nodes, Average #Agents removed from environment nodes\n";
	
	private int n; //the number of nodes (not including me)
	private Deque<StatBundle> messages;
	private int roundLimit; //number of rounds to run for... by default, -1
							// (waits for all nodes to be infected)
	private int currentRound;
	private boolean keepGoing;
	private BufferedWriter writer;
	
	//counters for infected nodes
	private int numSus;
	private int numInf;
	private int numRec;
	private int numNonHum;
	//number of agents/state
	private int numAgSus;
	private int numAgInf;
	private int numRemovedRec;
	private int numAgNonHum;
	private int numRemovedNonHum;
	private int numAgentsSent; //only totals for infected and environment nodes
	
	
	/**Makes a leader node. NOTE: setNumNodes() MUST be called after making this node
	 * and BEFORE the simulation starts!
	 */
	public Leader()
	{
		n = -1;
		messages = new ConcurrentLinkedDeque<>();
		roundLimit = -1;
		currentRound = 0;
		clearCounters();
		keepGoing = true;
		
		//attempt to make the writers for output file. Use BufferedWriter for efficiency
		try 
		{
			File root = new File("output");
			File[] numoutputs = root.listFiles();
			File output = new File("output/output" + numoutputs.length + ".csv");
			output.createNewFile();
			FileWriter innerwriter;
			innerwriter = new FileWriter(output);
			writer = new BufferedWriter(innerwriter);
		}
		catch (IOException e)
		{
			System.out.println("Exception thrown when writing output file");
			e.printStackTrace();
		}
	}
	
	public void setNumNodes(int numNodes)
	{
		n = numNodes;
	}
	
	public void setRoundLimit(int newLimit)
	{
		roundLimit = newLimit;
	}
	
	public void recieveMessage(StatBundle msg)
	{
		messages.add(msg);
	}
	
	public boolean continueSimulation()
	{
		return keepGoing;
	}
	
	public int getUniversalRound()
	{
		return currentRound;
	}
	
	/** Determines if we should keep running the simulation. 
	 * PRECONDITION: NUMSUS, NUMINF, NUMREC, NUMNONHUM HAVE BEEN UPDATED PRIOR
	 * TO THIS METHOD EXECUTING (AKA, PROCESSMESSAGES() HAS BEEN CALLED FIRST)
	 * @return true if we are running to a round limit and thisRound <= roundLimit
	 * 		else, return true if we are NOT running to a round limit and all nodes
	 * 		that can be infected have been or are currently infected
	 * 		return false otherwise
	 */
	private boolean keepRunning()
	{
		//check to see if we are running to a round limit
		if (roundLimit != -1)
		{
			if (currentRound < roundLimit)
				return true;
			return false;
		}
		else
		{
			//stop if everyone has recovered
			if ((numSus == 0 && currentRound > 0 && numInf == 0))
				return false;
			//also stop if there are no agents in the system
			int totalAgents = numAgSus + numAgInf + numAgNonHum + numAgentsSent;
			if (totalAgents == 0)
				return false;
			return true;
		}
	}
	
	/**Sets numSus, numInf, numRec, numNonHum, and their associated
	 * agent counters numAgSus, numAgInf, numAgRec, numAgNonHum to 0
	 * Also sets numAgentsSent to 0
	 */
	private void clearCounters()
	{
		numSus = 0;
		numInf = 0;
		numRec = 0;
		numNonHum = 0;
		
		numAgSus = 0;
		numAgInf = 0;
		numRemovedRec = 0;
		numAgNonHum = 0;
		numRemovedNonHum = 0;
		numAgentsSent = 0;
	}
	
	/** Iterates through all messages and counts the state of
	 * each node as well as the number of agents
	 */
	private void processMessages()
	{
		clearCounters();
		Iterator<StatBundle> iter = messages.iterator();
//		System.out.println("Processing messages. " + messages.size() + 
//				" messages receieved");
		while (iter.hasNext())
		{
			StatBundle bund = iter.next();
			NodeState state = bund.getState();
//			System.out.println("Bundle recieved from " + bund.getName() +
//					" State: " + state);
			switch (state)
			{
				case SUSCEPTIBLE:
					numSus++;
					numAgSus += bund.getNumAgents();
					break;
				case INFECTED:
					numInf++;
					numAgInf += bund.getNumAgents();
					numAgentsSent += bund.getMsgsSent();
					break;
				case NONHUMAN:
					numNonHum++;
					numAgNonHum += bund.getNumAgents();
					numRemovedNonHum += bund.getThrownAway();
					numAgentsSent += bund.getMsgsSent();
					break;
				case RECOVERED:
					numRec++;
					numRemovedRec += bund.getThrownAway();
					break;
			}
				
		}
	}
	
	/** PRECONDITION: PROCESSMESSAGES() HAS BEEN CALLED BEFORE THIS METHOD EXECUTES
	 * prints the statistics as received by statbundles
	 */
	private void printStatistics()
	{
		System.out.println("This is the leader node. Round "
				+ currentRound + " has ended. Printing statistics...");
		System.out.println("(Number of nodes) S: " + numSus + " I: " + numInf 
				+ " R: " + numRec + " Environment: " + numNonHum);
		
		//compute averages
		double infAvg;
		double recAvg;
		double nonHumAvg;
		double removedEnvAvg;
		
		if (numInf == 0) //keep in mind dividing by zero...
			infAvg = 0;
		else
			infAvg = (1.0 * numAgInf) / (1.0 * numInf);
		if (numRec == 0)
			recAvg = 0;
		else
			recAvg = (1.0 * numRemovedRec) / (1.0 * numRec);
		if (numNonHum == 0)
		{
			nonHumAvg = 0;
			removedEnvAvg = 0;
		}
		else
		{
			nonHumAvg = (1.0 * numAgNonHum) / (1.0 * numNonHum);
			removedEnvAvg = (1.0 * numRemovedRec) / (1.0 * numNonHum);
		}
		
		//remove numAgSus in future iterations, only here for debugging
		int totalAg = numAgSus + numAgInf + numAgNonHum + numAgentsSent; 
		
//		System.out.println("Total number of agents: " + totalAg 
//				+ ", distributed amoung the following:");
//		System.out.println("#Agents, I: " + numAgInf + ", Average: " + infAvg);
//		System.out.println("#Agents in transit: " + numAgentsSent);
//		System.out.println("#Agents, Environment: " + numAgNonHum + ", Average: " 
//				+ nonHumAvg);
//		System.out.println("#Agents removed R: " + numRemovedRec + ", Average removed: " + recAvg);
//		System.out.println("#Agents removed Environment: " + numRemovedNonHum + 
//				" Average removed: " + removedEnvAvg);
		
		//write the info that was printed as a 15-tuple to the output file
		try
		{
			String roundSummary = "" + currentRound + ", " + numSus + ", " + numInf +
					", " + numRec + ", " + numNonHum + ", " + totalAg + ", " +
					numAgInf + ", " + infAvg + ", " + numAgentsSent + ", " + numAgNonHum +
					", " + nonHumAvg + ", " + numRemovedRec + ", " + recAvg +
					 ", " + numRemovedNonHum + ", " + removedEnvAvg + "\n";
			writer.write(roundSummary);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			if (n == -1)
				throw new DataFormatException("Didn't initialize the number of nodes in the system");
			//log metadata to output file
			writer.write(OUTPUT_METADATA);
			
			while (keepGoing)
			{
				//wait until all the nodes finish
				while (messages.size() < n)
					Thread.sleep(50);
				processMessages();
				printStatistics();
				messages = new ConcurrentLinkedDeque<>();
				keepGoing = keepRunning();
				//let the nodes continue executing again
				currentRound++;
			}
			writer.close();
		}
		catch (Exception e)
		{
			System.out.println("Exception in leader thread.");
			e.printStackTrace();
		}
	}
}
