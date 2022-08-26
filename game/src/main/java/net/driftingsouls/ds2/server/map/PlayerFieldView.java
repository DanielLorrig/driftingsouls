package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.jooq.Records;
import org.jooq.impl.DSL;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.driftingsouls.ds2.server.entities.jooq.tables.Ally.ALLY;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Battles.BATTLES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Jumpnodes.JUMPNODES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Jumps.JUMPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipFleets.SHIP_FLEETS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Users.USERS;

/**
 * Eine Sicht auf ein bestimmtes Sternenkartenfeld.
 * Die Sicht geht davon aus, dass der Spieler das Feld sehen darf.
 * Es findet aus Performancegruenden keine(!) Abfrage ab, um das sicherzustellen.
 *
 * @author Drifting-Souls Team
 */
public class PlayerFieldView implements FieldView
{
	private final User user;
	private final Location location;
	private final PublicStarmap starmap;
	private final EntityManager em;

    /**
	 * Legt eine neue Sicht an.
	 *
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param position Der gesuchte Sektor.
     * @param starmap Die Sternenkarte des Systems.
	 */
	public PlayerFieldView(User user, Location position, PublicStarmap starmap, EntityManager em)
	{
		this.user = user;
        this.location = position;
		this.starmap = starmap;
		this.em = em;
	}

	/**
	 * Gibt die Liste aller Basen in dem Feld zurueck.
	 * @return Die Basenliste
	 */
	@Override
	public List<StationaryObjectData> getBases()
	{
        try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var select = db.select(BASES.ID, BASES.NAME, BASES.OWNER, USERS.NICKNAME, BASE_TYPES.LARGEIMAGE, BASE_TYPES.ID, BASE_TYPES.NAME)
				.from(BASES)
				.join(USERS)
				.on(BASES.OWNER.eq(USERS.ID))
				.join(BASE_TYPES)
				.on(BASES.KLASSE.eq(BASE_TYPES.ID))
				.where(BASES.STAR_SYSTEM.eq(location.getSystem())
					.and(BASES.X.eq(location.getX()))
					.and(BASES.Y.eq(location.getY())));

			if(isNotScanned()) {
				//TODO: Select bases of friends and allies
				select = select.and(BASES.OWNER.eq(user.getId()));
			}

			return select.fetch(Records.mapping(StationaryObjectData::new));

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	private boolean isNotScanned()
	{
		return !starmap.isScanned(location);
	}

	/**
	 * @return Die Schiffe, die der Spieler sehen kann.
	 */
	@Override
	public Map<UserData, Map<ShipTypeData, List<ShipData>>> getShips()
	{
		Map<UserData, Map<ShipTypeData, List<ShipData>>> ships = new TreeMap<>();
        if(isNotScanned())
        {
            return ships;
        }

		int minSize;
		if(!starmap.ownShipSectors.contains(location) && !starmap.allyShipSectors.contains(location) && getNebel() != null) {
			minSize = getNebel().getMinScansize();
		} else {
			minSize = 0;
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);
			var select = db.select(SHIPS.ID, SHIPS.NAME, SHIPS.OWNER, USERS.RACE, SHIPS.E, SHIPS.S, SHIPS.DOCKED, SHIPS.SENSORS, SHIPS.FLEET, SHIP_FLEETS.NAME, SHIP_TYPES.ID, SHIP_TYPES.NICKNAME, SHIP_TYPES.PICTURE, SHIP_TYPES.SIZE, SHIP_TYPES.JDOCKS, SHIP_TYPES.ADOCKS, SHIP_TYPES.EPS, SHIP_TYPES.COST, SHIP_TYPES.SENSORRANGE, USERS.NICKNAME)
				.from(SHIPS)
				.join(SHIP_TYPES)
				.on(SHIPS.TYPE.eq(SHIP_TYPES.ID))
				.join(USERS)
				.on(SHIPS.OWNER.eq(USERS.ID))
				.leftJoin(SHIP_FLEETS)
				.on(SHIPS.FLEET.eq(SHIP_FLEETS.ID))
				.where(SHIPS.STAR_SYSTEM.eq(location.getSystem())
					.and(SHIPS.X.eq(location.getX()))
					.and(SHIPS.Y.eq(location.getY())));

			for(var row: select.fetch()) {
				//TODO: Compute landed and docked ships
				var ship = new ShipData(row.get(SHIPS.ID), row.get(SHIPS.NAME), row.get(SHIPS.OWNER), row.get(USERS.RACE), 0, 0, row.get(SHIPS.E), row.get(SHIPS.S), row.get(SHIPS.DOCKED), row.get(SHIPS.SENSORS), row.get(SHIPS.FLEET), row.get(SHIP_FLEETS.NAME));
				var typeData = new ShipTypeData(row.get(SHIP_TYPES.ID), row.get(SHIP_TYPES.NICKNAME), row.get(SHIP_TYPES.PICTURE), row.get(SHIP_TYPES.SIZE), row.get(SHIP_TYPES.JDOCKS), row.get(SHIP_TYPES.ADOCKS), row.get(SHIP_TYPES.EPS), row.get(SHIP_TYPES.COST), row.get(SHIP_TYPES.SENSORRANGE));
				var userData = new UserData(row.get(SHIPS.OWNER), row.get(USERS.NICKNAME) , row.get(USERS.RACE));

				//TODO: Handle the whole "cannot see enemies if sector not scanned, has nebula and ships are small" thing

				ships.computeIfAbsent(userData, data -> new TreeMap<>())
					.computeIfAbsent(typeData, data -> new ArrayList<>())
					.add(ship);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return ships;
	}

	@Override
	public List<NodeData> getJumpNodes()
	{
		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var condition = JUMPNODES.STAR_SYSTEM.eq(location.getSystem())
				.and(JUMPNODES.X.eq(location.getX()))
				.and(JUMPNODES.Y.eq(location.getY()));
			if(isNotScanned()) {
				condition = condition.and(JUMPNODES.HIDDEN.eq(0));
			}

			var select = db.select(JUMPNODES.ID, JUMPNODES.NAME, JUMPNODES.GCPCOLONISTBLOCK)
				.from(JUMPNODES)
				.where(condition);

			return select.fetch().map(Records.mapping(NodeData::new));

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getJumpCount()
	{
		if(isNotScanned())
		{
			return 0;
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);


			@SuppressWarnings("ConstantConditions")
			int count = db.select(DSL.count()).from(JUMPS).where(
					JUMPS.STAR_SYSTEM.eq(location.getSystem())
					.and(JUMPS.X.eq(location.getX()))
					.and(JUMPS.Y.eq(location.getY()))
				)
				.fetchOne(DSL.count());
			return count;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<BattleData> getBattles()
	{
		if(isNotScanned())
		{
			return new ArrayList<>();
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var attacker = USERS.as("attacker");
			var defender = USERS.as("defender");
			var attackerAlly = ALLY.as("attacker_ally");
			var defenderAlly = ALLY.as("defender_ally");

			var select = db.select(BATTLES.ID,
					attacker.RACE, attacker.ID, attacker.NICKNAME, attacker.PLAINNAME, attackerAlly.ID, attackerAlly.NAME, attackerAlly.PLAINNAME,
					defender.RACE, defender.ID, defender.NICKNAME, defender.PLAINNAME, defenderAlly.ID, defenderAlly.NAME, defenderAlly.PLAINNAME)
				.from(BATTLES)
				.join(attacker)
				.on(BATTLES.COMMANDER1.eq(attacker.ID))
				.join(defender)
				.on(BATTLES.COMMANDER2.eq(defender.ID))
				.leftJoin(attackerAlly)
				.on(attacker.ALLY.eq(defenderAlly.ID))
				.leftJoin(defenderAlly)
				.on(defender.ALLY.eq(defenderAlly.ID))
				.where(BATTLES.STAR_SYSTEM.eq(location.getSystem())
					.and(BATTLES.X.eq(location.getX()))
					.and(BATTLES.Y.eq(location.getY())));

			return select.fetch(Records.mapping(BattleData::new));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<StationaryObjectData> getBrocken()
	{
		if( isNotScanned() || !starmap.hasRocks(location) )
		{
			return new ArrayList<>();
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var rockSelect = db.select(SHIPS.ID, SHIPS.NAME, SHIPS.OWNER, USERS.NICKNAME, SHIP_TYPES.PICTURE, SHIPS.TYPE, SHIP_TYPES.NICKNAME)
				.from(SHIPS)
				.join(SHIP_TYPES)
				.on(SHIP_TYPES.CLASS.eq(ShipClasses.FELSBROCKEN.ordinal()).and(SHIP_TYPES.ID.eq(SHIPS.TYPE)))
				.join(USERS)
				.on(SHIPS.OWNER.eq(USERS.ID))
				.where(
					SHIPS.STAR_SYSTEM.eq(location.getSystem())
					.and(SHIPS.X.eq(location.getX()))
					.and(SHIPS.Y.eq(location.getY())));

			return rockSelect.fetch(Records.mapping(StationaryObjectData::new));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isRoterAlarm()
	{
		return starmap.isRoterAlarmImSektor(location);
	}

	@Override
	public Nebel.Typ getNebel()
	{
		return starmap.getNebula(location);
	}

	public Location getLocation()
	{
		return location;
	}
}
