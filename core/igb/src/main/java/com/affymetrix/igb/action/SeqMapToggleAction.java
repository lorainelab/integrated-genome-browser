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
import javax.swing.Action;

/**
 * An action that delegates to one of two other mutually exclusive actions. Zero
 * or one of the delegate actions are enabled at any one time. This one toggles
 * between using one delegate or the other. It is enabled iff one of the
 * delegates is. This could probably be generalized to handle more than two
 * delegates, but that is not yet needed. This must extend
 * {@link com.affymetrix.genometry.event.GenericAction} to participate in
 * the tool bar, scripting, and the tutorials. Can we make this just a
 * GenericAction? No. It must extend SeqMapViewActionA because when added to the
 * generic actions holder some init method is called?
 *
 * @author Eric Blossom
 */
public class SeqMapToggleAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    // Buffer space for add .index or something else at the end.
    private static int BUFFER = 8;
    /**
     * This is used to answer {@link #getId()}. It is not the same as "id" in
     * the super class.
     */
    private String identifier = null;
    /**
     * This is used to answer {@link #getDisplay()}.
     */
    private String display = null;
    private int toolbarIndex;

    private SeqMapViewActionA shownAction, hiddenAction;
    private boolean isToolbarDefault = false;
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
                SeqMapToggleAction.this.setEnabled(shownAction.isEnabled());
            }
        }

    };
    
    public SeqMapToggleAction(SeqMapViewActionA one, SeqMapViewActionA two, boolean isToolbarDefault) {
        this(one, two);
        this.isToolbarDefault = isToolbarDefault;
    }
    
    public SeqMapToggleAction(SeqMapViewActionA one, SeqMapViewActionA two, boolean isToolbarDefault, int toolbarIndex) {
        this(one, two, isToolbarDefault);
        this.toolbarIndex = toolbarIndex;
    }

    public SeqMapToggleAction(SeqMapViewActionA one, SeqMapViewActionA two) {
        super(one.getText(), one.getIconPath(), one.getLargeIconPath());
        this.shownAction = one;
        this.hiddenAction = two;
        putValue(Action.SHORT_DESCRIPTION, this.shownAction.getValue(Action.SHORT_DESCRIPTION));
        one.addPropertyChangeListener(toggle);
        two.addPropertyChangeListener(toggle);
        this.identifier = this.getClass().getName()
                + ":" + one.getId() + ";" + two.getId();
        if (java.util.prefs.AbstractPreferences.MAX_KEY_LENGTH < this.identifier.length() + BUFFER) {
            this.identifier = this.getClass().getName() + ":"
                    + one.getClass().getSimpleName() + ";" + two.getClass().getSimpleName();
        }
        if (java.util.prefs.AbstractPreferences.MAX_KEY_LENGTH < this.identifier.length() + BUFFER) {
            this.identifier = this.getClass().getSimpleName() + ":"
                    + one.getClass().getSimpleName() + ";" + two.getClass().getSimpleName();
        }
        this.display = "Toggle " + one.getDisplay() + " and " + two.getDisplay();
        this.ordinal = Math.min(one.getOrdinal(), two.getOrdinal()) - 1;
        this.setEnabled(one.isEnabled() || two.isEnabled());
        loadPreferredKeystrokes();
    }
    
    private void switchTo(SeqMapViewActionA a) {
        assert a == this.shownAction || a == this.hiddenAction;
        if (a == this.hiddenAction) {
            this.hiddenAction = this.shownAction;
            this.shownAction = a;
        } else {
            //java.util.logging.Logger.getLogger(this.getClass().getPackage().getName())
            //		.warning("What!? switching to shown action?");
            return;
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
     * Forward theEvent to the currently shown delegate.
     */
    @Override
    public void actionPerformed(ActionEvent theEvent) {
        this.shownAction.actionPerformed(theEvent);
    }

    /**
     * Overridden to include both clients' displays.
     */
    @Override
    public String getDisplay() {
        return this.display;
    }

    /**
     * Overridden to include both clients' identifiers.
     */
    @Override
    public String getId() {
        return this.identifier;
    }

    /**
     * Dubious method just so we can participate in preferences. This horrifying
     * kludge is to sneak past the guardian in
     * {@link com.affymetrix.igb.prefs.KeyStrokesView}. That guardian might be
     * trying to enforce the use of singletons. I could not find documentation
     * on why this is so.
     *
     * @return this
     */
    public Action getAction() {
        return this;
    }

    @Override
    public boolean isToolbarDefault() {
        return isToolbarDefault;
    }

    @Override
    public int getToolbarIndex() {
        return toolbarIndex; //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
