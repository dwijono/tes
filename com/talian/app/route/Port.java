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

import psdi.mbo.MboRemote;
import psdi.util.MXException;

import com.talian.app.heli.Fleet;
import com.talian.app.scenario.FlightScenario;
import com.talian.app.world.WorldPoint;

/**
 * @author Seno
 *
 */
public class Port {
	String 	       port ;
	WorldPoint     point ;
	FlightScenario scenario ;
	Double         penalty ;
	Double 		   taxitime ;
	boolean 	   hasRefuelingCapability ;

	public Port (FlightScenario scenario) {
		this.scenario = scenario ;
		penalty = 0.0 ;
		hasRefuelingCapability = false ;
	}

	public String getPort () {
		return port ;
	}

	public Double getPenalty () {
		return penalty ;
	}

	public Double getTaxitime () {
		return taxitime ;
	}

	public boolean hasRefuelingCapability() {
		return hasRefuelingCapability;
	}

	@Override
	public String toString() {
		return port.toString();
	}

	public static Port readfromMBO (FlightScenario scenario, MboRemote mbo) throws RemoteException, MXException {
		Port port = new Port(scenario) ;
		port.port = mbo.getString("heliport") ;
		port.point = WorldPoint.getPoint(mbo) ;
		port.penalty = mbo.getDouble("penalty") ;
		port.taxitime = mbo.getDouble("taxitime") ;
		port.hasRefuelingCapability = mbo.getBoolean("isrefueling") ;

		return port ;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Port) {
			return equals(((Port)obj).getPort()) ;
		}
		return super.equals(obj);
	}

	public void save (MboRemote mbo) throws RemoteException, MXException {
		if (scenario != null) {
			mbo.setValue("heliport", port);
		}
		mbo.setValue("penalty", penalty) ;
		mbo.setValue("taxitime", taxitime) ;
		mbo.setValue("isrefueling", hasRefuelingCapability) ;

		point.save (mbo) ;
	}


}
