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
package com.talian.app.world;

import java.rmi.RemoteException ;

import psdi.mbo.MboRemote ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public class WorldPoint {
	Double  latD ;
	Double  longD ;

	static Double SIGN_NORTH=1.0 ;
	static Double SIGN_SOUTH=-1.0 ;
	static Double DEG2MIN=60.0 ;
	static Double DEG2SEC=3600.0 ;

	Double getLatitudeDeg () {
		return latD ;
	}

	Double getLongitudeDeg () {
		return longD ;
	}

	Double getLatitudeRad () {
		return Math.toRadians(getLatitudeDeg()) ;
	}

	Double getLongitudeRad () {
		return Math.toRadians(getLongitudeDeg()) ;
	}

	public static Double distanceBetween (WorldPoint pt0, WorldPoint pt1) {
		Double lat0 = pt0.getLatitudeRad() ;
		Double lon0 = pt0.getLongitudeRad() ;
		Double lat1 = pt1.getLatitudeRad() ;
		Double lon1 = pt1.getLongitudeRad() ;

		Double a = Math.pow(Math.sin((lat1-lat0)/2), 2)
        		   + Math.cos(lat0) * Math.cos(lat1) * Math.pow(Math.sin((lon1-lon0)/2), 2);
		Double angle2 = 2 * Math.asin(Math.min(1, Math.sqrt(a)));
	           angle2 = Math.toDegrees(angle2);   // convert back to degrees

		return angle2 * 60.0 ;
	}

	public void save (MboRemote mbo)  throws MXException, RemoteException {
		mbo.setValue("latDegree", latD);
		mbo.setValue("lotDegree", longD);
//		mbo.getThisMboSet().save();
	}

	static public WorldPoint getPoint (MboRemote mbo)  throws MXException, RemoteException {
		WorldPoint point = new WorldPoint () ;

		point.latD = mbo.getDouble("latdegree") ;
		point.longD = mbo.getDouble("lotdegree") ;

		return point ;
	}

}
