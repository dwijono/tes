/**
 * 
 */
package com.talian.webclient.beans.world;

import java.rmi.RemoteException;

import psdi.util.MXException;
import psdi.webclient.system.beans.AppBean;
import psdi.webclient.system.controller.Utility;
import psdi.webclient.system.controller.WebClientEvent;

import com.talian.app.world.VehiclecostSetRemote;

/**
 * @author THINKPAD
 *
 */
public class VehiclecostAppBean extends AppBean {

	public int CALCULATE() throws MXException, RemoteException
	{
		if (!sessionContext.haslongOpStarted())
		{
		}
		if (sessionContext.runLongOp(this,"CALCULATE"))
		{
			VehiclecostSetRemote set = (VehiclecostSetRemote) this.getMboSet();
			set.calculateVehicleCostTable();
			
		    Utility.addMXWarnings(sessionContext, this.app.getAppBean().getWarnings());
		}
		if (sessionContext.haslongOpCompleted())
		{
			Utility.sendEvent(new WebClientEvent("dialogclose", sessionContext.getCurrentPageId(), null, sessionContext));
			SAVE();
			sessionContext.queueRefreshEvent();
		}
		
		return EVENT_HANDLED;
	}
}
