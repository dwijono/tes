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
package com.talian.app.schedulling.data;

import java.rmi.RemoteException ;
import java.util.Hashtable ;

import psdi.mbo.MboRemote ;
import psdi.util.MXException ;

/**
 * @author Seno
 *
 */
public class Demand {
	MboRemote mbo ;
	String org,dest ;
	Fleet assigned ;
	
	Integer npax ;
	Double weight ;
	
	Hashtable<String, Demand> subdemands ;  // key = fleet, _UNASSIGNED
	static private String UNASSIGNED = "_UNASSIGNED" ;
	
	public Demand(MboRemote mbo) {
		this.mbo = mbo ;
		subdemands = new Hashtable<String, Demand>() ;
		assigned = null ;
	}
	
	public void assign (Fleet fleet) {
		assigned = fleet ;
	}
		
	private Demand(Demand other) {
		this(other.mbo) ;
		org = other.org ;
		dest = other.dest ;
		npax = other.npax ;
		weight = other.weight ;
	}
	
	public String getKey() {
		return org + "-" + dest ;
	}
	
	boolean isSplitted () 
	{
		return ! subdemands.isEmpty() ;
	}
	
	// weight is assummed to be propotional to number of passenger
	public Demand splitDemand (Fleet fleet, Double maxpayload) {
		Demand retval = null ;
		if (isSplitted()) {
			Demand unassigned = subdemands.get(UNASSIGNED) ;
			retval = unassigned ;
			if (unassigned.npax > fleet.maximumPax()) {
				subdemands.remove(UNASSIGNED) ;
				Integer beforesplitpax = unassigned.npax ;
				Double beforesplitweight = unassigned.weight ;

				Demand newchild = new Demand (unassigned) ;
				unassigned.npax = unassigned.npax - fleet.maximumPax() ;				
				unassigned.weight = unassigned.npax * beforesplitweight / beforesplitpax ;
				newchild.npax = fleet.maximumPax() ;
				newchild.weight = newchild.npax * beforesplitweight / beforesplitpax ;
				
				subdemands.put(fleet.getKey(), unassigned) ;
				
				retval = newchild ;
			}

			subdemands.put(UNASSIGNED, unassigned) ;
		}
		else {
			retval = this ;

			if (npax > fleet.maximumPax()) {
				Demand newchild0 = new Demand (this) ;
				Demand newchild1 = new Demand (this) ;
				newchild0.npax = newchild0.npax - fleet.maximumPax() ;
				newchild0.weight = newchild0.npax * this.weight / this.npax ;
				newchild1.npax = fleet.maximumPax() ;
				newchild0.weight = newchild1.npax * this.weight / this.npax ;
				
				subdemands.put(UNASSIGNED, newchild0) ;
				subdemands.put(fleet.getKey(), newchild1) ;
				
				retval = newchild1 ;
			}
		}
		
		if (retval != null) {
			if (retval.weight <= maxpayload) 
				retval.assign(fleet) ;
			else retval = null ;
		}
		
		return retval ;
	}
	
	public Double getWeight() {
		return weight ;
	}
	
	public Integer getPax() {
		return npax ;
	}
	
		
	static Demand getInstance (MboRemote mbo) throws RemoteException, MXException {
		Demand fl = new Demand (mbo) ;
		fl.org = mbo.getString("org") ;
		fl.dest = mbo.getString("dest") ;
		fl.npax = mbo.getInt("npax") ;
		fl.weight = mbo.getDouble("weight") ;
		
		return fl ;
	}
}
