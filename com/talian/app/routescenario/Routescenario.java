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
import psdi.mbo.MboSet;
import psdi.util.MXException;

/**
 * @author Elga
 *
 */
public class Routescenario extends Mbo implements RoutescenarioRemote {

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public Routescenario(MboSet ms) throws RemoteException {
		super(ms);
	}

	@Override
	public void add() throws MXException, RemoteException {
		super.add();
		
		System.err.println("add scenario!!!!!!!!!!!!!!!!");
	}
}
