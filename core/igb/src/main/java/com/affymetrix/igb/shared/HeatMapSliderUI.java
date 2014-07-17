package com.affymetrix.igb.shared;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * UI delegate for the HeatMapSlider component. HeatMapSliderUI paints two
 * thumbs, one for the lower value and one for the upper value.
 */
class HeatMapSliderUI extends BasicSliderUI {

    /**
     * Location and size of thumb for upper value.
     */
    private Rectangle[] thumbsRect;
    /**
     * Indicator that determines whether upper thumb is selected.
     */
    private boolean[] thumbsSelected;
    /**
     * Indicator that determines whether upper thumb is being dragged.
     */
    private transient boolean[] thumbsDragging;
    /**
     * Indicator that determines whether upper thumb is being dragged.
     */
//	private transient boolean middleDragging;
    private boolean isMac = false;
    private static final int TRACK_HEIGHT = 5;
    //Color Preferences	
    private static final Color TRACK_BACKGROUND_COLOR = new Color(138, 130, 129, 100);
    private static final Color TRACK_BORDER_COLOR = new Color(255, 255, 255, 200);
    private static final Color TOP_SLIDER_KNOB_COLOR = Color.gray;
    private static final Color BOTTOM_SLIDER_KNOB_COLOR = Color.lightGray;
    public static final Color BORDER_COLOR = new Color(0xc5c8cf);

    /**
     * Constructs a HeatMapSliderUI for the specified slider component.
     *
     * @param b HeatMapSlider
     */
    public HeatMapSliderUI(HeatMapSlider b, int noOfThumbs) {
        super(b);
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            isMac = true;
        }
        thumbsRect = new Rectangle[noOfThumbs];
        thumbsSelected = new boolean[noOfThumbs];
        thumbsDragging = new boolean[noOfThumbs];
    }

    /**
     * Installs this UI delegate on the specified component.
     */
    @Override
    public void installUI(JComponent c) {
        for (int i = 0; i < thumbsRect.length; i++) {
            thumbsRect[i] = new Rectangle();
        }
        super.installUI(c);
    }

    /**
     * Creates a listener to handle track events in the specified slider.
     */
    @Override
    protected TrackListener createTrackListener(JSlider slider) {
        return new RangeTrackListener();
    }

    /**
     * Creates a listener to handle change events in the specified slider.
     */
    @Override
    protected ChangeListener createChangeListener(JSlider slider) {
        return new ChangeHandler();
    }

    /**
     * Updates the dimensions for both thumbs.
     */
    @Override
    protected void calculateThumbSize() {
        // Call superclass method for lower thumb size.
        super.calculateThumbSize();

        // Set upper thumb size.
        for (int i = 0; i < thumbsRect.length; i++) {
            thumbsRect[i].setSize(thumbRect.width, thumbRect.height);
        }
    }

    /**
     * Updates the locations for both thumbs.
     */
    @Override
    protected void calculateThumbLocation() {
        // Call superclass method for lower thumb location.
        super.calculateThumbLocation();

        for (int i = 0; i < thumbsRect.length; i++) {

            // Adjust upper value to snap to ticks if necessary.
            if (slider.getSnapToTicks()) {
                int upperValue = ((HeatMapSlider) slider).getValue(i);
                int snappedValue = upperValue;
                int majorTickSpacing = slider.getMajorTickSpacing();
                int minorTickSpacing = slider.getMinorTickSpacing();
                int tickSpacing = 0;

                if (minorTickSpacing > 0) {
                    tickSpacing = minorTickSpacing;
                } else if (majorTickSpacing > 0) {
                    tickSpacing = majorTickSpacing;
                }

                if (tickSpacing != 0) {
                    // If it's not on a tick, change the value
                    if ((upperValue - slider.getMinimum()) % tickSpacing != 0) {
                        float temp = (float) (upperValue - slider.getMinimum()) / (float) tickSpacing;
                        int whichTick = Math.round(temp);
                        snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
                    }

                    if (snappedValue != upperValue) {
                        ((HeatMapSlider) slider).setValue(i, snappedValue - ((HeatMapSlider) slider).getValue(i));
                    }
                }
            }

            // Calculate upper thumb location.  The thumb is centered over its 
            // value on the track.
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                int upperPosition = xPositionForValue(((HeatMapSlider) slider).getValue(i));
                // Set upper thumb size.
                thumbsRect[i].x = upperPosition - (thumbsRect[i].width / 2);
                thumbsRect[i].y = trackRect.y;
            } else {
                int upperPosition = yPositionForValue(((HeatMapSlider) slider).getValue(i));
                thumbsRect[i].x = trackRect.x;
                thumbsRect[i].y = upperPosition - (thumbsRect[i].height / 2);
            }
        }
    }

    private AlphaComposite makeComposite(float alpha) {
        int type = AlphaComposite.SRC_OVER;
        return (AlphaComposite.getInstance(type, alpha));
    }

    /**
     * Paints the thumb for the upper value using the specified graphics object.
     */
    private void paintUpperThumb(Graphics g, Rectangle knobBounds) {
        int w = knobBounds.width;
        int h = knobBounds.height;
        float alpha = 0.95f;
        // Create graphics copy.
        Graphics2D g2d = (Graphics2D) g.create();

        // Create default thumb shape.
        Shape thumbShape = createThumbShape(w - 1, h - 1);

        // Draw thumb.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(knobBounds.x, knobBounds.y);

        g2d.setColor(Color.white);
        g2d.draw(thumbShape);

        g2d.setColor(TOP_SLIDER_KNOB_COLOR);
        g2d.setComposite(makeComposite(alpha));
        g2d.fill(thumbShape);

        // Dispose graphics.
        g2d.dispose();
    }

    /**
     * Returns the size of a thumb.
     */
    @Override
    protected Dimension getThumbSize() {
        if (isMac) {
            return new Dimension(12, 12);
        } else {
            return super.getThumbSize();
        }
    }

    /**
     * Paints the slider. The selected thumb is always painted on top of the
     * other thumb.
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        recalculateIfInsetsChanged();
        recalculateIfOrientationChanged();
        Rectangle clip = g.getClipBounds();
        if (!clip.intersects(trackRect) && slider.getPaintTrack()) {
            calculateGeometry();
        }

        if (slider.getPaintTrack() && clip.intersects(trackRect)) {
            paintTrack(g);
        }
        if (slider.getPaintTicks() && clip.intersects(tickRect)) {
            paintTicks(g);
        }
        if (slider.getPaintLabels() && clip.intersects(labelRect)) {
            paintLabels(g);
        }
        if (slider.hasFocus() && clip.intersects(focusRect)) {
            // paintFocus( g );
        }

        for (int i = 0; i < thumbsRect.length; i++) {
            if (clip.intersects(thumbsRect[i])) {
                if (isMac) {
                    paintUpperThumb(g, thumbsRect[i]);
                } else {
                    paintThumb(g, thumbsRect[i]);
                }
            }
        }
    }

    /**
     * Paints the track.
     */
    @Override
    public void paintTrack(Graphics g) {
        // Draw track.
        //super.paintTrack(g);
        Graphics2D graphics2d = (Graphics2D) g;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double trackY = slider.getHeight() / 2.0 - TRACK_HEIGHT / 2.0;
        RoundRectangle2D track = new RoundRectangle2D.Double(
                0, trackY, slider.getWidth() - 1, TRACK_HEIGHT - 1, 4, 2);

        g.setColor(TRACK_BACKGROUND_COLOR);
        graphics2d.fill(track);
        graphics2d.setColor(TRACK_BORDER_COLOR);
        graphics2d.draw(track);
    }

    /**
     * Overrides superclass method to do nothing. Thumb painting is handled
     * within the <code>paint()</code> method.
     */
    @Override
    public void paintThumb(Graphics g) {
        // Do nothing.
    }

    /**
     * Paints the thumb for the lower value using the specified graphics object.
     */
    private void paintThumb(Graphics g, Rectangle rect) {
        Rectangle tempRect = thumbRect;
        thumbRect = rect;
        super.paintThumb(g);
        thumbRect = tempRect;
    }

    /**
     * Returns a Shape representing a thumb.
     */
    private Shape createThumbShape(int width, int height) {
        // Use circular shape.
        Ellipse2D shape = new Ellipse2D.Double(0, 0, width, height);
        return shape;
    }

    /**
     * Sets the location of the upper thumb, and repaints the slider. This is
     * called when the upper thumb is dragged to repaint the slider. The
     * <code>setThumbLocation()</code> method performs the same task for the
     * lower thumb.
     */
    private void setUpperThumbLocation(int no, int x, int y) {
        Rectangle upperUnionRect = new Rectangle();
        upperUnionRect.setBounds(thumbsRect[no]);

        thumbsRect[no].setLocation(x, y);

        SwingUtilities.computeUnion(thumbsRect[no].x, thumbsRect[no].y, thumbsRect[no].width, thumbsRect[no].height, upperUnionRect);
        slider.repaint(upperUnionRect.x, upperUnionRect.y, upperUnionRect.width, upperUnionRect.height);
    }

    /**
     * Moves the selected thumb in the specified direction by a block increment.
     * This method is called when the user presses the Page Up or Down keys.
     */
    @Override
    public void scrollByBlock(int direction) {
        synchronized (slider) {
            int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
            if (blockIncrement <= 0 && slider.getMaximum() > slider.getMinimum()) {
                blockIncrement = 1;
            }
            int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

            for (int i = 0; i < thumbsSelected.length; i++) {
                if (thumbsSelected[i]) {
                    int oldValue = ((HeatMapSlider) slider).getValue(i);
                    ((HeatMapSlider) slider).setValue(i, oldValue + delta);
                }
            }
        }
    }

    /**
     * Moves the selected thumb in the specified direction by a unit increment.
     * This method is called when the user presses one of the arrow keys.
     */
    @Override
    public void scrollByUnit(int direction) {
        synchronized (slider) {
            int delta = 1 * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
            for (int i = 0; i < thumbsSelected.length; i++) {
                if (thumbsSelected[i]) {
                    int oldValue = ((HeatMapSlider) slider).getValue(i);
                    ((HeatMapSlider) slider).setValue(i, oldValue + delta);
                }
            }
        }
    }

    /**
     * Listener to handle model change events. This calculates the thumb
     * locations and repaints the slider if the value change is not caused by
     * dragging a thumb.
     */
    public class ChangeHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent arg0) {
            boolean any_dragging = true;
            for (int i = 0; i < thumbsDragging.length; i++) {
                any_dragging &= thumbsDragging[i];
            }
            if (!any_dragging) {
                calculateThumbLocation();
                slider.repaint();
            }
        }
    }

    /**
     * Listener to handle mouse movements in the slider track.
     */
    public class RangeTrackListener extends TrackListener {

        @Override
        public void mousePressed(MouseEvent e) {
            if (!slider.isEnabled()) {
                return;
            }

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (slider.isRequestFocusEnabled()) {
                slider.requestFocus();
            }

            // Determine which thumb is pressed.  If the upper thumb is 
            // selected (last one dragged), then check its position first;
            // otherwise check the position of the lower thumb first.    
            for (int i = 0; i < thumbsRect.length; i++) {
                if (thumbsRect[i].contains(currentMouseX, currentMouseY)) {
                    switch (slider.getOrientation()) {
                        case JSlider.VERTICAL:
                            offset = currentMouseY - thumbsRect[i].y;
                            break;
                        case JSlider.HORIZONTAL:
                            offset = currentMouseX - thumbsRect[i].x;
                            break;
                    }
                    thumbsSelected[i] = true;
                    thumbsDragging[i] = true;
                    return;
                }
            }

            for (int i = 0; i < thumbsDragging.length; i++) {
                thumbsDragging[i] = false;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            for (int i = 0; i < thumbsDragging.length; i++) {
                thumbsDragging[i] = false;
            }
            slider.setValueIsAdjusting(false);
            super.mouseReleased(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!slider.isEnabled()) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            for (int i = 0; i < thumbsDragging.length; i++) {
                if (thumbsDragging[i]) {
                    slider.setValueIsAdjusting(true);
                    moveUpperThumb(i);
                }
            }
        }

        @Override
        public boolean shouldScroll(int direction) {
            return false;
        }

        /**
         * Moves the location of the upper thumb, and sets its corresponding
         * value in the slider.
         */
        private void moveUpperThumb(int no) {

            switch (slider.getOrientation()) {
                case JSlider.VERTICAL:
                    int halfThumbHeight = thumbRect.height / 2;
                    int thumbTop = currentMouseY - offset;
                    int thumbHeightMiddle = thumbTop + halfThumbHeight;
                    ;

                    ((HeatMapSlider) slider).setValue(no, valueForYPosition(thumbHeightMiddle));
                    setUpperThumbLocation(no, thumbRect.x, yPositionForValue(((HeatMapSlider) slider).getValue(no)));

                    break;

                case JSlider.HORIZONTAL:
                    int halfThumbWidth = thumbRect.width / 2;
                    int thumbLeft = currentMouseX - offset;
                    int thumbWidthMiddle = thumbLeft + halfThumbWidth;

                    ((HeatMapSlider) slider).setValue(no, valueForXPosition(thumbWidthMiddle));
                    setUpperThumbLocation(no, xPositionForValue(((HeatMapSlider) slider).getValue(no)), thumbRect.y);
                    break;

                default:
            }
        }
    }
}
