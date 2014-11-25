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
package com.talian.test;

import java.lang.Thread.UncaughtExceptionHandler;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import psdi.util.MXAccessException;
import psdi.util.MXSession;

import com.talian.app.assignment.RouteAssignment;
import com.talian.app.heli.HeliServiceRemote;
import com.talian.app.scenario.FlightScenario;

/**
 * @author Seno
 *
 */
public class HelicoTest implements UncaughtExceptionHandler { 

	MXSession s;
	boolean   isQuit ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String msg ;
		HelicoTest test = new HelicoTest() ;
		msg = test.Connect("localhost:13400/MXServer123", "elga", "catamaran") ;
		System.out.println(msg) ;
		if (test.isConnected()) {
			System.out.println("Start the test ");
			test.testBed() ;
			System.out.println("Test is complete ");
		}
		System.exit(0) ;
	}

	public HelicoTest() {
		isQuit = false ;
		s = MXSession.getSession();
	}

	public MXSession getSession()
	{
		return MXSession.getSession(); // Interesting that return s; didn't work
	}

	boolean isConnected () {
		if (s == null)
			return false ;

		return s.isConnected() ;
	}

	public String Connect(String host, String user, String password)
	{
		String retVal = "";

		s.setUserName(user);
		s.setPassword(password);
		s.setHost(host);

		try
		{
			s.connect();
		} catch (MXAccessException ma)
		{
			String group = ma.getErrorGroup();
			String key = ma.getErrorKey();
			retVal = ma.getMessage();
		} catch (Exception e)
		{
            System.err.println(e.getMessage());
			e.printStackTrace();
			retVal = e.getMessage();
		}

		return retVal;
	}

	public void testBed () {
		HeliServiceRemote svcHeli = null ;
		Date reserveDate = reserveDate("21-AUG-2013") ;
		try {
			svcHeli = (HeliServiceRemote)s.lookup("HELI") ;
			LogMonitor lm = new LogMonitor(svcHeli) ;
			lm.start() ;

			FlightScenario scenario = FlightScenario.newRecord(s, reserveDate, "1500") ;

			scenario.setFleetAvailability("PK-TPD", null, true) ;
			scenario.setFleetAvailability("PK-TPE", null, false) ;
			scenario.setFleetAvailability("PK-TPF", null, false) ;
			scenario.setFleetAvailability("PK-TPG", null, false) ;

			svcHeli.setSchedullerRunning(true) ;
			RouteAssignment ra = new RouteAssignment(scenario, svcHeli) ;
			List<FlightScenario> fsList2 = null;
	    	List<FlightScenario> fsList = ra.findNBest(5, 1*60*1000, false, fsList2) ;   // five minutes
	    	if (fsList != null) {
	    		int nCount = fsList.size() ;
	    		for (int i=0; i<nCount; i++) {
	    			FlightScenario scen = fsList.get(i) ;
	    			System.out.println(scen);
	    			scen.save();
	    		}
	    	}
	    	else {
	    		System.out.println("Scenario is not found");
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			isQuit = true ;
			if (svcHeli != null) {
				try {
					svcHeli.setSchedullerRunning(false) ;
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}


		}
	}

	public Date reserveDate(String myDate)
	{
		Date newDate = null;

		try
		{
			if (myDate.equals(""))
			    newDate = new Date();
			else
			    newDate = new Date(myDate);
		} catch (Exception e)
		{
            System.err.println(e.getMessage());
            e.printStackTrace();
			newDate = new Date();
        }

		return newDate;
	}

	class LogMonitor implements Runnable {
		HeliServiceRemote svcHeli ;
		public LogMonitor(HeliServiceRemote svcHeli) {
			this.svcHeli = svcHeli ;
		}

		public void run ()  {
			while (! isQuit) {
				try {
		    		List<String> logs = svcHeli.getServiceLog() ;
		    		Iterator<String> it = logs.iterator() ;
		    		while (it.hasNext()) {
		    			String r = it.next() ;
		    			System.out.println(r);
		    		}
		    	}
		    	catch (Exception e) {
					e.printStackTrace();
				}

		    	try {
					Thread.sleep(1000) ;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				Thread.sleep(15000) ;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
	    		List<String> logs = svcHeli.getServiceLog() ;
	    		Iterator<String> it = logs.iterator() ;
	    		while (it.hasNext()) {
	    			String r = it.next() ;
	    			System.out.println(r);
	    		}
	    	}
	    	catch (Exception e) {
				e.printStackTrace();
			}
		}

		void start () {
			Thread thd = new Thread(this) ;
			thd.setName("automaticSchedulling" + thd.getId()) ;
			thd.setUncaughtExceptionHandler(HelicoTest.this) ;
			thd.start() ;
		}

	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.err.println(e);
	}

}
