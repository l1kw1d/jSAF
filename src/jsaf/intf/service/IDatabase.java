// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

package jsaf.intf.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import jsaf.intf.identity.ICredential;

/**
 * A session service interface for interacting with a relational database.
 *
 * @author David A. Solin
 * @version %I% %G%
 * @since 1.2
 */
public interface IDatabase {
    /**
     * An enumeration of relational ratabase engines. Supplies JDBC URL prefixes for vendor-supplied type-4 drivers,
     * except for MS SQL Server and Sybase which assume use of the jTDS JDBC driver.
     */
    enum Engine {
	DB2("jdbc:db2://", 50000),
	MSSQL("jdbc:jtds:sqlserver://", 1433),
	MYSQL("jdbc:mysql://", 3306),
	ORACLE("jdbc:oracle:thin:@//", 1521),
	POSTGRESQL("jdbc:postgresql://", 5432),
	SYBASE("jdbc:jtds:sybase://", 7100);

	private int port;
	private String prefix;

	private Engine(String prefix, int port) {
	    this.prefix = prefix;
	    this.port = port;
	}

	/**
	 * Return the "prefix" portion of the JDBC connection URL for this engine.
 	 */
	public String getPrefix() {
	    return prefix;
	}

	public int getDefaultPort() {
	    return port;
	}
    }

    /**
     * Get a JDBC connection to the specified database.
     *
     * @param name  Corresponds to the desired Oracle Service Name or DB2/MSSQL/MYSQL/PostgreSQL database instance name.
     * @param cred  The database login credential.
     * @param props Arbitrary JDBC connection properties.
     */
    Connection getConnection(String name, ICredential cred, Properties props) throws SQLException;
}
