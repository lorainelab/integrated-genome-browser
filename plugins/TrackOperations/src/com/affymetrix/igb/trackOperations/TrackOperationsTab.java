package com.affymetrix.igb.trackOperations;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.operator.OperatorComparator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.util.ThreadUtils;

import com.affymetrix.genoviz.swing.recordplayback.JRPComboBoxWithSingleListener;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;
import static com.affymetrix.igb.shared.Selections.*;

public final class TrackOperationsTab extends TrackOperationsTabGUI implements RefreshSelectionListener{ 

	boolean is_listening = true; // used to turn on and off listening to GUI events
	boolean DEBUG_EVENTS = false;
	private final IGBService igbService;
	
	private final Map<String, Operator> name2transformation;
	private final Map<String, Operator> name2operation;
	

	public TrackOperationsTab(IGBService igbS) {
		this.igbService = igbS;
		name2transformation = new HashMap<String, Operator>();
		name2operation = new HashMap<String, Operator>();
		
		
		getCombineB().setAction(new CombineGraphsAction(igbService));
		getSplitB().setAction(new SplitGraphsAction(igbService));
		
		
		getTransformationCB().addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setTransformationDisplay(true);
			}
		});
		
		getOperationCB().addMouseListener(new HoverEffect());
		getOperationCB().addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setOperationDisplay(true);
			}
		});

		getTransformationGoB().setAction(new TrackTransformAction(null) {
			private static final long serialVersionUID = 1L;
			@Override
			protected Operator getOperator() {
				String selection = (String) getTransformationCB().getSelectedItem();
				return name2transformation.get(selection);
			}
		});

		getOperationGoB().setAction(new TrackOperationAction(null) {
			private static final long serialVersionUID = 1L;
			@Override
			protected Operator getOperator() {
				String selection = (String) getOperationCB().getSelectedItem();
				return name2operation.get(selection);
			}
		});
		
		getThreshB().setAction(ThresholdingAction.createThresholdingAction(igbService));
		
		resetAll(false);
		Selections.addRefreshSelectionListener(this);
	}

	public void addOperator(Operator operator) {
		resetAll(true);
	}

	public void removeOperator(Operator operator) {
		resetAll(true);
	}

	@Override
	public void selectionRefreshed() {
		resetAll(true);
	}
			
	private void resetAll(boolean enable) {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		loadOperators(enable);
		setPanelEnabled(enable);
		is_listening = true; // turn back on GUI events
	}

	public void updateViewer() {
		// set selections to empty so that options get turned off
		resetAll(false);
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				igbService.getSeqMapView().updatePanel();
				resetAll(true);
			}
		});
	}

	private void loadOperators(boolean enable) {
		name2transformation.clear();
		name2operation.clear();
		
		getTransformationCB().removeAllItems();
		getOperationCB().removeAllItems();
		
		if (rootSyms.size() == 0 || !enable) {
			return;
		}
		FileTypeCategory transformCategory = rootSyms.get(0).getCategory();
		for (RootSeqSymmetry rootSym : rootSyms) {
			if (transformCategory != rootSym.getCategory()) {
				transformCategory = null;
				break;
			}
		}
		boolean transformOK = transformCategory != null;
		TreeSet<Operator> operators = new TreeSet<Operator>(new OperatorComparator());
		operators.addAll(ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls());
		List<RootSeqSymmetry> transformSyms = new ArrayList<RootSeqSymmetry>(); // fake List to test compatibility of Transform operations
		transformSyms.add(rootSyms.get(0));
		for (Operator operator : operators) {
			if (transformOK && TrackUtils.getInstance().checkCompatible(transformSyms, operator, true)) {
				name2transformation.put(operator.getDisplay(), operator);
				getTransformationCB().addItem(operator.getDisplay());
			}
			if (TrackUtils.getInstance().checkCompatible(rootSyms, operator, true)) {
				name2operation.put(operator.getDisplay(), operator);
				getOperationCB().addItem(operator.getDisplay());
			}
		}
	}

	public void setPanelEnabled(boolean enable) {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		getCombineB().setEnabled(enable && graphGlyphs.size() > 1 && !isAnyJoined());
		getSplitB().setEnabled(enable && isAnyJoined());
		getThreshB().setEnabled(enable && !graphGlyphs.isEmpty());
		int transformCount = name2transformation.size();
		int operatorCount = name2operation.size();
		boolean enableTransformation = enable && isAllRootSeqSymmetrySame() && transformCount > 0;
		
		getTransformationParamLabel().setEnabled(enableTransformation);
		getTransformationCB().setEnabled(enableTransformation);
		if (!enableTransformation) {
			getTransformationCB().removeAllItems();
		}
		getTransformationGoB().setEnabled(enableTransformation);
		setTransformationDisplay(enableTransformation);
		boolean enableOperation = enable && allGlyphs.size() > 1 && operatorCount > 0;
		
		getOperationParamLabel().setEnabled(enableOperation);
		getOperationCB().setEnabled(enableOperation);
		if (!enableOperation) {
			getOperationCB().removeAllItems();
		}
		getOperationGoB().setEnabled(enableOperation);
		setOperationDisplay(enableOperation);

		is_listening = true; // turn back on GUI events
	}

	private void setTransformationDisplay(boolean enable) {
		setAtionDisplay(getTransformationCB(), getTransformationParamLabel(), getTransformationParam(), name2transformation, getTransformationGoB(), enable, true);
	}

	private void setOperationDisplay(boolean enable) {
		setAtionDisplay(getOperationCB(), getOperationParamLabel(), getOperationParam(), name2operation, getOperationGoB(), enable, false);
	}

	private static void setAtionDisplay(
		JComboBox ationCB,
		JLabel ationParamLabel,
		JTextField ationParam,
		Map<String, Operator> name2ation,
		JButton ationGoB,
		boolean enable,
		boolean singleOK
		) {
		String selection = (String) ationCB.getSelectedItem();
		if (!enable || selection == null) {
			ationParamLabel.setText(" ");
			ationParamLabel.setEnabled(false);
			ationParam.setEditable(false);
			ationParam.setEnabled(false);
		} else {
			Operator operator = name2ation.get(selection);
			ationGoB.setToolTipText(getTooltipMessage(operator));
			Map<String, Class<?>> params = operator.getParameters();
			if (params == null || params.size() == 0 || (!singleOK && allGlyphs.size() < 2)) {
				ationParamLabel.setText(" ");
				ationParamLabel.setEnabled(false);
				ationParam.setEditable(false);
				ationParam.setEnabled(false);
			} else {
				ationParamLabel.setText(params.keySet().iterator().next() + " :"); // only one parameter, for now
				ationParamLabel.setEnabled(true);
				ationParam.setEditable(true);
				ationParam.setText("");
				ationParam.setEnabled(true);
			}
		}
	}

	/**
	 * get the error message text for an attempted graph/annotation operation
	 * @param graphCount the number of graph glyphs
	 * @param minCount the minimum graphs for the operator
	 * @param maxCount the maximum graphs for the operator
	 * @return the error message text
	 */
	private static final String selectExactGraphsMessage = BUNDLE.getString("operatorExactTooltip");
	private static final String selectMinGraphsMessage = BUNDLE.getString("operatorMinTooltip");
	private static final String selectMaxGraphsMessage = BUNDLE.getString("operatorMaxTooltip");
	private static final String selectRangeGraphsMessage = BUNDLE.getString("operatorRangeTooltip");
	public static String getTooltipMessage(Operator operator) {
		if (operator == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (FileTypeCategory category : FileTypeCategory.values()) {
			int minCount = operator.getOperandCountMin(category);
			int maxCount = operator.getOperandCountMax(category);
			String categoryName = category.toString(); // not translated for now
			if (maxCount > 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				if (minCount == maxCount) {
					sb.append(MessageFormat.format(selectExactGraphsMessage, minCount, categoryName));
				}
				else if (minCount == 0) {
					sb.append(MessageFormat.format(selectMaxGraphsMessage, maxCount, categoryName));
				}
				else if (maxCount == Integer.MAX_VALUE) {
					sb.append(MessageFormat.format(selectMinGraphsMessage, minCount, categoryName));
				}
				else {
					sb.append(MessageFormat.format(selectRangeGraphsMessage, minCount, maxCount, categoryName));
				}
			}
		}
		return sb.toString();
	}

	private class HoverEffect implements MouseListener {

		private String A = null;
		private String B = null;

		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
			JRPComboBoxWithSingleListener comp = (JRPComboBoxWithSingleListener) e.getComponent();
			String selection = (String) comp.getSelectedItem();
			Operator operator = name2operation.get(selection);
			if (operator == null) {
				return;
			}

			if (rootSyms.size() >= operator.getOperandCountMin(FileTypeCategory.Graph)
					&& rootSyms.size() <= operator.getOperandCountMax(FileTypeCategory.Graph)) {
				setGraphName(comp, operator);
			} else {
				comp.setToolTipText(getTooltipMessage(operator));
			}
		}

		public void mouseExited(MouseEvent e) {
			JRPComboBoxWithSingleListener comp = (JRPComboBoxWithSingleListener) e.getComponent();
			String selection = (String) comp.getSelectedItem();
			unsetGraphName(name2operation.get(selection));
		}

		public void setGraphName(JRPComboBoxWithSingleListener comp, Operator operator) {
			if (operator != null && operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
				A = ((GraphSym)rootSyms.get(0)).getGraphName();
				B = ((GraphSym)rootSyms.get(1)).getGraphName();

				((GraphSym)rootSyms.get(0)).setGraphName("A");
				((GraphSym)rootSyms.get(1)).setGraphName("B");

				comp.setToolTipText(null);
				ThreadUtils.runOnEventQueue(new Runnable() {

					public void run() {
						igbService.getSeqMap().updateWidget();
					}
				});
			}
		}

		public void unsetGraphName(Operator operator) {
			if (operator != null && operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
				if (A != null && B != null && rootSyms.size() > 1) {
					((GraphSym)rootSyms.get(0)).setGraphName(A);
					((GraphSym)rootSyms.get(1)).setGraphName(B);

					ThreadUtils.runOnEventQueue(new Runnable() {

						public void run() {
							igbService.getSeqMap().updateWidget();
						}
					});
					A = null;
					B = null;

				}
			}
		}
	}
}
