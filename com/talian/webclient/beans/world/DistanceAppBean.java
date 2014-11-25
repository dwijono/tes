/*
 *
 * 
 *
 * (C) COPYRIGHT Talian Limited, 2011
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets.
 *
 */
package com.talian.webclient.beans.world;

import java.rmi.RemoteException ;

import psdi.util.MXException ;
import psdi.webclient.system.beans.AppBean ;
import psdi.webclient.system.controller.Utility ;
import psdi.webclient.system.controller.WebClientEvent ;

import com.talian.app.world.DistanceSetRemote ;

/**
 * @author Seno
 *
 */
public class DistanceAppBean extends AppBean {
	public int GENERATE() throws MXException, RemoteException
	{
		if (!sessionContext.haslongOpStarted())
		{
		}
		if (sessionContext.runLongOp(this,"GENERATE"))
		{
			DistanceSetRemote set = (DistanceSetRemote) this.getMboSet() ;
			set.rebuildDistanceTable();
			
		    Utility.addMXWarnings(sessionContext, this.app.getAppBean().getWarnings());
		}
		if (sessionContext.haslongOpCompleted())
		{
			Utility.sendEvent(new WebClientEvent("dialogclose", sessionContext.getCurrentPageId(), null, sessionContext));
			SAVE() ;
			sessionContext.queueRefreshEvent();
		}
		
		return EVENT_HANDLED;
	}

}
