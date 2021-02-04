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
package net.driftingsouls.ds2.server.modules.ks;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Zeigt das Menue fuer sonstige Aktionen an, welche unter keine andere Kategorie fallen.
 * @author Christopher Jung
 *
 */
@Component
public class KSMenuOtherAction extends BasicKSMenuAction {
	private final ConfigService configService;
	private final KSMenuUndockAction undock;
	private final KSMenuShieldsAction shields;
	private final KSActivateAR enableRedAlert;
	private final KSDeactivateAR disableRedAlert;

	public KSMenuOtherAction(BattleService battleService, JavaSession javaSession, ConfigService configService, KSMenuUndockAction undock, KSMenuShieldsAction shields, KSActivateAR enableRedAlert, KSDeactivateAR disableRedAlert) {
		super(battleService, (User)javaSession.getUser());
		this.configService = configService;
		this.undock = undock;
		this.shields = shields;
		this.enableRedAlert = enableRedAlert;
		this.disableRedAlert = disableRedAlert;
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();

		//Alle Abdocken
		if( this.isPossible(battle, undock) == Result.OK ) {
			menuEntry(t, "Abdocken",
						"ship",		ownShip.getId(),
						"attack",	enemyShip.getId(),
						"ksaction",	"undock" );
		}

		//Schilde aufladen
		if( this.isPossible(battle, shields) == Result.OK ) {
			menuEntry(t, "Schilde aufladen",
						"ship",			ownShip.getId(),
						"attack",		enemyShip.getId(),
						"ksaction",		"shields" );
		}

        if( this.isPossible(battle, enableRedAlert) == Result.OK) {
            menuEntry(t, "Alarm Rot aktivieren",
                        "ship",     ownShip.getId(),
                        "attack",   enemyShip.getId(),
                        "ksaction", "activatear" );
        }

        if( this.isPossible(battle, disableRedAlert) == Result.OK) {
            menuEntry(t, "Alarm Rot deaktivieren",
                    "ship",     ownShip.getId(),
                    "attack",   enemyShip.getId(),
                    "ksaction", "deactivatear" );
        }

		//Kampf uebergeben
		menuEntry(t, "Kampf &uuml;bergeben",	"ship",		ownShip.getId(),
											"attack",	enemyShip.getId(),
											"ksaction",	"new_commander" );

		//History
		menuEntry(t, "Logbuch",	"ship",		ownShip.getId(),
								"attack",	enemyShip.getId(),
								"ksaction",	"history" );

		menuEntry(t, "zur&uuml;ck",	"ship",		ownShip.getId(),
									"attack",	enemyShip.getId() );
		
		return Result.OK;
	}
}
