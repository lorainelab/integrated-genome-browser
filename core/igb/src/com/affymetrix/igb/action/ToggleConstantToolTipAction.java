package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapViewMouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class ToggleConstantToolTipAction extends GenericAction {
	private static final long serialVersionUID = 1;
	private static final ToggleConstantToolTipAction ACTION = new ToggleConstantToolTipAction();

	private ToggleConstantToolTipAction() {
		super(BUNDLE.getString("toggleConstantPropertiesTooltip"), null,
				"toolbarButtonGraphics/general/ContextualHelp16.gif",
				"toolbarButtonGraphics/general/ContextualHelp16.gif", // for tool bar
				KeyEvent.VK_H);
		this.ordinal = 160;
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);
		this.putValue(SELECTED_KEY, SeqMapViewMouseListener.show_tooltip);
		this.putValue(SELECTED_KEY, PreferenceUtils.getTopNode().getBoolean(SeqMapViewMouseListener.PREF_SHOW_TOOLTIP, true));
		SeqMapViewMouseListener.show_tooltip = (Boolean)(this.getValue(SELECTED_KEY));
	}

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static ToggleConstantToolTipAction getAction() {
		return ACTION;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		SeqMapViewMouseListener.show_tooltip = (Boolean)(this.getValue(SELECTED_KEY));
		ACTION.putValue(AbstractAction.SELECTED_KEY, SeqMapViewMouseListener.show_tooltip);
		PreferenceUtils.getTopNode().putBoolean(
				SeqMapViewMouseListener.PREF_SHOW_TOOLTIP, (Boolean)getValue(SELECTED_KEY));

	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
