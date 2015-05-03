package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.SeekableBufferedStream;
import com.affymetrix.genometry.util.SynonymLookup;
import com.affymetrix.genometry.util.TwoBitIterator;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author sgblanch
 * @author hiralv
 * @version $Id$
 */
public final class TwoBitParser implements Parser {

    /**
     * Magic Number of 2bit files
     */
    private static final int MAGIC_NUMBER = 0x1A412743;

    /**
     * Size of integer, in bytes
     */
    private static final int INT_SIZE = 4;

    /**
     * Use a 4KB buffer, as that is the block size of most filesystems
     */
    private static int BUFFER_SIZE = 4096;

    /**
     * Byte mask for translating unsigned bytes into Java integers
     */
    private static final int BYTE_MASK = 0xff;

    /**
     * Byte mask for translating unsigned ints into Java longs
     */
    private static final long INT_MASK = 0xffffffff;

    /**
     * Character set used to decode strings. Currently ASCII
     */
    private static final Charset charset = Charset.forName("ASCII");

    /**
     * buffer for outputting
     */
    private static int BUFSIZE = 65536;

    private static final boolean DEBUG = false;

    public static List<BioSeq> parse(URI uri, GenomeVersion seq_group) throws IOException {
        SeekableBufferedStream bistr = new SeekableBufferedStream(LocalUrlCacher.getSeekableStream(uri));
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        loadBuffer(bistr, buffer);
        int seq_count = readFileHeader(buffer);
        List<BioSeq> seqs = readSequenceIndex(uri, bistr, buffer, seq_count, seq_group);
        GeneralUtils.safeClose(bistr);
        return seqs;
    }

    public static BioSeq parse(URI uri, GenomeVersion seq_group, String seqName) throws IOException {
        SeekableBufferedStream bistr = new SeekableBufferedStream(LocalUrlCacher.getSeekableStream(uri));
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        loadBuffer(bistr, buffer);
        int seq_count = readFileHeader(buffer);
        BioSeq retseq = readSequenceIndex(uri, bistr, buffer, seq_count, seq_group, seqName);
        GeneralUtils.safeClose(bistr);
        return retseq;
    }

    public static BioSeq parse(URI uri) throws IOException {
        return parse(uri, new GenomeVersion("No_Data")).get(0);
    }

    public static boolean parse(URI uri, OutputStream out) throws IOException {
        BioSeq seq = parse(uri, new GenomeVersion("No_Data")).get(0);
        return writeAnnotations(seq, 0, seq.getLength(), out);
    }

    public static boolean parse(URI uri, int start, int end, OutputStream out) throws IOException {
        BioSeq seq = parse(uri, new GenomeVersion("No_Data")).get(0);
        return writeAnnotations(seq, start, end, out);
    }

    public static boolean parse(URI uri, GenomeVersion seq_group, OutputStream out) throws IOException {
        BioSeq seq = parse(uri, seq_group).get(0);
        return writeAnnotations(seq, 0, seq.getLength(), out);
    }

    public static boolean parse(URI uri, GenomeVersion seq_group, int start, int end, OutputStream out) throws IOException {
        BioSeq seq = parse(uri, seq_group).get(0);
        return writeAnnotations(seq, start, end, out);
    }

    private static String getString(ByteBuffer buffer, int length) {
        byte[] string = new byte[length];
        buffer.get(string);
        return new String(string, charset);
    }

    /**
     * Load data from the bistr into the buffer. This convenience method is
     * used to ensure that the buffer has the correct endian and is rewound.
     */
    private static void loadBuffer(SeekableBufferedStream bistr, ByteBuffer buffer) throws IOException {
        buffer.rewind();
        bistr.read(buffer.array());
        //buffer.order(byteOrder);
        buffer.rewind();
    }

    private static int readFileHeader(ByteBuffer buffer) throws IOException {
        /* Java is big endian so try that first */
        int magic = buffer.getInt();

        /* Try little endian if big endian did not work */
        if (magic != MAGIC_NUMBER) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.rewind();
            magic = buffer.getInt();
        }

        /* Fail if we have no magic */
        if (magic != MAGIC_NUMBER) {
            throw new IOException("File is not in 2bit format:  Bad magic (0x" + Integer.toHexString(magic) + " actual, 0x" + Integer.toHexString(MAGIC_NUMBER) + " expected)");
        }

        /* Grab the rest of the header fields */
        int version = buffer.getInt();
        int seq_count = buffer.getInt();
        int reserved = buffer.getInt();

        /* Currently version and 'reserved' should be zero */
        if (version != 0 || reserved != 0) {
            throw new IOException("Unsupported 2bit format: version(" + version + ") and reserved(" + reserved + ") must equal 0");
        }

        return seq_count;
    }

    private static List<BioSeq> readSequenceIndex(URI uri, SeekableBufferedStream bistr, ByteBuffer buffer, int seq_count, GenomeVersion seq_group) throws IOException {
        String name;
        int name_length;
        long offset, position;
        List<BioSeq> seqs = new ArrayList<>();
        Map<String, Long> seqOffsets = new HashMap<>();
        position = bistr.position();
        for (int i = 0; i < seq_count; i++) {

            if (buffer.remaining() < INT_SIZE) {
                position = updateBuffer(bistr, buffer, position);
            }

            name_length = buffer.get() & BYTE_MASK;

            if (buffer.remaining() < name_length + INT_SIZE) {
                position = updateBuffer(bistr, buffer, position);
            }

            name = getString(buffer, name_length);
            offset = buffer.getInt() & INT_MASK;

            seqOffsets.put(name, offset);
        }

        for (Entry<String, Long> seqOffset : seqOffsets.entrySet()) {
            seqs.add(readSequenceHeader(uri, bistr, buffer.order(), seqOffset.getValue(), seq_group, seqOffset.getKey()));
        }

        return seqs;
    }

    private static BioSeq readSequenceIndex(URI uri, SeekableBufferedStream bistr, ByteBuffer buffer, int seq_count, GenomeVersion seq_group, String synonym) throws IOException {
        String name;
        int name_length;
        long offset, position;
        BioSeq seq = null;
        SynonymLookup chrLookup = SynonymLookup.getChromosomeLookup();
        position = bistr.position();
        for (int i = 0; i < seq_count; i++) {

            if (buffer.remaining() < INT_SIZE) {
                position = updateBuffer(bistr, buffer, position);
            }

            name_length = buffer.get() & BYTE_MASK;

            if (buffer.remaining() < name_length + INT_SIZE) {
                position = updateBuffer(bistr, buffer, position);
            }

            name = getString(buffer, name_length);
            offset = buffer.getInt() & INT_MASK;

            if (name.equals(synonym) || chrLookup.isSynonym(name, synonym)) {
                seq = readSequenceHeader(uri, bistr, buffer.order(), offset, seq_group, name);
                break;
            }
        }

        return seq;
    }

    private static BioSeq readSequenceHeader(URI uri, SeekableBufferedStream bistr, ByteOrder order, long offset, GenomeVersion seq_group, String name) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE);
        buffer.order(order);
        long residueOffset = offset;

        bistr.position(offset);
        loadBuffer(bistr, buffer);

        //dnaSize
        long size = buffer.getInt() & INT_MASK;

        if (DEBUG) {
            System.out.println("size is " + size + " bases");
        }

        residueOffset += INT_SIZE;

        if (size > Integer.MAX_VALUE) {
            throw new IOException("IGB can not handle sequences larger than " + Integer.MAX_VALUE + ".  Offending sequence length: " + size);
        }

        BioSeq seq = seq_group.addSeq(name, (int) size, uri.toString());

        seq.setResiduesProvider(new TwoBitIterator(uri, size, residueOffset, buffer.order()));

        return seq;
    }

    private static long updateBuffer(SeekableBufferedStream bistr, ByteBuffer buffer, long position) throws IOException {
        bistr.position(position - buffer.remaining());
        loadBuffer(bistr, buffer);
        return bistr.position();
    }

    public static String getMimeType() {
        return "binary/2bit";
    }

    private static boolean writeAnnotations(BioSeq seq, int start, int end, OutputStream outstream) {
        if (seq.getResiduesProvider() == null) {
            return false;
        }
        // sanity checks
        start = Math.max(0, start);
        end = Math.max(end, start);
        end = Math.min(end, seq.getLength());

        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(outstream));

            // Only keep BUFSIZE characters in memory at one time
            for (int i = start; i < end; i += BUFSIZE) {
                String outString = seq.getResidues(i, Math.min(i + BUFSIZE, end));
                dos.writeBytes(outString);
            }
            dos.flush();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        //String residues = "ACTGGGTCTCAGTACTAGGAATTCCGTCATAGCTAAA";
        String residues = "NACNTCNNNNNNNNNNNNGTCTCANNNNNGTACTANNNNGGAATTCNNNNNCGTCATAGNNNCTAAANNN";
        File f = new File("genometry/test/data/2bit/nblocks.2bit");
        ByteArrayOutputStream outStream = null;
        //File f = new File("genometry/test/data/2bit/at.2bit");
        try {
            int start = 11;
            int end = residues.length() + 4;
            outStream = new ByteArrayOutputStream();
            URI uri = URI.create("http://test.bioviz.org/testdata/nblocks.2bit");
            TwoBitParser.parse(uri, start, end, outStream);
            //BioSeq seq = TwoBitParser.parse(f);

            System.out.println("Result   :" + outStream.toString());

            if (start < end) {
                start = Math.max(0, start);
                start = Math.min(residues.length(), start);

                end = Math.max(0, end);
                end = Math.min(residues.length(), end);
            } else {
                start = 0;
                end = 0;
            }
            System.out.println("Expected :" + residues.substring(start, end));

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            GeneralUtils.safeClose(outStream);
        }
    }

    @Override
    public List<? extends SeqSymmetry> parse(InputStream is,
            GenomeVersion genomeVersion, String nameType, String uri,
            boolean annotate_seq) throws Exception {
        throw new IllegalStateException("2bit should not be processed here");
    }
}
