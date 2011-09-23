package com.affymetrix.igb.view.load;

import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import com.affymetrix.genometryImpl.style.DefaultTrackStyle;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.glyph.BasicImageGlyph;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.TransformTierGlyph;
import com.affymetrix.igb.util.IGBUtils;
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
	private static final float FONT_SIZE_3 = 12.0f;
	
	private static final Message MESSAGES[] = new Message[]{
												new Message("Welcome to", FONT_SIZE_1, COLOR_1), 
												new Message("Integrated Genome Browser", FONT_SIZE_1, COLOR_2),
												new Message("To get started, choose a species and genome version.", FONT_SIZE_2, COLOR_1)};
	private static final Message SPECIES[] = new Message[]{
												new Message("a_lyrata.gif", "A. lyrata", FONT_SIZE_3, COLOR_2),
												new Message("h_sapiens.gif", "H. Sapiens", FONT_SIZE_3, COLOR_2),
												new Message("m_musculus.gif", "M. Musculus", FONT_SIZE_3, COLOR_2),
												new Message("a_thaliana.gif", "A. Thaliana", FONT_SIZE_3, COLOR_2),
												new Message("d_melnogaster.gif", "D. Melnogaster", FONT_SIZE_3, COLOR_2),};
	
	private static final SeqMapView smv = Application.getSingleton().getMapView();
	
	private final NeoMap map;
	private final TransformTierGlyph parent;
	
	public Welcome() {
		parent = new TransformTierGlyph(new DefaultTrackStyle());
		parent.setHitable(false);
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

			sg.setCoords(0, 0, map_length, 10);
			Font font = sg.getFont();
			font = font.deriveFont(MESSAGES[i].font_size);
			sg.setFont(font);
			parent.addChild(sg);
		}
		
		for (int i = 0; i < SPECIES.length; i++) {
			sg = new StringGlyph(SPECIES[i].str);
			sg.setVerticalPlacement(StringGlyph.BELOW);
			sg.setHitable(false);
			sg.setForegroundColor(SPECIES[i].color);
			sg.setCoords(20 * i, 0, 15, 40);
			Font font = sg.getFont();
			font = font.deriveFont(SPECIES[i].font_size);
			sg.setFont(font);
			
			ImageIcon ii = IGBUtils.getIcon(SPECIES[i].image_name);
			Image image = ii.getImage();
			image = resizeImage(image, BufferedImage.TYPE_INT_RGB);
			map.prepareImage(image, map);
			
			BasicImageGlyph big = new BasicImageGlyph();
			big.setSelectable(false);
			big.setImage(image, map);
			big.setCoords(20 * i, 0, 15, 30);
			sg.addChild(big);
			
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
		
		final String image_name;
		final String str;
		final float font_size;
		final Color color;
		
		Message(String str, float font_size, Color color){
			this.image_name = null;
			this.str = str;
			this.font_size = font_size;
			this.color = color;
		}
		
		Message(String image_name, String str, float font_size, Color color){
			this.image_name = image_name;
			this.str = str;
			this.font_size = font_size;
			this.color = color;
		}
	}
	
	private static Image resizeImage(Image image, int type) {
		BufferedImage resizedImage = new BufferedImage(100, 100, type);
		java.awt.Graphics2D g = resizedImage.createGraphics();
		g.drawImage(image, 0, 0, 100, 100, null);
		g.dispose();

		return resizedImage;
	}
}
