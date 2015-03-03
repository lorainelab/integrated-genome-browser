package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import com.affymetrix.igb.swing.util.WeightUtil;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class JRPMenu extends JMenu implements WeightedJRPWidget {

    private static final long serialVersionUID = 1L;
    private final String id;
    private int weight;
    private List<WeightedJRPWidget> menuItemComponents;

    public JRPMenu(String id) {
        super();
        this.id = id;
        init();
    }

    public JRPMenu(String id, Action a) {
        super(a);
        this.id = id;
        init();
    }

    public JRPMenu(String id, String s) {
        super(s);
        this.id = id;
        init();
    }

    public JRPMenu(String id, String s, int weight) {
        this(id, s);
        this.weight = weight;
    }

    public JRPMenu(String id, String s, boolean b) {
        super(s, b);
        this.id = id;
        init();
    }

    private void init() {
        menuItemComponents = new ArrayList<>();
        ScriptManager.getInstance().addWidget(this);
        addActionListener(e -> ScriptManager.getInstance().recordOperation(new Operation(JRPMenu.this, "doClick()")));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return true;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public JMenuItem add(JMenuItem newMenuItem) {
        if (newMenuItem instanceof WeightedJRPWidget) {
            if (((WeightedJRPWidget) newMenuItem).getWeight() != -1) {
                int loc = WeightUtil.locationToAdd(menuItemComponents, (WeightedJRPWidget) newMenuItem);
                menuItemComponents.add(loc, (WeightedJRPWidget) newMenuItem);
                return (JMenuItem) super.add(newMenuItem, loc);
            } else {
                return (JMenuItem) super.add(newMenuItem, -1);
            }

        } else {
            throw new IllegalArgumentException("Only add WeightedJRPWidget to menu");
        }
    }

    @Override
    public void addSeparator() {
        if (!menuItemComponents.isEmpty()) {
            JRPSeparator separator = new JRPSeparator(menuItemComponents.get(menuItemComponents.size() - 1).getWeight() + 1);
            menuItemComponents.add(separator);
            super.add(separator, -1);
        }
    }

    public void addSeparator(int weight) {
        JRPSeparator separator = new JRPSeparator(weight);
        menuItemComponents.add(separator);
        super.add(separator, -1);
    }
}
