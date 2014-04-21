package net.driftingsouls.ds2.server.modules.admin.editoren;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * Adapter fuer {@link net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor}-Module
 * an das Admin-Pluginsystem.
 * @param <T> Der Typ der bearbeiteten Entity
 */
public class EditPlugin8<T> implements AdminPlugin
{
	private EntityEditor<T> entityEditor;
	private Class<? extends T> clazz;
	private Class<T> baseClass;

	public EditPlugin8(EntityEditor<T> entityEditor)
	{
		this.entityEditor = entityEditor;
		this.clazz = entityEditor.getEntityType();
		this.baseClass = entityEditor.getEntityType();
	}

	@Override
	public final void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		Request request = context.getRequest();
		String entityId = request.getParameter("entityId");

		EditorForm8<T> form = new EditorForm8<>(EditorMode.UPDATE, getPluginClass(), echo);
		configureFor(form);
		this.clazz = form.getDefaultEntityClass().orElse(this.clazz);

		if(this.isDeleteExecuted())
		{
			executeDelete(echo, form);

			entityId = null;
		}
		else if (this.isUpdateExecuted())
		{
			try
			{
				@SuppressWarnings("unchecked") T entity = (T) db.get(this.clazz, toEntityId(this.clazz, entityId));
				if ( entity != null && form.isUpdateAllowed(entity) && isUpdatePossible(entity))
				{
					db.evict(entity);
					@SuppressWarnings("unchecked") T updatedEntity = (T) db.get(this.clazz, toEntityId(this.clazz, entityId));
					form.applyRequestValues(request, updatedEntity);
					processJobs(echo, entity, updatedEntity, form.getUpdateTasks());
					echo.append("<p>Update abgeschlossen.</p>");
				}
			}
			catch (IOException | RuntimeException e)
			{
				echo.append("<p>Fehler bei Update: ").append(e.getMessage());
			}
		}
		else if (this.isAddExecuted())
		{
			executeAdd(echo);

			entityId = null;
		}
		else if (this.isResetExecuted())
		{
			try
			{
				@SuppressWarnings("unchecked") T entity = (T) db.get(this.clazz, toEntityId(this.clazz, entityId));
				reset(new DefaultStatusWriter(echo), entity);
				echo.append("<p>Update abgeschlossen.</p>");
			}
			catch (IOException | RuntimeException e)
			{
				echo.append("<p>Fehler bei Reset: ").append(e.getMessage()).append("</p>");
			}
		}

		outputEntitySelection(db, echo, form);

		if (entityId != null && !entityId.isEmpty())
		{
			@SuppressWarnings("unchecked") T entity = (T) db.get(clazz, toEntityId(this.clazz, entityId));
			if (entity == null)
			{
				return;
			}

			beginEditorTable(echo, this.getPluginClass(), entityId);
			form.generateForm(entity);
			endEditorTable(echo);
		}
		else if( isAddDisplayed() )
		{
			T entity = createEntity();
			form = new EditorForm8<>(EditorMode.CREATE, this.getPluginClass(), echo);
			configureFor(form);
			beginEditorTable(echo, this.getPluginClass(), -1);
			form.generateForm(entity);
			endEditorTable(echo);
		}
		else
		{
			outputEntityTable(echo, form);
		}
	}

	private void executeAdd(final StringBuilder echo)
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		Request request = context.getRequest();

		db.getTransaction().commit();

		new SingleUnitOfWork("Hinzufuegen") {
			@Override
			public void doWork() throws Exception
			{
				EditorForm8<T> form = new EditorForm8<>(EditorMode.CREATE, EditPlugin8.this.getPluginClass(), echo);
				configureFor(form);

				T entity = createEntity();
				form.applyRequestValues(request, entity);
				EditPlugin8.this.clazz = form.getEntityClassRequestValue(request, entity);
				if( entity.getClass() != EditPlugin8.this.clazz )
				{
					// Klasse wurde geaendert - erneut anwenden
					entity = createEntity();
					form.applyRequestValues(request, entity);
				}
				db.persist(entity);
				echo.append("<p>Hinzufügen abgeschlossen.</p>");
			}
		}.setErrorReporter((uow,w,t) -> echo.append("<p>Fehler bei Update: ").append(t.getMessage()).append("</p>"))
				.execute();

		db.beginTransaction();
	}

	private void executeDelete(final StringBuilder echo, EditorForm8<T> form) throws IOException
	{
		Context context = ContextMap.getContext();
		Session db = context.getDB();

		Request request = context.getRequest();
		String entityId = request.getParameter("entityId");

		@SuppressWarnings("unchecked") T entity = (T) db.get(this.clazz, toEntityId(this.clazz, entityId));
		if( entity != null && form.isDeleteAllowed(entity) )
		{
			processJobs(echo, entity, entity, form.getDeleteTasks());

			db.getTransaction().commit();

			new SingleUnitOfWork("Hinzufuegen")
			{
				@Override
				public void doWork() throws Exception
				{
					//noinspection unchecked
					T deletedEntity = (T) db.get(EditPlugin8.this.clazz, toEntityId(EditPlugin8.this.clazz, request.getParameter("entityId")));
					db.delete(deletedEntity);
					echo.append("<p>Löschen abgeschlossen.</p>");
				}
			}.setErrorReporter((uow,w,t) -> echo.append("<p>Fehler beim Loeschen: ").append(t.getMessage()).append("</p>"))
					.execute();

			db.getTransaction().begin();
		}
	}

	private void outputEntityTable(StringBuilder echo, EditorForm8<T> form)
	{
		Session db = ContextMap.getContext().getDB();

		JqGridViewModel model = new JqGridViewModel();
		model.url = "./ds?module=admin&namedplugin="+this.getPluginClass().getName()+"&action=tableData&FORMAT=JSON";
		model.pager = "#pager";
		model.colNames.add("Id");
		model.colModel.add(new JqGridColumnViewModel("id", null));
		Class identifierClass = db.getSessionFactory().getClassMetadata(this.clazz).getIdentifierType().getReturnedClass();
		if( Number.class.isAssignableFrom(identifierClass) )
		{
			model.colModel.get(0).width = 50;
		}
		for (ColumnDefinition columnDefinition : form.getColumnDefinitions())
		{
			model.colNames.add(columnDefinition.getLabel());
			model.colModel.add(new JqGridColumnViewModel(columnDefinition.getId(), columnDefinition.getFormatter()));
		}

		echo.append("<script type='text/javascript'>\n");
		echo.append("Admin.createEntityTable(\n");
		echo.append(new Gson().toJson(model));
		echo.append(");\n</script>");
		echo.append("<div id='entityListWrapper'>");
		echo.append("<table id='entityList'><tr><td></td></tr></table>");
		echo.append("<div id='pager'></div>");
		echo.append("</div>");
	}

	private void outputEntitySelection(Session db, StringBuilder echo, EditorForm8<T> form) throws IOException
	{
		echo.append("<div class='gfxbox adminSelection' style='width:390px'>");
		Long count = (Long)db.createCriteria(baseClass).setProjection(Projections.rowCount()).uniqueResult();
		if( count != null )
		{
			beginSelectionBox(echo, this.getPluginClass());

			if (count < 500)
			{
				List<Building> entities = Common.cast(db.createCriteria(baseClass).list());
				echo.append("<select size=\"1\" name=\"entityId\">");
				for (Object entity : entities)
				{
					addSelectionOption(echo, db.getIdentifier(entity), new ObjectLabelGenerator().generateFor(null, entity));
				}
				if (isAddDisplayed())
				{
					addSelectionOption(echo, null, "[Neu]");
				}
				echo.append("</select>");
			}
			else
			{
				String currentIdStr = ContextMap.getContext().getRequest().getParameter("entityId");
				if( currentIdStr == null )
				{
					currentIdStr = "";
				}
				HtmlUtils.textInput(echo, "entityId", isAddDisplayed(), db.getSessionFactory().getClassMetadata(baseClass).getIdentifierType().getReturnedClass(), currentIdStr);
			}

			endSelectionBox(echo);
		}
		if (form.isAddAllowed())
		{
			addForm(echo, this.getPluginClass());
		}
		echo.append("</div>");
	}

	private Serializable toEntityId(Class<?> entity, String idString)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Class<?> targetType = db.getSessionFactory().getClassMetadata(entity).getIdentifierType().getReturnedClass();
		return (Serializable) StringToTypeConverter.convert(targetType, idString);
	}

	private void processJobs(StringBuilder echo, T entity, T updatedEntity, List<Job<T, ?>> updateTasks) throws IOException
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		db.getTransaction().commit();

		for (final Job<T, ?> updateTask : updateTasks)
		{
			@SuppressWarnings("unchecked") final Job<T, Object> updateTask1 = (Job<T, Object>) updateTask;
			Collection<Object> jobData = updateTask1.supplier.apply(updatedEntity);
			new EvictableUnitOfWork<Object>(updateTask.name) {
				@Override
				public void doWork(Object object) throws Exception
				{
					updateTask1.job.accept(entity, updatedEntity,object);
				}
			}.setFlushSize(10).executeFor(jobData);

			echo.append(updateTask.name);
			if( jobData.size() > 1 )
			{
				echo.append(": ").append(Integer.toString(jobData.size())).append(" Objekte aktualisiert");
			}
			echo.append("<br />");
		}

		db.getTransaction().begin();
	}

	private void endEditorTable(StringBuilder echo) throws IOException
	{
		echo.append("</table>");
		echo.append("</form>\n");
		echo.append("</div>");
	}

	private T createEntity()
	{
		try
		{
			Constructor<? extends T> constructor = this.clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		}
		catch (NoSuchMethodException e)
		{
			throw new AssertionError("Kein default-Konstruktor fuer Entity '"+this.clazz.getName()+"' vorhanden");
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e)
		{
			throw new IllegalStateException("Konnte Entity '"+this.clazz.getName()+"' nicht instantiieren");
		}
	}

	protected final @Nonnull Session getDB()
	{
		return ContextMap.getContext().getDB();
	}

	protected void reset(StatusWriter writer, T entity) throws IOException
	{
		// TODO
	}

	protected boolean isUpdatePossible(@Nonnull T entity)
	{
		return true;
	}

	private void beginSelectionBox(StringBuilder echo, Class<?> plugin) throws IOException
	{
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(plugin.getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
	}

	private void addSelectionOption(StringBuilder echo, Object id, String label) throws IOException
	{
		Context context = ContextMap.getContext();
		String currentIdStr = context.getRequest().getParameter("entityId");
		String idStr = id != null ? id.toString() : null;
		boolean selektiert = idStr != null ? idStr.equals(currentIdStr) : null == currentIdStr;

		echo.append("<option value=\"").append(idStr).append("\" ").append(selektiert ? "selected=\"selected\"" : "").append(">").append(label).append("</option>");
	}

	private void endSelectionBox(StringBuilder echo) throws IOException
	{
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
	}

	private void addForm(StringBuilder echo, Class<?> plugin) throws IOException
	{
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(plugin.getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"submit\" name=\"add\" value=\"+\" />");
		echo.append("</form>");
	}

	private void beginEditorTable(final StringBuilder echo, Class<?> plugin, Object entityId) throws IOException
	{
		echo.append("<div class='gfxbox adminEditor' style='width:700px'>");
		echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(plugin.getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"hidden\" name=\"entityId\" value=\"").append(entityId != null ? entityId.toString() : "").append("\" />\n");

		echo.append("<table>");
	}

	protected final boolean isResetted(String name)
	{
		Context context = ContextMap.getContext();
		String reset = context.getRequest().getParameterString("reset");
		return name.equals(reset);
	}

	private boolean isResetExecuted()
	{
		Context context = ContextMap.getContext();
		String reset = context.getRequest().getParameterString("reset");
		return reset != null && !reset.trim().isEmpty();
	}

	private boolean isUpdateExecuted()
	{
		Context context = ContextMap.getContext();
		String change = context.getRequest().getParameterString("change");
		return "Aktualisieren".equals(change);
	}

	private boolean isAddExecuted()
	{
		Context context = ContextMap.getContext();
		String change = context.getRequest().getParameterString("change");
		return "Hinzufügen".equals(change);
	}

	private boolean isDeleteExecuted()
	{
		Context context = ContextMap.getContext();
		String change = context.getRequest().getParameterString("change");
		return "Löschen".equals(change);
	}

	private boolean isAddDisplayed()
	{
		Context context = ContextMap.getContext();
		String change = context.getRequest().getParameterString("add");
		return !change.trim().isEmpty();
	}

	protected final User getActiveUser()
	{
		return (User)ContextMap.getContext().getActiveUser();
	}

	public JqGridTableDataViewModel generateTableData(int page, int rows)
	{
		EditorForm8<T> form = new EditorForm8<>(EditorMode.UPDATE, this.getPluginClass(), new StringBuilder());
		configureFor(form);

		if( page <= 0 )
		{
			page = 1;
		}

		Number rowCount = (Number)getDB().createCriteria(baseClass).setProjection(Projections.rowCount()).uniqueResult();

		JqGridTableDataViewModel model = new JqGridTableDataViewModel();
		model.page = page;
		model.records = rowCount != null ? rowCount.intValue() : 0;
		model.total = rowCount != null ? (rowCount.intValue()-1)/rows+1 : 1;

		List<T> entities = Common.cast(getDB()
				.createCriteria(baseClass)
				.setFirstResult((page - 1) * rows)
				.setMaxResults(rows)
				.list());

		for (T entity : entities)
		{
			JqGridRowDataViewModel rowModel = new JqGridRowDataViewModel(getDB().getIdentifier(entity).toString(), form.getEntityValues(entity));
			rowModel.cell.add(0, rowModel.id);
			model.rows.add(rowModel);
		}

		return model;
	}

	protected void configureFor(@Nonnull EditorForm8<T> form)
	{
		this.entityEditor.configureFor(form);
	}

	protected Class<?> getPluginClass()
	{
		return entityEditor.getClass();
	}
}
