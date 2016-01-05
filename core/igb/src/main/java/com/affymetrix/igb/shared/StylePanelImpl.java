package com.affymetrix.igb.shared;

import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.util.ThreadUtils;
import com.jidesoft.combobox.ColorComboBox;
import org.lorainelab.igb.services.IgbService;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JComboBox;

public abstract class StylePanelImpl extends StylePanel implements Selections.RefreshSelectionListener {

    private static final long serialVersionUID = 1L;
    protected IgbService igbService;
    protected final List<ITrackStyleExtended> styles;

    public StylePanelImpl(IgbService _igbService) {
        super();
        igbService = _igbService;
        styles = new CopyOnWriteArrayList<>();
        setStyles();
        resetAll();
        Selections.addRefreshSelectionListener(this);
    }

    private void updateDisplay() {
        updateDisplay(true, true);
    }

    private void updateDisplay(final boolean preserveX, final boolean preserveY) {
        ThreadUtils.runOnEventQueue(() -> {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
            igbService.getSeqMapView().updatePanel(preserveX, preserveY);
        });
    }

    private void refreshView() {
        ThreadUtils.runOnEventQueue(() -> igbService.getSeqMap().updateWidget());
    }

    @Override
    protected void labelSizeComboBoxActionPerformedA(ActionEvent e) {
        final JComboBox labelSizeComboBox = getLabelSizeComboBox();
        int fontsize = (Integer) labelSizeComboBox.getSelectedItem();
        if (fontsize <= 0) {
            return;
        }
        Actions.setTierFontSize(fontsize);
        updateDisplay();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    protected void foregroundColorComboBoxActionPerformedA(ActionEvent e) {
        final ColorComboBox foregroundColorComboBox = getForegroundColorComboBox();
        if (igbService.getSeqMap() == null) {
            return;
        }
        Color color = foregroundColorComboBox.getSelectedColor();
        Actions.setForegroundColor(color);
        updateDisplay();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    protected void backgroundColorComboBoxActionPerformedA(ActionEvent e) {
        final ColorComboBox backgroundColorComboBox = getBackgroundColorComboBox();
        if (igbService.getSeqMap() == null) {
            return;
        }
        Color color = backgroundColorComboBox.getSelectedColor();
        Actions.setBackgroundColor(color);
        updateDisplay();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    protected void labelColorComboBoxActionPerformedA(ActionEvent e) {
        final ColorComboBox labelColorComboBox = getLabelColorComboBox();
        Color color = labelColorComboBox.getSelectedColor();
        Actions.setLabelColor(color);
        updateDisplay();
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    protected void labelSizeComboBoxReset() {
        JComboBox labelSizeComboBox = getLabelSizeComboBox();
        Integer labelSize = -1;
        boolean labelSizeSet = false;
        for (ITrackStyleExtended style : styles) {
            if (labelSize == -1 && !labelSizeSet) {
                labelSize = (int) style.getTrackNameSize();
                labelSizeSet = true;
            } else if (labelSize != (int) style.getTrackNameSize()) {
                labelSize = -1;
            }
        }
        boolean enable = styles.size() > 0 && !isAnyFloat();
        labelSizeComboBox.setEnabled(enable);
        getLabelSizeLabel().setEnabled(enable);
        if (!enable || labelSize == -1) {
            labelSizeComboBox.setSelectedIndex(-1);
        } else {
            labelSizeComboBox.setSelectedItem(labelSize);
        }
    }

    @Override
    protected void foregroundColorComboBoxReset() {
        ColorComboBox foregroundColorComboBox = getForegroundColorComboBox();
        boolean enable = styles.size() > 0;
        foregroundColorComboBox.setEnabled(enable);
        getForegroundColorLabel().setEnabled(enable);
        Color foregroundColor = null;
        if (enable) {
            foregroundColor = styles.get(0).getForeground();
            for (ITrackStyleExtended style : styles) {
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
        boolean enable = styles.size() > 0 && !isAnyFloat();
        Color backgroundColor = null;
        if (enable) {
            backgroundColor = styles.get(0).getBackground();
            for (ITrackStyleExtended style : styles) {
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
        for (ITrackStyleExtended style : styles) {
            if (labelColor == null && !labelColorSet) {
                labelColor = style.getLabelForeground();
                labelColorSet = true;
            } else if (labelColor != style.getLabelForeground()) {
                labelColor = null;
                break;
            }
        }
        boolean enable = styles.size() > 0 && !isAnyFloat();
        labelColorComboBox.setEnabled(enable);
        getLabelColorLabel().setEnabled(enable);
        labelColorComboBox.setSelectedColor(enable ? labelColor : null);
    }

    @Override
    public void selectionRefreshed() {
        styles.clear();
        setStyles();
        resetAll();
    }

    protected abstract void setStyles();

    protected abstract boolean isAnyFloat();
}
