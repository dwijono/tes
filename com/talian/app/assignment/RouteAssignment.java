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
package com.talian.app.assignment;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.SqlFormat;
import psdi.util.MXException;
import psdi.util.MXSession;

import com.talian.app.heli.Fleet;
import com.talian.app.heli.HeliServiceRemote;
import com.talian.app.reservation.Reservation;
import com.talian.app.reservation.ReservationRemote;
import com.talian.app.route.Port;
import com.talian.app.route.Route;
import com.talian.app.scenario.FlightScenario;

/**
 * @author Seno
 *
 */
public class RouteAssignment {
	FlightScenario scenref ;
	ArrayList<ReservationRemote> reservations ;
	HashMap<String, ReservationRemote> refuelings ;
	HashMap<String, ReservationRemote> roundTrip ;
	HeliServiceRemote svcHeli ;
	Double minScore ;
	Double maxScore;
	boolean isPriorityB = false;

	private static ThreadLocal<Boolean> inChain = new ThreadLocal<Boolean>();

	static int MAX_ITTERATION = 1000 ;

	public RouteAssignment (FlightScenario scen, HeliServiceRemote svcHeli) {
		scenref = scen ;
		reservations = new ArrayList<ReservationRemote> ();
		refuelings = new HashMap<String,ReservationRemote>() ;
		roundTrip = new HashMap<String,ReservationRemote>() ;
		minScore = Double.MAX_VALUE ;
		maxScore = Double.MAX_VALUE;
		this.svcHeli = svcHeli ;
	}

	private void loadReservation () throws RemoteException, MXException {
		MXSession s = scenref.getMXSession() ;
		if (s != null) {
			String flightsession = scenref.getFlightSession() ;
			MboSetRemote rset = s.getMboSet("ARESERVATION") ;
			if (flightsession != null) {
				SqlFormat sqf = new SqlFormat("reservedate=:1 and flightsession='" +flightsession+ "' and paxstatus=1") ;
				sqf.setDate(1, scenref.getDate()) ;
				rset.setWhere(sqf.format()) ;
			}
			else {  // only for test
				SqlFormat sqf = new SqlFormat("reservedate=:1 and paxstatus=1") ;
				sqf.setDate(1, scenref.getDate()) ;
				rset.setWhere(sqf.format()) ;
			}

			rset.setOrderBy("flightsession, org, dest") ;
			MboRemote mbo = rset.moveFirst() ;
			while (mbo != null) {
				ReservationRemote r = Reservation.readfromMBO(mbo) ;
				reservations.add(r) ;
				mbo = rset.moveNext() ;
			}

			Fleet[] fleets = scenref.getFleetArray() ;
			for (int i=0; i<fleets.length; i++) {
				Fleet flt = fleets[i] ;
				refuelings.remove(flt.acreg) ;
				if (flt.refuelport != null) {
					Port pFuel = scenref.getPort(flt.refuelport) ;
					if (pFuel!=null && pFuel.hasRefuelingCapability()) {
						ReservationRemote r = (ReservationRemote)rset.getZombie() ;

						r.setDummyRefueling(true) ;
						r.setServedBy(flt.acreg) ;
						r.setOrg(flt.startpos) ;
						r.setDest(pFuel.getPort()) ;
						reservations.add(r) ;
						refuelings.put(flt.acreg, r) ;
					}
				}
			}
		}
	}

	boolean isBetter (Double score) {
		synchronized (this) {
			return score < minScore ;
		}
	}
	
	boolean isBetterThanMax (Double score, List<FlightScenario> result) throws RemoteException, MXException {
		synchronized (this) {
			
			if (result.size() > 0) {
				for (int i = 0; i < result.size(); i++) {
					double tempScore;
					try {
						tempScore = result.get(i).getScore();
						if (tempScore > maxScore) {
							maxScore = tempScore;
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					} catch (MXException e) {
						e.printStackTrace();
					}
				}
			} 
			
			if (isAlreadyExists(score, result)) {
				return false;
			} else {
				return score < maxScore ;
			}
		}
	}
	
	boolean isAlreadyExists(Double score, List<FlightScenario> result) throws RemoteException, MXException {
		int sameCounter = 0;
		for (int i = 0; i < result.size(); i++) {
			if (score == result.get(i).getScore()) {
				sameCounter++;
			}
		}
		return  sameCounter > 0 ? true : false;
	}

	void updateScore (Double score) {
		synchronized (this) {
			minScore = score ;
		}
	}

	private synchronized double modifyScore(FlightScenario fs, double currScore) throws RemoteException, MXException {
		double modifiedScore = 0.0;

		HashMap<String, Fleet> HFleets = fs.getAvailableFleet();

		Iterator<String> it =  HFleets.keySet().iterator();
		while (it.hasNext()) {
			String acreg = it.next();
			Fleet fl = fs.getFleet(acreg);
			Route route = fl.getRoute();

			double totaltime = route.getTotalServeTime();
			double directtime = route.getTotalDirectTime(2);

			if (directtime > 0) {
				isPriorityB = true;
				modifiedScore += ((totaltime/directtime)) * currScore;
			} else {
				modifiedScore += currScore;
			}
		}
		return modifiedScore;
	}

	// maxtime in miliseconds
	public List<FlightScenario> findNBest (int n, long maxtime, boolean isResume, List<FlightScenario> oldResult) throws RemoteException, MXException {
		List<FlightScenario> result = null;
		int nSolution = 0;
		boolean bQuit = false ;
		
		if (isResume) {
			if (oldResult != null){
				svcHeli.putLog("RESUME") ;
				result = oldResult;
				nSolution = oldResult.size() ;
			} else 
				result = new ArrayList<FlightScenario> () ;
		} else {
			result = new ArrayList<FlightScenario> () ;
			nSolution = 0 ;
		}

		if (inChain()){
			return null ;
		}

		try {
			// timer set up
			long startTime = System.nanoTime();
			long nIteration = 0 ;
			long lastEllapse = 0 ;
//			svcHeli.putLog("Try to find " + n + " best scenario, time-out " + maxtime) ;
			loadReservation() ;
			FlightScenario reusable = null ;
			boolean forceAddRefuelingPoint = false ;

			while (!bQuit) {
				if (svcHeli.isStopping()) {
					bQuit = true ;
					break ;
				}

				FlightScenario fs = calcScenario(reusable, forceAddRefuelingPoint) ;
				if (fs != null) {
					Double score = modifyScore(fs, fs.getScore());

					if (isBetterThanMax(score, result)) {
						nSolution++ ;
						result.add(fs) ;

						long lScore = Math.round(score) ;
						System.out.println(">>>>>>>>>>>>> Found : " + fs.getScenarioId() + ", score : " + lScore );
						updateScore(score) ;

						boolean advDone = false ;
						HashMap<String, Fleet> availableFleet = fs.getAvailableFleet() ;
						Iterator<String> it = availableFleet.keySet().iterator() ;

						for (int i=0; it.hasNext(); i++) {
							Fleet flt = availableFleet.get(it.next()) ;
							Route rt0 = flt.getRoute() ;
							if (isPriorityB){
								Route rt1 = rt0.advanceOptimize() ;
								if (rt1 != null)
									advDone = true ;
							}
						}

						if (advDone) {
							score = fs.getScore() ;
							updateScore(score) ;

							lScore = Math.round(score) ;
							svcHeli.putLog("Update Score : " + fs.getScenarioId() + ", score : " + lScore ) ;
							System.out.println(">>>>>>>>>>>>> Update Score : " + fs.getScenarioId() + ", score : " + lScore );
							System.out.println(fs);
							printReservations("After Optimized", fs);
						}

						if (result.size() > n){
							int delIdx = 0;
							for (int i = 0; i < result.size(); i++) {
								FlightScenario tempDel1 = result.get(delIdx);
								FlightScenario tempDel2 = result.get(i);
								
								if (tempDel1.getScore() < tempDel2.getScore()) {
									delIdx = i;
								}
								
							}
							result.remove(delIdx) ;
							
							for (int i = 0; i < result.size(); i++) {
								for (int j = i+1; j < result.size(); j++) {
									FlightScenario tempDel1 = result.get(i);
									FlightScenario tempDel2 = result.get(i);
									if (tempDel1.getScore() == tempDel2.getScore()) {
										result.remove(i);
									}
								}
							}
							
						}
						
						svcHeli.putLog("Found:") ;
						for (int i = 0; i < result.size(); i++) {
							FlightScenario sclog = result.get(i);
							svcHeli.putLog("SC: " + sclog.getScenarioId() + ", score : " + sclog.getScore() ) ;
						}
						
						reusable = null ;
					}
					else {
						reusable = fs ;
					}
				}
				else {
					System.out.println("===== No solution after " + (++nIteration) + " iteration ") ;

					long ellapse = System.nanoTime() - startTime ;
					if (nSolution==0 && (ellapse>=maxtime * 1000000L/4))
						forceAddRefuelingPoint = true ;
				}

				long ellapse = System.nanoTime() - startTime ;
				if (ellapse > maxtime * 1000000L)
					bQuit = true;
				else {
					long delta = ellapse - lastEllapse ;
					lastEllapse = ellapse ;
					if (delta >= (2000*1000000L)) {
//						if (nSolution>0)
//							svcHeli.putLog("Elapsed time " + ellapse/1000000000L + " secs, Found " + nSolution + " with best time : " + minScore.intValue() );
//						else
//							svcHeli.putLog("Elapsed time " + ellapse/1000000000L + " secs, no solution found ") ;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace() ;
		}
		finally  {
			resetChain() ;
		}

//		svcHeli.putLog("Calculation has finished" );
		if (nSolution>0) {
//			svcHeli.putLog("Found " + nSolution + " with best time : " + minScore.intValue() );
//			svcHeli.putLog("Saving Scenario..." );
		}
		else {
//			svcHeli.putLog("NO feasible solution is found " );
		}
		return result ;
	}

	synchronized void resetChain()
	{
		inChain.set(null);
	}

	synchronized boolean inChain()
	{

		Boolean val = inChain.get();

		if (val==null)
		{
			inChain.set(new Boolean(true));
			return false;
		}

		return true;
	}

	private void putRefueling (Fleet fleet, Port refuelingport) throws RemoteException, MXException {
		ReservationRemote r = refuelings.get(fleet.acreg) ;
		if (r==null) {
			MXSession s = scenref.getMXSession() ;
			MboSetRemote rset = s.getMboSet("ARESERVATION") ;
			r = (ReservationRemote)rset.getZombie() ;
			r.setDummyRefueling(true) ;
			// r = new Reservation((MboSet)rset, 0, true) ;

			r.setServedBy(fleet.acreg) ;
			r.setOrg(fleet.startpos) ;
			r.setDest(refuelingport.getPort()) ;
			reservations.add(r) ;

			refuelings.put(fleet.acreg, r) ;
		}
	}

	private void modifyRefueling (FlightScenario scen) throws RemoteException, MXException {
		Port port = getRandomRefuelingPort(scen) ;
		if (port != null) {
			Fleet fleet = getRandomFleet(scen) ;
			putRefueling (fleet, port) ;
		}
	}

	void resetReservationTiming () throws RemoteException {
		for (int i = 0; i < reservations.size(); i++) {
			ReservationRemote resv = reservations.get(i) ;
			resv.resetTiming() ;
		}
	}

	public FlightScenario calcScenario (FlightScenario reusable, boolean forceAddRefuelingPoint) throws RemoteException, MXException {
		FlightScenario scenario = null ;

		if (scenref != null && scenref.getConfig() != null) {
			scenario = FlightScenario.reuseScenario(scenref, reusable) ;
			ArrayList<ReservationRemote> resvVal = new ArrayList<ReservationRemote>() ;

			if (forceAddRefuelingPoint)
				modifyRefueling(scenario) ;

			resetReservationTiming() ;
			resvVal.addAll(reservations) ;

			int divergenCount = 0 ;
			int previdx = -1 ;
			boolean lastSuccess = false ;
			ReservationRemote lastResv = null ;
			Fleet lastFleet = null ;
			
			int totCap = 0;
			int maxCap = 0;
			
			
			
			HashMap<String, Fleet> testAvail = scenario.getAvailableFleet() ;
			Iterator<String> its = testAvail.keySet().iterator() ;
			for (int i=0; its.hasNext(); i++) {
				Fleet flt = testAvail.get(its.next()) ;

				 maxCap += flt.getCapacity();				
			}
			
			System.err.println("size: " + resvVal.size());
			System.err.println("total capacity: " + maxCap);
			
			while (!resvVal.isEmpty() && divergenCount < MAX_ITTERATION) {
				if (svcHeli.isStopping())
					break ;

				Fleet fleet = null ;
				ReservationRemote resv = null ;
				int ridx = -1 ;
				if (lastSuccess)
					ridx = getEquivalentReservation (resvVal, lastResv) ;

				if (ridx >= 0) {
					resv = resvVal.get(ridx) ;
					fleet = lastFleet ;
				}
				else {
					ridx = getRandomReservation(resvVal, previdx, lastSuccess) ;
					resv = resvVal.get(ridx) ;

					String acreg = resv.getServedBy() ;
					if (acreg != null)
						fleet = scenario.getFleet(acreg) ;

					if (fleet == null) {
						fleet = getRandomFleet(scenario, resv) ;
					}
				}

				previdx = ridx ;
				if (fleet == null)
					break ;

				Route route = fleet.getRoute() ;
				Route testroute = route.copy() ;

				System.out.println("===== Inserting to " + fleet.acreg + ", " + route.toString() + ", validity : " + testroute.isValid()  ) ;
				System.out.println("===== Resv to be inserted : " + resv.getInfo());

				// first trial
				testroute.addPortAtEnd(resv) ;

				System.out.println("===== After insertion to ("+ scenario.getScenarioId()  +")" + fleet.acreg + ", " + testroute.toString() + ", validity : " + testroute.isValid() ) ;

				if (! testroute.isValid()) {
					System.out.println("===== Insert : not-valid, number of reservation = " + resvVal.size()) ;
					if (! testroute.hasRefuelingActivity()) {
						boolean hasrefueled = false ;

						// check if the invalidation is caused by weight on board

						ReservationRemote refuelingpoint = refuelings.get(fleet.acreg) ;
						if(refuelingpoint != null){
//							svcHeli.putLog("Try using refueling point " + refuelingpoint.getDest() + ", for ACREG " + fleet.getACREG()) ;

							Route refueled = route.copy() ;

							boolean refuelingcaps [] = refueled.getRefuelingCapability() ;
							String vports [] = refueled.toPortNameArray() ;
							for (int i=1; i<vports.length-1; i++) {
								if (refuelingcaps[i]) {
									refueled.setRefueling(vports[i], true) ;
									hasrefueled = true ;
								}
							}

							if (!hasrefueled) {
								if (testroute.isChanged()) {
									refueled.addPortAtEnd(refuelingpoint) ;

									System.out.println("===== After add refuel to " + fleet.acreg + ", " + refueled.toString() + ", validity : " + refueled.isValid() ) ;
									refueled.addPortAtEnd(resv) ;
									System.out.println("===== After add target port to " + fleet.acreg + ", " + refueled.toString() + ", validity : " + refueled.isValid() ) ;
								}
								else {
									String refuelport = refuelingpoint.getDest() ;

									for (int fi = vports.length-2; fi>=1; fi--) {
										String vports_alt [] = new String [vports.length+1] ;
										for (int j=0; j<vports.length+1; j++) {
											if (j<fi)
												vports_alt[j] = vports[j] ;
											else if (j==fi)
												vports_alt[j] = refuelport ;
											else
												vports_alt[j] = vports[j-1] ;

										}

										Route r1 = refueled.rearrange(vports_alt) ;
										System.out.println("===== (*" + fi + "*) After add refuel to " + fleet.acreg + ", " + r1.toString() + ", validity : " + r1.isValid() ) ;
										r1.addPortAtEnd(resv) ;
										System.out.println("===== (*" + fi + "*) After add target port to " + fleet.acreg + ", " + r1.toString() + ", validity : " + r1.isValid() ) ;

										if (r1.isValid()) {
											refueled = r1 ;
											break ;
										} else {
											refueled = testroute;
										}
									}
								}
							}

							if (refueled != null) {
								if(refueled.isValid()){
									testroute = null ;
									testroute = refueled ;
								}
							}
						}
					} //else {
						//call back the vehicle
//						System.err.println("%%%%%%%%%%%%%% passed");
//						Fleet fleet2 = scenario.getFleetArray()[0];
//						Route route99 = fleet2.getRoute();
//						route99.addPortAtEnd(resvVal.get(0));
					//}
					
				}

				if (testroute.isValid()) {
					System.out.println("===== Insert : valid, number of reservation = " + resvVal.size()) ;

					resvVal.remove(ridx) ;
					divergenCount = 0 ;
					fleet.replaceRoute (testroute) ;
					route = null ;
					lastSuccess = true ;
					lastResv = resv ;
					lastFleet = fleet ;
				}
				else {
					// check eligibility of a fleet to be refueled

					route.mergeInvalids (testroute) ;

					divergenCount ++ ;

					System.out.println("===== Insert : invalid, divergen count = " + divergenCount) ;
					testroute = null ;
					lastSuccess = false ;
					lastResv = null ;
					lastFleet = null ;

					reusable = scenario ;
				}
			}

			if (! resvVal.isEmpty()) {
				System.out.println("===== Number of reservation left = " + resvVal.size()) ;
								
				scenario = null ;
			}
		}

		if (scenario != null) {
			resetReservationTiming() ;
			scenario.adjustReservationTiming() ;

			printReservations("Found valid scenario", scenario) ;
		}

		return scenario ;
	}

	private void printReservations (String title, FlightScenario scenario) throws RemoteException, MXException {
		System.out.println("===== ("+title+")Reservation print For = " + scenario.getScenarioId()) ;
		for (int i = 0; i < reservations.size(); i++) {
			ReservationRemote resv = reservations.get(i);
			System.err.println(resv +"---"+ scenario.getScenarioId()+" O:"+resv.getOrg()+" D:"+resv.getDest()
					+" "+ resv.getString("displayname")+ ", etd "+ resv.getEtd()+ ", eta "+ resv.getEta());
		}
		System.out.println("===== End For = " + scenario.getScenarioId()) ;
	}

	private int getEquivalentReservation (ArrayList<ReservationRemote> resvs, ReservationRemote lastResv) throws RemoteException {
		int size = resvs.size() ;
		for (int i=0; i<resvs.size(); i++) {
			String lastOrg = lastResv.getOrg() ;
			String lastDest = lastResv.getDest() ;
			if (lastOrg.equalsIgnoreCase(resvs.get(i).getOrg()) &&
					lastDest.equalsIgnoreCase(resvs.get(i).getDest()))
				return i ;
		}

		return -1 ;
	}


	private int getRandomReservation (ArrayList<ReservationRemote> resvs, int previdx, boolean lastSuccess) {
		int size = resvs.size() ;
		if ((previdx < 0) || (previdx >= size) || (lastSuccess==false)) {
			return getRandomNumber(size) ;
		}

		return previdx ;
	}

	private int getRandomNumber (int max) {
		Double rNum = Math.floor(Math.random() * max) ;
		return rNum.intValue() ;
	}

	private Port getRandomRefuelingPort (FlightScenario scen) throws RemoteException, MXException {
		Port[] ports = scen.getRefuelingPortArray() ;
		if (ports != null) {
			int idx = getRandomNumber(ports.length) ;
			return ports[idx] ;
		}
		return null ;
	}

	private Fleet getRandomFleet (FlightScenario scen) throws RemoteException, MXException {
		Fleet[] fleets = scen.getFleetArray() ;
		if (fleets != null) {
			int idx = getRandomNumber(fleets.length) ;
			return fleets[idx] ;
		}
		return null ;
	}

	private Fleet getRandomFleet (FlightScenario scen, ReservationRemote resv) throws RemoteException, MXException {
		Fleet fleet = null ;
		int cnt = 0 ;
		while (fleet == null) {
			fleet = dogetRandomFleet (scen, resv) ;
			Route route = fleet.getRoute() ;
			if (route.isInInvalidList(resv)) {
				fleet = null ;

				if (cnt >= 10) {
					if (isAllInvalid(scen, resv))
						return null ;
				}
			}
			else {
				if (! route.preValidateResv(resv))
					fleet = null ;

					if (cnt >= 10) {
						if (isAllInvalid(scen, resv))
							return null ;
					}
			}
			cnt ++ ;
		}
		return fleet ;
	}

	private boolean isAllInvalid (FlightScenario scen, ReservationRemote resv) throws RemoteException, MXException {
		Fleet[] fleets = scen.getFleetArray() ;
		for (int i=0; i<fleets.length; i++) {
			Fleet lf = fleets[i] ;
			Route rt = lf.getRoute() ;
			if (! rt.isInInvalidList(resv)) {
				if (rt.preValidateResv(resv))
					return false ;
			}
		}
		return true ;
	}

	private Fleet dogetRandomFleet (FlightScenario scen, ReservationRemote resv) throws RemoteException, MXException {
		Fleet[] fleets = scen.getFleetArray() ;
		Integer[]  sumweights = new Integer[fleets.length] ;

		Integer ttlweight = 0 ;
		for (int i=0; i<sumweights.length; i++) {
			Fleet fl = fleets[i] ;
			Route rt = fl.getRoute() ;
			int idxs[] = rt.findPortPair(resv) ;
			Integer w = 100 ;
			sumweights[i] = 0 ;

			if (idxs[0]>=0)
				w += (100 + getRandomNumber(200)) ;
			if (idxs[1]>=0)
				w += (100 + getRandomNumber(400)) ;

			ttlweight += w ;
			if (i>0)
				sumweights[i] += sumweights[i-1] ;
			sumweights[i] += w ;
		}

		int idx = 0 ;
		int widx = getRandomNumber (ttlweight) ;
		for (; idx<sumweights.length; idx++) {
			if (widx < sumweights[idx])
				break ;
		}

		return fleets[idx] ;
	}

}
