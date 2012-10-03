package com.affymetrix.igb.trackOperations;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;
import com.affymetrix.igb.shared.OperationsImpl;
import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author hiralv
 */
public class GraphOperationsImpl extends OperationsImpl{
	
	boolean is_listening = true; // used to turn on and off listening to GUI events
	private javax.swing.JButton combineB, splitB, threshB;
	
	GraphOperationsImpl(IGBService igbS){
		super(igbS);
	}
	
	@Override
	protected void initComponents(IGBService igbS){
		combineB = new javax.swing.JButton(new CombineGraphsAction(igbS));
		splitB = new javax.swing.JButton(new SplitGraphsAction(igbS));
		threshB = new javax.swing.JButton(ThresholdingAction.createThresholdingAction(igbS));
		threshB.setText("Thresholding");
		
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getBtPanel());
		getBtPanel().setLayout(layout);	
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addComponent(combineB)
				.addComponent(splitB)
				.addComponent(threshB));
		layout.setVerticalGroup(
				layout.createSequentialGroup().
				addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
				.addComponent(combineB)
				.addComponent(splitB)
				.addComponent(threshB)));
		
		getSingleTrackLabel().setText("Single Graph");
		getMultiTrackLabel().setText("Multi Graph");
	}
	
	public void setPanelEnabled(boolean enable) {
		super.setPanelEnabled(enable);
		is_listening = false;
		
		combineB.setEnabled(enable && graphGlyphs.size() > 1 && !isAnyJoined());
		splitB.setEnabled(enable && isAnyJoined());
		threshB.setEnabled(enable && !graphGlyphs.isEmpty());
		
		is_listening = true;
	}
}
