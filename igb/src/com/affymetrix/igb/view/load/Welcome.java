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

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.glyph.WrappedStringGlyph;

/**
 *
 * @author hiralv
 */
public class Welcome implements ItemListener, ComponentListener{
	
	private static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
	private static final String MESSAGE = IGBConstants.BUNDLE.getString("welcomeMessage");
	final private NeoMap map;
	final private Color color;
	final StringGlyph sg;
	
	public Welcome(NeoMap map){
		this.map = map;
		this.color = Color.WHITE;
		sg = new WrappedStringGlyph(MESSAGE);
		this.map.addComponentListener(this);
		init();
	}
	
	private void init(){
		sg.setHitable(false);
		sg.setForegroundColor(color);
		sg.setCoordBox(map.getCoordBounds());
		Font font = sg.getFont();
		font = font.deriveFont(18.0f);
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

	public void componentMoved(ComponentEvent ce) {	}

	public void componentShown(ComponentEvent ce) { }

	public void componentHidden(ComponentEvent ce) { }

}
