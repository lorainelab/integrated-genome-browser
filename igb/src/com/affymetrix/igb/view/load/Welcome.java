package com.affymetrix.igb.view.load;

import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.glyph.BasicImageGlyph;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.TransformTierGlyph;
import com.affymetrix.igb.view.SeqMapView;

/**
 *
 * @author hiralv
 */
public class Welcome implements ItemListener, ComponentListener, SymSelectionListener{

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
												new Message("a_lyrata.gif", "A_lyrata_Apr_2011", "A. lyrata", FONT_SIZE_3, COLOR_2),
												new Message("h_sapiens.gif","H_sapiens_Feb_2009", "H. Sapiens", FONT_SIZE_3, COLOR_2),
												new Message("m_musculus.gif", "M_musculus_Jul_2007","M. Musculus", FONT_SIZE_3, COLOR_2),
												new Message("a_thaliana.gif", "A_thaliana_Jun_2009","A. Thaliana", FONT_SIZE_3, COLOR_2),
												new Message("d_melanogaster.gif", "D_melanogaster_Apr_2006", "D. Melanogaster", FONT_SIZE_3, COLOR_2),};

	private static final SeqMapView smv = Application.getSingleton().getMapView();

	private final NeoMap map;
	private final TransformTierGlyph parent;
	private final GenometryModel gmodel;

	private static final Welcome singleton = new Welcome();

	public static Welcome getWelcome(){
		return singleton;
	}

	private Welcome() {
		parent = new TransformTierGlyph(new SimpleTrackStyle("",false));
		parent.setHitable(false);
		FasterExpandPacker packer = new FasterExpandPacker();
		packer.setSpacing(5);
		parent.setPacker(packer);

		map = smv.getSeqMap();
		map.addComponentListener(this);

		gmodel = GenometryModel.getGenometryModel();

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
			
			ImageIcon ii = CommonUtils.getInstance().getIcon("images/" + SPECIES[i].image_name);
			Image image = ii.getImage();
			image = resizeImage(image, BufferedImage.TYPE_INT_RGB);
			map.prepareImage(image, map);

			BasicImageGlyph big = new BasicImageGlyph();
			big.setInfo(SPECIES[i].group);
			big.setImage(image, map);
			big.setCoords(20 * i, 0, 15, 30);
			sg.addChild(big);
			
			parent.addChild(sg);
		}

		map.addItem(parent);
		parent.pack(map.getView(), false);
		parent.setVisibility(true);
		gmodel.addSymSelectionListener(this);
	}

	public void itemStateChanged(ItemEvent evt) {
		map.removeItem(parent);
		parent.setVisibility(false);
		gmodel.removeSymSelectionListener(this);

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

	public void symSelectionChanged(SymSelectionEvent evt) {
		List<GlyphI> glyphs = map.getSelected();

		if(glyphs == null || glyphs.isEmpty())
			return;

		Object obj = glyphs.get(0).getInfo();

		if(obj == null)
			return;

		String groupStr = (String)obj;
		AnnotatedSeqGroup group = gmodel.getSeqGroup(groupStr);
		
		if(group == null){
			Application.getSingleton().setStatus(groupStr+" Not Available", true);
			return;
		}
		
		GeneralLoadView.getLoadView().initVersion(group.getID());
		gmodel.setSelectedSeqGroup(group);
		if(group.getSeqCount() > 0){
			gmodel.setSelectedSeq(group.getSeq(0));
		}
	}

	public void componentMoved (ComponentEvent ce){}
	public void componentShown (ComponentEvent ce){}
	public void componentHidden(ComponentEvent ce){}

	private static final class Message{

		final String image_name;
		final String group;
		final String str;
		final float font_size;
		final Color color;

		Message(String str, float font_size, Color color){
			this.image_name = null;
			this.group = null;
			this.str = str;
			this.font_size = font_size;
			this.color = color;
		}

		Message(String image_name, String group, String str, float font_size, Color color){
			this.image_name = image_name;
			this.group = group;
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
	
//		EfficientLabelledGlyph elg;
//		for (int i = 0; i < SPECIES.length; i++) {
//			elg = new EfficientLabelledGlyph();
//			elg.setDrawOrder(Glyph.DRAW_CHILDREN_FIRST);
//			elg.setLabelLocation(GlyphI.SOUTH);
//			elg.setSelectable(false);
//			elg.setLabel(SPECIES[i].str);
//			elg.setColor(SPECIES[i].color);
//			elg.setCoords(20 * i, 0, 15, 60);
//			
//			Font font = new Font("Monospaced", Font.PLAIN, 1);
//			font = font.deriveFont(SPECIES[i].font_size);
//			
//			
//			ImageIcon ii = CommonUtils.getInstance().getIcon("images/" + SPECIES[i].image_name);
//			Image image = ii.getImage();
//			image = resizeImage(image, BufferedImage.TYPE_INT_RGB);
//			map.prepareImage(image, map);
//
//			BasicImageGlyph big = new BasicImageGlyph();
//			//big.setInfo(gmodel.getSeqGroup(SPECIES[i].group));
//			big.setImage(image, map);
//			big.setCoords(20 * i, 0, 15, 30);
//			elg.addChild(big);
//			
//			parent.addChild(elg);
//		}
	
}
