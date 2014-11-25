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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.talian.app.fuel.IFuelBurnedJourney;
import com.talian.app.fuel.IFuelConsumer;
import com.talian.app.fuel.ITripFuelCalc;
import com.talian.app.heli.Fleet;
import com.talian.app.reservation.Reservation;
import com.talian.app.reservation.ReservationRemote;
import com.talian.app.route.Route.VisitedPort;

/**
 * @author Seno
 *
 */
public class Leg implements IFuelBurnedJourney,IValidationPoint,Remote {
	VisitedPort from ;
	VisitedPort to ;
	Fleet thefleet ;
	Route theroute ;

	int 	paxonboard ;
	Double 	weightonboard ;
	Double 	tripFuel ;
	boolean tripFuelCalculated ;
	HashMap<Integer, ReservationRemote> reservations ;
	Double  etd ;
	Double  eta ;

	Leg (Route theroute, Fleet thefleet, VisitedPort from, VisitedPort to) {
		this.thefleet = thefleet ;
		this.from = from ;
		this.to = to ;
		this.theroute = theroute ;
		tripFuelCalculated = false ;
		weightonboard = new Double (0.0) ;
		tripFuel = new Double (0.0) ;

		etd = Double.MAX_VALUE ;
		eta = -Double.MAX_VALUE ;

		reservations = new HashMap<Integer, ReservationRemote>();
	}

	public void setTo (VisitedPort to) {
		this.to = to ;
		tripFuelCalculated = false ;
	}

	public VisitedPort getTo () {
		return to ;
	}

	public VisitedPort getFrom () {
		return from ;
	}

	public Double getETD() {
		return etd ;
	}

	public Double getETA() {
		return eta ;
	}

	public void resetReservationTiming () throws RemoteException {
		Iterator<Integer> itt = reservations.keySet().iterator() ;
		while (itt.hasNext()) {
			Integer resvid = itt.next() ;
			ReservationRemote resv = reservations.get(resvid) ;
			resv.resetTiming() ;
		}
	}

	public void adjustReservationTiming () throws RemoteException {
		Iterator<Integer> itt = reservations.keySet().iterator() ;
		while (itt.hasNext()) {
			Integer resvid = itt.next() ;
			ReservationRemote resv = reservations.get(resvid) ;
			resv.setFirstETD(etd) ;
			resv.setLastETA(eta) ;
		}
	}

	@Override
	public String toString() {
		return from.toString() + "-" + to.toString() ;
	}

	@Override
	public Double tripFuel() {
		if (! tripFuelCalculated) {
			ITripFuelCalc calc = thefleet.getScenario().getTripFuelCalculator() ;
			tripFuel = calc.fuelNeeded(this) ;
			tripFuelCalculated = true ;
		}
		return tripFuel ;
	}

	@Override
	public Double distanceToInMinutes() {
		return Route.distanceInMinutes(theroute.scenario, from.theport, to.theport, thefleet.maxspeed) ;
	}

	@Override
	public IFuelConsumer getConsumer() {
		return thefleet ;
	}

	@Override
	public boolean validate() {
		return this.from.validate() ;
	}

	@Override
	public boolean isPOBValid() {
		return this.from.isPOBValid() ;
	}

	@Override
	public boolean isWOBValid() {
		return this.from.isWOBValid() ;
	}

	public int getPaxOnBoard () {
		return paxonboard ;
	}

	void addPax (ReservationRemote resv) throws RemoteException {
		if (!resv.isDummyRefueling())
			reservations.put(resv.getId(), resv) ;

		resv.setFirstETD(etd);
		resv.setLastETA(eta);

		/*
		try {
			System.err.println(resv.getString("displayname")+ ", etd "+ resv.getEtd()+ ", eta "+ resv.getEta());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CocoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

	void removePax (ReservationRemote resv) throws RemoteException {
		reservations.remove(resv.getId()) ;
	}

	void carryonLeg (Leg legbefore) {
		reservations.putAll(legbefore.reservations) ;
	}

	// return array of reservation sorted by display name
	public List<ReservationRemote> getServedPax () {
		int count = reservations.size() ;
		if (count==0)
			return null ;

		ReservationRemote resvs[] = new ReservationRemote[count] ;
		Collection<ReservationRemote> colval = reservations.values();
		colval.toArray(resvs) ;
		List<ReservationRemote> resvlist = Arrays.asList(resvs) ;
		Collections.sort (resvlist, Reservation.ETDETA_ORDER) ;

		return resvlist ;
	}
}

