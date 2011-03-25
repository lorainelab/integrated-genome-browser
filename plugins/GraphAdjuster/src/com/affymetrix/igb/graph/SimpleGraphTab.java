/**
 *   Copyright (c) 2006 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.graph;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.ErrorHandler;

import com.affymetrix.genometryImpl.BioSeq;

import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.MisMatchGraphSym;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.style.DefaultTrackStyle;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.util.FloatTransformer;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.IdentityTransform;

import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GraphScoreThreshSetter;
import com.affymetrix.igb.glyph.GraphVisibleBoundsSetter;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.JComboBoxWithSingleListener;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

public final class SimpleGraphTab extends IGBTabPanel
				implements SeqSelectionListener, SymSelectionListener {
	private static final long serialVersionUID = 1L;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("graph");
	private static final int TAB_POSITION = 4;

	SeqMapView gviewer = null;
	BioSeq current_seq;
	GenometryModel gmodel;
	boolean is_listening = true; // used to turn on and off listening to GUI events
	GraphScoreThreshSetter score_thresh_adjuster;
	GraphVisibleBoundsSetter vis_bounds_setter;

	// Whether to use this tab or not
	public static boolean USE_SIMPLE_GRAPH_TAB = true;
	boolean DEBUG_EVENTS = false;
	JLabel selected_graphs_label = new JLabel(BUNDLE.getString("selectedGraphsLabel"));
	JRadioButton mmavgB = new JRadioButton(BUNDLE.getString("minMaxAvgButton"));
	JRadioButton lineB = new JRadioButton(BUNDLE.getString("lineButton"));
	JRadioButton barB = new JRadioButton(BUNDLE.getString("barButton"));
	JRadioButton dotB = new JRadioButton(BUNDLE.getString("dotButton"));
	JRadioButton sstepB = new JRadioButton(BUNDLE.getString("stairStepButton"));
	JRadioButton hmapB = new JRadioButton(BUNDLE.getString("heatMapButton"));
	JRadioButton hidden_styleB = new JRadioButton("No Selection"); // this button will not be displayed
	ButtonGroup stylegroup = new ButtonGroup();
	JButton colorB = new JButton("Color");
	JSlider height_slider = new JSlider(JSlider.HORIZONTAL, 10, 500, 50);

	private final	List<GraphSym> grafs = new ArrayList<GraphSym>();
	private final List<GraphGlyph> glyphs = new ArrayList<GraphGlyph>();

	private static final String select2graphs= "Select exactly two graphs";

	private final JButton cloneB = new JButton(BUNDLE.getString("goButton"));
	private final JLabel scale_type_label = new JLabel(BUNDLE.getString("transformationLabel"));
	private final JComboBox scaleCB = new JComboBoxWithSingleListener();
	private final JCheckBox labelCB = new JCheckBox(BUNDLE.getString("labelCheckBox"));
	private final JCheckBox yaxisCB = new JCheckBox(BUNDLE.getString("yAxisCheckBox"));
	private final JCheckBox floatCB = new JCheckBox(BUNDLE.getString("floatingCheckBox"));

	private final Action select_all_graphs_action = new AbstractAction(BUNDLE.getString("selectAllGraphs")) {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (gviewer != null) {
				gviewer.selectAllGraphs();
			}
		}
	};
	private final Action delete_selected_graphs_action = new AbstractAction(BUNDLE.getString("deleteSelectedGraphs")) {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			deleteGraphs(gmodel, gviewer, grafs);
		}
	};
	private final Action save_selected_graphs_action = new AbstractAction(BUNDLE.getString("saveSelectedGraphs") + "...") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			saveGraphs(gviewer, gmodel, grafs);
		}
	};
	private final Action graph_threshold_action = new AbstractAction(
			BUNDLE.getString("graphThresholding") + "...") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			showGraphScoreThreshSetter();
		}
	};
	private final JButton selectAllB = new JButton(select_all_graphs_action);
	private final JButton saveB = new JButton(save_selected_graphs_action);
	private final JButton deleteB = new JButton(delete_selected_graphs_action);
	private final JButton threshB = new JButton(graph_threshold_action);
	private final JLabel param_label = new JLabel();
	private final JTextField paramT = new JTextField("", 2);
	private final JButton combineB = new JButton(BUNDLE.getString("combineButton"));
	private final JButton splitB = new JButton(BUNDLE.getString("splitButton"));
	private JButton addB;
	private JButton subB;
	private JButton mulB;
	private JButton divB;
	private JComboBox heat_mapCB;
	private AdvancedGraphPanel advanced_panel;

	public SimpleGraphTab(IGBService igbService) {
		super(igbService, BUNDLE.getString("graphAdjusterTab"), BUNDLE.getString("graphAdjusterTab"), false, TAB_POSITION);
		advanced_panel = new SimpleGraphTab.AdvancedGraphPanel();
		this.gviewer = (SeqMapView)igbService.getMapView();

		heat_mapCB = new JComboBox(HeatMap.getStandardNames());
		heat_mapCB.addItemListener(new HeatMapItemListener());

		// A box to contain the heat-map JComboBox, to help get the alignment right
		Box heat_mapCB_box = Box.createHorizontalBox();
		heat_mapCB_box.add(Box.createHorizontalStrut(16));
		heat_mapCB_box.add(heat_mapCB);
		heat_mapCB_box.setMaximumSize(heat_mapCB_box.getPreferredSize());

		Box stylebox_radiobox = Box.createHorizontalBox();
		Box stylebox_radiobox_col1 = Box.createVerticalBox();
		Box stylebox_radiobox_col2 = Box.createVerticalBox();
		stylebox_radiobox_col1.add(barB);
		stylebox_radiobox_col1.add(Box.createRigidArea(new Dimension(0, 3)));
		stylebox_radiobox_col1.add(dotB);
		stylebox_radiobox_col1.add(Box.createRigidArea(new Dimension(0, 3)));
		stylebox_radiobox_col1.add(hmapB);
		stylebox_radiobox_col1.add(Box.createRigidArea(new Dimension(0, 3)));
		stylebox_radiobox_col2.add(lineB);
		stylebox_radiobox_col2.add(Box.createRigidArea(new Dimension(0, 3)));
		stylebox_radiobox_col2.add(mmavgB);
		stylebox_radiobox_col2.add(Box.createRigidArea(new Dimension(0, 3)));
		stylebox_radiobox_col2.add(sstepB);
		stylebox_radiobox_col2.add(Box.createRigidArea(new Dimension(0, 3)));
		stylebox_radiobox.add(stylebox_radiobox_col1);
		stylebox_radiobox.add(stylebox_radiobox_col2);

		Box color_button_box = Box.createHorizontalBox();
		color_button_box.add(Box.createRigidArea(new Dimension(16, 1)));
		color_button_box.add(colorB);

		Box stylebox = Box.createVerticalBox();
		color_button_box.setAlignmentX(0.0f);
		stylebox.add(color_button_box);
		stylebox.add(Box.createRigidArea(new Dimension(0, 9)));
		stylebox_radiobox.setAlignmentX(0.0f);
		stylebox.add(stylebox_radiobox);
		heat_mapCB_box.setAlignmentX(0.0f);
		stylebox.add(heat_mapCB_box);

		barB.addActionListener(new GraphStyleSetter(GraphType.BAR_GRAPH));
		dotB.addActionListener(new GraphStyleSetter(GraphType.DOT_GRAPH));
		hmapB.addActionListener(new GraphStyleSetter(GraphType.HEAT_MAP));
		lineB.addActionListener(new GraphStyleSetter(GraphType.LINE_GRAPH));
		mmavgB.addActionListener(new GraphStyleSetter(GraphType.MINMAXAVG));
		sstepB.addActionListener(new GraphStyleSetter(GraphType.STAIRSTEP_GRAPH));

		stylegroup.add(barB);
		stylegroup.add(dotB);
		stylegroup.add(hmapB);
		stylegroup.add(lineB);
		stylegroup.add(mmavgB);
		stylegroup.add(sstepB);
		stylegroup.add(hidden_styleB); // invisible button
		stylebox.setBorder(BorderFactory.createTitledBorder(BUNDLE.getString("stylePanel")));

		hidden_styleB.setSelected(true); // deselect all visible radio buttons

		vis_bounds_setter = new GraphVisibleBoundsSetter(gviewer.getSeqMap());
		score_thresh_adjuster = new GraphScoreThreshSetter(gviewer, vis_bounds_setter);

		height_slider.setBorder(BorderFactory.createTitledBorder(BUNDLE.getString("heightSlider")));

		Box scalebox = Box.createVerticalBox();
		vis_bounds_setter.setAlignmentX(0.0f);
		height_slider.setAlignmentX(0.0f);
		scalebox.add(height_slider);
		scalebox.add(vis_bounds_setter);
		
		height_slider.addChangeListener(new GraphHeightSetter());

		Box butbox = Box.createHorizontalBox();
		butbox.add(Box.createRigidArea(new Dimension(5, 5)));
		butbox.add(selectAllB);
		butbox.add(Box.createRigidArea(new Dimension(5, 5)));
		butbox.add(saveB);
		butbox.add(Box.createRigidArea(new Dimension(5, 5)));
		butbox.add(deleteB);
		butbox.add(Box.createRigidArea(new Dimension(5, 5)));
//		butbox.add(Box.createHorizontalGlue());
		butbox.add(threshB);
		butbox.add(Box.createRigidArea(new Dimension(5, 5)));

		Box first_two_columns = Box.createHorizontalBox();
		stylebox.setAlignmentY(0.0f);
		first_two_columns.add(stylebox);
		scalebox.setAlignmentY(0.0f);
		first_two_columns.add(scalebox);
		Box megabox = Box.createVerticalBox();
		butbox.setAlignmentX(0.0f);
		megabox.add(butbox);
		megabox.add(Box.createRigidArea(new Dimension(1, 5)));
		first_two_columns.setAlignmentX(0.0f);
		megabox.add(first_two_columns);

		Box label_box = Box.createHorizontalBox();
		label_box.add(selected_graphs_label);
		label_box.add(Box.createHorizontalGlue());

		Box row1 = Box.createHorizontalBox();

		megabox.setAlignmentY(0.0f);
		row1.add(megabox);
		advanced_panel.setAlignmentY(0.0f);
		row1.add(advanced_panel);

		colorB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				changeColor(grafs, gviewer);
			}
		});

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		this.add(Box.createRigidArea(new Dimension(1, 5)));
		row1.setAlignmentX(0.0f);
		this.add(row1);
		butbox.setAlignmentX(0.0f);
		butbox.setAlignmentY(1.0f);
		this.add(Box.createVerticalGlue());

		this.setBorder(BorderFactory.createEtchedBorder());

		resetSelectedGraphGlyphs(Collections.EMPTY_LIST);

		setSeqMapView(this.gviewer); // called for the side-effects

		gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(this);
		gmodel.addSymSelectionListener(this);
	}

	private void showGraphScoreThreshSetter() {
		score_thresh_adjuster.showFrame();
	}

	private void setSeqMapView(SeqMapView smv) {
		this.gviewer = smv;
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		List<SeqSymmetry> selected_syms = evt.getSelectedSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (src != gviewer && src != gviewer.getSeqMap()) {
			return;
		}

		resetSelectedGraphGlyphs(selected_syms);
	}

	private void resetSelectedGraphGlyphs(List<?> selected_syms) {
		int symcount = selected_syms.size();
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		collectGraphsAndGlyphs(selected_syms, symcount);

		int num_glyphs = glyphs.size();
		//    System.out.println("number of selected graphs: " + num_glyphs);
		double the_height = -1; // -1 indicates unknown height

		boolean all_are_floating = false;
		boolean all_show_axis = false;
		boolean all_show_label = false;
		boolean any_are_combined = false; // are any selections inside a combined tier
		boolean all_are_combined = false; // are all selections inside (a) combined tier(s)

		// Take the first glyph in the list as a prototype
		GraphGlyph first_glyph = null;
		GraphType graph_style = GraphType.LINE_GRAPH;
		HeatMap hm = null;
		if (!glyphs.isEmpty()) {
			first_glyph = glyphs.get(0);
			graph_style = first_glyph.getGraphStyle();
			if (graph_style == GraphType.HEAT_MAP) {
				hm = first_glyph.getHeatMap();
			}
			the_height = first_glyph.getGraphState().getTierStyle().getHeight();
			all_are_floating = first_glyph.getGraphState().getFloatGraph();
			all_show_axis = first_glyph.getGraphState().getShowAxis();
			all_show_label = first_glyph.getGraphState().getShowLabel();
			boolean this_one_is_combined = (first_glyph.getGraphState().getComboStyle() != null);
			any_are_combined = this_one_is_combined;
			all_are_combined = this_one_is_combined;
		}

		// Now loop through other glyphs if there are more than one
		// and see if the graph_style and heatmap are the same in all selections
		for (GraphGlyph gl : glyphs) {
			all_are_floating = all_are_floating && gl.getGraphState().getFloatGraph();
			all_show_axis = all_show_axis && gl.getGraphState().getShowAxis();
			all_show_label = all_show_label && gl.getGraphState().getShowLabel();
			boolean this_one_is_combined = (gl.getGraphState().getComboStyle() != null);
			any_are_combined = any_are_combined || this_one_is_combined;
			all_are_combined = all_are_combined && this_one_is_combined;

			if (first_glyph.getGraphStyle() != gl.getGraphStyle()) {
				graph_style = GraphType.LINE_GRAPH;
			}
			if (graph_style == GraphType.HEAT_MAP) {
				if (first_glyph.getHeatMap() != gl.getHeatMap()) {
					hm = null;
				}
			} else {
				hm = null;
			}
		}

		if (num_glyphs == 0) {
			selected_graphs_label.setText("No graphs selected");
		} else if (num_glyphs == 1) {
			GraphSym graf_0 = grafs.get(0);
			selected_graphs_label.setText(graf_0.getGraphName());
		} else {
			selected_graphs_label.setText(num_glyphs + " graphs selected");
		}
		
		selectButtonBasedOnGraphStyle(graph_style);

		if (graph_style == GraphType.HEAT_MAP) {
			heat_mapCB.setEnabled(true);
			if (hm == null) {
				heat_mapCB.setSelectedIndex(-1);
			} else {
				heat_mapCB.setSelectedItem(hm.getName());
			}
		} else {
			heat_mapCB.setEnabled(false);
		}

		if (the_height != -1) {
			height_slider.setValue((int) the_height);
		}
		vis_bounds_setter.setGraphs(glyphs);
		score_thresh_adjuster.setGraphs(glyphs);

		if (!glyphs.isEmpty()) {
			floatCB.setSelected(all_are_floating);
			yaxisCB.setSelected(all_show_axis);
			labelCB.setSelected(all_show_label);
		}

		boolean b = !(grafs.isEmpty());
		height_slider.setEnabled(b);
		graph_threshold_action.setEnabled(b);
		boolean type = b;
		for(GraphSym graf : grafs){
			type = !(graf instanceof MisMatchGraphSym);
			if(type){
				break;
			}
		}
		
		enableButtons(stylegroup, type);
		
		floatCB.setEnabled(b);
		yaxisCB.setEnabled(b);
		labelCB.setEnabled(b);

		colorB.setEnabled(b);
		save_selected_graphs_action.setEnabled(grafs.size() == 1);
		delete_selected_graphs_action.setEnabled(b);
		cloneB.setEnabled(b);
		scaleCB.setEnabled(cloneB.isEnabled());
		paramT.setEnabled(cloneB.isEnabled());

		combineB.setEnabled(!all_are_combined && grafs.size() >= 2);
		splitB.setEnabled(any_are_combined);
		addB.setEnabled(grafs.size() == 2);
		subB.setEnabled(grafs.size() == 2);
		mulB.setEnabled(grafs.size() == 2);
		divB.setEnabled(grafs.size() == 2);
		
		is_listening = true; // turn back on GUI events
	}

	private class HoverEffect implements MouseListener {
		private String A = null;
		private String B = null;

		public void mouseClicked(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {
			JButton comp = (JButton) e.getComponent();
			
			if(grafs.size() == 2){
				setGraphName(comp);
			}else{
				comp.setToolTipText(select2graphs);
			}
		}

		public void mouseExited(MouseEvent e) {
			unsetGraphName();
		}

		public void setGraphName(JButton comp) {
			A = grafs.get(0).getGraphName();
			B = grafs.get(1).getGraphName();

			grafs.get(0).setGraphName("A");
			grafs.get(1).setGraphName("B");

			comp.setToolTipText(null);
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					gviewer.getSeqMap().updateWidget();
				}
			});
		}

		public void unsetGraphName() {
			if (A != null && B != null && grafs.size() > 1) {
				grafs.get(0).setGraphName(A);
				grafs.get(1).setGraphName(B);

				ThreadUtils.runOnEventQueue(new Runnable() {

					public void run() {
						gviewer.getSeqMap().updateWidget();
					}
				});
				A = null;
				B = null;

			}
		}
	}

	private void collectGraphsAndGlyphs(List<?> selected_syms, int symcount) {
		if (grafs != selected_syms) {
			// in certain cases selected_syms arg and grafs list may be same, for example when method is being
			//     called to catch changes in glyphs representing selected sym, not the syms themselves)
			//     therefore don't want to change grafs list if same as selected_syms (especially don't want to clear it!)
			grafs.clear();
		}
		glyphs.clear();
		// First loop through and collect graphs and glyphs
		for (int i = 0; i < symcount; i++) {
			if (selected_syms.get(i) instanceof GraphSym) {
				GraphSym graf = (GraphSym) selected_syms.get(i);
				// only add to grafs if list is not identical to selected_syms arg
				if (grafs != selected_syms) {
					grafs.add(graf);
				}
				List<GraphGlyph> multigl = gviewer.getSeqMap().<GraphGlyph>getItems(graf);
				// add all graph glyphs representing graph sym
				//	  System.out.println("found multiple glyphs for graph sym: " + multigl.size());
				glyphs.addAll(multigl);
			}
		}
	}

	private void selectButtonBasedOnGraphStyle(GraphType graph_style) {
		switch (graph_style) {
			case MINMAXAVG:
				mmavgB.setSelected(true);
				break;
			case LINE_GRAPH:
				lineB.setSelected(true);
				break;
			case BAR_GRAPH:
				barB.setSelected(true);
				break;
			case DOT_GRAPH:
				dotB.setSelected(true);
				break;
			case HEAT_MAP:
				hmapB.setSelected(true);
				break;
			case STAIRSTEP_GRAPH:
				sstepB.setSelected(true);
				break;
			default:
				hidden_styleB.setSelected(true);
				break;
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (DEBUG_EVENTS) {
			System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " received by " + this.getClass().getName());
		}
		current_seq = evt.getSelectedSeq();
		resetSelectedGraphGlyphs(gmodel.getSelectedSymmetries(current_seq));
	}

	private static void enableButtons(ButtonGroup g, boolean b) {
		Enumeration<AbstractButton> e = g.getElements();
		while (e.hasMoreElements()) {
			e.nextElement().setEnabled(b);
		}
	}

	public AdvancedGraphPanel getAdvancedPanel() {
		return advanced_panel;
	}

	private final class GraphStyleSetter implements ActionListener {

		GraphType style = GraphType.LINE_GRAPH;

		public GraphStyleSetter(GraphType style) {
			this.style = style;
		}

		public void actionPerformed(ActionEvent event) {
			if (DEBUG_EVENTS) {
				System.out.println(this.getClass().getName() + " got an ActionEvent: " + event);
			}
			if (gviewer == null || glyphs.isEmpty() || !is_listening) {
				return;
			}

			Runnable r = new Runnable() {

				public void run() {
					GraphGlyph first_glyph = glyphs.get(0);
					if (style == GraphType.HEAT_MAP) {
						// set to heat map FIRST so that getHeatMap() below will return default map instead of null
						first_glyph.setGraphStyle(GraphType.HEAT_MAP);
					}
					HeatMap hm = (glyphs.get(0)).getHeatMap();
					for (GraphGlyph sggl : glyphs) {
						sggl.setShowGraph(true);
						sggl.setGraphStyle(style); // leave the heat map whatever it was
						if ((style == GraphType.HEAT_MAP) && (hm != sggl.getHeatMap())) {
							hm = null;
						}
					}
					if (style == GraphType.HEAT_MAP) {
						heat_mapCB.setEnabled(true);
						if (hm == null) {
							heat_mapCB.setSelectedIndex(-1);
						} else {
							heat_mapCB.setSelectedItem(hm.getName());
						}
					} else {
						heat_mapCB.setEnabled(false);
					// don't bother to change the displayed heat map name
					}
					gviewer.getSeqMap().updateWidget();
				}
			};

			SwingUtilities.invokeLater(r);
		}
	}

	private void updateViewer() {
		final SeqMapView current_viewer = gviewer;
		final List<GraphSym> previous_graph_syms = new ArrayList<GraphSym>(grafs);
		// set selections to empty so that options get turned off
		resetSelectedGraphGlyphs(Collections.EMPTY_LIST);
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				current_viewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
				resetSelectedGraphGlyphs(previous_graph_syms);
			}
		});
	}

	private final class HeatMapItemListener implements ItemListener {

		public void itemStateChanged(ItemEvent e) {
			if (gviewer == null || glyphs.isEmpty() || !is_listening) {
				return;
			}

			if (e.getStateChange() == ItemEvent.SELECTED) {
				String name = (String) e.getItem();
				HeatMap hm = HeatMap.getStandardHeatMap(name);

				if (hm != null) {
					for (GraphGlyph gl : glyphs) {
						gl.setShowGraph(true);
						gl.setGraphStyle(GraphType.HEAT_MAP);
						gl.setHeatMap(hm);
					}
					gviewer.getSeqMap().updateWidget();
				}
			}
		}
	}

	private final class GraphHeightSetter implements ChangeListener {

		public void stateChanged(ChangeEvent e) {
			if (gviewer == null || glyphs.isEmpty() || !is_listening) {
				return;
			}

			if (e.getSource() == height_slider) {
				setTheHeights((double) height_slider.getValue());
			}
		}

		private void setTheHeights(double height) {
			if (gviewer == null) {
				return; // for testing
			}

			AffyTieredMap map = gviewer.getSeqMap();

			for (GraphGlyph gl : glyphs) {
				Rectangle2D.Double cbox = gl.getCoordBox();
				gl.setCoords(cbox.x, cbox.y, cbox.width, height);

				// If a graph is joined with others in a combo tier, repack that tier.
				GlyphI parentgl = gl.getParent();
				if (parentgl instanceof TierGlyph) {
					//	  System.out.println("Glyph: " + gl.getLabel() + ", packer: " + parentgl.getPacker());
					parentgl.pack(map.getView());
				}
			}
			map.packTiers(false, true, false);
			map.stretchToFit(false, true);
			map.updateWidget();
		}
	}
	
	final class AdvancedGraphPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int PARAM_TEXT_WIDTH = 60;
		private Map<String, FloatTransformer> name2transform;
		private final HoverEffect hovereffect;

		public AdvancedGraphPanel() {
			name2transform = new HashMap<String, FloatTransformer>();
			JPanel advanced_panel = this;
			hovereffect = new HoverEffect();

			advanced_panel.setLayout(new BoxLayout(advanced_panel, BoxLayout.Y_AXIS));

			paramT.setText("");
			paramT.setVisible(false);
			Box grouping_box = Box.createHorizontalBox();
			grouping_box.add(Box.createRigidArea(new Dimension(6, 0)));
			grouping_box.add(combineB);
			grouping_box.add(Box.createRigidArea(new Dimension(5, 0)));
			grouping_box.add(splitB);
			grouping_box.add(Box.createRigidArea(new Dimension(5, 0)));

			Box param_box = Box.createHorizontalBox();
			param_box.setAlignmentX(0.0f);
			param_box.add(param_label);
			param_box.add(paramT);
			paramT.setMaximumSize(new Dimension((int)Math.round(paramT.getPreferredSize().getWidth()), PARAM_TEXT_WIDTH));
			grouping_box.add(param_box);
			grouping_box.add(Box.createRigidArea(new Dimension(5, 0)));
			
			Box decoration_row = Box.createHorizontalBox();

			decoration_row.add(Box.createRigidArea(new Dimension(6, 5)));
			decoration_row.add(labelCB);
			decoration_row.add(yaxisCB);
			decoration_row.add(floatCB);

			scaleCB.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String selection = (String) scaleCB.getSelectedItem();
					if (selection == null) {
						param_label.setText("");
						paramT.setVisible(false);
					}
					else {
						FloatTransformer trans = name2transform.get(selection);
						String paramPrompt = trans.getParamPrompt();
						if (paramPrompt == null) {
							param_label.setText("");
							paramT.setVisible(false);
						}
						else {
							param_label.setText(paramPrompt + " ");
							paramT.setVisible(true);
						}
					}
				}
			});
			// A box to contain the scaleCB JComboBox, to help get the alignment right
			Box scaleCB_box = Box.createHorizontalBox();
			scaleCB_box.setAlignmentX(0.0f);
			scaleCB_box.add(Box.createRigidArea(new Dimension(6, 5)));
			scaleCB_box.add(scaleCB);
			scaleCB_box.add(Box.createRigidArea(new Dimension(5, 5)));
			scaleCB_box.add(cloneB);
			scaleCB_box.add(Box.createRigidArea(new Dimension(20, 5))); // kludge to get width correct

			scaleCB.setMaximumSize(scaleCB_box.getPreferredSize()); // kludge to get width correct

			advanced_panel.setBorder(BorderFactory.createTitledBorder(BUNDLE.getString("advancedPanel")));

			decoration_row.setAlignmentX(0.0f);
			advanced_panel.add(decoration_row);
			advanced_panel.add(Box.createRigidArea(new Dimension(5, 6)));

			advanced_panel.add(scale_type_label);
			scaleCB_box.setAlignmentX(0.0f);

			advanced_panel.add(scaleCB_box);

			param_box.setAlignmentX(0.0f);
			advanced_panel.add(Box.createRigidArea(new Dimension(5, 6)));
			advanced_panel.add(param_box);

			grouping_box.setAlignmentX(0.0f);
			advanced_panel.add(Box.createRigidArea(new Dimension(5, 6)));
			advanced_panel.add(grouping_box);

			addB = new JButton("A + B");
			subB = new JButton("A - B");
			mulB = new JButton("A * B");
			divB = new JButton("A / B");

			addB.addMouseListener(hovereffect);
			subB.addMouseListener(hovereffect);
			mulB.addMouseListener(hovereffect);
			divB.addMouseListener(hovereffect);
			
			addB.setMargin(new Insets(2, 2, 2, 2));
			subB.setMargin(new Insets(2, 2, 2, 2));
			mulB.setMargin(new Insets(2, 2, 2, 2));
			divB.setMargin(new Insets(2, 2, 2, 2));
			Box math_box = Box.createHorizontalBox();
			math_box.setBorder(new TitledBorder("Combine"));
//			math_box.add(Box.createRigidArea(new Dimension(6, 0)));
			math_box.add(addB);
//			math_box.add(Box.createRigidArea(new Dimension(4, 0)));
			math_box.add(subB);
//			math_box.add(Box.createRigidArea(new Dimension(4, 0)));
			math_box.add(mulB);
//			math_box.add(Box.createRigidArea(new Dimension(4, 0)));
			math_box.add(divB);
			math_box.setAlignmentX(0.0f);
			advanced_panel.add(Box.createRigidArea(new Dimension(0, 6)));
			advanced_panel.add(math_box);

			cloneB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					scaleGraphs();
				}
			});

			floatCB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					floatGraphs(floatCB.isSelected());
				}
			});

			labelCB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setShowLabels(labelCB.isSelected());
				}
			});

			yaxisCB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setShowAxis(yaxisCB.isSelected());
				}
			});

			combineB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					combineGraphs();
				}
			});
			splitB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					splitGraphs();
				}
			});

			addB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					graphArithmetic(GraphGlyphUtils.MATH_SUM);
				}
			});
			subB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					graphArithmetic(GraphGlyphUtils.MATH_DIFFERENCE);
				}
			});
			mulB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					graphArithmetic(GraphGlyphUtils.MATH_PRODUCT);
				}
			});
			divB.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					graphArithmetic(GraphGlyphUtils.MATH_RATIO);
				}
			});
		}

		public void loadTransforms(Set<FloatTransformer> floatTransformers) {
			scaleCB.removeAllItems();
			name2transform.clear();
			for (FloatTransformer transformer : floatTransformers) {
				name2transform.put(transformer.getName(), transformer);
			}
			ArrayList<FloatTransformer> transformerList = new ArrayList<FloatTransformer>(floatTransformers);
			Collections.sort(transformerList,
				new Comparator<FloatTransformer>() {
					@Override
					public int compare(FloatTransformer o1, FloatTransformer o2) {
						int value = o1.getClass().getName().compareTo(o2.getClass().getName());
						if (value == 0) {
							value = o1.getName().compareTo(o2.getName());
						}
						return value;
					}
			}
			);
			for (FloatTransformer transformer : transformerList) {
				scaleCB.addItem(transformer.getName());
			}
		}

		/**
		 *  Puts all selected graphs in the same tier.
		 *  Current glyph factories do not support floating the combined graphs.
		 */
		private void combineGraphs() {
			int gcount = grafs.size();

			// Note that the combo_style does not implement IFloatableTierStyle
			// because the glyph factory doesn't support floating combo graphs anyway.
			ITrackStyle combo_style = null;

			// If any of them already has a combo style, use that one
			for (int i = 0; i < gcount && combo_style == null; i++) {
				GraphSym gsym = grafs.get(i);
				combo_style = gsym.getGraphState().getComboStyle();
			}
			// otherwise, construct a new combo style
			if (combo_style == null) {
				combo_style = new DefaultTrackStyle("Joined Graphs", true);
				combo_style.setHumanName("Joined Graphs");
				combo_style.setExpandable(true);
				combo_style.setCollapsed(true);
			}
			//combo_style.setHeight(5); // just use the default height

			// Now apply that combo style to all the selected graphs
			for (GraphSym gsym : grafs) {
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(combo_style);
				gstate.getTierStyle().setHeight(combo_style.getHeight());
				gstate.setFloatGraph(false); // ignored since combo_style is set
			}
			updateViewer();
		}

		/**
		 *  Puts all selected graphs in separate tiers by setting the
		 *  combo state of each graph's state to null.
		 */
		private void splitGraphs() {
			if (grafs.isEmpty()) {
				return;
			}

			for (GraphSym gsym : grafs) {
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(null);

				// For simplicity, set the floating state of all new tiers to false.
				// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
				gstate.setFloatGraph(false); // for simplicity
			}
			updateViewer();
		}

		private void graphArithmetic(String operation) {
			if (glyphs.size() == 2 && glyphs.get(0) != null && glyphs.get(1) != null) {
				hovereffect.unsetGraphName();
				GraphGlyph graphA = glyphs.get(0);
				GraphGlyph graphB = glyphs.get(1);
				GraphSym newsym = GraphGlyphUtils.graphArithmetic(graphA, graphB, operation);

				if (newsym != null) {
					BioSeq aseq = newsym.getGraphSeq();
					aseq.addAnnotation(newsym);
					gviewer.setAnnotatedSeq(aseq, true, true);
					//GlyphI newglyph = gviewer.getSeqMap().getItem(newsym);

					updateViewer();
				}
			} else {
				ErrorHandler.errorPanel("ERROR", "Must choose exactly 2 graphs", this);
			}
		}

		private void setShowAxis(boolean b) {
			for (GraphGlyph gl : glyphs) {
				gl.setShowAxis(b);
			}
			gviewer.getSeqMap().updateWidget();
		}

		private void setShowLabels(boolean b) {
			for (GraphGlyph gl : glyphs) {
				gl.setShowLabel(b);
			}
			gviewer.getSeqMap().updateWidget();
		}

		private void scaleGraphs() {
			String selection = (String) scaleCB.getSelectedItem();
			FloatTransformer trans = name2transform.get(selection);
			if (trans.setParameter(paramT.getText())) {
				List<GraphSym> newgrafs = transformGraphs(grafs, trans.getDisplay(), trans);
				if (!newgrafs.isEmpty()) {
					updateViewer();
				}
			}
			else {
				ErrorHandler.errorPanel(BUNDLE.getString("invalidParam") + " \"" + paramT.getText() + "\" for " + trans.getParamPrompt());
			}
		}

		private void floatGraphs(boolean do_float) {
			boolean something_changed = false;
			for (GraphGlyph gl : glyphs) {
				GraphState gstate = gl.getGraphState();
				if (gstate.getComboStyle() != null) {
					gstate.setComboStyle(null);
					something_changed = true;
				}
				boolean is_floating = gstate.getFloatGraph();
				if (do_float && (!is_floating)) {
					//GraphGlyphUtils.floatGraph(gl, gviewer);

					// figure out correct height
					Rectangle2D.Double coordbox = gl.getCoordBox();
					Rectangle pixbox = new Rectangle();
					gviewer.getSeqMap().getView().transformToPixels(coordbox, pixbox);
					gstate.getTierStyle().setY(pixbox.y);
					gstate.getTierStyle().setHeight(pixbox.height);

					gstate.setFloatGraph(true);
					something_changed = true;
				} else if ((!do_float) && is_floating) {
					//GraphGlyphUtils.attachGraph(gl, gviewer);

					// figure out correct height
					Rectangle2D.Double tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
					Rectangle pixbox = new Rectangle((int) tempbox.x, (int) tempbox.y, (int) tempbox.width, (int) tempbox.height);
					Rectangle2D.Double coordbox = new Rectangle2D.Double();
					gviewer.getSeqMap().getView().transformToCoords(pixbox, coordbox);
					gstate.getTierStyle().setY(coordbox.y); // currently y has no effect on attached graphs, but will someday
					gstate.getTierStyle().setHeight(coordbox.height);

					gstate.setFloatGraph(false);
					something_changed = true;
				}
			}
			if (something_changed) {
				updateViewer();
			}
		}
	}

	// from GraphAdjusterView

	private void deleteGraphs(GenometryModel gmodel, SeqMapView gviewer, List<GraphSym> grafs) {
		int gcount = grafs.size();
		for (int i = 0; i < gcount; i++) {
			GraphSym graf = grafs.get(i);
			deleteGraph(gmodel, gviewer, graf);
		}
		gmodel.clearSelectedSymmetries(SimpleGraphTab.class);
		gviewer.getSeqMap().updateWidget();
	}

	/**
	 *  Removes a GraphSym from the annotated bio seq it is annotating (if any),
	 *     and tries to make sure the GraphSym can be garbage collected.
	 *  Tries to delete the GraphGlyph representing the GraphSym.  If the GraphSym
	 *  happens to be a child of a tier in the widget, and the tier has no children
	 *  left after deleting the graph, then delete the tier as well.
	 */
	private void deleteGraph(GenometryModel gmodel, SeqMapView gviewer, GraphSym gsym) {
		BioSeq aseq = gsym.getGraphSeq();
		if (aseq != null) {
			aseq.removeAnnotation(gsym);
		}

		GraphGlyph gl = (GraphGlyph) gviewer.getSeqMap().getItem(gsym);
		if (gl == null) {
			return;
		}
		gviewer.getSeqMap().removeItem(gl);
		// clean-up references to the graph, allowing garbage-collection, etc.
		gviewer.select(Collections.<SeqSymmetry>emptyList());

		// if this is not a floating graph, then it's in a tier,
		//    so check tier -- if this graph is only child, then get rid of the tier also
		if (!gl.getGraphState().getFloatGraph()) {
			
			GlyphI parentgl = gl.getParent();
			parentgl.removeChild(gl);
			if (parentgl.getChildCount() == 0) {  // if no children left in tier, then remove it
				if (parentgl instanceof TierGlyph) {
					TrackView.deleteTrack((TierGlyph) parentgl);
					AffyTieredMap map = gviewer.getSeqMap();
					map.packTiers(false, true, false);
					map.stretchToFit(false, false);
				}
			}
		}
	}

	private void saveGraphs(SeqMapView gviewer, GenometryModel gmodel, List<GraphSym> grafs) {
		int gcount = grafs.size();
		if (gcount > 1) {
			// actually shouldn't get here, since save button is disabled if more than one graph
			ErrorHandler.errorPanel("Can only save one graph at a time");
		} else if (gcount == 1) {
			GraphSym gsym = grafs.get(0);
			try {
				GraphSaverFileChooser chooser = new GraphSaverFileChooser(gsym);
				chooser.setCurrentDirectory(igbService.getLoadDirectory());
				int option = chooser.showSaveDialog(igbService.getFrame());
				if (option == JFileChooser.APPROVE_OPTION) {
					igbService.setLoadDirectory(chooser.getCurrentDirectory());
					File fil = chooser.getSelectedFile();
					GraphSymUtils.writeGraphFile(gsym, gmodel.getSelectedSeqGroup(), fil.getAbsolutePath());
				}
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Error saving graph", ex);
			}
		}
	}

	private List<GraphSym> transformGraphs(List<GraphSym> grafs, String trans_name, FloatTransformer transformer) {
		List<GraphSym> newgrafs = new ArrayList<GraphSym>(grafs.size());
		for (GraphSym graf : grafs) {
			float[] new_ycoords;

			if (transformer instanceof IdentityTransform && graf instanceof GraphSym) {
				new_ycoords = (graf).getGraphYCoords();
			} else {
				int pcount = graf.getPointCount();
				new_ycoords = new float[pcount];
				for (int k = 0; k < pcount; k++) {
					new_ycoords[k] = transformer.transform(graf.getGraphYCoord(k));
				}
			}
			String newname = trans_name + " (" + graf.getGraphName() + ") ";

			// Transforming on this one seq only, not the whole genome
			String newid = trans_name + " (" + graf.getID() + ") ";
			newid = GraphSymUtils.getUniqueGraphID(newid, graf.getGraphSeq());
			GraphSym newgraf;
			if (graf instanceof GraphIntervalSym) {
				newgraf = new GraphIntervalSym(graf.getGraphXCoords(),
						((GraphIntervalSym) graf).getGraphWidthCoords(),
						new_ycoords, newid, graf.getGraphSeq());
			} else {
				newgraf = new GraphSym(graf.getGraphXCoords(),
						new_ycoords, newid, graf.getGraphSeq());
			}

			newgraf.setProperty(GraphSym.PROP_GRAPH_STRAND, graf.getProperty(GraphSym.PROP_GRAPH_STRAND));


			GraphState newstate = newgraf.getGraphState();
			newstate.copyProperties(graf.getGraphState());
			newstate.getTierStyle().setHumanName(newname); // this is redundant
			if (!(transformer instanceof IdentityTransform)) {
				// unless this is an identity transform, do not copy the min-max range
				newstate.setVisibleMinY(Float.NEGATIVE_INFINITY);
				newstate.setVisibleMaxY(Float.POSITIVE_INFINITY);
			}

			newgraf.getGraphSeq().addAnnotation(newgraf);
			newgrafs.add(newgraf);
		}
		return newgrafs;
	}

	private void applyColorChange(List<GraphSym> graf_syms, SeqMapView gviewer, Color col) {
		for (GraphSym graf : graf_syms) {
			// using getItems() instead of getItem(), in case graph sym is represented by multiple graph glyphs
			List<GraphGlyph> glist = gviewer.getSeqMap().getItems(graf);
			for (GraphGlyph gl : glist) {
				gl.setColor(col); // this automatically sets the GraphState color
				// if graph is in a tier, change foreground color of tier also
				//   (which in turn triggers change in color for TierLabelGlyph...)
				GlyphI glParent = gl.getParent();
				if (glParent instanceof TierGlyph) {
					glParent.setForegroundColor(col);
					List<ViewI> views = glParent.getScene().getViews();
					for (ViewI v : views) {
						if (gl.withinView(v)) {
							gl.draw(v);
						}
					}
				}
			}
		}
	}

	private void changeColor(List<GraphSym> graf_syms, SeqMapView gviewer) {
		if (graf_syms.isEmpty()) {
			return;
		}

		// Set an initial color so that the "reset" button will work.
		GraphSym graf_0 = graf_syms.get(0);
		GraphGlyph gl_0 = (GraphGlyph) gviewer.getSeqMap().getItem(graf_0);
		Color initial_color = Color.GREEN;
		if (gl_0 != null) {
			// gl_0 could be null if there is a selected graph that isn't visible in
			// the current view.
			initial_color = gl_0.getColor();
		}
		Color col = JColorChooser.showDialog((Component) gviewer.getSeqMap(),
				"Graph Color Chooser", initial_color);
		// Note: If the user selects "Cancel", col will be null
		if (col != null) {
			applyColorChange(graf_syms, gviewer, col);
		}
		gviewer.getSeqMap().updateWidget();
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
}
