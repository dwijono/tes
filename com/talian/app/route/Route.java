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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;

import psdi.common.parse.IsNullNode;
import psdi.util.MXException;

import com.talian.app.fuel.IFuelBurnedJourney;
import com.talian.app.fuel.IFuelConsumer;
import com.talian.app.fuel.IFuelPoint;
import com.talian.app.heli.Fleet;
import com.talian.app.reservation.Reservation;
import com.talian.app.reservation.ReservationRemote;
import com.talian.app.scenario.Configuration;
import com.talian.app.scenario.FlightScenario;

/**
 * @author Seno
 *
 */
public class Route implements IFuelBurnedJourney {
	FlightScenario scenario ;
	VisitedPort startpos ;		// starting base station
	VisitedPort endpos ;		// ending base station
	LinkedList<VisitedPort> visitedports ;
	LinkedHashMap<Integer, ReservationRemote> reservations ; 
	HashMap<Integer, ReservationRemote> invalidreservations ;
	int lastIndex ;
	boolean changed ;
	boolean isValid ;
	Fleet   thefleet ;
	ReservationRemote blocking_vip ;

	public Route (FlightScenario scenario, Fleet afleet) {
		this.scenario = scenario ;
		this.thefleet = afleet ;
		lastIndex = 0 ;

		visitedports = new LinkedList<VisitedPort>() ;
		reservations = new LinkedHashMap<Integer, ReservationRemote>();
		invalidreservations = new HashMap<Integer, ReservationRemote>();

		isValid = true ;
		changed = false ;

		blocking_vip = null;
	}

	public Route copy () throws RemoteException {
		String [] ports = toPortNameArray() ;
		Route r = rearrange(ports) ;
		for (int i=0; i<visitedports.size(); i++) {
			VisitedPort vp = visitedports.get(i) ;
			r.setRefueling(vp.theport.getPort(), vp.isrefueling) ;
		}

		return r ;
	}

	public void mergeInvalids (Route tr) {
		for (Iterator<Integer> iterator = tr.invalidreservations.keySet().iterator(); iterator.hasNext();) {
			Integer resvid = iterator.next();
			ReservationRemote resv = tr.invalidreservations.get(resvid) ;
			invalidreservations.put(resvid, resv) ;
		}
	}

	public Route rearrange (String [] proposedports) throws RemoteException {
		Route newroute = new Route (scenario, thefleet) ;
		if (proposedports == null)
			return newroute ;

		VisitedPort lastvp = null ;

		// arrange the visited ports as proposed
		for (int i=0; i<proposedports.length; i++) {
			String port = proposedports[i] ;
			VisitedPort vp = new VisitedPort (thefleet, scenario.getPort(port)) ;
			newroute.visitedports.add(vp) ;

			if (lastvp != null) {
				Leg lg = new Leg(this, thefleet, lastvp, vp) ;
				lastvp.legafter = lg ;
				vp.legbefore = lg ;
			}
			lastvp = vp ;
		}
		newroute.startpos = newroute.visitedports.getFirst() ;
		newroute.endpos = newroute.visitedports.getLast() ;
		resetReservationTiming();
		// reinsert the reservation
		for (Iterator iterator = reservations.keySet().iterator(); iterator.hasNext();) {
			Integer resvid = (Integer) iterator.next();
			ReservationRemote resv = reservations.get(resvid) ;
			newroute.addPortAtEnd(resv, true) ;
		}
		adjustReservationTiming();
		return newroute ;
	}

	public Route rearrange (Port [] proposedports) throws RemoteException {
		Route newroute = new Route (scenario, thefleet) ;
		if (proposedports == null)
			return newroute ;

		VisitedPort lastvp = null ;

		// arrange the visited ports as proposed
		for (int i=0; i<proposedports.length; i++) {
			Port port = proposedports[i] ;
			VisitedPort vp = new VisitedPort (thefleet, port) ;

			if (vp.hasRefuelingCapability()) {
				for (int j = 0; j < visitedports.size(); j++) {
					VisitedPort trialVP = visitedports.get(j);
					if (vp.getPort().equals(trialVP.getPort())) {
						vp.isrefueling = trialVP.isrefueling;
						break;
					}
				}
			}

			newroute.visitedports.add(vp) ;

			if (lastvp != null) {
				Leg lg = new Leg(this, thefleet, lastvp, vp) ;
				lastvp.legafter = lg ;
				vp.legbefore = lg ;
			}
			lastvp = vp ;
		}
		newroute.startpos = newroute.visitedports.getFirst() ;
		newroute.endpos = newroute.visitedports.getLast() ;

		// reinsert the reservation
		for (Iterator iterator = reservations.keySet().iterator(); iterator.hasNext();) {
			Integer resvid = (Integer) iterator.next();
			ReservationRemote resv = reservations.get(resvid) ;
			newroute.addPortAtEnd(resv, true) ;
		}

		return newroute ;
	}

	public void setStartPos (String start) {
		this.startpos = new VisitedPort (thefleet, scenario.getPort(start)) ;
	}

	public void setEndPos (String end) {
		this.endpos = new VisitedPort (thefleet, scenario.getPort(end)) ;
	}

	public VisitedPort getStartPos () {
		return startpos ;
	}

	public void clear () {
		changed = true ;
		lastIndex = 0 ;

		visitedports= null ;

		visitedports = new LinkedList<VisitedPort>() ;

		invalidreservations.clear() ;
		reservations.clear() ;

		startpos = null ;
		endpos = null ;

		isValid = true ;
	}

	public HashMap<Integer,ReservationRemote> getReservation () {
		return reservations ;
	}

	public boolean isChanged () {
		return changed ;
	}

	public boolean preValidateResv (ReservationRemote resv) throws RemoteException {
		Port p1 = scenario.getPort(resv.getOrg()) ;
		return ! isFull(p1) ;
	}

	//TODO: check weight also
	public boolean isFull (Port p) {
		if (blocking_vip != null)
			return true ;

		int[] idxs = findPort(p) ;
		if (idxs!=null) {
			for (int i=0; i<idxs.length; i++) {
				int ivp = idxs[i] ;
				if (ivp>=0) {
					VisitedPort vp = visitedports.get(ivp) ;
					if (vp.legafter != null) {
						if (vp.legafter.paxonboard>=thefleet.paxcapacity)
							return true ;
					}
				}
			}
		}
		return false ;
	}

	public boolean isInInvalidList (ReservationRemote resv) throws RemoteException {
		return invalidreservations.containsKey(resv.getId()) ;
	}

	private void adjustFuelForward (VisitedPort port) throws RemoteException {
		if (port==null)
			return ;

		Double fuelneed = 0.0 ;
		Double last_eta = 0.0 ;
		Double first_last_eta = 0.0;
//		Double last_etd = 0.0 ;

		if (port.legbefore != null) {
			fuelneed = port.legbefore.from.fuelneed ;
			fuelneed += (1.06 * port.legbefore.tripFuel()) ;
			if (port.legafter != null) {
				last_eta = port.legbefore.eta + (port.isrefueling ? thefleet.refuelingtime:thefleet.groundtime);
			} else {
				if (port.getTaxitime() > 0) {
					last_eta = port.legbefore.eta + (port.isrefueling ? thefleet.refuelingtime:thefleet.taxitime);
				} else {
					last_eta = port.legbefore.eta + (port.isrefueling ? thefleet.refuelingtime:thefleet.groundtime);
				}
			}
		} else {
			first_last_eta = thefleet.taxitime ;
		}

		if (port.isrefueling)
			fuelneed = 0.0 ;
		else
			port.fuelneed = fuelneed ;


		if (port.legafter != null) {
			if (port.legbefore != null) {
				port.fuelneed += 1.06 * thefleet.takeofffuel ;
				port.legafter.eta = last_eta + port.legafter.distanceToInMinutes() ;
			} else {
				port.fuelneed += 1.06 * (thefleet.takeofffuel +  thefleet.taxifuel);
				if (port.getTaxitime() > 0) {
					port.legafter.eta = last_eta + port.legafter.distanceToInMinutes() + first_last_eta ;
				} else {
					port.legafter.eta = last_eta + port.legafter.distanceToInMinutes() ;
				}
			}

			port.legafter.etd = last_eta ;
			port.legafter.adjustReservationTiming() ;
			adjustFuelForward(port.legafter.to) ;
		}
		else {
			fuelneed += thefleet.alternatefuel + thefleet.resfuel + thefleet.taxifuel ;
			adjustFuelBackward(port) ;
		}
	}

	public void resetReservationTiming () throws RemoteException {
		VisitedPort vp = startpos ;
		while (vp != null) {
			Leg la = vp.legafter ;
			if (la!=null) {
				la.resetReservationTiming() ;
				vp = la.to ;
			}
			else
				vp = null ;
		}
	}

	public void adjustReservationTiming () throws RemoteException {
		VisitedPort vp = startpos ;
		while (vp != null) {
			Leg la = vp.legafter ;
			if (la!=null) {
				la.adjustReservationTiming() ;
				vp = la.to ;
			}
			else
				vp = null ;
		}
	}

	private void adjustFuelBackward (VisitedPort port) {
		if (port==null)
			return ;

		boolean hasRefuel = false;
		Double fueluploadneed = 0.0 ;
		if (port.legafter != null) {
			if (! port.legafter.to.isrefueling){
				fueluploadneed = port.legafter.to.fueluploadneed  ;
			} else if (port.legafter.to.isrefueling) {
				fueluploadneed = thefleet.resfuel;
				hasRefuel = true;
			}

			fueluploadneed += (1.06 *  port.legafter.tripFuel()) ;
			fueluploadneed += (thefleet.takeofffuel) ;
		}
		else {
			if (hasRefuel) {
				fueluploadneed += thefleet.alternatefuel + thefleet.taxifuel ;
			} else
				fueluploadneed += thefleet.alternatefuel + thefleet.resfuel + thefleet.taxifuel ;
		}

		port.fueluploadneed = fueluploadneed ;

		Double crewweight = this.thefleet.getScenario().getConfig().defaultCrewWeight ;
		port.payload = thefleet.mtow  - thefleet.eew - crewweight ;
		port.payload -= port.getPenalty () ;
		port.payload -= fueluploadneed ;

		if (port.legbefore != null)
			adjustFuelBackward(port.legbefore.from) ;    // forward only if refueling

	}

	void removePort (int idx) {
		VisitedPort vp = visitedports.get(idx) ;
		if (! vp.isrefueling) {
			VisitedPort vpafter = null ;
			if (vp.legafter != null)
				vpafter = vp.legafter.to ;

			if (vp.legbefore!=null)
				vp.legbefore.to = vpafter ;

			if (vpafter!=null)
				vpafter.legbefore = vp.legbefore ;

			visitedports.remove(idx) ;
			while (!visitedports.isEmpty() && visitedports.size()<=2) {
				visitedports.remove() ;
				startpos = null ;
				endpos = null ;
			}
		}
	}

	public void removeReservation (Integer resvid) throws RemoteException {
		ReservationRemote resv = reservations.get(resvid) ;
		if (resv != null) {
			int portpair[] = findPortPair (resv) ;
			if (portpair[0]>=0 && portpair[1]>=0) {    // found
				VisitedPort vpon = visitedports.get(portpair[0]) ;
				VisitedPort vpoff = visitedports.get(portpair[1]) ;

				vpon.paxOff(resv, vpoff) ;
				if (vpoff.paxon==0 && vpoff.paxoff==0) {
					// remove this port from visited list
					if (! vpoff.isrefueling && portpair[1]<visitedports.size()-1)
						removePort(portpair[1]) ;
				}
				if (vpon.paxon==0 && vpon.paxoff==0) {
					// remove this port from visited list
					if (! vpon.isrefueling && portpair[0]>0)
						removePort(portpair[0]) ;
				}
			}

			if (resv.isBlockingVIP()) {
				blocking_vip = null ;
			}

			reservations.remove(resvid) ;
		}
	}

	public boolean revalidate () {
		isValid = true ;
		VisitedPort p = startpos ;

		while (p != null) {
			isValid = isValid && p.validate() ;
			Leg la = p.legafter ;
			if (la != null) {
				isValid = isValid && la.validate() ;
				p = la.to ;
			}
			else
				p = null ;
		}
		return isValid ;
	}

	public void showPax () {
		VisitedPort p = startpos ;

		System.out.println("----------------") ;
		while (p != null) {
			Leg la = p.legafter ;

			System.out.println("[" + p + "], paxon = " + p.paxon + ", paxoff=" + p.paxoff);
			if (la != null) {
				System.out.println("--[" + la + "], paxonboard = " + la.paxonboard) ;
				p = la.to ;
			}
			else
				p = null ;
		}
	}

	public void addPortAtEnd (ReservationRemote resv) throws RemoteException {
		addPortAtEnd (resv, false) ;
	}

	public boolean setRefueling (String port, boolean isrefuel) throws RemoteException {
		boolean retval = false ;
		Port p = scenario.getPort(port) ;
		int [] idxs = findPort(p) ;
		if (idxs != null) {
			for (int i=0; i<idxs.length; i++) {
				int idx = idxs[i] ;
				if (idx>=0) {
					VisitedPort vp = visitedports.get(idx) ;
					vp.setRefueling(isrefuel);
					retval = true ;
				}
			}
		}
		if(retval){
			adjustFuelForward(startpos) ;
			revalidate () ;
		}
		return retval ;
	}

	public boolean hasRefuelingActivity () {
		for (int i=0; i<visitedports.size(); i++) {
			VisitedPort vp = visitedports.get(i) ;
			if (vp.isrefueling)
				return true;
		}
		return false ;
	}

	public void addPortAtEnd (ReservationRemote resv, boolean force) throws RemoteException {
		changed = false ;

		if (startpos == null || endpos == null) {
			// get default start and ending station from scenario
			setStartPos (thefleet.startpos) ;
			setEndPos (thefleet.endpos) ;
		}

		Port p1 = scenario.getPort(resv.getOrg()) ;
		Port p2 = scenario.getPort(resv.getDest()) ;
		VisitedPort firstinserted = null ;

		if (! resv.isDummyRefueling())
			reservations.put(resv.getId(), resv) ;

		resv.resetTiming();

		if (resv.isBlockingVIP())
			blocking_vip = resv ;

		if (resv.isDummyRefueling())
			force = true ;

		if (visitedports.isEmpty()) {
			VisitedPort vp1, vp2 ;

			visitedports.addFirst(startpos) ;
			firstinserted = startpos ;

			if (startpos.equals(p1)) {
				vp1 = startpos ;
			}
			else {
				vp1 = new VisitedPort(thefleet,p1) ;

				if (startpos.legafter == null) {
					startpos.legafter = new Leg(this, thefleet,startpos, vp1) ;
					vp1.legbefore = startpos.legafter ;
				}
				visitedports.add (vp1) ;
				lastIndex ++ ;
			}


			if (endpos.equals(p2)) {
				vp2 = endpos ;

				if (startpos.legafter == null) {
					startpos.legafter = new Leg(this, thefleet,startpos, endpos) ;
					endpos.legbefore = startpos.legafter ;
				}
				if (vp1.legafter == null) {
					vp1.legafter = new Leg(this, thefleet, vp1, endpos) ;
					endpos.legbefore = vp1.legafter ;
				}
			}
			else {
				vp2 = new VisitedPort(thefleet, p2) ;

				visitedports.add (vp2) ;
				lastIndex ++ ;

				if (startpos.legafter == null) {
					startpos.legafter = new Leg(this, thefleet, startpos, vp2) ;
					vp2.legbefore = startpos.legafter ;
				}
				if (vp1.legafter == null) {
					vp1.legafter = new Leg(this, thefleet, vp1, vp2) ;
					vp2.legbefore = vp1.legafter ;
				}

				vp2.legafter = new Leg (this, thefleet, vp2, endpos) ;
				endpos.legbefore = vp2.legafter ;
			}

			vp1.paxOn (resv, vp2) ;

			if (resv.isDummyRefueling())
				vp2.setRefueling(true) ;

			visitedports.addLast(endpos) ;
			changed = true ;
		}
		else {
			System.err.println("%%%%%%%%%%%%% else 1");
			int portpair[] = findPortPair (resv) ;
			if (portpair[0]>=0 && portpair[1]>=0) {    // found a pair for the new reservation
				// route is not changed
				// adjust pax on board
				System.err.println("%%%%%%%%%%%%% else 2");
				VisitedPort vpon = visitedports.get(portpair[0]) ;
				VisitedPort vpoff = visitedports.get(portpair[1]) ;

				if (vpon.legafter.getPaxOnBoard()+1 <= thefleet.paxcapacity) {
					System.err.println("%%%%%%%%%%%%% else 3");
					vpon.paxOn(resv, vpoff) ;
					if (resv.isDummyRefueling())
						System.err.println("%%%%%%%%%%%%% else 3a");
						vpoff.setRefueling(true) ;
				} else {
					System.err.println("%%%%%%%%%%%%% else 4");
					VisitedPort vpon1 = null, vpoff1 = null;
					VisitedPort vppref = null, vpnext = null ;
					int offset = 0 ;
					int insertion = visitedports.size() - 2 ;
					assert(insertion>0) ;
					assert(insertion<visitedports.size()) ;

					vppref = visitedports.get(insertion) ;
					vpnext = visitedports.get(insertion + 1) ;

					VisitedPort vp1 = new VisitedPort(thefleet,p1) ;
					visitedports.add(insertion + 1, vp1) ;

					vpon1 = vp1 ;
					vppref.legafter.setTo(vp1) ;
					vp1.legbefore = vppref.legafter ;

					// create new leg (vp1, vpnext)
					vp1.legafter = new Leg(this, thefleet, vp1, vpnext) ;
					vp1.legafter.carryonLeg(vp1.legbefore) ;

					vpnext.legbefore= vp1.legafter ;

					offset = 1 ;
					firstinserted = vp1 ;
					changed = true ;

					if (portpair[0] == -10){
						//edited by elga
						vpon1.seqvalid =  false;
					}
					
					int insertion2 = visitedports.size() - 2 ;
					assert(insertion2>0) ;
					assert(insertion2<visitedports.size()) ;

					vppref = visitedports.get(insertion2) ;    // insertion point is the current port
					vpnext = visitedports.get(insertion2 + 1) ;

					VisitedPort vp2 = new VisitedPort(thefleet, p2) ;
					visitedports.add(insertion2 + 1, vp2) ;    // linked list insertion is based on next index

					if (firstinserted==null)
						firstinserted = vp2 ;

					vpoff1 = vp2 ;
					vppref.legafter.setTo(vp2) ;
					vp2.legbefore = vppref.legafter ;

					// create new leg (vp2, vpnext)
					vp2.legafter = new Leg(this, thefleet, vp2, vpnext) ;
					vp2.legafter.carryonLeg(vp2.legbefore) ;

					vpnext.legbefore= vp2.legafter ;

					changed = true ;

					if (portpair[1] == -10){
						//edited by elga
						vpon1.seqvalid =  true;
					}
					
					vpon1.paxOn(resv, vpoff1) ;
					if (resv.isDummyRefueling())
						vpoff1.setRefueling(true) ;
				}
			} else if ((portpair[0]<0) && (portpair[1]>=0))  { //found just the destination of the reservation
				System.err.println("%%%%%%%%%%%%% else 5");
				VisitedPort vppref = null;
				VisitedPort	vpnext = null;
				VisitedPort vpon = null;
				VisitedPort vpoff = null ;

				int insertion = portpair[1];
				vppref = visitedports.get(insertion-1) ;
				vpnext = visitedports.get(insertion) ;

				VisitedPort vp1 = new VisitedPort(thefleet,p1) ;
				VisitedPort vp2 = new VisitedPort(thefleet, p2) ;
				visitedports.add(insertion, vp1);

				vpon = vp1 ;
				vppref.legafter.setTo(vp1) ;
				vp1.legbefore = vppref.legafter ;

				// create new leg (vp1, vp2)
				vp1.legafter = new Leg(this, thefleet, vp1, vpnext) ;
				vp1.legafter.carryonLeg(vp1.legbefore) ;

				vpnext.legbefore= vp1.legafter ;
				vpoff = visitedports.get(1 + portpair[1]) ;

				firstinserted=vp1;
				vpon.paxOn(resv, vpoff) ;
				changed = true ;
			}
			else {  									// if not found
				System.err.println("%%%%%%%%%%%%% else 6");
				VisitedPort vpon = null, vpoff = null;
				VisitedPort vppref = null, vpnext = null ;
				int offset = 0 ;

				//found only the origin of the reservation 
				if (portpair[0]>=0){
					System.err.println("%%%%%%%%%%%%% else 7");
					vpon = visitedports.get(portpair[0]) ;
				} else {
					System.err.println("%%%%%%%%%%%%% else 8");
					int insertion = visitedports.size() - 2 ;
					assert(insertion>0) ;
					assert(insertion<visitedports.size()) ;

					vppref = visitedports.get(insertion) ;
					vpnext = visitedports.get(insertion + 1) ;

					VisitedPort vp1 = new VisitedPort(thefleet,p1) ;
					visitedports.add(insertion + 1, vp1) ;

					vpon = vp1 ;
					vppref.legafter.setTo(vp1) ;
					vp1.legbefore = vppref.legafter ;

					// create new leg (vp1, vpnext)
					vp1.legafter = new Leg(this, thefleet, vp1, vpnext) ;
					vp1.legafter.carryonLeg(vp1.legbefore) ;

					vpnext.legbefore= vp1.legafter ;

					offset = 1 ;
					firstinserted = vp1 ;
					changed = true ;

					if (portpair[0] == -10){
						//edited by elga
						vpon.seqvalid =  false;
					}
				}

				if (portpair[1]>=0) {
					System.err.println("%%%%%%%%%%%%% else 9");
					vpoff = visitedports.get(offset + portpair[1]) ;
				}
				else {
					System.err.println("%%%%%%%%%%%%% else 10");
					int insertion = visitedports.size() - 2 ;
					assert(insertion>0) ;
					assert(insertion<visitedports.size()) ;

					vppref = visitedports.get(insertion) ;    // insertion point is the current port
					vpnext = visitedports.get(insertion + 1) ;

					VisitedPort vp2 = new VisitedPort(thefleet, p2) ;
					visitedports.add(insertion + 1, vp2) ;    // linked list insertion is based on next index

					if (firstinserted==null)
						firstinserted = vp2 ;

					vpoff = vp2 ;
					vppref.legafter.setTo(vp2) ;
					vp2.legbefore = vppref.legafter ;

					// create new leg (vp2, vpnext)
					vp2.legafter = new Leg(this, thefleet, vp2, vpnext) ;
					vp2.legafter.carryonLeg(vp2.legbefore) ;

					vpnext.legbefore= vp2.legafter ;

					changed = true ;

					if (portpair[1] == -10){
						//edited by elga
						vpon.seqvalid =  true;
					}
				}

				vpon.paxOn(resv, vpoff) ;
				if (resv.isDummyRefueling())
					vpoff.setRefueling(true) ;
			}
		}

		if (thefleet.adjustMTOW())
			force = true ;

		if (force)
			adjustFuelForward(startpos) ;
		else if (changed) {
			VisitedPort adjusted = firstinserted ;
			if (firstinserted.legbefore!=null)
				adjusted = firstinserted.legbefore.from ;

			adjustFuelForward(adjusted) ;
		}

		if (!revalidate ())
			invalidreservations.put(resv.getId(), resv) ;

		// for debug
		// showPax() ;
	}

	// Use 2opt heuristic algorithm
	public Route advanceOptimize () throws RemoteException, MXException {
		boolean retval = false ;
		int count = visitedports.size() ;
		if (count < 4)
			return null  ;

		// applied only for cyclic
		if (! startpos.equals(endpos))
			return null ;

		Port[] ports = new Port[count] ;
		for (int i=0; i<ports.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			ports[i] = vp.theport;
		}

		// skip the return port (it always assume cyclic)
		count-- ;
		boolean done = false;
		boolean changed = true ;
		Double oldscore = this.distanceToInMinutes() ;

		while (changed) {
			changed = false ;
			for (int i=0; i<count; i++) {
				Port pi0 = ports[i];
				Port pi1 = ports[(i+1)%count] ;
				Double t1a = Route.distanceInMinutes(scenario, pi0, pi1, thefleet.maxspeed) ;
				for(int j = i + 2; j < count; j++) {
					Port pj0 = ports[j] ;
					Port pj1 = ports[(j+1)%count] ;
					Double t1b = Route.distanceInMinutes(scenario, pj0, pj1, thefleet.maxspeed) ;
					Double t1 = t1a + t1b ;

					Double t2 = Route.distanceInMinutes(scenario, pi0, pj0, thefleet.maxspeed) ;
					t2 += Route.distanceInMinutes(scenario, pi1, pj1, thefleet.maxspeed) ;

					if (t1 > t2) {
						retval = true ;
						changed = true ;

						Port tmp = ports[(i+1) % count] ;
						ports[(i+1) % count] = ports[j] ;
						ports[j] = tmp ;
					}
				}
			}
		}

		if (retval) {
			Route r = rearrange(ports) ;
			if (r.isValid()) {
				Double newscore = r.distanceToInMinutes() ;
				if (newscore < oldscore)
					thefleet.replaceRoute(r) ;
				else {
					r = null ;
					System.err.println("advance optimize resulting lesser score");
				}

				return r ;
			}
		}

		return null ;
	}

	// added by efs
	public Route advanceOptimize1 () throws RemoteException, MXException {
		boolean retval = false ;
		Stack<Route> partialSolution = new Stack<Route>();
		int count = visitedports.size() ;
		if (count < 4)
			return null  ;

		// applied only for cyclic
		if (! startpos.equals(endpos))
			return null ;

		Port[] ports = new Port[count] ;
		for (int i=0; i<ports.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			ports[i] = vp.theport;
		}

		// skip the return port (it always assume cyclic)
		count-- ;
		boolean done = false;
		boolean changed = true ;
		Double oldscore = this.distanceToInMinutes() ;

		while (changed) {
			changed = false ;
			for (int i=0; i<count; i++) {
				Port pi0 = ports[i];
				Port pi1 = ports[(i+1)%count] ;
				Double t1a = Route.distanceInMinutes(scenario, pi0, pi1, thefleet.maxspeed) ;
				for(int j = i + 2; j < count; j++) {
					Port pj0 = ports[j] ;
					Port pj1 = ports[(j+1)%count] ;
					Double t1b = Route.distanceInMinutes(scenario, pj0, pj1, thefleet.maxspeed) ;
					Double t1 = t1a + t1b ;

					Double t2 = Route.distanceInMinutes(scenario, pi0, pj0, thefleet.maxspeed) ;
					t2 += Route.distanceInMinutes(scenario, pi1, pj1, thefleet.maxspeed) ;

					if (t1 > t2) {
						retval = true ;
						changed = true ;

						Port tmp = ports[(i+1) % count] ;
						ports[(i+1) % count] = ports[j] ;
						ports[j] = tmp ;
						Route r = rearrange(ports) ;
						if (r.isValid()) {
							Double newscore = r.distanceToInMinutes() ;
							if (newscore < oldscore){
								partialSolution.add(r);
							} else {
								r = null ;
								System.err.println("advance optimize resulting lesser score");
							}
						}
					}
				}
			}
		}

		Route temproute = null;
		if (!partialSolution.isEmpty()) {
			while (!partialSolution.isEmpty()) {
				Route firstRoute = partialSolution.pop();
				if (!partialSolution.isEmpty()) {
					Route secondRoute = partialSolution.pop();

					if (firstRoute.distanceToInMinutes() < secondRoute.distanceToInMinutes()){
						temproute = null;
						temproute = firstRoute;
					} else {
						temproute = null;
						temproute = secondRoute;
					}
				} else {
					if (temproute != null) {
						if (firstRoute.distanceToInMinutes() < temproute.distanceToInMinutes()) {
							return firstRoute;
						} else {
							return temproute;
						}
					} else
						return firstRoute;
				}

			}
			return temproute;
		}

		return null ;
	}

	public int[] findPort(Port p) {
		int [] retval = null ;
		if (visitedports.isEmpty())
			return retval ;

		retval = new int [visitedports.size()] ;
		Iterator<VisitedPort> ivp = visitedports.iterator() ;
		int ridx = 0 ;
		int vidx = 0 ;
		while (ivp.hasNext()) {
			VisitedPort vp = ivp.next() ;
			retval[vidx] = -1 ;
			if (vp.equals(p))
				retval[ridx++] = vidx ;

			vidx ++ ;
		}

		return retval ;
	}

	public VisitedPort getPortAtIndex (int index) {
		if (index < 0)
			return null ;
		if (index >= visitedports.size())
			return null ;
		return visitedports.get(index) ;
	}

	public int[] findPortPair (ReservationRemote resv) throws RemoteException {
		int [] retval = new int [2] ;
		retval[0] = -1;
		retval[1] = -1;
		
		int [] retvaltemp = new int [2] ;
		retvaltemp[0] = -1;
		retvaltemp[1] = -1;
		boolean isFirstFound = false;

		if (visitedports.isEmpty())
			return retval ;

		Port p1 = scenario.getPort(resv.getOrg()) ;
		Port p2 = scenario.getPort(resv.getDest()) ;

		Iterator<VisitedPort> ivp = visitedports.iterator() ;
		Iterator<VisitedPort> ivp2 = visitedports.iterator() ;
		
		int vidx = 0 ;
		while (ivp.hasNext()) {
			VisitedPort vp = ivp.next() ;
			if (vp.equals(p1) ) {
				if (retval[0]<0 || vidx < (visitedports.size()-1)) {
					if (vp.legafter == null || (vp.legafter.getPaxOnBoard()+1 <= thefleet.paxcapacity)) {
						for (int i = vidx+1; i < visitedports.size(); i++) {
							VisitedPort vptemp = visitedports.get(i);
							if(vptemp.equals(p2)){
								if (!isFirstFound){
									retvaltemp[0] = vidx;
									retvaltemp[1] = i;
									isFirstFound = true;
								}
								if (retvaltemp[1] == i) {
									retvaltemp[0] = vidx;
								}
							}
						}
						retval[0] = vidx ;
					} else {
						if (retval[0] < 0) {
							retval[0] = -99;
						}
					}
				} 
			}
			vidx ++ ;
		}
		
		if (retvaltemp[0] >= 0 && retval[0] > retvaltemp[0]) {
			retval[0] = retvaltemp[0];
			retval[1] = retvaltemp[1];
		} else {
			int vidx2 = 0 ;
			while (ivp2.hasNext()) {
				VisitedPort vp = ivp2.next() ;
				if (vp.equals(p2) && vidx2>0) {
					if (retval[0]<0 || (retval[0]<vidx2)){
						retval[1] = vidx2 ;
						break;
					}
				}
				vidx2 ++ ;
			}
		}
		
		if (retval[0] == -99) {
			retval[1] = -1;
		}

		if (retval[0]>=0 && retval[1]>=0) {
			if (retval[0]>retval[1]) {
				retval[1] = -10;
			}

		}

		return retval ;
	}


	public int[] oldfindPortPair (Reservation resv) {
		int [] retval = new int [2] ;
		retval[0] = -1;
		retval[1] = -1;

		if (visitedports.isEmpty())
			return retval ;

		Port ps[] = new Port[2] ;
		int index = 0 ;								// start from origin
		ps[0] = scenario.getPort(resv.getOrg()) ;
		ps[1] = scenario.getPort(resv.getDest()) ;

		Iterator<VisitedPort> ivp = visitedports.iterator() ;
		int vidx = 0 ;
		while (ivp.hasNext() && index<2) {
			VisitedPort vp = ivp.next() ;
			if (vp.equals(ps[index])) {
				retval[index] = vidx ;
				index ++ ;
			}
			vidx ++ ;
		}

		return retval ;
	}


	public class VisitedPort implements IFuelPoint, IValidationPoint {
		Port theport ;
		Fleet thefleet ;
		int  paxon ;
		int  paxoff ;
		Double weighton ;
		Double weightoff ;
		Leg  legafter ;
		Leg  legbefore ;
		Double fuelneed ;               // minimum fuel needed to go to this port
		Double fueluploadneed ;         // minimum fuel needed to complete the trip
		Double payload ;                // maximum passenger kgs
		boolean pobvalid ;
		boolean wobvalid ;
		boolean fuelvalid ;
		boolean isrefueling ;
		boolean seqvalid;

		VisitedPort (Fleet thefleet, Port theport) {
			this.theport = theport ;
			this.thefleet = thefleet ;
			paxon = 0 ;
			paxoff = 0 ;
			weighton = 0.0 ;
			weightoff = 0.0 ;
			fuelneed = 0.0 ;
			fueluploadneed = 0.0 ;
			payload = 0.0 ;

			pobvalid = true ;
			wobvalid = true ;
			fuelvalid = true ;
			isrefueling = false ;
			seqvalid = true;
		}

		public boolean isIsrefueling() {
			return isrefueling;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Port) {
				return theport.port.equals(((Port)obj).getPort()) ;
			}
			else if (obj instanceof VisitedPort) {
				return theport.port.equals(((VisitedPort)obj).theport.getPort()) ;
			}
			return super.equals(obj);
		}

		public Leg getLegAfter () {
			return legafter ;
		}

		public Leg getLegBefore () {
			return legbefore ;
		}

		public String getPort () {
			if (theport!=null)
				return theport.getPort() ;
			return null ;
		}

		void paxOff (ReservationRemote resv, VisitedPort paxOff) throws RemoteException {
			paxOn (-resv.getQty(), -resv.getWeight(), paxOff, resv) ;
		}

		void paxOn (ReservationRemote resv, VisitedPort paxOff) throws RemoteException {
			paxOn (resv.getQty(), resv.getWeight(), paxOff, resv) ;
		}

		void paxOn (int qty, Double weight, VisitedPort paxOff, ReservationRemote resv) throws RemoteException {
			paxon += qty  ;
			weighton += weight ;

			paxOff.paxoff += qty ;
			paxOff.weightoff += weight ;

			int prevpax ;
			Double prevweight ;
			VisitedPort p = this ;
			while (p != null) {
				if (p.legbefore != null) {
					prevpax = p.legbefore.paxonboard ;
					prevweight = p.legbefore.weightonboard ;
				}
				else {
					prevpax = 0 ;
					prevweight = 0.0 ;
				}

				Leg la = p.legafter ;

				if (la != null) {
					la.paxonboard = prevpax + p.paxon - p.paxoff ;
					la.weightonboard = prevweight + p.weighton - p.weightoff ;

					if (qty >= 0)
						la.addPax(resv) ;
					else
						la.removePax(resv) ;

					p = la.to ;

					if (p.equals(paxOff))
						p = null ;
				}
				else
					p = null ;
			}

		}

		public Double getPenalty () {
			return this.theport.getPenalty () ;
		}

		public Double getTaxitime () {
			return this.theport.getTaxitime();
		}

		public boolean setRefueling (boolean isrefuel) {
			if (this.theport.hasRefuelingCapability) {
				if (isrefuel)
					System.out.println("Set " + theport + " as refueling station ");

				isrefueling = isrefuel ;
			}

			return isrefueling ;
		}

		@Override
		public Double fuelNeed() {
			return fuelneed ;
		}

		@Override
		public String toString() {
			return this.theport.toString() ;
		}

		public boolean hasRefuelingCapability(){
			return this.theport.hasRefuelingCapability;
		}

		@Override
		public boolean validate() {
			wobvalid = true ;
			pobvalid = true ;
			fuelvalid = true ;
			if (legafter != null) {
				wobvalid = false ;
				pobvalid = false ;

				if (payload >= legafter.weightonboard)
						wobvalid = true ;
				if (this.thefleet.paxcapacity >= legafter.paxonboard){
					if (seqvalid){
						pobvalid = true ;
					}
				}


				/*
				try {
					if (!wobvalid)
						System.out.println("WOB invalid : (" + this.thefleet.acreg + "," + this.theport.port + ") on route  " + this.thefleet.getRoute()) ;
					if (!pobvalid)
						System.out.println("POB invalid : (" + this.thefleet.acreg + "," + this.theport.port + ") on route  " + this.thefleet.getRoute() + " pax on board = " + legafter.paxonboard) ;
				}
				catch (Exception e) {
					e.printStackTrace() ;
				}
				*/

			}
			if (legbefore != null) {
				fuelvalid = false ;
				if (this.fuelNeed()<=thefleet.tankcapacity)
					fuelvalid = true ;

				/*
				try {
					if (!fuelvalid)
						System.out.println("FUEL invalid : (" + this.thefleet.acreg + "," + this.theport.port + ") on route  " + this.thefleet.getRoute()) ;
				}
				catch (Exception e) {
					e.printStackTrace() ;
				}
				*/

			}

			return wobvalid && pobvalid && fuelvalid;
		}

		@Override
		public boolean isPOBValid() {
			return pobvalid ;
		}

		@Override
		public boolean isWOBValid() {
			return wobvalid ;
		}
	}

	@Override
	public String toString() {
		Iterator<VisitedPort> ivp = visitedports.iterator() ;
		String routetext = "" ;
		boolean first = true ;
		while (ivp.hasNext()) {
			VisitedPort vp = ivp.next() ;
			if (!first)
				routetext = routetext + "-" ;
			routetext = routetext + vp.theport.port ;
			if (vp.isrefueling)
				routetext += "(F)" ;
			first = false ;
		}
		return routetext ;
	}

	public double getLegTakeoffTime () {
		double totaltime = 0.0;

		Iterator<VisitedPort> ivp = visitedports.iterator() ;
		String routetext = "" ;
		boolean first = true ;
		while (ivp.hasNext()) {
			VisitedPort vp = ivp.next() ;
			if (first) {
				totaltime += vp.getTaxitime();
			} else {
				if (vp.isrefueling) {
					totaltime += this.thefleet.refuelingtime;
				} else {
					totaltime += this.thefleet.groundtime;
				}
			}

			if (!ivp.hasNext()) {
				totaltime += vp.getTaxitime();
			}
			first = false ;
		}
		return totaltime;
	}

	public String[] toPortNameArray () {
		if (visitedports.isEmpty())
			return null ;

		String[] ports = new String[visitedports.size()] ;
		for (int i=0; i<ports.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			ports[i] = vp.theport.getPort() ;
		}
		return ports ;
	}

	public Integer getTankCapacity(){
		Double tc = thefleet.tankcapacity;
		return tc.intValue();
	}

	public boolean[] getRefuelingCapability () {
		if (visitedports.isEmpty())
			return null ;

		boolean[] ports = new boolean[visitedports.size()] ;
		for (int i=0; i<ports.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			ports[i] = vp.hasRefuelingCapability() ;
		}
		return ports ;
	}

	public boolean[] getRefuelingStatus () {
		if (visitedports.isEmpty())
			return null ;

		boolean[] ports = new boolean[visitedports.size()] ;
		for (int i=0; i<ports.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			ports[i] = vp.isrefueling ;
		}
		return ports ;
	}

	public Integer [] toTOWArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] tow = new Integer[visitedports.size()] ;
		for (int i=0; i<tow.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			Double payload = 0.0D ;
			Double crewweight = this.thefleet.getScenario().getConfig().defaultCrewWeight;
			if (vp.getLegAfter() != null)
				payload += vp.getLegAfter().weightonboard ;

			Double d = vp.fueluploadneed + thefleet.eew + payload + crewweight;
			tow[i] = d.intValue() ;
		}
		return tow ;
	}

	// maximum fuel consumption
	public Integer [] toMaxFuelUploadArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] fuels = new Integer[visitedports.size()] ;
		for (int i=0; i<fuels.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			Double maxfuel = thefleet.mtow - vp.getPenalty() - thefleet.eew - vp.payload ;
			if (maxfuel > thefleet.tankcapacity)
				maxfuel = thefleet.tankcapacity ;

			if (maxfuel < vp.fueluploadneed)
				maxfuel = vp.fueluploadneed ;
			fuels[i] = maxfuel.intValue() ;
		}
		return fuels ;
	}


	// minimum fuel consumption
	public Integer [] toFuelUploadArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] fuels = new Integer[visitedports.size()] ;
		for (int i=0; i<fuels.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			fuels[i] = vp.fueluploadneed.intValue() ;
		}
		return fuels ;
	}

	// maximum payload
	public Integer [] toMaxPayLoadArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] fuels = new Integer[visitedports.size()] ;
		for (int i=0; i<fuels.length; i++) {
			VisitedPort vp = visitedports.get(i) ;

			int currentweight = 0;
			Leg l = vp.legafter;
			if (l != null) {
				if (l.weightonboard != null) {
					Double weightob = l.weightonboard;
					if (weightob > 0) {
						currentweight = weightob.intValue();
					} else {
						currentweight = 0;
					}
				} else {
					currentweight = 0;
				}
			} else {
				currentweight = 0;
			}

			Double maxpayload = thefleet.mtow - vp.getPenalty() - thefleet.eew
			- Math.ceil(vp.fueluploadneed) - Math.ceil(currentweight)
			- this.thefleet.getScenario().getConfig().defaultCrewWeight  ;
			fuels[i] = maxpayload.intValue() ;
		}
		return fuels ;
	}

	// current payload
	public Integer [] toPayLoadArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] fuels = new Integer[visitedports.size()] ;
		for (int i=0; i<fuels.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			fuels[i] = vp.payload.intValue() ;
		}
		return fuels ;
	}

	public boolean [] toFuelValidityArray () {
		if (visitedports.isEmpty())
			return null ;

		boolean[] fuelvalidity = new boolean[visitedports.size()] ;
		for (int i=0; i<fuelvalidity.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			fuelvalidity[i] = vp.fuelvalid ;
		}
		return fuelvalidity ;
	}

	public boolean isValid () {
		for (int i=0; i<visitedports.size(); i++) {
			VisitedPort vp = visitedports.get(i) ;
			if (!vp.isPOBValid()){
				System.err.println("pob error");
				return false ;
			}
			if (!vp.isWOBValid()){
				System.err.println("wob error");
				return false ;
			}
			if (!vp.fuelvalid){
				System.err.println("fuel error");
				return false ;
			}
		}

		return true ;
	}

	public boolean [] toPOBValidityArray () {
		if (visitedports.isEmpty())
			return null ;

		boolean[] pobvalidity = new boolean[visitedports.size()] ;
		for (int i=0; i<pobvalidity.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			pobvalidity[i] = vp.isPOBValid() ;
		}
		return pobvalidity ;
	}

	public boolean [] toWOBValidityArray () {
		if (visitedports.isEmpty())
			return null ;

		boolean[] wobvalidity = new boolean[visitedports.size()] ;
		for (int i=0; i<wobvalidity.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			wobvalidity[i] = vp.isWOBValid() ;
		}
		return wobvalidity ;
	}

	// return list of Pax On/Off in string as : x/y whereas x = pax-on, y = pax-off
	public String [] toPaxOnOffArray () {
		if (visitedports.isEmpty())
			return null ;

		String[] poo = new String[visitedports.size()] ;
		for (int i=0; i<poo.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			poo[i] = vp.paxon + "/" + vp.paxoff;
		}
		return poo ;
	}

	public Integer [] toWeightOnOffArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] weightonoffs = new Integer[visitedports.size()] ;
		for (int i=0; i<weightonoffs.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			Leg l = vp.legafter;
			if (l != null) {
				if (l.weightonboard != null) {
					Double weightob = l.weightonboard;
					if (weightob > 0) {
						weightonoffs[i] = weightob.intValue();
					} else {
						weightonoffs[i] = 0;
					}
				} else {
					weightonoffs[i] = 0;
				}
			} else {
				weightonoffs[i] = 0;
			}
		}
		return weightonoffs ;
	}

	public Integer [] toPaxOnArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] paxons = new Integer[visitedports.size()] ;
		for (int i=0; i<paxons.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			paxons[i] = vp.paxon ;
		}
		return paxons ;
	}

	public Integer [] toPaxOffArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] paxoffs = new Integer[visitedports.size()] ;
		for (int i=0; i<paxoffs.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			paxoffs[i] = vp.paxoff ;
		}
		return paxoffs ;
	}

	public Integer [] toFuelNeedArray () {
		if (visitedports.isEmpty())
			return null ;

		Integer[] fuels = new Integer[visitedports.size()] ;
		for (int i=0; i<fuels.length; i++) {
			VisitedPort vp = visitedports.get(i) ;
			fuels[i] = vp.fuelneed.intValue() ;
		}
		return fuels ;
	}

	@Override
	public Double tripFuel() {
		Double fuelneed = 0.0;
		if (startpos != null)
			return startpos.fueluploadneed ;

		return fuelneed;
	}

	private Leg getFirstLeg (ReservationRemote resv) throws RemoteException {
		VisitedPort p = startpos ;
		while (p != null) {
			Leg la = p.legafter ;

			if (la != null) {
				if (la.reservations.containsKey(resv.getId()))
					return la ;
				p = la.to ;
			}
			else
				p = null ;
		}

		return null ;
	}

	private Leg getLastLeg (ReservationRemote resv) throws RemoteException {
		VisitedPort p = endpos ;
		while (p != null) {
			Leg lb = p.legbefore ;

			if (lb != null) {
				if (lb.reservations.containsKey(resv.getId()))
					return lb ;
				p = lb.from ;
			}
			else
				p = null ;
		}

		return null ;
	}

	public Double getServeTime (ReservationRemote resv) throws RemoteException {
		Double etd = Double.MIN_VALUE / 2 ;
		Double eta = Double.MAX_VALUE / 2 ;

		Leg lg0 = getFirstLeg (resv) ;
		Leg lg1 = getLastLeg (resv) ;

		if (lg0 != null)
			etd = lg0.etd ;
		if (lg1 != null)
			eta = lg1.eta ;

		return eta - etd ;
	}

	public Double getTotalServeTime () throws RemoteException {
		return getTotalServeTime(ReservationRemote.PRIORITY_NORMAL) ;
	}

	public Double getTotalServeTime (int minPriority) throws RemoteException {
		Double servetime = 0.0 ;
		for (Iterator<Integer> iterator = reservations.keySet().iterator(); iterator.hasNext();) {
			Integer resvid = iterator.next();
			ReservationRemote resv = reservations.get(resvid) ;
			if (resv.getPriority()>=minPriority)
				servetime += getServeTime (resv) ;
		}
		return servetime ;
	}

	public Double getTotalDirectTime  (int minPriority) throws RemoteException {
		Double directtime = 0.0 ;
		for (Iterator<Integer> iterator = reservations.keySet().iterator(); iterator.hasNext();) {
			Integer resvid = iterator.next();
			ReservationRemote resv = reservations.get(resvid) ;
			if (resv.getPriority()>=minPriority) {
				Port org = scenario.getPort(resv.getOrg());
				Port dest = scenario.getPort(resv.getDest());
				directtime += (distanceInMinutes(scenario, org, dest, thefleet.maxspeed)+ org.taxitime + dest.taxitime) ;
			}
		}
		return directtime ;
	}

	@Override
	public Double distanceToInMinutes() {
		boolean bFirst = true  ;
		Double distance = 0.0 ;
		VisitedPort p = startpos ;

		Double groundtime = 0.0 ;
		while (p != null) {
			if (!bFirst) {
				if (p.isrefueling)
					groundtime += thefleet.refuelingtime ;
				else
					groundtime += thefleet.groundtime ;
			}

			Leg la = p.legafter ;
			if (la != null) {
				distance += la.distanceToInMinutes() ;
				p = la.to ;
			}
			else
				p = null ;

			bFirst = false ;
		}

		return distance ;
	}

	@Override
	public IFuelConsumer getConsumer() {
		return thefleet ;
	}

	static Double distanceInMinutes(FlightScenario scenario, Port p1, Port p2, Double maxspeed) {
		Distance dist = scenario.getDistance(p1, p2) ;
		if (dist == null)
			dist = Distance.getTempDistance(scenario, p1, p2, maxspeed) ;

		if (scenario.getCalculationBase() == Configuration.CALCBASE_DISTANCE) {
			Double tas = maxspeed ;
			return 60 * dist.getInNMI() / tas ;
		}
		else
			return dist.getInMinutes() ;
	}

}
