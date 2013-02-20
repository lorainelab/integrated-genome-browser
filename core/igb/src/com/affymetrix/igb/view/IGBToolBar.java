
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.OrderComparator;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.swing.CCPUtils;
import com.affymetrix.genoviz.swing.DragAndDropJPanel;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.Selections.RefreshSelectionListener;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackListProvider;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author hiralv
 */
public class IGBToolBar extends JToolBar {
	private static final String no_selection_text = "Click the map below to select annotations";
	private static final String selection_info = "Selection Info";
	private static final Comparator<String> comparator = new OrderComparator(PropertyHandler.prop_order);
	private static final SeqMapView smv = (SeqMapView) IGBServiceImpl.getInstance().getSeqMapView();
	
	public final IGBToolBarMapRangeBox mapRangeBox = new IGBToolBarMapRangeBox(smv);
	private final JPanel toolbar_items_panel;
	private final JTextField tf;
	private final Font selection_font;
	private final Font no_selection_font;
	private Map<String, Object> properties;
	
	public IGBToolBar(){
		super();
		
		toolbar_items_panel = new DragAndDropJPanel();
		toolbar_items_panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		toolbar_items_panel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		((DragAndDropJPanel)toolbar_items_panel).addDropTargetListener(new DropTargetAdapter(){
			@Override
			public void drop(DropTargetDropEvent dtde) {
				reIndex();
			}
		});
		
		tf = mapRangeBox.range_box;
		tf.setColumns(25);
		tf.setBackground(Color.WHITE);
		tf.setComponentPopupMenu(CCPUtils.getCCPPopup());
		selection_font = new JTextField().getFont();
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
			
		JLabel lf = new JLabel(selection_info+": ");
		lf.setFont(lf.getFont().deriveFont(Font.ITALIC));
		lf.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		lf.setBackground(Color.WHITE);
		
		selection_panel.add(lf);
		selection_panel.add(tf);
		
		JButton button = new JButton(new SelectionRulesAction());
		button.setMargin(new Insets(2,2,2,2));
//		button.setBorder(null);
		selection_panel.add(button);
		
		selection_panel.validate();
		
		add(toolbar_items_panel, BorderLayout.LINE_START);
		add(selection_panel, BorderLayout.LINE_END);
		
		super.validate();
		
		setSelectionText(null, null);
		
		Selections.addRefreshSelectionListener(refreshSelectionListener);
	}
	
	public void setSelectionText(Map<String, Object> properties, String selection_text) {
		if (selection_text == null || selection_text.length() == 0) {
			tf.setForeground(Color.LIGHT_GRAY);
			tf.setFont(no_selection_font);
			tf.setText(no_selection_text);
			tf.setEnabled(true);
		} else {
			tf.setForeground(Color.BLACK);
			tf.setFont(selection_font);
			tf.setText(selection_text);
			tf.setEnabled(true);
		}
		this.properties = properties;
	}

	public void addToolbarAction(GenericAction genericAction, int index){
		JRPButton button = new JRPButtonTLP(genericAction, index);
		button.setHideActionText(true);
		//button.setBorder(new LineBorder(Color.BLACK));
		button.setMargin(new Insets(0,0,0,0));
		if(genericAction instanceof ContinuousAction){
			button.addMouseListener(continuousActionListener);
		}
		
		int local_index = 0;
		while (local_index < index && local_index < toolbar_items_panel.getComponentCount() 
				&& index >= getOrdinal(toolbar_items_panel.getComponent(local_index))) {
			local_index++;
		}
		
		toolbar_items_panel.add(button, local_index);
		refreshToolbar();
	}
	
	public void removeToolbarAction(GenericAction action) {
		boolean removed = false;
		for (int i = 0; i < toolbar_items_panel.getComponentCount(); i++) {
			if (((JButton)toolbar_items_panel.getComponent(i)).getAction() == action) {
				if(action instanceof ContinuousAction){
					((JButton)toolbar_items_panel.getComponent(i)).removeMouseListener(continuousActionListener);
				}
				toolbar_items_panel.remove(i);
				refreshToolbar();
				removed = true;
				break;
			}
		}
		
		if (removed) {
			reIndex();
		}else{
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, ".removeToolbarAction: Could not find " + action);
		}
	}

	private void refreshToolbar(){
		toolbar_items_panel.validate();
		toolbar_items_panel.repaint();
		
		validate();
		repaint();
	}
	
	public void reIndex(){
		int index = 0;
		for(Component c : toolbar_items_panel.getComponents()){
			if(c instanceof JRPButtonTLP){
				((JRPButtonTLP)c).setIndex(index++);
			}
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
	
	public IGBToolBarMapRangeBox getIGBToolBarMapRangeBox() {
		return mapRangeBox;
	}
	
	public void addSearchListener(SearchListener search_listener) {
		mapRangeBox.addSearchListener(search_listener);
	}
	
	public void removeSearchListener(SearchListener search_listener) {
		mapRangeBox.removeSearchListener(search_listener);
	}

	private RefreshSelectionListener refreshSelectionListener = new RefreshSelectionListener(){
		@Override
		public void selectionRefreshed() {
			for (Component c : toolbar_items_panel.getComponents()) {
				if (c instanceof JButton && ((JButton) c).getAction() instanceof AbstractAction) {
					AbstractAction action = (AbstractAction) ((JButton) c).getAction();
					c.setEnabled(action.isEnabled());
				}
			}
		}
	};
			
	private MouseListener continuousActionListener = new MouseAdapter() {
		private Timer timer;

		@Override
		public void mousePressed(MouseEvent e) {
			if (!(e.getSource() instanceof JButton)
					|| ((JButton) e.getSource()).getAction() == null) {
				return;
			}

			timer = new Timer(200, ((JButton) e.getSource()).getAction());
			timer.start();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if(timer != null) {
				timer.stop();
			}
		}
	};
		
	private class JRPButtonTLP extends JRPButton implements TrackListProvider {
		private static final long serialVersionUID = 1L;
		private int index;
		private JRPButtonTLP(GenericAction genericAction, int index) {
    		super("Toolbar_" + genericAction.getId(), genericAction);
			setHideActionText(true);
			this.index = index;
    	}
		private int getIndex(){
			return index;
		}
		private void setIndex(int i) {
			this.index = i;
		}
		@Override
		public void fireActionPerformed(ActionEvent evt){
			if(((GenericAction)getAction()).isToggle() && this.getAction().getValue(AbstractAction.SELECTED_KEY) != null){
				this.getAction().putValue(AbstractAction.SELECTED_KEY, 
						!Boolean.valueOf(getAction().getValue(AbstractAction.SELECTED_KEY).toString()));
			}
			super.fireActionPerformed(evt);
		}
		@Override
		public List<TierGlyph> getTrackList() {
			return ((IGB)Application.getSingleton()).getMapView().getTierManager().getSelectedTiers();
		}
    }
	
	private class SelectionRulesAction extends GenericAction {	
		private static final long serialVersionUID = 1l;

		public SelectionRulesAction() {
			super(null, "16x16/actions/info.png", "16x16/actions/info.png");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JFrame messageFrame = new JFrame();
			JTextArea rules_text = new JTextArea();
			rules_text.setBorder(new EmptyBorder(10,10,10,10));
			rules_text.setEditable(false);
			messageFrame.add(rules_text);
			if(no_selection_text.equals(tf.getText())){
				messageFrame.setTitle("How to Select and De-select Data in IGB");
				rules_text.append(getRules());
			}else{
				messageFrame.setTitle(selection_info);
				if(properties != null && !properties.isEmpty()){
					List<String> keys = GeneralUtils.asSortedList(properties.keySet(), comparator);
					for(String key : keys){
						rules_text.append(key + " : " + properties.get(key) + "\n");
					}
				}else{
					rules_text.append(tf.getText());
				}
			}
			messageFrame.setMinimumSize(new Dimension(250,100));
			messageFrame.pack();
			messageFrame.setLocationRelativeTo(IGB.getSingleton().getFrame());
			messageFrame.setVisible(true);
		}
		
		private String getRules(){
			return "1. Click on an annotation to select it.\n"
					+ "2. Double-click something to zoom in on it.\n"
					+ "3. Click-drag a region to select and count many items.\n"
					+ "4. Click-SHIFT to add to the currently selected items.\n"
					+ "5. Control-SHIFT click to remove an item from the currently selected items.\n"
					+ "6. Click-drag the axis to zoom in on a region.\n";
		}
	}
	
	private class IGBToolBarMapRangeBox extends MapRangeBox {

		public IGBToolBarMapRangeBox(SeqMapView smv) {
			super(smv);
			range_box.addFocusListener(tool_bar_focus_listener);
			range_box.addMouseListener(tool_bar_mouse_listener);
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			setRange(range_box.getText());
		}
		
		@Override
		public void setRange(String search_text) {
			List<SeqSpan> mergedSpans = getSpanList(gview, search_text);
			if (mergedSpans != null && mergedSpans.size() > 0) {
				foundSpans = mergedSpans;
				spanPointer = 0;
				if (foundSpans.size() > 1) {
					range_box.setText(foundSpans.size() + " spans found");
				} else {
				}
			} else {
				range_box.setText("Nothing found");
			}
		}
		
		@Override
		public void addSearchListener(SearchListener listener) {
			search_listeners.add(listener);
		}

		@Override
		public void removeSearchListener(SearchListener listener) {
			search_listeners.remove(listener);
		}

		@Override
		public void viewBoxChanged(NeoViewBoxChangeEvent e) {
			// Do nothing when span changes
		}
		
		@Override
		public void groupSelectionChanged(GroupSelectionEvent evt) {
			super.groupSelectionChanged(evt);
			if(GenometryModel.getGenometryModel().getSelectedSeqGroup()!=null) {
				range_box.setEditable(true);
			} else {
				range_box.setEditable(false);
				setSelectionText(null, null);
			}
		}

		FocusListener tool_bar_focus_listener = new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if(range_box.getText().equals(no_selection_text)) {
					range_box.setText("");
					range_box.setForeground(Color.BLACK);
					range_box.setFont(selection_font);
				} else {
					range_box.setSelectionStart(0);
					range_box.setSelectionEnd(range_box.getText().length());
				}
			}

			@Override
			public void focusLost(FocusEvent e) {}
		};
		
		MouseListener tool_bar_mouse_listener = new MouseListener() {

			public void mouseClicked(MouseEvent me) {
				// Always clean or select the text box when clicking after group selected (not in the start view)
				tool_bar_focus_listener.focusGained(null);
			}

			public void mousePressed(MouseEvent me) {
			}

			public void mouseReleased(MouseEvent me) {
			}

			public void mouseEntered(MouseEvent me) {
			}

			public void mouseExited(MouseEvent me) {
			}
			
		};
	}
}
