/**
 * Copyright (C) 2005 - Bull S.A.
 *
 * CAROL: Common Architecture for RMI ObjectWeb Layer
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
 * $Id: IRMILocalContext.java,v 1.1 2005-09-15 13:04:16 benoitf Exp $
 * --------------------------------------------------------------------------
 */
package org.objectweb.carol.jndi.spi;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.objectweb.carol.jndi.registry.IRMIRegistryWrapperContext;
import org.objectweb.carol.jndi.wrapping.JNDIRemoteResource;
import org.objectweb.carol.jndi.wrapping.RemoteReference;
import org.objectweb.carol.rmi.exception.NamingExceptionHelper;


/**
 * Use the wrapper on registry object defined by RegistryWrapperContext class.
 * This class has been refactored to split :
 * <ul>
 * <li>- wrapper on registry object</li>
 * <li>- Single instance</li>
 * <li>- Wrapping of Serializable/Referenceable/... objects</li>
 * </ul>
 * @author Florent Benoit
 */
public class IRMILocalContext extends IRMIContext implements Context {

    /**
     * Constructs an IRMI local Wrapper context
     * @param irmiLocalContext the inital Local IRMI context
     * @throws NamingException if the registry wrapper cannot be build
     */
    public IRMILocalContext(Context irmiLocalContext) throws NamingException {
        super(new IRMIRegistryWrapperContext(irmiLocalContext.getEnvironment()));
    }

    /**
     * If this object is a reference wrapper return the reference If this object
     * is a resource wrapper return the resource
     * @param o the object to resolve
     * @param name name of the object to unwrap
     * @return the unwrapped object
     * @throws NamingException if the object cannot be unwraped
     */
    protected Object unwrapObject(Object o, Name name) throws NamingException {
        try {
            if (o instanceof RemoteReference) {
                // build of the Referenceable object with is Reference
                Reference objRef = ((RemoteReference) o).getReference();
                ObjectFactory objFact = (ObjectFactory) (Class.forName(objRef.getFactoryClassName())).newInstance();
                return objFact.getObjectInstance(objRef, name, this, getEnvironment());
            } else if (o instanceof JNDIRemoteResource) {
                return ((JNDIRemoteResource) o).getResource();
            } else {
                return o;
            }
        } catch (Exception e) {
            throw NamingExceptionHelper.create("Cannot unwrap object '" + o + "' with name '" + name + "'.", e);
        }
    }

}
