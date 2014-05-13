package com.affymetrix.igb.action;

import java.awt.event.KeyEvent;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapView;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;

/**
 *
 * @author hiralv
 */
public class ToggleToolTipAction extends GenericAction {
	private static final String SHOW_TOOLTIP = BUNDLE.getString("showDataTooltip");
	private static final String HIDE_TOOLTIP = BUNDLE.getString("hideDataTooltip");
	
	private static final long serialVersionUID = 1;
	private static final ToggleToolTipAction ACTION = new ToggleToolTipAction();

	private ToggleToolTipAction() {
		super(BUNDLE.getString("togglePropertiesTooltip"), null,
				"16x16/actions/speech-bubble.png",
				"22x22/actions/speech-bubble.png", // for tool bar
				KeyEvent.VK_H);
		this.ordinal = 160;
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);
		
		Boolean selected = (Boolean) getValue(Action.SELECTED_KEY);
		putValue(Action.SHORT_DESCRIPTION, selected != null && selected ? HIDE_TOOLTIP : SHOW_TOOLTIP);
		this.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
					if (evt.getNewValue() == Boolean.TRUE) {
						putValue(Action.SHORT_DESCRIPTION, HIDE_TOOLTIP);
					} else {
						putValue(Action.SHORT_DESCRIPTION, SHOW_TOOLTIP);
					}
				}
			}
		});
	}
	
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		PreferenceUtils.saveToPreferences(SeqMapView.PREF_SHOW_TOOLTIP, SeqMapView.default_show_prop_tooltip, ACTION);
	}
	
	public static ToggleToolTipAction getAction() {
		return ACTION;
	}

	@Override
	public boolean isToggle() {
		return true;
	}
}
