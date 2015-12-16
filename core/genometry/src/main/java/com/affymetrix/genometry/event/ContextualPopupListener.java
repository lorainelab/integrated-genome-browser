/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.event;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;
import javax.swing.JPopupMenu;

public interface ContextualPopupListener {

    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selectedItems, SeqSymmetry primarySym);
}
