package apollo.analysis;

import java.io.FileInputStream;

import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.StrandedFeatureSetI;

public class BlastXMLParserTest {

    public static void main(String[] args) throws Exception {
        testParse();
    }

    public static void testParse() throws Exception {
        FileInputStream fis = new FileInputStream("/Users/lorainelab/src/genoviz-trunk/plugins/NCBIBlast/src/apollo/analysis/completeTestBlast.xml");
        BlastXMLParser parser = new BlastXMLParser();
        StrandedFeatureSetI results = new StrandedFeatureSet();
        parser.parse(fis, results);
    }
}
