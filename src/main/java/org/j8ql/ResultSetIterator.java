/*
 * Copyright: 2014 Jeremy Chone
 * License: Apache V2 http://www.apache.org/licenses/LICENSE-2.0
 */

package org.j8ql;

import org.postgresql.jdbc4.Jdbc4ResultSetMetaData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;


/**
 * <p>Simple ResultSet Iterator wrapper</p>
 * <p>
 * <p><em>Inspired from http://java.dzone.com/articles/adding-java-8-lambda-goodness</em>, but moved the "close"
 * logic to the CloseableStream which is a better way of handling it.</p>
 */
public class ResultSetIterator implements Iterator<Record> {

	private final ResultSet rs;
	private final DB db;


	private ResultSetColumn[] resultSetColumns;
	private int columnCount;

	public Boolean hasNext = null;
	public Record next = null;

	ResultSetIterator(ResultSet rs, DB db) {
		this.rs = rs;
		this.db = db;
		init();
	}

	private void init() {
		try {
			Jdbc4ResultSetMetaData rsmd = (Jdbc4ResultSetMetaData) rs.getMetaData();
			columnCount = rsmd.getColumnCount();
			resultSetColumns = new ResultSetColumn[columnCount];
			for (int i = 0; i < columnCount; i++) {
				int cidx = i + 1;
				resultSetColumns[i] = new ResultSetColumn(rsmd, cidx);
			}
		} catch (SQLException e) {
			throw new RSQLException(e);
		}
	}

	/**
	 * Note: This hasNext actually does the resultSet.next. Seems dangerous, but apparently there are no other way and libs like JOOQ use the same pattern.
	 *
	 * @return
	 */
	@Override
	public boolean hasNext() {
		// if we do not know there is a next, we try to fetch it
		// and set the flag.
		if (hasNext == null) {
			next = fetchNext();
			hasNext = (next != null);
		}
		return hasNext;
	}

	@Override
	public Record next() {
		Record r = null;
		// hasNext, we consume it and reset the hasNext/next states
		if (hasNext()){
			r = next;
			// we reset the hasNext and next states
			hasNext = null;
			next = null;
		}
		return r;
	}

	public Record fetchNext(){
		Record record = null;
		try {

			if (rs.next()){

				record = new Record();
				for (int i = 0; i < columnCount; i++) {
					int cidx = i + 1;
					Object val = rs.getObject(cidx);
					if (val == null) {
						continue;
					}
					val = db.getJavaVal(val);
					ResultSetColumn cDef = resultSetColumns[i];
					String name = cDef.name;

					// add to the map only if the Name is not already added
					if (!record.containsKey(name)){
						record.put(name, val);
					}
					// TODO: need to add it to the future Record object at the index.
					//if (!Strings.isNullOrEmpty(cDef.baseTableName)){
					//	name = cDef.baseTableName + "." + name;
					//}
				}
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		return record;
	}
}
