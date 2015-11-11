package com.google.code.externalsorting;

import aQute.bnd.annotation.component.Component;
import com.lorainelab.externalsort.api.ComparatorInstance;
import com.lorainelab.externalsort.api.ComparatorMetadata;
import com.lorainelab.externalsort.api.ExternalSortConfiguration;
import com.lorainelab.externalsort.api.ExternalSortService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Referenced git@github.com:lemire/externalsortinginjava.git for external merge sort algorithm.
 */
@Component
public class ExternalMergeSort implements ExternalSortService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalMergeSort.class);

    public ExternalMergeSort() {
    }

    @Override
    public Optional<File> merge(File input, String compressionName, ComparatorMetadata comparatorMetadata, ExternalSortConfiguration conf) {

        try {
            File out = prepareHeader(input, conf);
            List<File> tmpFiles = sortInBatch(input, compressionName, comparatorMetadata, conf);
            logger.info("Temp file count: " + tmpFiles.size());
            File mergedData = mergeSortedFiles(tmpFiles, comparatorMetadata, conf);
            try (BufferedReader br = new BufferedReader(new FileReader(mergedData));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(out, true))) {
                int r;
                while ((r = br.read()) != -1) {
                    bw.append((char) r);
                }
            }
            return Optional.ofNullable(out);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private File prepareHeader(File input, ExternalSortConfiguration conf) throws IOException {
        int counter = 0;
        File out = Files.createTempFile(conf.getTmpDir().toPath(), "header", "", new FileAttribute[]{}).toFile();
        try (BufferedReader br = new BufferedReader(new FileReader(input))) {

            List<String> headerLines = new ArrayList<>();
            while (counter < conf.getNumHeaderRows()) {
                headerLines.add(br.readLine());
                counter++;
            }
            Files.write(out.toPath(), headerLines, StandardOpenOption.CREATE);
        }
        return out;
    }

    private File mergeSortedFiles(List<File> files,
            final ComparatorMetadata comparatorMetadata, ExternalSortConfiguration conf) throws IOException {
        boolean usegzip = conf.isUseGzipOnTmpFiles();
        Charset cs = conf.getCharset();
        boolean distinct = conf.isIsDistinctValues();
        Date now = new Date();
        File outputFile = Files.createTempFile(conf.getTmpDir().toPath(), "out", "", new FileAttribute[]{}).toFile();
        //File outputfile = new File(conf.getTmpDir().getPath() + File.pathSeparator + "out_" + UUID.randomUUID() + "_" + now.getTime());
        ArrayList<BinaryFileBuffer> bfbs = new ArrayList<>();
        for (File f : files) {
            final int BUFFERSIZE = 2048;
            InputStream in = new FileInputStream(f);
            BufferedReader br;
            if (usegzip) {
                br = new BufferedReader(
                        new InputStreamReader(
                                new GZIPInputStream(in,
                                        BUFFERSIZE), cs));
            } else {
                br = new BufferedReader(new InputStreamReader(
                        in, cs));
            }

            BinaryFileBuffer bfb = new BinaryFileBuffer(br);
            bfbs.add(bfb);
        }
        BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile, false), cs));
        mergeSortedFiles(fbw, comparatorMetadata, distinct, bfbs);
        files.stream().forEach((f) -> {
            f.delete();
        });
        return outputFile;
    }

    private int mergeSortedFiles(BufferedWriter fbw,
            final ComparatorMetadata comparatorMetadata, boolean distinct,
            List<BinaryFileBuffer> buffers) throws IOException {
        PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<>(
                11, (BinaryFileBuffer i, BinaryFileBuffer j) -> {
                    ComparatorInstance oI = new ComparatorInstance();
                    oI.setComparatorMetadata(comparatorMetadata);
                    oI.setLine(i.peek());

                    ComparatorInstance oJ = new ComparatorInstance();
                    oJ.setComparatorMetadata(comparatorMetadata);
                    oJ.setLine(j.peek());
                    return comparatorMetadata.getComparator().compare(oI, oJ);
                });
        buffers.stream().filter((bfb) -> (!bfb.empty())).forEach((bfb) -> {
            pq.add(bfb);
        });
        int rowcounter = 0;
        try {
            if (!distinct) {
                while (pq.size() > 0) {
                    BinaryFileBuffer bfb = pq.poll();
                    String r = bfb.pop();
                    fbw.write(r);
                    fbw.newLine();
                    ++rowcounter;
                    if (bfb.empty()) {
                        bfb.fbr.close();
                    } else {
                        pq.add(bfb); // add it back
                    }
                }
            } else {
                String lastLine = null;
                if (pq.size() > 0) {
                    BinaryFileBuffer bfb = pq.poll();
                    lastLine = bfb.pop();
                    fbw.write(lastLine);
                    fbw.newLine();
                    ++rowcounter;
                    if (bfb.empty()) {
                        bfb.fbr.close();
                    } else {
                        pq.add(bfb); // add it back
                    }
                }
                while (pq.size() > 0) {
                    BinaryFileBuffer bfb = pq.poll();
                    String r = bfb.pop();
                    // Skip duplicate lines
                    ComparatorInstance oR = new ComparatorInstance();
                    oR.setComparatorMetadata(comparatorMetadata);
                    oR.setLine(r);
                    ComparatorInstance oLastLine = new ComparatorInstance();
                    oLastLine.setComparatorMetadata(comparatorMetadata);
                    oLastLine.setLine(lastLine);

                    if (comparatorMetadata.getComparator().compare(oR, oLastLine) != 0) {
                        fbw.write(r);
                        fbw.newLine();
                        lastLine = r;
                    }
                    ++rowcounter;
                    if (bfb.empty()) {
                        bfb.fbr.close();
                    } else {
                        pq.add(bfb); // add it back
                    }
                }
            }
        } finally {

            fbw.close();
            for (BinaryFileBuffer bfb : pq) {
                bfb.close();
            }
        }
        return rowcounter;
    }

    /**
     * we divide the file into small blocks. If the blocks are too small, we shall create too many temporary files. If
     * they are too big, we shall be using too much memory.
     *
     * @param sizeoffile how much data (in bytes) can we expect
     * @param maxtmpfiles how many temporary files can we create (e.g., 1024)
     * @param maxMemory Maximum memory to use (in bytes)
     * @return the estimate
     */
    private long estimateBestSizeOfBlocks(final long sizeoffile,
            final int maxtmpfiles, final long maxMemory) {
        // we don't want to open up much more than maxtmpfiles temporary
        // files, better run
        // out of memory first.
        long blocksize = sizeoffile / maxtmpfiles
                + (sizeoffile % maxtmpfiles == 0 ? 0 : 1);

        // on the other hand, we don't want to create many temporary
        // files
        // for naught. If blocksize is smaller than half the free
        // memory, grow it.
        if (blocksize < maxMemory) {
            blocksize = maxMemory;
        }
        return blocksize;
    }

    private File sortAndSave(List<ComparatorInstance> tmplist,
            ComparatorMetadata comparatorMetadata, Charset cs, File tmpdirectory,
            boolean distinct, boolean usegzip) throws IOException {
        Collections.sort(tmplist, comparatorMetadata.getComparator());// In Java8, we can do tmplist = tmplist.parallelStream().sorted(comparatorMetadata).collect(Collectors.toCollection(ArrayList<String>::new));
        File newtmpfile = File.createTempFile("sortInBatch",
                "flatfile", tmpdirectory);
        newtmpfile.deleteOnExit();
        OutputStream out = new FileOutputStream(newtmpfile);
        int ZIPBUFFERSIZE = 2048;
        if (usegzip) {
            out = new GZIPOutputStream(out, ZIPBUFFERSIZE) {
                {
                    this.def.setLevel(Deflater.BEST_SPEED);
                }
            };
        }
        try (BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
                out, cs))) {
            if (!distinct) {
                for (ComparatorInstance r : tmplist) {
                    fbw.write(r.getLine());
                    fbw.newLine();
                }
            } else {
                ComparatorInstance lastLine = null;
                Iterator<ComparatorInstance> i = tmplist.iterator();
                if (i.hasNext()) {
                    lastLine = i.next();
                    fbw.write(lastLine.getLine());
                    fbw.newLine();
                }
                while (i.hasNext()) {
                    ComparatorInstance r = i.next();
                    // Skip duplicate lines

                    if (comparatorMetadata.getComparator().compare(r, lastLine) != 0) {
                        fbw.write(r.getLine());
                        fbw.newLine();
                        lastLine = r;
                    }
                }
            }
        }
        return newtmpfile;
    }

    /**
     * @param fbr data source
     * @param datalength estimated data volume (in bytes)
     * @param comparatorMetadata string comparator
     * @param maxtmpfiles maximal number of temporary files
     * @param maxMemory maximum amount of memory to use (in bytes)
     * @param cs character set to use (can use Charset.defaultCharset())
     * @param tmpdirectory location of the temporary files (set to null for default location)
     * @param distinct Pass <code>true</code> if duplicate lines should be discarded.
     * @param numHeader number of lines to preclude before sorting starts
     * @param usegzip use gzip compression for the temporary files
     * @return a list of temporary flat files
     * @throws IOException
     */
    private List<File> sortInBatch(final BufferedReader fbr,
            final long datalength, final ComparatorMetadata comparatorMetadata,
            final int maxtmpfiles, long maxMemory, final Charset cs,
            final File tmpdirectory, final boolean distinct,
            final int numHeader, final boolean usegzip) throws IOException {
        List<File> files = new ArrayList<>();
        long blocksize = estimateBestSizeOfBlocks(datalength,
                maxtmpfiles, maxMemory);// in
        // bytes

        try {
            List<ComparatorInstance> tmplist = new ArrayList<>();
            String line = "";
            try {
                int counter = 0;
                while (line != null) {
                    long currentblocksize = 0;// in bytes
                    while ((currentblocksize < blocksize)
                            && ((line = fbr.readLine()) != null)) {
                        // as long as you have enough
                        // memory
                        if (counter < numHeader) {
                            counter++;
                            continue;
                        }
                        ComparatorInstance ci = new ComparatorInstance();
                        ci.setComparatorMetadata(comparatorMetadata);
                        ci.setLine(line);
                        tmplist.add(ci);
                        currentblocksize += StringSizeEstimator
                                .estimatedSizeOf(line);
                    }
                    files.add(sortAndSave(tmplist, comparatorMetadata, cs,
                            tmpdirectory, distinct, usegzip));
                    tmplist.clear();
                }
            } catch (EOFException oef) {
                if (tmplist.size() > 0) {
                    files.add(sortAndSave(tmplist, comparatorMetadata, cs,
                            tmpdirectory, distinct, usegzip));
                    tmplist.clear();
                }
            }
        } finally {
            fbr.close();
        }
        return files;
    }

    /**
     * This will simply load the file by blocks of lines, then sort them in-memory, and write the result to temporary
     * files that have to be merged later. You can specify a bound on the number of temporary files that will be
     * created.
     *
     * @param file some flat file
     * @param comparatorMetadata string comparator
     * @param maxtmpfiles maximal number of temporary files
     * @param cs character set to use (can use Charset.defaultCharset())
     * @param tmpdirectory location of the temporary files (set to null for default location)
     * @param distinct Pass <code>true</code> if duplicate lines should be discarded.
     * @param numHeader number of lines to preclude before sorting starts
     * @param usegzip use gzip compression for the temporary files
     * @return a list of temporary flat files
     * @throws IOException
     */
    private List<File> sortInBatch(File file, String compressionName, ComparatorMetadata comparatorMetadata,
            int maxtmpfiles, long maxMemoryInBytes, Charset cs, File tmpdirectory,
            boolean distinct, int numHeader, boolean usegzip)
            throws IOException {
        BufferedReader fbr = new BufferedReader(new InputStreamReader(
                unzipStream(new FileInputStream(file), compressionName), cs));
        return sortInBatch(fbr, file.length(), comparatorMetadata, maxtmpfiles,
                maxMemoryInBytes, cs, tmpdirectory, distinct,
                numHeader, usegzip);
    }

    public static InputStream unzipStream(InputStream istr, String compressionName)
            throws IOException {
        String lc_stream_name = compressionName.toLowerCase();
        if (lc_stream_name.endsWith(".gz") || lc_stream_name.endsWith(".gzip")
                || lc_stream_name.endsWith(".z")) {
            InputStream gzstr = new GZIPInputStream(istr);
            String new_name = compressionName.substring(0, compressionName.lastIndexOf('.'));
            return unzipStream(gzstr, new_name);
        } else if (lc_stream_name.endsWith(".zip")) {
            ZipInputStream zstr = new ZipInputStream(istr);
            zstr.getNextEntry();
            String new_name = compressionName.substring(0, compressionName.lastIndexOf('.'));
            return unzipStream(zstr, new_name);
        } else if (lc_stream_name.endsWith(".bz2")) {
            BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(istr);
            String new_name = compressionName.substring(0, compressionName.lastIndexOf('.'));
            return unzipStream(bz2, new_name);
        } else if (lc_stream_name.endsWith(".tar")) {
            TarArchiveInputStream tarInput = new TarArchiveInputStream(istr);
            tarInput.getNextTarEntry();
            String new_name = compressionName.substring(0, compressionName.lastIndexOf('.'));
            return unzipStream(tarInput, new_name);
        }
        return istr;
    }

    private List<File> sortInBatch(File file, String compressionName, ComparatorMetadata comparatorMetadata, ExternalSortConfiguration conf) {
        try {
            return sortInBatch(file, compressionName, comparatorMetadata, conf.getMaxTmpFiles(), conf.getMaxMemoryInBytes(), conf.getCharset(), conf.getTmpDir(), conf.isIsDistinctValues(), conf.getNumHeaderRows(), conf.isUseGzipOnTmpFiles());
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

}

/**
 * This is essentially a thin wrapper on top of a BufferedReader... which keeps the last line in memory.
 *
 * @author Daniel Lemire
 */
final class BinaryFileBuffer {

    public BinaryFileBuffer(BufferedReader r) throws IOException {
        this.fbr = r;
        reload();
    }

    public void close() throws IOException {
        this.fbr.close();
    }

    public boolean empty() {
        return this.cache == null;
    }

    public String peek() {
        return this.cache;
    }

    public String pop() throws IOException {
        String answer = peek();// make a copy
        reload();
        return answer;
    }

    private void reload() throws IOException {
        this.cache = this.fbr.readLine();
    }

    public BufferedReader fbr;

    private String cache;

}
