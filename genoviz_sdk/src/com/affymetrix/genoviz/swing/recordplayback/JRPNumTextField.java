package com.affymetrix.genoviz.swing.recordplayback;

import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import javax.swing.text.Document;

/**
 *
 * @author hiralv
 */
public class JRPNumTextField extends JRPTextField {

	private static final Pattern NUMBERS = Pattern.compile("[0-9]");
	
	public JRPNumTextField(String id) {
		super(id);
	}
	public JRPNumTextField(String id, Document doc, String text, int columns) {
		super(id, doc, text, columns);
	}
	public JRPNumTextField(String id, int columns) {
		super(id, columns);
	}
	public JRPNumTextField(String id, String text) {
		super(id, text);
	}
	public JRPNumTextField(String id, String text, int columns) {
		super(id, text, columns);
	}
	
	@Override
	public void processKeyEvent(KeyEvent ev) {
		char ch = ev.getKeyChar();
		
		if(NUMBERS.matcher(String.valueOf(ch)).matches() || 
				ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_DELETE
				|| ch == KeyEvent.VK_ENTER){
			super.processKeyEvent(ev);
			return;
		}
			
		ev.consume();
	}

	
}
