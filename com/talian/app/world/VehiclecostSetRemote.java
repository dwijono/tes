/**
 * 
 */
package com.talian.app.world;

import java.rmi.RemoteException;

import psdi.mbo.MboSetRemote;
import psdi.util.MXException;

/**
 * @author THINKPAD
 *
 */
public interface VehiclecostSetRemote extends MboSetRemote {
	public void calculateVehicleCostTable () throws MXException, RemoteException ;
}
