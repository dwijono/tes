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

import psdi.mbo.Mbo ;
import psdi.mbo.MboSet ;

/**
 * @author Seno
 *
 */
public class Distance extends Mbo implements DistanceRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public Distance(MboSet ms) throws RemoteException {
		super(ms) ;
		// TODO Auto-generated constructor stub
	}

}
