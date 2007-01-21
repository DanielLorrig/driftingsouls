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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Schiffsmodul fuer die SRS-Sensoren
 * @author Christopher Jung
 *
 */
public class SensorsDefault implements SchiffPlugin {
	private int showOnly = 0;
	private int showId = 0;
	
	public String action(Parameters caller) {
		SchiffController controller = caller.controller;
		controller.parameterNumber("showonly");
		controller.parameterNumber("showid");
		controller.parameterString("order");
		
	 	showOnly = controller.getInteger("showonly");
		showId = controller.getInteger("showid");
		
		String order = controller.getString("order");
		if( !order.equals("") ) {
			if( !order.equals("id") && !order.equals("name") && !order.equals("owner") && !order.equals("type") ) {
				order = "id";
			}
			controller.getUser().setUserValue("TBLORDER/schiff/sensororder", order );
		}
		
		return "";
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		SQLResultRow data = caller.ship;
		SQLResultRow datatype = caller.shiptype;
		Offizier offizier = caller.offizier;
		SchiffController controller = caller.controller;
		
		Database db = controller.getDatabase();
		User user = controller.getUser();
		TemplateEngine t = controller.getTemplateEngine();

		int ship = data.getInt("id");
		
		t.set_file("_PLUGIN_"+pluginid, "schiff.sensors.default.html");
		t.set_var(	"global.ship",				ship,
					"global.pluginid",			pluginid,
					"ship.sensors.location",	Ships.getLocationText(data, true),
					"global.awac",				Ships.hasShipTypeFlag(datatype, Ships.SF_SRS_AWAC) );

		int jaegerfleet = -1;

		Map<Integer,SQLResultRow> fleetcache = new HashMap<Integer,SQLResultRow>();
		
		if ( ( datatype.getInt("sensorrange") > 0 ) && ( data.getInt("crew") >= datatype.getInt("crew")/3 ) ) {
			int nebel = Ships.getNebula(data);
			if( (nebel < 3) || (nebel > 5) ) {
				t.set_var("global.longscan",1);
			}
		}

		String order = user.getUserValue("TBLORDER/schiff/sensororder");

		if( ( data.getInt("sensors") > 30 ) && ( data.getInt("crew") >= datatype.getInt("crew") / 4 ) ) {
			t.set_var("global.goodscan",1);
		} else if( data.getInt("sensors") > 0 ) {
			t.set_var("global.badscan",1);
		}

		if( data.getInt("sensors") > 0 ) {
			t.set_var("global.scan",1);
		}

		t.set_block("_SENSORS","bases.listitem","bases.list");
		t.set_block("_SENSORS","battles.listitem","battles.list");
		t.set_block("_SENSORS","sships.listitem","sships.list");
		t.set_block("_SENSORS","sshipgroup.listitem","none");
		t.set_var("none","");
		
		/*
			Asteroiden
			-> Immer anzeigen, wenn die sensoren (noch so gerade) funktionieren
		*/
		if( data.getInt("sensors") > 0 ) {
			SQLQuery base = null;
			if( !order.equals("type") ) {
				base = db.query("SELECT id,owner,name,klasse,size FROM bases WHERE system=",data.getInt("system")," AND FLOOR(SQRT(POW(",data.getInt("x"),"-x,2)+POW(",data.getInt("y"),"-y,2))) <= size ORDER BY ",order,",id");
			}
			else {
				base = db.query("SELECT id,owner,name,klasse,size FROM bases WHERE system=",data.getInt("system")," AND FLOOR(SQRT(POW(",data.getInt("x"),"-x,2)+POW(",data.getInt("y"),"-y,2))) <= size ORDER BY id");
			}
			
			while( base.next() ) {
				SQLQuery datan = base;
				
				t.start_record();
				t.set_var(	"base.id",			datan.getInt("id"),
							"base.owner.id",	datan.getInt("owner"),
							"base.name",		datan.getString("name"),
							"base.klasse",		datan.getInt("klasse"),
							"base.size",		datan.getInt("size"),
							"base.image",		Configuration.getSetting("URL")+"data/starmap/kolonie"+datan.getInt("klasse")+"_srs.png",
							"base.transfer",	(datan.getInt("owner") != 0),
							"base.colonize",	((datan.getInt("owner") == 0) || (datan.getInt("owner") == -1)) && Ships.hasShipTypeFlag(datatype,Ships.SF_COLONIZER),
							"base.action.repair",	0 );

				if( datan.getInt("owner") == user.getID()) {
					t.set_var("base.ownbase",1);
				}

				String owner = Common._title(controller.createUserObject(datan.getInt("owner")).getName());
				if( owner.equals("") ) owner = "-";
				if( datan.getInt("owner") == -1) owner = "verlassen";
				if( !owner.equals("-") && (datan.getInt("owner") != -1) && (datan.getInt("owner") != user.getID()) ) {
					t.set_var("base.pm", 1);
				}
				t.set_var("base.owner.name",owner);

				// Offizier transferieren
				if( datan.getInt("owner") == user.getID() ) {
					if( offizier != null ) {
						t.set_var("base.offiziere.transfer",1);
					} else if( datan.getInt("owner") == user.getID() ) {
						t.set_var("base.offiziere.set",1);
					}
				}

				if( datan.getInt("owner") == user.getID() ) {
					int werft = db.first("SELECT id FROM werften WHERE col=",datan.getInt("id")).getInt("id");
					if( werft != 0 ) {
						//Werftfeld suchen
						Base baseData = new Base(db.first("SELECT * FROM bases WHERE id='",datan.getInt("id"),"'"));
						
						// TODO: Das geht sicherlich auch deutlich schoener.....
						int i=0;
						for( i=0; i < baseData.getBebauung().length; i++ ) {
							if( (baseData.getBebauung()[i] != 0) && Building.getBuilding(db, baseData.getBebauung()[i]).getClass().getName().indexOf("bases.Werft") > -1 ) {
								break;	
							}
						}
						t.set_var(	"base.action.repair",	1,
									"base.werft.field",		i );
					}
				}

				t.parse("bases.list","bases.listitem",true);
				t.stop_record();
				t.clear_record();
			}
			base.free();
		}
		
		//
		// Nebel,Jumpnodes und Schiffe nur anzeigen, wenn genuegend crew vorhanden und die Sensoren funktionsfaehig sind (>30)
		//
		if( ( data.getInt("sensors") > 30 ) && ( data.getInt("crew") >= datatype.getInt("crew") / 4 ) ) {
			/*
				Nebel
			*/

			SQLResultRow nebel = db.first("SELECT id,type FROM nebel WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));

			if( !nebel.isEmpty() ) {
				t.set_var(	"nebel.id",		nebel.getInt("id"),
							"nebel.type",	nebel.getInt("type"),
							"global.ship.deutfactor", (datatype.getInt("deutfactor") != 0 && (nebel.getInt("type") < 3) ));
			}
			
			/*
				Jumpnodes
			*/
	
			SQLResultRow node = db.first("SELECT * FROM jumpnodes WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));
			if( !node.isEmpty() ) {
				int blocked = 0;
				if( node.getBoolean("gcpcolonistblock") && Rassen.get().rasse(user.getRace()).isMemberIn( 0 ) ) {
					blocked = 1;
				}
				if( user.hasFlag( User.FLAG_NO_JUMPNODE_BLOCK ) ) blocked = 0;
							
				t.set_var(	"node.id",		node.getInt("id"),
							"node.name",	node.getInt("name"),
							"node.blocked",	blocked );
			}
			
			/*
				Schlachten
			*/
			SQLQuery battle = db.query("SELECT * FROM battles WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));
			while( battle.next() ) {
				boolean questbattle = false;
				if( (battle.getString("visibility") != null) && (battle.getString("visibility").length() > 0) ) {
					Integer[] visibility = Common.explodeToInteger(",",battle.getString("visibility"));
					if( Common.inArray(user.getID(),visibility) ) {
						questbattle = true;
					}
				}
				int ownAlly = user.getAlly();
				String party1 = null;
				String party2 = null;
				
				if( battle.getInt("ally1") == 0 ) {
					party1 = "<a class=\"profile\" href=\""+Common.buildUrl(controller.getContext(), "default", "module", "userprofile", "user", battle.getInt("commander1"))+"\">"+Common._title(db.first("SELECT name FROM users WHERE id=",battle.getInt("commander1")).getString("name"))+"</a>";
				} 
				else {
					party1 = "<a class=\"profile\" href=\""+Common.buildUrl(controller.getContext(), "default", "module", "allylist", "details", battle.getInt("ally1"))+"\">"+Common._title(db.first("SELECT name FROM ally WHERE id="+battle.getInt("ally1")).getString("name"))+"</a>";
				}
	
				if( battle.getInt("ally2") == 0 ) {
					party2 = "<a class=\"profile\" href=\""+Common.buildUrl(controller.getContext(), "default", "module", "userprofile", "user", battle.getInt("commander2") )+"\">"+Common._title(db.first("SELECT name FROM users WHERE id=",battle.getInt("commander2")).getString("name"))+"</a>";
				} 
				else {
					party2 = "<a class=\"profile\" href=\""+Common.buildUrl(controller.getContext(), "default", "module", "allylist", "details", battle.getInt("ally2"))+"\">"+Common._title(db.first("SELECT name FROM ally WHERE id=",battle.getInt("ally2")).getString("name"))+"</a>";
				}
				boolean fixedjoin = false;
				if( (battle.getInt("commander1") == user.getID()) || 
					(battle.getInt("commander2") == user.getID()) || 
					( (battle.getInt("ally1") > 0) && (battle.getInt("ally1") == ownAlly) ) || 
					( (battle.getInt("ally2") > 0) && (battle.getInt("ally2") == ownAlly) ) ) {
					fixedjoin = true;
				}
				boolean viewable = false;
				if( ((datatype.getInt("class") == ShipClasses.FORSCHUNGSKREUZER.ordinal()) || (datatype.getInt("class") == ShipClasses.AWACS.ordinal())) && !fixedjoin ) {
					viewable = true;
				}
				
				boolean joinable = true;
				if( datatype.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) {
					joinable = false;
				}
					
				int shipcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND battle='",battle.getInt("id"),"'").getInt("count");
					
				t.set_var(	"battle.id",		battle.getInt("id"),
							"battle.party1",	party1,
							"battle.party2",	party2,
							"battle.side1",		Common._stripHTML(party1),
							"battle.side2",		Common._stripHTML(party2),
							"battle.fixedjoin",	fixedjoin,
							"battle.joinable",	joinable,
							"battle.viewable",	viewable,
							"battle.shipcount",	shipcount,
							"battle.quest",		questbattle );
				t.parse("battles.list","battles.listitem",true);
			}
			battle.free();
		
			/*
				Subraumspalten (durch Sprungantriebe)
			*/
	
			SQLResultRow jumps = db.first("SELECT count(*) count FROM jumps WHERE x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system"));
			if( !jumps.isEmpty() ) {
				t.set_var(	"global.jumps",			jumps.getInt("count"),
							"global.jumps.name",	(jumps.getInt("count")>1 ? "Subraumspalten":"Subraumspalte"));
			}
			
			/*
				Schiffe
			*/
			
			boolean superdock = false;
			int user_wrapfactor = Integer.parseInt(user.getUserValue("TBLORDER/schiff/wrapfactor"));
			final int dockCount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='",data.getInt("id"),"'").getInt("count");
			
			if( datatype.getInt("adocks") > dockCount ) {	
				superdock = user.hasFlag( User.FLAG_SUPER_DOCK );
			}
			
			boolean spaceToLand = false;
			int fullcount = db.first("SELECT count(*) fullcount FROM ships WHERE id>0 AND docked='l ",data.getInt("id"),"'").getInt("fullcount");
			if( fullcount + 1 <= datatype.getInt("jdocks") ) {
				spaceToLand = true;
			}
			
			String thisorder = "t1."+order;
			if( order.equals("id") ) {
				thisorder = "myorder";
			}
			
			SQLQuery datas = null;
			boolean firstentry = false;
			Map<String,Integer> types = new HashMap<String,Integer>();
			
			// Soll nur ein bestimmter Schiffstyp angezeigt werden?
			if( this.showOnly != 0 ) { 
				datas = db.query("SELECT t1.id,t1.owner,t1.name,t1.type,t1.crew,t1.e,t1.s,t1.hull,t1.shields,t1.docked,t1.fleet,t1.jumptarget,t1.status,t3.name AS username,t3.ally,t1.battle,IF(t1.docked!='',t1.docked+0.1,t1.id) as myorder ",
									"FROM ships AS t1,users AS t3 ",
								   	"WHERE t1.id!=",ship," AND t1.id>0 AND t1.x=",data.getInt("x")," AND t1.y=",data.getInt("y")," AND t1.system=",data.getInt("system")," AND t1.battle=0 AND (t1.visibility IS NULL OR t1.visibility='",user.getID(),"') AND !LOCATE('l ',t1.docked) AND t1.owner=t3.id AND t1.type=",this.showOnly," AND t1.owner=",this.showId," AND !LOCATE('disable_iff',t1.status) ",
									"ORDER BY ",thisorder,",myorder,fleet");	
				firstentry = true;								
			} 
			else { 
				// wenn wir kein Wrap wollen, koennen wir uns das hier auch sparen
				
				if( user_wrapfactor != 0 ) {
					// herausfinden wieviele Schiffe welches Typs im Sektor sind		
					SQLQuery typesQuery = db.query("SELECT count(*) as menge,type,owner ",
										"FROM ships ",
										"WHERE id!=",ship," AND id>0 AND x=",data.getInt("x")," AND y=",data.getInt("y")," AND system=",data.getInt("system")," AND battle=0 AND (visibility IS NULL OR visibility='",user.getID(),"') AND !LOCATE('disable_iff',status) AND !LOCATE('l ',docked) ",
										"GROUP BY type,owner");
					while( typesQuery.next() ) {
						types.put(typesQuery.getInt("type")+"_"+typesQuery.getInt("owner"), typesQuery.getInt("menge"));
					}
					typesQuery.free();
				}
				datas = db.query("SELECT t1.id,t1.owner,t1.name,t1.type,t1.crew,t1.e,t1.s,t1.hull,t1.shields,t1.docked,t1.fleet,t1.jumptarget,t1.status,t1.oncommunicate,t3.name AS username,t3.ally,t1.battle,IF(t1.docked!='',t1.docked+0.1,t1.id) as myorder ",
									"FROM ships AS t1,users AS t3 ",
									"WHERE t1.id!=",ship," AND t1.id>0 AND t1.x=",data.getInt("x")," AND t1.y=",data.getInt("y")," AND t1.system=",data.getInt("system")," AND t1.battle=0 AND (t1.visibility IS NULL OR t1.visibility='",user.getID(),"') AND !LOCATE('l ',t1.docked) AND t1.owner=t3.id ",
									"ORDER BY ",thisorder,",myorder,fleet");
			}
			
			while( datas.next() ) {
				SQLResultRow ashiptype = Ships.getShipType( datas.getRow() );
				SQLResultRow mastertype = Ships.getShipType( datas.getInt("type"), false );

				// Schiff nur als/in Gruppe anzeigen
				if( (this.showOnly == 0) && (user_wrapfactor != 0) && (mastertype.getInt("groupwrap") != 0) && 
					(types.get(datas.getInt("type")+"_"+datas.getInt("owner")) >= mastertype.getInt("groupwrap")*user_wrapfactor) && 
					(datas.getString("status").indexOf("disable_iff") == -1))  {
					
					int fleetlesscount = 0;
					int ownfleetcount = 0;
					String groupidlist = ""; 				
					if( datas.getInt("owner") == user.getID() ) {
						fleetlesscount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getID(),"' AND type='",datas.getInt("type"),"' AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status) AND fleet=0").getInt("count");
						if( data.getInt("fleet") != 0 ) {
							ownfleetcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getID(),"' AND type='",datas.getInt("type"),"'  AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status) AND fleet=",data.getInt("fleet")).getInt("count");
						}
						groupidlist = db.first("SELECT GROUP_CONCAT(id SEPARATOR '|') grp FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getID(),"' AND type='",datas.getInt("type"),"'  AND !LOCATE('l ',docked) AND !LOCATE('disable_iff',status)").getString("grp");
					}		
					
					t.start_record();
					t.set_var(	"sshipgroup.name",			types.get(datas.getInt("type")+"_"+datas.getInt("owner"))+" x "+mastertype.getString("nickname"),
								"sshipgroup.idlist",		groupidlist,
								"sshipgroup.type.id",		datas.getInt("type"),
								"sshipgroup.owner.id",		datas.getInt("owner"),
								"sshipgroup.owner.name",	Common._title(datas.getString("username")),
								"sshipgroup.type.name",		mastertype.getString("nickname"),
								"sshipgroup.sublist",		0,																		
								"sshipgroup.type.image",	mastertype.getString("picture"),
								"sshipgroup.own",			datas.getInt("owner") == user.getID(),
								"sshipgroup.count",			types.get(datas.getInt("type")+"_"+datas.getInt("owner")) + (data.getInt("type") == datas.getInt("type") ? 1 : 0) - ownfleetcount,
								"sshipgroup.fleetlesscount",	fleetlesscount );
		
					if( datas.getInt("owner") == user.getID() ) {
						t.set_var("sshipgroup.ownship",1);
					} else {
						t.set_var("sshipgroup.ownship",0);
					}
									
					t.parse("sships.list","sshipgroup.listitem",true);
					t.stop_record();
					t.clear_record();									
					types.put(datas.getInt("type")+"_"+datas.getInt("owner"), -1);	// einmal anzeigen reicht
				} 
				else if( (this.showOnly != 0) || (types.get(datas.getInt("type")+"_"+datas.getInt("owner")) != -1) ) {
					if( (this.showOnly != 0) && firstentry ) {
						int count = datas.numRows();		
						
						int fleetlesscount = 0;
						int ownfleetcount = 0;					
						if( datas.getInt("owner") == user.getID() ) {
							fleetlesscount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getID(),"' AND type='",datas.getInt("type"),"' AND docked='' AND fleet=0").getInt("count");
							if( data.getInt("fleet") != 0 ) {
								ownfleetcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND system='",data.getInt("system"),"' AND x='",data.getInt("x"),"' AND y='",data.getInt("y"),"' AND owner='",user.getID(),"' AND type='",datas.getInt("type"),"' AND docked='' AND fleet=",data.getInt("fleet")).getInt("count");
							}
						}	
						
						t.set_var(	"sshipgroup.name",			count+" x "+mastertype.getString("nickname"),
									"sshipgroup.type.id",		datas.getInt("type"),
									"sshipgroup.owner.id",		datas.getInt("owner"),
									"sshipgroup.owner.name", 	Common._title(datas.getString("username")),
									"sshipgroup.type.name",		mastertype.getString("nickname"),
									"sshipgroup.sublist", 		1,																		
									"sshipgroup.type.image",	mastertype.getString("picture"),
									"sshipgroup.own",			datas.getInt("owner") == user.getID(),
									"sshipgroup.count",			count + (data.getInt("type") == datas.getInt("type") ? 1 : 0) - ownfleetcount,
									"sshipgroup.fleetlesscount",	fleetlesscount );
				
						if( datas.getInt("owner") == user.getID() ) {
							t.set_var("sshipgroup.ownship",1);
						} 
						else {
							t.set_var("sshipgroup.ownship",0);
						}			
						t.parse("sships.list","sshipgroup.listitem",true);	
						
						firstentry = false;
					}
					t.start_record();
					t.set_var(	"sships.id",			datas.getInt("id"),
								"sships.owner.id" ,		datas.getInt("owner"),
								"sships.owner.name",	Common._title(datas.getString("username")),
								"sships.name",			Common._plaintitle(datas.getString("name")),
								"sships.type.id",		datas.getInt("type"),
								"sships.hull",			Common.ln(datas.getInt("hull")),
								"sships.shields",		Common.ln(datas.getInt("shields")),
								"sships.fleet.id",		datas.getInt("fleet"),
								"sships.type.name",		ashiptype.getString("nickname"),
								"sships.type.image",	ashiptype.getString("picture"),
								"sships.docked.id",		datas.getString("docked") );

					boolean disableIFF = datas.getString("status").indexOf("disable_iff") > -1;
					t.set_var("sships.disableiff",disableIFF);
		
					if( datas.getInt("owner") == user.getID() ) {
						t.set_var("sships.ownship",1);
					} else {
						t.set_var("sships.ownship",0);
					}

					if( disableIFF ) t.set_var("sships.owner.name","Unbekannt");
		
					if( datas.getInt("fleet") > 0 ) {
						if( !fleetcache.containsKey(datas.getInt("fleet")) ) {
							fleetcache.put(datas.getInt("fleet"), db.first("SELECT * FROM ship_fleets WHERE id="+datas.getInt("fleet")));
						}
						t.set_var("sships.fleet.name",Common._plaintitle(fleetcache.get(datas.getInt("fleet")).getString("name")));
					}
					// Gedockte Schiffe zuordnen (gelandete brauchen hier nicht beruecksichtigt werden, da sie von der Query bereits aussortiert wurden)
					if( !datas.getString("docked").equals("") ) {
						String shipname = db.first("SELECT name FROM ships WHERE id>0 AND id="+datas.getString("docked")).getString("name");
						t.set_var("sships.docked.name",shipname);
					}
					
					// Anzeige Heat (Standard)
					if( Ships.hasShipTypeFlag(datatype, Ships.SF_SRS_EXT_AWAC) ) {
						t.set_var("sships.heat",datas.get("s"));
						
						// Anzeige Heat
						if( datas.getInt("s") == 0 ) {
							t.set_var("sships.heat.none",1);
						}
						if( (datas.getInt("s") > 0) && (datas.getInt("s") <= 100) ) {
							t.set_var("sships.heat.medium",1);
						} else if( datas.getInt("s") > 100 ) {
							t.set_var("sships.heat.hight",1);
						}
		
						// Anzeige Crew
						if( (datas.getInt("crew") == 0) && (ashiptype.getInt("crew") != 0) ) {
							t.set_var("sships.nocrew",1);
						} else if( datas.getInt("crew") > 0 ) {
							t.set_var("sships.crew",datas.get("crew"));
						}
		
						// Anzeige Energie
						if( datas.getInt("e") == 0 ) {
							t.set_var("sships.noe",1);
						} else if( datas.getInt("e") > 0 ) {
							t.set_var("sships.e",datas.get("e"));
						}
					} 
					else if( Ships.hasShipTypeFlag(datatype, Ships.SF_SRS_AWAC) ) {
						t.set_var("global.standartawac",1);
						
						if( datas.getInt("s") > 100 ) {
							t.set_var("sships.heat.high",1);
						} else if( datas.getInt("s") > 40 ) {
							t.set_var("sships.heat.medium",1);
						} else if( datas.getInt("s") > 0 ) {
							t.set_var("sships.heat.low",1);
						} else {
							t.set_var("sships.heat.none",1);
						}
					}

					//Angreifen
					if( !disableIFF && (datas.getInt("owner") != user.getID()) && (datas.getInt("battle")==0) && (datatype.getInt("military") != 0) ) {
						if( ( (user.getAlly() > 0) && (datas.getInt("ally") != user.getAlly()) ) || (user.getAlly() == 0) ) {
							t.set_var("sships.action.angriff",1);
						}
					}

					// Anfunken
					if( datas.getString("oncommunicate") != null && !datas.getString("oncommunicate").equals("") ) {
						boolean found = true;
						if( datas.getString("oncommunicate").indexOf("*:") == -1 ) {
							found = false;
							String[] comlist = datas.getString("oncommunicate").split(";");
							for( int i=0; i < comlist.length; i++ ) {
								String[] comentry = comlist[i].split(":");
								if( Integer.parseInt(comentry[0]) == user.getID() ) {
									found = true;
									break;	
								}	
							}
						}
						else {
							found = true;	
						}
						
						if( found ) {
							t.set_var("sships.action.communicate",1);
						}
					}

					// Springen (Knossosportal)
					if( !datas.getString("jumptarget").equals("") ) {
						/*
							Ermittlung der Sprungberechtigten
							moeglich sind: default,all,user,ally,ownally,group
						 */
						String[] target = datas.getString("jumptarget").split("|");
						String[] targetuser = target[2].split(":");
						if( targetuser[0].equals("all") ) {
							t.set_var("sships.action.jump",1);
						}
						else if( targetuser[0].equals("ally") ) {
							if(  (user.getAlly() > 0) && (Integer.parseInt(targetuser[1]) == user.getAlly()) ) {
								t.set_var("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("user") ) {
							if ( Integer.parseInt(targetuser[1]) == user.getID() ){
								t.set_var("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("ownally") ) {
							if ( (user.getAlly() > 0) && (datas.getInt("ally") == user.getAlly()) ){
								t.set_var("sships.action.jump",1);
							}
						}
						else if( targetuser[0].equals("group") ) {
							String[] userlist = targetuser[1].split(",");
							if( Common.inArray(Integer.toString(user.getID()),userlist) )  {
								t.set_var("sships.action.jump",1);
							}
						}
						else {
							// Default: Selbe Allianz wie der Besitzer oder selbst der Besitzer
							if( ( (user.getAlly() > 0) && (datas.getInt("ally") == user.getAlly()) ) || ((user.getAlly() == 0) && (datas.getInt("owner") == user.getID()) ) ) {
								t.set_var("sships.action.jump",1);
							}
						}
					}

					//Handeln, Pluendernlink, Waren transferieren
					if( datas.getString("status").indexOf("tradepost") != -1 ) {
						t.set_var("sships.action.trade",1);
					} 
					else if( !disableIFF && (datas.getInt("owner") == -1) && (ashiptype.getInt("class") == ShipClasses.SCHROTT.ordinal() || ashiptype.getInt("class") == ShipClasses.FELSBROCKEN.ordinal()) ) {
						t.set_var("sships.action.transferpluender",1);
					} 
					else if( !disableIFF || (datas.getInt("owner") == user.getID()) ) {
						t.set_var("sships.action.transfer",1);
					}

					//Bemannen, Kapern
					if( !disableIFF && (datas.getInt("owner") != user.getID()) && (ashiptype.getInt("class") != ShipClasses.GESCHUETZ.ordinal()) &&
						((datas.getInt("owner") != -1) || (ashiptype.getInt("class") == ShipClasses.SCHROTT.ordinal() || ashiptype.getInt("class") == ShipClasses.FELSBROCKEN.ordinal())) ) {
						if( ( (user.getAlly() > 0) && (datas.getInt("ally") != user.getAlly()) ) || (user.getAlly() == 0) ) {
							if( !Ships.hasShipTypeFlag(ashiptype, Ships.SF_NICHT_KAPERBAR) ) {
								t.set_var("sships.action.kapern",1);
							}
							else {
								t.set_var("sships.action.pluendern",1);
							}
						}
					} else if( !disableIFF && (datas.getInt("owner") == user.getID()) && (ashiptype.getInt("crew") > 0)  ) {
						t.set_var("sships.action.crewtausch",1);
					}

					//Offiziere: Captain transferieren
					boolean hasoffizier = datas.getString("status").indexOf("offizier") != -1;
					if( !disableIFF && (offizier != null) && (!hasoffizier || Ships.hasShipTypeFlag(ashiptype, Ships.SF_OFFITRANSPORT) ) ) {
						if( ashiptype.getInt("size") > 2 ) {
							boolean ok = true;
							if( Ships.hasShipTypeFlag(ashiptype, Ships.SF_OFFITRANSPORT) ) {
								int officount = db.first("SELECT count(*) count FROM offiziere WHERE dest='s "+datas.getInt("id")+"'").getInt("count");
								
								if( officount >= ashiptype.getInt("crew") ) {
									ok = false;
								}
							}
							
							if( ok ) {
								t.set_var("sships.action.tcaptain",1);
							}
						}
					}

					//Schiff in die Werft fliegen
					if( (datas.getInt("owner") == user.getID()) && !ashiptype.getString("werft").equals("") ) {
						t.set_var("sships.action.repair",1);
					}

					//Externe Docks: andocken
					if( ( datatype.getInt("adocks") > dockCount ) && ( (datas.getInt("owner") == user.getID() ) || superdock) ) {
						if( superdock || ( ashiptype.getInt("size") < 3 ) ) {
							t.set_var("sships.action.aufladen",1);
						}
					}

					//Jaegerfunktionen: laden, Flotte landen
					if( Ships.hasShipTypeFlag(datatype, Ships.SF_JAEGER) ) {
						if( ( ashiptype.getInt("jdocks") > 0 ) && ( datas.getInt("owner") == user.getID() ) ) {
							if( fullcount + 1 <= ashiptype.getInt("jdocks") ) {
								t.set_var("sships.action.land",1);
								if( data.getInt("fleet") > 0 ) {
									List<Integer> fleetlist = new ArrayList<Integer>();
									
									boolean ok = true;
									if( jaegerfleet == -1) {
										SQLQuery tmp = db.query("SELECT id,type,status FROM ships WHERE id>0 AND fleet='"+data.getInt("fleet")+"'");
										while( tmp.next() ) {
											SQLResultRow tmptype = Ships.getShipType( tmp.getRow() );
											if( !Ships.hasShipTypeFlag(tmptype, Ships.SF_JAEGER) ) {
												ok = false;
												break;
											}
											fleetlist.add(tmp.getInt("id"));
										}		
										tmp.free();
										if( ok ) jaegerfleet = 1;
										else jaegerfleet = 0;
									}

									if( (jaegerfleet == 1) && (fleetlist.size() <= ashiptype.getInt("jdocks")) ) {
										if( fullcount + fleetlist.size() <= ashiptype.getInt("jdocks") )
											t.set_var(	"sships.action.landfleet", 1,
														"global.shiplist", Common.implode("|",fleetlist) );
									}
								}
							}
						}
					}
				
					//Aktuellen Jaeger auf dem (ausgewaehlten) Traeger laden lassen
					if( (datas.getInt("owner") == user.getID()) && spaceToLand && Ships.hasShipTypeFlag(ashiptype, Ships.SF_JAEGER) ) {
						t.set_var("sships.action.landthis",1);
					}

					//Flottenfunktionen: anschliessen
					if( datas.getInt("owner") == user.getID() ) {
						if( (data.getInt("fleet") <= 0) && (datas.getInt("fleet")>0) ) {
							t.set_var("sships.action.joinfleet",1);
						}
						else if( (data.getInt("fleet") > 0) && (datas.getInt("fleet") <= 0) ) {
							t.set_var("sships.action.add2fleet",1);
						}
						else if( (datas.getInt("fleet")<=0) && (data.getInt("fleet")<=0) ) {
							t.set_var("sships.action.createfleet",1);
						}
					}
					t.parse("sships.list","sships.listitem",true);
					t.stop_record();
					t.clear_record();
				}
			}
			datas.free();
		}
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
