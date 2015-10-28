package com.affymetrix.igb.view;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.ContinuousAction;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.swing.CCPUtils;
import com.affymetrix.genoviz.swing.DragAndDropJPanel;
import com.affymetrix.igb.action.SelectionRuleAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.Selections.RefreshSelectionListener;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.WeightedJRPWidget;
import com.affymetrix.igb.swing.util.WeightUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public class IGBToolBar extends JToolBar {

    private static final long serialVersionUID = 1L;

    private static final String NO_SELECTION_TEXT = "Click the map below to select annotations";
    private static final String SELECTION_INFO = "Selection Info";
    private static final SelectionRuleAction SELECTION_RULE_ACTION = SelectionRuleAction.getAction();
    private final JPanel toolbarItemPanel;
    private final JTextField selectionInfoTextField;
    private final Font selectionFont;
    private final Font noSelectionFont;
    private static final Logger logger = LoggerFactory.getLogger(IGBToolBar.class);
    private boolean dragDropInProgress = false;

    public IGBToolBar() {
        super();

        toolbarItemPanel = new DragAndDropJPanel();
        toolbarItemPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        toolbarItemPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        ((DragAndDropJPanel) toolbarItemPanel).addDragSourceListener(new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent dsde) {
                dragDropInProgress = false;
                triggerMouseReleasedEvent();
            }

        });
        ((DragAndDropJPanel) toolbarItemPanel).addDropTargetListener(new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dragDropInProgress = false;
                reIndex();
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                dragDropInProgress = true;
            }
        });

        selectionInfoTextField = new JTextField(25);
        selectionInfoTextField.setBackground(Color.WHITE);
        selectionInfoTextField.setComponentPopupMenu(CCPUtils.getCCPPopup());
        selectionFont = selectionInfoTextField.getFont();
        noSelectionFont = selectionFont.deriveFont(Font.ITALIC);
        setFloatable(false);
        setLayout(new BorderLayout());

        setup();
    }

    private void setup() {
        JPanel selectionPanel = new JPanel();
        selectionPanel.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
        selectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        selectionPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        selectionPanel.setBackground(Color.WHITE);

        selectionInfoTextField.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        selectionInfoTextField.setEditable(false);

        JLabel lf = new JLabel(SELECTION_INFO + ": ");
        lf.setFont(lf.getFont().deriveFont(Font.ITALIC));
        lf.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        lf.setBackground(Color.WHITE);

        selectionPanel.add(lf);
        selectionPanel.add(selectionInfoTextField);

        JButton button = new JButton(SELECTION_RULE_ACTION);
        button.setText("");
        button.setMargin(new Insets(2, 2, 2, 2));
//		button.setBorder(null);
        selectionPanel.add(button);

        selectionPanel.validate();

        add(toolbarItemPanel, BorderLayout.LINE_START);
        add(selectionPanel, BorderLayout.LINE_END);

        super.validate();

        setSelectionText(null, null, null);
        refreshSelectionListener = new RefreshSelectionListener() {
            @Override
            public void selectionRefreshed() {
                for (Component c : toolbarItemPanel.getComponents()) {
                    if (c instanceof JButton && ((JButton) c).getAction() instanceof AbstractAction) {
                        AbstractAction action = (AbstractAction) ((JButton) c).getAction();
                        c.setEnabled(action.isEnabled());
                    }
                }
            }
        };

    }
    private RefreshSelectionListener refreshSelectionListener;

    //prevent static block in Selections calling IGB.getInstance() before IGB constructor has returned leading to two instances of what should be a singleton
    public void initHack() {
        Selections.addRefreshSelectionListener(refreshSelectionListener);
    }

    private void triggerMouseReleasedEvent() {
        for (Component c : toolbarItemPanel.getComponents()) {
            if (c instanceof JRPButtonTLP) {
                for (MouseListener m : c.getMouseListeners()) {
                    MouseEvent me = new MouseEvent(c, 0, 0, 0, 100, 100, 1, false);
                    m.mouseReleased(me);
                }
            }
        }
    }

    public void setSelectionText(Map<String, Object> properties, String selectionText, SeqSymmetry sym) {
        if (selectionText == null || selectionText.length() == 0) {
            selectionInfoTextField.setForeground(Color.LIGHT_GRAY);
            selectionInfoTextField.setFont(noSelectionFont);
            selectionInfoTextField.setText(NO_SELECTION_TEXT);
            selectionInfoTextField.setEnabled(false);
        } else {
            selectionInfoTextField.setForeground(Color.BLACK);
            selectionInfoTextField.setFont(selectionFont);
            selectionInfoTextField.setText(selectionText);
            selectionInfoTextField.setEnabled(true);
        }
        SELECTION_RULE_ACTION.setProperties(properties);
        if (sym instanceof SymWithProps) {
            SELECTION_RULE_ACTION.setSym((SymWithProps) sym);
        }
        SELECTION_RULE_ACTION.setSelectionText(selectionInfoTextField.getText());
    }

    public void addToolbarAction(GenericAction genericAction, int index) {
        if (!checkIfAlreadyAdded(genericAction)) {
            JRPButtonTLP button = new JRPButtonTLP(genericAction, index);
            button.setHideActionText(true);
            //button.setBorder(new LineBorder(Color.BLACK));
            button.setMargin(new Insets(0, 0, 0, 0));
            if (genericAction instanceof ContinuousAction) {
                button.addMouseListener(continuousActionListener);
            }

            int loc = WeightUtil.locationToAdd(toolbarItemPanel, button);
            toolbarItemPanel.add(button, loc);
            refreshToolbar();
        }
    }

    private boolean checkIfAlreadyAdded(GenericAction action) {
        for (int i = 0; i < toolbarItemPanel.getComponentCount(); i++) {
            if (((JButton) toolbarItemPanel.getComponent(i)).getAction() == action) {
                return true;
            }
        }
        return false;
    }

    public void removeToolbarAction(GenericAction action) {
        boolean removed = false;
        for (int i = 0; i < toolbarItemPanel.getComponentCount(); i++) {
            if (((JButton) toolbarItemPanel.getComponent(i)).getAction() == action) {
                if (action instanceof ContinuousAction) {
                    toolbarItemPanel.getComponent(i).removeMouseListener(continuousActionListener);
                }
                toolbarItemPanel.remove(i);
                refreshToolbar();
                removed = true;
                break;
            }
        }

        if (removed) {
            reIndex();
        } else {
            logger.warn(".removeToolbarAction: Could not find {}", action);
        }
    }

    private void refreshToolbar() {
        toolbarItemPanel.validate();
        toolbarItemPanel.repaint();

        validate();
        repaint();
    }

    public void reIndex() {
        int index = 0;
        for (Component c : toolbarItemPanel.getComponents()) {
            if (c instanceof JRPButtonTLP) {
                ((JRPButtonTLP) c).setWeight(index++);
                // ((JRPButtonTLP) c).getm
            }
        }
    }

    public void saveToolBar() {
        int index = 0;
        for (Component c : toolbarItemPanel.getComponents()) {
            if (c instanceof JButton && ((JButton) c).getAction() instanceof GenericAction) {
                GenericAction action = (GenericAction) ((JButton) c).getAction();
                PreferenceUtils.getToolbarNode().putInt(action.getId() + ".index", index++);
            }
        }
    }

    public int getItemCount() {
        return toolbarItemPanel.getComponentCount();
    }

    private int getOrdinal(Component c) {
        int ordinal = 0;
        if (c instanceof JRPButtonTLP) {
            ordinal = ((JRPButtonTLP) c).getWeight();
        }
        return ordinal;
    }

    private final MouseListener continuousActionListener = new MouseAdapter() {
        private Timer timer;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!(e.getSource() instanceof JButton)
                    || ((JButton) e.getSource()).getAction() == null) {
                return;
            }

            timer = new Timer(200, ((JButton) e.getSource()).getAction());
            timer.start();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (timer != null) {
                timer.stop();
            }
        }
    };

    private class JRPButtonTLP extends JRPButton implements WeightedJRPWidget {

        private static final long serialVersionUID = 1L;
        private int weight;

        private JRPButtonTLP(GenericAction genericAction, int weight) {
            super("Toolbar_" + genericAction.getId(), genericAction);
            setHideActionText(true);
            this.weight = weight;
        }

        private void setWeight(int i) {
            this.weight = i;
        }

        @Override
        public void fireActionPerformed(ActionEvent evt) {
            if (dragDropInProgress) {
                return;
            }
            if (((GenericAction) getAction()).isToggle() && this.getAction().getValue(AbstractAction.SELECTED_KEY) != null) {
                this.getAction().putValue(AbstractAction.SELECTED_KEY,
                        !Boolean.valueOf(getAction().getValue(AbstractAction.SELECTED_KEY).toString()));
            }
            super.fireActionPerformed(evt);
        }

        @Override
        public int getWeight() {
            return weight;
        }

    }

}
