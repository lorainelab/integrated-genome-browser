package com.affymetrix.genometry.symloader;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.style.DefaultTrackStyle;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.style.ITrackStyle;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.GraphIntervalSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.tooltip.ToolTipConstants;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SHOW_MASK;
import com.affymetrix.genometry.util.ErrorHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import org.broad.tribble.readers.LineReader;

public class VCF extends UnindexedSymLoader implements LineProcessor {

    private static final String[] EXTENSIONS = new String[]{"vcf"};
    private static final String NO_DATA = ".";
    private static final Pattern line_regex = Pattern.compile("\\s+");
    private static final Pattern info_regex = Pattern.compile(";");

    private enum Type {
//		Numeric,

        Integer,
        String,
        Float,
        Flag
    }

    private class INFO {

        private final String ID;
//		private final int number;
        private final Type type;
        private final String description;
//		private final boolean onePerAllele;
//		private final boolean onePerGenotype;

        public INFO(String ID, int number, Type type, String description, boolean onePerAllele, boolean onePerGenotype) {
            this.ID = ID;
//			this.number = number;
            this.type = type;
            this.description = description;
//			this.onePerAllele = onePerAllele;
//			this.onePerGenotype = onePerGenotype;
        }

        public String getID() {
            return ID;
        }
//		public int getNumber() {
//			return number;
//		}

        public Type getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
//		public boolean isOnePerAllele() {
//			return onePerAllele;
//		}
//		public boolean isOnePerGenotype() {
//			return onePerGenotype;
//		}
    }

    private class FILTER {

        private final String ID;
        private final String description;

        public FILTER(String ID, String description) {
            this.ID = ID;
            this.description = description;
        }

        public String getID() {
            return ID;
        }

        public String getDescription() {
            return description;
        }
    }

    private class FORMAT {

        private final String ID;
//		private final int number;
        private final Type type;
//		private final String description;

        public FORMAT(String ID, int number, Type type, String description) {
            this.ID = ID;
//			this.number = number;
            this.type = type;
//			this.description = description;
        }

        public String getID() {
            return ID;
        }
//		public int getNumber() {
//			return number;
//		}

        public Type getType() {
            return type;
        }
//		public String getDescription() {
//			return description;
//		}
    }
//	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private double version = -1.0;
//	private Date date;
    private String[] samples = new String[]{};
    private Map<String, String> metaMap = new HashMap<>();
    private Map<String, INFO> infoMap = new HashMap<>();
    private Map<String, FILTER> filterMap = new HashMap<>();
    private Map<String, FORMAT> formatMap = new HashMap<>();
    private boolean combineGenotype;
    private List<String> selectedFields = new ArrayList<>();

    static {
        Set<String> types = new HashSet<>();

        types.add("protein");
    }

    private static final Pattern idPattern = Pattern.compile(",ID=\\w+,");
    private static final Pattern numberPattern = Pattern.compile(",Number=\\w+,");
    private static final Pattern typePattern = Pattern.compile(",Type=\\w+,");
    private static final Pattern descriptionPattern = Pattern.compile(",Description=\\\"[^\\\"]+\\\",");

    public VCF(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
    }

    /**
     * Parses VCF format
     */
    public List<? extends SeqSymmetry> processLines(BioSeq seq, final LineReader lineReader) {
        SimpleSymWithProps mainSym = new SimpleSymWithProps();
        mainSym.setProperty("seq", seq);
        mainSym.setProperty("type", featureName);
        mainSym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
        int line_count = 0;
        Map<String, SimpleSymWithProps> dataMap = new HashMap<>();
        Map<String, GraphData> graphDataMap = new HashMap<>();
        Map<String, SimpleSymWithProps> genotypeDataMap = new HashMap<>();

        String line = null;

        try {
            while ((line = lineReader.readLine()) != null && (!Thread.currentThread().isInterrupted())) {
                if (line.startsWith("#")) {
                    line_count++;
                    continue;
                } else if (line.length() > 0) {
                    processDataLine(mainSym, seq, 0, Integer.MAX_VALUE, featureName, dataMap, graphDataMap, genotypeDataMap, line, line_count, combineGenotype);
                    line_count++;
                }
            }
        } catch (Exception x) {
            Logger.getLogger(this.getClass().getName()).log(
                    Level.SEVERE, "failed to parse vcf file ", x);
        }
        SeqSpan span = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
        List<SeqSymmetry> symlist = new ArrayList<>();
        if (mainSym.getChildCount() > 0) {
            mainSym.addSpan(span);
        }
        symlist.add(mainSym);
        for (String key : dataMap.keySet()) {
            SimpleSymWithProps container = dataMap.get(key);
            container.addSpan(span);
            symlist.add(container);
        }
        for (String key : genotypeDataMap.keySet()) {
            SimpleSymWithProps container = genotypeDataMap.get(key);
            container.addSpan(span);
            symlist.add(container);
        }
        Map<String, ITrackStyle> styleMap = new HashMap<>();
        for (String key : graphDataMap.keySet()) {
            GraphData graphData = graphDataMap.get(key);
            int dataSize = graphData.xData.size();
            int[] xList = Arrays.copyOf(graphData.xData.elements(), dataSize);
            float[] yList = Arrays.copyOf(graphData.yData.elements(), dataSize);
            int[] wList = Arrays.copyOf(graphData.wData.elements(), dataSize);
            GraphIntervalSym graphIntervalSym = new GraphIntervalSym(xList, wList, yList, key, seq);
            String comboKey = key.substring(0, key.lastIndexOf('/'));
            GraphState gstate = graphIntervalSym.getGraphState();
            if (combineGenotype && key.indexOf('/') != key.lastIndexOf('/')) {
                ITrackStyle combo_style = styleMap.get(comboKey);
                if (combo_style == null) {
                    combo_style = new DefaultTrackStyle(comboKey, true);
                    combo_style.setTrackName(comboKey);
                    combo_style.setExpandable(true);
                    combo_style.setCollapsed(true);
                    styleMap.put(comboKey, combo_style);
                }
                if (combo_style instanceof ITrackStyleExtended) {
                    gstate.setComboStyle((ITrackStyleExtended) combo_style, 0);
                } else {
                    gstate.setComboStyle(null, 0);
                }
//				gstate.getTierStyle().setHeight(combo_style.getHeight());
                gstate.getTierStyle().setFloatTier(false); // ignored since combo_style is set
            } else {
                gstate.setComboStyle(null, 0);
//				gstate.getTierStyle().setHeight(combo_style.getHeight());
                gstate.getTierStyle().setFloatTier(false); // ignored since combo_style is set
            }
            symlist.add(graphIntervalSym);
        }
        return symlist;
    }

    @Override
    public void init(URI uri) {
    }

    public void setCombineGenotype(boolean combineGenotype) {
        this.combineGenotype = combineGenotype;
    }

    public void select(String name, boolean separateTracks, Map<String, List<String>> selections) {
        setCombineGenotype(!separateTracks);
        new ArrayList<>(selectedFields).stream().filter(dataField -> dataField.indexOf('/') > -1).forEach(selectedFields::remove);
        for (String type : selections.keySet()) {
            for (String sample : selections.get(type)) {
                selectedFields.add(name + "/" + type + "/" + sample);
            }
        }
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    @Override
    public List<String> getFormatPrefList() {
        return Arrays.asList(EXTENSIONS);
    }

    public List<String> getAllFields() {
        return new ArrayList<>(infoMap.keySet());
    }

    public List<String> getSamples() {
        return Arrays.asList(samples);
    }

    public List<String> getGenotypes() {
        return new ArrayList<>(formatMap.keySet());
    }

    private String getID(String line) {
        Matcher matcher = idPattern.matcher(line);
        if (matcher.find()) {
            String group = matcher.group();
            return group.substring(",ID=".length(), group.length() - 1);
        } else {
            return null;
        }
    }

    private int getNumber(String line) {
        int number = -1;
        String numberString = getNumberString(line);
        if (numberString != null) {
            try {
                number = Integer.parseInt(numberString);
            } catch (NumberFormatException x) {
            }
        }
        return number;
    }

    private String getNumberString(String line) {
        Matcher matcher = numberPattern.matcher(line);
        if (matcher.find()) {
            String group = matcher.group();
            return group.substring(",Number=".length(), group.length() - 1);
        } else {
            return null;
        }
    }

    private Type getType(String line) {
        Matcher matcher = typePattern.matcher(line);
        if (matcher.find()) {
            String group = matcher.group();
            return Type.valueOf(group.substring(",Type=".length(), group.length() - 1));
        } else {
            return null;
        }
    }

    private String getDescription(String line) {
        Matcher matcher = descriptionPattern.matcher(line);
        if (matcher.find()) {
            String group = matcher.group();
            return group.substring(",Description=\"".length(), group.length() - 2);
        } else {
            return null;
        }
    }

    private INFO getInfo(String line) {
        String dataline = "," + line.substring(1, line.length() - 1) + ",";
        int number = -1;
        boolean onePerAllele = false;
        boolean onePerGenotype = false;
        String numberString = getNumberString(line);
        if (numberString == null) {
        } else if ("A".equals(numberString)) {
            onePerAllele = true;
        } else if ("G".equals(numberString)) {
            onePerGenotype = true;
        } else if (".".equals(numberString)) {
        } else {
            number = Integer.parseInt(numberString);
        }
        return new INFO(getID(dataline), number, getType(dataline), getDescription(dataline), onePerAllele, onePerGenotype);
    }

    private FILTER getFilter(String line) {
        String dataline = "," + line.substring(1, line.length() - 1) + ",";
        return new FILTER(getID(dataline), getDescription(dataline));
    }

    private FORMAT getFormat(String line) {
        String dataline = "," + line.substring(1, line.length() - 1) + ",";
        return new FORMAT(getID(dataline), getNumber(dataline), getType(dataline), getDescription(dataline));
    }

    private void processMetaInformationLine(String line) {
        if (line.startsWith("fileformat=")) {
            String format = line.substring("fileformat=".length());
            switch (format) {
                case "VCFv4.0":
                    version = 4.0;
                    break;
                case "VCFv4.1":
                    version = 4.1;
                    break;
                default:
                    ErrorHandler.errorPanel("file version not supported " + format);
                    throw new UnsupportedOperationException("file version not supported " + format);
            }
            Logger.getLogger("com.affymetrix.genometry.symloader").log(Level.INFO, "vcf file version {0}", version);
        } else if (line.startsWith("format=")) {
            String format = line.substring("format=".length());
            ErrorHandler.errorPanel("file version not supported " + format);
            throw new UnsupportedOperationException("file version not supported " + format);
        } //		else if (line.startsWith("fileDate=")) {
        //			try {
        //				date = DATE_FORMAT.parse(line.substring("fileDate=".length()));
        //			}
        //			catch (ParseException x) {
        //				Logger.getLogger(this.getClass().getName()).log(
        //					Level.WARNING, "Unable to process date " + line.substring("fileDate=".length()));
        //			}
        //		}
        else if (line.startsWith("INFO=")) {
            INFO info = getInfo(line.substring("INFO=".length()));
            infoMap.put(info.getID(), info);
        } else if (line.startsWith("FILTER=")) {
            FILTER filter = getFilter(line.substring("FILTER=".length()));
            filterMap.put(filter.getID(), filter);
        } else if (line.startsWith("FORMAT=")) {
            FORMAT format = getFormat(line.substring("FORMAT=".length()));
            formatMap.put(format.getID(), format);
        } else {
            int pos = line.indexOf('=');
            metaMap.put(line.substring(0, pos), line.substring(pos + 1));
        }
    }

    private void processHeaderLine(String line) {
        String[] fields = line_regex.split(line);
        if (fields.length > 8) {
            samples = Arrays.copyOfRange(fields, 9, fields.length);
        } else {
            samples = new String[]{};
        }
    }

    private SimpleSymWithProps getContainerSymFromMap(Map<String, SimpleSymWithProps> symMap, String key, BioSeq seq) {
        SimpleSymWithProps container = symMap.get(key);
        if (container == null) {
            container = new SimpleSymWithProps();
            container.setProperty("seq", seq);
            container.setProperty("type", key);
            container.setProperty("id", key);
            container.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
            symMap.put(key, container);
        }
        return container;
    }

    private String getMultiple(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private BAMSym getBAMSym(String nameType, BioSeq seq, String id, int start, int end, int width, String qualString, String filter, String ref, String alt) {
        Cigar cigar = null;
// Cigar cigar = TextCigarCodec.getSingleton().decode(cigarString);
        boolean equal = false;
        boolean equalLength = false;
        boolean insertion = false;
        boolean deletion = false;
        if (ref.equals(alt)) {
            equal = true;
        } else if (ref.length() == alt.length()) {
            cigar = new Cigar();
            CigarElement cigarElement = new CigarElement(width, CigarOperator.M);
            cigar.add(cigarElement);
            equalLength = true;
        } else if (ref.length() == 1) {
            cigar = new Cigar();
            CigarElement cigarElement = new CigarElement(1, CigarOperator.M);
            cigar.add(cigarElement);
            cigarElement = new CigarElement(alt.length() - 1, CigarOperator.I);
            cigar.add(cigarElement);
            insertion = true;
        } else if (alt.length() == 1) {
            cigar = new Cigar();
            CigarElement cigarElement = new CigarElement(1, CigarOperator.M);
            cigar.add(cigarElement);
            cigarElement = new CigarElement(ref.length() - 1, CigarOperator.D);
            cigar.add(cigarElement);
            deletion = true;
        }
        int[] iblockMins = insertion ? new int[]{start + 1} : new int[]{};
        int[] iblockMaxs = insertion ? new int[]{start + alt.length()} : new int[]{};
        String residuesStr = "";
        if (equal || equalLength) {
            residuesStr = alt;
        } else if (insertion) {
            residuesStr = ref;
        } else if (deletion) {
            String repeated = getMultiple('_', ref.length() - 1);
            residuesStr = alt + repeated;
        }
        BAMSym residueSym = new BAMSym(nameType, seq, start, end, id, true, new int[]{start}, new int[]{end}, iblockMins, iblockMaxs, cigar, residuesStr);
        if (cigar != null) {
            residueSym.setProperty(ToolTipConstants.CIGAR, cigar);
        }
        residueSym.setInsResidues(insertion ? alt.substring(1) : "");
        residueSym.setProperty(SHOW_MASK, true);
        residueSym.setProperty("type", nameType);
        residueSym.setProperty("seq", seq.getId());
        residueSym.setProperty("pos", start);
        residueSym.setProperty("id", id);
        residueSym.setProperty("ref", ref);
        residueSym.setProperty("alt", alt);
        if (!NO_DATA.equals(qualString)) {
            residueSym.setProperty("qual", Float.parseFloat(qualString));
        }
        if (!"PASS".equals(filter) && filterMap.get(filter) != null) {
            filter += " - " + filterMap.get(filter).getDescription();
        }
        residueSym.setProperty("filter", filter);
        return residueSym;
    }

    private void processInfo(String key, String valuesString, BioSeq seq, String nameType, int start, int end, int width,
            Map<String, SimpleSymWithProps> dataMap, Map<String, GraphData> graphDataMap) {
        String[] values = valuesString.split(",");
        if (infoMap.get(key).getType() == Type.Integer || infoMap.get(key).getType() == Type.Float) {
            for (String value : values) {
                addGraphData(graphDataMap, nameType + "/" + key, seq, start, width, Float.parseFloat(value));
            }
        } else {
            SimpleSymWithProps container = getContainerSymFromMap(dataMap, nameType + "/" + key, seq);
            for (String value : values) {
                SimpleSymWithProps sym = new SimpleSymWithProps();
                sym.addSpan(new SimpleSeqSpan(start, end, (BioSeq) container.getProperty("seq")));
                sym.setProperty(key, value);
                container.addChild(sym);
            }
        }
    }

    private void processSamples(BioSeq seq, String nameType, int start, int end, int width,
            String[] fields, Map<String, SimpleSymWithProps> genotypeDataMap,
            Map<String, GraphData> graphDataMap, boolean combineGenotype,
            BAMSym refSym, BAMSym[] altSyms, int line_count) {
        if (samples.length > 0) {
            if (fields.length < samples.length + 9) {
                Logger.getLogger(this.getClass().getName()).log(
                        Level.WARNING, "vcf line {0} has {1} genotype records, but header has {2}", new Object[]{line_count, fields.length - 9, samples.length});
            } else if (fields.length > samples.length + 9) {
                throw new IllegalStateException("vcf format error, line " + line_count + " has " + (fields.length - 9) + " genotype records, but header has " + samples.length);
            }
        }
        if (fields.length > 8) {
            String[] format = fields[8].split(":");
            // format[0] must be "GT"
            if (!"GT".equals(format[0])) {
                throw new IllegalStateException("vcf format error, line " + line_count + " first genotype field must be \"GT\"");
            }
            for (int j = 9; j < fields.length; j++) {
                String sample;
                if (j - 9 >= samples.length || samples[j - 9].trim().length() == 0) {
                    sample = "sample #" + (j - 8); // start with sample1, not sample0
                } else {
                    sample = samples[j - 9];
                }
                String[] data = fields[j].split(":");
                if (format.length < data.length) {
                    throw new IllegalStateException("vcf format error, line " + line_count + " has " + data.length + "genotype fields, but definition has " + format.length);
                }
                for (int k = 0; k < format.length; k++) {
                    String type = format[k];
                    String fullKey = nameType + "/" + type + "/" + sample;
                    String dataKey = nameType + "/" + type + (combineGenotype ? "" : ("/" + sample));
                    if (k < data.length && selectedFields.contains(fullKey)) {
                        if (!NO_DATA.equals(data[k])) {
                            if (formatMap.get(format[k]) != null && (formatMap.get(format[k]).getType() == Type.Integer || formatMap.get(format[k]).getType() == Type.Float)) {
                                for (String datum : data[k].split(",")) {
                                    if (!NO_DATA.equals(datum)) {
                                        addGraphData(graphDataMap, fullKey, seq, start, width, Float.parseFloat(datum));
                                    }
                                }
                            } else {
                                if ("GT".equals(format[k])) {
                                    String[] genotypes = data[k].split("[|/]");
                                    for (String genotype : genotypes) {
                                        if (!NO_DATA.equals(genotype)) {
                                            SimpleSymWithProps container = getContainerSymFromMap(genotypeDataMap, dataKey, seq);
                                            int index = Integer.parseInt(genotype);
                                            if (index == 0) {
                                                container.addChild(refSym);
                                            } else {
                                                container.addChild(altSyms[index - 1]);
                                            }
                                        }
                                    }
                                } else {
                                    SimpleSymWithProps container = getContainerSymFromMap(genotypeDataMap, dataKey, seq);
                                    for (String datum : data[k].split(",")) {
                                        if (!NO_DATA.equals(datum)) {
                                            SimpleSymWithProps sym = new SimpleSymWithProps();
                                            sym.addSpan(new SimpleSeqSpan(start, end, (BioSeq) container.getProperty("seq")));
                                            container.addChild(sym);
                                            sym.setProperty(format[k], datum);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void processDataLine(SimpleSymWithProps mainSym, BioSeq seq, int min, int max, String nameType,
            Map<String, SimpleSymWithProps> dataMap, Map<String, GraphData> graphDataMap,
            Map<String, SimpleSymWithProps> genotypeDataMap,
            String line, int line_count, boolean combineGenotype) {
        String[] fields = line_regex.split(line);
        int start = Integer.parseInt(fields[1]) - 1; // vcf is one based, but IGB is zero based
        if (max <= start) {
            return;
        }
        String ref = fields[3];
        int width = ref.length();
        int end = start + width;
        if (min >= end) {
            return;
        }
        String id = fields[2];
        String[] alts = fields[4].split(",");
        String qualString = fields[5];
        String filter = fields[6];
        if (!NO_DATA.equals(qualString) && selectedFields.contains("qual")) {
            addGraphData(graphDataMap, nameType + "/qual", seq, start, width, Float.parseFloat(qualString));
        }
        BAMSym[] altSyms = new BAMSym[alts.length];
        for (int i = 0; i < alts.length; i++) {
            String alt = alts[i];
            altSyms[i] = getBAMSym(nameType, seq, id, start, end, width, qualString, filter, ref, alt);
            mainSym.addChild(altSyms[i]);
            String[] info_fields = info_regex.split(fields[7]);
            for (String info_field : info_fields) {
                String[] prop_fields = info_field.split("=");
                String key = prop_fields[0];
                String valuesString = (prop_fields.length == 1) ? "true" : prop_fields[1];
                String fullKey = key;
                if (infoMap.get(key) != null && infoMap.get(key).getDescription() != null) {
                    fullKey += " - " + infoMap.get(key).getDescription();
                }
                altSyms[i].setProperty(fullKey, valuesString);
                if (selectedFields.contains(key)) {
                    processInfo(key, valuesString, seq, nameType, start, end, width, dataMap, graphDataMap);
                }
            }
        }
        BAMSym refSym = getBAMSym(nameType, seq, id, start, end, width, qualString, filter, ref, ref);
        processSamples(seq, nameType, start, end, width, fields, genotypeDataMap, graphDataMap,
                combineGenotype, refSym, altSyms, line_count);
    }

    private class GraphData {

        IntArrayList xData = new IntArrayList();
        FloatArrayList yData = new FloatArrayList();
        IntArrayList wData = new IntArrayList();
    }

    private void addGraphData(Map<String, GraphData> graphDataMap, String key, BioSeq seq, int pos, int width, float value) {
        GraphData graphData = graphDataMap.get(key);
        if (graphData == null) {
            graphData = new GraphData();
            graphDataMap.put(key, graphData);
        }
        graphData.xData.add(pos);
        graphData.yData.add(value);
        graphData.wData.add(width);
    }

    @Override
    public SeqSpan getSpan(String line) {
        String[] fields = line_regex.split(line);
        String seq_name = fields[0];
        int start = Integer.parseInt(fields[1]) - 1; // vcf is one based, but IGB is zero based
        String ref = fields[3];
        int width = ref.length();
        int end = start + width;
        BioSeq seq = GenometryModel.getInstance().getSelectedGenomeVersion().getSeq(seq_name);
        if (seq == null) {
            seq = new BioSeq(seq_name, 0);
        }
        return new SimpleSeqSpan(start, end, seq);
    }

    @Override
    public boolean processInfoLine(String line, List<String> infoLines) {
        if (line.startsWith("##")) {
            processMetaInformationLine(line.substring(2));
            return true;
        } else if (line.startsWith("#")) {
            if (version < 0) {
                ErrorHandler.errorPanel("version error", "file version not supported or not found for " + uri, Level.SEVERE);
                throw new UnsupportedOperationException("file version not supported or not found");
            }
            processHeaderLine(line.substring(1));
            return true;
        }
        return false;
    }

    @Override
    protected LineProcessor createLineProcessor(String featureName) {
        return this;
    }
}
