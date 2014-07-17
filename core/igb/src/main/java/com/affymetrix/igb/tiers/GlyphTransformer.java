package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.ExponentialTransform;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.ITransformableTierGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JWindow;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author hiralv
 */
public class GlyphTransformer {

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int WINDOW_OFFSET = 4;
    private static final int SPEED = 10;

    final AffyLabelledTierMap map;
    final Listeners listener;

    JWindow scroll_window;
    JScrollBar scrollbar;
    ExponentialTransform zoomtrans;

    TierLabelGlyph tierlabel;
    ITransformableTierGlyph svmg;

    private int zoom_pos;

    public GlyphTransformer(AffyLabelledTierMap map) {
        this.map = map;
        listener = new Listeners();
    }

    public void startscroll(TierLabelGlyph tlg) {
        if (!(tlg.getReferenceTier() instanceof ITransformableTierGlyph)
                || !((ITransformableTierGlyph) tlg.getReferenceTier()).isScrollingAllowed()) {
            return;
        }

        // Initialize all variables
        zoomtrans = new ExponentialTransform(map.getMinZoom(AffyLabelledTierMap.Y), map.getMaxZoom(AffyLabelledTierMap.Y),
                map.getZoomer(AffyLabelledTierMap.Y).getMinimum(), map.getZoomer(AffyLabelledTierMap.Y).getMaximum());
        tierlabel = tlg;
        svmg = (ITransformableTierGlyph) tlg.getReferenceTier();

//		scrollbar = getScrollBar(tlg.getReferenceTier(), svmg.getOffset(), svmg.getScale());
//		scroll_window = getWindow(scrollbar);
        // Add listeners
        addListeners();

        // Set scroll window properties
//		resizeWindow();
//		repositionWindow();
//		scroll_window.setVisible(true);
    }

    private void scroll(int i) {
        svmg.setOffset(i);
        map.updateWidget(true);
    }

    private void zoom(float scale) {
        svmg.setScale(scale);

		// Update scrollbar
//		scrollbar.removeAdjustmentListener(listener);
//		scrollbar.setMaximum((int)(scrollbar.getMaximum() * scale));
//		scrollbar.setMinimum((int)(scrollbar.getMinimum() * scale));
//		scrollbar.repaint();
//		scrollbar.addAdjustmentListener(listener);
        map.updateWidget(true);
    }

    private void stopscroll() {
        // Remove Listeners
        removeListners(true);

		// Dispose window
//		scroll_window.dispose();
        // Helps with garbage collection
//		scrollbar = null;
        tierlabel = null;
        svmg = null;
    }

    private void bringToFront() {
        scroll_window.toFront();
    }

    private void repositionWindow() {
        scroll_window.setLocation(map.getLabelMap().getLocationOnScreen().x + Math.min(0, tierlabel.getPixelBox(map.getLabelMap().getView()).x) + WINDOW_OFFSET,
                map.getLabelMap().getLocationOnScreen().y + Math.max(0, tierlabel.getPixelBox(map.getLabelMap().getView()).y + WINDOW_OFFSET));
    }

    private void resizeWindow() {
        int height = Math.min(map.getLabelMap().getView().getPixelBox().height, tierlabel.getPixelBox(map.getLabelMap().getView()).height - WINDOW_OFFSET * 2);

        scroll_window.setPreferredSize(new Dimension(SCROLLBAR_WIDTH, height));
        scroll_window.pack();
    }

    private void addListeners() {
        // Store value for restoring later
        zoom_pos = map.getZoomer(AffyLabelledTierMap.Y).getValue();

        // Flush previous listeners
        removeListners(false);

        map.getLabelMap().getNeoCanvas().addMouseWheelListener(listener);
        map.addListSelectionListener(listener);

        // Remove actual zoom listener
        map.getZoomer(AffyLabelledTierMap.Y).removeAdjustmentListener(map);
        map.getZoomer(AffyLabelledTierMap.Y).removeAdjustmentListener(map.getLabelMap());
        // Add this as zoom listener
        map.getZoomer(AffyLabelledTierMap.Y).addAdjustmentListener(listener);

//		scrollbar.addAdjustmentListener(listener);
//		Application.getSingleton().getFrame().addComponentListener(listener);
        // Set value for the glyph
        map.getZoomer(AffyLabelledTierMap.Y).setValue((int) zoomtrans.inverseTransform(AffyLabelledTierMap.Y, svmg.getScale()));
    }

    private void removeListners(boolean add) {
        map.getLabelMap().getNeoCanvas().removeMouseWheelListener(listener);
        map.removeListSelectionListener(listener);

        // Remove this zoom listener
        map.getZoomer(AffyLabelledTierMap.Y).removeAdjustmentListener(listener);
        // add map zoom listener
        if (add) {
            map.getZoomer(AffyLabelledTierMap.Y).addAdjustmentListener(map);
            map.getZoomer(AffyLabelledTierMap.Y).addAdjustmentListener(map.getLabelMap());
        }

//		scrollbar.removeAdjustmentListener(listener);
//		Application.getSingleton().getFrame().removeComponentListener(listener);
        // Restore value
        map.getZoomer(AffyLabelledTierMap.Y).setValue(zoom_pos);
    }

    private static JWindow getWindow(JComponent... components) {
        JWindow window = new JWindow(Application.getSingleton().getFrame());
        for (int i = 0; i < components.length; i++) {
            window.add(components[i]);
        }

        return window;
    }

    private static JScrollBar getScrollBar(TierGlyph tier, int sb_curr, float scale) {
        int style_height = (int) tier.getChildHeight() * tier.getActualSlots() + 75;
        if (tier.getDirection() != TierGlyph.Direction.REVERSE) {
            style_height *= -1;
        }

        style_height *= scale;
        int sb_min = Math.min(1, style_height);
        int sb_max = Math.max(1, style_height);
        sb_max = Math.max(sb_curr, sb_max);

        return new JScrollBar(JScrollBar.VERTICAL, sb_curr, 0, sb_min, sb_max) {

            @Override
            public void addAdjustmentListener(AdjustmentListener listener) {
                super.addAdjustmentListener(listener);
            }
        };
    }

    private class Listeners implements MouseWheelListener,
            ListSelectionListener, AdjustmentListener, ComponentListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
//			scrollbar.setValue(svmg.getOffset() + (e.getWheelRotation() * e.getScrollAmount() * SPEED));
            scroll(svmg.getOffset() + (e.getWheelRotation() * e.getScrollAmount() * SPEED));
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            if (e.getSource() == map.getZoomer(AffyLabelledTierMap.Y)) {
                zoom((float) zoomtrans.transform(AffyLabelledTierMap.Y, e.getValue()));
            } else {
                scroll(e.getValue());
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            stopscroll();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            repositionWindow();
        }

        public void componentResized(ComponentEvent e) {
            resizeWindow();
            repositionWindow();
        }

        public void componentShown(ComponentEvent e) {
            bringToFront();
        }

        public void componentHidden(ComponentEvent e) {
        }
    }

}
