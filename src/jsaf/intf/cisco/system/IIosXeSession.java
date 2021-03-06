// Copyright (C) 2015 JovalCM.com.  All rights reserved.
// This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

package jsaf.intf.cisco.system;

import jsaf.intf.netconf.INetconf;

/**
 * A representation of an IOS-XE command-line session.
 *
 * @see jsaf.intf.system.IComputerSystem
 *
 * @author David A. Solin
 * @version %I% %G%
 * @since 1.3.1
 */
public interface IIosXeSession extends ICiscoSession {
    /**
     * Get an INetconf for this session.
     */
    INetconf getNetconf();
}
