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

package genoviz.demo;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.datamodel.BaseCalls;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.datamodel.Trace;
import com.affymetrix.genoviz.datamodel.TraceI;
import com.affymetrix.genoviz.parser.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.neotracer.TraceGlyph;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.WindowConstants;


/**
 *
 * @version $Id$
 */
public class NeoTracerDemo extends Applet
	implements ActionListener, NeoRangeListener {

	NeoPanel widg_pan;
	boolean clone_in_same_frame = true;
	boolean optScrolling = false, optDamage = false;
	boolean external_zoomers = false;
	TraceI trace;
	public NeoTracer widget;
	NeoTracer oneClone;
	Adjustable xzoomer, yzoomer;
	Frame zoomframe = new Frame("Zoom controls");

	int pixel_width = 500;
	int pixel_height = 250;

	int scroll_value = 100;
	float xzoom_value = 2.0f;
	float yzoom_value = 0.1f;

	Button scrollLeftB, scrollRightB, optDamageB, optScrollingB;
	Button toggleTraceB, toggleRevCompB;
	Button xzoomB, yzoomB;
	Button clipper;

	TextField posText;
	Button findButton;
	TextField strText;
	Label posLabel;
	Label descLabel;
	Choice searchChoice;
	Button cloneButton;
	Panel controlPanel;

	Menu editMenu = new Menu("Edit");
	MenuItem propertiesMenuItem = new MenuItem("Properties...");
	Frame propFrame; // For Properties
	private boolean propFrameShowing = false;

	private ActionListener searcher = new ActionListener() {
		public void actionPerformed( ActionEvent evt ) {
			String searchString = strText.getText().toUpperCase();
			if ( searchString.length() < 1 ) return;
			String traceString;
			BaseCalls bc = ( ( Trace ) trace ).getActiveBaseCalls();
			// Ann's note:getBaseString is
			// removed from v. 6
			// See: http://genoviz.svn.sourceforge.net/viewvc/genoviz/trunk/genoviz_sdk/src/com/affymetrix/genoviz/datamodel/TraceI.java?r1=6&r2=2846
			// The method was also noted as being deprecated and 
			// the comments advised using getActiveBaseCalls().getBaseString()
			// instead. 
			// 
			/**

			  if ( null == bc ) {
			  traceString = trace.getBaseString();
			  }
			  else {
			  */
			traceString = bc.getBaseString().toUpperCase();
			//}
			String searchOption = searchChoice.getSelectedItem();
			int basenum = -1;
			if (searchOption == "First") {
				basenum = traceString.indexOf(searchString);
			}
			else if (searchOption == "Last") {
				basenum = traceString.lastIndexOf(searchString);
			}
			else if (searchOption == "Next") {
				basenum = traceString.indexOf(searchString, prevSearchPosition+1);
			}
			else if (searchOption == "Prev") {
				basenum = traceString.lastIndexOf(searchString,
						prevSearchPosition-1);
			}
			if (basenum == -1) {
				showStatus( "Could not find \"" + searchString + "\"" );
			}
			else {
				showStatus( "Found it starting at " + basenum );
				centerAtBase(basenum);
				prevSearchPosition = basenum;
				widget.selectResidues( basenum, basenum + searchString.length() - 1 );
			}
		}
	};

	public NeoTracerDemo() {
		if (external_zoomers) {
			widget = new NeoTracer(true, false, false);
			xzoomer = new JScrollBar(JScrollBar.HORIZONTAL);
			yzoomer = new JScrollBar(JScrollBar.HORIZONTAL);
			widget.setZoomer(NeoTracer.X, xzoomer);
			widget.setZoomer(NeoTracer.Y, yzoomer);
		}
		else {
			widget = new NeoTracer();
		}
		widget.addRangeListener(this);

		descLabel = new Label("");
		this.setLayout(new BorderLayout());

		// moved from init to constructor -- GAH 3-30-99
		widg_pan = new NeoPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", (Component)widget);
		this.setLayout(new BorderLayout());
		this.add("Center", widg_pan);
	}

	@Override
	public String getAppletInfo() {
		return ("Demonstration of genoviz Software's Trace Viewing Widget");
	}

	@Override
	public void init() {

		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);
		propertiesMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent evt ) {
				showProperties();
			}
		});

		xzoomB = new Button("XZoom");
		xzoomB.addActionListener(this);
		yzoomB = new Button("YZoom");
		yzoomB.addActionListener(this);

		scrollLeftB = new Button("Scroll Left");
		scrollLeftB.addActionListener(this);
		scrollRightB = new Button("Scroll Right");
		scrollRightB.addActionListener(this);
		optScrollingB = new Button("No Scroll Opt");
		optScrollingB.addActionListener(this);
		optDamageB = new Button("No Damage Opt");
		optDamageB.addActionListener(this);
		toggleTraceB = new Button("Toggle G");
		toggleTraceB.addActionListener(this);
		toggleRevCompB = new Button("RevComp");
		toggleRevCompB.addActionListener(this);
		clipper = new Button("Clip");
		clipper.addActionListener(this);

		posText = new TextField(3);
		posText.addActionListener(this);
		posLabel = new Label("Center At Loc:");
		strText = new TextField(10);
		strText.addTextListener( new TextListener() {
			public void textValueChanged( TextEvent e ) {
				findButton.setEnabled( 0 < strText.getText().length() );
			}
		} );
		strText.addActionListener( this.searcher );
		findButton = new Button( "Find:" );
		findButton.addActionListener( this.searcher );
		searchChoice = new Choice();
		searchChoice.addItem("Next");
		searchChoice.addItem("Prev");
		searchChoice.addItem("First");
		searchChoice.addItem("Last");
		cloneButton = new Button("Clone");
		cloneButton.addActionListener(this);

		controlPanel = new Panel();
		//    controlPanel.setBackground(Color.white);
		//    controlPanel.add(scrollLeftB);
		//    controlPanel.add(scrollRightB);

		controlPanel.add(descLabel);
		//    controlPanel.add(posLabel);
		//    controlPanel.add(posText);
		controlPanel.add(findButton);
		controlPanel.add(searchChoice);
		controlPanel.add(strText);
		controlPanel.add(cloneButton);

		//    controlPanel.add(xzoomB);
		//    controlPanel.add(yzoomB);

		controlPanel.add(toggleRevCompB);
		controlPanel.add(toggleTraceB);
		controlPanel.add(clipper);

		//    controlPanel.add(optScrollingB);
		//    controlPanel.add(optDamageB);

		if (external_zoomers)  {
			zoomFrameSetup();
		}

		((Component)widget).setSize(pixel_width, pixel_height);

		String param;
		param = getParameter("noControlPanel");
		if (null == param) {
			add("North", controlPanel);
		}

		String filestr = new String();
		try {
			String scff = getParameter("scf_file");
			if (scff != null) {
				URL scfURL = new URL(this.getDocumentBase(), scff);
				if ( null != scfURL ) {
					SCFTraceParser scfp = new SCFTraceParser();
					Trace t = (Trace) scfp.importContent(scfURL.openStream());
					setTrace(t);
					filestr = scfURL.getFile();
				}
			}
			else {
				String abif = getParameter("abi_file");
				if (null != abif) {
					URL abiURL = new URL(this.getDocumentBase(), abif);
					if ( null != abiURL ) {
						ABITraceParser abip = new ABITraceParser();
						Trace t = (Trace) abip.importContent(abiURL.openStream());
						setTrace(t);
						filestr = abiURL.getFile();
					}
				}
			}
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}

		param = getParameter( "phd" );
		if ( null != param ) {
			try {
				URL r = new URL( this.getDocumentBase(), param );
				if ( null != r ) {
					ReadConfidence rc = new ReadConfidence();
					ContentParser parser = new PHDReadConfParser();
					rc = ( ReadConfidence ) parser.importContent( r.openStream() );
					widget.replaceBaseCalls( rc.getBaseCalls() );
				}
			}
			catch ( Exception phdex ) {
				System.err.println( phdex.getMessage() );
				phdex.printStackTrace();
			}
		}

		param = getParameter( "addphd" );
		if ( null != param ) {
			try {
				URL r = new URL( this.getDocumentBase(), param );
				if ( null != r ) {
					ReadConfidence rc = new ReadConfidence();
					ContentParser parser = new PHDReadConfParser();
					rc = ( ReadConfidence ) parser.importContent( r.openStream() );
					widget.addBaseCalls( rc.getBaseCalls() );
				}
			}
			catch ( Exception phdex ) {
				System.err.println( phdex.getMessage() );
				phdex.printStackTrace();
			}
		}

		if (null != trace) {

			int tempint = filestr.lastIndexOf('/');
			if (tempint != -1) {
				filestr = filestr.substring(tempint+1);
			}

			int trace_length = trace.getTraceLength();

			setTraceLabel(filestr + ": " + trace.getBaseCount() + " bases");

		}

		widget.setBasesTrimmedLeft(9);
		widget.setBasesTrimmedRight(19);

	}

	public void setTrace(TraceI trace) {
		if (trace == null) {
			System.err.println("no trace!");
			return;
		}
		this.trace = trace;
		widget.setTrace(trace);
	}

	public void setTraceLabel(String label) {
		descLabel.setText(label);
	}


	@Override
	public void start() {
		Container parent;
		parent = this.getParent();
		while (null != parent && ! (parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if ( null != parent ) {
			MenuBar mb = ((Frame)parent).getMenuBar();
			if ( null != mb ) mb.add(editMenu);
		}

		if (external_zoomers)  {
			zoomframe.setVisible(true); //zoomframe.show();
		}
		if (null != propFrame && propFrameShowing) {
			propFrame.setVisible(true);//propFrame.show();
		}
		super.start();
		NeoTracer nt = widget;

	}

	@Override
	public void stop() {
		Container parent;
		parent = this.getParent();
		while (null != parent && ! (parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if ( null != parent ) {
			MenuBar mb = ( ( Frame ) parent ).getMenuBar();
			if ( null != mb ) mb.remove( editMenu );
		}
		if (null != propFrame) {
			propFrameShowing = propFrame.isVisible();
			propFrame.setVisible(false);
		}
		super.stop();
	}


	public void showProperties() {
		if (null == propFrame) {
			propFrame = new Frame("NeoTracer Properties");
			NeoTracerCustomizer customizer = new NeoTracerCustomizer();
			customizer.setObject(this.widget);
			propFrame.add("Center", customizer);
			propFrame.pack();
			propFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					propFrameShowing = false;
					propFrame.setVisible(false);
				}
			});
		}
		propFrame.setBounds(200, 200, 500, 300);
		propFrame.setVisible(true);//propFrame.show();
	}

	public void centerAtBase(int baseNum) {
		widget.centerAtBase(baseNum);
		widget.updateWidget();
	}

	int prevSearchPosition = -1;

	public void actionPerformed(ActionEvent evt)  {
		Object evtSource = evt.getSource();
		if (evtSource == posText) {
			try  {
				int basenum = Integer.parseInt(posText.getText());
				centerAtBase(basenum);
			}
			catch(Exception ex) { System.out.println("parse error"); }
		}
		else if (evtSource == xzoomB) {
			xzoom_value *= 1.1;
			widget.zoom(NeoTracer.X, xzoom_value);
			widget.updateWidget();
		}
		else if (evtSource == yzoomB) {
			yzoom_value *= 1.1;
			widget.zoom(NeoTracer.Y, yzoom_value);
			widget.updateWidget(true);
		}
		else if (evtSource == scrollLeftB) {
			scroll_value -= 5;
			widget.scroll(NeoTracer.X, scroll_value);
			widget.updateWidget();
		}
		else if (evtSource == scrollRightB) {
			scroll_value += 5;
			widget.scroll(NeoTracer.X, scroll_value);
			widget.updateWidget();
		}
		else if (evtSource == optScrollingB) {
			optScrolling = !optScrolling;
			widget.setScrollingOptimized(optScrolling);
			widget.updateWidget();
			if (optScrolling) {
				optScrollingB.setLabel("Opt Scrolling");
			}
			else {
				optScrollingB.setLabel("No Opt Scrolling");
			}
		}
		else if (evtSource == optDamageB) {
			optDamage = !optDamage;
			widget.setDamageOptimized(optDamage);
			widget.updateWidget();
			if (optDamage) {
				optDamageB.setLabel("Opt Damage");
			}
			else {
				optDamageB.setLabel("No Opt Damage");
			}
		}
		else if (evtSource == toggleTraceB) {
			widget.setVisibility(TraceGlyph.G, !widget.getVisibility(TraceGlyph.G));
			widget.updateWidget();
		}
		else if (evtSource == clipper) {
			clipTrace(100, 200);
			widget.updateWidget();
		}
		else if (evtSource == toggleRevCompB) {
			if (NeoTracer.FORWARD == widget.getDirection()) {
				widget.setDirection(NeoTracer.REVERSE_COMPLEMENT);
			}
			else {
				widget.setDirection(NeoTracer.FORWARD);
			}
			widget.updateWidget();
		}
		else if (evtSource == cloneButton) {
			cloneWidget();
			cloneButton.setEnabled(false);
		}
	}


	public void cloneWidget() {
		oneClone = new NeoTracer(widget);
		if (clone_in_same_frame) {
			widg_pan.remove((Component)widget);
			widg_pan.setLayout(new GridLayout(0, 1));
			widg_pan.add((Component)widget);
			widg_pan.add((Component)oneClone);
			doLayout();
			validate();
		}
		else {
			Frame cloneFrame = new Frame("NeoTracer clone");
			cloneFrame.setLayout(new BorderLayout());
			cloneFrame.setSize(400, 200);
			Panel new_pan = new NeoPanel();
			new_pan.setLayout(new BorderLayout());
			new_pan.add("Center", (Component)oneClone);
			cloneFrame.add("Center", widg_pan);
			cloneFrame.setVisible(true);//cloneFrame.show();
		}
	}

	/**
	 * Testing external zoom controls
	 */
	public void zoomFrameSetup() {
		zoomframe.setBackground(Color.white);
		zoomframe.setLayout(new BorderLayout());
		zoomframe.add("South", (JScrollBar)xzoomer);
		zoomframe.add("North", (JScrollBar)yzoomer);
		zoomframe.pack();
		zoomframe.setSize(200, zoomframe.getSize().height);
	}

	public void setTraceColors(Color[] colors) {
		widget.setTraceColors(colors);
	}

	public void setBasesBackground(Color col) {
		widget.setBackground(NeoTracer.BASES, col);
	}
	public void setTracesBackground(Color col) {
		widget.setBackground(NeoTracer.TRACES, col);
	}

	public void clipTrace(int theFirstBase, int theLastBase) {
		System.err.println("clipping from base " + theFirstBase + " to " + theLastBase);
		int theFirstPeak = 0;
		if ( 0 < theFirstBase ) {
			// Ann's note: It seems weird to me that we have to cast the interface trace to
			// its implementation. Why is this? Anyhow, to get this file to compile, I need
			// to cast trace to Trace, which implements TraceI
			int a = (((Trace)trace).getBaseCall(theFirstBase-1)).getTracePoint();
			int b = (((Trace)trace).getBaseCall(theFirstBase)).getTracePoint();
			theFirstPeak = ( a + b ) / 2;
		}
		int theLastPeak = trace.getTraceLength() - 1;
		if ( theLastBase < trace.getBaseCount() - 1 ) {
			int a = ((Trace)trace).getBaseCall(theLastBase).getTracePoint();
			int b = ((Trace) trace).getBaseCall(theLastBase+1).getTracePoint();
			theLastPeak = ( a + b ) / 2;
		}
		widget.setRange(theFirstPeak, theLastPeak);

	}

	public void rangeChanged(NeoRangeEvent evt) {
	}

	@Override
	public URL getCodeBase()
	{
		if (isApplication) {
				return this.getClass().getResource("/");
			}
		return super.getCodeBase();
	}


	@Override
	public AppletContext getAppletContext()
	{
		if(isApplication)
			return null;
		return super.getAppletContext();
	}


	@Override
	public URL getDocumentBase()
	{
		if(isApplication)
			return getCodeBase();
		return super.getDocumentBase();
	}

	@Override
	public String getParameter(String name)
	{
		if(isApplication)
			return parameters.get(name);
		return super.getParameter(name);
	}

	static Boolean isApplication = false;
	static Hashtable<String,String> parameters;
	static public void main(String[] args)
	{
		isApplication = true;
		NeoTracerDemo me = new NeoTracerDemo();
		parameters = new Hashtable<String, String>();
		parameters.put("abi_file","data/traceTest/trace.abi");
		parameters.put("phd","data/traceTest/trace.phd");
		me.init();
		me.start();
		JFrame frm = new JFrame("NeoQuallerDemo");
		frm.getContentPane().add("Center", me);
		JButton properties = new JButton("Properties");
		frm.getContentPane().add("South", properties);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		//frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}
}
