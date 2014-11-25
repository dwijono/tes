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

import psdi.mbo.MboSetRemote ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public interface ReservationSetRemote extends MboSetRemote {
	public void rebuildSummaryTable () throws MXException, RemoteException ;
}
