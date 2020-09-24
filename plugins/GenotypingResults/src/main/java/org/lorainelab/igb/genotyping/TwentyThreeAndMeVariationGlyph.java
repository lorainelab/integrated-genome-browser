package org.lorainelab.igb.genotyping;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.EfficientSolidGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TwentyThreeAndMeVariationGlyph extends EfficientSolidGlyph {
    private static final boolean OUTLINE_PIXELBOX = true;
    private static final boolean DEBUG_OPTIMIZED_FILL = false;
    static Rectangle2D.Double scratch_cbox = new Rectangle2D.Double();
    static final int maxCharYpix = 40;
    static final int maxCharXpix = 30;
    static final int minCharYpix = 5;
    static final int minCharXpix = 4;
    static final Font[] ypix2fonts = new Font[41];
    static final Font[] xpix2fonts = new Font[31];
    static final int pixelSeparation = 4;
    protected boolean show_label = true;
    protected String label;
    protected int label_loc = 4;
    protected static Font baseFont;

    public TwentyThreeAndMeVariationGlyph() {
    }

    public static void setBaseFont(Font base_fnt) {
        baseFont = base_fnt;
        int pntcount = 3;

        while(true) {
            Font smaller_font = base_fnt.deriveFont((float)pntcount);
            FontMetrics fm = GeneralUtils.getFontMetrics(smaller_font);
            int text_width = fm.stringWidth("G");
            int text_height = fm.getAscent();
            if (text_width > 30 || text_height > 40) {
                smaller_font = null;

                int i;
                for(i = 0; i < xpix2fonts.length; ++i) {
                    if (xpix2fonts[i] != null) {
                        smaller_font = xpix2fonts[i];
                    } else {
                        xpix2fonts[i] = smaller_font;
                    }
                }

                smaller_font = null;

                for(i = 0; i < ypix2fonts.length; ++i) {
                    if (ypix2fonts[i] != null) {
                        smaller_font = ypix2fonts[i];
                    } else {
                        ypix2fonts[i] = smaller_font;
                    }
                }

                return;
            }

            xpix2fonts[text_width] = smaller_font;
            ypix2fonts[text_height] = smaller_font;
            ++pntcount;
        }
    }

    public void drawTraversal(ViewI view) {
        Rectangle pixelbox = view.getScratchPixBox();
        view.transformToPixels(this.getCoordBox(), pixelbox);
        if (this.withinView(view) && this.isVisible()) {
            if (pixelbox.width > 3 && pixelbox.height > 3) {
                super.drawTraversal(view);
            } else if (this.isSelected()) {
                this.drawSelected(view);
            } else {
                this.fillDraw(view);
            }
        }

    }

    public void fillDraw(ViewI view) {
        super.draw(view);
        Rectangle pixelbox = view.getScratchPixBox();
        Graphics g = view.getGraphics();
        g.setColor(this.getBackgroundColor());
        if (this.show_label) {
            Rectangle2D.Double cbox = this.getCoordBox();
            scratch_cbox.x = cbox.x;
            scratch_cbox.width = cbox.width;
            if (this.label_loc == 4) {
                scratch_cbox.y = cbox.y + cbox.height / 2.0D;
                scratch_cbox.height = cbox.height / 2.0D;
            } else if (this.label_loc == 5) {
                scratch_cbox.y = cbox.y;
                scratch_cbox.height = cbox.height / 2.0D;
            }

            view.transformToPixels(scratch_cbox, pixelbox);
        } else {
            view.transformToPixels(this.getCoordBox(), pixelbox);
        }

        optimizeBigRectangleRendering(view, pixelbox);
        if (pixelbox.width < 1) {
            pixelbox.width = 1;
        }

        if (pixelbox.height < 1) {
            pixelbox.height = 1;
        }
        //g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    }

    public void draw(ViewI view) {
        super.draw(view);
        Rectangle pixelbox = view.getScratchPixBox();
        Graphics g = view.getGraphics();
        view.transformToPixels(this.getCoordBox(), pixelbox);
        int original_pix_width = pixelbox.width;
        if (pixelbox.width == 0) {
            pixelbox.width = 1;
        }

        if (pixelbox.height == 0) {
            pixelbox.height = 1;
        }

        Rectangle compbox = view.getComponentSizeRect();
        if (pixelbox.x < compbox.x || pixelbox.x + pixelbox.width > compbox.x + compbox.width) {
            pixelbox = pixelbox.intersection(compbox);
        }

        g.setColor(Color.yellow);
        BufferedImage folderImage = null;
        try {
            File folderInput = new File("C:\\Users\\srish\\Desktop\\a.jpg");
            folderImage = ImageIO.read(folderInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
        g.drawImage(folderImage,pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height,null);
        g.setColor(this.getBackgroundColor());
        if (this.show_label) {
            if (this.label != null && this.label.length() != 0) {
                int xpix_per_char = original_pix_width / this.label.length();
                int ypix_per_char = pixelbox.height / 2 - 4;
                if (xpix_per_char >= 4 && ypix_per_char >= 5) {
                    if (xpix_per_char > 30) {
                        xpix_per_char = 30;
                    }

                    if (ypix_per_char > 40) {
                        ypix_per_char = 40;
                    }

                    Font xmax_font = xpix2fonts[xpix_per_char];
                    Font ymax_font = ypix2fonts[ypix_per_char];
                    Font chosen_font = xmax_font.getSize() < ymax_font.getSize() ? xmax_font : ymax_font;
                    g.setFont(chosen_font);
                    FontMetrics fm = g.getFontMetrics();
                    int text_width = fm.stringWidth(this.label);
                    int text_height = fm.getAscent();
                    if (text_width <= pixelbox.width && text_height <= 4 + pixelbox.height / 2) {
                        int xpos = pixelbox.x + pixelbox.width / 2 - text_width / 2;
                        if (this.label_loc == 4) {
                            g.drawString(this.label, xpos, pixelbox.y + pixelbox.height / 2 - 4 - 2);
                        } else if (this.label_loc == 5) {
                            g.drawString(this.label, xpos, pixelbox.y + pixelbox.height / 2 + text_height + 4 - 1);
                        }
                    }
                }

            }
        }
    }

    public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
        return this.isVisible() && coord_hitbox.intersects(this.getCoordBox());
    }

    public void setLabelLocation(int loc) {
        this.label_loc = loc;
    }

    public void setShowLabel(boolean b) {
        this.show_label = b;
    }

    public int getLabelLocation() {
        return this.label_loc;
    }

    public boolean getShowLabel() {
        return this.show_label;
    }

    public void setLabel(String str) {
        this.label = str;
    }

    public String getLabel() {
        return this.label;
    }

    static {
        setBaseFont(new Font("Monospaced", 0, 1));
    }
}