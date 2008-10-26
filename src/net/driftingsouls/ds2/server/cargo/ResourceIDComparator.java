/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.cargo;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Vergleichsklasse fuer Resourcen-IDs
 * @author Christopher Jung 
 *
 */
class ResourceIDComparator implements Comparator<ResourceID>, Serializable
{
	private static final long serialVersionUID = 3588397636540631609L;
	
	private boolean descending;
	
	ResourceIDComparator(boolean descending)
	{
		this.descending = descending;
	}
	
	public int compare(ResourceID o1, ResourceID o2)
	{
		int val = compareEntries(o1, o2);
		if( descending )
		{
			val = -val;
		}
		return val;
	}
	
	private int compareEntries(ResourceID id1, ResourceID id2)
	{
		// Falls es Waren sind...
		if( !id1.isItem() && !id2.isItem() )
		{
			return id1.getID() > id2.getID() ? 1 : (id1.getID() == id2.getID() ? 0 : -1);
		}
		
		// Items...
		if( id1.isItem() && id2.isItem() )
		{
			// IDs vergleichen
			if( id1.getItemID() != id2.getItemID() )
			{
				return id1.getItemID() > id2.getItemID() ? 1 : -1;
			}
			// Quests vergleichen
			if( id1.getQuest() != id2.getQuest() )
			{
				return id1.getQuest() > id2.getQuest() ? 1 : -1;
			}
			// Benutzungen vergleichen
			return id1.getUses() > id2.getUses() ? 1 : (id1.getUses() == id2.getUses() ? 0 : -1);
		}
		
		// Einmal Ware und einmal Item...
		return id1.isItem() ? 1 : -1;
	}
}
