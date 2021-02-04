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

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Zeigt das Hauptmenue des KS an.
 *
 * @author Christopher Jung
 */
@Component
public class KSMenuDefaultAction extends BasicKSMenuAction {
    private final KSMenuAttackAction attackAction;
    private final KSEndBattleEqualAction endBattleEqualAction;
    private final KSLeaveSecondRowAction leaveSecondRowAction;
    private final KSHijackAction hijackAction;
    private final KSMenuGroupAttackAction groupAttackAction;
    private final KSSecondRowAttackAction secondRowAttackAction;
    private final KSSecondRowAction secondRowAction;
    private final KSSecondRowEngageAction engageAction;

    public KSMenuDefaultAction(BattleService battleService, KSMenuAttackAction attackAction, KSEndBattleEqualAction endBattleEqualAction,
        KSLeaveSecondRowAction leaveSecondRowAction, KSHijackAction hijackAction, KSMenuGroupAttackAction groupAttackAction, KSSecondRowAttackAction secondRowAttackAction, KSSecondRowAction secondRowAction, KSSecondRowEngageAction engageAction) {
        super(battleService, null);
        this.attackAction = attackAction;
        this.endBattleEqualAction = endBattleEqualAction;
        this.leaveSecondRowAction = leaveSecondRowAction;
		this.hijackAction = hijackAction;
        this.groupAttackAction = groupAttackAction;
        this.secondRowAttackAction = secondRowAttackAction;
        this.secondRowAction = secondRowAction;
        this.engageAction = engageAction;
    }

    @Override
    public Result execute(TemplateEngine t, Battle battle) throws IOException {
        Result result = super.execute(t, battle);
        if (result != Result.OK) {
            return result;
        }

        BattleShip ownShip = battle.getOwnShip();
        BattleShip enemyShip = battle.getEnemyShip();

        if (this.isPossible(battle, attackAction) == Result.OK) {
            this.menuEntry(t, "Angriff", "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "attack");
        }

        if (this.isPossible(battle, groupAttackAction) == Result.OK) {
            this.menuEntry(t, "Gruppen-Angriff", "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "groupattack");
        }

        this.menuEntry(t, "Flucht", "ship", ownShip.getId(),
            "attack", enemyShip.getId(),
            "ksaction", "flucht");


        // Kampf beenden weil die eigene Streitmacht deutlich groesser ist?
        if (this.isPossible(battle, endBattleEqualAction) == Result.OK) {
            this.menuEntryAsk(t, "Durchbrechen",
                new Object[]{"ship", ownShip.getId(),
                    "attack", enemyShip.getId(),
                    "ksaction", "endbattleequal"},
                "Wollen Sie mit so vielen Schiffen wie möglich die Schlachtlinien durchbrechen?");
        }


        // Kapern?
        if (this.isPossible(battle, hijackAction) == Result.OK) {
            this.menuEntry(t, "Kapern",
                "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "kapern");
        }

        // Die zweiten Reihe stuermen
        if (this.isPossible(battle, secondRowAttackAction) == Result.OK) {
            this.menuEntry(t, "Sturmangriff",
                "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "secondrowattack");
        }

        // Zur zweiten Reihe vorruecken
        if (this.isPossible(battle, engageAction) == Result.OK) {
            this.menuEntry(t, "Vorr&uuml;cken",
                "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "secondrowengage");
        }

        // Zweite Reihe verlassen
        if (this.isPossible(battle, leaveSecondRowAction) == Result.OK) {
            this.menuEntry(t, "zweite Reihe verlassen",
                "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "leavesecondrow");
        }

        // Zweite Reihe
        if (this.isPossible(battle, secondRowAction) == Result.OK) {
            this.menuEntry(t, "in zweite Reihe verlegen",
                "ship", ownShip.getId(),
                "attack", enemyShip.getId(),
                "ksaction", "secondrow");
        }

        // Naechstes eigenes Schiff
        if (battle.getOwnShipCount() > 1) {
            this.menuEntry(t, "n&auml;chstes eigenes Schiff",
                "ship", battle.getNextOwnBattleShip().getId(),
                "attack", enemyShip.getId(),
                "ksaction", "default");
        }

        // Naechstes feindliches Schiff
        if (battle.getEnemyShipCount() > 1) {
            this.menuEntry(t, "n&auml;chstes feindliches Schiff",
                "ship", ownShip.getId(),
                "attack", battle.getNextEnemyBattleShip().getId(),
                "ksaction", "default");
        }

        menuEntry(t, "Sonstiges", "ship", ownShip.getId(),
            "attack", enemyShip.getId(),
            "ksaction", "other");

        return Result.OK;
    }
}
