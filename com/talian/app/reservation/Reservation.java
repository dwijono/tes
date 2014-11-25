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

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;
import psdi.mbo.Mbo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;

import com.talian.app.heli.Fleet;
import com.talian.app.route.Leg;

/**
 * @author Seno
 *
 */
public class Reservation extends Mbo implements ReservationRemote {
	private Integer reservationid ;
	private String org ;
	private String dest ;
	private String name ;
	private Double weight ;
	private String servedby ;
	private int    qty ;
	private int    priority ;
	private boolean dummyRefueling ;
	private Double etd ;
	private Double eta ;

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public Reservation(MboSet ms) throws RemoteException {
		super(ms) ;
		qty = 1 ;
		weight = 0.0 ;
		dummyRefueling = false ;
		priority = PRIORITY_NORMAL ;

		etd = Double.MAX_VALUE ;
		eta = -Double.MAX_VALUE ;
	}

	/**
	 * @param ms
	 * @throws RemoteException
	 */
	public Reservation(MboSet ms, int qty, boolean dummyRefueling) throws RemoteException {
		super(ms) ;
		if (dummyRefueling)
			reservationid = 0 ;

		this.qty = qty ;
		weight = 0.0 ;
		this.dummyRefueling = dummyRefueling ;
	}

	public void setServedBy (String servedby) {
		this.servedby = servedby ;
	}

	public void setFirstETD (Double etd) {
		if (this.etd > etd)
			this.etd = etd ;
	}

	public void setLastETA (Double eta) {
		if (this.eta < eta)
			this.eta = eta ;
	}

	public void setFirstETD (Leg leg) {
		if (etd > leg.getETD())
			etd = leg.getETD() ;
	}

	public void setLastETA (Leg leg) {
		if (eta < leg.getETA())
			eta = leg.getETA() ;
	}

	public void resetTiming() {
		etd = Double.MAX_VALUE ;
		eta = Double.MIN_VALUE ;
	}

	public String getServedBy() throws RemoteException  {
		return this.servedby ;
	}

	public void setDummyRefueling(boolean dummy) throws RemoteException {
		dummyRefueling = dummy ;
	}

	public void readfromMbo () throws RemoteException, MXException {
		reservationid = getInt ("areservationid") ;
		setOrg(getString ("org")) ;
		setDest(getString ("dest")) ;
		setPaxName(getString ("displayname")) ;
		setWeight(getDouble ("paxweight") + getDouble ("luggageweight")) ;

		String strPriority = getString("priority") ;
		if ("A".equalsIgnoreCase(strPriority))
			priority = PRIORITY_VIP_01 ;
		else if ("B".equalsIgnoreCase(strPriority))
			priority = PRIORITY_VIP_02 ;
		else
			priority = PRIORITY_NORMAL ;
	}

	static public ReservationRemote readfromMBO (MboRemote mbo) throws RemoteException, MXException {
		ReservationRemote resv = (ReservationRemote)mbo;
		resv.readfromMbo() ;
		return resv ;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getOrg() {
		return org;
	}

	public Integer getPriority () {
		return priority;
	}

	public boolean isBlockingVIP () {
		return priority == PRIORITY_VIP_01 ;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getDest() {
		return dest;
	}

	public void setPaxName(String name) {
		this.name = name;
	}

	public String getPaxName() {
		return name;
	}

	public void setQty(int qty) {
		this.qty = qty ;
	}

	public int getQty() {
		return qty;
	}

	public boolean isDummyRefueling () {
		return dummyRefueling ;
	}


	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getWeight() {
		return weight;
	}

	public Integer getId () {
		return reservationid ;
	}

	public Double getEtd() {
		return etd;
	}

	public Double getEta() {
		return eta;
	}

	public String getEtdAsString() throws RemoteException, MXException {
		return toNormalTime(toStdTime(getString("flightsession")), etd);
	}

	public String getEtaAsString() throws RemoteException, MXException {
		return toNormalTime(toStdTime(getString("flightsession")), eta);
	}

	static public Double toStdTime (String sTime) {
		Double min = new Double(sTime.substring(sTime.length()-(sTime.length()-2))) ;
		Double hour = new Double (sTime.substring(0, sTime.length()-2)) ;

		min += 60*hour ;
		return min ;
	}

	static public String toNormalTime (Double stdtime, Double delta) {
		Double d = stdtime + delta ;
		Double h = Math.floor(d/60) ;
		Double m = d - h* 60 ;

		return String.format("%02d:%02d", h.intValue(), m.intValue()) ;
	}

	@Override
	public String toString() {
		return this.name +"|"+ this.org +"|"+ this.dest + "|";
	}

	public String getInfo () {
		return toString() + (isDummyRefueling() ? " (DFUEL)" : " (Normal)") ;
	}

	static String cellHeader[] = {"reservedate", "flightsession", "displayname", "org", "dest", "priority", "pov", "paxweight", "luggageweight", "paxstatus" } ;

	public static void loadExcel (MboSetRemote mboset, String name, InputStream is) {
		try {
			Workbook workbook = Workbook.getWorkbook(is);
			Sheet sheet = workbook.getSheet(0);
			int maxrows = sheet.getRows() ;

			HashMap<Date, Set<String>> addedFlightsession = new HashMap<Date, Set<String>>() ;

			for (int rowidx=1; rowidx < maxrows; rowidx++) {
				if (sheet.getCell(0, rowidx).getType() != CellType.DATE)
					break ;
				else {
					MboRemote resv = mboset.add() ;

					for (int i=0; i<cellHeader.length; i++) {
						Cell cell = sheet.getCell(i, rowidx) ;
						resv.setValue(cellHeader[i], cell.getContents()) ;
					}
					Date currentDate = new Date() ;
					resv.setValue("changeddate", currentDate) ;
					resv.setValue("reschangeddate", currentDate) ;
					resv.setValue("description", "XLS unload " + name) ;

					Calendar dtResv = Calendar.getInstance() ;
					dtResv.setTime(resv.getDate("reservedate")) ;

					// truncate time
					dtResv.set(Calendar.HOUR_OF_DAY, 0) ;
					dtResv.set(Calendar.MINUTE, 0) ;
					dtResv.set(Calendar.SECOND, 0) ;
					dtResv.set(Calendar.MILLISECOND, 0) ;

					Date rDate = dtResv.getTime() ;
					Set<String> savedFSset = addedFlightsession.get(rDate) ;
					if (savedFSset == null) {
						savedFSset = new HashSet<String> () ;
						addedFlightsession.put(rDate, savedFSset) ;
					}

					String fstobesaved = resv.getString("flightsession") ;
					if (!savedFSset.contains(fstobesaved)) {
						savedFSset.add(fstobesaved) ;
						MboSetRemote flightsessions = resv.getMboSet("flightsession", "flightsession", "trunc(reserveddate)=trunc(:reservedate) and flightsession=:flightsession") ;
						if (flightsessions.moveFirst() == null) {
							MboRemote fs = flightsessions.add() ;
							fs.setValue("reserveddate", resv.getDate("reservedate")) ;
							fs.setValue("flightsession", resv.getString("flightsession")) ;
							fs.setValue("etd", resv.getString("flightsession")) ;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}

	}

	public static final Comparator<ReservationRemote> NAME_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			try {
				return r1.getName().compareTo(r2.getName()) ;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
		}
	} ;

	public static final Comparator<ReservationRemote> ETD_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			Long etd1;
			try {
				etd1 = r1.getEtd().longValue() * 10000L;
				Long etd2 = r2.getEtd().longValue() * 10000L ;

				Long delta = etd1 - etd2 ;
				if (delta == 0L)
					return NAME_ORDER.compare(r1, r2) ;

				return delta.intValue() ;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
		}
	} ;
	
	public static final Comparator<ReservationRemote> ETA_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			Long eta1;
			try {
				eta1 = r1.getEta().longValue() * 10000L;
				Long eta2 = r2.getEta().longValue() * 10000L ;

				Long delta = eta1 - eta2 ;
				if (delta == 0L)
					return NAME_ORDER.compare(r1, r2) ;

				return delta.intValue() ;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
		}
	} ;
	
	public static final Comparator<ReservationRemote> ETDETA_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			Long etd1;
			try {
				etd1 = r1.getEtd().longValue() * 10000L;
				Long etd2 = r2.getEtd().longValue() * 10000L ;

				Long delta = etd1 - etd2 ;
				if (delta == 0L)
					return ETA_ORDER.compare(r1, r2) ;

				return delta.intValue() ;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
		}
	} ;

	public static final Comparator<ReservationRemote> PRIORITY_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			Integer priority1 = 0 ;
			Integer priority2 = 0 ;
			try {
				priority1 = r1.getPriority();
				priority2 = r2.getPriority();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Integer delta = (priority1 - priority2) * 10000 ;
			if (delta == 0)
				return ORGDEST_ORDER.compare(r1, r2) ;

			return delta;
		}
	} ;

	public static final Comparator<ReservationRemote> ORGDEST_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			try {
				String orgdest1 = r1.getOrg() + r1.getDest();
				String orgdest2 = r2.getOrg() + r2.getDest();

				int cmp1 = orgdest1.compareTo(orgdest2)*10000;

				if(cmp1 == 0){
					Integer priority1 = r1.getPriority();
					Integer priority2 = r2.getPriority();

					return priority1 - priority2;
				}

			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return -1 ;
		}
	} ;

	public static final Comparator<ReservationRemote> ORG_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			try {
				return r1.getOrg().compareTo(r2.getOrg()) ;
			}
			catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1 ;
		}
	} ;

	public static final Comparator<ReservationRemote> DEST_ORDER  = new Comparator<ReservationRemote>() {
		public int compare (ReservationRemote r1, ReservationRemote r2) {
			try {
				return r1.getDest().compareTo(r2.getDest()) ;
			}
			catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1 ;
		}
	} ;

}
