package org.lorainelab.igb.image.exporter;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 *
 * @author dcnorris
 */
public class PreviewLabel extends JLabel {

    BufferedImage bufferedImage;

    public PreviewLabel() {
        super();
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bufferedImage != null) {
            Dimension aspectRatioMaintainedDim = GraphicsUtil.getScaledDimension(new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()), new Dimension(getWidth(), getHeight()));
            int width = new Double(aspectRatioMaintainedDim.getWidth()).intValue();
            int height = new Double(aspectRatioMaintainedDim.getHeight()).intValue();
            g.drawImage(new ImageIcon(GraphicsUtil.getScaledImage(bufferedImage, width, height)).getImage(), 0, 0, width, height, this);
        }
    }
}
