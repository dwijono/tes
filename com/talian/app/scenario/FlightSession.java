/**
 *
 */
package com.talian.app.scenario;

import java.rmi.RemoteException;
import java.util.Date;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;
import psdi.util.MXSession;

/**
 * @author efs
 *
 */
public class FlightSession {
	private Date reservationdate;
	private String flightsession;
	private MXSession mxsession;
	private MboRemote mbo;

	public FlightSession(String flightsession, Date reservationdate){
		this.flightsession = flightsession;
		this.reservationdate = reservationdate;
	}

	public FlightSession(){
	}

	static public FlightSession newRecord (MXSession session, String flightsession, Date reserveDate) throws RemoteException, MXException {
		MboSetRemote mboset = session.getMboSet ("flightsession");

		FlightSession fs = new FlightSession();
		fs.setMXSession(session);

 		MboRemote mbo = mboset.add();
		mbo.setValue("flightsession", flightsession);
		mbo.setValue("reserveddate", reserveDate);
		mbo.setValue("etd", flightsession);
		fs.setMbo(mbo);

		return fs;
	}

	public void save() throws RemoteException , MXException {
		if (mbo == null)
			return;

		if (mbo.isNew()) {

			mbo.getThisMboSet().save();
		}
	}

	public void setMXSession (MXSession mxsession) {
		this.mxsession = mxsession;
	}

	private void setMbo (MboRemote mbo) {
		this.mbo = mbo;
	}
}
