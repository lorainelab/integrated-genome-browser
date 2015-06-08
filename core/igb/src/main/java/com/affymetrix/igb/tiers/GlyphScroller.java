package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.ExponentialTransform;
import com.affymetrix.igb.IGB;
import com.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import com.lorainelab.igb.services.search.ITransformableTierGlyph;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JWindow;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author hiralv
 */
public class GlyphScroller {

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SLIDER_WIDTH = 16;
    private static final int WINDOW_OFFSET = 4;
    private static final int SPEED = 10;

    private static JWindow getWindow(JComponent... components) {
        JWindow window = new JWindow();
        for (JComponent component : components) {
            window.add(component);
        }

        return window;
    }

    private static JScrollBar getScrollBar(TierGlyph tier, int sb_curr, float scale) {
        int style_height = (int) tier.getChildHeight() * tier.getActualSlots() + 75;
        if (tier.getDirection() != StyledGlyph.Direction.REVERSE) {
            style_height *= -1;
        }

        style_height *= scale;
        int sb_min = Math.min(1, style_height);
        int sb_max = Math.max(1, style_height);
        sb_max = Math.max(sb_curr, sb_max);

        return new JScrollBar(JScrollBar.VERTICAL, sb_curr, 0, sb_min, sb_max);
    }

    private static JSlider getSlider(TierGlyph tier, int sc_curr) {
        JSlider slider = new JSlider(JSlider.VERTICAL, 1, 100, sc_curr);
        slider.setInverted(true);
        return slider;
    }

    JWindow scroll_window, zoom_window;
    JScrollBar scrollbar;
    JSlider zoomer;
    Listeners listener;
    ExponentialTransform zoomtrans;

    AffyLabelledTierMap map;
    TierLabelGlyph tierlabel;
    ITransformableTierGlyph svmg;

    public GlyphScroller(AffyLabelledTierMap map) {
        this.map = map;
    }

    public void startscroll(TierLabelGlyph tlg) {
        if (!(tlg.getReferenceTier() instanceof ITransformableTierGlyph)
                || !((ITransformableTierGlyph) tlg.getReferenceTier()).isScrollingAllowed()) {
            return;
        }

        // Initialize all variables
        zoomtrans = new ExponentialTransform(map.getMinZoom(AffyLabelledTierMap.Y),
                map.getMaxZoom(AffyLabelledTierMap.Y), 1, 100);
        tierlabel = tlg;
        svmg = (ITransformableTierGlyph) tlg.getReferenceTier();
        zoomer = getSlider(tlg.getReferenceTier(), (int) zoomtrans.inverseTransform(AffyLabelledTierMap.Y, svmg.getScale()));
        zoom_window = getWindow(zoomer);
        scrollbar = getScrollBar(tlg.getReferenceTier(), svmg.getOffset(), svmg.getScale());
        scroll_window = getWindow(scrollbar);

        // Add listeners
        listener = new Listeners();
        addListeners();

        // Set scroll window properties
        resizeWindow();
        repositionWindow();
        zoom_window.setVisible(true);
        scroll_window.setVisible(true);
    }

    private void scroll(int i) {
        svmg.setOffset(i);
        map.updateWidget(true);
    }

    private void zoom(float scale) {
        svmg.setScale(scale);

        // Update scrollbar
        scrollbar.removeAdjustmentListener(listener);
        scrollbar.setMaximum((int) (scrollbar.getMaximum() * scale));
        scrollbar.setMinimum((int) (scrollbar.getMinimum() * scale));
        scrollbar.repaint();
        scrollbar.addAdjustmentListener(listener);

        map.updateWidget(true);
    }

    private void stopscroll() {
        // Remove Listeners
        removeListners();

        // Dispose window
        zoom_window.dispose();
        scroll_window.dispose();

        // Helps with garbage collection
        zoomer = null;
        scrollbar = null;
        tierlabel = null;
        svmg = null;
        map = null;
        listener = null;
    }

    private void bringToFront() {
        zoom_window.toFront();
        scroll_window.toFront();
    }

    private void repositionWindow() {
        zoom_window.setLocation(map.getLabelMap().getLocationOnScreen().x + Math.min(0, tierlabel.getPixelBox(map.getLabelMap().getView()).x) + WINDOW_OFFSET,
                map.getLabelMap().getLocationOnScreen().y + Math.max(0, tierlabel.getPixelBox(map.getLabelMap().getView()).y + WINDOW_OFFSET));

        scroll_window.setLocation(
                map.getLabelMap().getLocationOnScreen().x
                + Math.min(0, tierlabel.getPixelBox(map.getLabelMap().getView()).x) - WINDOW_OFFSET
                + tierlabel.getPixelBox(map.getLabelMap().getView()).width - SCROLLBAR_WIDTH,
                map.getLabelMap().getLocationOnScreen().y + Math.max(0, tierlabel.getPixelBox(map.getLabelMap().getView()).y + WINDOW_OFFSET));
    }

    private void resizeWindow() {
        int height = Math.min(map.getLabelMap().getView().getPixelBox().height, tierlabel.getPixelBox(map.getLabelMap().getView()).height - WINDOW_OFFSET * 2);

        zoom_window.setPreferredSize(new Dimension(SLIDER_WIDTH, height));
        zoom_window.pack();

        scroll_window.setPreferredSize(new Dimension(SCROLLBAR_WIDTH, height));
        scroll_window.pack();
    }

    private void addListeners() {
        map.getLabelMap().getNeoCanvas().addMouseWheelListener(listener);
        map.addListSelectionListener(listener);
        zoomer.addChangeListener(listener);
        scrollbar.addAdjustmentListener(listener);
        IGB.getInstance().getFrame().addComponentListener(listener);
    }

    private void removeListners() {
        map.getLabelMap().getNeoCanvas().removeMouseWheelListener(listener);
        map.removeListSelectionListener(listener);
        zoomer.removeChangeListener(listener);
        scrollbar.removeAdjustmentListener(listener);
        IGB.getInstance().getFrame().removeComponentListener(listener);
    }

    private class Listeners implements MouseWheelListener,
            ListSelectionListener, AdjustmentListener, ComponentListener, ChangeListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            scrollbar.setValue(svmg.getOffset() + (e.getWheelRotation() * e.getScrollAmount() * SPEED));
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            scroll(e.getValue());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            stopscroll();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            zoom((float) zoomtrans.transform(AffyLabelledTierMap.Y, zoomer.getValue()));
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
