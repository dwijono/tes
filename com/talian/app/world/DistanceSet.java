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
import java.util.ArrayList ;

import psdi.mbo.Mbo ;
import psdi.mbo.MboRemote ;
import psdi.mbo.MboServerInterface ;
import psdi.mbo.MboSet ;
import psdi.mbo.MboSetRemote ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public class DistanceSet extends MboSet implements DistanceSetRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public DistanceSet(MboServerInterface ms) throws RemoteException {
		super(ms) ;
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see psdi.bo.MboSet#getMboInstance(psdi.bo.MboSet)
	 */
	@Override
	protected Mbo getMboInstance(MboSet ms) throws MXException,
			RemoteException {
		return new Distance(ms) ;
	}

	public void rebuildDistanceTable () throws MXException, RemoteException {
		this.deleteAll() ;

		MboServerInterface server = getMboServer() ;
		MboSetRemote ports = server.getMboSet("heliport", this.getUserInfo()) ;
		int cnt = ports.count() ;

		for (int i=0; i<cnt; i++)
			for (int j=0; j<cnt; j++) {
				if (i != j) {
					MboRemote port1 = ports.getMbo(i) ;
					MboRemote port2 = ports.getMbo(j) ;

					WorldPoint pos1 = WorldPoint.getPoint(port1) ;
					WorldPoint pos2 = WorldPoint.getPoint(port2) ;

					Double nmi = WorldPoint.distanceBetween(pos1, pos2) ;

					System.out.println("=== Calculating distance from " + port1.getString("heliport") + " to " + port2.getString("heliport")) ;
					System.out.println("=== result = " + nmi ) ;

					MboRemote distance = this.add(NOACCESSCHECK|NOVALIDATION_AND_NOACTION) ;
					distance.setValue("fromport", port1.getString("heliport"), NOACCESSCHECK|NOVALIDATION_AND_NOACTION) ;
					distance.setValue("toport", port2.getString("heliport"), NOACCESSCHECK|NOVALIDATION_AND_NOACTION) ;
					distance.setValue("nmi", nmi, NOACCESSCHECK|NOVALIDATION_AND_NOACTION) ;
				}
			}

		save() ;

	}

}
