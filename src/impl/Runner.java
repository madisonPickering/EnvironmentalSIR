/** Runner.java runs the simulation. It can either run a simulation
 * based on an input file, or a default simulation.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import util.IntegerComparator;
import util.Link;
import util.NodeState;

public class Runner {

	public static int NUM_NODES = 5; //should be constant for the run
	public static final double DEFAULT_AGENT_STAY_PROBABILITY = 0.78;
	private static final int INFECT_X_NODES = 5; //number of nodes to infect
	private static final int ENV_X_NODES = 50; //number of nodes to randomly set as environment
	
	
	public static void main(String[] args)
	{	
		//make a leader
		Leader leader = new Leader();
		
		//determine how we are going to run the simulation
		System.out.println("Run simulation using input file? y/n");
		Scanner userInput = new Scanner(System.in);
		String yno = "yes";//userInput.next(); /***** COMMENT OUT IF NOT WANTING TO RUN MANY TIMES**/
		userInput.close();
		
		if (yno.equalsIgnoreCase("y") || yno.equalsIgnoreCase("yes"))
		{
			//read from input
			//first line = number of threads (other than leader)
			File file = new File("input/input_hires.txt"); //needs to be changed later
			try
			{
				Scanner scan = new Scanner(file);
				scan.next(); //clear human tokens 
				scan.next();
				int n = scan.nextInt();
				NUM_NODES = n;
				
				//parse to get indexes of those infected
				scan.next();
				scan.next();
				String indexesofinfected = scan.nextLine();
				//parse to get indexes of those recovered
				scan.next();
				scan.next();
				String indexesofrecovered = scan.nextLine();
				//parse to get indexes of those nonhuman
				scan.next();
				scan.next();
				String indexesofnonhuman = scan.nextLine();
				
				//convert string info to a list of indexes
				ArrayList<Integer> infected = getNodesIndexes(indexesofinfected);
				
				//if no infected were chosen, randomly choose some
				if (infected.size() == 0)
				{
					while (infected.size() < INFECT_X_NODES)
					{
						Integer infectedInd = (int)(Math.random() * n);
						boolean contains = infected.contains(infectedInd);
						if (!contains)
							infected.add(infectedInd);
					}
					Collections.sort(infected);
				}
				
				ArrayList<Integer> recovered = getNodesIndexes(indexesofrecovered);
				
				//if no env were chosen, randomly generate some
				ArrayList<Integer> nonhuman = getNodesIndexes(indexesofnonhuman);
				if (nonhuman.size() == 0)
				{
					while (nonhuman.size() < ENV_X_NODES)
					{
						Integer nonHumInd = (int)(Math.random() * n);
						//System.out.println()
						boolean nonHumContains = nonhuman.contains(nonHumInd);
						boolean infectedContains = infected.contains(nonHumInd);
						if (!nonHumContains && !infectedContains)
							nonhuman.add(nonHumInd);
					}
					Collections.sort(nonhuman);
				}
				
				//check to make sure we set the sizes right
				if (infected.size() != INFECT_X_NODES || nonhuman.size() != ENV_X_NODES)
				{
					scan.close();
					if (infected.size() != INFECT_X_NODES)
						throw new Exception("incorrect number of infected");
					//else
					throw new Exception("incorrect number of environment");
				}
				
				//determine that the indexes have been read correctly
				System.out.println("N = " + n + ", Infected indexes: ");
				for (int i = 0; i < infected.size(); i++)
					System.out.print(infected.get(i) + ", ");
				System.out.println();
				System.out.println("recovered indexes: ");
				for (int i = 0; i < recovered.size(); i++)
					System.out.print(recovered.get(i) + ", ");
				System.out.println();
				System.out.println("nonhuman indexes: ");
				for (int i = 0; i < nonhuman.size(); i++)
					System.out.print(nonhuman.get(i) + ", ");
				System.out.println();
				
				//initialize nodes to their states
				Node nodes[] = setNodeStates(n, leader, infected, recovered, nonhuman);
				
				//verify that node names and states were set correctly
				System.out.println("Nodes and states: ");
				for (int i = 0; i < nodes.length; i++)
				{
					Node node = nodes[i];
//					System.out.print("Name: " + node.getNodeName());
					if (node.getNodeState() == NodeState.INFECTED)
						System.out.println("Name: " + node.getNodeName() + " , State == INFECTED");
//					else if (node.getNodeState() == NodeState.SUSCEPTIBLE)
//						System.out.print(" , State == SUSCEPTIBLE");
//					else if (node.getNodeState() == NodeState.RECOVERED)
//						System.out.print(" , State == RECOVERED");
//					else
//						System.out.print(" , State == NONHUMAN");
//					System.out.println();
					else if (node.getNodeState() == NodeState.NONHUMAN)
						System.out.println("Name: " + node.getNodeName() + " , State == ENVIRONMENT");
				}
				
				//add links
				while (scan.hasNextLine())
				{
					String line = scan.nextLine();
					Scanner lineScanner = new Scanner(line);
					int index = lineScanner.nextInt();
					lineScanner.next(); // get rid of the :
					ArrayList<Link> links = new ArrayList<>();
					while (lineScanner.hasNext())
					{
						int destination = lineScanner.nextInt();
						destination--; //since nodes are numbered 1=>n in the input files
						Link edge = new Link(nodes[destination]);
						links.add(edge);
					}
					Node node = nodes[index - 1]; //since nodes are numbered 1 => n
					Link.weightEdges(links, DEFAULT_AGENT_STAY_PROBABILITY);

					node.setEdges(links);
					lineScanner.close();
				}
				scan.close();
	
				//set the number of nodes for the leader process
				leader.setNumNodes(nodes.length);
				
				//start the simulation
				leader.start();
				for (int i = 0; i < nodes.length; i++)
					nodes[i].start();
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
		}
		
		else //use placeholder simulation
		{

			//leader.setRoundLimit(2);
			
			//generate 5 threads
			Node one = new Node(1, NodeState.INFECTED, leader);
			Node two = new Node(2, NodeState.NONHUMAN, leader);
			Node three = new Node(3, NodeState.NONHUMAN, leader);
			Node four = new Node(4, NodeState.NONHUMAN, leader);
			Node five = new Node(5, NodeState.NONHUMAN, leader);
			
		/*	//Make some edges using set probabilities
			Link oneTwo = new Link(0, 0.5, two);
			Link twoThree = new Link(0, 0.5, three);
			Link threeFour = new Link(0, 0.5, four);
			Link fourFive = new Link(0, 0.5, five);
		*/	
			//generate some edges using randomly made probabilities
			Link ltwo = new Link(two);
			Link lthree = new Link(three);
			Link lfour = new Link(four);
			Link lfive = new Link(five);
			
			ArrayList<Link> edges = new ArrayList<>();
			edges.add(ltwo);
			edges.add(lthree);
			edges.add(lfour);
			edges.add(lfive);
			
			Link.weightEdges(edges, DEFAULT_AGENT_STAY_PROBABILITY);
			//verify that it works
			for (int i = 0; i < edges.size(); i++)
			{
				System.out.println("Link " + i +": Edge low: " + edges.get(i).getProbLow() +
						", Edge high: " + edges.get(i).getProbHigh());
			}
		
			//associate edgeLists w/links
			one.setEdges(edges);
			
			//start executing the threads
			leader.setNumNodes(5);
			leader.start();
			one.start();
			two.start();
			three.start();
			four.start();
			five.start();
		}
	
	}
	
	/** Reads a line (param line) and determines which indexes the line
	 * is referring to, and puts them in sorted order into an ArrayList
	 * which is then returned
	 * @param line the line of text to parse
	 * @return indexes a sorted ArrayList of indexes, parsed from line
	 */
	public static ArrayList<Integer> getNodesIndexes(String line)
	{
		ArrayList<Integer> indexes = new ArrayList<>();
		Scanner lineScanner = new Scanner(line);

		while (lineScanner.hasNextInt())
		{
			int next = lineScanner.nextInt();
			if (next < 0)
				break;
			else
				indexes.add(next);
		}
		lineScanner.close();
		
		IntegerComparator sortHelper = new IntegerComparator();
		indexes.sort(sortHelper);
		return indexes;
	}
	
	/** creates n Nodes and initializes their state according to the indexes
	 * specified by infected, recovered, and nonhuman. If it is not specified
	 * according to any of them, the node's state is set to susceptible
	 * PRECONDITION: each node belongs only to one state; arraylists of those
	 * infected, recovered, and nonhuman are in ASCENDING order
	 * @param n the number of nodes
	 * @param leader the leader of the nodes
	 * @param infected a list of indexes corresponding to which nodes are to be infected
	 * @param recovered a list of indexes corresponding to which nodes are to be recovered
	 * @param nonhuman a list of indexes corresponding to which nodes are to be nonhuman
	 * @return nodes[] an array of initialized nodes
	 */
	public static Node[] setNodeStates(int n, Leader leader, ArrayList<Integer> infected, 
			ArrayList<Integer> recovered, ArrayList<Integer> nonhuman)
	{
		Node[] nodes = new Node[n];
		//set up variables to point to which node needs to be infected, recovered, etc
		int infectedInd = -1;
		if (infected.size() > 0)
			infectedInd = 0;
		int recoveredInd = -1;
		if (recovered.size() > 0)
			recoveredInd = 0;
		int nonhumanInd = -1;
		if (nonhuman.size() > 0)
			nonhumanInd = 0;
		
		
		for (int i = 0; i < n; i++) //make n nodes
		{
			NodeState state; 
			//if this node is to be infected
			if (infectedInd >= 0 && infected.get(infectedInd) == (i + 1)) // +1 bc node names start at 1
			{
				state = NodeState.INFECTED;
				if (infectedInd < infected.size() - 1)
					infectedInd++;
				else
					infectedInd = -1;
			}
			else if (recoveredInd >= 0 && recovered.get(recoveredInd) == (i + 1)) // +1 bc node names start at 1
			{
				state = NodeState.RECOVERED;
				if (recoveredInd < recovered.size() - 1)
					recoveredInd++;
				else
					recoveredInd = -1;
			}
			else if (nonhumanInd >= 0 && nonhuman.get(nonhumanInd) == (i + 1)) // +1 bc node names start at 1
			{
				state = NodeState.NONHUMAN;
				if (nonhumanInd < nonhuman.size() - 1)
					nonhumanInd++;
				else
					nonhumanInd = -1;
			}
			else
				state = NodeState.SUSCEPTIBLE; //sus by default
			
			nodes[i] = new Node(i + 1, state, leader);
		}
		return nodes;
	}

}
