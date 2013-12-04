package org.tigris.scarab.om;


import org.apache.torque.om.UnsecurePersistent;
import org.apache.torque.util.Criteria; 

import org.tigris.scarab.util.ScarabException;
import org.tigris.scarab.util.ScarabConstants;
import org.tigris.scarab.security.ScarabSecurity;
import org.tigris.scarab.security.SecurityFactory;

/** 
 * You should add additional methods to this class to meet the
 * application requirements.  This class will only be generated as
 * long as it does not already exist in the output directory.
 */
public  class RQueryUser 
    extends org.tigris.scarab.om.BaseRQueryUser
    implements UnsecurePersistent
{

    /**
     * Delete the subscription.
     */
    public void delete(ScarabUser user, ScarabModule module) throws Exception
                                                             
    { 
        ScarabSecurity security = SecurityFactory.getInstance();
        if (user.getUserId().equals(getUserId())
            || security.hasPermission(ScarabSecurity.ITEM__APPROVE, user,
                                      module))
        {
            Criteria c = new Criteria()
                .add(RQueryUserPeer.USER_ID, getUserId())
                .add(RQueryUserPeer.QUERY_ID, getQueryId());
            RQueryUserPeer.doDelete(c);
        }
        else
        {
            throw new ScarabException(ScarabConstants.NO_PERMISSION_MESSAGE);
        }
    }
    
}
