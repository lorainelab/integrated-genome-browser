package com.affymetrix.igb.tabs.annotation;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import com.jidesoft.combobox.ColorComboBox;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.shared.ChangeExpandMaxOptimizeAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.*;
import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author hiralv
 */
public class AnnotationPanelImpl extends AnnotationPanel implements Selections.RefreshSelectionListener{
	private static final long serialVersionUID = 1L;
	protected IGBService igbService;
	
	public AnnotationPanelImpl(IGBService _igbService){
		super();
		igbService = _igbService;
		resetAll();
		Selections.addRefreshSelectionListener(this);
	}
	
	private void updateDisplay() {
		updateDisplay(true, true);
	}

	private void updateDisplay(final boolean preserveX, final boolean preserveY){
		ThreadUtils.runOnEventQueue(new Runnable() {
	
			public void run() {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
				igbService.getSeqMapView().updatePanel(preserveX, preserveY);
			}
		});
	}
	
	private void refreshView() {
		ThreadUtils.runOnEventQueue(new Runnable() {	
			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}
	
	private void setStackDepth() {
		final JTextField stackDepthTextField = getStackDepthTextField();
		String mdepth_string = stackDepthTextField.getText();
		if (mdepth_string == null) {
			return;
		}
		
		try{
			Actions.setStackDepth(Integer.parseInt(mdepth_string));
			updateDisplay(true, false);
		}catch(Exception ex){
			ErrorHandler.errorPanel("Invalid value "+mdepth_string);
		}
	}
	
	@Override
	protected void stackDepthTextFieldActionPerformedA(ActionEvent evt) {
		setStackDepth();
	}

	@Override
	protected void labelFieldComboBoxActionPerformedA(ActionEvent evt) {
		final JComboBox labelFieldComboBox = getLabelFieldComboBox();
		String labelField = (String)labelFieldComboBox.getSelectedItem();
		if (labelField == null) {
			return;
		}
		Actions.setLabelField(labelField);
		updateDisplay();
	}

	@Override
	protected void strands2TracksCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox strands2TracksCheckBox = getStrands2TracksCheckBox();
		Actions.showOneTwoTier(!(strands2TracksCheckBox.isSelected()), evt);
		updateDisplay();
	}

	@Override
	protected void strandsArrowCheckBoxActionPerformedA(ActionEvent evt) {
		final JCheckBox strandsArrowCheckBox = getStrandsArrowCheckBox();
		Actions.showArrow(strandsArrowCheckBox.isSelected(), evt);
		updateDisplay();
	}

	@Override
	protected void strandsColorCheckBoxActionPerformedA(ActionEvent evt) {
		 final JCheckBox strandsColorCheckBox = getStrandsColorCheckBox();
		Actions.showStrandsColor(strandsColorCheckBox.isSelected(), evt);
		is_listening = false;
		strandsForwardColorComboBoxReset();
		strandsReverseColorComboBoxReset();
		is_listening = true;
		updateDisplay();
	}

	@Override
	protected void strandsReverseColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox strandsReverseColorComboBox = getStrandsReverseColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = strandsReverseColorComboBox.getSelectedColor();
		Actions.setStrandsReverseColor(color);
		updateDisplay();
	}

	@Override
	protected void strandsForwardColorComboBoxActionPerformedA(ActionEvent evt) {
		 final ColorComboBox strandsForwardColorComboBox = getStrandsForwardColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = strandsForwardColorComboBox.getSelectedColor();
		Actions.setStrandsForwardColor(color);
		updateDisplay();
	}

	@Override
	protected void stackDepthGoButtonActionPerformedA(ActionEvent evt) {
		setStackDepth();
	}

	@Override
	protected void stackDepthAllButtonActionPerformedA(ActionEvent evt) {
		ChangeExpandMaxOptimizeAction.getAction().actionPerformed(evt);
		int optimum = getOptimum();
		if(optimum != -1){
			getStackDepthTextField().setText("" + optimum);
		}else{
			getStackDepthTextField().setText("");
		}
	}

	@Override
	protected void setPxHeightTextBoxActionPerformedA(ActionEvent evt) {
		pxGoButtonActionPerformedA(evt);
	}

	@Override
	protected void lockTierHeightCheckBoxActionPerformedA(ActionEvent evt){
		JCheckBox lockTierHeightCheckBox = getLockTierHeightCheckBox();
		if(lockTierHeightCheckBox.isSelected()){
			LockTierHeightAction.getAction().actionPerformed(evt);
		}else{
			UnlockTierHeightAction.getAction().actionPerformed(evt);
		}
	}
	
	@Override
	protected void pxGoButtonActionPerformedA(ActionEvent evt) {
		final JTextField pxTextField = this.getSetPxHeightTextBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		int height = Integer.valueOf(pxTextField.getText());
		Actions.setLockedTierHeight(height);
		updateDisplay();
	}

	@Override
	protected void stackDepthTextFieldReset() {
		JTextField stackDepthTextField = getStackDepthTextField();
		boolean enabled = allGlyphs.size() > 0 && isAllAnnot();
		stackDepthTextField.setEnabled(enabled);
		stackDepthTextField.setText("");
		if (enabled) {
			Integer stackDepth = -1;
			boolean stackDepthSet = false;
			for (StyledGlyph glyph : allGlyphs) {
				if (stackDepth == -1 && !stackDepthSet) {
					if (glyph instanceof TierGlyph) {
						switch (((TierGlyph) glyph).getDirection()) {
							case FORWARD:
								stackDepth = glyph.getAnnotStyle().getForwardMaxDepth();
								break;
							case REVERSE:
								stackDepth = glyph.getAnnotStyle().getReverseMaxDepth();
								break;
							default:
								stackDepth = glyph.getAnnotStyle().getMaxDepth();
						}
					}
					stackDepthSet = true;
				} else if (stackDepth != glyph.getAnnotStyle().getMaxDepth()) {
					stackDepth = -1;
					break;
				}
			}
			if (stackDepth != -1) {
				stackDepthTextField.setText("" + stackDepth);
			}
		}
	}

	@Override
	protected void labelFieldComboBoxReset() {
		JComboBox labelFieldComboBox = getLabelFieldComboBox();
		labelFieldComboBox.setEnabled(isAllAnnot());
		getLabelFieldLabel().setEnabled(isAllAnnot());
		String labelField = null;
		boolean labelFieldSet = false;
		Set<String> allFields = null;
		for (ITrackStyleExtended style : annotStyles) {
			if (style.getLabelField() != null) {
				String field = style.getLabelField();
				if (!labelFieldSet) {
					labelField = field;
					labelFieldSet = true;
				}
				else if (labelField != null && !field.equals(labelField)) {
					labelField = null;
				}
			}
			Set<String> fields = getFields(style);
			SeqSymmetry sym = GenometryModel.getInstance().getSelectedSeq().getAnnotation(style.getMethodName());
			if (sym instanceof SeqSymmetry) {
				if (allFields == null) {
					allFields = new TreeSet<String>(fields);
				}
				else {
					allFields.retainAll(fields);
				}
			}
		}
		if (allFields == null) {
			allFields = new TreeSet<String>();
			allFields.add("* none *");
			allFields.add("id");
			if (labelField != null && labelField.trim().length() > 0) {
				allFields.add(labelField);
			}
		}
		
		labelFieldComboBox.setModel(new DefaultComboBoxModel(allFields.toArray()));
		if (labelField != null) {
			labelFieldComboBox.setSelectedItem(labelField);
		}
	}

	@Override
	protected void strands2TracksCheckBoxReset() {
		JCheckBox strands2TracksCheckBox = getStrands2TracksCheckBox();
		strands2TracksCheckBox.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
		boolean all2Tracks = isAllAnnot();
		for (ITrackStyleExtended style : annotStyles) {
			if (!style.getSeparate()) {
				all2Tracks = false;
				break;
			}
		}
		strands2TracksCheckBox.setSelected(!(all2Tracks));
	}

	@Override
	protected void strandsArrowCheckBoxReset() {
		JCheckBox strandsArrowCheckBox = getStrandsArrowCheckBox();
		strandsArrowCheckBox.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
		strandsArrowCheckBox.setSelected(isAllAnnot() && isAllStrandsArrow());
	}

	@Override
	protected void strandsColorCheckBoxReset() {
		JCheckBox strandsColorCheckBox = getStrandsColorCheckBox();
		strandsColorCheckBox.setEnabled(isAllAnnot() && isAllSupportTwoTrack());
		strandsColorCheckBox.setSelected(isAllAnnot() && isAllStrandsColor());
	}

	@Override
	protected void strandsReverseColorComboBoxReset() {
		ColorComboBox strandsReverseColorComboBox = getStrandsReverseColorComboBox();
		strandsReverseColorComboBox.setEnabled(isAllAnnot() && isAllStrandsColor() && isAllSupportTwoTrack());
		getStrandsReverseColorLabel().setEnabled(isAllAnnot() && isAllStrandsColor());
		Color strandsReverseColor = null;
		if (isAllAnnot() && isAllStrandsColor()) {
			boolean strandsReverseColorSet = false;
			for (ITrackStyleExtended style : annotStyles) {
				if (strandsReverseColor == null && !strandsReverseColorSet) {
					strandsReverseColor = style.getReverseColor();
					strandsReverseColorSet = true;
				}
				else if (strandsReverseColor != style.getReverseColor()) {
					strandsReverseColor = null;
					break;
				}
			}
		}
		strandsReverseColorComboBox.setSelectedColor(strandsReverseColor);
	}

	@Override
	protected void strandsForwardColorComboBoxReset() {
		ColorComboBox strandsForwardColorComboBox = getStrandsForwardColorComboBox();
		strandsForwardColorComboBox.setEnabled(isAllAnnot() && isAllStrandsColor() && isAllSupportTwoTrack());
		getStrandsForwardColorLabel().setEnabled(isAllAnnot() && isAllStrandsColor());
		Color strandsForwardColor = null;
		if (isAllAnnot() && isAllStrandsColor()) {
			boolean strandsForwardColorSet = false;
			for (ITrackStyleExtended style : annotStyles) {
				if (strandsForwardColor == null && !strandsForwardColorSet) {
					strandsForwardColor = style.getForwardColor();
					strandsForwardColorSet = true;
				}
				else if (strandsForwardColor != style.getForwardColor()) {
					strandsForwardColor = null;
					break;
				}
			}
		}
		strandsForwardColorComboBox.setSelectedColor(strandsForwardColor);
	}

	@Override
	protected void stackDepthGoButtonReset() {
		JButton stackDepthGoButton = getStackDepthGoButton();
		stackDepthGoButton.setEnabled(annotStyles.size() > 0 && isAllAnnot());
	}

	@Override
	protected void stackDepthAllButtonReset() {
		JButton stackDepthAllButton = getStackDepthAllButton();
		stackDepthAllButton.setEnabled(annotSyms.size() > 0 && isAllAnnot());
	}

	@Override
	protected void lockTierHeightCheckBoxReset() {
		JCheckBox lockTierHeightCheckBox = getLockTierHeightCheckBox();
		lockTierHeightCheckBox.setSelected(isAnyLocked());
		if((!isAllButOneLocked() && isAnyLockable()) || isAnyLocked()){
			lockTierHeightCheckBox.setEnabled(true);
		}else{
			lockTierHeightCheckBox.setEnabled(false);
		}
	}

	@Override
	protected void setPxHeightTextBoxReset() {
		JTextField pxHeightTextField = getSetPxHeightTextBox();
		pxHeightTextField.setEnabled(isAnyLocked());
		pxHeightTextField.setText("");
		if(pxHeightTextField.isEnabled()){
			pxHeightTextField.setText(""+getLockedHeight());
		}
	}

	@Override
	protected void pxGoButtonReset() {
		JButton pxGoButton = getPxGoButton();
		pxGoButton.setEnabled(isAnyLocked());
	}
	
	private Set<String> getFields(ITrackStyleExtended style) {
		Set<String> fields = new TreeSet<String>();
		BioSeq seq = GenometryModel.getInstance().getSelectedSeq();
		if (seq != null) {
			SeqSymmetry sym = seq.getAnnotation(style.getMethodName());
			if (sym != null && sym.getChildCount() > 0) {
				SeqSymmetry child = sym.getChild(0);
				SeqSymmetry original = getMostOriginalSymmetry(child);
				if (original instanceof SymWithProps) {
					Map<String, Object> props = ((SymWithProps) original).getProperties();
					fields.add("* none *");
					if(props != null){
						fields.addAll(props.keySet());
					}
				}
			}
		}
		return fields;
	}
	
	private static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		}
		return sym;
	}
	
	@Override
	public void selectionRefreshed() {
		resetAll();
	}
	
}
