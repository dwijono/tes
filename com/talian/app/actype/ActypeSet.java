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
package com.talian.app.actype;

import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboServerInterface;
import psdi.mbo.MboSet;
import psdi.util.MXException;


/**
 * @author Seno
 *
 */
public class ActypeSet extends MboSet implements ActypeSetRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public ActypeSet(MboServerInterface ms) throws RemoteException {
		super(ms) ;
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see psdi.bo.MboSet#getMboInstance(psdi.bo.MboSet)
	 */
	@Override
	protected Mbo getMboInstance(MboSet ms) throws MXException,
			RemoteException {
		return new Actype(ms) ;
	}

}
