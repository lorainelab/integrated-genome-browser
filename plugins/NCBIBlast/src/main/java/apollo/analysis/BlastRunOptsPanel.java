package apollo.analysis;

import com.affymetrix.igb.shared.IPrefEditorComponent;
import apollo.util.GuiUtil;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class BlastRunOptsPanel extends IPrefEditorComponent implements BlastRunOpts {

    private static final long serialVersionUID = 1;
//	private JTextField startField;
//	private JTextField endField;
    private JComboBox types;
    private JCheckBox filterLowComplexity;
    private JCheckBox filterHumanRepeats;
    private JCheckBox filterMaskLookup;
    private JTextField gapOpenCost;
    private JTextField gapExtendCost;
    private JTextField numOfHits;
//	private int start, end;

    public BlastRunOptsPanel() {
        super();
        init();
        setName("Blast Options");
    }

    @Override
    public RemoteBlastNCBI.BlastOptions getBlastOptions() {
        RemoteBlastNCBI.BlastOptions opts = new RemoteBlastNCBI.BlastOptions();
//        Style style = Config.getStyle();
//        opts.setGeneticCode(style.getGeneticCodeNumber());
        if (filterLowComplexity.isSelected()) {
            opts.setFilterLowComplexity(true);
        }
        if (filterHumanRepeats.isSelected()) {
            opts.setFilterHumanRepeats(true);
        }
        if (filterMaskLookup.isSelected()) {
            opts.setFilterMaskLookup(true);
        }
        opts.setGapOpenCost(Integer.parseInt(gapOpenCost.getText()));
        opts.setGapExtendCost(Integer.parseInt(gapExtendCost.getText()));
        opts.setNumberOfHits(Integer.parseInt(numOfHits.getText()));
//		start = Integer.parseInt(startField.getText());
//		end = Integer.parseInt(endField.getText());
        return opts;
    }

    @Override
    public RemoteBlastNCBI.BlastType getBlastType() {
        return (RemoteBlastNCBI.BlastType) types.getSelectedItem();
    }

    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = GuiUtil.makeConstraintAt(0, 0, 1);

        JLabel spacer1 = new JLabel();
        spacer1.setPreferredSize(new JCheckBox().getPreferredSize());
        add(spacer1, c);

        gapOpenCost = new JTextField(6);
        gapExtendCost = new JTextField(6);
        numOfHits = new JTextField("100", 6);

        types = new JComboBox(RemoteBlastNCBI.BlastType.values());
        types.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getItem() == RemoteBlastNCBI.BlastType.blastn) {
                    gapOpenCost.setText("5");
                    gapExtendCost.setText("2");
                } else {
                    gapOpenCost.setText("11");
                    gapExtendCost.setText("1");
                }
            }
        });
        types.setSelectedIndex(1);
        types.setSelectedIndex(0);

//		startField = new JTextField(Integer.toString(start), 6);
//		endField = new JTextField(Integer.toString(end), 6);
//		c.gridwidth = 1;
//		c.gridx = 2;
//		c.anchor = GridBagConstraints.CENTER;
//		add(new JLabel("Start"), c);
//		++c.gridx;
//		add(new JLabel("End"), c);
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.anchor = GridBagConstraints.WEST;
//		c.gridx = 1;
//		++c.gridy;
//		add(new JLabel("Genomic region"), c);
//		++c.gridx;
//		add(startField, c);
//		++c.gridx;
//		add(endField, c);
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("BLAST type"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        add(types, c);

        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        JLabel gapOpenLabel = new JLabel("Gap open cost");
        gapOpenLabel.setPreferredSize(new JLabel("Split hits that are tandemly duplicated on subject into separate hits").getPreferredSize());
        add(gapOpenLabel, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(gapOpenCost, c);

        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Gap extend cost"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(gapExtendCost, c);

        c.gridwidth = 1;
        c.gridx = 1;
        ++c.gridy;
        add(new JLabel("Number of hits"), c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(numOfHits, c);

        filterLowComplexity = new JCheckBox();
        filterHumanRepeats = new JCheckBox();
        filterMaskLookup = new JCheckBox();

        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(filterLowComplexity, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Filter out low complexity sequence"), c);

        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(filterHumanRepeats, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Filter out human repeats"), c);

        c.gridwidth = 1;
        c.gridx = 0;
        ++c.gridy;
        add(filterMaskLookup, c);
        ++c.gridx;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Filter out masked sequence"), c);

    }

    @Override
    public void refresh() {
//		throw new UnsupportedOperationException("Not supported yet.");
    }
}
