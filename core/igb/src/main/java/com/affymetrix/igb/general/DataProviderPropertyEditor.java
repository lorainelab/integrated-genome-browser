package com.affymetrix.igb.general;

import javax.swing.JFrame;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class DataProviderPropertyEditor extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(DataProviderPropertyEditor.class);

    public DataProviderPropertyEditor() {

    }

    private void initializeFrame() {
        setName("Add/Edit Data Source");
        setTitle("Add/Edit Data Source");
        setSize(400, 300);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void initializeLayout() {
        this.setLayout(new MigLayout("fill", "", ""));
    }

}
