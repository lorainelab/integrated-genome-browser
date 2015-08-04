/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.view;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.widget.tieredmap.GlyphSearchNode;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.lang.reflect.Field;
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
