package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.event.ContinuousAction;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.CCPUtils;
import com.affymetrix.genoviz.swing.DragAndDropJPanel;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.action.SelectionRuleAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.shared.Selections.RefreshSelectionListener;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackListProvider;
import com.affymetrix.igb.swing.JRPButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 *
 * @author hiralv
 */
public class IGBToolBar extends JToolBar {

    private static final String NO_SELECTION_TEXT = "Click the map below to select annotations";
    private static final String SELECTION_INFO = "Selection Info";
    private static final SelectionRuleAction SELECTION_RULE_ACTION= SelectionRuleAction.getAction();
    private final JPanel TOOLBAR_ITEM_PANEL;
    private final JTextField TEXT_FIELD;
    private final Font SELECTION_FONT;
    private final Font NO_SELECTION_FONT;

    public IGBToolBar() {
        super();

        TOOLBAR_ITEM_PANEL = new DragAndDropJPanel();
        TOOLBAR_ITEM_PANEL.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        TOOLBAR_ITEM_PANEL.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        ((DragAndDropJPanel) TOOLBAR_ITEM_PANEL).addDropTargetListener(new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                reIndex();
            }
        });

        TEXT_FIELD = new JTextField(25);
        TEXT_FIELD.setBackground(Color.WHITE);
        TEXT_FIELD.setComponentPopupMenu(CCPUtils.getCCPPopup());
        SELECTION_FONT = TEXT_FIELD.getFont();
        NO_SELECTION_FONT = SELECTION_FONT.deriveFont(Font.ITALIC);
        setFloatable(false);
        setLayout(new BorderLayout());

        setup();
    }

    private void setup() {
        JPanel selectionpanel = new JPanel();
        selectionpanel.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
        selectionpanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        selectionpanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        selectionpanel.setBackground(Color.WHITE);

        TEXT_FIELD.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        TEXT_FIELD.setEditable(false);

        JLabel lf = new JLabel(SELECTION_INFO + ": ");
        lf.setFont(lf.getFont().deriveFont(Font.ITALIC));
        lf.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        lf.setBackground(Color.WHITE);

        selectionpanel.add(lf);
        selectionpanel.add(TEXT_FIELD);

        JButton button = new JButton(SELECTION_RULE_ACTION);
        button.setText("");
        button.setMargin(new Insets(2, 2, 2, 2));
//		button.setBorder(null);
        selectionpanel.add(button);

        selectionpanel.validate();

        add(TOOLBAR_ITEM_PANEL, BorderLayout.LINE_START);
        add(selectionpanel, BorderLayout.LINE_END);

        super.validate();

        setSelectionText(null, null);

        Selections.addRefreshSelectionListener(refreshSelectionListener);
    }

    public void setSelectionText(Map<String, Object> properties, String selectionText) {
        if (selectionText == null || selectionText.length() == 0) {
            TEXT_FIELD.setForeground(Color.LIGHT_GRAY);
            TEXT_FIELD.setFont(NO_SELECTION_FONT);
            TEXT_FIELD.setText(NO_SELECTION_TEXT);
            TEXT_FIELD.setEnabled(false);
        } else {
            TEXT_FIELD.setForeground(Color.BLACK);
            TEXT_FIELD.setFont(SELECTION_FONT);
            TEXT_FIELD.setText(selectionText);
            TEXT_FIELD.setEnabled(true);
        }
        SELECTION_RULE_ACTION.setProperties(properties);
        SELECTION_RULE_ACTION.setSelectionText(TEXT_FIELD.getText());
    }

    public void addToolbarAction(GenericAction genericAction, int index) {
        JRPButton button = new JRPButtonTLP(genericAction, index);
        button.setHideActionText(true);
        //button.setBorder(new LineBorder(Color.BLACK));
        button.setMargin(new Insets(0, 0, 0, 0));
        if ("Nimbus".equals(UIManager.getLookAndFeel().getName())){
            UIDefaults def = new UIDefaults();
            def.put("Button.contentMargins", new Insets(4, 4, 4, 4));
            button.putClientProperty("Nimbus.Overrides", def);
        }
        if (genericAction instanceof ContinuousAction) {
            button.addMouseListener(continuousActionListener);
        }

        int local_index = 0;
        while (local_index < index && local_index < TOOLBAR_ITEM_PANEL.getComponentCount()
                && index >= getOrdinal(TOOLBAR_ITEM_PANEL.getComponent(local_index))) {
            local_index++;
        }

        TOOLBAR_ITEM_PANEL.add(button, local_index);
        refreshToolbar();
    }

    public void removeToolbarAction(GenericAction action) {
        boolean removed = false;
        for (int i = 0; i < TOOLBAR_ITEM_PANEL.getComponentCount(); i++) {
            if (((JButton) TOOLBAR_ITEM_PANEL.getComponent(i)).getAction() == action) {
                if (action instanceof ContinuousAction) {
                    ((JButton) TOOLBAR_ITEM_PANEL.getComponent(i)).removeMouseListener(continuousActionListener);
                }
                TOOLBAR_ITEM_PANEL.remove(i);
                refreshToolbar();
                removed = true;
                break;
            }
        }

        if (removed) {
            reIndex();
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, ".removeToolbarAction: Could not find {0}", action);
        }
    }

    private void refreshToolbar() {
        TOOLBAR_ITEM_PANEL.validate();
        TOOLBAR_ITEM_PANEL.repaint();

        validate();
        repaint();
    }

    public void reIndex() {
        int index = 0;
        for (Component c : TOOLBAR_ITEM_PANEL.getComponents()) {
            if (c instanceof JRPButtonTLP) {
                ((JRPButtonTLP) c).setIndex(index++);
            }
        }
    }

    public void saveToolBar() {
        int index = 0;
        for (Component c : TOOLBAR_ITEM_PANEL.getComponents()) {
            if (c instanceof JButton && ((JButton) c).getAction() instanceof GenericAction) {
                GenericAction action = (GenericAction) ((JButton) c).getAction();
                PreferenceUtils.getToolbarNode().putInt(action.getId() + ".index", index++);
            }
        }
    }

    public int getItemCount() {
        return TOOLBAR_ITEM_PANEL.getComponentCount();
    }

    private int getOrdinal(Component c) {
        int ordinal = 0;
        if (c instanceof JRPButtonTLP) {
            ordinal = ((JRPButtonTLP) c).getIndex();
        }
        return ordinal;
    }

    private RefreshSelectionListener refreshSelectionListener = new RefreshSelectionListener() {
        @Override
        public void selectionRefreshed() {
            for (Component c : TOOLBAR_ITEM_PANEL.getComponents()) {
                if (c instanceof JButton && ((JButton) c).getAction() instanceof AbstractAction) {
                    AbstractAction action = (AbstractAction) ((JButton) c).getAction();
                    c.setEnabled(action.isEnabled());
                }
            }
        }
    };

    private final MouseListener continuousActionListener = new MouseAdapter() {
        private Timer timer;

        @Override
        public void mouseExited(MouseEvent e) {
            mouseReleased(e);
        }
       
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

    private class JRPButtonTLP extends JRPButton implements TrackListProvider {

        private static final long serialVersionUID = 1L;
        private int index;

        private JRPButtonTLP(GenericAction genericAction, int index) {
            super("Toolbar_" + genericAction.getId(), genericAction);
            setHideActionText(true);
            this.index = index;
        }

        private int getIndex() {
            return index;
        }

        private void setIndex(int i) {
            this.index = i;
        }

        @Override
        public void fireActionPerformed(ActionEvent evt) {
            if (((GenericAction) getAction()).isToggle() && this.getAction().getValue(AbstractAction.SELECTED_KEY) != null) {
                this.getAction().putValue(AbstractAction.SELECTED_KEY,
                        !Boolean.valueOf(getAction().getValue(AbstractAction.SELECTED_KEY).toString()));
            }
            super.fireActionPerformed(evt);
        }

        @Override
        public List<TierGlyph> getTrackList() {
            return ((IGB) Application.getSingleton()).getMapView().getTierManager().getSelectedTiers();
        }
    }

    
}
