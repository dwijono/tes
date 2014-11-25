package com.talian.app.heli;

import java.util.Map;

public class BasicFleet {
	public String acreg ;
	public Double eew ;
	public Double mtow ;
	public Double tankcapacity ;
	public Double cruisefuel ;
	public Double alternatefuel ;
	public Double maxspeed ;
	public Integer paxcapacity ;
	public Double resfuel ;
	public Double taxifuel ;
	public Double takeofffuel ;
	public String startpos ;
	public String endpos ;
	public String refuelport ;
	public Double groundtime ;
	public Double taxitime ;
	public Double refuelingtime ;
	public Double oatbase ;
	public String status;

	public BasicFleet basicCopy(){
		BasicFleet bi = new BasicFleet();
		bi.acreg = acreg;
		bi.eew = eew ;
		bi.mtow = mtow;
		bi.tankcapacity = tankcapacity ;
		bi.cruisefuel = cruisefuel ;
		bi.alternatefuel = alternatefuel ;
		bi.maxspeed = maxspeed;
		bi.paxcapacity = paxcapacity ;
		bi.resfuel = resfuel ;
		bi.taxifuel = taxifuel ;
		bi.takeofffuel = takeofffuel ;
		bi.startpos = startpos ;
		bi.endpos = endpos ;
		bi.refuelport = refuelport ;
		bi.groundtime = groundtime ;
		bi.taxitime = taxitime ;
		bi.refuelingtime = refuelingtime ;
		bi.oatbase = oatbase ;
		bi.status = status;

		return bi;
	}

	public void readFromMap(Map<String, String> map){
		if (map.containsKey("acreg"))
			acreg = map.get("acreg") ;
		if (map.containsKey("eew"))
			eew = new Double(map.get("eew")) ;
		if (map.containsKey("mtow"))
			mtow = new Double(map.get("mtow"));
		if (map.containsKey("tankcapacity"))
			tankcapacity = new Double(map.get("tankcapacity")) ;
		if (map.containsKey("cruisefuel"))
			cruisefuel = new Double(map.get("cruisefuel")) ;
		if (map.containsKey("alternatefuel"))
			alternatefuel = new Double(map.get("alternatefuel")) ;
		if (map.containsKey("maxspeed"))
			maxspeed = new Double(map.get("maxspeed"));
		if (map.containsKey("paxcapacity"))
			paxcapacity = new Integer(map.get("paxcapacity")) ;
		if (map.containsKey("resfuel"))
			resfuel = new Double(map.get("resfuel")) ;
		if (map.containsKey("taxifuel"))
			taxifuel = new Double(map.get("taxifuel")) ;
		if (map.containsKey("takeofffuel"))
			takeofffuel = new Double(map.get("takeofffuel")) ;
		if (map.containsKey("startpos"))
			startpos = map.get("startpos") ;
		if (map.containsKey("endpos"))
			endpos = map.get("endpos") ;
		if (map.containsKey("refuelport"))
			refuelport = map.get("refuelport") ;
		if (map.containsKey("groundtime"))
			groundtime = new Double(map.get("groundtime")) ;
		if (map.containsKey("taxitime"))
			taxitime = new Double(map.get("taxitime")) ;
		if (map.containsKey("refuelingtime"))
			refuelingtime = new Double(map.get("refuelingtime")) ;
		if (map.containsKey("oatbase"))
			oatbase = new Double(map.get("oatbase")) ;
		if (map.containsKey("status"))
			status = map.get("status");
	}
}
