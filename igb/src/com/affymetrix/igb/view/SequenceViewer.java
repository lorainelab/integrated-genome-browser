/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.widget.NeoSeq;
import com.affymetrix.genoviz.widget.NeoSeqCustomizer;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.action.CopyFromSeqViewerAction;
import com.affymetrix.igb.action.ExitSeqViewerAction;
import com.affymetrix.igb.action.ExportFastaSequenceAction;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.concurrent.Executor;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

public class SequenceViewer extends JPanel
		implements WindowListener, ItemListener {

	protected SeqMapView seqmapview;
	private NeoSeq seqview;
	private JFrame mapframe;
	private Frame propframe;
	private Sequence seqmodel;
	private Vector<GlyphI> annotations;
	private String seq;
	// Fake Sequences for Testing
	private boolean use_real_seq = false;
	// All the Standard Nucleotide Code Letters
	private String fake_seq = "ACGT UMRWSYK VHDBXN."; // The two gaps make it 0 mod 10 long.
	private boolean showComp = false; // show complementary strand?
	private int pixel_width = 500;
	private int pixel_height = 400;
	private Color text_annot_color = Color.blue;
	private Color back_annot_color = Color.green;
	private Color out_annot_color = Color.white;
	private Image backgroundImage = null;
	private boolean clicking = false;
	private NeoSeqCustomizer customizer;
	private boolean framesShowing = true;
	private boolean going = false;
	private Color nicePaleBlue = new Color(180, 250, 250);
	private SeqSpan[] seqSpans = null;
	private Boolean isGenomic = false;
	private GenometryModel gm = GenometryModel.getGenometryModel();
	private String version = "";
	private SeqSymmetry residues_sym;
	BioSeq aseq;
	boolean isGenomicRequest;
	SequenceViewer sv;
	String errorMessage = null;

	public SequenceViewer() {
		seqmapview = IGB.getSingleton().getMapView();

	}

	public void customFormatting(SeqSymmetry residues_sym, String seq) throws HeadlessException, NumberFormatException {
		Color[] okayColors = {Color.black, Color.black};
		seqview.setStripeColors(okayColors);
		seqview.setFont(new Font("Arial", Font.BOLD, 14));
		seqview.setNumberFontColor(Color.black);
		seqview.setSpacing(20);
		if (residues_sym.getID() != null) {
			isGenomic = true;
			addCdsStartEnd(residues_sym);
		} else {
			AnnotatedSeqGroup ag = gm.getSelectedSeqGroup();
			mapframe = new JFrame("Genomic Sequence - " + ag.getID());
			seqview.setFirstOrdinal(residues_sym.getSpan(0).getStart());
			isGenomic = true;
//			seqview.forSequenceViewer = true;
		}
		mapframe.setLayout(new BorderLayout());
		mapframe = setupMenus(mapframe);
		mapframe.setLayout(new BorderLayout());
		mapframe.add("Center", seqview);
		Dimension prefsize = seqview.getPreferredSize(50, 15);
		mapframe.setMinimumSize(prefsize);
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		mapframe.setLocation((screen_size.width - pixel_width) / 2, (screen_size.height - pixel_height) / 2);
		mapframe.setVisible(true);
	}

	private void areResiduesFine(SeqSymmetry residues_sym, BioSeq aseq, boolean isGenomic){
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
		if (errorMessage != null) {
				ErrorHandler.errorPanel("Can not open sequence viewer", "" + this.errorMessage);

			}

	}

	public void startSequenceViewer(final boolean isGenomic) {
		sv = new SequenceViewer();
		this.isGenomicRequest = isGenomic;
		try {
			this.aseq = seqmapview.getAnnotatedSeq();
			if (!isGenomic) {
				List<SeqSymmetry> syms = seqmapview.glyphsToSyms(seqmapview.getSeqMap().getSelected());
				if (syms.size() == 1) {
					this.residues_sym = syms.get(0);
				} else {
					this.errorMessage = "Multiple selections, please select one feature or a parent";
					return;
				}
				Executor vexec = ThreadUtils.getPrimaryExecutor(new Object());

				vexec.execute(new Runnable() {

					public void run() {
						GeneralLoadView.getLoadView().loadResidues(residues_sym.getSpan(aseq), true);
						seqmapview.setAnnotatedSeq(aseq, true, true, true);
						sv.areResiduesFine(residues_sym, aseq, isGenomic);
					}
				});


			} else {
				this.residues_sym = seqmapview.getSeqSymmetry();
				sv.areResiduesFine(residues_sym, aseq, isGenomic);

			}
		} catch (Exception e) {
			this.errorMessage = "Error loading residues";
		} finally {
			if (errorMessage != null) {
				ErrorHandler.errorPanel("Can not open sequence viewer", "" + this.errorMessage);
				
			}
		}
	}

	public void init(final SeqSymmetry residues_sym) {

		/** Using an inner class to catch mouseReleased (nee mouseUp) */
		this.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent evt) {
				if (clicking) {
					if (framesShowing) {
						hideFrames();
					} else {
						showFrames(residues_sym);
					}
					framesShowing = !framesShowing;
				}
			}
		});

		seqview = new NeoSeq();

		seqview.addKeyListener(new KeyAdapter() {
			// Why is this not getting called?

			@Override
			public void keyPressed(KeyEvent evt) {
				System.err.println("NeoSeqDemo saw key pressed.");
			}
		});

//		String ref = getParameter("seq_file");
//		this.use_real_seq = ( null != ref );
//		if ( this.use_real_seq ) {
//			seqmodel = loadSequence( ref );
//		}

	}

	private void addCdsStartEnd(SeqSymmetry residues_sym) throws NumberFormatException, HeadlessException {
		String id = null, type = null;
		String chromosome = null;
		String forward = null;
		int cdsMax = 0;
		int cdsMin = 0;
		//		((SymWithProps) residues_sym).getProperty(seq)
		Map<String, Object> sym = ((SymWithProps) residues_sym).getProperties();
		Iterator iterator = sym.keySet().iterator();
		AnnotatedSeqGroup ag = gm.getSelectedSeqGroup();
		while (iterator.hasNext()) {
			String key = iterator.next().toString();
			String value = sym.get(key).toString();
			if (key.equals("id")) {
				id = value;
			} else if (key.equals("forward")) {
				forward = value;
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
				cdsMin = Integer.parseInt(value);
			}
		}
		version = ag.getID();
//		System.out.println("cds min  "+ cdsMin +"  cds max "+cdsMax+"  end "+ seqSpans[seqSpans.length-1].getStart());
		if (seqSpans[0].getStart() < seqSpans[0].getEnd()) {
			seqview.addOutlineAnnotation(cdsMin - seqSpans[0].getStart(), cdsMin - seqSpans[0].getStart() + 2, Color.green);
			seqview.addOutlineAnnotation(cdsMax - seqSpans[0].getStart() - 3, cdsMax - seqSpans[0].getStart() - 1, Color.red);
		} else {
			seqview.addOutlineAnnotation(Math.abs(cdsMax - seqSpans[seqSpans.length - 1].getStart()), Math.abs(cdsMax - seqSpans[seqSpans.length - 1].getStart()) + 2, Color.green);
			seqview.addOutlineAnnotation(Math.abs(cdsMin - seqSpans[seqSpans.length - 1].getStart()) - 3, Math.abs(cdsMin - seqSpans[seqSpans.length - 1].getStart()) - 1, Color.red);
		}
		//		String str = (((SymWithProps) residues_sym).getProperty("id")).toString()+" "+(((SymWithProps) residues_sym).getProperty("chromosome")).toString();
		mapframe = new JFrame();
		mapframe.setTitle(version + " " + id + " " + chromosome + " " + type);
	}

	private void convertSpansForSequenceViewer(String[] seqArray, String[] intronArray, SeqSpan[] spans, String seq) {
		int i = 1;
		if (spans[0].getStart() < spans[0].getEnd()) {
			seqArray[0] = seq.substring(0, spans[0].getLength());
		} else {
			seqArray[0] = seq.substring(0, spans[spans.length - 1].getLength());
		}
		if (spans.length > 1) {
			if (spans[0].getStart() > spans[0].getEnd()) {
				SeqSpan[] spans_duplicate = new SeqSpan[spans.length];
				for (int k = 0; k < spans.length; k++) {
					spans_duplicate[spans.length - 1 - k] = spans[k];
				}
				spans = spans_duplicate;
			}
			intronArray[0] = seq.substring(spans[0].getLength(), Math.abs(spans[i].getStart() - spans[0].getStart()));
//				System.out.println("intron array[0] "+intronArray[0]);

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

		going = true;

		seqview.enableDragScrolling(true);

		annotations = new Vector<GlyphI>();
		if (residues_sym != null) {
			int numberOfChild = residues_sym.getChildCount();
			if (numberOfChild > 0) {
				int i = 0;
				seqSpans = new SeqSpan[numberOfChild];
				while (i < numberOfChild) {
					seqSpans[i] = residues_sym.getChild(i).getSpan(0);
					i++;
				}
			} else {
				seqSpans = new SeqSpan[1];
				seqSpans[0] = residues_sym.getSpan(0);
			}
		}
		if (null == seqSpans) {
			seqview.setResidues(seq);
			seqview.setResidueFontColor(Color.YELLOW);
		} else {
			String[] seqArray = new String[seqSpans.length];
			String[] intronArray = new String[seqSpans.length - 1];
			convertSpansForSequenceViewer(seqArray, intronArray, seqSpans, seq);
			//Below is done because NeoSeq has first character as white space
			seqview.setResidues("");
			int count = 0;
			for (int j = 0, k = 0, l = 0; j < (2 * seqSpans.length) - 1; j++) {
				if ((j % 2) == 0) {
					seqview.appendResidues(seqArray[k]);
					seqview.addTextColorAnnotation(count, (count + seqArray[k].length()) - 1, Color.YELLOW);
					count += seqArray[k].length();
					k++;

				} else {
					seqview.appendResidues(intronArray[l]);
					seqview.addTextColorAnnotation(count, (count + intronArray[l].length()) - 1, Color.WHITE);
					count += intronArray[l].length();
					l++;

				}
			}

		}

		customFormatting(residues_sym, seq);
		seqview.setShow(NeoSeq.COMPLEMENT, showComp);
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

	private void showFrames(SeqSymmetry residues_sym) {
		if (!going) {
			getGoing(residues_sym);
		}
		mapframe.setVisible(true);
	}

	private void hideFrames() {
		if (null != this.propframe) {
			this.propframe.setVisible(false);
		}
		if (null != mapframe) {
			mapframe.setVisible(false);
		}
	}

	public void start(SeqSymmetry residues_sym) {
		if (framesShowing) {
			showFrames(residues_sym);
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

	public void exportSequenceFasta() {
		FileDialog fd = new FileDialog(mapframe, "Save As", FileDialog.SAVE);
		fd.setVisible(true);
		String fileName = fd.getFile();

		if (null != fileName) {
			try {

				FileWriter fw = new FileWriter(fd.getDirectory() + fileName);
				String r = seqview.getResidues();
//				String header =
//				">" +
//				chrom_name +
//				" range:" +
//				NumberFormat.getIntegerInstance().format(start) +
//				"-" +
//				NumberFormat.getIntegerInstance().format(end) +
//				" interbase genome:" +
//				genome_name +
//				"\n";
				fw.write(">" + fileName);
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
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	JCheckBoxMenuItem compCBMenuItem = new JCheckBoxMenuItem("Reverse Complement");
	JCheckBoxMenuItem transOneCBMenuItem = new JCheckBoxMenuItem(" +1 Translation");
	JCheckBoxMenuItem transTwoCBMenuItem = new JCheckBoxMenuItem(" +2 Translation");
	JCheckBoxMenuItem transThreeCBMenuItem = new JCheckBoxMenuItem(" +3 Translation");
	JCheckBoxMenuItem transNegOneCBMenuItem = new JCheckBoxMenuItem(" -1 Translation");
	JCheckBoxMenuItem transNegTwoCBMenuItem = new JCheckBoxMenuItem(" -2 Translation");
	JCheckBoxMenuItem transNegThreeCBMenuItem = new JCheckBoxMenuItem(" -3 Translation");

	public JFrame setupMenus(JFrame dock) {

		/* Edit Menu */


//		JMenuItem copyMenuItem = new JMenuItem("Copy selected sequence to clipboard");
//		//copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//
//		/* File Menu */
//		JMenu fileMenu = new JMenu("File");
//		JMenuItem saveAsMenuItem = new JMenuItem("save As", KeyEvent.VK_A);
//		JMenuItem exitMenuItem = new JMenuItem("eXit", KeyEvent.VK_X);
		JMenu showMenu = new JMenu("Show");
		JMenu fileMenu = new JMenu("File");
		JMenu editMenu = new JMenu("Edit");
		MenuUtil.addToMenu(fileMenu, new JMenuItem(new ExportFastaSequenceAction(this)));
		MenuUtil.addToMenu(fileMenu, new JMenuItem(new ExitSeqViewerAction(this.mapframe)));
		MenuUtil.addToMenu(editMenu, new JMenuItem(new CopyFromSeqViewerAction(this)));
//		copyMenuItem.addActionListener(this);
//
//		// file menu
//		fileMenu.add(saveAsMenuItem);
//		fileMenu.add(exitMenuItem);
//		saveAsMenuItem.addActionListener(this);
//		exitMenuItem.addActionListener(this);
		showMenu.add(compCBMenuItem);
		showMenu.add(transOneCBMenuItem);
		showMenu.add(transTwoCBMenuItem);
		showMenu.add(transThreeCBMenuItem);
		showMenu.add(transNegOneCBMenuItem);
		showMenu.add(transNegTwoCBMenuItem);
		showMenu.add(transNegThreeCBMenuItem);
//
		compCBMenuItem.addItemListener(this);
		transOneCBMenuItem.addItemListener(this);
		transTwoCBMenuItem.addItemListener(this);
		transThreeCBMenuItem.addItemListener(this);
		transNegOneCBMenuItem.addItemListener(this);
		transNegTwoCBMenuItem.addItemListener(this);
		transNegThreeCBMenuItem.addItemListener(this);

		// add the menus to the menubar
		JMenuBar bar = new JMenuBar();
//		if (null == bar) {
//			bar = new MenuBar();
//			dock.setMenuBar(bar);
//		}

		bar.add(fileMenu);
		bar.add(editMenu);
		bar.add(showMenu);
		dock.setJMenuBar(bar);
		return dock;
	}

	/* EVENT HANDLING */
	/** ActionListener Implementation */
	public void copyAction() {
		String selectedSeq = seqview.getSelectedResidues();
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
//	public void actionPerformed(ActionEvent e) {
//		Object theItem = e.getSource();
//
//		if (theItem == copyMenuItem) {
//			String selectedSeq = seqview.getSelectedResidues();
//			if (selectedSeq != null) {
//				Clipboard clipboard = this.getToolkit().getSystemClipboard();
//				StringBuffer hackbuf = new StringBuffer(selectedSeq);
//				String hackstr = new String(hackbuf);
//				StringSelection data = new StringSelection(hackstr);
//				clipboard.setContents(data, null);
//			} else {
//				ErrorHandler.errorPanel("Missing Sequence Residues",
//						"Don't have all the needed residues, can't copy to clipboard.\n"
//						+ "Please load sequence residues for this region.");
//			}
//
//		} else if (theItem == saveAsMenuItem) {
//			exportSequenceFasta();
//		} else if (theItem == exitMenuItem) {
//			mapframe.dispose();
//			this.destroy();
//		}
//
//	}

//	private void setMenuItemState(JMenu theMenu, JCheckBoxMenuItem theItem) {
//		for (int i = theMenu.getItemCount() - 1; 0 <= i; i--) {
//			JMenuItem item = theMenu.getItem(i);
//			if (item instanceof JCheckBoxMenuItem) {
//				((JCheckBoxMenuItem) item).setState(item == theItem);
//			}
//		}
//	}
	/** ItemListener Implementation */
	public void itemStateChanged(ItemEvent e) {
		Object theItem = e.getSource();

		if (theItem == compCBMenuItem) {
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem) theItem;
			boolean showRevComp = mi.getState();
			seqview.setRevShow(NeoSeq.COMPLEMENT, showRevComp);
			seqview.setRevShow(NeoSeq.NUCLEOTIDES, !showRevComp);
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
		System.setProperty("apple.laf.useScreenMenuBar", "false");
		init(residues_sym);
		start(residues_sym);

	}
}
