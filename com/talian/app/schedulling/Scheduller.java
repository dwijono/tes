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
package com.talian.app.schedulling;

import java.rmi.RemoteException ;
import java.text.SimpleDateFormat ;
import java.util.Date ;
import java.util.Enumeration ;
import java.util.Hashtable ;

import org.apache.commons.lang.math.RandomUtils ;

import psdi.util.MXException ;
import psdi.util.MXApplicationException ;

import com.talian.app.heli.HeliService ;
import com.talian.app.schedulling.data.Demand ;
import com.talian.app.schedulling.data.DemandSet ;
import com.talian.app.schedulling.data.Distance ;
import com.talian.app.schedulling.data.DistanceSet ;
import com.talian.app.schedulling.data.Fleet ;
import com.talian.app.schedulling.data.FleetSet ;

/**
 * @author Seno
 *
 */
public class Scheduller implements Runnable {
	Date currentdate  ;
	String flightsession ;
	HeliService svc ;
	DemandSet demandset ;
	
	static Hashtable<String, Scheduller> schedullers = new Hashtable<String, Scheduller> () ;
	static int MAXPORTS = 32 ;
	static double SPEEDRATIO = 0.8 ;
	
	public Scheduller (HeliService asvc, Date tripdate, String sid) {
		currentdate = tripdate ;
		flightsession = sid ;
		svc = asvc ;
	}
	
	public static Scheduller startInstance (HeliService asvc, Date tripdate, String sid) throws RemoteException, MXException {
		if (isStarted (tripdate, sid)) 
			throw new MXApplicationException("schedulling", "hasRun", new String[] {tripdate.toString(), sid} ) ;
		Scheduller sched = new Scheduller (asvc, tripdate, sid) ;
		
		schedullers.put(sched.getKey(), sched) ;
		return sched ;
	}
	
	public static boolean isStarted (Date tripdate, String sid) {
		return (schedullers.containsKey(getKey(tripdate, sid))) ;
	}
	
	
	public static String getKey(Date tripdate, String sid) {
		return tripdate.getTime() + "-" + sid ;
	}
	
	public String getKey() {
		return getKey(currentdate, flightsession) ;
	}
	
	public void init () throws RemoteException, MXException {
		SimpleDateFormat dtf = new SimpleDateFormat() ;
		
		svc.putLog("Loading data... for " + dtf.format(currentdate) + ", flight session" + flightsession) ;
		FleetSet.load(currentdate, flightsession) ;

		//FIXME: it should be treated in thread context
		DistanceSet.load() ;
		DemandSet.load(currentdate, flightsession) ;
				
		Thread thd = new Thread(this) ;
		thd.setName("Scheduller " + thd.getId()) ;
		svc.putLog("Scheduller initialized. Launching thread : " + thd.getName()) ;
		thd.start() ;
	}
	
	public void run ()  {
		try {
			svc.putLog("Process started in " + Thread.currentThread().getName()) ;
			svc.putLog("Loading demand...") ;
			
			demandset = DemandSet.getCopy() ;
			
			String [] ports = new String[demandset.portlist.keySet().size()] ; 
			ports = demandset.portlist.keySet().toArray(ports) ;

			String portlabels = "" ;
			for (int i=0; i<ports.length; i++) {
				if (i!=0)
					portlabels = portlabels + "," ;
				portlabels = portlabels + ports[i] ;
			}

			svc.putLog("Recognizing flight demand for " + ports.length + " locations. [" + portlabels + "]" ) ;

			svc.putLog("Fleet serving this session : " ) ;
			
			Enumeration<String> fl = FleetSet.list.keys() ;
			while (fl.hasMoreElements()) {
				String fleetcode = fl.nextElement() ;			
				Fleet fleetdata = FleetSet.list.get(fleetcode) ;
				svc.putLog("A/C Reg : " + fleetdata.fleet + " from base : " + fleetdata.startPosition ) ;
			}
					
			svc.putLog("Searching Initial Solutions") ;
			
			SolutionSpace solspace = new SolutionSpace();
			for (int i=0; i<solspace.nfleets; i++) {
				solspace.step(0) ;
			}
			
			
			svc.putLog("Scheduller finished.") ;		
		}
		catch (Throwable t) {
			t.printStackTrace() ;
		}
		finally {
			try {
				stop () ;
			}
			catch (Throwable te) {
				te.printStackTrace();
			}
		}		
	}
	
	class Assignment {
		String [] ports ;
		Double [] tripfuels ;
		Double [] durations ;
		Double [] distances ;
		Double [] weights ;
		Double [] maxpayloads ;
		Double totaldistances ;
		Integer [] paxes ;
		int nports ;	
		Fleet fleet ;
		Double lastaddeddistance ;
		
		Assignment(Fleet fl) {
			fleet = fl ;
			nports = 0 ;
			ports = new String[MAXPORTS] ;
			tripfuels = new Double[MAXPORTS] ;
			durations = new Double[MAXPORTS] ;
			distances = new Double[MAXPORTS] ;
			weights = new Double[MAXPORTS] ;
			maxpayloads = new Double[MAXPORTS] ;
			totaldistances = 0.0 ;
			paxes = new Integer[MAXPORTS] ;
			lastaddeddistance = 0.0 ;
		}
		
		int putport (String port, Demand toport, Demand fromport) {
			ports[nports] = port ;
			paxes[nports] = 0 ;
			weights[nports] = 0.0 ;
			
			if (nports>1) {
				Distance dist0 = DistanceSet.getDistance(ports[nports-1], ports[nports]) ;
				distances [nports] = dist0.nmi ;
				durations [nports] = dist0.nmi / (SPEEDRATIO * fleet.speed) ;
				Double tripfuel = fleet.tripFuelRatio(dist0.nmi, SPEEDRATIO) ; 
				tripfuels [nports] = tripfuel ;
				totaldistances += dist0.nmi ;
				totaldistances -= lastaddeddistance ;
				
				Distance dist1 = DistanceSet.getDistance(ports[nports], ports[0]) ;
				Double returnfuel = fleet.tripFuelRatio(dist0.nmi, SPEEDRATIO) ;
				totaldistances += dist1.nmi ;
				lastaddeddistance  = dist1.nmi ;
				
				Double maxpayload = fleet.avlpayload ;
				for (int i=nports-1; i>=0; i--) {
					if (i==nports-1)
						maxpayloads[i] = maxpayload - returnfuel ;
					else {
						maxpayloads[i+1] -= tripfuels [i] ;
					}
				}

				for (int i=0; i<nports; i++) {
					paxes[i] += toport.getPax() ;
					weights[i] += toport.getWeight() ;
				}
				
			}
			fleet.endPosition = port ;
			nports += 1 ;

			return nports - 1 ;
		}
		
		boolean checkpayload () {
			return true ;
		}
	}
	
	class SolutionSpace {
		Assignment assignments[] ;
		int nfleets ;
		
		SolutionSpace() {
			Object fleetcodes[] = FleetSet.list.keySet().toArray() ; 
			nfleets = FleetSet.list.size() ;
			assignments = new Assignment[nfleets] ;
			for (int i=0; i<nfleets; i++) {
				Fleet fleet = FleetSet.list.get(fleetcodes[i]) ;
				assignments[i] = new Assignment(fleet) ;
				fleet.endPosition = null ;
			}
		}
				
		boolean step (int fleetidx) {
			boolean isvalid = true ;
			Assignment assignment = assignments[fleetidx] ;
			Fleet fleet = assignment.fleet ;
			Integer demands [] = demandset.portlist.get(fleet.startPosition) ;

			String [] ports = new String[demandset.portlist.keySet().size()] ; 
			boolean [] visited = new boolean[ports.length] ;
			
			ports = demandset.portlist.keySet().toArray(ports) ;
			fleet.init() ;
			
			
			if (demands != null && demands[0]>0) {		// demands[0] records departing passengers
				assignments[fleetidx].putport(fleet.startPosition, null, null) ;
				Double carryoverfuel = 0.0 ;
				Double lastpayload = 0.0 ;
				boolean allvisited_and_invalid = false ;
				for (int i=0; i<ports.length; i++) {
					visited[i] = false ;
				}

				while (fleet.avlpayload>0 && !allvisited_and_invalid) {
					int idx = selectPort (fleet, ports) ;
					if (visited[idx]) {
						allvisited_and_invalid = true ;
						for (int i=0; i<ports.length; i++) {
							allvisited_and_invalid = allvisited_and_invalid && visited[i] ;
						}
						continue ;
					}
					visited[idx] = true ;
					String selectedport = ports[idx] ;
					
					Distance dist0 = DistanceSet.getDistance(fleet.endPosition, selectedport) ;
					Distance dist1 = DistanceSet.getDistance(selectedport, fleet.startPosition) ; // goto start position
					Double fuel0= fleet.tripFuelRatio(dist0.nmi, SPEEDRATIO) ;
					Double fuel1= fleet.tripFuelRatio(dist1.nmi, SPEEDRATIO) ;
					Double fuelneed = fuel0 + fuel1 ;
					if (fleet.avlpayload + carryoverfuel > fuelneed) {
						fleet.avlpayload += carryoverfuel ;
						fleet.avlpayload -= fuelneed ;
						carryoverfuel = fuel1 ;						// add back last return trip fuel
						
						// pesawat menurunkan penumpang sebanyak y, maka avlpayload bertambah sebanyak weight
						// 

						Demand demand0 = demandset.getDemand(fleet.endPosition, selectedport) ;
						Demand demand1 = demandset.getDemand(selectedport, fleet.startPosition) ;
						Double maxpayload = fleet.avlpayload ;
						Demand assigned0 = demand0.splitDemand(fleet,maxpayload) ;
						Demand assigned1 = demand1.splitDemand(fleet,maxpayload) ;
						
						// reverse back last accounted payload
						
						if (assigned0 != null && assigned1 != null) {
							// check the payload 
							// 
							
							lastpayload = demand0.getWeight() + demand1.getWeight() ;
							/*if (payload_at_origin + lastpayload <= maxpayload) {
								payload_at_origin += lastpayload ;
								assignment.putport(selectedport) ;
							}
							else {
								fleet.avlpayload += fuelneed ;
								fleet.avlpayload -= carryoverfuel ;		// reverse back
							}*/
						}
						else {
							fleet.avlpayload += fuelneed ;
							fleet.avlpayload -= carryoverfuel ;		// reverse back							
						}
						
					}
				}
			}
			
			return isvalid ;
		}
	}
	
	public void stop () throws RemoteException, MXException {
		svc.putLog("DONE") ;
		schedullers.remove(getKey()) ;
	}
	
	private int selectPort (Fleet fleet, String []ports) {
		int idx = -1 ;
		boolean found = false ;
		while (!found) {
			idx = RandomUtils.nextInt(ports.length) ;
			if (!ports[idx].equalsIgnoreCase(fleet.endPosition))
				return idx ;
		}
		
		return idx ;
	}
}
