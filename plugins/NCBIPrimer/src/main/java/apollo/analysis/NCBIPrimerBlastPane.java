package apollo.analysis;

import apollo.util.GuiUtil;
import com.affymetrix.igb.shared.IPrefEditorComponent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class NCBIPrimerBlastPane extends IPrefEditorComponent implements NCBIPrimerBlastOpts {

    private static final long serialVersionUID = 1l;
    private PrimerOptsPanel primerOptsPanel;
    private PrimerCheckOptsPanel primerCheckOptsPanel;
    private PrimerPostProcessPanel primerPostProcessPanel;

    public NCBIPrimerBlastPane() {
        super();
        init();
        setName("Primer Options");
    }

    @Override
    public RemotePrimerBlastNCBI.PrimerBlastOptions getOptions() {
        RemotePrimerBlastNCBI.PrimerBlastOptions opts = new RemotePrimerBlastNCBI.PrimerBlastOptions();
        primerOptsPanel.setOptions(opts);
        primerCheckOptsPanel.setOptions(opts);
        primerPostProcessPanel.setOptions(opts);
        return opts;
    }

    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

        primerOptsPanel = new PrimerOptsPanel();
        primerOptsPanel.setBorder(BorderFactory.createTitledBorder("Primer Parameters"));
        add(primerOptsPanel, c);
        ++c.gridy;
        primerCheckOptsPanel = new PrimerCheckOptsPanel();
        primerCheckOptsPanel.setBorder(BorderFactory.createTitledBorder("Primer Pair Specificity Checking Parameters"));
        add(primerCheckOptsPanel, c);
        ++c.gridy;
        primerPostProcessPanel = new PrimerPostProcessPanel();
        primerPostProcessPanel.setBorder(BorderFactory.createTitledBorder("Post Processing Options"));
        add(primerPostProcessPanel, c);
    }

    private class PrimerOptsPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private JTextField startField;
        private JTextField endField;
        private JTextField forwardPrimerField;
        private JTextField reversePrimerField;
        private JTextField minPcrSizeField;
        private JTextField maxPcrSizeField;
        private JTextField numPrimersField;
        private JTextField minTmField;
        private JTextField optTmField;
        private JTextField maxTmField;
        private JTextField maxTmDiffField;

        public PrimerOptsPanel() {
            super();
            init();
        }

        public void setOptions(RemotePrimerBlastNCBI.PrimerBlastOptions opts) {
            if (forwardPrimerField.getText().length() > 0) {
                opts.setPrimerLeftInput(forwardPrimerField.getText());
            }
            if (reversePrimerField.getText().length() > 0) {
                opts.setPrimerLeftInput(reversePrimerField.getText());
            }
            if (minPcrSizeField.getText().length() > 0) {
                opts.setPrimerProductMin(Integer.parseInt(minPcrSizeField.getText()));
            }
            if (maxPcrSizeField.getText().length() > 0) {
                opts.setPrimerProductMax(Integer.parseInt(maxPcrSizeField.getText()));
            }
            if (numPrimersField.getText().length() > 0) {
                opts.setPrimerNumReturn(Integer.parseInt(numPrimersField.getText()));
            }
            if (minTmField.getText().length() > 0) {
                opts.setPrimerMinTm(Double.parseDouble(minTmField.getText()));
            }
            if (optTmField.getText().length() > 0) {
                opts.setPrimerOptTm(Double.parseDouble(optTmField.getText()));
            }
            if (maxTmField.getText().length() > 0) {
                opts.setPrimerMaxTm(Double.parseDouble(maxTmField.getText()));
            }
            if (maxTmDiffField.getText().length() > 0) {
                opts.setPrimerMaxDiffTm(Double.parseDouble(maxTmDiffField.getText()));
            }
//			start = Integer.parseInt(startField.getText());
//			end = Integer.parseInt(endField.getText());
        }

        private void init() {
            setLayout(new GridBagLayout());

            GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

//			startField = new JTextField(Integer.toString(start), 6);
//			endField = new JTextField(Integer.toString(end), 6);
//
//			c.gridwidth = 1;
//			c.gridx = 1;
//			c.anchor = GridBagConstraints.CENTER;
//			add(new JLabel("Start"), c);
//			++c.gridx;
//			add(new JLabel("End"), c);
//
//			c.fill = GridBagConstraints.HORIZONTAL;
//			c.anchor = GridBagConstraints.WEST;
//			c.gridx = 0;
//			++c.gridy;
//			add(new JLabel("Genomic region"), c);
//			++c.gridx;
//			add(startField, c);
//			++c.gridx;
//			add(endField, c);
            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Custom forward primer"), c);
            ++c.gridx;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            forwardPrimerField = new JTextField(10);
            add(forwardPrimerField, c);

            c.gridwidth = 1;
            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Custom reverse primer"), c);
            ++c.gridx;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            reversePrimerField = new JTextField(10);
            add(reversePrimerField, c);

            c.gridwidth = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 1;
            ++c.gridy;
            add(new JLabel("Min"), c);
            ++c.gridx;
            add(new JLabel("Max"), c);

            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("PCR product size"), c);
            ++c.gridx;
            minPcrSizeField = new JTextField("200", 6);
            add(minPcrSizeField, c);
            ++c.gridx;
            maxPcrSizeField = new JTextField("1000", 6);
            add(maxPcrSizeField, c);

            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Number of primers to return"), c);
            ++c.gridx;
            numPrimersField = new JTextField("10", 6);
            add(numPrimersField, c);

            c.gridwidth = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 1;
            ++c.gridy;
            add(new JLabel("Min"), c);
            ++c.gridx;
            add(new JLabel("Opt"), c);
            ++c.gridx;
            add(new JLabel("Max"), c);
            ++c.gridx;
            add(new JLabel("Max Tm difference"), c);

            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Primer melting temperatures (Tm)"), c);
            ++c.gridx;
            minTmField = new JTextField("57.0", 4);
            add(minTmField, c);
            ++c.gridx;
            optTmField = new JTextField("60.0", 4);
            add(optTmField, c);
            ++c.gridx;
            maxTmField = new JTextField("63.0", 4);
            add(maxTmField, c);
            ++c.gridx;
            maxTmDiffField = new JTextField("3", 4);
            add(maxTmDiffField, c);

        }
    }

    private class PrimerCheckOptsPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private JCheckBox searchSpecificPrimerCheckbox;
        private JTextField organismField;
        private JComboBox databaseComboBox;
        private JComboBox totalPrimerSpecificityMismatchComboBox;
        private JComboBox primer3endSpecificityMismatchComboBox;
        private JComboBox mismatchRegionLengthComboBox;
        private JTextField productSizeDeviationField;

        public PrimerCheckOptsPanel() {
            super();
            init();
        }

        public void setOptions(RemotePrimerBlastNCBI.PrimerBlastOptions opts) {
            opts.setSearchSpecificPrimer(searchSpecificPrimerCheckbox.isSelected());
            if (organismField.getText().length() > 0) {
                opts.setOrganism(organismField.getText());
            }
            opts.setPrimerSpecificityDatabase((RemotePrimerBlastNCBI.PrimerBlastOptions.Database) databaseComboBox.getSelectedItem());
            opts.setTotalPrimerSpecificityMismatch((Integer) totalPrimerSpecificityMismatchComboBox.getSelectedItem());
            opts.setPrimer3endSpecificityMismatch((Integer) primer3endSpecificityMismatchComboBox.getSelectedItem());
            opts.setMismatchRegionLength((Integer) mismatchRegionLengthComboBox.getSelectedItem());
            if (productSizeDeviationField.getText().length() > 0) {
                opts.setProductSizeDeviation(Integer.parseInt(productSizeDeviationField.getText()));
            }
        }

        private void init() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

            add(new JLabel("Specificity check"), c);
            ++c.gridx;
            c.gridwidth = GridBagConstraints.REMAINDER;
            searchSpecificPrimerCheckbox = new JCheckBox("Enable search for primer pairs specific to the intended PCR template", true);
            add(searchSpecificPrimerCheckbox, c);

            c.gridwidth = 1;
            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Organism"), c);
            ++c.gridx;
            c.gridwidth = GridBagConstraints.REMAINDER;
            organismField = new JTextField(20);
            add(organismField, c);

            c.gridwidth = 1;
            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Database"), c);
            c.gridwidth = GridBagConstraints.REMAINDER;
            ++c.gridx;
            databaseComboBox = new JComboBox(RemotePrimerBlastNCBI.PrimerBlastOptions.Database.values());
            add(databaseComboBox, c);

            c.gridwidth = 1;
            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Primer specificity stringency"), c);
            ++c.gridx;
            c.insets.left = 5;
            add(new JLabel("At least"), c);
            c.insets.left = 0;
            ++c.gridx;
            totalPrimerSpecificityMismatchComboBox = new JComboBox(new Integer[]{1, 2, 3, 4});
            totalPrimerSpecificityMismatchComboBox.setSelectedIndex(1);
            add(totalPrimerSpecificityMismatchComboBox, c);
            c.gridwidth = GridBagConstraints.REMAINDER;
            ++c.gridx;
            add(new JLabel("total mismatches to unintended targets, including"), c);
            c.gridwidth = 1;
            c.gridx = 1;
            ++c.gridy;
            c.insets.left = 5;
            add(new JLabel("at least"), c);
            c.insets.left = 0;
            ++c.gridx;
            primer3endSpecificityMismatchComboBox = new JComboBox(new Integer[]{1, 2, 3, 4});
            primer3endSpecificityMismatchComboBox.setSelectedIndex(1);
            add(primer3endSpecificityMismatchComboBox, c);
            ++c.gridx;
            add(new JLabel("mismatches within the last"), c);
            ++c.gridx;
            mismatchRegionLengthComboBox = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
            mismatchRegionLengthComboBox.setSelectedIndex(4);
            add(mismatchRegionLengthComboBox, c);
            ++c.gridx;
            add(new JLabel("bps at the 3' end"), c);

            c.gridx = 0;
            ++c.gridy;
            add(new JLabel("Misprimed product size deviation"), c);
            c.gridwidth = GridBagConstraints.REMAINDER;
            ++c.gridx;
            productSizeDeviationField = new JTextField("1000", 4);
            c.insets.left = 5;
            add(productSizeDeviationField, c);
            c.insets.left = 0;
        }
    }

    private class PrimerPostProcessPanel extends JPanel {

        private static final long serialVersionUID = 1l;
        private JCheckBox removePairsNotInExonsCheckBox;

        public PrimerPostProcessPanel() {
            super();
            init();
        }

        public void setOptions(RemotePrimerBlastNCBI.PrimerBlastOptions opts) {
            opts.setRemovePairsNotInExons(removePairsNotInExonsCheckBox.isSelected());
        }

        private void init() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

            removePairsNotInExonsCheckBox = new JCheckBox("Remove pairs where each primer is not fully contained within distinct exons");
            if (true) {
                removePairsNotInExonsCheckBox.setEnabled(false);
            }
            add(removePairsNotInExonsCheckBox, c);
        }
    }

    @Override
    public void refresh() {
//		throw new UnsupportedOperationException("Not supported yet.");
    }
}
