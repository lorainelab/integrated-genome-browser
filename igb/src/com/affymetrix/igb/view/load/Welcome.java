package com.affymetrix.igb.view.load;

import java.awt.Font;
import java.awt.Color;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;


import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.igb.glyph.WrappedStringGlyph;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.Application;

/**
 *
 * @author hiralv
 */
public class Welcome implements ItemListener, ComponentListener{
	
	private static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
	private static final String MESSAGE = IGBConstants.BUNDLE.getString("welcomeMessage");
	private static final SeqMapView smv = Application.getSingleton().getMapView();
	private static final float FONT_SIZE = 18.0f;
	private static final Color color = Color.WHITE;
	
	private final NeoMap map;
	private final StringGlyph sg;
	
	public Welcome(){
		sg = new WrappedStringGlyph(MESSAGE);
		map = smv.getSeqMap();
		map.addComponentListener(this);
		initGlyph();
	}
	
	private void initGlyph(){
		sg.setHitable(false);
		sg.setForegroundColor(color);
		sg.setCoordBox(map.getCoordBounds());
		Font font = sg.getFont();
		font = font.deriveFont(FONT_SIZE);
		sg.setFont(font);
		map.addItem(sg);
	}
	
	public void itemStateChanged(ItemEvent evt) {
		JComboBox jb = (JComboBox) evt.getSource();
		if(jb.getSelectedItem() != null &&
				SELECT_SPECIES.equals(jb.getSelectedItem().toString())){
			sg.setVisibility(true);
		}else{
			sg.setVisibility(false);
		}
		map.updateWidget();
	}

	public void componentResized(ComponentEvent ce) { 
		if(sg.isVisible()){
			map.stretchToFit(true, true);
			map.updateWidget();
		}
	}

	public void componentMoved (ComponentEvent ce){}
	public void componentShown (ComponentEvent ce){}
	public void componentHidden(ComponentEvent ce){}

}
