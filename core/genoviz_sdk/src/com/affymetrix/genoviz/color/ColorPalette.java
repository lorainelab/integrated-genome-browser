package com.affymetrix.genoviz.color;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ColorPalette {
	private String backGround;
	private ColorScheme colorScheme;
	private Map<String, Color> colorMap;
	
	public ColorPalette(ColorScheme cs){
		this(cs, cs.getBackground());
	}
	
	public ColorPalette(ColorScheme cs, String bg){
		colorScheme = cs;
		backGround = bg;
		colorMap = new HashMap<String, Color>();
	}
	
	public Color getColor(String key){
		Color color = colorMap.get(key);
		if(color == null){
			if(colorScheme.getForegroundColors().size() > colorMap.size()) {
				color = Color.decode(colorScheme.getForeground(colorMap.size()));
			}else{
				color = randomColor();
			}
			
			colorMap.put(key, color);
		}
		return color;
	}

	/**
	 * http://stackoverflow.com/questions/4246351/creating-random-colour-in-java
	 * @return 
	 */
	private Color randomColor() {
		Random random = new Random();
		final float hue = random.nextFloat();
		// Saturation between 0.1 and 0.3
		final float saturation = (random.nextInt(2000) + 1000) / 10000f;
		final float luminance = 0.9f;
		return Color.getHSBColor(hue, saturation, luminance);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("<html>");
        sb.append("<span style=\"background-color: ").append(this.backGround).append("\"> &nbsp; ");
        for (String c: colorScheme.getForegroundColors()) {
			// http://www.unicode.org/charts/PDF/U2580.pdf
			// http://en.wikipedia.org/wiki/Box-drawing_character
			// Look for "Block Elements" in the above page.
			sb.append("<span style=\"color: ").append(c).append("\"> &#9608; </span>");
        }
		sb.append(" &nbsp;</span>");
 		//sb.append(" ").append(this.name()); // uncomment this to include names.
        return sb.toString();
	}
}
