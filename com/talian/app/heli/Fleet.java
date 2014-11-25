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
package com.talian.app.heli;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;
import psdi.util.MXSession;

import com.talian.app.fuel.IFuelConsumer;
import com.talian.app.reservation.Reservation;
import com.talian.app.reservation.ReservationRemote;
import com.talian.app.route.Route;
import com.talian.app.scenario.FlightScenario;

/**
 * @author Seno
 *
 */
public class Fleet extends BasicFleet implements IFuelConsumer {

	FlightScenario scenario ;						// nullable
	Route  route ;

	HashMap<String, ReservationRemote> reservations ;
	ArrayList<MTOW> mtowarray ;


	public Fleet (FlightScenario scenario) {
		this.scenario = scenario ;
		reservations = new HashMap<String, ReservationRemote> () ;
		route = new Route (scenario, this);
		mtowarray = new ArrayList<MTOW>();
		oatbase = 0.0 ;
	}

	@Override
	public String toString() {
		return acreg.toString();
	}

	public String getACREG () {
		return acreg ;
	}

	public int getCapacity(){
		return this.paxcapacity;
	}
	
	public String getStartPosition(){
		return this.startpos;
	}
	
	public String getEndPosition(){
		return this.endpos;
	}
	
	public FlightScenario getScenario () {
		return scenario ;
	}

	public void setScenario (FlightScenario scenario) {
		this.scenario = scenario ;
	}

	public Route clearAssignment () throws MXException, RemoteException {
		if (route != null) {
			route.clear() ;
		}
		return route ;
	}

	public Route unassignReservation (Integer resvId) throws MXException, RemoteException {
		if (route != null) {
			route.removeReservation(resvId) ;
			route.revalidate();
		}
		return route ;
	}

	public boolean adjustMTOW () {
		Double oat = scenario.getConfig().defaultOAT ;
		Double delta = Math.abs(oat - oatbase) ;
		if (delta > 1.0) {
			for (int i=0; i<mtowarray.size(); i++) {
				MTOW staged = mtowarray.get(i) ;
				if (oat<=staged.oat) {
					mtow = staged.mtow ;
					oatbase = staged.oat ;
					return true ;
				}
			}
		}

		return false ;
	}

	public void changeStatus(MboRemote mbo) throws RemoteException, MXException{
		String fleetstatus = mbo.getString("status");
		if (fleetstatus.equals("ACTIVE")) {
			mbo.setValue("status", "INACTIVE");
		} else {
			mbo.setValue("status", "INACTIVE");
		}
	}

	public Route getRoute () throws MXException, RemoteException {
		return route ;
	}

	public void replaceRoute (Route newRoute) throws MXException, RemoteException {
		route = newRoute ;
	}

	public Route assignAtEnd (MboRemote resv) throws MXException, RemoteException {
		ReservationRemote r = Reservation.readfromMBO(resv) ;
		route.addPortAtEnd(r) ;

		r.setServedBy(acreg) ;

		return route ;
	}

	public Route assignAtEnd (Integer reservation) throws MXException, RemoteException {
		MboSetRemote mboset = null ;
		if (this.scenario != null) {
			MXSession mxsession = this.scenario.getMXSession() ;
			if (mxsession != null) {
				mboset = mxsession.getMboSet("reservation");
			}

			if (startpos == null)
				startpos = scenario.getConfig().defaultStartPos ;

			if (endpos == null)
				endpos = scenario.getConfig().defaultEndPos ;
		}

		if (mboset != null) {
			mboset.setWhere("reservationid = " + reservation) ;
			MboRemote resv = mboset.moveFirst() ;
			return assignAtEnd (resv) ;
		}

		return route ;
	}

	public Route rearrageRoute (String [] proposedports) throws MXException, RemoteException  {
		if (route != null) {
			Route newroute = route.rearrange(proposedports) ;
			int newLength = newroute.toPortNameArray().length;
			int oldLength = proposedports.length;
			if (newLength == oldLength) {
				route = null ;
				route = newroute ;
			}
		}

		return route ;
	}

	public void save (MboRemote mbo) throws RemoteException, MXException {
		mbo.setValue ("acreg", acreg) ;
		mbo.setValue ("eew", eew) ;
		mbo.setValue ("maxspeed", maxspeed) ;
		mbo.setValue ("paxcapacity", paxcapacity) ;
		mbo.setValue ("tankcapacity", tankcapacity) ;
		mbo.setValue ("resfuel", resfuel) ;
		mbo.setValue ("taxifuel", taxifuel) ;
		mbo.setValue ("takeofffuel", takeofffuel) ;
		mbo.setValue ("cruisefuel", cruisefuel) ;
		mbo.setValue ("startpos", startpos) ;
		mbo.setValue ("endpos", endpos) ;
		mbo.setValue ("refuelport", refuelport) ;
		mbo.setValue ("mtow", mtow) ;
		mbo.setValue ("alternatefuel", alternatefuel) ;
		mbo.setValue ("refuelingtime", refuelingtime) ;
		mbo.setValue ("groundtime", groundtime) ;
		mbo.setValue ("taxitime", taxitime) ;
		mbo.setValue("status", status);
//		mbo.getThisMboSet().save();
	}

	public Fleet copy () {
		Fleet flt = new Fleet(scenario) ;
		flt.acreg = this.acreg ;
		flt.eew = this.eew ;
		flt.maxspeed = this.maxspeed ;
		flt.paxcapacity = this.paxcapacity ;
		flt.tankcapacity = this.tankcapacity ;
		flt.resfuel = this.resfuel ;
		flt.taxifuel = this.taxifuel ;
		flt.takeofffuel = this.takeofffuel ;
		flt.cruisefuel = this.cruisefuel ;
		flt.alternatefuel = this.alternatefuel ;
		flt.refuelingtime = this.refuelingtime ;
		flt.groundtime = this.groundtime ;
		flt.taxitime = this.taxitime ;
		flt.mtow = this.mtow ;

		flt.startpos = startpos ;
		flt.endpos = endpos ;
		flt.refuelport = refuelport ;

		return flt ;
	}

	public static Fleet readfromMBO (FlightScenario scenario, MboRemote mbo) throws RemoteException, MXException {
		Fleet flt = new Fleet(scenario) ;
		flt.acreg = mbo.getString ("acreg") ;
		flt.eew = mbo.getDouble ("eew") ;
		flt.maxspeed = mbo.getDouble ("maxspeed") ;
		flt.paxcapacity = mbo.getInt ("paxcapacity") ;
		flt.tankcapacity = mbo.getDouble ("tankcapacity") ;
		flt.resfuel = mbo.getDouble ("resfuel") ;
		flt.taxifuel = mbo.getDouble ("taxifuel") ;
		flt.takeofffuel = mbo.getDouble ("takeofffuel") ;
		flt.cruisefuel = mbo.getDouble ("cruisefuel") ;
		flt.alternatefuel = mbo.getDouble ("alternatefuel") ;
		flt.refuelingtime = mbo.getDouble ("refuelingtime") ;
		flt.groundtime = mbo.getDouble ("groundtime") ;
		flt.taxitime = mbo.getDouble ("taxitime") ;
		flt.mtow = mbo.getDouble ("mtow") ;
		flt.status = mbo.getString("status");

		flt.startpos = mbo.getString ("startpos") ;
		flt.endpos = mbo.getString ("endpos") ;
		flt.refuelport = mbo.getString ("refuelport") ;

		MboSetRemote serve = mbo.getMboSet("reservations") ;
		MboRemote resv = serve.moveFirst() ;
		while (resv != null) {
			ReservationRemote r = Reservation.readfromMBO(resv) ;
			r.setServedBy(flt.acreg) ;
			flt.reservations.put(resv.getString("reservationid"), r) ;
		}

		MboSetRemote mtowset = mbo.getMboSet("$mtow", "mtow", "actype=:actype") ;
		mtowset.setOrderBy("oat") ;
		MboRemote mtow = mtowset.moveFirst() ;
		while (mtow!=null) {
			MTOW m = flt.new MTOW (mtow.getDouble("oat"), mtow.getDouble("mtow")) ;
			flt.mtowarray.add (m) ;
			mtow = mtowset.moveNext() ;
		}

		return flt;
	}

	@Override
	public Double cruisefuelConsumptionRate() {
		return this.cruisefuel ;
	}

	class MTOW {
		Double oat ;
		Double mtow ;
		MTOW (Double oat, Double mtow) {
			this.oat = oat ;
			this.mtow = mtow ;
		}
	} ;

}
