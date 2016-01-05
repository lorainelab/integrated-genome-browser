/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package org.lorainelab.igb.genoviz.extensions;

import com.affymetrix.genoviz.swing.NumericFilter;
import com.affymetrix.genoviz.swing.RangeSlider;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import org.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;

public class GraphVisibleBoundsSetter extends JPanel
        implements ChangeListener, ActionListener, FocusListener {

    private static final long serialVersionUID = 1L;
    private static ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private final static DecimalFormat val_format;
    private final static DecimalFormat per_format;
    private static final int max_chars = 8;
    private static final int max_pix_per_char = 6;
    private static final int tf_min_xpix = max_chars * max_pix_per_char;
    private static final int tf_max_xpix = tf_min_xpix + (2 * max_pix_per_char);
    private static final int tf_min_ypix = 10;
    private static final int tf_max_ypix = 25;
    /*
     * Now trying to map slider values to percentages, such that each slider
     * unit = 0.1 percent (or in other words slider units per percent = 10)
     */
    private static final float sliders_per_percent = 10.0f;
    private static final float abs_min_percent = 0.0f;
    private static final float abs_max_percent = 100.0f;
    private static final int total_val_sliders = 1000;
    private static final float per_offset = 0.1f;
    private static final float val_offset = 0.1f;
    private static final boolean show_min_and_max = false;

    static {
        val_format = new DecimalFormat();
        val_format.setMinimumIntegerDigits(1);
        val_format.setMinimumFractionDigits(0);
        val_format.setMaximumFractionDigits(1);
        val_format.setGroupingUsed(false);
        per_format = new DecimalFormat();
        per_format.setMinimumIntegerDigits(1);
        per_format.setMinimumFractionDigits(0);
        per_format.setMaximumFractionDigits(1);
        per_format.setPositiveSuffix("%");
        per_format.setGroupingUsed(false);
    }

    static GraphVisibleBoundsSetter showFramedThresholder(GraphGlyph sgg, NeoAbstractWidget widg) {
        GraphVisibleBoundsSetter thresher = new GraphVisibleBoundsSetter(widg);
        List<GraphGlyph> glist = new ArrayList<>();
        glist.add(sgg);
        thresher.setGraphs(glist);
        JFrame frm = new JFrame(BUNDLE.getString("graphPercentileAdjuster"));
        Container cpane = frm.getContentPane();
        cpane.setLayout(new BorderLayout());
        cpane.add("Center", thresher);
        frm.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                Window w = evt.getWindow();
                w.setVisible(false);
                w.dispose();
            }
        });

        frm.pack();
        frm.setVisible(true);
        return thresher;
    }
    private NeoAbstractWidget widg;
    public RangeSlider PercentSlider = new RangeSlider(0, 100);
    public RangeSlider ValueSlider = new RangeSlider(0, 100);
    public JTextField min_perT;
    public JTextField max_perT;
    public JTextField min_valT;
    public JTextField max_valT;
    public final JRadioButton by_valRB = new JRadioButton(BUNDLE.getString("byValue"));
    public final JRadioButton by_percentileRB = new JRadioButton(BUNDLE.getString("byPercentile"));
    // info2pscores is a hash of GraphGlyphs' data model
    //   (usually a GraphSym if using genometry) to float[] arrays, each of length
    //   (sliders_per_percent * total_percent), and each value v at index i is
    //   value at which (i * sliders_per_percent) percent of the y values in the graph
    //   are below v
    // assuming abs_min_percent = 0, abs_max_percent = 100, so total_percent = 100
    // Using glyph's data model instead of glyph itself because AbstractGraphGlyph may get
    //    recreated from data model, but still want new AbstractGraphGlyph to hash to same
    //    cached percent-to-score array
    //TODO:
    // WARNING!  this caching currently causes a persistent reference to
    //    a data model (usually a GraphSym) for _every_ graph that is ever
    //    selected.  For times when many graphs are looked at and discarded, this
    //    will quickly eat up memory that could otherwise be freed.  NEED TO
    //    FIX THIS!  But also need to balance between memory concerns and the
    //    desire to avoid recalculation of percent-to-score array (which requires a
    //    sort) every time a graph is selected...
    private final List<GraphGlyph> graphs = new ArrayList<>();

    private float prev_min_per = 0;
    private float prev_max_per = 100;
    private float sliders_per_val; // slider units per yval unit
    private float prev_min_val;
    private float prev_max_val;
    private boolean includePercentileControls = true;
    public ButtonGroup by_val_group = new ButtonGroup();

    public GraphVisibleBoundsSetter(NeoAbstractWidget w) {
        this(w, true);
    }

    private GraphVisibleBoundsSetter(NeoAbstractWidget w, boolean includePercentileControls) {
        super();
        this.includePercentileControls = includePercentileControls;

        widg = w;

        min_valT = new JTextField(max_chars);
        max_valT = new JTextField(max_chars);
        min_perT = new JTextField(max_chars);
        max_perT = new JTextField(max_chars);

        ((AbstractDocument) min_valT.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());
        ((AbstractDocument) max_valT.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());
        ((AbstractDocument) min_perT.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());
        ((AbstractDocument) max_perT.getDocument()).setDocumentFilter(new NumericFilter.FloatNumericFilter());

        min_perT.setText(per_format.format(prev_min_per));
        max_perT.setText(per_format.format(prev_max_per));
        PercentSlider = new RangeSlider((int) (abs_min_percent * sliders_per_percent),
                (int) (abs_max_percent * sliders_per_percent));
        PercentSlider.setValue((int) (prev_min_per * sliders_per_percent));
        PercentSlider.setHighValue((int) (prev_max_per * sliders_per_percent));

        min_valT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
        max_valT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
        min_perT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
        max_perT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
        min_valT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
        max_valT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
        min_perT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
        max_perT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));

        by_val_group.add(by_valRB);
        by_val_group.add(by_percentileRB);
        by_valRB.setSelected(true);
        by_percentileRB.setSelected(false);

        turnOnListening();
    }

    /**
     * Set the set of graphs to the given List of AbstractGraphGlyph objects.
     */
    public void setGraphs(List<GraphGlyph> newgraphs) {
        turnOffListening();
        graphs.clear();
        if (newgraphs != null) {
            int gcount = newgraphs.size();
            for (GraphGlyph gl : newgraphs) {
                graphs.add(gl);
            }
        }
        if (includePercentileControls) {
            initPercents();
        }
        initValues();
        setEnabled(!graphs.isEmpty());
        turnOnListening();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        min_valT.setEnabled(b);
        max_valT.setEnabled(b);
        min_perT.setEnabled(b);
        max_perT.setEnabled(b);
        PercentSlider.setEnabled(b);
        ValueSlider.setEnabled(b);
    }

    private void initValues() {
        float min_of_mins = Float.POSITIVE_INFINITY;
        float max_of_maxes = Float.NEGATIVE_INFINITY;
        float avg_of_mins = 0;
        float avg_of_maxes = 0;

        float min_of_vismins = Float.POSITIVE_INFINITY;
        float max_of_vismins = Float.NEGATIVE_INFINITY;
        float min_of_vismaxes = Float.POSITIVE_INFINITY;
        float max_of_vismaxes = Float.NEGATIVE_INFINITY;
        float avg_of_vismins = 0;
        float avg_of_vismaxes = 0;
        int gcount = graphs.size();

        if (gcount == 0) {
            min_of_mins = 0;
            max_of_maxes = 0;
            avg_of_mins = avg_of_maxes = 0;
            min_of_vismins = max_of_vismins = 0;
            min_of_vismaxes = max_of_vismaxes = 0;
            avg_of_vismins = avg_of_vismaxes = 0;

        } else {

            for (GraphGlyph gl : graphs) {
                float min = gl.getGraphMinY();
                float max = gl.getGraphMaxY();
                float vismin = gl.getVisibleMinY();
                float vismax = gl.getVisibleMaxY();

                min_of_mins = Math.min(min_of_mins, min);
                max_of_maxes = Math.max(max_of_maxes, max);

                min_of_vismins = Math.min(min_of_vismins, vismin);
                max_of_vismins = Math.max(max_of_vismins, vismin);
                max_of_vismaxes = Math.max(max_of_vismaxes, vismax);
                min_of_vismaxes = Math.min(min_of_vismaxes, vismax);

                avg_of_mins += min;
                avg_of_maxes += max;
                avg_of_vismins += vismin;
                avg_of_vismaxes += vismax;
            }

            avg_of_mins /= gcount;
            avg_of_maxes /= gcount;
            avg_of_vismins /= gcount;
            avg_of_vismaxes /= gcount;
        }

        sliders_per_val = (total_val_sliders) / (max_of_maxes - min_of_mins);

        if (min_of_vismins == max_of_vismins) {
            min_valT.setText(val_format.format(min_of_vismins));
        } else {
            if (show_min_and_max) {
                min_valT.setText(val_format.format(min_of_vismins)
                        + " : " + val_format.format(max_of_vismins));
            } else {
                min_valT.setText("");
            }
        }
        if (min_of_vismaxes == max_of_vismaxes) {
            max_valT.setText(val_format.format(max_of_vismaxes));
        } else {
            if (show_min_and_max) {
                max_valT.setText(val_format.format(min_of_vismaxes)
                        + " : " + val_format.format(max_of_vismaxes));
            } else {
                max_valT.setText("");
            }
        }

        ValueSlider.setMinimum((int) (min_of_mins * sliders_per_val));
        ValueSlider.setMaximum((int) (max_of_maxes * sliders_per_val));
        ValueSlider.setValue((int) (avg_of_vismins * sliders_per_val));
        ValueSlider.setHighValue((int) (avg_of_vismaxes * sliders_per_val));

        prev_min_val = avg_of_vismins;
        prev_max_val = avg_of_vismaxes;
    }

    // assumes listening has already been turned off
    private void initPercents() {
        float min_of_vismins = Float.POSITIVE_INFINITY;
        float max_of_vismins = Float.NEGATIVE_INFINITY;
        float min_of_vismaxes = Float.POSITIVE_INFINITY;
        float max_of_vismaxes = Float.NEGATIVE_INFINITY;
        float avg_of_vismins = 0;
        float avg_of_vismaxes = 0;

        int gcount = graphs.size();
        if (gcount == 0) {
            min_of_vismins = max_of_vismins = 0;
            min_of_vismaxes = max_of_vismaxes = 0;
            avg_of_vismins = avg_of_vismaxes = 0;
        } else {
            for (GraphGlyph gl : graphs) {
                float vismin_val = gl.getVisibleMinY();
                float vismax_val = gl.getVisibleMaxY();
                float vismin_per = GraphGlyphUtils.getPercentForValue(gl, vismin_val);
                float vismax_per = GraphGlyphUtils.getPercentForValue(gl, vismax_val);

                min_of_vismins = Math.min(min_of_vismins, vismin_per);
                max_of_vismins = Math.max(max_of_vismins, vismin_per);
                max_of_vismaxes = Math.max(max_of_vismaxes, vismax_per);
                min_of_vismaxes = Math.min(min_of_vismaxes, vismax_per);

                avg_of_vismins += vismin_per;
                avg_of_vismaxes += vismax_per;
            }
        }

        avg_of_vismins /= gcount;
        avg_of_vismaxes /= gcount;

        if (min_of_vismins == max_of_vismins) {
            min_perT.setText(per_format.format(min_of_vismins));
        } else {
            if (show_min_and_max) {
                min_perT.setText(per_format.format(min_of_vismins) + " : " + per_format.format(max_of_vismins));
            } else {
                min_perT.setText("");
            }
        }
        if (min_of_vismaxes == max_of_vismaxes) {
            max_perT.setText(per_format.format(max_of_vismaxes));
        } else {
            if (show_min_and_max) {
                max_perT.setText(per_format.format(min_of_vismaxes) + " : " + per_format.format(max_of_vismaxes));
            } else {
                max_perT.setText("");
            }
        }

        PercentSlider.setValue((int) (avg_of_vismins * sliders_per_percent));
        PercentSlider.setHighValue((int) (avg_of_vismaxes * sliders_per_percent));

        prev_min_per = avg_of_vismins;
        prev_max_per = avg_of_vismaxes;
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        if (graphs.size() <= 0) {
            return;
        }
        Object src = evt.getSource();

        if (src == PercentSlider) {
            setVisibleMinPercent(PercentSlider.getValue() / sliders_per_percent);
            setVisibleMaxPercent(PercentSlider.getHighValue() / sliders_per_percent);
        } else if (src == ValueSlider) {
            setVisibleMaxValue(ValueSlider.getHighValue() / sliders_per_val);
            setVisibleMinValue(ValueSlider.getValue() / sliders_per_val);

        }
    }

    /**
     * When a JTextField gains focus, do nothing special.
     */
    @Override
    public void focusGained(FocusEvent e) {
    }

    /**
     * When a JTextField loses focus, process its value.
     */
    @Override
    public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        if (src instanceof JTextField) {
            doAction(src);
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        doAction(evt.getSource());
    }

    private void doAction(Object src) {
        if (graphs.size() <= 0) {
            return;
        }

        NumberFormat numberParser = NumberFormat.getNumberInstance();
        if (src == min_valT) {
            try {
                float minval = numberParser.parse(min_valT.getText()).floatValue();
                if (minval > prev_max_val - val_offset) {
                    minval = prev_max_val - val_offset;
                }
                // do not enforce an absolute minimum in the text field.
                // let the user enter any value to get the desired scaling.
                // this flexibility was requested on SourceForge.
                //else if (minval < abs_min_val) { minval = abs_min_val; }
                setVisibleMinValue(minval);
            } catch (ParseException ex) {
                min_valT.setText(val_format.format(prev_min_val));
            }
        } else if (src == max_valT) {
            try {
                float maxval = numberParser.parse(max_valT.getText()).floatValue();
                if (maxval < prev_min_val + val_offset) {
                    maxval = prev_min_val + val_offset;
                }
                // do not enforce an absolute maximum in the text field.
                // let the user enter any value to get the desired scaling
                //else if (maxval > abs_max_val) { maxval = abs_max_val; }
                setVisibleMaxValue(maxval);
            } catch (ParseException ex) {
                max_valT.setText(val_format.format(prev_max_val));
            }
        } else if (src == min_perT) {
            try {
                String text = min_perT.getText();
                if (text.endsWith("%")) {
                    text = text.substring(0, text.length() - 1);
                }
                float min_per = numberParser.parse(text).floatValue();
                if (min_per < 0) {
                    min_per = 0;
                } else if (min_per > prev_max_per - per_offset) {
                    min_per = prev_max_per - per_offset;
                }
                setVisibleMinPercent(min_per);  // resets min_perT text also
            } catch (ParseException ex) {
                min_perT.setText(per_format.format(prev_min_per));
            }
        } else if (src == max_perT) {
            try {
                String text = max_perT.getText();
                if (text.endsWith("%")) {
                    text = text.substring(0, text.length() - 1);
                }
                float max_per = numberParser.parse(text).floatValue();
                if (max_per < prev_min_per + per_offset) {
                    max_per = prev_min_per + per_offset;
                } else if (max_per > 100) {
                    max_per = 100;
                }
                setVisibleMaxPercent(max_per);  // resets max_perT text also
            } catch (ParseException ex) {
                max_perT.setText(per_format.format(prev_max_per));
            }
        }
    }

    private void setVisibleMinValue(float val) {
        int gcount = graphs.size();
        if (gcount > 0 /*
                 * && (val != prev_min_val)
                 */) {
            turnOffListening();

            float min_of_mins = Float.POSITIVE_INFINITY;
            float max_of_mins = Float.NEGATIVE_INFINITY;
            float avg_of_mins = 0;
            // set values
            for (GraphGlyph gl : graphs) {
                float min_per = GraphGlyphUtils.getPercentForValue(gl, val);
                min_of_mins = Math.min(min_per, min_of_mins);
                max_of_mins = Math.max(min_per, max_of_mins);
                avg_of_mins += min_per;
                gl.setVisibleMinY(val);
            }
            avg_of_mins /= gcount;
            if (widg != null) {
                widg.updateWidget();
            }

            // set values
            min_valT.setText(val_format.format(val));
            ValueSlider.setValue((int) (val * sliders_per_val));

            // then set percentages
            if (min_of_mins == max_of_mins) {
                min_perT.setText(per_format.format(min_of_mins));
            } else {
                if (show_min_and_max) {
                    min_perT.setText(per_format.format(min_of_mins) + " : " + per_format.format(max_of_mins));
                } else {
                    min_perT.setText("");
                }
            }
            PercentSlider.setValue((int) (avg_of_mins * sliders_per_percent));

            prev_min_val = val;
            //      prev_min_per = avg_of_mins; ???
            turnOnListening();
        }
    }

    private void setVisibleMaxValue(float val) {
        int gcount = graphs.size();
        if (gcount > 0 /*
                 * && (val != prev_max_val)
                 */) {
            turnOffListening();

            float min_of_maxes = Float.POSITIVE_INFINITY;
            float max_of_maxes = Float.NEGATIVE_INFINITY;
            float avg_of_maxes = 0;
            for (GraphGlyph gl : graphs) {
                float max_per = GraphGlyphUtils.getPercentForValue(gl, val);
                min_of_maxes = Math.min(max_per, min_of_maxes);
                max_of_maxes = Math.max(max_per, max_of_maxes);
                avg_of_maxes += max_per;
                gl.setVisibleMaxY(val);
            }
            avg_of_maxes /= gcount;
            if (widg != null) {
                widg.updateWidget();
            }

            max_valT.setText(val_format.format(val));
            ValueSlider.setHighValue((int) (val * sliders_per_val));

            if (min_of_maxes == max_of_maxes) {
                max_perT.setText(per_format.format(min_of_maxes));
            } else {
                if (show_min_and_max) {
                    max_perT.setText(per_format.format(min_of_maxes) + " : " + per_format.format(max_of_maxes));
                } else {
                    max_perT.setText("");
                }
            }
            PercentSlider.setHighValue((int) (avg_of_maxes * sliders_per_percent));

            prev_max_val = val;
            //      prev_max_per = avg_of_maxes; ???
            turnOnListening();
        }
    }

    /**
     * Set visible min Y to the specified percent value for all graphs under
     * control of GraphVisibleBoundsSetter, adjusts the controls, and updates
     * the widget.
     */
    private void setVisibleMinPercent(float percent) {
        //    System.out.println("setting min percent: " + percent + ", previous: " + prev_min_per);
        int gcount = graphs.size();
        if (gcount > 0 /*
                 * && (percent != prev_min_per)
                 */) {
            turnOffListening();

            if (percent > prev_max_per - per_offset) {
                percent = prev_max_per - per_offset;
            }
            if (percent < 0) {
                percent = 0;
            }

            float min_of_mins = Float.POSITIVE_INFINITY;
            float max_of_mins = Float.NEGATIVE_INFINITY;
            float avg_of_mins = 0;
            // set percentages
            for (GraphGlyph gl : graphs) {
                float min_val = GraphGlyphUtils.getValueForPercent(gl, percent);
                min_of_mins = Math.min(min_val, min_of_mins);
                max_of_mins = Math.max(min_val, max_of_mins);
                avg_of_mins += min_val;
                gl.setVisibleMinY(min_val);
            }
            avg_of_mins /= gcount;
            if (widg != null) {
                widg.updateWidget();
            }

            // set percents
            min_perT.setText(per_format.format(percent));
            PercentSlider.setValue((int) (percent * sliders_per_percent));

            // then set values
            if (min_of_mins == max_of_mins) {
                min_valT.setText(val_format.format(min_of_mins));
            } else {
                if (show_min_and_max) {
                    min_valT.setText(val_format.format(min_of_mins) + " : " + val_format.format(max_of_mins));
                } else {
                    min_valT.setText("");
                }
            }
            ValueSlider.setValue((int) (avg_of_mins * sliders_per_val));

            prev_min_per = percent;
            //      prev_min_val = avg_of_mins; ???
            turnOnListening();
        }
    }

    /**
     * Set visible max Y to the specified percent value for all graphs under
     * control of GraphVisibleBoundsSetter, adjusts the controls, and updates
     * the widget.
     */
    private void setVisibleMaxPercent(float percent) {
        int gcount = graphs.size();

        if (gcount > 0 /*
                 * && (percent != prev_max_per)
                 */) {
            turnOffListening();

            if (percent < prev_min_per + per_offset) {
                percent = prev_min_per + per_offset;
            }
            if (percent > 100) {
                percent = 100;
            }

            max_perT.setText(per_format.format(percent));
            PercentSlider.setHighValue((int) (percent * sliders_per_percent));

            float min_of_maxes = Float.POSITIVE_INFINITY;
            float max_of_maxes = Float.NEGATIVE_INFINITY;
            float avg_of_maxes = 0;
            for (GraphGlyph gl : graphs) {
                float max_val = GraphGlyphUtils.getValueForPercent(gl, percent);
                min_of_maxes = Math.min(max_val, min_of_maxes);
                max_of_maxes = Math.max(max_val, max_of_maxes);
                avg_of_maxes += max_val;
                gl.setVisibleMaxY(max_val);
            }
            avg_of_maxes /= gcount;
            if (widg != null) {
                widg.updateWidget();
            }

            if (min_of_maxes == max_of_maxes) {
                max_valT.setText(val_format.format(min_of_maxes));
            } else {
                if (show_min_and_max) {
                    max_valT.setText(val_format.format(min_of_maxes) + " : " + val_format.format(max_of_maxes));
                } else {
                    max_valT.setText("");
                }
            }
            //		max_val_slider.setValue((int) (avg_of_maxes * sliders_per_val));
            ValueSlider.setHighValue((int) (avg_of_maxes * sliders_per_val));

            prev_max_per = percent;

            turnOnListening();
        }
    }

    private void turnOffListening() {
        PercentSlider.removeChangeListener(this);
        ValueSlider.removeChangeListener(this);
        min_perT.removeActionListener(this);
        max_perT.removeActionListener(this);
        min_valT.removeActionListener(this);
        max_valT.removeActionListener(this);
        min_perT.removeFocusListener(this);
        max_perT.removeFocusListener(this);
        min_valT.removeFocusListener(this);
        max_valT.removeFocusListener(this);
    }

    private void turnOnListening() {
        PercentSlider.addChangeListener(this);
        ValueSlider.addChangeListener(this);
        min_perT.addActionListener(this);
        max_perT.addActionListener(this);
        min_valT.addActionListener(this);
        max_valT.addActionListener(this);
        min_perT.addFocusListener(this);
        max_perT.addFocusListener(this);
        min_valT.addFocusListener(this);
        max_valT.addFocusListener(this);
    }

}
