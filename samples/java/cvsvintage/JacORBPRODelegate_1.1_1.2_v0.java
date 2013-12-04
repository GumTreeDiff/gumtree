/**
 * JOnAS: Java(TM) Open Application Server
 * Copyright (C) 2004 Bull S.A.
 * Contact: jonas-team@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * --------------------------------------------------------------------------
 * $Id: JacORBPRODelegate.java,v 1.1 2004-12-13 16:24:13 benoitf Exp $
 * --------------------------------------------------------------------------
 */
package org.objectweb.carol.rmi.multi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;

import org.objectweb.carol.jndi.spi.JacORBIIOPContext;
import org.objectweb.carol.util.configuration.TraceCarol;

import com.sun.corba.se.internal.javax.rmi.PortableRemoteObject;

/**
 * TODO : should extends non com.sun classes.
 * For example an OpenOrb class or implements our own Class, or use Classpath project
 * It seems to be javax.rmi.CORBA.PortableRemoteObjectDelegateImpl class
 * @author Florent Benoit
 */
public class JacORBPRODelegate extends PortableRemoteObject {

    /**
     * Makes a server object ready to receive remote calls. Note that subclasses
     * of PortableRemoteObject do not need to call this method, as it is called
     * by the constructor.
     * @param obj the server object to export.
     * @exception RemoteException if export fails.
     */
    public void exportObject(Remote obj) throws RemoteException {

        // For JacORB, we need first to unexport object as it is not associated
        // to an ORB
        try {
            unexportObject(obj);
        } catch (Exception eee) {
            TraceCarol.debugExportCarol("JacORBPRODelegate :exportObject() unexport = " + eee);
        }

        /* Now export it */
        try {
            super.exportObject(obj);
        } catch (Exception ee) {
            TraceCarol.debugExportCarol("JacORBPRODelegate: exportObject()  export:" + ee);
        }

        Tie theTie = Util.getTie(obj);

        // Then connect it to the ORB
        if (theTie != null) {
            theTie.orb(JacORBIIOPContext.getOrb());
        }



    }
}
