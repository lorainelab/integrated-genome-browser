/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.shared;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.AminoAcid;
import com.affymetrix.igb.util.ResidueColorHelper;
import java.awt.Color;

/**
 *
 * @author jeckstei
 */
@Component(provide = ResidueColorService.class)
public class ResidueColorService {

    public Color getDefaultColor(AminoAcid aminoAcid) {
        switch (aminoAcid) {
            case Alanine:
                return ResidueColorHelper.DEFAULT_A_COLOR;
            case Cysteine:
                return ResidueColorHelper.DEFAULT_C_COLOR;
            case Threonine:
                return ResidueColorHelper.DEFAULT_T_COLOR;
            case Glycine:
                return ResidueColorHelper.DEFAULT_G_COLOR;
            default:
                return Color.gray;
        }
    }

}
