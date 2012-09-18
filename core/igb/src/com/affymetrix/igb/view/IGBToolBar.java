
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.DragAndDropJPanel;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.action.SelectionRulesAction;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackListProvider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

/**
 *
 * @author hiralv
 */
public class IGBToolBar extends JToolBar {
	private static final String no_selection_text = "Click the map below to select annotations";
	
	private final JPanel toolbar_items_panel;
	private final JTextField tf;
	private final Font selection_font;
	private final Font no_selection_font;
	
	public IGBToolBar(){
		super();
		
		toolbar_items_panel = new DragAndDropJPanel();
		toolbar_items_panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		toolbar_items_panel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		
		tf = new JTextField(25);
		selection_font = tf.getFont();
		no_selection_font = selection_font.deriveFont(Font.ITALIC);
		setLayout(new BorderLayout());
		
		setup();
	}
	
	private void setup(){
		JPanel selection_panel = new JPanel();
		selection_panel.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
		selection_panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		selection_panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		selection_panel.setBackground(Color.WHITE);
		
		tf.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		tf.setEditable(false);
		
		JLabel lf = new JLabel("Selection Info: ");
		lf.setFont(lf.getFont().deriveFont(Font.ITALIC));
		lf.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		lf.setBackground(Color.WHITE);
		
		selection_panel.add(lf);
		selection_panel.add(tf);
		SelectionRulesAction action = SelectionRulesAction.getAction();
		JButton button = new JButton(action);
		selection_panel.add(button);
		selection_panel.validate();
		
		add(toolbar_items_panel, BorderLayout.LINE_START);
		add(selection_panel, BorderLayout.LINE_END);
		
		super.validate();
		
		setSelectionText(null);
	}
	
	public void setSelectionText(String selection_text) {
		if (selection_text == null || selection_text.length() == 0) {
			tf.setForeground(Color.LIGHT_GRAY);
			tf.setFont(no_selection_font);
			tf.setText(no_selection_text);
			tf.setEnabled(false);
		} else {
			tf.setForeground(Color.BLACK);
			tf.setFont(selection_font);
			tf.setText(selection_text);
			tf.setEnabled(true);
		}
	}

	public void addToolbarAction(GenericAction genericAction, int index){
		JRPButton button = new JRPButtonTLP(genericAction, index);
		button.setHideActionText(true);
		
		int local_index = 0;
		while (local_index < index && local_index < toolbar_items_panel.getComponentCount() 
				&& index >= getOrdinal(toolbar_items_panel.getComponent(local_index))) {
			local_index++;
		}
		
		toolbar_items_panel.add(button, local_index);	
		toolbar_items_panel.validate();
	}
	
	public void removeToolbarAction(GenericAction action) {
		boolean removed = false;
		for (int i = 0; i < toolbar_items_panel.getComponentCount(); i++) {
			if (((JButton)toolbar_items_panel.getComponent(i)).getAction() == action) {
				toolbar_items_panel.remove(i);
				toolbar_items_panel.validate();
				toolbar_items_panel.repaint(); // to really make it gone.
				removed = true;
				break;
			}
		}
		if (!removed) {
			System.err.println(this.getClass().getName()
					+ ".removeToolbarAction: Could not find " + action);
		}
	}

	public void saveToolBar() {
		int index = 0;
		for(Component c : toolbar_items_panel.getComponents()){
			if(c instanceof JButton && ((JButton)c).getAction() instanceof GenericAction){
				GenericAction action = (GenericAction) ((JButton)c).getAction();
				PreferenceUtils.getToolbarNode().putInt(action.getId()+".index", index++);
			}
		}
	}
	
	public int getItemCount() {
		return toolbar_items_panel.getComponentCount();
	}
	
	private int getOrdinal(Component c) {
		int ordinal = 0;
		if (c instanceof JRPButtonTLP) {
			ordinal = ((JRPButtonTLP)c).getIndex();
		}
		return ordinal;
	}

	private class JRPButtonTLP extends JRPButton implements TrackListProvider {
		private static final long serialVersionUID = 1L;
		private final int index;
		private JRPButtonTLP(GenericAction genericAction, int index) {
    		super("Toolbar_" + genericAction.getId(), genericAction);
			setHideActionText(true);
			this.index = index;
    	}
		public int getIndex(){
			return index;
		}
		@Override
		public List<TierGlyph> getTrackList() {
			return ((IGB)Application.getSingleton()).getMapView().getTierManager().getSelectedTiers();
		}
    }
	
}
