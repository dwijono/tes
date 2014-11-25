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

import com.talian.app.actype.Actype;

import psdi.mbo.Mbo;
import psdi.mbo.MboSet;
import psdi.util.MXException;

/**
 * @author Elga
 *
 */
public class Acreg extends Mbo implements AcregRemote {

	public Acreg(MboSet ms) throws RemoteException {
		super(ms);
	}

	@Override
	public void backToDefaultSetting() throws RemoteException, MXException {
		this.setValue("startpos", this.getString("defstartpos"), NOACCESSCHECK);
		this.setValue("endpos", this.getString("defendpos"), NOACCESSCHECK);
		this.setValue("status", this.getString("status"), NOACCESSCHECK);
		this.setValue("paxcapacity", this.getInt("defpaxcapacity"), NOACCESSCHECK);
		this.setValue("refuelport", this.getString("defrefuelport"), NOACCESSCHECK);
	}
}
