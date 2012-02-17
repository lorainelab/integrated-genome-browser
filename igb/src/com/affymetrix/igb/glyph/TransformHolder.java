
package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.operator.LogTransform;
import com.affymetrix.genometryImpl.operator.NotOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.igb.tiers.TrackConstants;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class TransformHolder {
	java.util.LinkedHashMap<String, Operator> transform2Operator = new java.util.LinkedHashMap<String, Operator>();
	private static final TransformHolder instance = new TransformHolder();
	
	public static TransformHolder getInstance(){
		return instance;
	}
	
	private TransformHolder(){
		// Adding operators
		addOperator(new NotOperator());
		addOperator(new LogTransform(2.0));
	}
		
	public Operator getOperator(String transform){
		if(transform == null){
			return null;
		}
		return transform2Operator.get(transform);
	}
		
	public final void addOperator(Operator operator){
		if(operator == null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add null operator");
			return;
		}
		String transform = operator.getName();
		if(transform2Operator.get(transform) != null){
			Logger.getLogger(MapViewModeHolder.class.getName()).log(Level.WARNING, "Trying to add duplicate operator for {0}", transform);
			return;
		}
		transform2Operator.put(transform, operator);
	}
	
	public final void removeOperator(Operator operator){
		transform2Operator.remove(operator.getName());
	}
	
	public Object[] getAllTransformFor(String file_format) {
		java.util.List<Object> mode = new java.util.ArrayList<Object>(transform2Operator.size());
		FileTypeHandler handler = FileTypeHolder.getInstance().getFileTypeHandler(file_format);
		if(handler == null){
			return mode.toArray(new Object[0]);
		}
		
		FileTypeCategory file_category = handler.getFileTypeCategory();
		if (file_category != null) {
			mode.add(TrackConstants.default_operator);
			for (java.util.Map.Entry<String, Operator> entry : transform2Operator.entrySet()) {
				Operator emv = entry.getValue();
				if (emv.getOperandCountMax(file_category) == 1 &&
						emv.getOperandCountMin(file_category) == 1) {
					mode.add(entry.getKey());
				}
			}
		}
		
		return mode.toArray(new Object[0]);
	}
}
