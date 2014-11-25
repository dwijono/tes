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
import java.util.HashSet ;
import java.util.Hashtable ;
import java.util.Set ;

import psdi.mbo.MboRemote ;
import psdi.mbo.MboSetRemote ;
import psdi.mbo.SqlFormat ;
import psdi.server.MXServer ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public class DemandSet {
	static DemandSet demandset = null ;
	public Hashtable<String, Demand> list ;
	public Hashtable<String, Integer[]> portlist ;
	public Hashtable<String, Set<Demand>> outgoing ;
	public Hashtable<String, Set<Demand>> incoming ;
	
	private DemandSet() {
		list = new Hashtable<String, Demand> ();
		portlist = new Hashtable<String, Integer[]> () ;
		outgoing = new Hashtable<String, Set<Demand>> () ;
		incoming = new Hashtable<String, Set<Demand>> () ;
	}
	
	public static DemandSet getInstance () {
		if (demandset == null) 
			demandset = new DemandSet() ;
		return demandset ;
	}
	
	public void load (MboSetRemote set, Date dt, String flightsession) throws RemoteException, MXException {
		list.clear() ;
		portlist.clear() ;
		
		MboRemote mbo = set.moveFirst() ;
		while (mbo != null) {
			Demand fl = Demand.getInstance(mbo) ;
			list.put (fl.getKey(), fl) ;
			
			Integer[] dtlOrg = (Integer[])portlist.get(fl.org) ;
			if (dtlOrg == null) {
				dtlOrg = new Integer[] { fl.npax, 0 } ;
				portlist.put(fl.org, dtlOrg) ;
			}
			else {
				dtlOrg[0] += fl.npax ;
			}
			
			Set<Demand> dtlOutgoing = (Set<Demand>)outgoing.get(fl.org) ;
			if (dtlOutgoing == null) {
				dtlOutgoing = new HashSet<Demand>() ;
				outgoing.put(fl.org, dtlOutgoing) ;
			}			
			dtlOutgoing.add(fl) ; 
			
			Set<Demand> dtlIncoming = (Set<Demand>)incoming.get(fl.dest) ;
			if (dtlIncoming == null) {
				dtlIncoming = new HashSet<Demand>() ;
				incoming.put(fl.dest, dtlOutgoing) ;
			}			
			dtlIncoming.add(fl) ; 

			Integer[] dtlDest = (Integer[])portlist.get(fl.dest) ;
			if (dtlDest == null) {
				dtlDest = new Integer[] { 0, fl.npax } ;
				portlist.put(fl.dest, dtlDest) ;
			}
			else {
				dtlDest[1] += fl.npax ;
			}
			
			mbo = set.moveNext() ;
		}		
	}
	
	public static void load (Date dt, String flightsession) throws RemoteException, MXException {
		MXServer server = MXServer.getMXServer() ;
		MboSetRemote reservation = server.getMboSet("RESERVSUM", server.getSystemUserInfo()) ;
		SqlFormat sqf = new SqlFormat("tripdate=:0 and flightsession=:1") ;
		sqf.setDate(0, dt) ;
		sqf.setObject(1, "RESERVSUM", "FLIGHTSESSION", flightsession) ;
		reservation.setWhere(sqf.format()) ;
		
		getInstance().load (reservation, dt, flightsession) ;
	}
	
	static public DemandSet getCopy () {
		DemandSet original = getInstance () ;
		DemandSet copy = new DemandSet () ;
		
		copy.list.putAll(original.list) ;
		copy.portlist.putAll(original.portlist) ;
		
		return copy ;
	}
	
	public Demand getDemand(String org, String dest ) {
		return list.get(org+"-"+dest) ;
	}
	
}
