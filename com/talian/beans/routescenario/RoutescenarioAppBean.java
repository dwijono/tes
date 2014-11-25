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

/**
 * @author Elga
 *
 */
public class RoutescenarioAppBean extends AppBean {

	/**
	 *
	 */
	public RoutescenarioAppBean() {
	}

	public void ADDSCEN(Date reservedate, String session) throws MXException, RemoteException {
		MboSetRemote mboset = this.getMboSet();
		MboRemote mbo =  mboset.add();
	}
}
