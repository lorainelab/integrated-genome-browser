/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.GenometryConstants;

/**
 *
 * @author auser
 */
public class IdentityTransform extends AbstractFloatTransformer implements Operator{

	@Override
	public String getName() {
		return "copy";
	}

	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
	}
	
	@Override
	public float transform(float x) { 
		return x; 
	}
}
