/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.view;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.widget.tieredmap.GlyphSearchNode;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public class ProtAnnotMapTierGlyph extends MapTierGlyph {
    
    private static final Logger logger = LoggerFactory.getLogger(ProtAnnotMapTierGlyph.class);
    
    @Override
    public void addChild(GlyphI glyph, int position) {
        try {
            Field gsnField = MapTierGlyph.class.getDeclaredField("gsn");
            gsnField.setAccessible(true);
            GlyphSearchNode gsn = (GlyphSearchNode) gsnField.get(this);
            
            parentAddChild(glyph, position);
            gsn.addGlyph(glyph);
            gsnField.setAccessible(false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    @Override
    public void addChild(GlyphI glyph) {
        try {
            Field gsnField = MapTierGlyph.class.getDeclaredField("gsn");
            gsnField.setAccessible(true);
            GlyphSearchNode gsn = (GlyphSearchNode) gsnField.get(this);
            
            Field lrpField = MapTierGlyph.class.getDeclaredField("last_removed_position");
            lrpField.setAccessible(true);
            int last_removed_position = (Integer) lrpField.get(this);
            
            if (last_removed_position != -1) {
                parentAddChild(glyph, last_removed_position);

                last_removed_position = -1;
                lrpField.setInt(this, last_removed_position);
                
            } else {
                parentAddChild(glyph);
            }
            
            lrpField.setAccessible(false);
            gsn.addGlyph(glyph);
            gsnField.setAccessible(false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public void parentAddChild(GlyphI glyph, int position) {
        if (this == glyph) {
            throw new IllegalArgumentException(
                    "Illegal to add a Glyph as a child of itself!");
        }
        GlyphI prev_parent = glyph.getParent();
        if (prev_parent != null) {
            prev_parent.removeChild(glyph);
        }
        
        if (getChildren() == null) {
            resetChildren();
        }
        if (position == getChildren().size()) {
            getChildren().add(glyph);
        } else {
            getChildren().add(position, glyph);
        }
        // setParent() also calls setScene()
        glyph.setParent(this);
    }
    
    public void parentAddChild(GlyphI glyph) {
        GlyphI prev_parent = glyph.getParent();
        if (prev_parent != null) {
            prev_parent.removeChild(glyph);
        }
        if (getChildren() == null) {
            resetChildren();
        }
        getChildren().add(glyph);
        glyph.setParent(this);
    }
    
    @Override
    public void resetChildren() {
        try {
            Field childrenField = Glyph.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            childrenField.set(this, new ArrayList<GlyphI>());
            childrenField.setAccessible(false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * Remove all children of the glyph
     */
    @Override
    public void removeChildren() {
        
        try {
            
            Field gsnField = MapTierGlyph.class.getDeclaredField("gsn");
            gsnField.setAccessible(true);
            GlyphSearchNode gsn = (GlyphSearchNode) gsnField.get(this);
            
            List kids = this.getChildren();
            
            if (kids != null) {
//            for (Object kid : kids) {
//                this.removeChild((GlyphI) kid);
//            }
                Iterator iterator = kids.iterator();
                while (iterator.hasNext()) {
                    Object kid = iterator.next();
                    int last_removed_position = getChildren().indexOf((GlyphI) kid);
                    Field lrpField = MapTierGlyph.class.getDeclaredField("last_removed_position");
                    lrpField.setAccessible(true);
                    lrpField.setInt(this, last_removed_position);
                    lrpField.setAccessible(false);
                    gsn.removeGlyph((GlyphI) kid);
                    iterator.remove();
                    //this.removeChild((GlyphI) kid);
                }
            }
            gsn.removeChildren();
            // CLH: This is a hack. Instead of removing gsn,
            // I just assign a new one. Is this a massive leak???
            //
            // EEE: Yes, so I added the gsn.removeChildren() to help.
            //gsn = new GlyphSearchNode();
            gsnField.set(this, new GlyphSearchNode());
            
            gsnField.setAccessible(false);
            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
}
