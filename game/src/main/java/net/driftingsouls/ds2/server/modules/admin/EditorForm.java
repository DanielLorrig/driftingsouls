package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContent;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.modules.admin.editoren.HtmlUtils;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Klasse zum Erstellen eines Eingabeformulars.
 */
class EditorForm implements AutoCloseable
{
	private final StringBuilder echo;
	private final Class<? extends AdminPlugin> plugin;
	private final List<CustomFieldGenerator> fields = new ArrayList<>();

	EditorForm(Class<? extends AdminPlugin> plugin, StringBuilder echo)
	{
		this.echo = echo;
		this.plugin = plugin;
	}

	/**
	 * Standardinterface fuer Feldgeneratoren. Jede Instanz generiert genau
	 * ein Feld fuer ein konkretes Form.
	 */
	public interface CustomFieldGenerator
	{
		/**
		 * Generiert den HTML-Code fuer das Eingabefeld.
		 * @param echo Der Writer in den der HTML-Code geschrieben werden soll
		 */
        void generate(StringBuilder echo);
	}

	/**
	 * Fuegt einen Generator fuer ein Eingabefeld zum Form hinzu.
	 * @param generator Der Generator
	 * @param <T> Der Typ des Generators
	 * @return Der Generator
	 */
	public <T extends CustomFieldGenerator> T custom(T generator)
	{
		fields.add(generator);
		return generator;
	}

	public class DynamicContentFieldGenerator implements CustomFieldGenerator
	{
		private final String label;
		private final String name;
		private final String value;
		private boolean withRemove;

		DynamicContentFieldGenerator(String label, String name, String value)
		{
			this.label = label;
			this.name = name;
			this.value = value;
			this.withRemove = false;
		}

		@Override
		public void generate(StringBuilder echo) {
			echo.append("<tr class='dynamicContentEdit'>");

			writeCommonDynamicContentPart(label, name, value);

			if( withRemove )
			{
				String entityId = ContextMap.getContext().getRequest().getParameter("entityId");

				echo.append("<td><a title='entfernen' href='./ds?module=admin&amp;namedplugin=").append(plugin.getName()).append("&amp;entityId=").append(entityId).append("&reset=").append(name).append("'>X</a>");
			}

			echo.append("</tr>");
		}

		@SuppressWarnings("UnusedReturnValue")
		DynamicContentFieldGenerator withRemove()
		{
			this.withRemove = true;
			return this;
		}

		private void writeCommonDynamicContentPart(String label, String name, String value) {
			echo.append("<td>").append(label).append(": </td>").append("<td>").append(value != null && !value.trim().isEmpty() ? "<img src='" + value + "' />" : "").append("</td>").append("<td>");

			DynamicContent content = Objects.requireNonNull(DynamicContentManager.lookupMetadata(value != null ? value : "dummy", true));

			echo.append("<input type=\"file\" name=\"").append(name).append("\">");

			echo.append("<table>");
			echo.append("<tr><td>Lizenz</td><td><select name='").append(name).append("_dc_lizenz'>");
			echo.append("<option>Bitte wählen</option>");
			for (DynamicContent.Lizenz lizenz : DynamicContent.Lizenz.values())
			{
				echo.append("<option value='").append(lizenz.name()).append("' ").append(content.getLizenz() == lizenz ? "selected='selected'" : "").append(">").append(lizenz.getLabel()).append("</option>");
			}
			echo.append("</select></tr>");
			echo.append("<tr><td>Lizenzdetails</td><td><textarea rows='3' cols='30' name='").append(name).append("_dc_lizenzdetails'>").append(content.getLizenzdetails()).append("</textarea></td></tr>");
			echo.append("<tr><td>Quelle</td><td><input type='text' maxlength='255' name='").append(name).append("_dc_quelle' value='").append(content.getQuelle()).append("'/></td></tr>");
			echo.append("<tr><td>Autor (RL-Name+Nick)</td><td><input type='text' maxlength='255' name='").append(name).append("_dc_autor' value='").append(content.getAutor()).append("'/></td></tr>");
			echo.append("</table>");

			echo.append("</td>\n");
		}
	}

	/**
	 * Generiert ein Eingabefeld (Editor) fuer ein via {@link net.driftingsouls.ds2.server.framework.DynamicContent}
	 * gemanagetes Bild.
	 * @param label Der Label fuer das Eingabefeld
	 * @param name Der interne Name
	 * @param value Der momentane Wert des Felds
	 * @return Der erzeugte Generator
	 */
	public DynamicContentFieldGenerator dynamicContentField(String label, String name, String value)
	{
		return custom(new DynamicContentFieldGenerator(label, name, value));
	}

	public static class LabelGenerator implements CustomFieldGenerator
	{
		private final String label;
		private final Object value;

		LabelGenerator(String label, Object value)
		{
			this.label = label;
			this.value = value;
		}

		@Override
		public void generate(StringBuilder echo) {
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>").append("<td>").append(value != null ? value.toString() : "").append("</td></tr>\n");
		}
	}

	/**
	 * Erzeugt ein Eingabefeld (Editor) in Form eines nicht editierbaren Werts.
	 * @param label Der Label zum Wert
	 * @param value Der nicht editierbare Wert
	 */
	public LabelGenerator label(String label, Object value)
	{
		return custom(new LabelGenerator(label, value));
	}

	public class FieldGenerator implements CustomFieldGenerator
	{
		private final String label;
		private final String name;
		private final Class<?> type;
		private final Object value;
		private final Map<Object, Object> selectionOptions = new LinkedHashMap<>();

		FieldGenerator(String label, String name, Class<?> type, Object value)
		{
			this.label = label;
			this.name = name;
			this.type = type;
			this.value = value;

			if( type.isAnnotationPresent(Entity.class) )
			{
				this.selectionOptions.putAll(generateSelectionOptions(type));
			}
		}

		public FieldGenerator withNullOption(String label)
		{
			this.selectionOptions.put(null, label);
			return this;
		}

		@Override
		public void generate(StringBuilder echo) {
			echo.append("<tr>");
			echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
			echo.append("<td>");
			if (Cargo.class.isAssignableFrom(type))
			{
				echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id='").append(name).append("' value=\"").append(value != null ? value.toString() : new Cargo().toString()).append("\">");
				echo.append("<script type='text/javascript'>$(document).ready(function(){new CargoEditor('#").append(name).append("')});</script>");
			}
			else if (type.isAnnotationPresent(Entity.class) || !this.selectionOptions.isEmpty() )
			{
				editEntityBySelection(name, type, value);
			}
			else if (Boolean.class.isAssignableFrom(type))
			{
				boolean bool = false;
				if (value != null)
				{
					bool = (Boolean) value;
				}
				echo.append("<input type=\"checkbox\" name=\"").append(name).append("\" value=\"true\" ").append(bool ? "checked='checked'" : "").append(" \">");
			}
			else
			{
				HtmlUtils.textInput(echo, name, false, type, value);
			}
			echo.append("</td></tr>\n");
		}

		private Map<Serializable, Object> generateSelectionOptions(Class<?> entityClass)
		{
			Map<Serializable,Object> result = new LinkedHashMap<>();
			org.hibernate.Session db = ContextMap.getContext().getDB();

			List<?> editities = Common.cast(db.createCriteria(entityClass).list());
			for (Object entity : editities)
			{
				Serializable identifier = db.getIdentifier(entity);
				result.put(identifier, entity);
			}
			return result;
		}

		private void editEntityBySelection(String name, Class<?> type, Object value) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Serializable selected = -1;
			if (type.isInstance(value) && type.isAnnotationPresent(Entity.class))
			{
				selected = db.getIdentifier(value);
			}
			else if (value instanceof Serializable)
			{
				selected = (Serializable) value;
			}

			HtmlUtils.select(echo, name, false, this.selectionOptions, selected);
		}
	}

	/**
	 * Erzeugt ein Eingabefeld (Editor) fuer einen bestimmten Datentyp. Das konkret erzeugte Eingabefeld
	 * kann von Datentyp zu Datentyp unterschiedlich sein.
	 * @param label Das Anzeigelabel
	 * @param name Der interne Name
	 * @param type Der Datentyp
	 * @param value Der momentane Wert
	 */
	public FieldGenerator field(String label, String name, Class<?> type, Object value)
	{
		return custom(new FieldGenerator(label, name, type, value));
	}

	@Override
	public void close()
	{
		for (CustomFieldGenerator field : fields)
		{
			field.generate(echo);
		}

		echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
		echo.append("</table>");
		echo.append("</form>\n");
		echo.append("</div>");
	}
}
