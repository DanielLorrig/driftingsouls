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
package net.driftingsouls.ds2.server.framework.pipeline.reader;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.ReaderPipeline;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Liesst Dateien von der Festplatte und schreibt sie in die Antwort.
 * @author Christopher Jung
 *
 */
public class FileReader implements Reader {
	private static final Log log = LogFactory.getLog(FileReader.class);

	@Override
	public void read(Context context, ReaderPipeline pipeline) throws Exception {
		String filename = pipeline.getFile();
		
		/*
		 * TODO: Die Nutzung von HttpServletResponse.SC_* ist nicht gerade so elegant....
		 */
		
		// Keine Range-Unterstuetzung bis jetzt
		String range = context.getRequest().getHeader("Range");
		if( (range != null) && !"".equals(range) ) {
			context.getResponse().setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			context.getResponse().getWriter().append("416");
			
			return;
		}
		
		String path = Configuration.getAbsolutePath()+filename;
		File file = new File(path);
		if( !file.exists() ) {
			context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
			context.getResponse().getWriter().append("404 - Die von ihnen gesuchte Datei existiert nicht");
			log.warn("Warning: file not found: '"+file+"'");
			
			return;
		}

		if(context.getRequest() instanceof HttpServletRequest) {
			var req = (HttpServletRequest)context.getRequest();
			var servletContext = req.getServletContext();

			var type = servletContext.getMimeType(filename);
			if(type != null) {
				context.getResponse().setContentType(type);
			}
		}
		
		final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		
		context.getResponse().setHeader("Content-Length", ""+file.length() );
		context.getResponse().setContentLength((int)file.length());
		context.getResponse().setHeader("Accept-Ranges", "none" );
		context.getResponse().setHeader("Date", dateFormat.format(new Date()));
		context.getResponse().setHeader("Last-Modified", dateFormat.format( new Date(file.lastModified()) ) );

		try (FileInputStream fin = new FileInputStream(new File(path)))
		{
			IOUtils.copy(fin, context.getResponse().getOutputStream());
		}
		catch (IOException e)
		{
			// Ignorieren, da es sich in den meisten Faellen um einen Browser handelt,
			// der die Verbindung zu frueh dicht gemacht hat
		}
	}
}
