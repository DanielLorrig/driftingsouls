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

import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Liste von gaengigen Resourcen sowie einigen Hilfsfunktionen fuer Resourcen-IDs
 * @author Christopher Jung
 *
 */
public class Resources {
	/**
	 * Die Resource Nahrung
	 */
	public static final ResourceID NAHRUNG = new WarenID(0);
	/**
	 * Die Resource Deuterium
	 */
	public static final ResourceID DEUTERIUM = new WarenID(1);
	/**
	 * Die Resource Kunststoffe
	 */
	public static final ResourceID KUNSTSTOFFE = new WarenID(2);
	/**
	 * Die Resource Titan
	 */
	public static final ResourceID TITAN = new WarenID(3);
	/**
	 * Die Resource Uran
	 */
	public static final ResourceID URAN = new WarenID(4);
	/**
	 * Die Resource Antimaterie
	 */
	public static final ResourceID ANTIMATERIE = new WarenID(5);
	/**
	 * Die Resource Adamatium
	 */
	public static final ResourceID ADAMATIUM = new WarenID(6);
	/**
	 * Die Resource Platin
	 */
	public static final ResourceID PLATIN = new WarenID(7);
	/**
	 * Die Resource Silizium
	 */
	public static final ResourceID SILIZIUM = new WarenID(8);
	/**
	 * Die Resource Xentronium
	 */
	public static final ResourceID XENTRONIUM = new WarenID(9);
	/**
	 * Die Resource Erz
	 */
	public static final ResourceID ERZ = new WarenID(10);
	/**
	 * Die Resource Isochips
	 */
	public static final ResourceID ISOCHIPS = new WarenID(11);
	/**
	 * Die Resource Batterien
	 */
	public static final ResourceID BATTERIEN = new WarenID(12);
	/**
	 * Die Resource Leere Batterien
	 */
	public static final ResourceID LBATTERIEN = new WarenID(13);
	/**
	 * Die Resource Antarit
	 */
	public static final ResourceID ANTARIT = new WarenID(14);
	/**
	 * Die Resource Shivanische Artefakte
	 */
	public static final ResourceID SHIVARTE = new WarenID(15);
	/**
	 * Die Resource Artefakte der Uralten
	 */
	public static final ResourceID ANCIENTARTE = new WarenID(16);
	/**
	 * Die Resource Boese Admins
	 */
	public static final ResourceID BOESERADMIN = new WarenID(17);
	/**
	 * Die Spezialresource Items
	 */
	public static final ResourceID ITEMS = new WarenID(18);
	
	/**
	 * Ein Cargo, in dem jede Resource genau einmal vorkommt. Items sind in der Form ohne Questbindung und mit
	 * unbegrenzter Nutzbarkeit vorhanden
	 */
	public static final Cargo RESOURCE_LIST;
	
	static {
		Cargo resList = new Cargo();
		
		for( int i=0; i < Cargo.MAX_RES; i++ ) {
			resList.addResource(new WarenID(i), 1);
		}
		
		for( Item item : Items.get() ) {
			resList.addResource(new ItemID(item.getID()), 1);
		}
		
		RESOURCE_LIST = new UnmodifiableCargo(resList);
	}

	/**
	 * Wandelt einen String in eine Resourcen-ID um.
	 * Es werden sowohl normale Waren alsauch Items beruecksichtigt.
	 * 
	 * @param rid Der String
	 * @return die Resourcen-ID
	 */
	public static ResourceID fromString(String rid) {
		if( rid == null ) {
			return null;
		}
		if( rid.equals("") ) {
			return null;
		}
		ResourceID res = ItemID.fromString(rid);
		if( res != null ) {
			return res;
		}

		return new WarenID(Integer.parseInt(rid));
	}
	
	/**
	 * Gibt die <code>ResourceList</code> via TemplateEngine aus. Ein Item des TemplateBlocks
	 * muss den Namen templateBlock+"item" haben.
	 * 
	 * @param t Das TemplateEngine
	 * @param reslist Die ResourceList
	 * @param templateblock Der Name des betreffenden TemplateBlocks
	 */
	public static void echoResList( TemplateEngine t, ResourceList reslist, String templateblock) {
		echoResList(t,reslist,templateblock,templateblock+"item");
	}
	
	/**
	 * Gibt die <code>ResourceList</code> via TemplateEngine aus
	 * @param t Das TemplateEngine
	 * @param reslist Die ResourceList
	 * @param templateblock Der Name des betreffenden TemplateBlocks
	 * @param templateitem Der Name eines Items des TemplateBlocks
	 */
	public static void echoResList( TemplateEngine t, ResourceList reslist, String templateblock, String templateitem ) {
		t.setVar(templateblock,"");
		
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.image",		res.getImage(),
						"res.cargo",		res.getCargo1(),
						"res.cargo1",		res.getCargo1(),
						"res.cargo2",		res.getCargo2(),
						"res.name",			res.getName(),
						"res.plainname",	res.getPlainName() );
			
			t.parse(templateblock,templateitem,true);
		}
	}
}
