package com.affymetrix.genometry.event;

import com.affymetrix.common.CommonUtils;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

/**
 * Superclass of all IGB actions.
 * This is so we can keep track of actions for scripting, shortcuts, etc.
 * All actions in IGB also need to be added
 * to a singleton {@link GenericActionHolder}.
 *
 * @see GenericActionDoneCallback
 * @see GenericActionListener
 */
public abstract class GenericAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final char POPUP_DIALOG = '\u2026';
    private final String text;
    private final String tooltip;
    private final String iconPath;
    private final String largeIconPath;
    private final int mnemonic;
    private final Object extraInfo;
    private final boolean popup;
    private Set<GenericActionDoneCallback> doneCallbacks;

    /**
     * For ordering buttons in the toolbar.
     * Subclasses should assign different numbers
     * in the constructor or static initializer.
     */
    protected int ordinal = 0;

    public GenericAction(String text, int mnemonic) {
        this(text, null, null, null, mnemonic);
    }

    public GenericAction(String text, String iconPath, String largeIconPath) {
        this(text, null, iconPath, largeIconPath, KeyEvent.VK_UNDEFINED);
    }

    public GenericAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic) {
        this(text, tooltip, iconPath, largeIconPath, mnemonic, null, false);
    }

    public GenericAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup) {
        super();
        this.text = text;
        this.tooltip = tooltip;
        this.iconPath = iconPath;
        this.largeIconPath = largeIconPath;
        this.mnemonic = mnemonic;
        this.extraInfo = extraInfo;
        this.popup = popup;
        doneCallbacks = new HashSet<>();

        putValue(Action.NAME, getDisplay());
        if (iconPath != null) {
            ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
            if (icon == null) {
                System.out.println("icon " + iconPath + " returned null");
            }
            putValue(Action.SMALL_ICON, icon);
        }
        if (largeIconPath != null) {
            final ImageIcon icon = CommonUtils.getInstance().getIcon(largeIconPath);
            final ImageIcon alternateIcon = isToggle() && icon != null ? CommonUtils.getInstance().getAlternateIcon(largeIconPath) : null;
            if (icon == null) {
                System.out.println("icon " + largeIconPath + " returned null");
            }
            putValue(Action.LARGE_ICON_KEY, icon);

            if (alternateIcon != null) {
                this.addPropertyChangeListener(evt -> {
                    if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if (evt.getNewValue() == Boolean.TRUE) {
                            putValue(Action.LARGE_ICON_KEY, icon);
                        } else {
                            putValue(Action.LARGE_ICON_KEY, alternateIcon);
                        }
                    }
                });
            }
        }
        if (mnemonic != KeyEvent.VK_UNDEFINED) {
            this.putValue(MNEMONIC_KEY, mnemonic);
        }
        if (tooltip != null) {
            this.putValue(SHORT_DESCRIPTION, tooltip);
        }
//		if (tooltip == null && text != null) {
//			this.putValue(SHORT_DESCRIPTION, text);
//		}
    }

    public final String getText() {
        return text;
    }

    public String getDisplay() {
        if (text == null) {
            return null;
        }
        return text + (popup ? ("" + POPUP_DIALOG) : "");
    }

    public final String getTooltip() {
        return tooltip;
    }

    public final String getIconPath() {
        return iconPath;
    }

    public final int getMnemonic() {
        return mnemonic;
    }

    public final Object getExtraInfo() {
        return extraInfo;
    }

    public String getId() {
        return this.getClass().getName();
    }

    public final String getLargeIconPath() {
        return largeIconPath;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GenericActionHolder.getInstance().notifyActionPerformed(this);
    }

    public boolean usePrefixInMenu() {
        return false;
    }

    /**
     * @return true if this action should appear with a checkbox in the menu.
     */
    public boolean isToggle() {
        return false;
    }

    public final boolean isPopup() {
        return popup;
    }

    public void addDoneCallback(GenericActionDoneCallback doneCallback) {
        doneCallbacks.add(doneCallback);
    }

    public void removeDoneCallback(GenericActionDoneCallback doneCallback) {
        doneCallbacks.remove(doneCallback);
    }

    protected void actionDone() {
        for (GenericActionDoneCallback doneCallback : doneCallbacks) {
            doneCallback.actionDone(this);
        }
    }

    public final int getOrdinal() {
        return this.ordinal;
    }

    public boolean isToolbarAction() {
        return true;
    }
}
