/**
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 */
package com.affymetrix.sequenceviewer;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.comparator.SeqSpanComparator;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.datamodel.NASequence;
import com.affymetrix.genoviz.datamodel.Translatable;
import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.JRPMenu;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.util.Selection;
import com.affymetrix.genoviz.widget.NeoSeq;
import com.affymetrix.igb.osgi.service.IGBService;
import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewI;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.sequenceviewer.actions.ExitSeqViewerAction;
import com.affymetrix.sequenceviewer.actions.ExportFastaSequenceAction;
import com.affymetrix.sequenceviewer.actions.ExportSequenceViewerAction;
import com.affymetrix.sequenceviewer.actions.SelectAllInSeqViewerAction;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JToggleButton;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public abstract class AbstractSequenceViewer implements ActionListener, WindowListener, ItemListener, MenuListener {

    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sequenceviewer");
    private static final String PREF_COLOR_SCHEME = "Sequenceviewer Color Scheme";
    private SeqMapViewI seqmapview;
    DecimalFormat comma_format = new DecimalFormat("#,###.###");
    private NeoSeq seqview;
    private JFrame mapframe;
    private int COMMA = 0;
    private GenometryModel gm = GenometryModel.getInstance();
    private String version = "";
    public SeqSymmetry residues_sym;
    BioSeq aseq;
    boolean isGenomicRequest;
    private final int INITIAL_NUMBER_OF_RESIDUES = 50;
    private final int INITIAL_NUMBER_OF_LINES = 10;
    private final int OFFSET = 3;
    String errorMessage = null;
    private int cdsMax = -1;
    private int cdsMin = -1;
    private int txMin = -1;
    private String title = null;
    private boolean showcDNASwitch = false;
    private String[] buttonText = new String[]{"Show cDNA", "Show Genomic"};
    private String[] multipleSelection = new String[]{"Concatenate", "Show Genomic"};
    private String[] buttonNumbering = new String[]{"Internal Numbering", "Genomic Numbering"};
    private boolean colorSwitch = (Boolean) PreferenceUtils.load(PreferenceUtils.getTopNode(), PREF_COLOR_SCHEME, false);
    private final static int EXON_COLOR = 1;
    private final static int INTRON_COLOR = 2;
    private boolean toggleReverseComplement = false;

    List<CreateValueSet> bundle, reverseBundle, reverseComplementList, workingList;
    Color[] defaultColors = {Color.BLACK, Color.YELLOW, Color.WHITE};
    Color[] reverseColors = {Color.WHITE, Color.BLUE, Color.BLACK};
    Color[] okayColors = {Color.black, Color.black};
    Color[] reverted = {Color.white, Color.white};
    private String id;
    int[] selectedFrames = new int[6];

    /* default constructor to get the singleton object of SeqMapView
     * This is required to get the symmetry of the selected glyphs and genomic sequence in IGB
     */
    public AbstractSequenceViewer(IGBService igbService) {
        seqmapview = igbService.getSeqMapView();
    }

    /*This method provides the properties to diplay of the sequence viewer fonts, background, text spacing, borders
     * size and location of sequenceviewer on screen
     */
    public void customFormatting(SeqSymmetry residues_sym) throws HeadlessException, NumberFormatException {
        seqview.setFont(new Font("Arial", Font.BOLD, 13));
        seqview.setNumberFontColor(Color.black);
        seqview.setNumberLabelFormat(COMMA);
        seqview.setResidueMultipleConstraint(1);
        seqview.setSpacing(20);
        this.getTitle();
        mapframe.setLocationRelativeTo(null);
        mapframe.setTitle(title);
        mapframe.setLayout(new BorderLayout());
        mapframe = setupMenus(mapframe);
        mapframe.add("Center", seqview);
    }
    /* This method is used for returning the desired coloring scheme, at present there are two color schemes
     * for the text
     */

    private Color[] getColorScheme() {
        if (colorSwitch) {
            seqview.setStripeColors(reverted);
            return reverseColors;
        } else {
            seqview.setStripeColors(okayColors);
            return defaultColors;
        }
    }

    private void checkNegativeStrand(SeqSpan cdsSpan) {
        if (!cdsSpan.isForward()) {
            this.toggleReverseComplement = true;
            if (!revCompCBMenuItem.isSelected()) {
                revCompCBMenuItem.setSelected(true);
            }
        }
    }

    /*This is the starting point for sequence viewer
     * syms.size()=1 valid case for sequenceviewer and it is not a genomic request
     * syms.size()>1 there are multiple selections done in IGB and it would throw an error
     * syms.size()=0 it is a genomic request
     * residues_syms1 instanceof SupportsCdsSpan) this is true when the selection in IGB has cds start and end.
     */
    public void startSequenceViewer() {
        List<SeqSymmetry> syms = seqmapview.getSelectedSyms();
        if (syms.size() >= 1) {
            if (syms.size() == 1) {
                residues_sym = syms.get(0);
                txMin = residues_sym.getSpan(seqmapview.getAnnotatedSeq()).getMin();
                checkNegativeStrand(residues_sym.getSpan(seqmapview.getAnnotatedSeq()));//check for negative strand and 
                if (residues_sym.getChildCount() == 0) {//for single child
                    SeqSymmetry residues_syms1 = residues_sym;
                    if ((residues_syms1 instanceof SupportsCdsSpan) && ((SupportsCdsSpan) residues_syms1).hasCdsSpan()) {
                        SeqSpan cdsSpan = ((SupportsCdsSpan) residues_syms1).getCdsSpan();
                        cdsMin = cdsSpan.getStart();
                        cdsMax = cdsSpan.getEnd();
                    }
                } else {//for single strand bed file will children
                    if ((residues_sym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) residues_sym).hasCdsSpan()) {
                        SeqSpan cdsSpan = ((SupportsCdsSpan) residues_sym).getCdsSpan();
                        cdsMin = cdsSpan.getStart();
                        cdsMax = cdsSpan.getEnd();
                    }
                }
                this.isGenomicRequest = false;
            } else {
                if (syms.size() > 1 || seqmapview.getSeqSymmetry() != null) {
                    MutableSeqSymmetry temp = new SimpleMutableSeqSymmetry();
                    SeqUtils.union(syms, temp, seqmapview.getAnnotatedSeq());

                    // Make all reverse children forward
                    residues_sym = new SimpleMutableSeqSymmetry();
                    SeqSymmetry child;
                    SeqSpan childSpan;
                    Boolean checkFirst = false;
                    for (int i = 0; i < temp.getChildCount(); i++) {
                        child = temp.getChild(i);
                        childSpan = child.getSpan(seqmapview.getAnnotatedSeq());
                        if (!checkFirst) {
                            checkFirst = true;
                            txMin = childSpan.getMin();
                        }
                        ((MutableSeqSymmetry) residues_sym).addChild(new MutableSingletonSeqSymmetry(childSpan.getMin(), childSpan.getMax(), childSpan.getBioSeq()));
                    }
                    ((MutableSeqSymmetry) residues_sym).addSpan(temp.getSpan(seqmapview.getAnnotatedSeq()));

                    this.isGenomicRequest = false;
                    buttonText = multipleSelection;
                    showcDNAButton.setText(buttonText[0]);
                    //this.errorMessage = "Multiple selections, please select only one feature at a time";
                }
            }
        } else {
            residues_sym = seqmapview.getSeqSymmetry();
            if (residues_sym != null) {
                this.isGenomicRequest = false;
            } else {
                this.isGenomicRequest = true;
            }
        }
        /*
         * This loads the reads for the selection in IGB if they are not already loaded
         */
        try {
            if (this.errorMessage == null) {
                this.aseq = seqmapview.getAnnotatedSeq();
                if (aseq == null) {
                    return;
                }

                final GenericActionDoneCallback doneback = new GenericActionDoneCallback() {
                    public void actionDone(GenericAction action) {
                        mapframe = new JFrame();
                        System.setProperty("apple.laf.useScreenMenuBar", "false");//this is done to have menu attached with the frame because in mac the default menu bar is different
                        getGoing(residues_sym);//next destination to start the sequence viewer
                    }
                };

                if (!isGenomicRequest) {
                    doBackground(doneback);
                }
//				else {
//					if (residues_sym == null) {
//						final SeqSpan span = seqmapview.getVisibleSpan();
//						residues_sym = new SingletonSeqSymmetry(span.getMin(), span.getMax(), span.getBioSeq());
//						//doneback.actionDone(null);
//
//						SequenceViewWorker worker = new SequenceViewWorker("start abstract sequence viewer", span, doneback);
//						CThreadHolder.getInstance().execute(this, worker);
//					}
//				}
            }
        } catch (Exception e) {
            if (this.errorMessage == null) {
                this.errorMessage = BUNDLE.getString("errorMessage");
            }
        } finally {
            if (errorMessage != null) {
                ErrorHandler.errorPanel("Can not open sequence viewer", "" + this.errorMessage, Level.SEVERE);

            }
        }
    }
    /* This method gets the title for sequence viewer window. The title depends on whether it is
     * a genomic request or not.
     */

    private void getTitle() {
        AnnotatedSeqGroup ag = gm.getSelectedSeqGroup();
        version = ag.getID();
        if (isGenomicRequest) {
            title = residues_sym.getSpan(0).getStart() + " - " + (residues_sym.getSpan(0).getEnd() - 1) + " : " + version + " : " + this.aseq;
//			seqview.setFirstOrdinal(residues_sym.getSpan(0).getStart());
        } else {
            if (residues_sym.getID() != null) {
                id = residues_sym.getID();
            }
            if (id == null) {
                title = version + " : " + this.aseq;
            } else {
                title = id + " : " + version + " : " + this.aseq;
            }
//			this.calculateCdsStartEnd();
            //title = version + " : " + type + " : " + chromosome + " : " + id + " : " + direction;
        }

    }
    /* It creates four array lists containing objects of CreateValueSet
     * bundle - contains symmetries which comes initially from IGB
     * reverse_bundle - if the request is from negative strand, this list is copied to working_list and is used to display in sequence viewer
     * working_list - this is a copy of bundle, reverse_bundle, or a reverse of either of them.
     * reverse_complement - i am creating reverse complement of the final working list to use for reverse complement
     */
    /*
     private void createAllLists() {
     Iterator<CreateValueSet> it = bundle.listIterator();
     if (it.hasNext()) {
     CreateValueSet cv = it.next();
     int start = cv.getSpan().getStart();
     int end = cv.getSpan().getEnd();
     //			if ((start > end) && it.hasNext() && (start < (((CreateValueSet) it.next()).getSpan().getStart()))) { 
     //above condition gives some kind of exception, not sure why? so i have to write below two separate if conditions
     //this handles for the positive and negative strand
     if (start > end) {
     if (it.hasNext()) {//we need to jump an item in list here because we need to check with the introns only
     it.next();
     if (it.hasNext()) {
     cv = it.next();
     if (start < cv.getSpan().getStart()) {
     reverse_bundle = new ArrayList<CreateValueSet>(bundle);
     Collections.reverse(reverse_bundle);
     working_list = new ArrayList<CreateValueSet>(reverse_bundle);
     } else {
     working_list = new ArrayList<CreateValueSet>(bundle);
     }
     }
     }
     else
     working_list = new ArrayList<CreateValueSet>(bundle);
     } else {
     working_list = new ArrayList<CreateValueSet>(bundle);
     }
     reverse_complement = new ArrayList<CreateValueSet>(working_list);
     Collections.reverse(reverse_complement);
     }
     }
     */

    public static class CreateValueSet implements Comparable<CreateValueSet> {

        static final SeqSpanComparator spanCompare = new SeqSpanComparator();
        public SeqSpan span;
        public SequenceViewerItems si;

        public CreateValueSet(SeqSpan span, SequenceViewerItems si) {
            this.span = span;
            this.si = si;
        }

        public SeqSpan getSpan() {
            return this.span;
        }

        public SequenceViewerItems getSi() {
            return this.si;
        }

        public int compareTo(CreateValueSet t) {
            return spanCompare.compare(span, t.getSpan());
        }
    }

    private void createItemListForSequenceviewer(SeqSymmetry residues_sym, BioSeq aseq) {
        bundle = new ArrayList<CreateValueSet>();
        if (isGenomicRequest || (residues_sym.getChildCount() == 0)) {
            addSequenceViewerItem(residues_sym, SequenceViewerItems.TYPE.EXON.ordinal(), aseq);
        } else {
            addSequenceViewerItems(residues_sym, SequenceViewerItems.TYPE.EXON.ordinal(), aseq);
            addIntron(residues_sym, aseq);
        }
        Collections.sort(bundle);

        if (!bundle.isEmpty()) {
            SeqSpan span = residues_sym.getSpan(aseq);
            if (!span.isForward() && shouldReverseOnNegative()) {
                Collections.reverse(bundle);
                workingList = new ArrayList<CreateValueSet>(bundle);
            } else {
                workingList = new ArrayList<CreateValueSet>(bundle);
            }
            reverseComplementList = new ArrayList<CreateValueSet>(workingList);
            Collections.reverse(reverseComplementList);
        }
    }

    protected abstract void addIntron(SeqSymmetry residues_sym, BioSeq aseq);

    protected abstract boolean shouldReverseOnNegative();

    protected void addSequenceViewerItems(SeqSymmetry sym, int type, BioSeq aseq) {
        for (int i = 0; i < sym.getChildCount(); i++) {
            addSequenceViewerItem(sym.getChild(i), type, aseq);
        }
    }

    protected void addSequenceViewerItem(SeqSymmetry sym, int type, BioSeq aseq) {
        SeqSpan span;
        SequenceViewerItems sequenceViewerItems = new SequenceViewerItems();
        sequenceViewerItems.setResidues(getResidues(sym, aseq).toUpperCase());
        span = sym.getSpan(aseq);
        sequenceViewerItems.setType(type);

        if (cdsMin >= 0 && cdsMax >= 0) {
            if ((cdsMin >= span.getStart() && cdsMin <= span.getEnd()) || (cdsMin <= span.getStart() && cdsMin >= span.getEnd())) {
                sequenceViewerItems.setCdsStart(Math.abs(cdsMin - span.getStart()));
                sequenceViewerItems.setReverseCdsStart(Math.abs(span.getEnd() - cdsMin));
                sequenceViewerItems.setIsCDS(true);
            }
            if ((cdsMax >= span.getStart() && cdsMax <= span.getEnd()) || (cdsMax <= span.getStart() && cdsMax >= span.getEnd())) {
                sequenceViewerItems.setCdsEnd(Math.abs(cdsMax - span.getStart()));
                sequenceViewerItems.setReverseCdsEnd(Math.abs(span.getEnd() - cdsMax));
                sequenceViewerItems.setIsCDS(true);
            }

        }
        sequenceViewerItems.setReverseResidues((DNAUtils.reverseComplement(sequenceViewerItems.getResidues())));
        bundle.add(new CreateValueSet(span, sequenceViewerItems));
    }

    public abstract String getResidues(SeqSymmetry sym, BioSeq aseq);

    private void addFormattedResidues() {
        Color[] cols = getColorScheme();
        int start = 0, end = 0;
        Iterator<CreateValueSet> it_working = null;
        seqview.setResidues("");
        if (toggleReverseComplement) {
            it_working = reverseComplementList.listIterator();
        } else {
            it_working = workingList.listIterator();
        }
        while (it_working.hasNext()) {
            CreateValueSet cv = it_working.next();
            if (showcDNASwitch) {
                if (cv.si.getType() == SequenceViewerItems.TYPE.INTRON.ordinal()) {
                    continue;
                }
            }
            String residues = cv.getSi().getResidues();
            String reverse_residues = cv.getSi().getReverseResidues();
            int cdsStart = cv.getSi().getCdsStart();
            int cdsEnd = cv.getSi().getCdsEnd();
            int revCdsStart = cv.getSi().getReverseCdsStart();
            int revCdsEnd = cv.getSi().getReverseCdsEnd();
            if (toggleReverseComplement) {
                seqview.appendResidues(reverse_residues);
            } else {
                seqview.appendResidues(residues);
            }
            end += cv.getSi().getResidues().length();
            if (cv.getSi().getType() == SequenceViewerItems.TYPE.EXON.ordinal()) {
                seqview.addTextColorAnnotation(start, end - 1, cols[EXON_COLOR]);
            } else {
                seqview.addTextColorAnnotation(start, end - 1, cols[INTRON_COLOR]);
            }
            if (cv.getSi().getCdsStart() >= 0) {
                if (toggleReverseComplement) {
                    seqview.addOutlineAnnotation(start + revCdsStart - 3, start + revCdsStart - 1, Color.green);
                    seqview.setCdsStart(start + revCdsStart - 3);
                } else {
                    seqview.addOutlineAnnotation(start + cdsStart, start + cdsStart + 2, Color.green);
                    seqview.setCdsStart(start + cdsStart);
                }
            }

            if (cv.getSi().getCdsEnd() >= 0) {
                if (toggleReverseComplement) {
                    seqview.addOutlineAnnotation(start + revCdsEnd, start + revCdsEnd + 2, Color.red);
                    seqview.setCdsEnd(start + revCdsEnd);
                } else {
                    seqview.addOutlineAnnotation(start + cdsEnd - 3, start + cdsEnd - 1, Color.red);
                    seqview.setCdsEnd(start + cdsEnd - 3);
                }
            }
            start += cv.getSi().getResidues().length();
        }
    }

    private void enableShowCDNA() {
        showcDNAButton.setEnabled(false);
        if (!bundle.isEmpty()) {
            for (CreateValueSet b : bundle) {
                if (b.getSi().getType() == SequenceViewerItems.TYPE.INTRON.ordinal()) {
                    showcDNAButton.setEnabled(true);
                    break;
                }
            }
        }
    }

    /* This method calls all the important methods to start sequence viewer
     *
     */
    protected String stringRepresentation(int num) {
        return comma_format.format(num);
    }

    protected void getGoing(SeqSymmetry residues_sym) {
        this.getNeoSeqInstance();
        createItemListForSequenceviewer(residues_sym, aseq);
        enableShowCDNA();
        customFormatting(residues_sym);
        //this.createAllLists();
        addFormattedResidues();
        if (toggleReverseComplement) {
            copyAnnotAction.setEnabled(seqview.getCdsStart() > seqview.getCdsEnd());
        } else {
            copyAnnotAction.setEnabled(seqview.getCdsStart() < seqview.getCdsEnd());
        }
        copyAnnotatedSeqAction.setEnabled(seqview.getCdsStart() < seqview.getCdsEnd());
        seqview.setPreferredSize(seqview.getPreferredSize(INITIAL_NUMBER_OF_RESIDUES + OFFSET, INITIAL_NUMBER_OF_LINES));
        mapframe.setPreferredSize(seqview.getPreferredSize());
        mapframe.pack();
        mapframe.setLocationRelativeTo(null);
        mapframe.setVisible(true);
        mapframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (e.getSource() == mapframe) {
                    mapframe.dispose();
                } else {
                    ((Window) e.getSource()).setVisible(false);
                }
            }
        });
    }

    public abstract void doBackground(final GenericActionDoneCallback doneback);

    @SuppressWarnings("serial")
    private void getNeoSeqInstance() {
        seqview = new NeoSeq() {
            @Override
            protected void setResiduesSelected(boolean bool) {
                super.setResiduesSelected(bool);
                copySelectedSeqAction.setEnabled(bool);
            }

            @Override
            public String getSelectedResidues() {
                String selectedResidues = super.getSelectedResidues();
                if (selectedResidues == null) {
                    selectedResidues = seq.getResidues(sel_range.getStart(), sel_range.getEnd());
                }
                return selectedResidues;
            }
        };
        seqview.setOffset(10);
        seqview.enableDragScrolling(true);
        seqview.addKeyListener(new KeyAdapter() {
            // Why is this not getting called?
            @Override
            public void keyPressed(KeyEvent evt) {
                System.err.println("NeoSeqDemo saw key pressed.");
            }
        });
    }

    public void exportSequenceFasta(Boolean isReverse) {
        JFileChooser chooser = UniFileChooser.getFileChooser("Fasta file", "fasta");
        chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
        String r = null;
        int option = chooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
            String fileName = chooser.getSelectedFile().toString();
            if (null != fileName) {
                try {
                    FileWriter fw = new FileWriter(fileName);
                    String firstLine = title;
                    if (isReverse) {
                        r = DNAUtils.getReverseComplement(seqview.getResidues());
                        firstLine = title + "Reverse Complement";
                    } else {
                        r = seqview.getResidues();
                    }
                    if (!isGenomicRequest) {
                        firstLine = title;
                    }
                    fw.write(">" + firstLine);
                    fw.write('\n');
                    int i;
                    for (i = 0; i < r.length() - 50; i += 50) {
                        fw.write(r, i, 50);
                        fw.write('\n');
                    }
                    if (i < r.length()) {
                        fw.write(r.substring(i) + '\n');
                    }
                    fw.flush();
                    fw.close();
                } catch (Exception ex) {
                    ErrorHandler.errorPanel("Problem saving file", ex, Level.SEVERE);
                }
            }
        }
    }
    ButtonGroup bg = new ButtonGroup();
    JToggleButton showcDNAButton = new JToggleButton("Show cDNA");
    JToggleButton reverseColorsButton = new JToggleButton("Change color scheme");
    JToggleButton showNumberingButton = new JToggleButton(buttonNumbering[1]);
    JCheckBoxMenuItem compCBMenuItem = new JCheckBoxMenuItem("Complement");
    JCheckBoxMenuItem revCompCBMenuItem = new JCheckBoxMenuItem("Reverse Complement");
    JCheckBoxMenuItem transOneCBMenuItem = new JCheckBoxMenuItem(BUNDLE.getString("copyTranslation2ToClipBoard"));
    JCheckBoxMenuItem transTwoCBMenuItem = new JCheckBoxMenuItem(BUNDLE.getString("copyTranslation3ToClipBoard"));
    JCheckBoxMenuItem transThreeCBMenuItem = new JCheckBoxMenuItem(BUNDLE.getString("copyTranslation4ToClipBoard"));
    JCheckBoxMenuItem transNegOneCBMenuItem = new JCheckBoxMenuItem(BUNDLE.getString("copyTranslation5ToClipBoard"));
    JCheckBoxMenuItem transNegTwoCBMenuItem = new JCheckBoxMenuItem(BUNDLE.getString("copyTranslation6ToClipBoard"));
    JCheckBoxMenuItem transNegThreeCBMenuItem = new JCheckBoxMenuItem(BUNDLE.getString("copyTranslation7ToClipBoard"));
    JCheckBoxMenuItem showAllPositiveTranslation = new JCheckBoxMenuItem(new ShowAllPositiveTranslationAction(this));
    JCheckBoxMenuItem showAllNegativeTranslation = new JCheckBoxMenuItem(new ShowAllNegativeTranslationAction(this));
    JCheckBoxMenuItem colorScheme1 = new JCheckBoxMenuItem("Yellow on black");
    JCheckBoxMenuItem colorScheme2 = new JCheckBoxMenuItem("Blue on white");
    JRPMenuItem exportRComplementFasta = new JRPMenuItem("sequenceViewer_exportRComplementFasta", "Save As Fasta (Reverse Complement)");
    JRPMenuItem copyTransp1MenuItem = new JRPMenuItem("sequenceViewer_copyTransp1", new CopyTransFromSeqViewerAction(this, DNAUtils.FRAME_ONE));
    JRPMenuItem copyTransp2MenuItem = new JRPMenuItem("sequenceViewer_copyTransp2", new CopyTransFromSeqViewerAction(this, DNAUtils.FRAME_TWO));
    JRPMenuItem copyTransp3MenuItem = new JRPMenuItem("sequenceViewer_copyTransp3", new CopyTransFromSeqViewerAction(this, DNAUtils.FRAME_THREE));
    JRPMenuItem copyTransn1MenuItem = new JRPMenuItem("sequenceViewer_copyTransn1", new CopyTransFromSeqViewerAction(this, DNAUtils.FRAME_NEG_ONE));
    JRPMenuItem copyTransn2MenuItem = new JRPMenuItem("sequenceViewer_copyTransn2", new CopyTransFromSeqViewerAction(this, DNAUtils.FRAME_NEG_TWO));
    JRPMenuItem copyTransn3MenuItem = new JRPMenuItem("sequenceViewer_copyTransn3", new CopyTransFromSeqViewerAction(this, DNAUtils.FRAME_NEG_THREE));
    CopyAnnotatedTranslationToClipBoardAction copyAnnotAction = new CopyAnnotatedTranslationToClipBoardAction(this);
    JRPMenu showMenu = new JRPMenu("sequenceViewer_show", "Show");
    JRPMenu fileMenu = new JRPMenu("sequenceViewer_file", "File");
    JRPMenu copyMenu = new JRPMenu("sequenceViewer_copy", "Copy");
    JRPMenu colorMenu = new JRPMenu("sequenceViewer_colors", "Colors");
    JRPMenu copyToClipMenu = new JRPMenu("copy_translations", "Copy Translation");
    CopyResidueAction copySelectedSeqAction = new CopyResidueAction(this);
    SelectAllInSeqViewerAction selectAllAction = new SelectAllInSeqViewerAction(BUNDLE.getString("selectAll"), this);
    CopySelectedTransFromSeqViewerAction copySelectedTransAction = new CopySelectedTransFromSeqViewerAction(this, selectedFrames);
    CopyAnnotatedSequenceToClipBoardAction copyAnnotatedSeqAction = new CopyAnnotatedSequenceToClipBoardAction(this);

    public JFrame setupMenus(JFrame dock) {

        copySelectedSeqAction.setEnabled(false);
        copyAnnotAction.setEnabled(false);
        copyAnnotatedSeqAction.setEnabled(false);
        copySelectedTransAction.setEnabled(false);
        copyToClipMenu.add(copyTransp1MenuItem);
        copyToClipMenu.add(copyTransp2MenuItem);
        copyToClipMenu.add(copyTransp3MenuItem);
        copyToClipMenu.add(copyTransn1MenuItem);
        copyToClipMenu.add(copyTransn2MenuItem);
        copyToClipMenu.add(copyTransn3MenuItem);
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem("sequenceViewer_exportFastaSequence", new ExportFastaSequenceAction(this)));
        MenuUtil.addToMenu(fileMenu, exportRComplementFasta);
        fileMenu.addSeparator();
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem("sequenceViewer_exportView", new ExportSequenceViewerAction(dock, seqview.getScroller())));
        fileMenu.addSeparator();
        MenuUtil.addToMenu(fileMenu, new JRPMenuItem("sequenceViewer_exitSeqViewer", new ExitSeqViewerAction(this.mapframe)));
        MenuUtil.addToMenu(copyMenu, new JRPMenuItem("sequenceViewer_copySeq", selectAllAction));
        MenuUtil.addToMenu(copyMenu, new JRPMenuItem("sequenceViewer_copySelectedSeq", copySelectedSeqAction));
        copyMenu.add(copyAnnotatedSeqAction);
//		copyMenu.add(copyToClipMenu);
        copyMenu.add(copyAnnotAction);
        copyMenu.add(copySelectedTransAction);
        copyMenu.addMenuListener(this);
        showMenu.add(revCompCBMenuItem);
        showMenu.add(compCBMenuItem);
        showMenu.addSeparator();
        showMenu.add(transOneCBMenuItem);
        showMenu.add(transTwoCBMenuItem);
        showMenu.add(transThreeCBMenuItem);
        showMenu.add(transNegOneCBMenuItem);
        showMenu.add(transNegTwoCBMenuItem);
        showMenu.add(transNegThreeCBMenuItem);
        showMenu.addSeparator();
        showMenu.add(showAllPositiveTranslation);
        showMenu.add(showAllNegativeTranslation);
        colorMenu.add(colorScheme1);
        colorMenu.add(colorScheme2);
        exportRComplementFasta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                exportSequenceFasta(true);
            }
        });
        bg.add(colorScheme1);
        bg.add(colorScheme2);
        if (colorSwitch) {
            colorScheme2.setState(true);
        } else {
            colorScheme1.setState(true);
        }
        colorScheme1.addItemListener(this);
        colorScheme2.addItemListener(this);
        revCompCBMenuItem.addItemListener(this);
        compCBMenuItem.addItemListener(this);
        transOneCBMenuItem.addItemListener(this);
        transTwoCBMenuItem.addItemListener(this);
        transThreeCBMenuItem.addItemListener(this);
        transNegOneCBMenuItem.addItemListener(this);
        transNegTwoCBMenuItem.addItemListener(this);
        transNegThreeCBMenuItem.addItemListener(this);
        showcDNAButton.addActionListener(this);
        reverseColorsButton.addActionListener(this);
        showNumberingButton.addActionListener(this);
        // add the menus to the menubar
        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu);
        bar.add(copyMenu);
        bar.add(showMenu);
        bar.add(colorMenu);
        bar.add(showcDNAButton);
//		bar.add(showNumberingButton);
//		bar.add(reverseColorsButton);
        dock.setJMenuBar(bar);
        return dock;
    }

    public void copySelectedResidues() {
        copyToClipBoard(seqview.getSelectedResidues().trim());
    }

    public void copyWholeSequence() {
        copyToClipBoard(seqview.getResidues().trim());
    }

    private void copyToClipBoard(String seqToBeCopied) {
        if (seqToBeCopied != null) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringBuffer hackbuf = new StringBuffer(seqToBeCopied);
            String hackstr = new String(hackbuf);
            StringSelection data = new StringSelection(hackstr);
            clipboard.setContents(data, null);
        } else {
            ErrorHandler.errorPanel("Missing Sequence Residues",
                    "Don't have all the needed residues, can't copy to clipboard.\n"
                    + "Please load sequence residues for this region.", Level.WARNING);
        }
    }

    public void copyTransAction(int frametype) {
        String residues = seqview.getResidues().trim();
        if (residues == null) {
            return;
        }
        residues = DNAUtils.translate(residues, frametype, DNAUtils.ONE_LETTER_CODE);
        if (residues != null) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringBuffer hackbuf = new StringBuffer(residues);
            String hackstr = new String(hackbuf);
            StringSelection data = new StringSelection(hackstr);
            clipboard.setContents(data, null);
        } else {
            ErrorHandler.errorPanel("Missing Sequence Residues",
                    "Don't have all the needed residues, can't copy to clipboard.\n"
                    + "Please load sequence residues for this region.", Level.WARNING);
        }
    }

    public void copySelectedTransAction(int[] frameArray) {
        int frameType;
        StringBuffer selectedTrans = new StringBuffer();

        for (int i = 0; i < frameArray.length; i++) {
            frameType = frameArray[i];
            if (frameType < Translatable.FRAME_ONE) {
                continue;
            }
            selectedTrans.append(">Frame ").append(BUNDLE.getString("copyTranslation" + frameType + "ToClipBoard").substring(0, 2)).append("\n");
            selectedTrans.append(getSelectedResidues(frameType).trim().replaceAll("\\s", "")).append("\n\n");
        }

        if (selectedTrans != null || selectedTrans.length() < 0) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection data = new StringSelection(selectedTrans.toString());
            clipboard.setContents(data, null);
        } else {
            ErrorHandler.errorPanel("Missing Translations",
                    "Don't have all the needed residues, can't copy to clipboard.\n"
                    + "Please select the translations with correct sequence to be copied.", Level.WARNING);
        }
    }

    private String getSelectedResidues(int frame) {
        return ((NASequence) seqview.getSequence()).getTranslation(frame).substring(seqview.getSelectedStart(), seqview.getSelectedEnd()).trim();
    }
    
    public void copyAnnotatedTransAction() {

        StringBuilder annotatedSeqStringBuffer = new StringBuilder();
        Iterator<CreateValueSet> workingListIterator = workingList.listIterator();
        boolean isStartCdsReached = false;
        while (workingListIterator.hasNext()) {
            CreateValueSet cv = workingListIterator.next();
            String residues = cv.getSi().getResidues();

            if (cv.si.getCdsStart() >= 0) {
                isStartCdsReached = true;
            }
            if (!isStartCdsReached) {
                continue;
            }
            if (!(cv.si.getType() == SequenceViewerItems.TYPE.INTRON.ordinal())) {
                int start = cv.si.getCdsStart()>0?  cv.si.getCdsStart():0;
                int end = cv.si.getCdsEnd() > 0 ? cv.si.getCdsEnd():residues.length();
                annotatedSeqStringBuffer.append(residues.substring(start, end));
            }
            if (cv.si.getCdsEnd() >= 0) {
                break;
            }

        }

        String annotatedResidues = annotatedSeqStringBuffer.toString();
        annotatedResidues = DNAUtils.translate(annotatedResidues, DNAUtils.FRAME_ONE, DNAUtils.ONE_LETTER_CODE);
        copyToClipBoard(annotatedResidues);
    }

    /*
     * Copy the annotated sequence and replace the 'starting', 'ending' and introns with lower cases
     */
    public void copyAnnotatedSequenceAction() {
        StringBuffer copyAnnotatedSeqStringBuffer = new StringBuffer();

        int start = 0, end = 0;
        Iterator<CreateValueSet> it_working = null;
        if (toggleReverseComplement) {
            it_working = reverseComplementList.listIterator();
        } else {
            it_working = workingList.listIterator();
        }
        while (it_working.hasNext()) {
            CreateValueSet cv = it_working.next();
            if (showcDNASwitch) {
                if (cv.si.getType() == SequenceViewerItems.TYPE.INTRON.ordinal()) {
                    continue;
                }
            }
            String residues = cv.getSi().getResidues();
            String reverse_residues = cv.getSi().getReverseResidues();
            int cdsStart = cv.getSi().getCdsStart();
            int cdsEnd = cv.getSi().getCdsEnd();
            int revCdsStart = cv.getSi().getReverseCdsStart();
            int revCdsEnd = cv.getSi().getReverseCdsEnd();

            if (toggleReverseComplement) {
                copyAnnotatedSeqStringBuffer.append(reverse_residues);
            } else {
                copyAnnotatedSeqStringBuffer.append(residues);
            }

            end += cv.getSi().getResidues().length();

            if (cv.getSi().getType() == SequenceViewerItems.TYPE.INTRON.ordinal()) {
                copyAnnotatedSeqStringBuffer.replace(start, end, copyAnnotatedSeqStringBuffer.substring(start, end).toLowerCase());
            }

            if (cv.getSi().getCdsStart() >= 0) {
                if (toggleReverseComplement) {
                    copyAnnotatedSeqStringBuffer.replace(start + revCdsStart - 3, start + revCdsStart, copyAnnotatedSeqStringBuffer.substring(start + revCdsStart - 3, start + revCdsStart).toLowerCase());
                } else {
                    copyAnnotatedSeqStringBuffer.replace(start + cdsStart, start + cdsStart + 3, copyAnnotatedSeqStringBuffer.substring(start + cdsStart, start + cdsStart + 3).toLowerCase());
                }
            }

            if (cv.getSi().getCdsEnd() >= 0) {
                if (toggleReverseComplement) {
                    copyAnnotatedSeqStringBuffer.replace(start + revCdsEnd, start + revCdsEnd + 3, copyAnnotatedSeqStringBuffer.substring(start + revCdsEnd, start + revCdsEnd + 3).toLowerCase());
                } else {
                    copyAnnotatedSeqStringBuffer.replace(start + cdsEnd - 3, start + cdsEnd, copyAnnotatedSeqStringBuffer.substring(start + cdsEnd - 3, start + cdsEnd).toLowerCase());
                }
            }
            start += cv.getSi().getResidues().length();
        }

        if (copyAnnotatedSeqStringBuffer != null) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection data = new StringSelection(copyAnnotatedSeqStringBuffer.toString());
            clipboard.setContents(data, null);
        } else {
            ErrorHandler.errorPanel("Copy",
                    "Can't copy annotated sequence to clipboard.\n", Level.WARNING);
        }
    }

    public void selectAll() {
        String residues = seqview.getResidues().trim();
        Selection selectAll = new Selection();
        selectAll.setRange(0, residues.length());
        seqview.highlightResidues(0, residues.length());
        seqview.setSelection(selectAll);
    }

    /**
     * ItemListener Implementation
     */
    public void itemStateChanged(ItemEvent e) {
        Object theItem = e.getSource();
        if (theItem == compCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            seqview.setShow(NeoSeq.COMPLEMENT, mi.getState());
//			seqview.setRevShow(NeoSeq.COMPLEMENT, showRevComp);
//			seqview.setRevShow(NeoSeq.NUCLEOTIDES, !showRevComp);
            seqview.updateWidget();
        } else if (theItem == revCompCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            if (mi.getState()) {
                this.toggleReverseComplement = true;
            } else {
                this.toggleReverseComplement = false;
            }
            seqview.clearWidget();
            this.addFormattedResidues();
            seqview.updateWidget();
        } else if (theItem == transOneCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            setShowAllTranslation(true); // Select 'All + Translation' menu item if all the +1 +2 +3 are selected
            seqview.setShow(NeoSeq.FRAME_ONE, mi.getState());
            seqview.updateWidget();
        } else if (theItem == transTwoCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            setShowAllTranslation(true);
            seqview.setShow(NeoSeq.FRAME_TWO, mi.getState());
            seqview.updateWidget();
        } else if (theItem == transThreeCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            setShowAllTranslation(true);
            seqview.setShow(NeoSeq.FRAME_THREE, mi.getState());
            seqview.updateWidget();
        } else if (theItem == transNegOneCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            setShowAllTranslation(false);  // Enable 'All - Translation' menu item if all the -1 -2 -3 are selected
            seqview.setShow(NeoSeq.FRAME_NEG_ONE, mi.getState());
            seqview.updateWidget();
        } else if (theItem == transNegTwoCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            setShowAllTranslation(false);
            seqview.setShow(NeoSeq.FRAME_NEG_TWO, mi.getState());
            seqview.updateWidget();
        } else if (theItem == transNegThreeCBMenuItem) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            setShowAllTranslation(false);
            seqview.setShow(NeoSeq.FRAME_NEG_THREE, mi.getState());
            seqview.updateWidget();
        } else if (theItem == colorScheme1) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            if (mi.getState()) {
                colorSwitch = false;
                PreferenceUtils.save(PreferenceUtils.getTopNode(), PREF_COLOR_SCHEME, colorSwitch);
                this.colorSwitching();
            }
        } else if (theItem == colorScheme2) {
            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
            if (mi.getState()) {
                colorSwitch = true;
                PreferenceUtils.save(PreferenceUtils.getTopNode(), PREF_COLOR_SCHEME, colorSwitch);
                this.colorSwitching();
            }
        }

        for (int i = Translatable.FRAME_ONE; i <= Translatable.FRAME_NEG_THREE; i++) {
            if (seqview.getShow(i)) {
                selectedFrames[i - 2] = i;
            } else {
                selectedFrames[i - 2] = 0;
            }
        }
    }

    private void setShowAllTranslation(boolean forPositive) {
        if (forPositive) {
            if (transOneCBMenuItem.isSelected() == transTwoCBMenuItem.isSelected()
                    && transTwoCBMenuItem.isSelected() == transThreeCBMenuItem.isSelected()) {
                showAllPositiveTranslation.setSelected(transOneCBMenuItem.isSelected());
            } else {
                showAllPositiveTranslation.setSelected(false);
            }
        } else {
            if (transNegOneCBMenuItem.isSelected() == transNegTwoCBMenuItem.isSelected()
                    && transNegTwoCBMenuItem.isSelected() == transNegThreeCBMenuItem.isSelected()) {
                showAllNegativeTranslation.setSelected(transNegOneCBMenuItem.isSelected());
            } else {
                showAllNegativeTranslation.setSelected(false);
            }
        }
    }

    private void colorSwitching() {
        seqview.clearWidget();
        addFormattedResidues();
    }

    /**
     * WindowListener Implementation
     */
    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        ((Window) e.getSource()).setVisible(false);
    }

    public void initSequenceViewer(SeqSymmetry residues_sym) {
        mapframe = new JFrame();
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        getGoing(residues_sym);
    }

    public void actionPerformed(ActionEvent e) {
        Object evtSource = e.getSource();
        if (evtSource == showNumberingButton) {
            String text = e.getActionCommand();
            if (text.equals(buttonNumbering[1])) {
                seqview.setFirstOrdinal(txMin);
                showNumberingButton.setText(buttonNumbering[0]);
            } else {
                seqview.setFirstOrdinal(0);
                showNumberingButton.setText(buttonNumbering[1]);
            }
            seqview.clearWidget();
            addFormattedResidues();
            seqview.updateWidget();
        }
        if (evtSource == showcDNAButton) {
            String text = e.getActionCommand();
            seqview.setFirstOrdinal(0);
            if (text.equals(buttonText[0])) {
                showcDNASwitch = true;
                showcDNAButton.setText(buttonText[1]);
                showNumberingButton.setText(buttonNumbering[1]);
                showNumberingButton.setEnabled(false);
            } else {
                showcDNASwitch = false;
                showcDNAButton.setText(buttonText[0]);
                showNumberingButton.setText(buttonNumbering[1]);
                showNumberingButton.setEnabled(true);
            }
            seqview.clearWidget();
            addFormattedResidues();
            seqview.updateWidget();
//		} else if (evtSource == reverseColorsButton) {
//			String text = e.getActionCommand();
//			if (text.equals("Change color scheme")) {
//				reverseColorsButton.setText("Revert color scheme");
//				colorSwitch = true;
//			} else {
//				reverseColorsButton.setText("Change color scheme");
//				colorSwitch = false;
//			}
//			seqview.clearWidget();
//			if (!showcDNASwitch) {
//				addFormattedResidues();
//			} else {
//				seqview.setResidues(seq1);
//				seqview.addTextColorAnnotation(0, seq1.length(), getColorScheme()[EXON_COLOR]);
//			}
        }
    }

    public void menuSelected(MenuEvent me) {
        Object evtSource = me.getSource();
        if (evtSource == copyMenu) {
            if (!seqview.getSelectedResidues().trim().isEmpty()) {
                copySelectedSeqAction.setEnabled(true);
                if (seqview.getShow(Translatable.FRAME_ONE)
                        || seqview.getShow(Translatable.FRAME_TWO)
                        || seqview.getShow(Translatable.FRAME_THREE)
                        || seqview.getShow(Translatable.FRAME_NEG_ONE)
                        || seqview.getShow(Translatable.FRAME_NEG_TWO)
                        || seqview.getShow(Translatable.FRAME_NEG_THREE)) {
                    copySelectedTransAction.setEnabled(true);
                }
            } else {
                copySelectedSeqAction.setEnabled(false);
                copySelectedTransAction.setEnabled(false);
            }
        }
    }

    public void menuDeselected(MenuEvent me) {
    }

    public void menuCanceled(MenuEvent me) {
    }

    class CopyTransFromSeqViewerAction extends GenericAction {

        AbstractSequenceViewer sv;
        public int frameType;

        public CopyTransFromSeqViewerAction(AbstractSequenceViewer sv, int frameType) {
            super(BUNDLE.getString("copyTranslation" + frameType + "ToClipBoard"), KeyEvent.VK_C);
            this.sv = sv;
            this.frameType = frameType;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            sv.copyTransAction(frameType);
        }
    }

    class CopySelectedTransFromSeqViewerAction extends GenericAction {

        AbstractSequenceViewer sv;
        int frameTypeArray[];

        public CopySelectedTransFromSeqViewerAction(AbstractSequenceViewer sv, int frameTypeArray[]) {
            super(BUNDLE.getString("copySelectedTranslation"), KeyEvent.VK_C);
            this.sv = sv;
            this.frameTypeArray = frameTypeArray;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            sv.copySelectedTransAction(frameTypeArray);
        }
    }

    class CopyAnnotatedTranslationToClipBoardAction extends GenericAction {

        AbstractSequenceViewer sv;

        public CopyAnnotatedTranslationToClipBoardAction(AbstractSequenceViewer sv) {
            super(BUNDLE.getString("copyAnnotatedTranslationToClipBoard"), KeyEvent.VK_C);
            this.sv = sv;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            sv.copyAnnotatedTransAction();
        }
    }

    class CopyAnnotatedSequenceToClipBoardAction extends GenericAction {

        AbstractSequenceViewer sv;

        public CopyAnnotatedSequenceToClipBoardAction(AbstractSequenceViewer sv) {
            super(BUNDLE.getString("copyAnnotatedSequence"), KeyEvent.VK_C);
            this.sv = sv;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            sv.copyAnnotatedSequenceAction();
        }
    }

    class CopyResidueAction extends GenericAction {

        AbstractSequenceViewer sv;

        public CopyResidueAction(AbstractSequenceViewer sv) {
            super(BUNDLE.getString("copySelectedSequence"), KeyEvent.VK_C);
            this.sv = sv;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            if (sv != null) {
                sv.copySelectedResidues();
            }
        }
    }

    class ShowAllPositiveTranslationAction extends GenericAction {

        AbstractSequenceViewer sv;

        public ShowAllPositiveTranslationAction(AbstractSequenceViewer sv) {
            super(BUNDLE.getString("showAllPositiveTranslations"), KeyEvent.VK_UNDEFINED);
            this.sv = sv;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            boolean showAllStatus = ((JCheckBoxMenuItem) e.getSource()).getState();
            sv.transOneCBMenuItem.setSelected(showAllStatus);
            sv.transTwoCBMenuItem.setSelected(showAllStatus);
            sv.transThreeCBMenuItem.setSelected(showAllStatus);
        }
    }

    class ShowAllNegativeTranslationAction extends GenericAction {

        AbstractSequenceViewer sv;

        public ShowAllNegativeTranslationAction(AbstractSequenceViewer sv) {
            super(BUNDLE.getString("showAllNegativeTranslations"), KeyEvent.VK_UNDEFINED);
            this.sv = sv;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            boolean showAllStatus = ((JCheckBoxMenuItem) e.getSource()).getState();
            sv.transNegOneCBMenuItem.setSelected(showAllStatus);
            sv.transNegTwoCBMenuItem.setSelected(showAllStatus);
            sv.transNegThreeCBMenuItem.setSelected(showAllStatus);
        }
    }
}
