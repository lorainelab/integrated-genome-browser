package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import javax.swing.Action;
import javax.swing.JMenu;

public class JRPMenu extends JMenu implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private final String id;
        private int index;

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
        
        public JRPMenu(String id, String s, int index) {
            this(id, s);
            this.index = index;
        }

	public JRPMenu(String id, String s, boolean b) {
		super(s, b);
		this.id = id;
		init();
	}

	private void init() {
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
        
        public int getIndex() {
            return index;
        }
}
