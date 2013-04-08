package com.affymetrix.genoviz.swing;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * REF: http://www.coderanch.com/t/345628/GUI/java/DocumentFilter
 */
public abstract class NumericFilter extends DocumentFilter {

	@Override
	public void insertString(DocumentFilter.FilterBypass fb, int offset,
			String text, AttributeSet attr) throws BadLocationException {
		fb.insertString(offset, text.replaceAll(getRegex(), ""), attr);
	}

	// no need to override remove(): inherited version allows all removals  
	@Override
	public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
			String text, AttributeSet attr) throws BadLocationException {
		fb.replace(offset, length, text.replaceAll(getRegex(), ""), attr);
		fb.replace(0, fb.getDocument().getLength(), checkRange(fb.getDocument().getText(0, fb.getDocument().getLength()), text.length()), attr);
	}
	
	protected abstract String getRegex();
	
	protected String checkRange(String check, int insLen){
		return check;
	}
	
	public static class IntegerNumericFilter extends NumericFilter{
		private final int min, max;
		private final String regex;
		
		public IntegerNumericFilter(){
			this(0, Integer.MAX_VALUE);
		}
		
		public IntegerNumericFilter(int min, int max){
			this.min = min;
			this.max = max;
			if(min < 0){
				regex = "[^0-9\\-]";
			} else {
				regex = "\\D";
			}
		}
	
		@Override
		protected String checkRange(String check, int insLen){
			if(check.length() == 1 && insLen == 1 && check.equals("-")){
				return check;
			}
					
			boolean decrease = false;
			try {
				int result = Integer.parseInt(check);
				if(result > max || result < min){
					decrease = true;
				}
			} catch (NumberFormatException nfe) {
				decrease = true;
			}
			if(decrease && check.length() >= insLen){
				return check.substring(0, check.length() - insLen);
			}
			return check;
		}
		
		@Override
		protected String getRegex() {
			return regex;
		}
	}
	
	public static class FloatNumericFilter extends NumericFilter{
		@Override
		protected String getRegex() {
			return "[^0-9\\.\\-]";
		}
	}
}