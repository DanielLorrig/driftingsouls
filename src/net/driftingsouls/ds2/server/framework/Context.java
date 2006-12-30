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
package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;

/**
 * Repraesentiert einen Kontext. Bei einem Kontext handelt es sich um einen
 * Aufruf des Systems z.B. ueber HTTP. Die mit dem Aufruf verbundenen Daten koennen
 * hier abgerufen werden. Zudem kann die Ausgabe hier getaetigt werden.
 * Zudem verfuegt der Kontext noch ueber Caches fuer diverse Dinge, die direkt
 * an einen Aufruf gebunden sein muessen und nicht dauerhaft gecached werden koennen.
 * 
 * @author Christopher Jung
 *
 */
public interface Context {
	/**
	 * Gibt einen gecachetes User-Objekt zur?ck
	 * 
	 * @param id Die ID des Users
	 * @return Das User-Objekt bzw null
	 */
	public abstract User getCachedUser( int id );
	
	/**
	 * Erstellt ein neues User-Objekt sofern nicht bereits ein gecachtes vorhanden ist.
	 * Wenn ein neues User-Objekt erstellt wird, werden die in prepare angegebenen Attribute
	 * aus der Datenbank geholt
	 * 
	 * @param id Die ID des zu erstellenden Users
	 * @param prepare Die aus der Datenbank zu ladenden Attribute
	 * @return Das User-Objekt
	 */
	@Deprecated
	public abstract User createUserObject(int id, String ... prepare);
	
	/**
	 * Erstellt ein neues User-Objekt sofern nicht bereits ein gecachtes vorhanden ist.
	 * 
	 * @param id Die ID des zu erstellenden Users
	 * @return Das User-Objekt
	 */
	public abstract User createUserObject(int id);

	/**
	 * Erstellt einen UserIterator fuer eine Datenbank-Query
	 * 
	 * @param query Die Query
	 * @return Ein UserIterator
	 */
	public abstract UserIterator createUserIterator( Object ... query );
	
	
	/**
	 * Liefert eine Instanz der Datenbank-Klasse zur?ck
	 * 
	 * @return Eine Database-Instanz
	 */
	public abstract Database getDatabase();

	/**
	 * Liefert den gerade aktiven User
	 * 
	 * @return Das zum gerade aktiven User gehoerende User-Objekt
	 */
	public abstract User getActiveUser();

	/**
	 * Setzt den gerade aktiven User auf das angebene User-Objekt
	 * 
	 * @param user Der neue aktive User
	 */
	public abstract void setActiveUser(User user);

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu
	 * 
	 * @param error Die Beschreibung des Fehlers
	 */
	public abstract void addError(String error);

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu und bietet zudem eine Ausweich-URL an.
	 * 
	 * @param error Die Beschreibung des Fehlers
	 * @param link Die Ausweich-URL
	 */
	public abstract void addError(String error, String link);

	/**
	 * Liefert den letzten Fehler zurueck
	 * 
	 * @return Der letzte Fehlers
	 * 
	 * @see #addError(String, String)
	 * @see #addError(String)
	 */
	public abstract Error getLastError();

	/**
	 * Liefert eine Liste aller Fehler zurueck
	 * 
	 * @return Eine Liste aller Fehlerbeschreibungen 
	 */
	public abstract Error[] getErrorList();
	
	/**
	 * Cached ein User-Objekt
	 * 
	 * @param userobj Das zu cachende User-Objekt
	 */
	public abstract void cacheUser( User userobj );
	
	/**
	 * Liefert die Request fuer diesen Aufruf
	 * @return Die Request des Aufrufs
	 */
	public abstract Request getRequest();
	
	/**
	 * Liefert die zum Aufruf gehoerende Response
	 * @return Die Response des Aufrufs
	 */
	public abstract Response getResponse();
	
	/**
	 * Liefert eine pro Kontext einmalige Instanz einer Klasse.
	 * Sollte keine Instanz dieser Klasse im Kontext vorhanden sein,
	 * wird dieses erstellt.
	 * Hinweis: Die Klasse muss die Annotation ContextInstance besitzen
	 * und auf den Wert SINGLETON gesetzt haben
	 * 
	 * @param <T> Eine Klasse, welche Kontextbezogen arbeiten kann
	 * @param cls Die gewuenschte Klasse
	 * @return Eine Instanz der Klase
	 */
	public abstract <T> T get(Class<T> cls);
	
	/**
	 * Gibt die zum Kontext gehoerende Session-ID zurueck.
	 * Sollte keine Session-ID vorliegen, wird ein leerer String
	 * (nicht <code>null</code>!) zurueckgegeben.
	 * 
	 * @return Die Session-ID oder ein leerer String
	 */
	public abstract String getSession();
	
	/**
	 * Setzt eine Kontext-lokale Variable auf einen angegebenen Wert.
	 * @param cls Die Klasse, welche die Variable setzen moechte - fungiert als zusaetzlicher Schluessel
	 * @param varname Der Name der Variablen
	 * @param value Der neue Wert der Variablen
	 */
	public abstract void putVariable(Class<?> cls, String varname, Object value);
	
	/**
	 * Liefert eine Kontext-lokale Variable zurueck
	 * @param cls Die Klasse, welche die Variable abrufen moechte - fungiert als zusaetzlicher Schluessel
	 * @param varname Der Name der Variablen
	 * @return Die Variable oder <code>null</code>, falls die Variable nicht existiert
	 */
	public abstract Object getVariable(Class<?> cls, String varname);
	
	/**
	 * Ueberprueft, ob eine neue Session gesetzt wurde und authentifiziert ggf den Benutzer.
	 * Die Liste der Fehler wird in dem Fall zurueckgesetzt. 
	 *
	 */
	public abstract void revalidate();
}