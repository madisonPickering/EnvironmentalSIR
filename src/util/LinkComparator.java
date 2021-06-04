/**LinkComparator.java provides a method of comparing two links,
 * which is done based on the values of their respective probOne's
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
import java.util.Comparator;

/** for sorting using the low probability threshold (which should be monotonically
 * increasing) **/
public class LinkComparator implements Comparator<Link>
{
	@Override
	public int compare(Link one, Link two)
	{
		double probOne = one.getProbLow();
		double probTwo = two.getProbLow();
		if (probOne < probTwo)
			return -1;
		if (probOne > probTwo)
			return 1;
		
		return 0;
	}
}