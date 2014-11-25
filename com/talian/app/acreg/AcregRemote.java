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
package com.talian.app.acreg;

import java.rmi.RemoteException;

import psdi.mbo.MboRemote;
import psdi.util.MXException;

/**
 * @author Seno
 *
 */
public interface AcregRemote extends MboRemote {
	public void backToDefaultSetting () throws RemoteException, MXException ;
}
