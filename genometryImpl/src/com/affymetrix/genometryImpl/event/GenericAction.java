package com.affymetrix.genometryImpl.event;

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
 * This is done via the constructor, a dubious practice.
 * @see GenericActionDoneCallback
 * @see GenericActionListener
 */
public abstract class GenericAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private static final char POPUP_DIALOG = '\u2026';
	private static final String ELLIPSIS = "...";
	private final String text;
	private final String tooltip;
	private final String iconPath;
	private final int mnemonic;
	private final Object extraInfo;
	private final boolean popup;
	private Set<GenericActionDoneCallback> doneCallbacks;

	public GenericAction(String text, String tooltip, String iconPath, int mnemonic) {
		this(text, tooltip, iconPath, mnemonic, null, false);
	}

	public GenericAction(String text, String tooltip, String iconPath, int mnemonic, Object extraInfo, boolean popup) {
		super();
		this.text = text;
		this.tooltip = tooltip;
		this.iconPath = iconPath;
		this.mnemonic = mnemonic;
		this.extraInfo = extraInfo;
		this.popup = popup;
		doneCallbacks = new HashSet<GenericActionDoneCallback>();
		_setProperties();
	}

	public GenericAction(String text, String iconPath) {
		this(text, null, iconPath, KeyEvent.VK_UNDEFINED);
	}

	public GenericAction(String text, int mnemonic) {
		this(text, null, null, mnemonic);
	}

	private void _setProperties() {
		setProperties(true);
	}
	protected void setProperties(boolean add) {
		putValue(Action.NAME, getDisplay());
		if (iconPath != null) {
			ImageIcon icon = CommonUtils.getInstance().getIcon(iconPath);
			if (icon == null) {
				System.out.println("icon " + iconPath + " returned null");
			}
			putValue(Action.SMALL_ICON, icon);
		}
		if (mnemonic != KeyEvent.VK_UNDEFINED) {
			this.putValue(MNEMONIC_KEY, mnemonic);
		}
		if (tooltip != null) {
			this.putValue(SHORT_DESCRIPTION, tooltip);
		}
		if (add) {
			GenericActionHolder.getInstance().addGenericAction(this);
		}
	}
	public final String getText() {
		return text;
	}
	public final String getDisplay() {
		if (text == null) {
			return null;
		}
		return  text + (popup ? ("" + POPUP_DIALOG) : "");
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
	public final String getId() {
		return this.getClass().getName();
	}
	public static String getCleanText(String text) {
		String cleanText = text;
		if (cleanText.endsWith("" + POPUP_DIALOG)) {
			cleanText = cleanText.substring(0, cleanText.length() - 1);
		}
		if (cleanText.endsWith(ELLIPSIS)) {
			cleanText = cleanText.substring(0, cleanText.length() - ELLIPSIS.length());
		}
		return cleanText;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		GenericActionHolder.getInstance().notifyActionPerformed(this);
	}
	public boolean usePrefixInMenu() {
		return false;
	}
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

}
