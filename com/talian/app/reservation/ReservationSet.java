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
package com.talian.app.reservation;

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
public class ReservationSet extends MboSet implements ReservationSetRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public ReservationSet(MboServerInterface ms) throws RemoteException {
		super(ms) ;
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see psdi.bo.MboSet#getMboInstance(psdi.bo.MboSet)
	 */
	@Override
	protected Mbo getMboInstance(MboSet ms) throws MXException,
			RemoteException {
		return new Reservation(ms) ;
	}
	
	public void rebuildSummaryTable () throws MXException, RemoteException {
		
		MboServerInterface server = getMboServer() ; 		
		MboSetRemote summaries = server.getMboSet("resvsum", this.getUserInfo()) ;
		summaries.deleteAll() ;
		
		summaries.save() ;		
	}
	
	/**
	 * Split reservation to chunks according to available leg
	 * @throws MXException
	 * @throws RemoteException
	 */
	public void splitIntoSubReservation() throws MXException, RemoteException {
		
	}

}
