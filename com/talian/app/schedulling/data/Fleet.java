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
package com.talian.app.schedulling.data;

import java.rmi.RemoteException ;
import java.util.Date ;

import psdi.mbo.MboRemote ;
import psdi.mbo.MboSetRemote ;
import psdi.mbo.SqlFormat ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public class Fleet {
	public MboRemote mbo ;
	public String fleet ;
	public String fleettype ;

	public Double speed ;
	public Double payload ;
	public Integer maxpax ;
	public Double crewweight ;
	public Double eew ;
	public Double oew ;
	public Double resvfuel ;
	public Double taxifuel ;
	public Double cruisefuel ;
	public Double altfuel ;
	public Double groundtime ;
	public Double taxitime ;
	public Double refueltime ;

	public Double avlpayload ;

	public String startPosition ;
	public String endPosition ;

	public Fleet(MboRemote mbo) {
		this.mbo = mbo ;
	}

	public String getKey() {
		return fleet ;
	}

	public void adjustPosition (Date dt, String flightsession) throws RemoteException, MXException {
		SqlFormat sqf = new SqlFormat("fleet=:0 and tripdate=:1 and flightsession=:2") ;
		sqf.setObject(0, "FLEETPOS", "FLEET", fleet) ;
		sqf.setDate(1, dt) ;
		sqf.setObject(2, "FLEETPOS", "FLIGHTSESSION", flightsession) ;
		MboSetRemote set = mbo.getMboSet("$pos", "FLEETPOS", sqf.format()) ;
		MboRemote pos = set.moveFirst() ;
		if (pos != null)
			startPosition = pos.getString("position") ;
		else
			startPosition = "" ;
	}

	static Fleet getInstance (MboRemote mbo) throws RemoteException, MXException {
		Fleet fl = new Fleet (mbo) ;
		fl.fleet = mbo.getString("fleet") ;
		fl.fleettype = mbo.getString("fleettype") ;
		fl.speed = mbo.getDouble("speed") ;
		fl.payload = mbo.getDouble("payload") ;
		fl.maxpax = mbo.getInt("maxpax") ;
		fl.crewweight = mbo.getDouble("crewweight") ;
		fl.eew = mbo.getDouble("eew") ;
		fl.oew = mbo.getDouble("oew") ;
		fl.resvfuel = mbo.getDouble("resvfuel") ;
		fl.taxifuel = mbo.getDouble("taxifuel") ;
		fl.cruisefuel = mbo.getDouble("cruisefuel") ;
		fl.altfuel = mbo.getDouble("altfuel") ;
		fl.groundtime = mbo.getDouble("groundtime") ;
		fl.taxitime = mbo.getDouble("taxitime") ;
		fl.refueltime = mbo.getDouble("refueltime") ;

		// fl.startPosition = mbo.getString("fleetpos.position") ;

		return fl ;
	}

	public Integer maximumPax () {
		return maxpax ;
	}

	public Double maximumWeight () {
		return payload ;
	}

	public void init () {
		avlpayload = payload ;
		endPosition = null ;
	}

	public Double tripFuelRatio (Double distance, Double speedratio) {
		return tripFuel (distance, this.speed * speedratio);
	}

	public Double tripFuel (Double distance) {
		return tripFuel (distance, this.speed);
	}

	public Double tripFuel (Double distance, Double givenspeed) {
		Double flyingtime = distance / givenspeed ;
		Double cruisefuel = flyingtime * this.cruisefuel + taxifuel ;

		return (1 + ALLOWANCE) * cruisefuel ;
	}

	Double ALLOWANCE = 0.06 ; // 6 pct for trip fuel
}
