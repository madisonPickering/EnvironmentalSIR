/**Node.java implements a node by extending Thread.java.
 * The fields RECOVERY_THRESHOLD and SANITATION_THRESHOLD
 * can be set to vary those parameters.
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
 * 
 */
package impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.DataFormatException;

import util.Agent;
import util.AgentComparator;
import util.Link;
import util.LinkComparator;
import util.NodeState;
import util.StatBundle;

public class Node extends Thread
{
	private int thisRound; //to make sure no thread skips ahead
	private int name; //"UID"
	private NodeState state;
	private int sickCounter; //rounds after being infected
	private int sanitationCounter; //rounds after last removing agents
	private int thrownAway; //messages (if any) thrown away (if recovered or nonhuman)
	private int msgsSent;
	private final static int RECOVERY_THRESHOLD = 5; //transition to being recovered x rounds after exposure
	private final static int SANITATION_THRESHOLD = 5; //remove agents after x rounds
	private Deque<Agent> inQueue;
	private ArrayList<Agent> agents;			//agents residing w/in this node
	private ArrayList<Link> outEdges;
	private Leader leader;
	
	/**Makes a node
	 * 
	 * @param name the name/id of the node
	 * @param startState the starting state of the node
	 * @param synch the barrier serving as the implementation of rounds
	 */
	public Node(int name, NodeState startState, Leader leader)
	{
		this.name = name;
		
		state = startState;
		if (state != NodeState.NONHUMAN)
			sickCounter = 0;
		else
			sickCounter = -1;
		
		this.leader = leader;
		
		agents = new ArrayList<>();
		//for now, generate a single agent if we are infected
		if (state == NodeState.INFECTED)
			agents.add(new Agent(name));
		
		outEdges = new ArrayList<>();
		inQueue = new ArrayDeque<>();
		thrownAway = 0;
		msgsSent = 0;
		thisRound = 0;
		sanitationCounter = 0;
	}
	
	
	public void setEdges(ArrayList<Link> edges)
	{
		edges.sort(new LinkComparator());
		outEdges = edges;
	}
	
	public ArrayList<Link> getEdges()
	{
		return outEdges;
	}
	
	public int getNodeName()
	{
		return name;
	}
	
	public void setNodeName(int newName)
	{
		name = newName;
	}
	
	public NodeState getNodeState()
	{
		return state;
	}
	
	/** Adds msg to the FIFO queue **/
	public void recieveMessage(Agent msg)
	{
//		System.out.println("(in Node.java, recieveMessage): I am " + name
//				+ " and an agent was recieved");
		inQueue.add(msg);
	}
	
	
	/** Goes through the inqueue and adds those agents to the arraylist agents */
	private void receiveMessages()
	{
		if (inQueue.isEmpty())
			return;
			
		while (!inQueue.isEmpty())
			agents.add(inQueue.poll());
	}
	
	/** transitions and generates messages as needed.
	 * precondition: recieveMessages has already been called for this round
	 * precondition: links are in order (should be done if links were added using setEdges() )
	 */
	private void transition()
	{
		//if we are sus
		if (state == NodeState.SUSCEPTIBLE)
		{
			//if we have no agents && we are sus, do nothing
			if (agents.isEmpty())
				return;
			else
				state = NodeState.INFECTED;
		}
		if (state == NodeState.INFECTED)
		{
			sickCounter++;
			if (sickCounter > RECOVERY_THRESHOLD)
				state = NodeState.RECOVERED;
			//if we are infected and have received no agents, generate one
			else if (agents.isEmpty())
			{
				Agent agent = new Agent(name);
				agents.add(agent);
			}
		}
		if (state == NodeState.RECOVERED)
		{
			//make a note of how many agents were removed
			thrownAway += agents.size();
			agents = new ArrayList<Agent>(); //throw away all received messages
			return;
		}
		if (state == NodeState.NONHUMAN)
		{
			sanitationCounter++;
			//throw out agents, making a note of how many were removed
			if (sanitationCounter > SANITATION_THRESHOLD)
			{
				thrownAway += agents.size();
				agents = new ArrayList<>();
				sanitationCounter = 0;
			}
		}
		//if we are at this point, we are infected or nonhuman. We send agents as needed
		
		//if we have no outgoing edges, each agent we receive stays...
		if (outEdges.size() == 0)
			return;
		
		//associate each agent with a probability, and sort
		for (int i = 0; i < agents.size(); i++)
		{
			double randNum = ThreadLocalRandom.current().nextDouble();
			agents.get(i).setProbability(randNum);
		}
		agents.sort(new AgentComparator());
		
		//clear the msgsSent counter
		msgsSent = 0;
		
		//determine which agent will use which link (similar to merge in mergesort)
		ArrayList<Agent> staying = new ArrayList<>();
		int agentPtr = 0;
		int linkPtr = 0;
		while (agentPtr < agents.size())
		{
			Agent thisAgent = agents.get(agentPtr);
			Link thisLink = outEdges.get(linkPtr);
			double probability = thisAgent.getProbablity();
			if (probability > thisLink.getProbHigh())
			{
				linkPtr++;
				//if no more links, the remaining agents must be staying here
				if (linkPtr >= outEdges.size())
				{
					for (int i = agentPtr; i < agents.size(); i++)
						staying.add(agents.get(i));
					break; //exit the while loop
				}
			}
			else //(probability <= thisLink.getProbHigh())
			{
				agentPtr++;
//				System.out.println("I am " + name + " and I am sending an agent to " + 
//						thisLink.getDestination().getNodeName());
				thisLink.getDestination().recieveMessage(thisAgent);
				msgsSent++;
			}
		}
		agents = staying; //the current list of agents should be only those that are staying
	}
	
	/** reports needed statistics to the leader using a StatBundle **/
	private void reportStats()
	{
		StatBundle report = new StatBundle(name, state, sickCounter,
				agents.size(), thrownAway, msgsSent);
		leader.recieveMessage(report);
	}

	
	@Override
	public void run()
	{
		try
		{
			//check to make sure this node was properly initialized (according to graph topology)
			//verifyInitalization();
			while (leader.continueSimulation())
			{

				receiveMessages();
				transition();
				reportStats();
				thisRound++;

				while (leader.getUniversalRound() < thisRound)
					sleep(50);
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
