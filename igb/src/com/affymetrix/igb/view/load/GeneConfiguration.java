/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import be.pwnt.jflow.Configuration;
import be.pwnt.jflow.Shape;
import com.affymetrix.common.CommonUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
 * [image file][tab][Data set to load upon click][tab][Name to display on tag]
 * An example is next:<br>
 * a_lyrata.png	A_lyrata_Apr_2011	A. lyrata
 * 
 * @author jfvillal
 */
public class GeneConfiguration extends Configuration {
	
	private static final Color COLOR_1 = Color.WHITE;
	private static final Color COLOR_2 = new Color( 0x88588a );//Color.YELLOW;
	private static final float FONT_SIZE_1 = 36.0f;
	private static final float FONT_SIZE_2 = 22.0f;
	private static final float FONT_SIZE_3 = 12.0f;
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
	
	public GeneConfiguration( ) {
		BufferedReader stream = null;
		try {
			//load the messge class from a configuration file.
			List<Message> list = new ArrayList<Message>();
			//this is at $IGB_SRC/common/rerources and image paths are relative to $IGB_SRC/common/images/
			URL config_file_url = CommonUtils.class.getClassLoader().getResource("display_species.txt");
			stream = new BufferedReader( 
							 new InputStreamReader( 
								 new DataInputStream(  
										 config_file_url.openStream()
								 ) 
							 )
);			String line = "";
			try {
				while( (line = stream.readLine() ) != null){
					if( line.startsWith("#")){
						continue;
					}
					String[] vals = line.split("\t");
					list.add(new Message( vals[0], vals[1], vals[2], FONT_SIZE_3 , COLOR_2 ));
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			DisplaySpecies = new Message[list.size()];
			for( int i = 0; i < list.size() ; i++){
				DisplaySpecies[i] =  list.get(i);
			}
			
			
			this.activeShapeBorderColor = Color.white ;
			this.framesPerSecond=30;
			shapes = new Shape[DisplaySpecies.length];
			for (int i = 0; i < shapes.length; i++) {
				
				try {
					URL url = CommonUtils.class.getClassLoader().getResource("images/" + DisplaySpecies[i].image_name);
					
					if( url == null){
						url = CommonUtils.class.getClassLoader().getResource("images/default.png");
					}
				
					BufferedImage img = ImageIO.read(url);
					Graphics2D g = img.createGraphics();
					
					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
								RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
					//make a black background to hold the label
					//g.setColor(new Color( 0x000000 ));
					//g.fillRect( 0 , img.getHeight() - 40 , img.getWidth(), 40);
					
					//TODO find cross-platform font
					Font f = new Font("Arial", Font.PLAIN, 30);
					//ImageIO.write( img, "png", new File("saved.png") );
					FontMetrics metrics = g.getFontMetrics(f);
					g.setColor(new Color( 0xd4d4d4 ));
					g.setFont(f );
					int num = metrics.stringWidth(DisplaySpecies[i].str);
					/**
					 * create a soft shaddow around the text in case the picture has 
					 * a white background
					 */
					g.setColor(new Color( 0x000000 ));
					g.drawString( DisplaySpecies[i].str, img.getWidth()/2 - num/2 - 2  , img.getHeight() - 22);
					for( int e = 0 ; e < 10; e++){
						for( int k = 10; k < img.getWidth() - 10; k++){
							for( int j = img.getHeight() - 44 ; j < img.getHeight() - 10; j++){
								Color colt  = new Color(img.getRGB( k, j-1));
								Color colb  = new Color(img.getRGB( k, j+1));
								Color coll  = new Color(img.getRGB( k-1, j));
								Color colr  = new Color(img.getRGB( k+1, j));
								Color c = new Color(img.getRGB(k,  j));
								float red = ((float)(colt.getRed() + colb.getRed() + coll.getRed() + colr.getRed() + c.getRed()) / 5.0f)/255.0f;
								float green = ((float)(colt.getGreen() + colb.getGreen() + coll.getGreen() + colr.getGreen() + c.getGreen()) / 5.0f )/255.0f;
								float blue = ((float)(colt.getBlue() + colb.getBlue() + coll.getBlue() + colr.getBlue() + c.getBlue()) / 5.0f ) /255.0f;
								Color n_col = new Color( red, green, blue);
								g.setColor( n_col );
								g.fillRect(k, j, 1, 1);
							}
						}
					}
					
					//draw the label
					g.setColor(new Color( 0xFFFFFF ));
					g.drawString( DisplaySpecies[i].str, img.getWidth()/2 - num/2  , img.getHeight() - 20);
					
					CargoPicture n = new CargoPicture(img);
					n.setCargo( DisplaySpecies[i].group);
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
	 * Handles the information necesary to presente a data set on the welcome screen 
	 */
	private static final class Message{
		/**
		 * The name of the image to be loaded for this icon. <br>
		 * <br>
		 * Notes about the image.
		 * The image provided should be a 266 wide by 400 height picture (266x400)
		 * "full bleed" ( no frame )
		 * PNG format is preffered.
		 */
		final String image_name;
		/**
		 * This is the data set name.  This dataset should be present in 
		 * one of the default data sources. <br>
		 * <br>
		 * Example name:  A_lyrata_Apr_2011
		 */
		final String group;
		/**
		 * This is the name that is displayed at the bottom of the image
		 * icon
		 */
		final String str;
		/**
		 * Font size (not used on the cover flow version of the welcome screen
		 */
		final float font_size;
		/**
		 * Color (not used on the cover flow version of the welcome screen
		 */
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
}
