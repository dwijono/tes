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
package com.talian.app.route;
import java.rmi.RemoteException;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import psdi.mbo.MboRemote;
import psdi.util.MXException;

import com.talian.app.scenario.FlightScenario;
import com.talian.app.world.WorldPoint;

/**
 * @author Seno
 *
 */
public class Distance {
	Port fromPort ;
	Port toPort ;
	Double inMinutes ;
	Double inNMI ;
	FlightScenario scenario ;

	public Distance (FlightScenario scenario) {
		this.scenario = scenario ;
	}

	public Double getInMinutes () {
		return inMinutes ;
	}

	public Double getInNMI () {
		return inNMI ;
	}

	public String getKey () {
		return fromPort.port + "-" + toPort.port ;
	}

	public static Distance readfromMBO (FlightScenario scenario, MboRemote mbo) throws RemoteException, MXException {
		Distance dist = new Distance(scenario) ;
		dist.fromPort = scenario.getPort(mbo.getString("fromport")) ;
		dist.toPort = scenario.getPort(mbo.getString("toport")) ;
		dist.inMinutes = mbo.getDouble("timedistance") ;
		dist.inNMI  = mbo.getDouble("nmi") ;

		return dist ;
	}

	public static Distance getTempDistance (FlightScenario scenario, Port fromPort, Port toPort, Double speed) {
		Distance dist = new Distance (scenario) ;
		dist.fromPort = fromPort ;
		dist.toPort = toPort ;
		dist.inNMI = WorldPoint.distanceBetween(fromPort.point, toPort.point) ;

		if (speed != Double.NaN)
			dist.inMinutes = 60 * dist.inNMI / speed ;
		else
			dist.inMinutes = Double.MAX_VALUE ;

		return dist ;
	}

	public void save (MboRemote mbo) throws RemoteException, MXException {
		if (scenario != null) {
			mbo.setValue("scenarioid", scenario.getScenarioId()) ;
		}
		if (mbo.isNew()) {
			mbo.setValue("fromport", fromPort.getPort()) ;
			mbo.setValue("toport", fromPort.getPort()) ;
		}
		mbo.setValue("timedistance", inMinutes) ;
		mbo.setValue("NMI", inNMI) ;
//		mbo.getThisMboSet().save();
	}



}
