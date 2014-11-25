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
public class DistanceSet {
	static Hashtable<String, Distance> list = new Hashtable<String, Distance> ();
	public static void load (MboSetRemote set) throws RemoteException, MXException {
		MboRemote mbo = set.moveFirst() ;
		while (mbo != null) {
			Distance obj = Distance.getInstance(mbo) ;
			list.put (obj.getKey(), obj) ;
			mbo = set.moveNext() ;
		}		
	}
	
	public static void load () throws RemoteException, MXException {
		if (list.isEmpty()) {
			MXServer server = MXServer.getMXServer() ;
			MboSetRemote set = server.getMboSet("DISTANCE", server.getSystemUserInfo()) ;
			load (set) ;
		}
	}

	
	public static Distance getDistance(String org, String dest ) {
		return list.get(org+"-"+dest) ;
	}
}
