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
package com.talian.beans.acreg;

import java.rmi.RemoteException;

import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;
import psdi.webclient.system.beans.AppBean;

/**
 * @author Elga
 *
 */
public class AcregAppBean extends AppBean {

	/**
	 *
	 */
	public AcregAppBean() {
	}

	public int GETSTD() throws MXException, RemoteException {
		MboSetRemote mboset1 = getMbo().getMboSet("actype#1", "actype", "actype= :actype");

		MboRemote currentAcreg = getMbo();
		MboRemote currentActype = mboset1.moveFirst();

		currentAcreg.setValue("eew", currentActype.getDouble("eew"));
		currentAcreg.setValue("mtow", currentActype.getDouble("mtow"));
		currentAcreg.setValue("tankcapacity", currentActype.getDouble("tankcapacity"));
		currentAcreg.setValue("maxspeed", currentActype.getDouble("maxspeed"));
		currentAcreg.setValue("paxcapacity", currentActype.getDouble("paxcapacity"));
		currentAcreg.setValue("resfuel", currentActype.getDouble("resfuel"));
		currentAcreg.setValue("taxifuel", currentActype.getDouble("taxifuel"));
		currentAcreg.setValue("cruisefuel", currentActype.getDouble("cruisefuel"));
		currentAcreg.setValue("alternatefuel", currentActype.getDouble("alternatefuel"));
		currentAcreg.setValue("refuelingtime", currentActype.getDouble("refuelingtime"));
		currentAcreg.setValue("groundtime", currentActype.getDouble("groundtime"));
		currentAcreg.setValue("mtow", currentActype.getDouble("mtow"));

		currentAcreg.setValue("taxitime", 10.00);
		currentAcreg.setValue("takeofffuel", 30.00);

		currentAcreg.setValue("startpos", "SPG");
		currentAcreg.setValue("endpos", "SPG");

		SAVE();
		sessionContext.queueRefreshEvent();
		return EVENT_HANDLED;
	}

	@Override
	public int SAVE() throws MXException, RemoteException {
		MboRemote currentAcreg = getMbo();

		String pathcolor = currentAcreg.getString("pathcolor");

		String template =	"<tr><th class=\""+pathcolor.toLowerCase()+"desc"+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th><th class=\""+pathcolor.toLowerCase()+"\">&nbsp</th></tr> "+
							"<tr><td class=\""+pathcolor.toLowerCase()+"desc"+"\">Take Off Weight</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td></tr> "+
							"<tr><td class=\""+pathcolor.toLowerCase()+"desc"+"\">Min Fuel On Board</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td></tr> "+
							"<tr><td class=\""+pathcolor.toLowerCase()+"desc"+"\">Current Payload</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td></tr> "+
							"<tr><td class=\""+pathcolor.toLowerCase()+"desc"+"\">Allowance Payload</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td></tr> "+
							"<tr><td class=\""+pathcolor.toLowerCase()+"desc"+"\">Pax On - Pax Off</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td><td class=\""+pathcolor.toLowerCase()+"\">&nbsp</td></tr> ";

		currentAcreg.setValue("presentation", template);

		return super.SAVE();
	}
}
