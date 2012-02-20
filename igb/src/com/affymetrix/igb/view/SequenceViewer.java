/**
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.comparator.SeqSpanComparator;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import com.affymetrix.genoviz.widget.NeoSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.action.CopyFromSeqViewerAction;
import com.affymetrix.igb.action.ExitSeqViewerAction;
import com.affymetrix.igb.action.ExportFastaSequenceAction;
import com.affymetrix.igb.action.ExportSequenceViewerAction;
import com.affymetrix.igb.action.LoadResidueAction;
import com.affymetrix.igb.shared.FileTracker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class SequenceViewer implements ActionListener, WindowListener, ItemListener, MenuListener {

	private SeqMapView seqmapview;
	private NeoSeq seqview;
	private JFrame mapframe;
	private int pixel_width = 500;
	private int pixel_height = 400;
	private GenometryModel gm = GenometryModel.getGenometryModel();
	private String version = "";
	private SeqSymmetry residues_sym;
	BioSeq aseq;
	boolean isGenomicRequest;
	SequenceViewer sv;
	String errorMessage = null;
	private int cdsMax = -1;
	private int cdsMin = -1;
	private String title = null;
	private boolean showcDNASwitch = false;
	private boolean colorSwitch = false;
	private final static int EXON_COLOR = 1;
	private final static int INTRON_COLOR = 2;
	private boolean toggle_Reverse_Complement = false;;
	List<CreateValueSet> bundle, reverse_bundle, reverse_complement, working_list;
	Color[] defaultColors = {Color.BLACK, Color.YELLOW, Color.WHITE};
	Color[] reverseColors = {Color.WHITE, Color.BLUE, Color.BLACK};
	Color[] okayColors = {Color.black, Color.black};
	Color[] reverted = {Color.white, Color.white};
	private String id;
/* default constructor to get the singleton object of SeqMapView
 * This is required to get the symmetry of the selected glyphs and genomic sequence in IGB
 */
	public SequenceViewer() {
		seqmapview = IGB.getSingleton().getMapView();
	}

	/*This method provides the properties to diplay of the sequence viewer fonts, background, text spacing, borders
	 * size and location of sequenceviewer on screen
	 */
	public void customFormatting(SeqSymmetry residues_sym) throws HeadlessException, NumberFormatException {
		seqview.setFont(new Font("Arial", Font.BOLD, 13));
		seqview.setNumberFontColor(Color.black);
		seqview.setSpacing(20);
		this.getTitle();
		mapframe.setTitle(title);
		mapframe.setLayout(new BorderLayout());
		mapframe = setupMenus(mapframe);
		mapframe.add("Center", seqview);
		Dimension dim = new Dimension(600,400);
		seqview.setPreferredSize(dim);
		mapframe.setPreferredSize(dim);
		Dimension igb_size = IGB.getSingleton().getFrame().getSize();
		mapframe.setLocation((igb_size.width - pixel_width) / 2, (igb_size.height - pixel_height) / 2);
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
				if (residues_sym.getChildCount() == 0) {
					SeqSymmetry residues_syms1 = SeqMapView.glyphsToSyms(SeqMapView.getParents(seqmapview.getSeqMap().getSelected())).get(0);
					if ((residues_syms1 instanceof SupportsCdsSpan) && ((SupportsCdsSpan) residues_syms1).hasCdsSpan()) {
						SeqSpan cdsSpan = ((SupportsCdsSpan) residues_syms1).getCdsSpan();
						cdsMin = cdsSpan.getStart();
						cdsMax = cdsSpan.getEnd();
					}
				} else {
					if ((residues_sym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) residues_sym).hasCdsSpan()) {
						SeqSpan cdsSpan = ((SupportsCdsSpan) residues_sym).getCdsSpan();
						cdsMin = cdsSpan.getStart();
						cdsMax = cdsSpan.getEnd();
					}
				}
				this.isGenomicRequest = false;
			} else {
				if (syms.size() > 1 || seqmapview.getSeqSymmetry() != null) {
					this.errorMessage = "Multiple selections, please select one feature or a parent";
				}
			}
		} else {
			residues_sym = seqmapview.getSeqSymmetry();
			this.isGenomicRequest = true;
		}
/*This loads the reads for the selection in IGB if they are not already loaded
 *
 */
		try {
			if (this.errorMessage == null) {
				this.aseq = seqmapview.getAnnotatedSeq();

				final GenericActionDoneCallback doneback = new GenericActionDoneCallback() {

					public void actionDone(GenericAction action) {
						mapframe = new JFrame();
						System.setProperty("apple.laf.useScreenMenuBar", "false");//this is done to have menu attached with the frame because in mac the default menu bar is different
						getGoing(residues_sym);//next destination to start the sequence viewer
					}
				};

				if (!isGenomicRequest) {

					SwingWorker worker = new SwingWorker() {

						@Override
						protected Object doInBackground() throws Exception {
							LoadResidueAction loadResidue = new LoadResidueAction(residues_sym.getSpan(aseq), true);
							
							loadResidue.addDoneCallback(doneback);
							loadResidue.actionPerformed(null);
							loadResidue.removeDoneCallback(doneback);
							return null;
						}
					};
					worker.execute();
					
				}else{
					doneback.actionDone(null);
				}
			}
		} catch (Exception e) {
			if (this.errorMessage == null) {
				this.errorMessage = "Some error ocurred, Please raise a bug request";
			}
		} finally {
			if (errorMessage != null) {
				ErrorHandler.errorPanel("Can not open sequence viewer", "" + this.errorMessage);

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
	public static class CreateValueSet implements Comparable<CreateValueSet>{
		static final SeqSpanComparator spanCompare = new SeqSpanComparator();
		public SeqSpan span;
		public SequenceViewerItems si;

		public CreateValueSet(SeqSpan span, SequenceViewerItems si) {
			this.span = span;
			this.si = si;
		};

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
			addSequenceViewerItem(residues_sym, 0, aseq);
		} else {
			addSequenceViewerItems(residues_sym, SequenceViewerItems.TYPE.EXON.ordinal(), aseq);
			addSequenceViewerItems(SeqUtils.getIntronSym(residues_sym, aseq), SequenceViewerItems.TYPE.INTRON.ordinal(), aseq);
		}
		Collections.sort(bundle);
		
		if (!bundle.isEmpty()) {
			SeqSpan span = residues_sym.getSpan(aseq);
			if (!span.isForward()) {
				Collections.reverse(bundle);
				working_list = new ArrayList<CreateValueSet>(bundle);
			} else {
				working_list = new ArrayList<CreateValueSet>(bundle);
			}
			reverse_complement = new ArrayList<CreateValueSet>(working_list);
			Collections.reverse(reverse_complement);
		}
	}

	private void addSequenceViewerItems(SeqSymmetry sym, int type, BioSeq aseq) {
		for (int i = 0; i < sym.getChildCount(); i++) {
			addSequenceViewerItem(sym.getChild(i), type, aseq);
		}
	}

	private void addSequenceViewerItem(SeqSymmetry sym, int type, BioSeq aseq) {
		SeqSpan span;
		SequenceViewerItems sequenceViewerItems = new SequenceViewerItems();
		sequenceViewerItems.setResidues(SeqUtils.getResidues(sym, aseq));
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
	
	private void addFormattedResidues() {
		Color[] cols = getColorScheme();
		int start = 0, end = 0;
		Iterator<CreateValueSet> it_working = null;
		seqview.setResidues("");
		if (toggle_Reverse_Complement) {
			it_working = reverse_complement.listIterator();
		} else {
			it_working = working_list.listIterator();
		}
		while (it_working.hasNext()) {
			CreateValueSet cv = it_working.next();
			if(showcDNASwitch){
				if(cv.si.getType() == SequenceViewerItems.TYPE.INTRON.ordinal()){
					continue;
				}
			}
			String residues = cv.getSi().getResidues();
			String reverse_residues = cv.getSi().getReverseResidues();
			int cdsStart = cv.getSi().getCdsStart();
			int cdsEnd = cv.getSi().getCdsEnd();
			int revCdsStart = cv.getSi().getReverseCdsStart();
			int revCdsEnd = cv.getSi().getReverseCdsEnd();
			if (toggle_Reverse_Complement) {
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
				if (toggle_Reverse_Complement) {
					seqview.addOutlineAnnotation(start + revCdsStart - 3, start + revCdsStart - 1, Color.green);
				} else {
					seqview.addOutlineAnnotation(start + cdsStart, start + cdsStart + 2, Color.green);
				}
			}

			if (cv.getSi().getCdsEnd() >= 0) {
				if (toggle_Reverse_Complement) {
					seqview.addOutlineAnnotation(start + revCdsEnd, start + revCdsEnd + 2, Color.red);
				} else {
					seqview.addOutlineAnnotation(start + cdsEnd - 3, start + cdsEnd - 1, Color.red);
				}
			}
			start += cv.getSi().getResidues().length();
		}
	}
	
	private void enableShowCDNA(){
		showcDNAButton.setEnabled(false);
		if(!bundle.isEmpty()){
			for(CreateValueSet b : bundle){
				if(b.getSi().getType() == SequenceViewerItems.TYPE.INTRON.ordinal()){
					showcDNAButton.setEnabled(true);
					break;
				}
			}
		}
	}
	
/* This method calls all the important methods to start sequence viewer
 *
 */
	protected void getGoing(SeqSymmetry residues_sym) {
		this.getNeoSeqInstance();
		createItemListForSequenceviewer(residues_sym, aseq);
		enableShowCDNA();
		customFormatting(residues_sym);
		//this.createAllLists();
		addFormattedResidues();

		mapframe.pack();
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

	@SuppressWarnings("serial")
	private void getNeoSeqInstance() {
		seqview = new NeoSeq() {

			@Override
			protected void setResiduesSelected(boolean bool) {
				super.setResiduesSelected(bool);
				copyAction.setEnabled(bool);
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
		seqview.enableDragScrolling(true);
		seqview.addKeyListener(new KeyAdapter() {

			// Why is this not getting called?
			@Override
			public void keyPressed(KeyEvent evt) {
				System.err.println("NeoSeqDemo saw key pressed.");
			}
		});
	}

	public static String getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				String text = (String) t.getTransferData(DataFlavor.stringFlavor);

				return text.trim();
			}
		} catch (Exception e) {
		}
		return "";
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
					ErrorHandler.errorPanel("Problem saving file", ex);
				}
			}
		}
	}
	ButtonGroup bg = new ButtonGroup();
	JToggleButton showcDNAButton = new JToggleButton("Show cDNA");
	JToggleButton reverseColorsButton = new JToggleButton("Change color scheme");
	JCheckBoxMenuItem compCBMenuItem = new JCheckBoxMenuItem("Complement");
	JCheckBoxMenuItem revCompCBMenuItem = new JCheckBoxMenuItem("Reverse Complement");
	JCheckBoxMenuItem transOneCBMenuItem = new JCheckBoxMenuItem(" +1 Translation");
	JCheckBoxMenuItem transTwoCBMenuItem = new JCheckBoxMenuItem(" +2 Translation");
	JCheckBoxMenuItem transThreeCBMenuItem = new JCheckBoxMenuItem(" +3 Translation");
	JCheckBoxMenuItem transNegOneCBMenuItem = new JCheckBoxMenuItem(" -1 Translation");
	JCheckBoxMenuItem transNegTwoCBMenuItem = new JCheckBoxMenuItem(" -2 Translation");
	JCheckBoxMenuItem transNegThreeCBMenuItem = new JCheckBoxMenuItem(" -3 Translation");
	JCheckBoxMenuItem colorScheme1 = new JCheckBoxMenuItem("Yellow on black");
	JCheckBoxMenuItem colorScheme2 = new JCheckBoxMenuItem("Blue on white");
	JRPMenuItem exportRComplementFasta = new JRPMenuItem("sequenceViewer_exportRComplementFasta", "Save As Fasta (Reverse Complement)");
	JRPMenu showMenu = new JRPMenu("sequenceViewer_show", "Show");
	JRPMenu fileMenu = new JRPMenu("sequenceViewer_file", "File");
	JRPMenu editMenu = new JRPMenu("sequenceViewer_edit", "Edit");
	JRPMenu colorMenu = new JRPMenu("sequenceViewer_colors", "Colors");
	CopyFromSeqViewerAction copyAction = new CopyFromSeqViewerAction(this);

	public JFrame setupMenus(JFrame dock) {

		copyAction.setEnabled(false);
		MenuUtil.addToMenu(fileMenu, new JRPMenuItem("sequenceViewer_exportFastaSequence", new ExportFastaSequenceAction(this)));
		MenuUtil.addToMenu(fileMenu, exportRComplementFasta);
		fileMenu.addSeparator();
		MenuUtil.addToMenu(fileMenu, new JRPMenuItem("sequenceViewer_exportView", new ExportSequenceViewerAction(seqview)));
		fileMenu.addSeparator();
		MenuUtil.addToMenu(fileMenu, new JRPMenuItem("sequenceViewer_exitSeqViewer", new ExitSeqViewerAction(this.mapframe)));
		MenuUtil.addToMenu(editMenu, new JRPMenuItem("sequenceViewer_copy", copyAction));
		editMenu.addMenuListener(this);
		showMenu.add(revCompCBMenuItem);
		showMenu.add(compCBMenuItem);
		showMenu.add(transOneCBMenuItem);
		showMenu.add(transTwoCBMenuItem);
		showMenu.add(transThreeCBMenuItem);
		showMenu.add(transNegOneCBMenuItem);
		showMenu.add(transNegTwoCBMenuItem);
		showMenu.add(transNegThreeCBMenuItem);
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
		// add the menus to the menubar
		JMenuBar bar = new JMenuBar();
		bar.add(fileMenu);
		bar.add(editMenu);
		bar.add(showMenu);
		bar.add(colorMenu);
		bar.add(showcDNAButton);
//		bar.add(reverseColorsButton);
		dock.setJMenuBar(bar);
		return dock;
	}

	/* EVENT HANDLING */
	/** ActionListener Implementation */
	public void copyAction() {
		String selectedSeq = seqview.getSelectedResidues().trim();
		if (seqview.getShow(NeoSeq.COMPLEMENT)) {
			selectedSeq = DNAUtils.complement(selectedSeq);
		}
		if (selectedSeq != null) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringBuffer hackbuf = new StringBuffer(selectedSeq);
			String hackstr = new String(hackbuf);
			StringSelection data = new StringSelection(hackstr);
			clipboard.setContents(data, null);
		} else {
			ErrorHandler.errorPanel("Missing Sequence Residues",
					"Don't have all the needed residues, can't copy to clipboard.\n"
					+ "Please load sequence residues for this region.");
		}
	}

	/** ItemListener Implementation */
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
				this.toggle_Reverse_Complement = true;
			} else {
				this.toggle_Reverse_Complement = false;
			}
			seqview.clearWidget();
			this.addFormattedResidues();
			seqview.updateWidget();
		} else if (theItem == transOneCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_ONE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transTwoCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_TWO, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transThreeCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_THREE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transNegOneCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_NEG_ONE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transNegTwoCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_NEG_TWO, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transNegThreeCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_NEG_THREE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == colorScheme1) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			if (mi.getState()) {
				colorSwitch = false;
				this.colorSwitching();
			}
		} else if (theItem == colorScheme2) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			if (mi.getState()) {
				colorSwitch = true;
				this.colorSwitching();
			}
		}
	}

	private void colorSwitching() {
		seqview.clearWidget();
			addFormattedResidues();
	}

	/** WindowListener Implementation */
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
		if (evtSource == showcDNAButton) {
			String text = e.getActionCommand();
			if (text.equals("Show cDNA")) {
				showcDNASwitch = true;	
				showcDNAButton.setText("Show genomic");
			} else {
				showcDNASwitch = false;
				showcDNAButton.setText("Show cDNA");
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
		if (evtSource == editMenu) {
			if (!seqview.getSelectedResidues().trim().isEmpty()) {
				copyAction.setEnabled(true);
			} else {
				copyAction.setEnabled(false);
			}
		}
	}

	public void menuDeselected(MenuEvent me) {
	}

	public void menuCanceled(MenuEvent me) {
	}
}
