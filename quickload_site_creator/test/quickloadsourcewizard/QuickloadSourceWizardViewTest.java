/*
 * This is the GUI version.  QuickloadSourceWizardCmd for command line version
 * and open the template in the editor.
 */
package quickloadsourcewizard;


import edu.uncc.bioinformatics.quickloadbuilder.Annotation;
import java.io.File;
import edu.uncc.bioinformatics.quickloadbuilder.QuickloadSourceCreator;
import edu.uncc.bioinformatics.quickloadbuilder.QuickLoadArchiveBuilderApp;
import edu.uncc.bioinformatics.quickloadbuilder.QuickloadSourceValidator;
import edu.uncc.bioinformatics.quickloadbuilder.QuickloadSourceValidator.ErrorListener;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import java.awt.Component;
import java.util.Collection;
import javax.swing.JSlider;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.SAXException;

/**
 * This is a sample application on testing swing just using jUnit.
 * There are other libraries to make this process less painfull.  These 
 * suits can be studied in the future.
 * @author jfvillal
 */
public class QuickloadSourceWizardViewTest {
    
    public QuickloadSourceWizardViewTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test 
    public void testChromeInfoValidator(){
        String web = "http://igbquickload.iplantcollaborative.org/quickload/A_thaliana_Apr_2008/mod_chromInfo.txt";
        
        assert ( 
            QuickloadSourceValidator.validateModchromeURI(web, new ErrorListener(){
                        public void setError(String error) {
                            System.out.println( error );
                        }
                    }
                ) 
        );
        
        String computer = "file:///home/jfvillal/igb/genoviz/trunk/quickload_site_creator/testdata/mod_chromInfo.txt";
        assert ( 
            QuickloadSourceValidator.validateModchromeURI(computer, new ErrorListener(){
                        public void setError(String error) {
                            System.out.println( error );
                        }
                    }
                ) 
        );
        //has one of the sequences not an intger.
        String bad = "file:///home/jfvillal/igb/genoviz/trunk/quickload_site_creator/testdata/bad_chrome_info/mod_chromInfo.txt";
        assert ( !
            QuickloadSourceValidator.validateModchromeURI(bad, new ErrorListener(){
                        public void setError(String error) {
                            System.out.println( error );
                        }
                    }
                ) 
        );
    }
    /**
     * Tests the proper reading of the annotation file
     */
    @Test
    public void testReadAnnotsFile(){
        QuickloadSourceCreator creator = new QuickloadSourceCreator();
        try {
            HashMap<String, Annotation> map = creator.readAnnotationFile( new File( "testdata/annots.xml" ) );

            File test = new File ("testdata/" + "TAIR10.bed.gz");
            //System.out.println("test #" + test.getAbsolutePath() +"#");
            String key = test.getAbsolutePath();
            Annotation value = map.get( key );
            /*Collection<String> keyset = map.keySet();
            for( String s : keyset){
                System.out.println("key: #" + s + "#" );
            }
            
            Collection<Annotation> valueset = map.values();
            for( Annotation a : valueset){
                System.out.println( a );
            }*/
            assert( value != null);
            //test title, description read.
            assert( value.Title.equals( "TAIR10 ALL GENES") );
            assertEquals( value.Description , "All gene annotations - anything with a name like AT1G12345, including transposable element genes");
            assert( value.BackgroundColor.equals(Color.decode("0xd04949")) );
            
            test = new File ("testdata/" + "TAIR9_pseudogene.bed.gz" );
            key = test.getAbsolutePath();
            value = map.get( key );
            
            //test max depth, namesize when not present in the tag
            assert( value.BackgroundColor == null);
            assert( value.PositiveStrandColor.equals( Color.decode("0xb7b229" )));
            assert( value.MaxDepth == -1);
            assert( value.NameSize == -1);
            
            //test invalid negative_strand color string, test read max_dept, name_size, connected, loadhit
            test = new File ("testdata/" + "TAIR9_ncRNA.bed.gz" );
            key = test.getAbsolutePath();
            value = map.get( key );
            assert( value.MaxDepth == 23);
            assert( value.NameSize == 22);
            assert( value.NegativeStrandColor == null );
            assert( value.Connected );
            assert( value.LoadHint );
            
            //test invalid max_depth and invalid name_size read
            test = new File ("testdata/" + "TAIR9_snRNA.bed.gz" );
            key = test.getAbsolutePath();
            value = map.get( key );
            assert( value.MaxDepth == -1);
            assert( value.NameSize == -1);
            
            
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(QuickloadSourceWizardViewTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(QuickloadSourceWizardViewTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(QuickloadSourceWizardViewTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Test of showAboutBox method, of class QuickloadSourceWizardView.
     */
   
}
