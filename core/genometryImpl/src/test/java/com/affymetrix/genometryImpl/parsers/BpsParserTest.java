package com.affymetrix.genometryImpl.parsers;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.symmetry.impl.UcscPslSym;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Ignore;

/**
 *
 * @author jnicol
 */
public class BpsParserTest {

    /**
     * Verify that converting to a Bps file always works the same. (This doesn't
     * mean it's correct, just that its behavior hasn't changed.)
     */
    @Test
    public void testConvertToBps() {
        InputStream istr = null;
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        try {
            String filename = "data/psl/test1.psl";
            filename = BpsParserTest.class.getClassLoader().getResource(filename).getFile();
            istr = new FileInputStream(filename);
            assertNotNull(istr);
            AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
            boolean annot_seq = true;
            String stream_name = "test_file";

            PSLParser parser = new PSLParser();
            List<UcscPslSym> syms = parser.parse(istr, stream_name, group, group, annot_seq, true);

            BpsParser instance2 = new BpsParser();
            boolean writeResult = instance2.writeAnnotations(syms, null, "", outstream);
            assertEquals(true, writeResult);

        } catch (Exception ex) {
            Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                istr.close();
            } catch (IOException ex) {
                Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            String filename = "test1.bps";
            filename = BpsParserTest.class.getClassLoader().getResource(filename).getFile();
            istr = new FileInputStream(filename);
            assertNotNull(istr);

            BufferedInputStream bis = new BufferedInputStream(istr);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int result = bis.read();
            while (result != -1) {
                byte b = (byte) result;
                buf.write(b);
                result = bis.read();
            }

            assertEquals(outstream.toString(), buf.toString());

        } catch (Exception ex) {
            Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                istr.close();
            } catch (IOException ex) {
                Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

}
