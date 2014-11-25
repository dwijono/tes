/*
 *
 *
 *
 * (C) COPYRIGHT Talian Limited, 2010
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited.
 *
 */


package com.talian.app.heli;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import psdi.server.AppService;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.MXSystemException;
import psdi.util.logging.FixedLoggerNames;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

import com.talian.app.scenario.FlightScenario;
import com.talian.app.schedulling.Scheduller;


public class HeliService extends AppService implements HeliServiceRemote
{
	List<String> servicelog = new ArrayList<String>();
	boolean isRunning = false ;
	boolean isStopping = false ;
	int test;
	List<FlightScenario> fsList;

    public List<FlightScenario> getFsList() {
		return fsList;
	}

	public void setFsList(List<FlightScenario> fsList) {
		this.fsList = fsList;
	}

	public HeliService() throws RemoteException
	{
        super();
        test = 0;
    }

    public HeliService(MXServer mxServer) throws RemoteException
	{
    	super(mxServer);
    }

    public boolean isSchedullerRunning ()  throws RemoteException {
    	return isRunning ;
    }

    public void setSchedullerRunning (boolean isRun)  throws RemoteException {
    	if (isRun)
    		isStopping = false ;
    	isRunning = isRun ;
    }

    public void runScheduller(Date tripDate, String flightsession) throws RemoteException, MXException
    {
    	Scheduller scheduller = null ;
    	try {
	    	scheduller = Scheduller.startInstance(this, tripDate, flightsession) ;
	    	scheduller.init() ;

	    }
    	catch (RemoteException re) {
    		if (scheduller != null)
    			scheduller.stop() ;
    		throw re ;
    	}
    	catch (MXException ce) {
    		if (scheduller != null)
    			scheduller.stop() ;
    		throw ce ;
    	}
    	catch (Throwable t) {
    		if (scheduller != null)
    			scheduller.stop() ;
    		throw new MXSystemException("system", "major", t) ;
    	}
    }

    public void putLog (String s) throws RemoteException {
    	synchronized (this) {
    		if (s.startsWith("Found:")) {
    			servicelog.clear();
            	servicelog.add(s) ;
			} else if (s.startsWith("RESUME")) {
    			servicelog.clear();
			} else {
				servicelog.add(s) ;
			}
		}
    }

    public List<String> getServiceLog () throws RemoteException, MXException {
    	synchronized (this) {
    		List<String> result = new ArrayList<String>() ;
    		result.addAll(servicelog) ;
    		servicelog.clear() ;
    		return result ;
		}
    }

    public boolean isRunning (Date tripDate, String flightsession) throws RemoteException, MXException {
    	return Scheduller.isStarted(tripDate, flightsession) ;
    }

	@Override
	public void stopScheduller() throws RemoteException {
		putLog ("Stopping the scheduller..") ;
		isStopping = true ;
	}

	@Override
	public boolean isStopping() throws RemoteException {
		return isStopping;
	}

	private void testSched() {
		test = test + 1;
	}

	public boolean runSched() throws RemoteException {
		if (!isRunning){
			SchedThread thd = new SchedThread();
			thd.start();
			return true;
		}
		return false;
	}

	class SchedThread extends Thread {

		@Override
		public void run() {

			try {
				while (!isStopping) {
					isRunning = true;
					testSched();
					if (test > 65535) {
						test = 0;
					}
						putLog("test = "+ test);

				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			isRunning = false;
		}

	}

	@Override
	public void setScenarioList(List<FlightScenario> fsList)
			throws RemoteException {
		this.fsList = fsList;
	}

	@Override
	public List<FlightScenario> getScenarioList() throws RemoteException {
		return this.fsList;
	}
}
