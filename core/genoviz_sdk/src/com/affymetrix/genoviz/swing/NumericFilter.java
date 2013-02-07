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
	}
	
	protected abstract String getRegex();
	
	public static class IntegerNumericFilter extends NumericFilter{
		@Override
		protected String getRegex() {
			return "\\D";
		}
	}
	
	public static class FloatNumericFilter extends NumericFilter{
		@Override
		protected String getRegex() {
			return "[^0-9\\.\\-]";
		}
	}
}