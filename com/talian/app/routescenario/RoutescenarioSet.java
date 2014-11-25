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
package com.talian.app.routescenario;

import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboServerInterface;
import psdi.mbo.MboSet;
import psdi.mbo.custapp.CustomMboSet;
import psdi.util.MXException;

/**
 * @author Elga
 *
 */
public class RoutescenarioSet extends CustomMboSet implements RoutescenarioSetRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public RoutescenarioSet(MboServerInterface ms) throws RemoteException {
		super(ms);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Mbo getMboInstance(MboSet arg0) throws MXException,
			RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
