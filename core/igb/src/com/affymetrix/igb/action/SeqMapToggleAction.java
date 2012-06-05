/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;

/**
 * An action that delegates to one of two other mutually exclusive actions.
 * Zero or one of the delegate actions are enabled at any one time.
 * This one toggles between using one delegate or the other.
 * It is enabled iff one of the delegates is.
 * This could probably be generalized to handle more than two delegates,
 * but that is not yet needed.
 * This must extend {@link GenericAction}
 * to participate in the tool bar, scripting, and the tutorials.
 * Can we make this just aGenericAction? No.
 * It must extend SeqMapViewActionA
 * because when added to the generic actions holder some init method is called?
 * @author Eric Blossom
 */
public class SeqMapToggleAction extends SeqMapViewActionA {

	private SeqMapViewActionA shownAction, hiddenAction;
	private PropertyChangeListener toggle = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Object o = evt.getSource();
			SeqMapViewActionA source = (SeqMapViewActionA) o;
			if (evt.getPropertyName().equals("enabled")) {
				o = evt.getNewValue();
				Boolean enabled = (Boolean) o;
				if (enabled) {
					SeqMapToggleAction.this.switchTo(source);
				}
			}
		}

	};
	
	public SeqMapToggleAction(SeqMapViewActionA one, SeqMapViewActionA two) {
		super(one.getText(), one.getIconPath(), one.getLargeIconPath());
		this.shownAction = one;
		this.hiddenAction = two;
		putValue(Action.SHORT_DESCRIPTION, one.getTooltip());
		one.addPropertyChangeListener(toggle);
		two.addPropertyChangeListener(toggle);
	}

	private void switchTo(SeqMapViewActionA a) {
		assert a == this.shownAction || a == this.hiddenAction;
		if (a == this.hiddenAction) {
			this.hiddenAction = this.shownAction;
			this.shownAction = a;
		}
		else {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
					"What!? switching to shown action?");
		}
		this.putValue(Action.NAME, this.shownAction.getValue(Action.NAME));
		this.putValue(Action.SHORT_DESCRIPTION,
				this.shownAction.getValue(Action.SHORT_DESCRIPTION));
		this.putValue(Action.SMALL_ICON,
				this.shownAction.getValue(Action.SMALL_ICON));
		this.putValue(Action.LARGE_ICON_KEY,
				this.shownAction.getValue(Action.LARGE_ICON_KEY));
	}

	/**
	 * Hide the currently shown delegate and then forward theEvent to it.
	 */
	@Override
	public void actionPerformed(ActionEvent theEvent) {
		this.shownAction.actionPerformed(theEvent);
		this.hiddenAction.setEnabled(true);
	}

}
