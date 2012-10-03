package com.affymetrix.igb.shared;

import com.jidesoft.combobox.ColorComboBox;
import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import static com.affymetrix.igb.shared.Selections.*;

public class StylePanelImpl extends StylePanel implements Selections.RefreshSelectionListener{
	private static final long serialVersionUID = 1L;
	protected IGBService igbService;
	
	public StylePanelImpl(IGBService _igbService){
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
	
	@Override
	protected void labelSizeComboBoxActionPerformedA(ActionEvent evt) {
		final JComboBox labelSizeComboBox = getLabelSizeComboBox();
		int fontsize = (Integer)labelSizeComboBox.getSelectedItem();
		if (fontsize <= 0) {
			return;
		}
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.TierFontSizeAction");
		action.performAction(fontsize);
		updateDisplay();
	}

	@Override
	protected void foregroundColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox foregroundColorComboBox = getForegroundColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = foregroundColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeForegroundColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void backgroundColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = backgroundColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeBackgroundColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void labelColorComboBoxActionPerformedA(ActionEvent evt) {
		final ColorComboBox labelColorComboBox = getLabelColorComboBox();
		Color color = labelColorComboBox.getSelectedColor();
		ParameteredAction action = (ParameteredAction) GenericActionHolder.getInstance()
				.getGenericAction("com.affymetrix.igb.action.ChangeLabelColorAction");
		if (action != null && color != null) {
			action.performAction(color);
		}
		updateDisplay();
	}

	@Override
	protected void labelSizeComboBoxReset() {
		JComboBox labelSizeComboBox = getLabelSizeComboBox();
		Integer labelSize = -1;
		boolean labelSizeSet = false;
		for (ITrackStyleExtended style: allStyles) {
			if (labelSize == -1 && !labelSizeSet) {
				labelSize = (int)style.getTrackNameSize();
				labelSizeSet = true;
			}
			else if (labelSize != (int)style.getTrackNameSize()) {
				labelSize = -1;
			}
		}
		boolean enable = allStyles.size() > 0 && !isAnyFloat();
		labelSizeComboBox.setEnabled(enable);
		getLabelSizeLabel().setEnabled(enable);
		if (!enable || labelSize == -1) {
			labelSizeComboBox.setSelectedIndex(-1);
		}
		else {
			labelSizeComboBox.setSelectedItem(labelSize);
		}
	}

	@Override
	protected void foregroundColorComboBoxReset() {
		ColorComboBox foregroundColorComboBox = getForegroundColorComboBox();
		boolean enable = allStyles.size() > 0;
		foregroundColorComboBox.setEnabled(enable);
		getForegroundColorLabel().setEnabled(enable);
		Color foregroundColor = null;
		if (enable) {
			foregroundColor = allStyles.get(0).getForeground();
			for (ITrackStyleExtended style : allStyles) {
				if (!(foregroundColor.equals(style.getForeground()))) {
					foregroundColor = null;
					break;
				}
			}
		}
		foregroundColorComboBox.setSelectedColor(foregroundColor);
	}

	@Override
	protected void backgroundColorComboBoxReset() {
		ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
		boolean enable = allStyles.size() > 0 && !isAnyFloat();
		Color backgroundColor = null;
		if (enable) {
			backgroundColor = allStyles.get(0).getBackground();
			for (ITrackStyleExtended style : allStyles) {
				if (backgroundColor != style.getBackground()) {
					backgroundColor = null;
					break;
				}
			}
		}
		backgroundColorComboBox.setEnabled(enable);
		getBackgroundColorLabel().setEnabled(enable);
		backgroundColorComboBox.setSelectedColor(enable ? backgroundColor : null);
	}

	@Override
	protected void labelColorComboBoxReset() {
		// Need to consider joined glyphs
		ColorComboBox labelColorComboBox = getLabelColorComboBox();
		Color labelColor = null;
		boolean labelColorSet = false;
		for (ITrackStyleExtended style : allStyles) {
			if (labelColor == null && !labelColorSet) {
				labelColor = style.getLabelForeground();
				labelColorSet = true;
			}
			else if (labelColor != style.getLabelForeground()) {
				labelColor = null;
				break;
			}
		}
		boolean enable = allStyles.size() > 0 && !isAnyFloat();
		labelColorComboBox.setEnabled(enable);
		getLabelColorLabel().setEnabled(enable);
		labelColorComboBox.setSelectedColor(enable ? labelColor : null);
	}

	@Override
	public void selectionRefreshed() {
		resetAll();
	}
}
