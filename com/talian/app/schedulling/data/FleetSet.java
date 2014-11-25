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
import java.util.Hashtable ;

import psdi.mbo.MboRemote ;
import psdi.mbo.MboSetRemote ;
import psdi.server.MXServer ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public class FleetSet {
	public static Hashtable<String, Fleet> list = new Hashtable<String, Fleet> ();
	public static void load (MboSetRemote set, Date dt, String flightsession) throws RemoteException, MXException {
		MboRemote mbo = set.moveFirst() ;
		while (mbo != null) {
			Fleet fl = Fleet.getInstance(mbo) ;
			fl.adjustPosition(dt, flightsession) ;
			list.put (fl.getKey(), fl) ;
			mbo = set.moveNext() ;
		}		
	}
	
	public static void load (Date dt, String flightsession) throws RemoteException, MXException {
		MXServer server = MXServer.getMXServer() ;
		MboSetRemote fleet = server.getMboSet("FLEET", server.getSystemUserInfo()) ;
		load (fleet, dt, flightsession) ;
	}
	
	public Fleet get(String key) {
		return list.get(key) ;
	}
}
