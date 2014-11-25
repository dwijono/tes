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
package com.talian.app.route;

/**
 * @author Seno
 *
 */
public interface IValidationPoint {
	boolean validate () ;
	boolean isPOBValid () ;		// pax on board valid
	boolean isWOBValid () ;		// weight on board valid
}
