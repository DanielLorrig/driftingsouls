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
package net.driftingsouls.ds2.server.bbcodes;

import java.util.Iterator;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeFunction;

/**
 * BBCode fuer Resourcen
 * @author Christopher Jung
 *
 */
public class TagResource implements BBCodeFunction {

	public String handleMatch(String content, String... values) {
		Context context = ContextMap.getContext();
		
		try {
			long count = 0;
			if( content.length() > 0 ) {
				count = Long.parseLong(content);
			}
			ResourceID rid = Resources.fromString(values[0]);
			String format = "in";
			if( values.length > 1 ) {
				format = values[1];
			}
			
			
			if( rid.isItem() ) {			
				String unknstr = "Unbekannter Gegenstand";
				if( count != 0 ) {
					unknstr = Common.ln(count)+"x "+unknstr;
				}
				
				Item item = Items.get().item(rid.getItemID());
				
				if( item == null ) {
					return unknstr;	
				}
				
				User user = (User)context.getActiveUser();
				if( (user == null) && (item.getAccessLevel() > 0 || item.isUnknownItem() )) {
					return unknstr;
				}
				else if( user != null ){
					if( item.getAccessLevel() > user.getAccessLevel() ) {		
						return unknstr;
					}
				
					if( item.isUnknownItem() && !user.isKnownItem(item.getID()) && (user.getAccessLevel() < 15) ) {
						return unknstr;
					}
				}	
			}
	
			Cargo cargo = new Cargo();
			if( count != 0 ) {
				cargo.addResource(rid, count);
			}
			else {
				cargo.addResource(rid,1);
			}
			cargo.setOption( Cargo.Option.SHOWMASS, false );
	
			StringBuilder tmpString = new StringBuilder(30);
			
			ResourceList reslist = cargo.getResourceList();
			Iterator<ResourceEntry> iter = reslist.iterator();
			if( iter.hasNext() ) {
				ResourceEntry res = iter.next();
		
				if( count != 0 ) {
					tmpString.append(Common.ln(count));
					tmpString.append("x ");
				}
		
				if( format.indexOf('i') != -1 ) {
					tmpString.append("<img align=\"middle\" border=\"0\" src=\"");
					tmpString.append(res.getImage());
					tmpString.append("\" alt=\"\" />");
				}
		
				if( format.indexOf('n') != -1 ) {
					tmpString.append(res.getName());
				}
			}
			else {
				tmpString.append("Fehlerhafte Resource '"+rid+"'");
			}

			return tmpString.toString();
		}
		catch( NumberFormatException e ) {
			return "<span style=\"color:red\">Ungueltiger Resourcen-Tag: [resource="+Common.implode(",", values)+"]"+content+"[/resource]</span>";
		}
	}

}
