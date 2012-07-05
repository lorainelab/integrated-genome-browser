
package com.affymetrix.igb.action;


import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionGroup;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.tiers.AccordionTierResizer;
import com.affymetrix.igb.tiers.TierResizer;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.event.MouseInputAdapter;

/**
 *
 * @author hiralv
 */
public class ToggleTrackResizeActionA extends SeqMapViewActionA implements PropertyChangeListener{
	private static final ToggleTrackResizeActionA ACTION = new ToggleTrackResizeActionA();
	
	public static ToggleTrackResizeActionA getAction(){
		return ACTION;
	}
	
	private final TrackResizingAction trackAjustAllAction, trackAdjustAdjacentAction;
	private final GenericActionGroup group;
	protected ToggleTrackResizeActionA() {
		super(null, null, null);
		trackAjustAllAction = new TrackResizingAction(BUNDLE.getString("adjustAllTracks"), new AccordionTierResizer(getTierMap()));
		trackAdjustAdjacentAction = new TrackResizingAction(BUNDLE.getString("adjustAdjacentTracks"), new TierResizer(getTierMap()));
		
		group = new GenericActionGroup();
		group.add(trackAjustAllAction);
		group.add(trackAdjustAdjacentAction);
	}
	
	public Iterator<GenericAction> getAllActions(){
		return group.getIterator();
	}
	
	private void addResizer(MouseInputAdapter resizer){
		getLabelMap().addMouseListener(resizer);
		getLabelMap().addMouseMotionListener(resizer);
	}
	
	private void removeResizer(MouseInputAdapter resizer){
		getLabelMap().removeMouseListener(resizer);
		getLabelMap().removeMouseMotionListener(resizer);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getSource() == group)
				return;
			
		if (evt.getPropertyName().equals(AbstractAction.SELECTED_KEY)) {
			TrackResizingAction oldSelection = (TrackResizingAction) evt.getOldValue();
			TrackResizingAction newSelection = (TrackResizingAction) evt.getNewValue();
			if(oldSelection != null){
				removeResizer(oldSelection.resizer);
			}
			if(newSelection != null){
				addResizer(newSelection.resizer);
			}
		}
	}
	
	private class TrackResizingAction extends GenericAction{
		final MouseInputAdapter resizer;
		protected TrackResizingAction(String text, MouseInputAdapter resizer) {
			super(text, null, null);
			this.resizer = resizer;
		}
		
		@Override
		public void actionPerformed(ActionEvent e){
			super.actionPerformed(e);
		}
	};
}
