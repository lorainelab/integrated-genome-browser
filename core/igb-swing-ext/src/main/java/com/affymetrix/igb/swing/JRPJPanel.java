/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptManager;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/**
 *
 * @author dcnorris
 */
public class JRPJPanel extends JPanel implements JRPWidget {

	private static final long serialVersionUID = 1L;
	private String id;

	public JRPJPanel(String id, LayoutManager lm, boolean bln) {
		super(lm, bln);
		this.id = id;
		init();
	}

	public JRPJPanel(String id, LayoutManager lm) {
		super(lm);
		this.id = id;
		init();
	}

	public JRPJPanel(String id, boolean bln) {
		super(bln);
		this.id = id;
		init();
	}

	public JRPJPanel(String id) {
		super();
		this.id = id;
		init();
	}

	public void setId(String id) {
		if (this.id == null) {
			this.id = id;
			ScriptManager.getInstance().addWidget(this);
		}
	}

	private void init() {
		ScriptManager.getInstance().addWidget(this);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean consecutiveOK() {
		return true;
	}
}
