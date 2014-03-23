package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

import java.io.IOException;
import java.io.Writer;

/**
 * Ein Tool, um die diversen globalen Konfigurationswerte zu aendern.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Sonstiges", name = "Configwerte editieren")
public class EditConfigValues implements AdminPlugin
{
	@Override
	@SuppressWarnings("unchecked")
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();

		WellKnownConfigValue[] configValues = WellKnownConfigValue.values();
		ConfigService configService = new ConfigService();
		
		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		if(update)
		{
			for(WellKnownConfigValue value: configValues)
			{
				ConfigValue configValue = configService.get(value);
				String newValue = context.getRequest().getParameterString(value.getName());
				configValue.setValue(newValue);
			}
		}

		echo.append("<div class='gfxbox' style='width:790px'>");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"").append(page).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"").append(Integer.toString(action)).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<table width=\"100%\">");
		for(WellKnownConfigValue value: configValues)
		{
			Object configServiceValue = configService.getValue(value);
			echo.append("<tr><td>").append(value.getName()).append("</td>");
			if( Number.class.isAssignableFrom(value.getType()) )
			{
				echo.append("<td><input type=\"text\" name=\"").append(value.getName()).append("\" value=\"");
				echo.append(configServiceValue != null ? configServiceValue.toString() : "").append("\" /></td>");
			}
			else if( Boolean.class.isAssignableFrom(value.getType()) )
			{
				echo.append("<td><input type=\"checkbox\" name=\"").append(value.getName());
				echo.append("\" value=\"true\" ").append(Boolean.TRUE.equals(configServiceValue) ? "checked=\"checked\"" : "").append(" /></td>");
			}
			else
			{
				echo.append("<td><textarea name=\"").append(value.getName()).append("\" rows=\"2\" cols=\"30\">");
				echo.append(configServiceValue != null ? configServiceValue.toString() : "").append("</textarea></td>");
			}
			echo.append("<td>").append(value.getDescription()).append("</td></tr>");
		}
		echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
		echo.append("</table>");
		echo.append("</div>");
	}
}
