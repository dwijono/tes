/*
 *
 *
 *
 * (C) COPYRIGHT Talian Limited, 2010
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets.
 *
 */
package com.talian.app.scenass;

import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboSet;

/**
 * @author Elga
 *
 */
public class Scenass extends Mbo implements ScenassRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public Scenass(MboSet ms) throws RemoteException {
		super(ms);
		// TODO Auto-generated constructor stub
	}

}
