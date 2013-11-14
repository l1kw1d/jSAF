// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

package jsaf.intf.system;

import java.io.File;
import java.io.IOException;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.util.ILoggable;
import jsaf.intf.util.IProperty;

/**
 * A representation of a session.
 *
 * @author David A. Solin
 * @version %I% %G%
 * @since 1.0
 */
public interface ISession extends ILoggable {
    /**
     * An enumeration of possible session types.
     *
     * @since 1.0
     */
    enum Type {
	/**
	 * An SSH-type session. This type is only intended to be a transition state used during a discovery process, that
	 * will ultimately yield a type of UNIX, CISCO_IOS, JUNIPER_JUNOS or UNKNOWN.
	 *
	 * @since 1.0
	 */
	SSH("ssh"),

	/**
	 * Indicates a session with a Unix host.
	 *
	 * @see jsaf.intf.unix.system.IUnixSession
	 *
	 * @since 1.0
	 */
	UNIX("unix"),

	/**
	 * Indicates a session with a device running Cisco IOS.
	 *
	 * @see jsaf.intf.cisco.system.IIosSession
	 *
	 * @since 1.0
	 */
	CISCO_IOS("ios"),

	/**
	 * Indicates a session with a device running Juniper JunOS.
	 *
	 * @see jsaf.intf.juniper.system.IJunosSession
	 *
	 * @since 1.0
	 */
	JUNIPER_JUNOS("junos"),

	/**
	 * Indicates a session with a device running Apple iOS.
	 *
	 * @see jsaf.intf.apple.system.IiOSSession
	 *
	 * @since 1.0
	 */
	APPLE_IOS("apple_iOS"),

	/**
	 * Indicates a session with a Windows host.
	 *
	 * @see jsaf.intf.windows.system.IWindowsSession
	 *
	 * @since 1.0
	 */
	WINDOWS("windows"),

	/**
	 * Indicates that the session type cannot be determined.
	 *
	 * @since 1.0
	 */
	UNKNOWN("unknown");

	private String s;

	Type(String s) {
	    this.s = s;
	}

	/**
	 * Get the Type's corresponding String value.
	 *
	 * @since 1.0
	 */
	public String value() {
	    return s;
	}

	/**
	 * Given a String value, obtain a corresponding Type.
	 *
	 * @since 1.0
	 */
	public static Type typeOf(String s) {
	    for (Type t : values()) {
		if (t.s.equals(s)) {
		    return t;
		}
	    }
	    return UNKNOWN;
	}
    }

    /**
     * An enumeration of timeouts.
     *
     * @since 1.0
     */
    public enum Timeout {
	/**
	 * A short timeout.
	 *
	 * @since 1.0
	 */
	S,

	/**
	 * A medium-sized timeout.
	 *
	 * @since 1.0
	 */
	M,

	/**
	 * A long timeout.
	 *
	 * @since 1.0
	 */
	L,

	/**
	 * An extra-long timeout.
	 *
	 * @since 1.0
	 */
	XL;
    }

    /**
     * A constant defining the String "localhost".
     *
     * @since 1.0
     */
    String LOCALHOST = "localhost";

    /**
     * Property key used to define a "small" amount of time.
     *
     * @since 1.0
     */
    String PROP_READ_TIMEOUT_S = "read.timeout.small";

    /**
     * Property key used to define a "medium" amount of time.
     *
     * @since 1.0
     */
    String PROP_READ_TIMEOUT_M = "read.timeout.medium";

    /**
     * Property key used to define a "large" amount of time.
     *
     * @since 1.0
     */
    String PROP_READ_TIMEOUT_L = "read.timeout.large";

    /**
     * Property key used to define an "extra-large" amount of time.
     *
     * @since 1.0
     */
    String PROP_READ_TIMEOUT_XL = "read.timeout.xl";

    /**
     * Property indicating whether the session should run in debug mode (true/false).
     *
     * @since 1.0
     */
    String PROP_DEBUG = "debug";

    /**
     * Get the session type.
     *
     * @since 1.0
     */
    Type getType();

    /**
     * Get the timeout value corresponding to the Timeout enumeration.
     *
     * @since 1.0
     */
    long getTimeout(Timeout to);

    /**
     * Get the session's properties. See the PROP_* property keys.
     *
     * @since 1.0
     */
    IProperty getProperties();

    /**
     * Connect the session.
     *
     * @since 1.0
     */
    boolean connect();

    /**
     * Disconnect the session.
     *
     * @since 1.0
     */
    void disconnect();

    /**
     * Check whether or not the session is connected.
     *
     * @since 1.0
     */
    boolean isConnected();

    /**
     * Get the name of the host to which the session is connected.  This must be a DNS name (safe for lookup).
     *
     * @since 1.0
     */
    String getHostname();

    /**
     * Get the directory in which state information for the session is stored.
     *
     * @return null if this is a stateless session.
     *
     * @since 1.0
     */
    File getWorkspace();

    /**
     * Returns the account name used by this session, if any.
     *
     * @since 1.0
     */
    String getUsername();

    /**
     * When you're completely finished using the session, call this method to clean up caches and other resources.
     *
     * @since 1.0
     */
    void dispose();
}
