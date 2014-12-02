/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.bamindexer;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GFileChooser;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.sf.picard.sam.BuildBamIndex;
import net.sf.picard.sam.SortSam;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;

/**
 *
 * @author ktsuttle
 */
public class BAMIndexer {

    static boolean Debug = true;
    static int printTabs = 0;
    public static StringBuilder sb = new StringBuilder();
    private static int ERRORcount;

    public static String DEBUG(Object... args) {
        if (!Debug) {
            return "";
        }
        return SystemIO(System.err, args);
    }
    private boolean cancelOperations = false;
    private int errorCount;

    private EnumMap<opt, Object> checkSortFlag(File bamFile, EnumMap<opt, Object> option) {
        if (option.get(opt.SortType) != null) {
            return option;
        }
        mainGUI.updateStatus("Checking sort type...");
        DEBUG(TAB.RIGHT, "Header");

        //check for sorted flag. see http://gatkforums.broadinstitute.org/discussion/1317/collected-faqs-about-bam-files
        SAMFileReader sfReader = new SAMFileReader(bamFile);
        SAMFileHeader sfHeader = sfReader.getFileHeader();
        Set<Map.Entry<String, String>> attributes = sfHeader.getAttributes();
        String sortType = null;
        for (Map.Entry<String, String> attr : attributes) {
            if (attr.getKey().equalsIgnoreCase("SO")) {
                sortType = attr.getValue();
                break;
            }
        }
        DEBUG(TAB.ONCE, "SO:", sortType);
        option.put(opt.SortType, sortType);

        if (sortType == null || sortType.isEmpty() || !sortType.equalsIgnoreCase("coordinate") || sortType.equalsIgnoreCase("sorted")) {
            int dialog = JOptionPane.showConfirmDialog(mainGUI, "BAM file \"" + bamFile.getName() + "\" does not appear to be coordinate sorted.\nIndex files can be created for sorted files only.\n"
                    + "Would you like to create a new, coordinate sorted copy of the file? \n"
                    + " Note that this will create a new file that is " + readableFileSize(bamFile.length()) + " in size.", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);

            if (dialog == JOptionPane.YES_OPTION) {
                option.put(opt.DoSort, true);
            } else if (dialog == JOptionPane.CANCEL_OPTION) {
                throw new CancellationException("User canceled bam indexing operations.");
            }

        } else {
            option.put(opt.DoSort, false); //nothing needs to be sorted
        }
        return option;
    }

    static class BAMIndexAction extends GenericAction {

        BAMIndexAction() {
            super("Make Index for BAM File(s)", "16x16/actions/blank_placeholder.png", "22x22/actions/blank_placeholder.png"); //Set name and icon
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                BAMIndexer.main(null);
            } catch (Exception ex) {
                Logger.getLogger(BAMIndexer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    static BAMIndexAction getAction() {

        return new BAMIndexAction();
    }

    public static String PRINT(Object... args) {
        return SystemIO(System.out, args);
    }

    private static String SystemIO(PrintStream stream, Object... args) {
        if (args.length == 1 && args[0] == "") {
            return "";
        }

        if (args[0] instanceof TAB) {
            switch ((TAB) args[0]) {
                case RIGHT:
                    printTabs++;
                    break;
                case LEFT:
                    if (printTabs < 0) {
                        printTabs--;
                    }
                    break;
                case EDGE:
                    printTabs = 0;
                    break;
                case ONCE:
                    args[0] = "\t";
                    break;
                case RETURNRIGHT:
                    break;
                case RETURNLEFT:
                    break;
                case SET:
                    printTabs = (Integer) args[1];
                    args[1] = "";
                    break;
                case HEADER:
                    printTabs = 2;
                    break;
                case SUB:
                    printTabs = 3;
                    break;
            }
        }

        sb.append(new String(new char[printTabs]).replace("\0", "\t"));
        for (Object arg : args) {
            try {
                if (arg == null) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            if (arg instanceof TAB) {
                continue;
            }
            sb.append(arg);
            sb.append(" ");
        }
        String string = sb.toString();
        stream.println(string);
        sb.setLength(0);
        return string;
    }

    /**
     * Original Source:
     * http://stackoverflow.com/questions/2925153/can-i-pass-an-array-as-arguments-to-a-method-with-variable-arguments-in-java
     *
     * @param <T>
     * @param arr
     * @param lastElement
     * @return
     */
    public static <T> T[] CAT(T[] arr, T elem) {
        final int N = arr.length;
        arr = java.util.Arrays.copyOf(arr, N + 1);
        arr[N] = elem;
        return arr;
    }

    public static <T> T[] CAT(T elem, T[] arr) {
        final int N = arr.length;
        arr = java.util.Arrays.copyOf(arr, N + 1);
        System.arraycopy(arr, 0, arr, 1, N);
        arr[0] = elem;
        return arr;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] CAT(T elem1, T elem2) {
        T[] arr = (T[]) Array.newInstance(elem1.getClass(), 2);
        arr[0] = elem1;
        arr[1] = elem2;
        return arr;
    }

    /**
     * Original
     * Source:http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
     *
     * @param <T>
     * @param A
     * @param B
     * @return
     */
    public static <T> T[] CAT(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static <T> T[] CAT(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Original Source:
     * http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
     * Original Author: http://stackoverflow.com/users/699240/mr-ed
     *
     * @param size
     * @return
     */
    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /*
     public List<Foo> processFiles(Iterable<File> files){
     List<Future<Foo>> futures = new ArrayList<Future<Foo>>();
     ExecutorService exec = Executors.newFixedThreadPool(
     Runtime.getRuntime().availableProcessors());
     for (File f : files){
     final byte[] bytes = readAllBytes(f); // defined elsewhere
     futures.add(exec.submit(new Callable<Foo>(){
     public Foo call(){
     InputStream in = new ByteArrayInputStream(bytes);
     // Read a Foo object from "in" and return it
     }
     }));
     }
     List<Foo> foos = new List<Foo>(futures.size());
     for (Future<Foo> f : futures) foos.add(f.get());
     exec.shutdown();
     return foos;
     }*/
    public static void main(String[] args) throws Exception {
        boolean unitTest = false;
        if (args != null && args.length >= 1 && args[0].equalsIgnoreCase("test")) {
            unitTest = true;
        }
        if (unitTest) {
            new BAMIndexer(new File("/Users/ktsuttle/Workspace/BAMIndexer/test/untitled folder/aligned.sorted.bam"));
        } else {
            new BAMIndexer();
        }
    }

    public static void ERROR(Container panel, String message, Exception e) {
        ErrorHandler.errorPanel(message, e, Level.SEVERE);
        Logger.getLogger(BAMIndexer.class.getName()).log(Level.WARNING, message, e);
        JOptionPane.showMessageDialog(panel, message + "\n" + e.getLocalizedMessage());
        ERRORcount++;
    }

    public static int getERRORcount(boolean reset) {
        int tmp = ERRORcount;
        if (reset) {
            ERRORcount = 0;
        }
        return tmp;
    }

    public static int getERRORcount() {
        return getERRORcount(false);
    }

    BAMIndexer() throws Exception {
        this((List<File>) null);
    }
    private BAMIndexerProgress mainGUI = null;

    BAMIndexer(File... files) throws Exception {
        this(Arrays.asList(files));
    }

    BAMIndexer(List<File> files) throws Exception {
        DEBUG("Loading BAMIndexer...");
        this.mainGUI = new BAMIndexerProgress(this, new Double[]{.25, .75});
        try {
            if (files == null || files.isEmpty()) {
                GFileChooser fc = new GFileChooser();
                fc.setSize(650, 400);
                fc.setVisible(true);
                fc.setMultiSelectionEnabled(true);
                int dialog = fc.showOpenDialog(mainGUI);
                if (dialog == JOptionPane.OK_OPTION) {
                    files = Arrays.asList(fc.getSelectedFiles());
                }
            }
            //if nothing selected close out
            if (files == null || files.isEmpty()) {
                return;
            }
            DEBUG(TAB.RIGHT, "Loaded", files.size(), "files into BAMIndexer");
            DEBUG(files);

            this.createIndexFiles(files);
        } catch (CancellationException e) {
            mainGUI.updateTitle(e.getMessage());
            mainGUI.dispose();

        } catch (Exception e) {
            ERROR(mainGUI, "Canceled BAMIndexer operations ", e);
        }

        mainGUI.isDone(true); //force done messages
    }

    /**
     * This function will take in N number of BAM or SAM files and create
     * indexes.
     *
     * The work flow is as follows... Checks: 1)Simple check to see if there is
     * a file name collision 2) Simple SO: flag check. values expected = unknown
     * (default), unsorted, queryname and coordinate. values that pass =
     * coordinate and the unofficial "sorted" Processing: 3) Sorting and
     * indexing.
     *
     * @param bamFiles any List interface full of File objects. Expected to be
     * SAM or BAM
     * @return
     * @throws Exception
     */
    public List<File> createIndexFiles(List<File> bamFiles) throws Exception {
        //if theres no work to be done then exit out quick
        if (bamFiles == null || bamFiles.isEmpty()) {
            return bamFiles;
        }

        //////////////////////
        //INIT
        //create options hash map so we can ask all the questions to the user before doing indexes
        HashMap<String, EnumMap<opt, Object>> options = new HashMap<String, EnumMap<opt, Object>>();
        EnumMap<opt, Object> option = null; //option map per file
        // location for our .bai file
        File indexFile = null; //index output file object

        /////////////////////
        //First loop to check all the files before processing
        //go ahead and check all the files before doing any operations
        //this way the user can make all decisions before running the batch
        mainGUI.updateTitle("Checking files...");
        mainGUI.setNewTasks(bamFiles.size());
        for (File bamFile : bamFiles) {
            mainGUI.updateStatus(bamFile.getName());

            //save options from last iteration (i-1)
            if (option != null) { //skip the first iteration
                options.put(bamFile.getName(), option);
            }
            option = new EnumMap<opt, Object>(opt.class);

            //default options
            option.put(opt.DoesIndexExist, false); //boolean
            option.put(opt.Skip, false); //boolean
            option.put(opt.SortType, null); //set to null or a string
            option.put(opt.DoSort, false); //boolean

            //attempt to create a tmp file object
            try {
                //create index file object holder
                indexFile = new File(bamFile.getAbsolutePath() + ".bai"); //ex: fileName.bam.bai
            } catch (Exception e) {
                ERROR(mainGUI, "Unable to create index file. Check logger for error thrown", e);
                option.put(opt.Skip, true);
                continue;
            }
            option.put(opt.Index, indexFile.getAbsolutePath()); //save the index file to the options

            //Now see if we have a file named fileName.bai NOTE: not official ext but accepted in the community
            File altIndexName = new File(removeExtension(bamFile.getAbsolutePath()) + ".bai");
            if (altIndexName.exists()) { //alt name exists
                if (!indexFile.exists()) { //if the real extention does not exist then let's clean up by enforcing the accepted ext
                    JOptionPane.showMessageDialog(mainGUI, ""
                            + "Index file \"" + altIndexName.getName() + "\" exists. Renaming to " + indexFile.getName());
                    option.put(opt.DoesIndexExist, true);
                    altIndexName.renameTo(indexFile);
                } else { //both exist.
                    JOptionPane.showMessageDialog(mainGUI, ""
                            + "Index file \"" + altIndexName.getName() + "\" and " + indexFile.getName() + " exists. This indexing utility will use " + indexFile.getName());
                }
            }

            //See if the file exists and if the user wants to over write it
            if (indexFile.exists()) { //check to see if fileName.bam.bai exists
                int dialog = JOptionPane.showConfirmDialog(mainGUI, "Index file \"" + indexFile.getName() + "\" exists.\n Would you like to overwrite it?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                if (dialog == JOptionPane.NO_OPTION) {
                    //user does not want to overwrite this file. Keep the file on disk and set output as null
                    ErrorHandler.errorPanel("Unable to create \"" + indexFile.getName() + "\" because user does not wish to overwrite the current file on disk");
                    option.put(opt.DoesIndexExist, true);
                    continue;
                } else if (dialog == JOptionPane.YES_OPTION) {
                    indexFile.delete();
                    option.put(opt.DoesIndexExist, false);
                } else {
                    throw new CancellationException("User canceled");
                }
            }

            checkSortFlag(bamFile, option);
            //do not do work yet. Save it for the next iteration
            //option is saved into options on the next iteration or after the loop on the last one
            //it is done this way so that we can use continue safely within the loop.
            mainGUI.finishedTask();
            if (cancelOperations) {
                mainGUI.cancel();
                return bamFiles;
            }
        } //END FOR
        options.put(bamFiles.get(bamFiles.size() - 1).getName(), option); //save last options

        //////////////
        //Second loop that actually does all the processing
        mainGUI.updateTitle("Processing Files... ");
        mainGUI.updateProgressBarMessage("You may leave this window running unattended");
        mainGUI.setNewTasks(bamFiles.size());
        //File operations will be done in this loop
        for (File bamFile : bamFiles) {
            mainGUI.updateStatus(bamFile.getName());
            //load options back
            option = options.get(bamFile.getName());
            indexFile = new File((String) option.get(opt.Index));

            //if we should skip it then do so
            if ((Boolean) option.get(opt.Skip)) {
                mainGUI.updateStatus("Skipping", bamFile.getName());
                continue;
            }

            //Sort the file if needed
            File sortedFile = null;
            String sortType = (String) option.get(opt.SortType);
            if (sortType == null && (Boolean) option.get(opt.DoSort)) {

                String fileName = removeExtension(bamFile.getAbsolutePath());
                String nameFlag = ".sorted";

                //if(fileName.endsWith(nameFlag)){ //if the file name ends with sorted do not trust it. Remove it!
                //    fileName=fileName.substring(0,fileName.length()-nameFlag.length());
                //}
                DEBUG("Sorted File name", fileName);
                sortedFile = new File(fileName + nameFlag + ".bam");

                if (sortedFile.exists()) {
                    sortedFile.delete();
                }
                DEBUG("Creating new sorted BAM file -> ", sortedFile.getName());
                try {
                    String[] sortCommand = new String[]{"INPUT=" + bamFile.getAbsolutePath(), "OUTPUT=" + sortedFile.getAbsolutePath(), "SORT_ORDER=" + "coordinate"};
                    SortSam sortSam = new SortSam();
                    sortSam.instanceMain(sortCommand);
                } catch (Exception e) {
                    ERROR(mainGUI, "Error while sorting " + bamFile.getName(), e);
                    continue;
                }

            }

            //Some simple debug output
            DEBUG("Creating new BAM index file ->", indexFile.getName());

            //create index
            File unindexedFile = (sortedFile == null) ? bamFile : sortedFile;
            //FINALLY! do the index build
            try {
                String[] indexCommand = new String[]{"INPUT=" + unindexedFile.getAbsolutePath(), "OUTPUT=" + indexFile.getAbsolutePath(), "QUIET=" + !Debug};
                BuildBamIndex buildIndex = new BuildBamIndex();
                buildIndex.instanceMain(indexCommand);
            } catch (Exception e) {
                ERROR(mainGUI, "Error while indexing " + unindexedFile.getName(), e);
                continue;
            }

            mainGUI.finishedTask();
            if (cancelOperations) {
                mainGUI.cancel();
                return bamFiles;
            }
        }

        int errors = getERRORcount();
        if (errors != 0) {
            this.mainGUI.updateProgressBarMessage("There were " + errors + "errors.");
            this.mainGUI.updateStatus("check the console for errors");
        }
        return null;
    }

    public static String removeExtension(String txt) {
        int index = txt.lastIndexOf('.');
        if (index != -1) {
            return txt.substring(0, index);
        }
        return txt;
    }

    void cancel() {
        this.cancelOperations = true;
        mainGUI.setTitle("Canceling Operations after current file");

    }

    enum TAB {

        LEFT, RIGHT, EDGE, RETURNLEFT, RETURNRIGHT, ONCE, SET, HEADER, SUB
    }

    enum opt {

        DoesIndexExist, Skip, SortType, Index, DoSort
    }
}
