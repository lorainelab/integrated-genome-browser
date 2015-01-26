/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.shared;

import com.affymetrix.igb.service.api.SeqSymmetryPreprocessorI;

/**
 *
 * @author dcnorris
 */
public interface PreprocessorRegistry {

    void addPreprocessor(SeqSymmetryPreprocessorI factory);

    void removePreprocessor(SeqSymmetryPreprocessorI factory);

}
