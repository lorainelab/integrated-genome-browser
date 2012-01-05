/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import be.pwnt.jflow.Configuration;
import be.pwnt.jflow.Shape;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Loads pictures into the Cover Flow Welcome screen.
 * 
 * This configuration class can control multiple setting on the cover flow component
 * Refer to the parent class for more information.
 * 
 * The class reads a resource file at $IGB_HOME/common/display_species.txt 
 * to configure the data sets displayed on the welcome screen.<br>
 * The file should be of the form<br>
 * [image file][tab][Data set to load upon click][tab][Name to display on tag][tab][color]
 * An example is next:<br>
 * a_lyrata.png	A_lyrata_Apr_2011	A. lyrata #000000
 * 
 * @author jfvillal
 */
public class GeneConfiguration extends Configuration {
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final Color COLOR_1 = Color.WHITE;
	private static final Color COLOR_2 = new Color(0xfffb86);//Color.YELLOW;
	private static final float FONT_SIZE_1 = 36.0f;
	private static final float FONT_SIZE_2 = 22.0f;
	private static final float FONT_SIZE_3 = 12.0f;
	public final static int THUMB_WIDTH = 250;
	public final static int THUMB_HEIGHT = 250;
	/**
	 * * TODO:  the list of data sets should be loaded from a resource file.
	 */
	private Message DisplaySpecies[];
	/*= new Message[]{
	new Message("a_lyrata.png", "A_lyrata_Apr_2011", "A. lyrata", FONT_SIZE_3, COLOR_2),
	new Message("h_sapiens.png","H_sapiens_Feb_2009", "H. Sapiens", FONT_SIZE_3, COLOR_2),
	new Message("m_musculus.png", "M_musculus_Jul_2007","M. Musculus", FONT_SIZE_3, COLOR_2),
	new Message("a_thaliana.png", "A_thaliana_Jun_2009","A. Thaliana", FONT_SIZE_3, COLOR_2),
	new Message("d_melanogaster.png", "D_melanogaster_Apr_2006", "D. Melanogaster", FONT_SIZE_3, COLOR_2),
	};*/

	public GeneConfiguration() {
		String os = System.getProperty("os.name");
		this.zoomFactor = 0.1;
		this.zoomScale = 1.0;
		this.shapeRotation = 0.0;
		this.shapeSpacing = 1.2 / 3.0;
		this.shadingFactor = 0;
		this.reflectionOpacity = 0.0;

		/*if(os.equals("Mac OS X") || os.equals("Mac OS") ){
		this.SlowSystem = true;
		this.reflectionOpacity = 0.0;
		this.highQuality = false;
		}else{*/
		this.SlowSystem = false;
		this.highQuality = true;
		//}
		BufferedReader stream = null;
		try {
			//load the messge class from a configuration file.
			List<Message> list = new ArrayList<Message>();
			//this is at $IGB_SRC/common/rerources and image paths are relative to $IGB_SRC/common/images/
			URL config_file_url = CommonUtils.class.getClassLoader().getResource("display_species.txt");
			stream = new BufferedReader(
					new InputStreamReader(
					new DataInputStream(
					config_file_url.openStream())));
			String line = "";
			try {
				while ((line = stream.readLine()) != null) {
					if (line.startsWith("#")) {
						continue;
					}
					
					//allocate array values to check display_species.txt is 
					//correctly formated.					
					String genome = line;
					String imageLabel = SpeciesLookup.getSpeciesName(genome);
					//tripple check no null message is being inserted into the list.
					Message m = new Message(genome, imageLabel, FONT_SIZE_3);
					if (m != null) {
						list.add(m);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				System.err.println("The display_species.txt may not comply with specification.  make sure it has \n"
						+ "[image file][tab][Data set to load upon click][tab][Name to display on tag][tab][color (example #122322]");
				e.printStackTrace();
			}
			DisplaySpecies = new Message[list.size()];
			for (int i = 0; i < list.size(); i++) {
				DisplaySpecies[i] = list.get(i);
			}


			this.activeShapeBorderColor = Color.white;
			this.framesPerSecond = 30;
			shapes = new Shape[DisplaySpecies.length];
			for (int i = 0; i < shapes.length; i++) {

				try {
					URL url = CommonUtils.class.getClassLoader().getResource("images/" + DisplaySpecies[i].genomeName);

					if (url == null) {
						url = CommonUtils.class.getClassLoader().getResource("images/default.png");
					}

					BufferedImage img = ImageIO.read(url);
					Graphics2D g = img.createGraphics();

					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
							RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
					Font f = new Font("Sans Serif", Font.BOLD, 18);
					//ImageIO.write( img, "png", new File("saved.png") );
					FontMetrics metrics = g.getFontMetrics(f);
					g.setColor(new Color(0xd4d4d4));
					g.setFont(f);
					int num = metrics.stringWidth(DisplaySpecies[i].str);
					
					try {
						g.setColor(Color.BLACK);
						g.fill(new Rectangle2D.Double(0, img.getHeight() - 20, img.getWidth(), metrics.getHeight()+3));
						//draw the label
						g.setColor(COLOR_2);
						g.drawString(DisplaySpecies[i].str, img.getWidth() / 2 - num / 2, img.getHeight() - 4);
					} catch (Exception x) {
					} // ignore NPE
					CargoPicture n = null;
					if (!this.SlowSystem) {
						n = new CargoPicture(img);
					} else {
						n = new CargoPicture(scaleImage(img, 10));
					}
					n.setCargo(DisplaySpecies[i].genomeName);
					shapes[i] = n;

				} catch (IOException ex) {
					Logger.getLogger(GeneConfiguration.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(GeneConfiguration.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				stream.close();
			} catch (IOException ex) {
				Logger.getLogger(GeneConfiguration.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * from http://stackoverflow.com/questions/1324106/jai-change-jpeg-resolution
	 * @param sourceImage
	 * @param scaledWidth
	 * @return 
	 */
	BufferedImage scaleImage(BufferedImage sourceImage, int scaledWidth) {
		float scale = scaledWidth / (float) sourceImage.getWidth();
		int scaledHeight = (int) (sourceImage.getHeight() * scale);
		Image scaledImage = sourceImage.getScaledInstance(
				scaledWidth,
				scaledHeight,
				Image.SCALE_AREA_AVERAGING);

		BufferedImage bufferedImage = new BufferedImage(
				scaledImage.getWidth(null),
				scaledImage.getHeight(null),
				BufferedImage.TYPE_INT_RGB);
		Graphics g = bufferedImage.createGraphics();
		g.drawImage(scaledImage, 0, 0, null);
		g.dispose();

		return bufferedImage;
	}

	public double getGrayScale(Color col) {
		int g = col.getGreen();
		int b = col.getBlue();
		int r = col.getRed();
		double gray_scale = (double) (g + r + b) / 3.0;
		return gray_scale;
	}

	/**
	 * Handles the information necesary to presente a data set on the welcome screen 
	 */
	private static final class Message {

		/**
		 * This is the data set name.  This dataset should be present in 
		 * one of the default data sources. <br>
		 * <br>
		 * Example name:  A_lyrata_Apr_2011
		 */
		final String genomeName;
		/**
		 * This is the name that is displayed at the bottom of the image
		 * icon
		 */
		final String str;
		/**
		 * Font size (not used on the cover flow version of the welcome screen
		 */
		final float font_size;
		
		Message(String str, float font_size) {
			this.genomeName = null;
			this.str = str;
			this.font_size = font_size;
		}

		Message(String image_name, String str, float font_size) {
			this.genomeName = image_name;
			this.str = str;
			this.font_size = font_size;
		}
	}
}
