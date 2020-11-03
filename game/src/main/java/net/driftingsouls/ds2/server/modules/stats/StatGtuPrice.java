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
package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.statistik.StatGtu;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Zeigt die Liste hoechsten Gebote (welche zur Ersteigerung fuehrten) in der GTU.
 * @author Christopher Jung
 *
 */
@Service
public class StatGtuPrice extends AbstractStatistic implements Statistic {
	@PersistenceContext
	private EntityManager em;

	private final BBCodeParser bbCodeParser;
	private final ShipService shipService;

	public StatGtuPrice(BBCodeParser bbCodeParser, ShipService shipService) {
        super(bbCodeParser);
		this.bbCodeParser = bbCodeParser;
		this.shipService = shipService;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();

		String url = getUserURL();

		Writer echo = getContext().getResponse().getWriter();

		echo.append("<h1>Die höchsten Gebote:</h1>");
		echo.append("<table class='stats'>\n");

		int a = 1;

		List<StatGtu> gebote = em.createQuery("from StatGtu order by preis desc", StatGtu.class)
			.setMaxResults(size)
			.getResultList();

		for( StatGtu gebot : gebote )
		{
			String name = null;

			if( gebot.getMType() == 1 ) {
				ShipTypeData shiptype = shipService.getShipType(Integer.parseInt(gebot.getType()));
				name = "<a class=\"forschinfo\" onclick='ShiptypeBox.show("+gebot.getType()+");return false;' href=\"./ds?module=schiffinfo&ship="+gebot.getType()+"\">"+shiptype.getNickname()+"</a>";
			}
			else if( gebot.getMType() == 2 ) {
				Cargo mycargo = new Cargo( Cargo.Type.AUTO, gebot.getType() );
				ResourceEntry resource = mycargo.getResourceList().iterator().next();

				name = ( resource.getCount1() > 1 ? resource.getCount1()+"x " : "" )+Cargo.getResourceName(resource.getId());
			}

	   		echo.append("<tr><td>").append(Integer.toString(a)).append(".</td>\n");
	   		User user = em.find(User.class, gebot.getUserId());
	   		if( user != null )
	   		{
	   			echo.append("<td><a class=\"profile\" href=\"").append(url).append(Integer.toString(user.getId())).append("\">").append(Common._title(bbCodeParser, user.getName())).append(" (").append(Integer.toString(user.getId())).append(")</a></td>");
	   		}
	   		else
	   		{
	   			echo.append("<td>").append(Common._title(bbCodeParser, gebot.getUsername())).append(" (").append(Integer.toString(gebot.getUserId())).append(")</td>");
		   	}
	   		echo.append("<td>").append(name).append("</td>\n");
			echo.append("<td><span class=\"nobr\">").append(Common.ln(gebot.getPrice())).append(" RE</span></td></tr>\n");

			a++;
		}

		echo.append("</table>\n");
	}
}
