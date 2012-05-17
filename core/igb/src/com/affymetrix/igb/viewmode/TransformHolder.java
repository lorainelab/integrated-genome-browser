
package com.affymetrix.igb.viewmode;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.operator.Operator;

/**
 *
 * @author hiralv
 */
public class TransformHolder {
	private static final TransformHolder instance = new TransformHolder();
	
	public static TransformHolder getInstance(){
		return instance;
	}
		
	public Operator getOperator(String transform){
		if(transform == null || transform.isEmpty()){
			return null;
		}
		
		for (final Operator operator : ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls()) {
			if(operator.getName().equals(transform))
				return operator;
		}
		
		return null;
	}
			
	public java.util.List<Operator> getAllTransformFor(com.affymetrix.genometryImpl.parsers.FileTypeCategory category) {
		java.util.List<Operator> operators = new java.util.ArrayList<Operator>();

		operators.add(new com.affymetrix.igb.view.NoneOperator());
		for (final Operator operator : ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls()) {
			if (operator.getOutputCategory() != null && operator.getOperandCountMax(category) == 1) {
				operators.add(operator);
			}
		}

		return operators;
	}
}
