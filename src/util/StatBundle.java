/**Simple utility class for passing statistics to the leader
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

public class StatBundle 
{
	private int name; //"UID"
	private NodeState state;
	private int sickCounter;
	private int numAgents;
	private int thrownAway; //the number of agents thrown away (if recovered or env)
	private int msgsSent;
	
	
	public StatBundle(int nodeName, NodeState state, int sickCounter, int numAgents,
			int thrownAway, int msgsSent)
	{
		name = nodeName;
		this.state = state;
		this.sickCounter = sickCounter;
		this.numAgents = numAgents;
		this.thrownAway = thrownAway;
		this.msgsSent = msgsSent;
	}
	
	public int getName()
	{
		return name;
	}
	
	public NodeState getState()
	{
		return state;
	}
	
	public int getSickCounter()
	{
		return sickCounter;
	}
	
	public int getNumAgents()
	{
		return numAgents;
	}
	
	public int getThrownAway()
	{
		return thrownAway;
	}
	
	public int getMsgsSent()
	{
		return msgsSent;
	}
	
	/** needs to be updated **/
	public void printStats()
	{
		System.out.print("Node " + name + " is ");
		switch (state)
		{
		case INFECTED:
			System.out.print("infected, and has been so for " + sickCounter 
					+ "rounds. ");
			break;
		case NONHUMAN:
			System.out.print("nonhuman ");
			break;
		case RECOVERED:
			System.out.print("recovered ");
			break;
		case SUSCEPTIBLE:
			System.out.print("susceptible ");
			break;
		}
		System.out.print("The node has " + numAgents + " agents currently within it\n");
		if (state == NodeState.RECOVERED || state == NodeState.NONHUMAN)
			System.out.println("This node removed " + thrownAway + " agents this round.");
	}
}
