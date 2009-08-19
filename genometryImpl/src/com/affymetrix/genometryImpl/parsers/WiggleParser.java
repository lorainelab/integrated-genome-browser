package com.affymetrix.genometryImpl.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.awt.Color;

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.util.Iterator;


/**
 *  A parser for graph data in the UCSC browser Wiggle format.
 *  See http://genome.ucsc.edu/google/goldenPath/help/wiggle.html
 *  See http://gmod.org/wiki/GBrowse/Uploading_Wiggle_Tracks for test data.
 *  There are three sub-formats: BED4, VARSTEP, and FIXEDSTEP.
 *  This parser reads the "Track" lines and applies some properties from
 *  them, but ignores properties that don't easily apply to IGB.
 * 	Generally should be a "track" line, followed by optional "format" line
 * (If there is no format line, BED4 format is assumed.)
 *  There can be multiple formats per track.
 */
public final class WiggleParser {
	private static enum WiggleFormat {

		BED4, VARSTEP, FIXEDSTEP
	};

	private static final Pattern field_regex = Pattern.compile("\\s+");  // one or more whitespace
	private static final boolean ensure_unique_id = true;
	private final TrackLineParser track_line_parser;

	public WiggleParser() {
		track_line_parser = new TrackLineParser();
	}

	/**
	 *  Reads a Wiggle-formatted file using any combination of the three formats
	 *  {@link #BED4}, {@link #VARSTEP}, {@link #FIXEDSTEP}.
	 *  The format must be specified on the first line following a track line,
	 *  otherwise BED4 is assumed.
	 */
	public List<GraphSym> parse(InputStream istr, AnnotatedSeqGroup seq_group, boolean annotate_seq, String stream_name)
					throws IOException {
		WiggleFormat current_format = WiggleFormat.BED4;

		List<GraphSym> grafs = new ArrayList<GraphSym>();
		WiggleData current_data = null;
		Map<String, WiggleData> current_datamap = null; // Map: seq_id -> WiggleData
		boolean previous_track_line = false;

		BufferedReader br = new BufferedReader(new InputStreamReader(istr));
		String line;

		// these may be used by fixedStep or variableStep
		String current_seq_id = null;
		int current_start=0;
		int current_step=0;
		int current_span=0;

		while ((line = br.readLine()) != null && !Thread.currentThread().isInterrupted()) {
			if (line.length() == 0) {
				continue;
			}
			if (line.startsWith("#") || line.startsWith("%") || line.startsWith("browser")) {
				continue;
			}

			if (line.startsWith("track")) {
				if (previous_track_line) {
					// finish previous graph(s) using previous track properties
					grafs.addAll(createGraphSyms(track_line_parser.getCurrentTrackHash(), seq_group, current_datamap, stream_name));
				}

				track_line_parser.parseTrackLine(line);
				previous_track_line = true;

				current_format = WiggleFormat.BED4; // assume BED4 until changed.
				current_data = null;
				current_datamap = new HashMap<String, WiggleData>();
				continue;
			}

			if (line.startsWith("variableStep")) {
				if (!previous_track_line) {
					throw new IllegalArgumentException("Wiggle format error: 'variableStep' line does not have a previous 'track' line");
				}

				current_format = WiggleFormat.VARSTEP;
				current_seq_id = WiggleParser.parseFormatLine( line, "chrom","unknown");
				current_span = Integer.parseInt(WiggleParser.parseFormatLine( line, "span","1"));
				continue;
			}

			if (line.startsWith("fixedStep")) {
				if (!previous_track_line) {
					throw new IllegalArgumentException("Wiggle format error: 'fixedStep' line does not have a previous 'track' line");
				}

				current_format = WiggleFormat.FIXEDSTEP;
				current_seq_id = WiggleParser.parseFormatLine( line, "chrom","unknown");
				current_start = Integer.parseInt(WiggleParser.parseFormatLine( line, "start","1"));
				if (current_start < 1) {
					throw new IllegalArgumentException("'fixedStep' format with start of " + current_start +".");
				}
				current_step = Integer.parseInt(WiggleParser.parseFormatLine( line, "step","1"));
				current_span = Integer.parseInt(WiggleParser.parseFormatLine( line, "span","1"));
				continue;
			}

			// Else, it is a data line

			// There should have been one track line at least...
			if (!previous_track_line) {
				throw new IllegalArgumentException("Wiggle format error: File does not have a previous 'track' line");
			}


			String[] fields = field_regex.split(line.trim()); // trim() because lines are allowed to start with whitespace
			
			validateArguments(current_format, line, fields);

			if (current_format == WiggleFormat.BED4) {
				parseDataLine(fields, seq_group, current_data, current_datamap);
				continue;
			}
			if (current_format == WiggleFormat.VARSTEP) {
				parseDataLine(fields, current_seq_id, current_span, seq_group, current_data, current_datamap);
				continue;
			}
			if (current_format == WiggleFormat.FIXEDSTEP) {
				parseDataLine(fields, current_seq_id, current_start, current_span, seq_group, current_data, current_datamap);
				current_start += current_step;	// We advance the start based upon the step.
				continue;
			}
		}

		grafs.addAll(createGraphSyms(track_line_parser.getCurrentTrackHash(), seq_group, current_datamap, stream_name));

		if (annotate_seq) {
			for (GraphSym graf : grafs) {
				MutableAnnotatedBioSeq seq = graf.getGraphSeq();
				seq.addAnnotation(graf);
			}
		}

		return grafs;
	}

	/**
	 * Sanity checking on arguments.
	 * @param current_format
	 * @param line
	 * @param fields
	 */
	private static void validateArguments(WiggleFormat current_format, String line, String [] fields) {
		if (current_format == WiggleFormat.BED4) {
			if (fields.length < 4) {
				throw new IllegalArgumentException("Wiggle format error: Improper " + current_format + " line: " + line);
			}
		}
		if (current_format == WiggleFormat.VARSTEP) {
			if (fields.length < 2) {
				throw new IllegalArgumentException("Wiggle format error: Improper " + current_format + " line: " + line);
			}
		}
		if (current_format == WiggleFormat.FIXEDSTEP) {
			if (fields.length < 1) {
				throw new IllegalArgumentException("Wiggle format error: Improper " + current_format + " line: " + line);
			}
		}
	}


		/**
	 * Parse a single line of data (BED4 format).
	 * @param line
	 * @param current_format
	 * @param seq_group
	 * @param current_data
	 * @param current_datamap
	 */
	private static void parseDataLine(
					String[] fields,
					AnnotatedSeqGroup seq_group,
					WiggleData current_data,
					Map<String, WiggleData> current_datamap) {

		// chrom  start end value
		String seq_id = fields[0];	// chrom

		current_data = current_datamap.get(seq_id);
		if (current_data == null) {
			current_data = new WiggleData(seq_group, seq_id);
			current_datamap.put(seq_id, current_data);
		}

		int x1 = Integer.parseInt(fields[1]);	// start, or perhaps end
		int x2 = Integer.parseInt(fields[2]);	// start, or perhaps end
		int start = Math.min(x1, x2);
		int width = Math.max(x1, x2) - start;

		current_data.add(x1, Float.parseFloat(fields[3]), width);
	}
	
	/**
	 * Parse a single line of data (variableStep format).
	 * @param line
	 * @param current_format
	 * @param seq_group
	 * @param current_data
	 * @param current_datamap
	 */
	private static void parseDataLine(
					String[] fields,
					String current_seq_id,
					int current_span,
					AnnotatedSeqGroup seq_group,
					WiggleData current_data,
					Map<String, WiggleData> current_datamap) {

		current_data = current_datamap.get(current_seq_id);
		if (current_data == null) {
			current_data = new WiggleData(seq_group, current_seq_id);
			current_datamap.put(current_seq_id, current_data);
		}

		int current_start = Integer.parseInt(fields[0]);
		if (current_start < 1) {
			throw new IllegalArgumentException("'variableStep' format with start of " + current_start +".");
		}
		current_start -=1;	// This is because fixedStep and variableStep sequences are 1-indexed.  See http://genome.ucsc.edu/goldenPath/help/wiggle.html

		current_data.add(current_start, Float.parseFloat(fields[1]), current_span);
	
	}

	/**
	 * Parse a single line of data (fixedStep format).
	 * @param line
	 * @param current_format
	 * @param seq_group
	 * @param current_data
	 * @param current_datamap
	 */
	private static void parseDataLine(
					String[] fields,
					String current_seq_id,
					int current_start,
					int current_span,
					AnnotatedSeqGroup seq_group,
					WiggleData current_data,
					Map<String, WiggleData> current_datamap) {

		current_data = current_datamap.get(current_seq_id);
		if (current_data == null) {
			current_data = new WiggleData(seq_group, current_seq_id);
			current_datamap.put(current_seq_id, current_data);
		}

		current_start -=1;	// This is because fixedStep and variableStep formats are 1-indexed.  See http://genome.ucsc.edu/goldenPath/help/wiggle.html

		current_data.add(current_start, Float.parseFloat(fields[0]), current_span);
	}



	/**
	 * Parse the line, looking for the field name.  If it can't be found, return the default value.
	 * @param name
	 * @param line
	 * @param default_val
	 * @return
	 */
	private static String parseFormatLine(String line, String name, String default_val) {
		String[] fields = field_regex.split(line);
		String fieldName = name +"=";
		for (String field : fields) {
			if (field.startsWith(fieldName)) {
				return field.substring(name.length() + 1);
			}
		}
		return default_val;
	}

	/**
	 * Finishes the current data section and creates a list of GraphSym objects.
	 */
	private static List<GraphSym> createGraphSyms(Map<String,String> track_hash, AnnotatedSeqGroup seq_group, Map<String, WiggleData> current_datamap, String stream_name) {
		if (current_datamap == null) {
			return Collections.<GraphSym>emptyList();
		}

		List<GraphSym> grafs = new ArrayList<GraphSym>(current_datamap.size());

		String graph_id = track_hash.get(TrackLineParser.NAME);
		if (graph_id == null) {
			graph_id = stream_name;
		}
		if (ensure_unique_id) {
			graph_id = AnnotatedSeqGroup.getUniqueGraphID(graph_id, seq_group);
		}
		track_hash.put(TrackLineParser.NAME, graph_id);

		GraphStateI gstate = AnnotatedSeqGroup.getStateProvider().getGraphState(graph_id);
		TrackLineParser.applyTrackProperties(track_hash, gstate);

		// Need iterator because we're removing data on the fly
		Iterator<WiggleData> wiggleDataIterator = current_datamap.values().iterator();
		while (wiggleDataIterator.hasNext()) {
			GraphSymFloat gsym = wiggleDataIterator.next().createGraph(graph_id);

			if (gsym != null) {
				grafs.add(gsym);
			}
			wiggleDataIterator.remove();	// free up memory now that we've created the graph.
		}

		return grafs;
	}

	/** Writes the given GraphIntervalSym in wiggle-BED format.
	 *  Also writes a track line as a header.
	 */
	public static void writeBedFormat(GraphIntervalSym graf, String genome_version, OutputStream outstream) throws IOException {
		int xpos[] = graf.getGraphXCoords();
		int widths[] = graf.getGraphWidthCoords();

		MutableAnnotatedBioSeq seq = graf.getGraphSeq();
		String seq_id = (seq == null ? "." : seq.getID());
		String human_name = graf.getGraphState().getTierStyle().getHumanName();
		String gname = graf.getGraphName();
		GraphStateI state = graf.getGraphState();
		Color color = state.getTierStyle().getColor();

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(outstream));
			if (genome_version != null && genome_version.length() > 0) {
				bw.write("# genome_version = " + genome_version + '\n');
			}
			bw.write("track type=wiggle_0 name=\"" + gname + "\"");
			bw.write(" description=\"" + human_name + "\"");
			bw.write(" visibility=full");
			bw.write(" color=" + color.getRed() + "," + color.getGreen() + "," + color.getBlue());
			bw.write(" viewLimits=" + Float.toString(state.getVisibleMinY()) + ":" + Float.toString(state.getVisibleMaxY()));
			bw.write("");
			bw.write('\n');
			for (int i = 0; i < xpos.length; i++) {
				int x2 = xpos[i] + widths[i];
				bw.write(seq_id + ' ' + xpos[i] + ' ' + x2 + ' ' + graf.getGraphYCoord(i) + '\n');
			}
			bw.flush();
		} finally {
			GeneralUtils.safeClose(bw);
		}
	}
}
