package com.affymetrix.igb.view.load;

import java.awt.Font;
import java.awt.Color;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;

import com.affymetrix.genometryImpl.style.DefaultTrackStyle;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.bioviews.ViewI;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;

/**
 *
 * @author hiralv
 */
public class Welcome implements ItemListener, ComponentListener{
	
	private static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
	private static final Color COLOR_1 = Color.WHITE;
	private static final Color COLOR_2 = Color.YELLOW;
	private static final float FONT_SIZE_1 = 36.0f;
	private static final float FONT_SIZE_2 = 22.0f;
	
	private static final Message MESSAGES[] = new Message[]{new Message("", FONT_SIZE_1, COLOR_1),
												new Message("Welcome to", FONT_SIZE_1, COLOR_1), 
												new Message("Integrated Genome Browser", FONT_SIZE_1, COLOR_2),
												new Message("To get started, choose a species and genome version.", FONT_SIZE_2, COLOR_1)};
	
	private static final SeqMapView smv = Application.getSingleton().getMapView();

	private final NeoMap map;
	private final TierGlyph parent;
	
	public Welcome() {
		parent = new TierGlyph(new DefaultTrackStyle()) {

			@Override
			public void pack(ViewI view, boolean manual) {
				packer.pack(this, view, manual);
				Rectangle2D.Double mbox = scene.getCoordBox();
				Rectangle2D.Double cbox = this.getCoordBox();
				this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);

			}
		};
		FasterExpandPacker packer = new FasterExpandPacker();
		packer.setSpacing(5);
		parent.setPacker(packer);
		
		map = smv.getSeqMap();
		map.addComponentListener(this);
		initGlyph();
	}
	
	private void initGlyph(){
		StringGlyph sg;
		int map_length = map.getMapRange()[1] - map.getMapRange()[0];
	
		for (int i = 0; i < MESSAGES.length; i++) {
			sg = new StringGlyph(MESSAGES[i].str);
			sg.setHitable(false);
			sg.setForegroundColor(MESSAGES[i].color);
			int l = sg.getString().length();
			sg.setCoords((map_length - l) / 2, 0, l, 10);
			Font font = sg.getFont();
			font = font.deriveFont(MESSAGES[i].font_size);
			sg.setFont(font);
			parent.addChild(sg);
		}
				
		map.addItem(parent);
		parent.pack(map.getView(), false);
		parent.setVisibility(true);
	}
	
	public void itemStateChanged(ItemEvent evt) {
		map.removeItem(parent);
		parent.setVisibility(false);
		
		JComboBox jb = (JComboBox) evt.getSource();
		if(jb.getSelectedItem() != null &&
				SELECT_SPECIES.equals(jb.getSelectedItem().toString())){
			initGlyph();
		}
		
		map.updateWidget();
	}

	public void componentResized(ComponentEvent ce) { 
		if(parent.isVisible()){
			map.stretchToFit(true, true);
			map.updateWidget();
		}
	}

	public void componentMoved (ComponentEvent ce){}
	public void componentShown (ComponentEvent ce){}
	public void componentHidden(ComponentEvent ce){}

	private static final class Message{
		
		final String str;
		final float font_size;
		final Color color;
		
		Message(String str, float font_size, Color color){
			this.str = str;
			this.font_size = font_size;
			this.color = color;
		}
		
	}
}
