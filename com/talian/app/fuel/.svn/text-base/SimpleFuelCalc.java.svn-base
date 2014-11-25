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
package com.talian.app.fuel;

/**
 * @author Seno
 *
 */
public class SimpleFuelCalc implements ITripFuelCalc {

	/* (non-Javadoc)
	 * @see com.talian.app.fuel.ITripFuelCalc#fuelNeeded(com.talian.app.fuel.IFuelBurnedJourney)
	 */
	@Override
	public Double fuelNeeded(IFuelBurnedJourney journey) {
		Double mins = journey.distanceToInMinutes() ;
		Double cruisefuelrate = journey.getConsumer().cruisefuelConsumptionRate() ;
		
		return mins * cruisefuelrate / 60.0 ;
	}

}
