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
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.util.GeneralUtils;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import com.affymetrix.genoviz.widget.NeoSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.action.CopyFromSeqViewerAction;
import com.affymetrix.igb.action.ExitSeqViewerAction;
import com.affymetrix.igb.action.ExportFastaSequenceAction;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class SequenceViewer extends JPanel
		implements ActionListener, WindowListener, ItemListener, MenuListener {

	private static final long serialVersionUID = 1L;
	private SeqMapView seqmapview;
	private NeoSeq seqview;
	private JFrame mapframe;
	private String seq;
	private int pixel_width = 500;
	private int pixel_height = 400;
	private SeqSpan[] seqSpans = null;
	private GenometryModel gm = GenometryModel.getGenometryModel();
	private String version = "";
	private SeqSymmetry residues_sym;
	BioSeq aseq;
	boolean isGenomicRequest;
	SequenceViewer sv;
	String errorMessage = null;
	private int seqStart = 0;
	private int seqEnd = 0;
	private int cdsMax = 0;
	private int cdsMin = 0;
	private int counter = 0;
	private int countIntronsBeforecdsMax = 0;
	private int countIntronsBeforecdsMin = 0;
	private String id = null, type = null;
	private String chromosome = null;
	private String title = null;
	private int cdsMaxDNAonly = 0;
	private int cdsMinDNAonly = 0;
	private String[] seqArray;
	private String[] intronArray;
	private boolean showcDNASwitch = false;
	private boolean colorSwitch = false;
	private final static int EXON_COLOR = 1;
	private final static int INTRON_COLOR = 2;
	private boolean cdsFound = false;
	Color[] defaultColors = {Color.BLACK, Color.YELLOW, Color.WHITE};
	Color[] reverseColors = {Color.WHITE, Color.BLUE, Color.BLACK};
	Color[] okayColors = {Color.black, Color.black};
	Color[] reverted = {Color.white, Color.white};

	public SequenceViewer() {
		seqmapview = IGB.getSingleton().getMapView();
	}

	public void customFormatting(SeqSymmetry residues_sym) throws HeadlessException, NumberFormatException {

		seqview.setFont(new Font("Arial", Font.BOLD, 14));
		seqview.setNumberFontColor(Color.black);
		seqview.setSpacing(20);
		this.getTitle();
		mapframe.setTitle(title);
		mapframe.setLayout(new BorderLayout());
		mapframe = setupMenus(mapframe);
		Dimension prefsize = seqview.getPreferredSize(50, 15);
		prefsize = new Dimension(prefsize.width + 90, prefsize.height);
		mapframe.setMinimumSize(prefsize);
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		mapframe.setLocation((screen_size.width - pixel_width) / 2, (screen_size.height - pixel_height) / 2);
		mapframe.setVisible(true);
	}

	private Color[] getColorScheme() {
		if (colorSwitch) {
			seqview.setStripeColors(reverted);
			return reverseColors;
		} else {
			seqview.setStripeColors(okayColors);
			return defaultColors;
		}
	}

	private void addFormattedResidues() {

		//Below is done because NeoSeq has first character as white space
		seqview.setResidues("");
		int count = 0;
		Color[] cols = getColorScheme();
		for (int j = 0, k = 0, l = 0; j < (2 * seqSpans.length) - 1; j++) {
			if ((j % 2) == 0) {
				seqview.appendResidues(seqArray[k]);
				seqview.addTextColorAnnotation(count, (count + seqArray[k].length()) - 1, cols[EXON_COLOR]);
				count += seqArray[k].length();
				k++;
			} else {
				seqview.appendResidues(intronArray[l]);
				seqview.addTextColorAnnotation(count, (count + intronArray[l].length()) - 1, cols[INTRON_COLOR]);
				count += intronArray[l].length();
				if (cdsMax >= count) {
					countIntronsBeforecdsMax++;
				}
				if (cdsMin >= count) {
					countIntronsBeforecdsMin++;
				}

				l++;
			}
		}
	}

	private void areResiduesFine(SeqSymmetry residues_sym, BioSeq aseq, boolean isGenomic) {
		this.residues_sym = residues_sym;
		this.aseq = aseq;
		seq = SeqUtils.selectedAllResidues(residues_sym, aseq);
		SeqSpan span = residues_sym.getSpan(aseq);
		if (seq != null) {
			if (aseq.isAvailable(span.getMin(), span.getMax())) {
				if (isGenomicRequest && residues_sym.getID() == null) {
					this.initSequenceViewer(residues_sym);
				} else if (isGenomicRequest && residues_sym.getID() != null) {
					this.errorMessage = "Please select the genomic sequence";
				} else if (!isGenomicRequest) {
					this.initSequenceViewer(residues_sym);
				}
			} else {
				this.errorMessage = "Residues are not loaded properly";
			}
		} else {
			this.errorMessage = "No residues found";
		}

	}

	public void startSequenceViewer() {

		List<SeqSymmetry> syms = SeqMapView.glyphsToSyms(seqmapview.getSeqMap().getSelected());
		if (syms.size() >= 1) {
			if (syms.size() == 1) {
				this.residues_sym = syms.get(0);
				this.isGenomicRequest = false;
			} else {
				if (syms.size() > 1 || seqmapview.getSeqSymmetry() != null) {
					this.errorMessage = "Multiple selections, please select one feature or a parent";
				}
			}
		} else {
			this.residues_sym = seqmapview.getSeqSymmetry();
			this.isGenomicRequest = true;
		}

		try {
			if (this.errorMessage == null) {
				this.aseq = seqmapview.getAnnotatedSeq();
				if (!isGenomicRequest) {
					if (!aseq.isAvailable(residues_sym.getSpan(aseq))) {
						boolean confirm = IGB.confirmPanel("Residues for " + aseq.getID()
								+ " not loaded.  \nDo you want to load residues?");
						if (!confirm) {
							return;
						}

						GeneralLoadView.getLoadView().loadResidues(residues_sym.getSpan(aseq), true);
						ThreadUtils.runOnEventQueue(new Runnable() {

							public void run() {
								seqmapview.setAnnotatedSeq(aseq, true, true, true);
							}
						});
					}
					areResiduesFine(residues_sym, aseq, isGenomicRequest);



				} else {
					areResiduesFine(residues_sym, aseq, isGenomicRequest);
				}
			}
		} catch (Exception e) {
			this.errorMessage = "Error loading residues";
		} finally {
			if (errorMessage != null) {
				ErrorHandler.errorPanel("Can not open sequence viewer", "" + this.errorMessage);

			}
		}
	}

	private void getTitle() {
		AnnotatedSeqGroup ag = gm.getSelectedSeqGroup();
		version = ag.getID();
		if(isGenomicRequest){
			title = "Genomic Sequence : " + version + " : " + this.aseq + " : " + residues_sym.getSpan(0).getStart() + " - " + (residues_sym.getSpan(0).getEnd() - 1);
//			seqview.setFirstOrdinal(residues_sym.getSpan(0).getStart());
			showcDNAButton.setEnabled(false);
		}
		else{
		if (residues_sym.getID() != null) {
			id = residues_sym.getID();
			Map<String, Object> sym = ((SymWithProps) residues_sym).getProperties();
			if (sym != null) {
				Iterator<String> iterator = sym.keySet().iterator();

				while (iterator.hasNext()) {
					String key = iterator.next().toString();
					String value = sym.get(key).toString();
					if (key.equals("id")) {
						id = value;
					} else if (key.equals("forward")) {
//					String forward = value;
//					if (forward.equals("true")) {
//						direction = "+";
//					}
					} else if (key.equals("type")) {
						type = value;
						if (type == null) {
							type = "";
						}
					} else if (key.equals("seq id")) {
						chromosome = value;
						if (chromosome == null) {
							chromosome = "";
						}
					} else if (key.equals("cds max")) {
						cdsMax = Integer.parseInt(value);
					} else if (key.equals("cds min")) {
						cdsFound = true;
						cdsMin = Integer.parseInt(value);
					}
				}
			}

			}
		if(id == null){
			title = version + " : " + this.aseq;
		}
		else{
			title = id + " : " + version + " : " + this.aseq;
		}
				this.calculateCdsStartEnd();
				//title = version + " : " + type + " : " + chromosome + " : " + id + " : " + direction;
		}


	}

	private void calculateCdsStartEnd() {
		int i = 0;
		if (seqSpans[0].getStart() < seqSpans[0].getEnd()) {
			cdsMin = cdsMin - seqSpans[0].getStart();
			cdsMax = cdsMax - seqSpans[0].getStart() - 3;
		} else {
			int temp = cdsMin;
			cdsMin = Math.abs(cdsMax - seqSpans[seqSpans.length - 1].getStart());
			cdsMax = Math.abs(temp - seqSpans[seqSpans.length - 1].getStart()) - 3;
		}
	}

	private void calculateNewCdsStartEnd() {
		counter = 0;
		while (countIntronsBeforecdsMax > 0) {
			counter += intronArray[countIntronsBeforecdsMax - 1].length();
			countIntronsBeforecdsMax--;
		}
		cdsMaxDNAonly = cdsMax - counter;
		counter = 0;
		while (countIntronsBeforecdsMin > 0) {
			counter += intronArray[countIntronsBeforecdsMin - 1].length();
			countIntronsBeforecdsMin--;
		}
		cdsMinDNAonly = cdsMin - counter;
	}

	private void addCdsStartEnd(SeqSymmetry residues_sym) throws NumberFormatException, HeadlessException {
		if (showcDNASwitch) {
			if (seqview.getResidues().length() > cdsMinDNAonly && cdsMinDNAonly >= 0 && cdsFound) {
				seqview.addOutlineAnnotation(cdsMinDNAonly, cdsMinDNAonly + 2, Color.green);
			}
			if (seqview.getResidues().length() > cdsMaxDNAonly && cdsMaxDNAonly >= 0 && cdsFound) {
				seqview.addOutlineAnnotation(cdsMaxDNAonly, cdsMaxDNAonly + 2, Color.red);
			}
		} else {
			if (seqview.getResidues().length() > cdsMin && cdsMin >= 0 && cdsFound) {
				seqview.addOutlineAnnotation(cdsMin, cdsMin + 2, Color.green);
			}
			if (seqview.getResidues().length() > cdsMax && cdsMax >= 0 && cdsFound) {
				seqview.addOutlineAnnotation(cdsMax, cdsMax + 2, Color.red);
			}
		}
	}

	private void convertSpansForSequenceViewer(SeqSpan[] spans, String seq) {
		int i = 1;
		if (spans[0].getStart() < spans[0].getEnd()) {
			seqArray[0] = seq.substring(0, spans[0].getLength());
			seqStart = spans[0].getMin();
			seqEnd = spans[spans.length - 1].getMax();
		} else {
			seqArray[0] = seq.substring(0, spans[spans.length - 1].getLength());
			seqStart = spans[spans.length - 1].getMin();
			seqEnd = spans[0].getMax();
		}
		if (spans.length > 1) {
			if ((spans[0].getStart() > spans[0].getEnd()) && (spans[0].getStart() < spans[1].getStart())) {
				SeqSpan[] spans_duplicate = new SeqSpan[spans.length];
				for (int k = 0; k < spans.length; k++) {
					spans_duplicate[spans.length - 1 - k] = spans[k];
				}
				spans = spans_duplicate;
			}
			intronArray[0] = seq.substring(spans[0].getLength(), Math.abs(spans[i].getStart() - spans[0].getStart()));
		}
		while (i < spans.length) {

			seqArray[i] = seq.substring(Math.abs(spans[i].getStart() - spans[0].getStart()), Math.abs(spans[i].getEnd() - spans[0].getStart()));
			if (i < spans.length - 1) {
				intronArray[i] = seq.substring(Math.abs(spans[i].getEnd() - spans[0].getStart()), Math.abs(spans[i + 1].getStart() - spans[0].getStart()));

			}
			i++;
		}

		i = 0;
		return;
	}

	protected void getGoing(SeqSymmetry residues_sym) {
		this.getNeoSeqInstance();
		generateSpansArray(residues_sym);
		if (null == seqSpans) {
			seqview.setResidues(seq);
			seqview.setResidueFontColor(getColorScheme()[EXON_COLOR]);
			customFormatting(residues_sym);
		} else {
			seqArray = new String[seqSpans.length];
			intronArray = new String[seqSpans.length - 1];
			convertSpansForSequenceViewer(seqSpans, seq);
			customFormatting(residues_sym);
			addFormattedResidues();
			this.addCdsStartEnd(residues_sym);
		}
		this.calculateNewCdsStartEnd();
		mapframe.add("Center", seqview);
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

	private void generateSpansArray(SeqSymmetry residues_sym) {
		if (residues_sym != null) {
			int numberOfChild = residues_sym.getChildCount();
			if (numberOfChild > 0) {
				int i = 0;
				seqSpans = new SeqSpan[numberOfChild];
				while (i < numberOfChild) {
					seqSpans[i] = residues_sym.getChild(i).getSpan(aseq);
					i++;
				}
			} else {
				seqSpans = new SeqSpan[1];
				seqSpans[0] = residues_sym.getSpan(0);
			}
		}
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
						firstLine = title + " : " + seqStart + " - " + seqEnd;
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
	JCheckBoxMenuItem transOneCBMenuItem = new JCheckBoxMenuItem(" +1 Translation");
	JCheckBoxMenuItem transTwoCBMenuItem = new JCheckBoxMenuItem(" +2 Translation");
	JCheckBoxMenuItem transThreeCBMenuItem = new JCheckBoxMenuItem(" +3 Translation");
	JCheckBoxMenuItem transNegOneCBMenuItem = new JCheckBoxMenuItem(" -1 Translation");
	JCheckBoxMenuItem transNegTwoCBMenuItem = new JCheckBoxMenuItem(" -2 Translation");
	JCheckBoxMenuItem transNegThreeCBMenuItem = new JCheckBoxMenuItem(" -3 Translation");
	JCheckBoxMenuItem colorScheme1 = new JCheckBoxMenuItem("Yellow on black");
	JCheckBoxMenuItem colorScheme2 = new JCheckBoxMenuItem("Blue on white");
	JMenuItem exportRComplementFasta = new JMenuItem("Save As Fasta(Reverse Complement)");
	JMenu showMenu = new JMenu("Show");
	JMenu fileMenu = new JMenu("File");
	JMenu editMenu = new JMenu("Edit");
	JMenu colorMenu = new JMenu("Colors");
	CopyFromSeqViewerAction copyAction = new CopyFromSeqViewerAction(this);

	public JFrame setupMenus(JFrame dock) {

		copyAction.setEnabled(false);
		MenuUtil.addToMenu(fileMenu, new JMenuItem(new ExportFastaSequenceAction(this)));
		MenuUtil.addToMenu(fileMenu, exportRComplementFasta);
		MenuUtil.addToMenu(fileMenu, new JMenuItem(new ExitSeqViewerAction(this.mapframe)));
		MenuUtil.addToMenu(editMenu, new JMenuItem(copyAction));
		editMenu.addMenuListener(this);
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
			Clipboard clipboard = this.getToolkit().getSystemClipboard();
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
		if (!showcDNASwitch) {
			addFormattedResidues();
		} else {
			String seq1 = null;
			seq1 = SeqUtils.determineSelectedResidues(this.residues_sym, this.aseq);
			seqview.setResidues(seq1);
			seqview.addTextColorAnnotation(0, seq1.length(), getColorScheme()[EXON_COLOR]);
		}
		if (!isGenomicRequest) {
			this.addCdsStartEnd(residues_sym);
		}
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
		String seq1 = null;
		seq1 = SeqUtils.determineSelectedResidues(this.residues_sym, this.aseq);
		if (evtSource == showcDNAButton) {
			String text = e.getActionCommand();
			if (text.equals("Show cDNA")) {
				showcDNASwitch = true;
				seqview.clearWidget();
				seqview.setResidues(seq1);
				seqview.addTextColorAnnotation(0, seq1.length(), getColorScheme()[EXON_COLOR]);
				this.addCdsStartEnd(residues_sym);
				seqview.updateWidget();
				showcDNAButton.setText("Show genomic");
			} else {
				showcDNASwitch = false;
				seqview.clearWidget();
				addFormattedResidues();
				this.addCdsStartEnd(residues_sym);
				showcDNAButton.setText("Show cDNA");
			}
		} else if (evtSource == reverseColorsButton) {
			String text = e.getActionCommand();
			if (text.equals("Change color scheme")) {
				reverseColorsButton.setText("Revert color scheme");
				colorSwitch = true;
			} else {
				reverseColorsButton.setText("Change color scheme");
				colorSwitch = false;
			}
			seqview.clearWidget();
			if (!showcDNASwitch) {
				addFormattedResidues();
			} else {
				seqview.setResidues(seq1);
				seqview.addTextColorAnnotation(0, seq1.length(), getColorScheme()[EXON_COLOR]);
			}
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
