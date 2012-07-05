package com.affymetrix.igb.action;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JDialog;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.*;

/**
 * note - this class contains an instance of SeqMapView. For now, there
 * is just one instance using the regular SeqMapView, no instance for
 * AltSpliceView
 */
public abstract class ChangeColorActionA extends SeqMapViewActionA implements ParameteredAction {
	protected static final java.awt.Color DEFAULT_COLOR = javax.swing.UIManager.getColor("Button.background");
	private static final long serialVersionUID = 1L;
	private boolean iterateMultigraph = true;
	
	protected ChangeColorActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor();
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	protected abstract void setChooserColor(JColorChooser chooser, ITrackStyleExtended style);
	protected abstract void setStyleColor(Color color, ITrackStyleExtended style);
	protected void setGraphColor(AbstractGraphGlyph gg, Color color) { }

	protected final void iterateMultiGraph(boolean iterate){
		iterateMultigraph = iterate;
	}
	
	private void changeColor() {
		if (getSeqMapView().getAllSelectedTiers().isEmpty()) {
			return;
		}

		final JColorChooser chooser = new JColorChooser();

		ITrackStyleExtended style_0 = ((ViewModeGlyph)getSeqMapView().getAllSelectedTiers().get(0)).getAnnotStyle();
		if (style_0 != null) {
			setChooserColor(chooser, style_0);
		}

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				performAction(chooser.getColor());
				getSeqMapView().getSeqMap().updateWidget();
			}

		};

		JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
				"Pick a Color",
				true, //modal
				chooser,
				al, //OK button handler
				null); //no CANCEL button handler
		dialog.setVisible(true);

	}
	
	private void changeColor(Color color){
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<ViewModeGlyph> vgList = (List)getSeqMapView().getAllSelectedTiers();
		if (!vgList.isEmpty()) {
			for (ViewModeGlyph vg : vgList) {
				if (iterateMultigraph && vg instanceof MultiGraphGlyph && vg.getChildren() != null) {
					for (GlyphI child : vg.getChildren()) {
						if (child instanceof ViewModeGlyph) {
							ITrackStyleExtended style = ((ViewModeGlyph)child).getAnnotStyle();
							if (style != null) {
								setStyleColor(color, style);
							}
						}
					}
				}else{
					ITrackStyleExtended style = vg.getAnnotStyle();
					if (style != null) {
						setStyleColor(color, style);
					}	
				}
				
			}
		}
	}
	
	@Override
	public void performAction(Object parameter){
		if(parameter.getClass() != Color.class)
			return;
		
		changeColor((Color)parameter);
	}

	@Override
	public int getOrdinal() {
		return -6000000;
	}
}
