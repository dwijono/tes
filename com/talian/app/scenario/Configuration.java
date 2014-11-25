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
package com.talian.app.scenario;

import java.rmi.RemoteException;

import psdi.mbo.MboRemote;
import psdi.util.MXException;
import psdi.util.MXSession;

/**
 * @author Seno
 *
 */
public class Configuration {
	private MXSession mxsession ;
	public String defaultStartPos ;
	public String defaultEndPos ;
	public Double defaultOAT ;
	public Double defaultCrewWeight ;
	public Double defaultHeadwind ;
	public Double defaultContigencyFactor ;
	public int    calculationBase ;

	public Configuration () {
		calculationBase = CALCBASE_DISTANCE ;
		defaultContigencyFactor = DEFAULT_CONTIGENCY_FACTOR ;
	}

	public Configuration copy() {
		Configuration cfg = new Configuration () ;
		cfg.mxsession = this.mxsession ;
		cfg.defaultStartPos = this.defaultStartPos ;
		cfg.defaultEndPos = this.defaultEndPos ;
		cfg.defaultOAT = this.defaultOAT ;
		cfg.defaultHeadwind = this.defaultHeadwind ;
		cfg.defaultCrewWeight = this.defaultCrewWeight ;
		cfg.defaultContigencyFactor = this.defaultContigencyFactor ;

		return cfg ;
	}

	public void setMXSession (MXSession mxsession) {
		this.mxsession = mxsession ;
	}

	public void setOAT (Double oat) {
		defaultOAT = oat ;
	}

	public void save (MboRemote mbo) throws RemoteException, MXException {
		mbo.setValue("startpos", defaultStartPos) ;
		mbo.setValue("endpos", defaultEndPos) ;
		mbo.setValue("oat", defaultOAT) ;
		mbo.setValue("crewweight", defaultCrewWeight) ;
		mbo.setValue("headwind", defaultHeadwind) ;
		mbo.setValue("contigency", defaultContigencyFactor) ;
//		mbo.getThisMboSet().save();
	}

	static public Configuration readfromMBO (MboRemote mbo) throws RemoteException, MXException {
		Configuration cfg = new Configuration() ;
		cfg.defaultStartPos = mbo.getString("startpos") ;
		cfg.defaultEndPos = mbo.getString("endpos") ;
		cfg.defaultOAT = mbo.getDouble("oat") ;
		cfg.defaultHeadwind = mbo.getDouble("headwind") ;
		cfg.defaultCrewWeight = mbo.getDouble("crewweight") ;
		cfg.defaultContigencyFactor = mbo.getDouble("contigency") ;

		return cfg ;
	}

	public static int CALCBASE_DISTANCE = 0 ;
	public static int CALCBASE_TIME = 1 ;

	public static double DEFAULT_CONTIGENCY_FACTOR = 1.06 ;
}
