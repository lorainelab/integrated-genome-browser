package com.affymetrix.igb.swing;

import java.awt.event.ItemListener;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ComboBoxModel;

/**
 * Only allow one listener to be added to the combo box. Otherwise potentially
 * many events can be fired with one change.
 */
public class JRPComboBoxWithSingleListener extends JRPComboBox {

	private static final long serialVersionUID = 1L;

	public JRPComboBoxWithSingleListener(String id) {
		super(id);
	}

	public JRPComboBoxWithSingleListener(String id, ComboBoxModel aModel) {
		super(id, aModel);
	}

	public JRPComboBoxWithSingleListener(String id, Object[] items) {
		super(id, items);
	}

	public JRPComboBoxWithSingleListener(String id, Vector<?> items) {
		super(id, items);
	}

	/**
	 * Default implementation of addListener permits the same class to be added
	 * as a listener multiple times, causing it to be notified of an event
	 * multiple times.
	 *
	 * This is a quick kludge to prevent a listener from being added multiple
	 * times. Hopefully this can be removed once we sort out adding and removing
	 * ItemListeners.
	 */
	@Override
	public void addItemListener(ItemListener aListener) {
		for (ItemListener listener : this.getItemListeners()) {
			if (listener == aListener) {
				Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Attempt to add duplicate ItemListener, ignoring");
				return;
			}
		}
		super.addItemListener(aListener);
	}
}
