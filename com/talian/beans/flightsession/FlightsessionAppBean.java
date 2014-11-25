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
package com.talian.beans.flightsession;

import java.rmi.RemoteException;
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

/**
 * @author Elga
 *
 */
public class FlightsessionAppBean extends AppBean {

	/**
	 *
	 */
	public FlightsessionAppBean() {
	}

	public void FINDNBEST() throws MXException, RemoteException {
		MXSession mxsession = this.getMXSession();
//		System.err.println("FINDNBEST-> reservedate: "+ this.getMbo().getDate("reserveddate") + " flightsession: "+ this.getMbo().getString("flightsession"));
		
		FlightScenario scenario = FlightScenario.newRecord(mxsession, this.getMbo().getDate("reserveddate"), this.getMbo().getString("flightsession"));
		
		scenario.setFleetAvailability("PK-TPD", null, true);
		scenario.setFleetAvailability("PK-TPE", null, false);
		scenario.setFleetAvailability("PK-TPF", null, false);
		scenario.setFleetAvailability("PK-TPG", null, false);
		
		HeliServiceRemote svcHeli = (HeliServiceRemote)mxsession .lookup("HELI");
		
		svcHeli.setSchedullerRunning(true);
		RouteAssignment ra = new RouteAssignment(scenario, svcHeli);
		List<FlightScenario> fsList2 = null;
    	List<FlightScenario> fsList = ra.findNBest(5, 1*60*1000, false, fsList2);
    	
    	if (fsList != null) {
    		int nCount = fsList.size();
    		for (int i=0; i<nCount; i++) {
    			FlightScenario scen = fsList.get(i);
    			System.out.println(scen);
    			scen.save();
    		}
    	}
    	else {
    		System.out.println("Scenario is not found");
    	}
	}
}
