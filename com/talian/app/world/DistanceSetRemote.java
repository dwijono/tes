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

import psdi.mbo.MboSetRemote ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public interface DistanceSetRemote extends MboSetRemote {
	public void rebuildDistanceTable () throws MXException, RemoteException ;
}
