/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.operator;

/**
 *
 * @author auser
 */
public class IdentityTransform extends AbstractFloatTransformer implements Operator{

	@Override
	public String getName() {
		return "Copy";
	}
	
	@Override
	public float transform(float x) { 
		return x; 
	}
			
}
