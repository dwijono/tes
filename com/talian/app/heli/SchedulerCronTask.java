/**
 *
 */
package com.talian.app.heli;

import java.rmi.RemoteException;

import psdi.app.system.CrontaskInstanceRemote;
import psdi.app.system.CrontaskParamInfo;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.FixedLoggerNames;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/**
 * @author Talian1
 *
 */
public class SchedulerCronTask extends SimpleCronTask {
	private MXLogger	logger ;
	/**
	 *
	 */
	public SchedulerCronTask() {
		logger = MXLoggerFactory.getLogger(FixedLoggerNames.LOGGERNAME_MAXIMO);
	}

	/* (non-Javadoc)
	 * @see psdi.server.SimpleCronTask#cronAction()
	 */
	@Override
	public void cronAction() {
		try {
			logger.info(getLogMessage("-- PING : url " + this.getParamAsString("server") )) ;

			HeliServiceRemote svc = (HeliServiceRemote)MXServer.getMXServer().lookup("HELI");
			if(svc.runSched()) {
				logger.info("successfully running scheduler");
			} else {
				logger.info("scheduler already running");
			}
		}
		catch (Exception e) {
			logger.error(getLogMessage("Error occured"), e) ;
		}

	}

	protected String getLogMessage(String msg) {
		msg = "task (" + this.getName() + ") instance (" + this.getInstanceName() + ") :" + msg ;

		return msg ;
	}

	public String getInstanceName() {
		String instanceName = null ;

		CrontaskInstanceRemote instance = this.getCrontaskInstance() ;

		try {
			instanceName = instance.getString("INSTANCENAME") ;
		}
		catch (Exception e) {
			if (this.getCronTaskLogger().isErrorEnabled()) {
				logger.error(this.getLogMessage("Failed to get instance name for PING cron task"),e) ;
			}
		}

		return instanceName ;
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException
	{
		try
		{
			String[] names={"server"};
			String[] defs={ "CocoServer" };
			String[][] descriptions={
				{"schedcron","CronParamServer"} };

			CrontaskParamInfo[] ret=new CrontaskParamInfo[names.length];
			for (int i=0; i<names.length; i++)
			{
				ret[i]=new CrontaskParamInfo();
				ret[i].setName(names[i]);
				ret[i].setDefault(defs[i]);
				ret[i].setDescription(descriptions[i][0], descriptions[i][1]);
			}
			return ret;
		}
		catch(Exception e)
		{
			if (getCronTaskLogger().isErrorEnabled())
				getCronTaskLogger().error(e);
			return null;
		}
	}

}
