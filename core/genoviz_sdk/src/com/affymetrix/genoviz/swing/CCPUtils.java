package com.affymetrix.genoviz.swing;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/**
 *
 * @author hiralv
 */
public class CCPUtils {

	public static JPopupMenu getCCPPopup(){
        final JMenuItem cutMenuItem = new JMenuItem(new DefaultEditorKit.CutAction());
        cutMenuItem.setText("Cut");
        
        final JMenuItem copyMenuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
        copyMenuItem.setText("Copy");
        
        final JMenuItem pasteMenuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
        pasteMenuItem.setText("Paste");
        
		final JMenuItem pasteAndGoItem = new JMenuItem(new PasteAndGo());
		pasteAndGoItem.setText("Paste & Go");
		
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(cutMenuItem);
		popupMenu.add(copyMenuItem);
		popupMenu.add(pasteMenuItem);
		popupMenu.add(pasteAndGoItem);
		
		popupMenu.addPopupMenuListener(new PopupMenuListener(){

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				// If component is not enabled then disable all items and return.
				if(e.getSource() != popupMenu 
						|| !(popupMenu.getInvoker() instanceof JTextComponent) 
						|| !((JTextComponent)popupMenu.getInvoker()).isEnabled()){
					cutMenuItem.setEnabled(false);
					copyMenuItem.setEnabled(false);
					pasteMenuItem.setEnabled(false);
					pasteAndGoItem.setEnabled(false);
					return;
				}
				JTextComponent jTextComponent = (JTextComponent)popupMenu.getInvoker();
				
				// First enable all
				cutMenuItem.setEnabled(true);
				copyMenuItem.setEnabled(true);
				pasteMenuItem.setEnabled(true);
				pasteAndGoItem.setEnabled(true);
				
				// Conditions to disable cut
				if(jTextComponent.getSelectedText() == null 
						|| jTextComponent.getSelectedText().length() == 0
						|| !jTextComponent.isEditable()){
					cutMenuItem.setEnabled(false);
				}
				
				// Conditions to disable copy
				if(jTextComponent.getSelectedText() == null 
						|| jTextComponent.getSelectedText().length() == 0){
					copyMenuItem.setEnabled(false);
				}
				
				// Conditions to disable paste
				if(!jTextComponent.isEditable()
						|| getClipboard() == null
						|| getClipboard().length() == 0){
					pasteMenuItem.setEnabled(false);
				}
				
				// Conditions to disable paste & go
				if(!jTextComponent.isEditable()
						|| !(jTextComponent instanceof JTextField)
						|| getClipboard() == null
						|| getClipboard().length() == 0){
					pasteAndGoItem.setEnabled(false);
				}

			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// Do Nothing.
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
				// Do Nothing.
			}
			
			private String getClipboard() {
				Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
				try {
					if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
						String text = (String) t.getTransferData(DataFlavor.stringFlavor);
						return text.trim();
					}
				} catch (Exception e) {
				}
				return "";
			}
			
		});
	
		return popupMenu;
	}
	
	private static class PasteAndGo extends TextAction {
		TextAction pasteAction = new DefaultEditorKit.PasteAction();

		public PasteAndGo(){
			super("Paste & Go");
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(!(getTextComponent(e) instanceof JTextField)) {
				return;
			}
			
			pasteAction.actionPerformed(e);
			JTextField component = (JTextField)getTextComponent(e);
			for(ActionListener al : component.getActionListeners()){
				al.actionPerformed(e);
			}
		}
	
	}
}
