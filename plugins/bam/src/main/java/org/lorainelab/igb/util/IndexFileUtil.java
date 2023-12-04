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
    public static File findIndexFile(File bamfile, String extension, int trimLength) throws IndexFileNotFoundException {
        try {
            String path = bamfile.getPath();
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

    public static String findIndexFile(String bamfile, String extension, int trimLength) throws IndexFileNotFoundException {
        try {
            String baiUriStr = bamfile + extension;
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }

            baiUriStr = bamfile.substring(0, bamfile.length() - trimLength) + extension;
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
