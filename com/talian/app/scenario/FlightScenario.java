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
package com.talian.app.scenario;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import psdi.mbo.MboConstants;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.MXSession;

import com.talian.app.fuel.ITripFuelCalc;
import com.talian.app.fuel.SimpleFuelCalc;
import com.talian.app.heli.Fleet;
import com.talian.app.reservation.Reservation;
import com.talian.app.reservation.ReservationRemote;
import com.talian.app.route.Distance;
import com.talian.app.route.Leg;
import com.talian.app.route.Port;
import com.talian.app.route.Route;
import com.talian.app.route.Route.VisitedPort;
import com.talian.app.scenass.ScenassSet;
import com.talian.app.scenass.ScenassSetRemote;
import com.talian.app.scenresv.ScenresvSetRemote;

/**
 * @author Seno
 * Class for managing flight session scenario
 * This class is optimized for speed, so there is redundant variables
 */
public class FlightScenario implements MboConstants {
	private String scenarioId ;
	private String description ;
	private String status ;

	private HashMap<String, Fleet> availableFleet ;
	private HashMap<String, Port> availablePorts ;
	private HashMap<String, Port> refuelingPorts ;
	private HashMap<String, Distance> distanceTable ;
	private MboRemote mbo ;
	private MXSession mxsession ;
	private Configuration config ;
	private ITripFuelCalc tripcalc ;
	private boolean isChanged ;

	public FlightScenario () {
		setScenarioId(null) ;
		availableFleet = new HashMap<String, Fleet>() ;
		availablePorts = new HashMap<String, Port>() ;
		refuelingPorts = new HashMap<String, Port>() ;
		distanceTable = new HashMap<String, Distance>() ;
		tripcalc = new SimpleFuelCalc() ;
		isChanged = false ;

		description = "" ;
		status = "DRAFT" ;
	}

	private void setMbo (MboRemote mbo) {
		this.mbo = mbo ;
	}

	public Fleet[] getFleetArray () {
		if (availableFleet.isEmpty())
			return null ;

		Fleet p[] = new Fleet[availableFleet.size()] ;
		Iterator<String> it =availableFleet.keySet().iterator() ;
		for (int i=0; it.hasNext(); i++) {
			p[i] = availableFleet.get(it.next()) ;
		}

		return p ;
	}

	public Port[] getPortArray () {
		if (availablePorts.isEmpty())
			return null ;

		Port p[] = new Port[availablePorts.size()] ;
		Iterator<String> it =availablePorts.keySet().iterator() ;
		for (int i=0; it.hasNext(); i++) {
			p[i] = availablePorts.get(it.next()) ;
		}

		return p ;
	}

	public Port[] getRefuelingPortArray () {
		if (refuelingPorts.isEmpty())
			return null ;

		Port p[] = new Port[refuelingPorts.size()] ;
		Iterator<String> it =refuelingPorts.keySet().iterator() ;
		for (int i=0; it.hasNext(); i++) {
			p[i] = refuelingPorts.get(it.next()) ;
		}

		return p ;
	}


	// score is calculated based on distance in minutes
	public Double getScore () throws RemoteException, MXException {
		Iterator<String> it = availableFleet.keySet().iterator() ;
		Double score = 0.0 ;
		while (it.hasNext()) {
			String acreg = it.next() ;
			Fleet thefleet = availableFleet.get(acreg) ;
			Route theroute = thefleet.getRoute() ;

			score += (theroute.distanceToInMinutes() + theroute.getLegTakeoffTime());
		}
		return score ;
	}

	public Fleet getFleet (String acreg) {
		return availableFleet.get(acreg) ;
	}

	public void setFleetAvailability (String acreg, Fleet newFleet, boolean isActivate) {
		Fleet fleet = availableFleet.get(acreg) ;

		if (isActivate) {
			if (fleet == null && newFleet!=null) {
				newFleet.setScenario(this) ;
				availableFleet.put(acreg, newFleet) ;
			}
		}
		else {
			if (fleet!=null) {
				availableFleet.remove(acreg) ;
			}
		}

	}

	public HashMap<String, Fleet> getAvailableFleet () {
		return availableFleet ;
	}

	public HashMap<String, Port> getAvailablePorts () {
		return availablePorts ;
	}

	@Override
	public String toString() {
		String retval = "" ;
//		Iterator<Fleet> itfl = availableFleet.values().iterator() ;
//		try {
//			retval = this.scenarioId + ",score : " + Math.round(this.getScore()) + "\n" ;
//
//			while (itfl.hasNext()) {
//				Fleet fl = itfl.next() ;
//				Route rt = fl.getRoute() ;
//
//				retval += "acreg " + fl + " : " + rt + "\n";
//			}
//		} catch (Exception e) {
//			retval = e.getMessage() ;
//			e.printStackTrace();
//		}

		retval = this.scenarioId;
		return retval ;
	}

	public Port getPort (String port) {
		return availablePorts.get(port) ;
	}

	public Distance getDistance (String p1, String p2) {
		return distanceTable.get(p1 + p2) ;
	}

	public Distance getDistance (Port p1, Port p2) {
		return distanceTable.get(p1.getPort()+p2.getPort()) ;
	}

	public ITripFuelCalc getTripFuelCalculator () {
		return tripcalc ;
	}

	public Configuration getConfig () {
		return config ;
	}

	public int getCalculationBase () {
		return config.calculationBase ;
	}

	public void cleanUp () throws RemoteException, MXException {
		Iterator<String> it = availableFleet.keySet().iterator() ;
		while (it.hasNext()) {
			String acreg = it.next() ;
			Fleet thefleet = availableFleet.get(acreg) ;
			thefleet.clearAssignment() ;
		}
	}

	public void adjustReservationTiming () throws RemoteException, MXException {
		Iterator<String> it = availableFleet.keySet().iterator() ;
		while (it.hasNext()) {
			String acreg = it.next() ;
			Fleet thefleet = availableFleet.get(acreg) ;
			Route r = thefleet.getRoute() ;
			r.adjustReservationTiming() ;
		}
	}

	public boolean isModified () {
		return isChanged ;
	}

	public void setModified () {
		isChanged = true ;
	}

	public void clearModified () {
		isChanged = false ;
	}

	public void setOAT (Double oat) {
		config.defaultOAT = oat ;
	}

	public void setCrewWeight (Double cw) {
		config.defaultCrewWeight = cw ;
	}

	public void setHeadwind (Double hw) {
		config.defaultHeadwind = hw ;
	}

	void resetReservationTiming () throws RemoteException, MXException {
		Iterator<String> it = availableFleet.keySet().iterator() ;
		while (it.hasNext()) {
			String acreg = it.next() ;
			Fleet thefleet = availableFleet.get(acreg) ;
			Route theroute = thefleet.getRoute();
			theroute.resetReservationTiming();
		}
	}

	public void updateFleet(String acreg) throws RemoteException, MXException{
		ScenassSetRemote oldscenassset = (ScenassSetRemote)mbo.getMboSet("scenass#1", "scenass",
				"scenarioid="+getScenarioId() + " and acreg='"+acreg+"'") ;
		oldscenassset.deleteAll();

		//delete old scenario reservation
		ScenresvSetRemote oldscenresvset = (ScenresvSetRemote)mbo.getMboSet("scenresv#1", "scenresv",
				"scenarioid="+getScenarioId()+ " and acreg='"+acreg+"'") ;
		oldscenresvset.deleteAll();

		//delete old scenario assignment
		MboSetRemote oldresvonlegset = (MboSetRemote)mbo.getMboSet("resvonleg#1", "resvonleg",
				"scenarioid="+getScenarioId()+ " and acreg='"+acreg+"'") ;
		oldresvonlegset.deleteAll();


		Fleet fleet = getFleet(acreg);
		Route usedRoute = fleet.getRoute();
		String[] usedPorts = usedRoute.toPortNameArray();
		if (usedPorts != null) {
			for (int i = 0; i < usedPorts.length; i++) {
				MboRemote mbo = oldscenassset.add();
				String port = usedPorts[i];
				mbo.setValue("scenarioid", getScenarioId());
				mbo.setValue("heliport", port);
				mbo.setValue("sequence", i+1);
				mbo.setValue("acreg", acreg);
				mbo.setValue("refuel", usedRoute.getRefuelingStatus()[i]);
			}
		}


		HashMap<Integer, ReservationRemote> currResv = usedRoute.getReservation();
		if (currResv != null) {
			Iterator<Integer> it6 = currResv.keySet().iterator();
			int resvidx = 0;
			while (it6.hasNext()) {
				MboRemote mbo = oldscenresvset.add();
				mbo.setValue("scenarioid", getScenarioId());
				mbo.setValue("reservationid", it6.next());
				mbo.setValue("sequence", resvidx);
				mbo.setValue("acreg", acreg);
				resvidx++;
			}
		}

		// Route
		VisitedPort vp = usedRoute.getStartPos() ;

		int legseq = 1 ;
		while (vp != null) {
			Leg la = vp.getLegAfter () ;
			if (la != null) {
				List<ReservationRemote> servedpax = la.getServedPax() ;
				if (servedpax != null) {
					Iterator<ReservationRemote> ir = servedpax.iterator() ;
					int order = 1 ;
					while (ir.hasNext()) {
						ReservationRemote r  = ir.next() ;
						MboRemote mbo = oldresvonlegset.add();

						mbo.setValue("ord", order);
						mbo.setValue("acreg", acreg);
						mbo.setValue("legseq", legseq);
						mbo.setValue("leg", la.toString());
						mbo.setValue("legorg", la.getFrom().toString());
						mbo.setValue("legdest", la.getTo().toString());
						mbo.setValue("scenarioid", getScenarioId());
						mbo.setValue("displayname", r.getString("displayname"));
						mbo.setValue("company", r.getString("company"));
						mbo.setValue("dest", r.getString("dest"));
						mbo.setValue("org", r.getString("org"));
						mbo.setValue("pov", r.getString("pov"));
						mbo.setValue("flightsession", getFlightSession());

						int hours = Integer.parseInt(r.getString("flightsession").substring(0, 2));
						int minutes = Integer.parseInt(r.getString("flightsession").substring(2, 4));

						Date dtD = r.getDate("reservedate");
						dtD.setHours(hours);
						dtD.setMinutes((minutes+r.getEtd().intValue()));
						String sEtd = String.format("%02d:%02d", dtD.getHours(), dtD.getMinutes());
						mbo.setValue("etd", sEtd);

						Date dtA = r.getDate("reservedate");
						dtA.setHours(hours);
						dtA.setMinutes((minutes+r.getEta().intValue()));
						String sEta = String.format("%02d:%02d", dtA.getHours(), dtA.getMinutes());
						mbo.setValue("eta", sEta);

						if (r.getString("org").equalsIgnoreCase(la.getFrom().toString())) {
							mbo.setValue("transit", "NO");
						} else {
							mbo.setValue("transit", "YES");
						}
						order ++ ;
					}
				}
			}
			legseq ++ ;
			if (la != null)
				vp = la.getTo() ;
			else
				vp = null ;
		}

		mbo.getThisMboSet().save();
	}

	public void save () throws RemoteException, MXException {
		if (mbo == null)
			return ;

		resetReservationTiming();
		adjustReservationTiming();
		
		isChanged = false ;

		mbo.setValue("description", description, NOACCESSCHECK) ;
		mbo.setValue("status", status, NOACCESSCHECK) ;
		mbo.setValue("totsvctime", this.getScore(), NOACCESSCHECK);

		if (mbo.isNew()) {

			MboSetRemote fleets = mbo.getMboSet("availfleets") ;
			MboSetRemote resvonleg = mbo.getMboSet("resvonleg") ;
			Iterator<String> it = availableFleet.keySet().iterator() ;
			while (it.hasNext()) {
				String acreg = it.next() ;
				Fleet thefleet = availableFleet.get(acreg) ;

				// Route
				Route route = thefleet.getRoute() ;
				VisitedPort vp = route.getStartPos() ;

				int legseq = 1 ;
				while (vp != null) {
					Leg la = vp.getLegAfter () ;
					if (la != null) {
						List<ReservationRemote> servedpax = la.getServedPax() ;
						if (servedpax != null) {
							Iterator<ReservationRemote> ir = servedpax.iterator() ;
							int order = 1 ;
							while (ir.hasNext()) {
								ReservationRemote r  = ir.next() ;
								MboRemote mbo = resvonleg.add();
								mbo.setValue("ord", order);
								mbo.setValue("acreg", acreg);
								mbo.setValue("legseq", legseq);
								mbo.setValue("leg", la.toString());
								mbo.setValue("legorg", la.getFrom().toString());
								mbo.setValue("legdest", la.getTo().toString());
								mbo.setValue("scenarioid", getScenarioId());
								mbo.setValue("displayname", r.getString("displayname"));
								mbo.setValue("company", r.getString("company"));
								mbo.setValue("dest", r.getString("dest"));
								mbo.setValue("org", r.getString("org"));
								mbo.setValue("pov", r.getString("pov"));
								mbo.setValue("flightsession", getFlightSession());
								mbo.setValue("etd", r.getEtdAsString());
								mbo.setValue("eta", r.getEtaAsString());

								System.err.println(r +"---"+ getScenarioId()+" O:"+r.getOrg()+" D:"+r.getDest()
										+" "+ r.getString("displayname")+ ", etd "+ r.getEtd()+ ", eta "+ r.getEta() +
										", etd "+ r.getEtdAsString()+ ", eta "+ r.getEtaAsString());

								if (r.getString("org").equalsIgnoreCase(la.getFrom().toString())) {
									mbo.setValue("transit", "NO");
								} else {
									mbo.setValue("transit", "YES");
								}
								order ++ ;
							}
						}
					}

					legseq ++ ;
					if (la != null) {
						vp = la.getTo() ;
					}
					else
						vp = null ;
				}
				MboRemote flt = fleets.add() ;
				flt.setValue ("scenarioid", getScenarioId()) ;
				thefleet.save (flt) ;
			}

			MboSetRemote ports = mbo.getMboSet("availports") ;
			Iterator<String> it2 = availablePorts.keySet().iterator() ;
			while (it2.hasNext()) {
				String port = it2.next() ;
				Port theport = availablePorts.get(port) ;
				MboRemote prt = ports.add() ;
				prt.setValue ("scenarioid", getScenarioId()) ;
				theport.save (prt) ;
			}

			MboSetRemote configs = mbo.getMboSet("scenconfig") ;
			MboRemote cfg = configs.add() ;
			cfg.setValue ("scenarioid", getScenarioId()) ;
			config.save (cfg) ;

			//disabled and will processed in oracle procedure
//			MboSetRemote distance = mbo.getMboSet("scendistance") ;
//			Iterator<String> it3 = distanceTable.keySet().iterator() ;
//			while (it3.hasNext()) {
//				String key = it3.next() ;
//				Distance thedistance = distanceTable.get(key) ;
//				MboRemote dist = distance.add() ;
//				dist.setValue("scenarioid", getScenarioId());
//				thedistance.save (dist) ;
//			}

			Iterator<String> it4 = availableFleet.keySet().iterator();
			ScenassSetRemote scenassset = (ScenassSetRemote)mbo.getMboSet("scenass", "scenass", "scenarioid="+getScenarioId()) ;
//			MboSetRemote ms = mbo.getMboSet("resvonleg#2", "resvonleg", "scenarioid="+getScenarioId());
			while (it4.hasNext()) {
				Fleet usedFleet = getFleet(it4.next());
				Route usedRoute = usedFleet.getRoute();

				VisitedPort vp = usedRoute.getStartPos() ;
				int seq = 0 ;
				double etd = -1 ;
				double eta = -1 ;
				String port ;
				String s_etd ;
				String s_eta ;
				Double m0 = Reservation.toStdTime(getFlightSession()) ;
				boolean isrefueling = false ;

				while (vp != null) {
					Leg la = vp.getLegAfter() ;
					Leg lb = vp.getLegBefore() ;

					port = vp.getPort() ;
					isrefueling = vp.isIsrefueling() ;
					etd = -1 ;
					eta = -1 ;
					seq ++ ;

					if (lb!=null)
						eta = lb.getETA() ;

					if (la!=null) {
						etd = la.getETD() ;
						vp = la.getTo() ;
					}
					else
						vp = null ;

					s_etd = "" ;
					s_eta = "" ;
					if (etd>=0)
						s_etd = Reservation.toNormalTime(m0, etd) ;
					if (eta>=0)
						s_eta = Reservation.toNormalTime(m0, eta) ;

					// save here
					//
					MboRemote mbo = scenassset.add();
					mbo.setValue("scenarioid", getScenarioId());
					mbo.setValue("heliport", port);
					mbo.setValue("sequence", seq);
					mbo.setValue("acreg", usedFleet.getACREG());
					mbo.setValue("refuel", isrefueling);
					mbo.setValue("etd", s_etd);
					mbo.setValue("eta", s_eta);
				}
//				scenassset.save();
			}

			Iterator<String> it5 = availableFleet.keySet().iterator();
			ScenresvSetRemote scenresvset = (ScenresvSetRemote)mbo.getMboSet("scenresv") ;
			while (it5.hasNext()) {
				Fleet usedFleet = getFleet(it5.next());
				Route usedRoute = usedFleet.getRoute();
				HashMap<Integer, ReservationRemote> currResv = usedRoute.getReservation();
				if (currResv != null) {
					Iterator<Integer> it6 = currResv.keySet().iterator();
					int resvidx = 0;
					while (it6.hasNext()) {
						MboRemote mbo = scenresvset.add();
						mbo.setValue("scenarioid", getScenarioId());
						mbo.setValue("reservationid", it6.next());
						mbo.setValue("sequence", resvidx);
						mbo.setValue("acreg", usedFleet.getACREG());
						resvidx++;
					}
				}
			}
			mbo.getThisMboSet().save();
		}else {
			String oldstatus = mbo.getString("status");
			mbo.setValue("status", "UPD");
			//delete old scenario assignment
			ScenassSetRemote oldscenassset = (ScenassSetRemote)mbo.getMboSet("scenass#1", "scenass",
					"scenarioid="+getScenarioId()) ;
			oldscenassset.deleteAll();

			//delete old scenario reservation
			ScenresvSetRemote oldscenresvset = (ScenresvSetRemote)mbo.getMboSet("scenresv#1", "scenresv",
					"scenarioid="+getScenarioId()) ;
			oldscenresvset.deleteAll();

			//delete old scenario assignment
			MboSetRemote oldresvonlegset = (MboSetRemote)mbo.getMboSet("resvonleg#1", "resvonleg",
					"scenarioid="+getScenarioId()) ;
			oldresvonlegset.deleteAll();


			Iterator<String> it4 = availableFleet.keySet().iterator();
			while (it4.hasNext()) {
				Fleet usedFleet = getFleet(it4.next());
				Route usedRoute = usedFleet.getRoute();

				VisitedPort vp = usedRoute.getStartPos() ;
				int seq = 0 ;
				double etd = -1 ;
				double eta = -1 ;
				String port ;
				String s_etd ;
				String s_eta ;
				Double m0 = Reservation.toStdTime(getFlightSession()) ;
				boolean isrefueling = false ;

				while (vp != null) {
					Leg la = vp.getLegAfter() ;
					Leg lb = vp.getLegBefore() ;

					port = vp.getPort() ;
					isrefueling = vp.isIsrefueling() ;
					etd = -1 ;
					eta = -1 ;
					seq ++ ;

					if (lb!=null)
						eta = lb.getETA() ;

					if (la!=null) {
						etd = la.getETD() ;
						vp = la.getTo() ;
					}
					else
						vp = null ;

					s_etd = "" ;
					s_eta = "" ;
					if (etd>=0)
						s_etd = Reservation.toNormalTime(m0, etd) ;
					if (eta>=0)
						s_eta = Reservation.toNormalTime(m0, eta) ;

					// save here
					//
					MboRemote mbo = oldscenassset.add();
					mbo.setValue("scenarioid", getScenarioId());
					mbo.setValue("heliport", port);
					mbo.setValue("sequence", seq);
					mbo.setValue("acreg", usedFleet.getACREG());
					mbo.setValue("refuel", isrefueling);
					mbo.setValue("etd", s_etd);
					mbo.setValue("eta", s_eta);
				}
//				scenassset.save();
			}

			Iterator<String> it5 = availableFleet.keySet().iterator();
			while (it5.hasNext()) {
				Fleet usedFleet = getFleet(it5.next());
				Route usedRoute = usedFleet.getRoute();
				HashMap<Integer, ReservationRemote> currResv = usedRoute.getReservation();
				if (currResv != null) {
					Iterator<Integer> it6 = currResv.keySet().iterator();
					int resvidx = 0;
					while (it6.hasNext()) {
						MboRemote mbo = oldscenresvset.add();
						mbo.setValue("scenarioid", getScenarioId());
						mbo.setValue("reservationid", it6.next());
						mbo.setValue("sequence", resvidx);
						mbo.setValue("acreg", usedFleet.getACREG());
						resvidx++;
					}
				}
			}

			Iterator<String> it = availableFleet.keySet().iterator() ;
			while (it.hasNext()) {
				String acreg = it.next() ;
				Fleet thefleet = availableFleet.get(acreg) ;

				// Route
				Route route = thefleet.getRoute() ;
				VisitedPort vp = route.getStartPos() ;

				int legseq = 1 ;
				while (vp != null) {
					Leg la = vp.getLegAfter () ;
					if (la != null) {
						List<ReservationRemote> servedpax = la.getServedPax() ;
						if (servedpax != null) {
							Iterator<ReservationRemote> ir = servedpax.iterator() ;
							int order = 1 ;
							while (ir.hasNext()) {
								ReservationRemote r  = ir.next() ;
								MboRemote mbo = oldresvonlegset.add();

								mbo.setValue("ord", order);
								mbo.setValue("acreg", acreg);
								mbo.setValue("legseq", legseq);
								mbo.setValue("leg", la.toString());
								mbo.setValue("legorg", la.getFrom().toString());
								mbo.setValue("legdest", la.getTo().toString());
								mbo.setValue("scenarioid", getScenarioId());
								mbo.setValue("displayname", r.getString("displayname"));
								mbo.setValue("company", r.getString("company"));
								mbo.setValue("dest", r.getString("dest"));
								mbo.setValue("org", r.getString("org"));
								mbo.setValue("pov", r.getString("pov"));
								mbo.setValue("flightsession", getFlightSession());
								mbo.setValue("etd", r.getEtdAsString());
								mbo.setValue("eta", r.getEtaAsString());

								System.err.println(getScenarioId()+" O:"+r.getOrg()+" D:"+r.getDest()
										+" "+ r.getString("displayname")+ ", etd "+ r.getEtd()+ ", eta "+ r.getEta() +
										", etd "+ r.getEtdAsString()+ ", eta "+ r.getEtaAsString());

								if (r.getString("org").equalsIgnoreCase(la.getFrom().toString())) {
									mbo.setValue("transit", "NO");
								} else {
									mbo.setValue("transit", "YES");
								}
								order ++ ;
							}
						}
					}
					legseq ++ ;
					if (la != null)
						vp = la.getTo() ;
					else
						vp = null ;
				}
			}
			mbo.setValue("status", oldstatus);
			mbo.getThisMboSet().save();
		}
	}

	public HashMap<Integer, ReservationRemote> getAssignResv(String acreg) throws RemoteException, MXException {
		Fleet usedFleet = getFleet(acreg);
		if (usedFleet != null) {
			Route usedRoute = usedFleet.getRoute();
			HashMap<Integer, ReservationRemote> currResv = usedRoute.getReservation();
			return currResv;
		}

		return null ;
	}

	static public FlightScenario readfromMBO (MboRemote mbo) throws RemoteException, MXException {
		FlightScenario fs = new FlightScenario() ;
		fs.setMbo(mbo) ;

		fs.setScenarioId(mbo.getString("scenarioid")) ;
		fs.description = mbo.getString("description") ;
		fs.status = mbo.getString("status") ;

		MboSetRemote fleets = mbo.getMboSet("availfleets") ;

		MboRemote fleet = fleets.moveFirst() ;
		while (fleet != null) {
			Fleet flt = Fleet.readfromMBO(fs,fleet) ;
			fs.availableFleet.put(flt.getACREG(), flt) ;
			fleet = fleets.moveNext() ;
		}

		MboSetRemote ports = mbo.getMboSet("availports") ;
		MboRemote port = ports.moveFirst() ;
		while (port != null) {
			Port prt = Port.readfromMBO(fs,port) ;
			fs.availablePorts.put(prt.getPort(), prt) ;
			if (prt.hasRefuelingCapability())
				fs.refuelingPorts.put(prt.getPort(), prt) ;

			port = ports.moveNext();
		}

		MboSetRemote configs = mbo.getMboSet("scenconfig") ;
		MboRemote cfg = configs.moveFirst() ;
		fs.config = Configuration.readfromMBO(cfg) ;

//		MboSetRemote distances = mbo.getMboSet("scendistance") ;
//		MboRemote distance = distances.moveFirst() ;
//		while (distance != null) {
//			Distance d0 = Distance.readfromMBO(fs,distance) ;
//			fs.distanceTable.put(d0.getKey(), d0) ;
//			distance = distances.moveNext() ;
//		}

		return fs ;
	}

	static public FlightScenario reuseScenario (FlightScenario scen, FlightScenario reusable) throws RemoteException, MXException {
		MXSession session = scen.getMXSession() ;

		FlightScenario fs = new FlightScenario() ;
		fs.setMXSession(session) ;

 		MboRemote mbo = null ;
 		if (reusable != null)
 			mbo = reusable.mbo ;
 		if (mbo == null) {
 			MboSetRemote mboset = session.getMboSet ("flightscenario") ;
 			mbo = mboset.add() ;
 		}

		fs.setMbo(mbo) ;
		mbo.setValue("reservedate", scen.getDate()) ;
		mbo.setValue("flightsession", scen.getFlightSession()) ;

		fs.scenarioId = mbo.getString("scenarioid") ;
		fs.availableFleet.putAll(scen.availableFleet) ;
		fs.scenarioId = mbo.getString("scenarioid") ;
		fs.availablePorts.putAll(scen.availablePorts) ;
		fs.refuelingPorts.putAll(scen.refuelingPorts) ;
		fs.distanceTable.putAll(scen.distanceTable) ;
		fs.config = scen.getConfig().copy() ;

		Iterator<String> it = scen.availableFleet.keySet().iterator();
		while (it.hasNext()) {
			String acreg = it.next() ;
			Fleet flt = scen.availableFleet.get(acreg) ;
			Fleet copyflt = flt.copy() ;
			fs.availableFleet.put(acreg, copyflt) ;
		}
		return fs ;
	}

	static public FlightScenario copyScenario (FlightScenario scen) throws RemoteException, MXException {
		return reuseScenario(scen, null) ;
	}


	static public FlightScenario newRecord (MXSession session, Date reserveDate, String flightsession) throws RemoteException, MXException {
		MboSetRemote mboset = session.getMboSet ("flightscenario") ;

		FlightScenario fs = new FlightScenario() ;
		fs.setMXSession(session) ;

 		MboRemote mbo = mboset.add() ;
		fs.setMbo(mbo) ;
		mbo.setValue("reservedate", reserveDate) ;
		mbo.setValue("flightsession", flightsession) ;

		fs.setScenarioId(mbo.getString("scenarioid")) ;
		fs.status = "DRAFT" ;

		MboSetRemote fleets = session.getMboSet("acreg") ;
		fleets.setWhere("status = 'ACTIVE'") ;

		MboRemote fleet = fleets.moveFirst() ;
		while (fleet != null) {
			Fleet flt = Fleet.readfromMBO(fs,fleet) ;
			fs.availableFleet.put(flt.getACREG(), flt) ;
			fleet = fleets.moveNext() ;
		}

		MboSetRemote ports = session.getMboSet ("heliport") ;
		MboRemote prt = ports.moveFirst() ;
		while (prt != null) {
			Port heliport = Port.readfromMBO(fs,prt) ;
			fs.availablePorts.put(heliport.getPort(), heliport) ;
			if (heliport.hasRefuelingCapability())
				fs.refuelingPorts.put(heliport.getPort(), heliport) ;
			prt  = ports.moveNext() ;
		}

		MboSetRemote configs = session.getMboSet("scenconfig") ;
		configs.setWhere("scenarioid is null") ;
		MboRemote cfg = configs.moveFirst() ;
		fs.config = Configuration.readfromMBO(cfg) ;

		MboSetRemote distances = session.getMboSet ("distance") ;
		MboRemote dist = distances.moveFirst() ;
		while (dist != null) {
			Distance d0 = Distance.readfromMBO(fs,dist) ;
			fs.distanceTable.put(d0.getKey(), d0) ;
			dist  = distances.moveNext() ;
		}

		return fs ;
	}

	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getScenarioId() {
		return scenarioId;
	}

	public MXSession getMXSession() {
		return mxsession;
	}

	public void setMXSession (MXSession mxsession) {
		this.mxsession = mxsession ;
	}

	public Date getDate () {
		try {
			if (mbo != null)
				return mbo.getDate("reservedate") ;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null ;
	}

	public String getFlightSession () {
		try {
			if (mbo != null)
				return mbo.getString("flightsession") ;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null ;
	}

	public String[] getRefuelingPorts(Route result){
		String[] currRes = result.toPortNameArray();
		if (currRes==null)
			return null ;

		List<Port> LRefuelingPorts = new ArrayList<Port>();
		Port[] availablePort = getPortArray();
		for (int i = 0; i < availablePort.length; i++) {
			int counter = 0;
			Port port = availablePort[i];
			if (port.hasRefuelingCapability()) {
				for (int j = 0; j < currRes.length; j++) {
					if (currRes[j].equals(port.getPort())) {
						counter++;
					}
				}
				if (counter<2) {
					LRefuelingPorts.add(port);
				}
			}
		}

		String[] refuelingPorts= new String[LRefuelingPorts.size()];
		for (int i=0; i < LRefuelingPorts.size(); i++) {
			refuelingPorts[i] = LRefuelingPorts.get(i).getPort();
		}
		return refuelingPorts;
	}

	public static int MAXFLEET = 64 ;
}
