package org.lorainelab.igb.util;

import com.affymetrix.genometry.util.LocalUrlCacher;
import htsjdk.samtools.util.FileExtensions;
import org.lorainelab.igb.Exception.IndexFileNotFoundException;
import org.lorainelab.igb.bam.BAM;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexFileUtil {
    /**
     * Check if the alignment file has the index file. Firstly, it looks for file ending with .bam.bai (for bam) or .cram.crai (for cram).
     * If not found, it looks for the file ending with .bai or .crai.
     * @param file an alignment file, for example, BAM or CRAM.
     * @param extension extension of the alignment file (Example: .bam for BAM file).
     * @param trimLength length of the extension including "." (Example: length of .bam is 4).
     * @return Index file, if exists.
     * @throws IndexFileNotFoundException if the index file is not present in the location of the alignment file.
     */
    public static File findIndexFile(File file, String extension, int trimLength) throws IndexFileNotFoundException {
        try {
            String path = file.getPath();
            File f = new File(path + extension);
            if (f.exists()) {
                return f;
            }
            path = path.substring(0, path.length() - trimLength) + extension;
            f = new File(path);
            if (f.exists()) {
                return f;
            }
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                Logger.getLogger(BAM.class.getName()).log(
                        Level.WARNING, null, e);
            }

        }
        switch(extension){
            case FileExtensions.CRAM_INDEX:
                throw new IndexFileNotFoundException("could not find cram index file");
            case FileExtensions.BAI_INDEX:
                throw new IndexFileNotFoundException("could not find bam index file");
            default:
                throw new IndexFileNotFoundException();

        }
    }

    /**
     * Check if the alignment file has the index file in the remote server.
     * @param file an alignment file, for example, BAM or CRAM.
     * @param extension extension of the alignment file (Example: .bam for BAM file).
     * @param trimLength length of the extension including "." (Example: length of .bam is 4).
     * @return Index file, if exists
     * @throws IndexFileNotFoundException if the index file is not present in the location of the alignment file.
     */
    public static String findIndexFile(String file, String extension, int trimLength) throws IndexFileNotFoundException {
        try {
            String baiUriStr = file + extension;
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }

            baiUriStr = file.substring(0, file.length() - trimLength) + extension;
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                Logger.getLogger(BAM.class.getName()).log(
                        Level.WARNING, null, e);
            }
        }
        switch(extension){
            case FileExtensions.CRAM_INDEX:
                throw new IndexFileNotFoundException("could not find cram index file");
            case FileExtensions.BAI_INDEX:
                throw new IndexFileNotFoundException("could not find bam index file");
            default:
                throw new IndexFileNotFoundException();

        }
    }
}
