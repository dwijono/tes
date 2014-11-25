/*
 *
 *
 *
 * (C) COPYRIGHT Talian Limited, 2010
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets.
 *
 */
package com.talian.beans.routescenario;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import com.talian.app.assignment.RouteAssignment;
import com.talian.app.heli.HeliServiceRemote;
import com.talian.app.scenario.FlightScenario;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.server.MXServerRemote;
import psdi.util.MXException;
import psdi.util.MXSession;
import psdi.webclient.system.beans.AppBean;
import psdi.webclient.system.beans.DataBean;
import psdi.webclient.system.controller.Utility;
import psdi.webclient.system.controller.WebClientEvent;

/**
 * @author Elga
 *
 */
public class NewscenarioBean extends AppBean {
	@Override
	protected void initialize() throws MXException, RemoteException {
		// TODO Auto-generated method stub
		super.initialize();
		insert();
		fireStructureChangedEvent();
	}
	
	public int createScenario() throws MXException, RemoteException {
		MboRemote mboparam = this.getMbo();
		System.err.println("mboname: "+ mboparam.getName());
		Date reservedate = mboparam.getDate("reservedate");
		String session = mboparam.getString("session");

		MboSetRemote mboset = this.app.getDataBean().getMboSet();
		MboRemote newScenario = mboset.add();
		newScenario.setValue("reservedate", reservedate);
		newScenario.setValue("flightsession", session);
		mboset.save();
		
		Utility.sendEvent(new WebClientEvent("dialogclose", app.getCurrentPageId(), null, sessionContext));
		WebClientEvent eventType = new WebClientEvent("loadapp" , "scenario", null, sessionContext);
		
		eventType.addParameter("additionalevent", "useqbe");
		eventType.addParameter("additionaleventvalue", "scenarioid=1407");
		
		WebClientEvent eventExec = new WebClientEvent("execevent", sessionContext.getCurrentAppId(), eventType, sessionContext);
		
		//Define the created event in sessionContext object
		sessionContext.setCurrentEvent(eventExec);
		
		//The send event from Utility
		Utility.sendEvent(eventExec); 
		
		sessionContext.queueRefreshEvent();
		
		return EVENT_HANDLED;
//		System.err.println("Create SCENARIO: reservedate: "+reservedate+" flightsession: "+session);
//		System.err.println("scenarioid: "+ databean.getString("scenarioid"));
	}
	
}
