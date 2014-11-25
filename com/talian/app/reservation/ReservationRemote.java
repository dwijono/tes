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

import java.rmi.RemoteException;

import psdi.mbo.MboRemote;
import psdi.util.MXException;

import com.talian.app.heli.Fleet;
import com.talian.app.route.Leg;

/**
 * @author Seno
 *
 */
public interface ReservationRemote extends MboRemote {
	static public int PRIORITY_VIP_01 = 1 ;
	static public int PRIORITY_VIP_02 = 2 ;
	static public int PRIORITY_NORMAL = 0 ;

	public void readfromMbo () throws RemoteException, MXException ;
	public Integer getId() throws RemoteException ;
	public Integer getPriority() throws RemoteException ;
	public boolean isBlockingVIP() throws RemoteException ;
	public boolean isDummyRefueling() throws RemoteException ;
	public void resetTiming() throws RemoteException ;
	public int getQty() throws RemoteException ;
	public Double getWeight() throws RemoteException ;
	public String getOrg() throws RemoteException ;
	public String getDest() throws RemoteException ;
	public String getServedBy() throws RemoteException ;
	public String getInfo () throws RemoteException ;

	public void setOrg(String org) throws RemoteException ;
	public void setDest(String dest) throws RemoteException ;
	public void setServedBy(String fleet) throws RemoteException ;
	public void setDummyRefueling(boolean dummy) throws RemoteException ;

	public void setFirstETD (Double etd) throws RemoteException ;
	public void setLastETA (Double eta) throws RemoteException ;
	public Double getEtd() throws RemoteException ;
	public Double getEta() throws RemoteException ;
	public String getEtdAsString() throws RemoteException, MXException ;
	public String getEtaAsString() throws RemoteException, MXException ;
}
