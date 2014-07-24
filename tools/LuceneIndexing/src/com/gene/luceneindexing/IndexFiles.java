package com.gene.luceneindexing;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class IndexFiles {
	private static final PrintStream DUMP_STREAM = System.err;
	private static final int SYMS_PER_DOT = 64 * 64;
	private static final int DOTS_PER_LINE = 64;
	private static final int SYMS_PER_LINE = SYMS_PER_DOT * DOTS_PER_LINE;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("index");
	private static final List<String> DEFAULT_PROPS = new ArrayList<String>();
	static {
		Collections.addAll(DEFAULT_PROPS, BUNDLE.getString("_default").split(","));
	}
	private static final List<String> IGNORE_PROPS = new ArrayList<String>();
	static {
		Collections.addAll(IGNORE_PROPS, BUNDLE.getString("_ignore").split(","));
	}
	private static final Analyzer analyzer = new KeywordAnalyzer();

	public IndexFiles() {
		super();
	}

	// main method for testing
	public static void main(final String[] args) {
		try {
			new IndexFiles().createIndex(args[0], args.length > 1 && "dump".equals(args[1]));
		} catch (IOException e) {
			e.printStackTrace(System.out);
			System.out.println("Lucene Indexing caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}

	/**
	 * @param docsPath the path of the directory to index
	 */
	public void createIndex(String docsPath, boolean dump) throws IOException {
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			DUMP_STREAM.println("Document directory '" +
				docDir.getAbsolutePath() +
				"' does not exist or is not readable, please check the path");
			return;
		}

		Date start = new Date();
		indexDocs(docDir, dump);
		Date end = new Date();
		DUMP_STREAM.println("Indexing completed successfully - " + (end.getTime() - start.getTime()) / 1000 + " total seconds");

	}

	// Deletes all files and subdirectories under dir.
	// Returns true if all deletions were successful.
	// If a deletion fails, the method stops attempting to delete and returns
	// false.
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	private void addProp(Document doc, String name, Object value, boolean index) {
		if (doc == null) {
			DUMP_STREAM.println(">>>index string name=" + name + ";value=" + ArrayUtils.toString(value) + ";index=" + index);
		}
		else {
			Field field = new Field(name, ArrayUtils.toString(value), Field.Store.YES, Field.Index.NO);
			doc.add(field);
			if (index) {
				Field field2 = new Field(name, ArrayUtils.toString(value).toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS);
				field2.setIndexOptions(IndexOptions.DOCS_ONLY);
				doc.add(field2);
			}
		}
	}

	private void addProp(Document doc, String name, int value) {
		if (doc == null) {
			DUMP_STREAM.println(">>>index int name=" + name + ";value=" + value);
		}
		else {
			NumericField field = new NumericField(name, Field.Store.YES, false);
			field.setIntValue(value);
			doc.add(field);
		}
	}

	private void processSym(IndexWriter writer, SeqSymmetry sym, List<String> props, BioSeq seq, boolean index_childrens) throws IOException {
		if (sym.getChildCount() > 0 && index_childrens) {
			for (int i = 0; i < sym.getChildCount(); i++) {
				processSym(writer, sym.getChild(i), props, seq, index_childrens);
			}
		}
		Document doc = (writer == null) ? null : new Document();
		if (sym.getID() != null) {
			addProp(doc, "id", sym.getID(), true);
		}
		SeqSpan span = sym.getSpan(seq);
		addProp(doc, "seq", span.getBioSeq().getID(), false);
		addProp(doc, "start", span.getStart());
		addProp(doc, "end", span.getEnd());
        if (sym instanceof SymWithProps) {
        	Map<String, Object> symProps = ((SymWithProps)sym).getProperties();
        	if (props.contains("*")) {
        		props = new ArrayList<String>(symProps.keySet());
        	}
	        for (String prop : props) {
	        	if (!IGNORE_PROPS.contains(prop.toLowerCase())) {
		        	if ("id".equals(prop) && symProps.get(prop) != null && sym.getID() != null) {
		        		if (!sym.getID().equals(symProps.get(prop))) {
		        			DUMP_STREAM.println("!!!!! ERROR !!!!! - seq symmetry sym.getID() = \"" + sym.getID() + "\" is not the same as sym.getProperties().get(\"" + prop + "\") = \"" + symProps.get(prop));
		        		}
		        	}
					addProp(doc, prop, symProps.get(prop), symProps.get(prop) != null);
	        	}
	        }
        }
        if (writer == null) {
	    	DUMP_STREAM.println("==============================================================");
        }
        else {
        	writer.addDocument(doc);
        }
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is
	 * given, recurses over files and directories found under the given
	 * directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For
	 * good throughput, put multiple documents into your input file(s). An
	 * example of this is in the benchmark module, which can create "line doc"
	 * files, one document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be
	 *            stored
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @throws IOException
	 */
	private void indexDocs(File file, boolean dump) throws IOException {

		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					if (!dump) {
						// first delete old indexes
						for (String dirName : file.list()) {
							File dir = new File(file, dirName);
							if (dir.isDirectory() && FileUtil.getInstance().isIndexName(dirName)) {
								deleteDir(dir);
								File dirFile = new File(dir.getAbsolutePath() + ".dir");
								if (dirFile.exists()) {
									dirFile.delete();
								}
							}
						}
					}
					if (!FileUtil.getInstance().isIndexName(file.getAbsolutePath())) {
						for (int i = 0; i < files.length; i++) {
							indexDocs(new File(file, files[i]), dump);
						}
					}
				}
			}
			else {
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				}
				catch (FileNotFoundException fnfe) {
					// at least on windows, some temporary files raise this
					// exception with an "access denied" message
					// checking if the file can be read doesn't help
					return;
				}

				String indexPath = FileUtil.getInstance().getIndexName(file.getAbsolutePath());
				Directory dir = FSDirectory.open(new File(indexPath));
				try {
					URI uri = file.toURI();
					String extension = FileTypeHolder.getInstance().getExtensionForURI(uri.toString());
					FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(extension);
					if (FileUtil.getInstance().isDirName(uri.toString()) || fth == null 
							|| fth.getFileTypeCategory() == FileTypeCategory.Graph 
							|| fth.getFileTypeCategory() == FileTypeCategory.Mismatch
							|| fth.getFileTypeCategory() == FileTypeCategory.ScoredContainer
							|| fth.getFileTypeCategory() == FileTypeCategory.Sequence) {
						DUMP_STREAM.println("skipping " + file);
						return;
					}
					// get properties to index
					List<String> props = new ArrayList<String>(DEFAULT_PROPS);
					try {
						Collections.addAll(props, BUNDLE.getString(extension).split(","));
					}
					catch (MissingResourceException x) {}
					String featureName = ""; // dummy
					AnnotatedSeqGroup group = new AnnotatedSeqGroup(""); // dummy
					DUMP_STREAM.println("loading " + file);
					SymLoader symL = fth.createSymLoader(uri, featureName, group);
                                        IndexWriter writer = null;
					if (!dump) {
//						IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_35, analyzer);
//						iwc.setOpenMode(OpenMode.CREATE);
						writer = new IndexWriter(dir, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
					}
					// get all seq symmetries and index them.
					List<? extends SeqSymmetry> syms;// = symL.getGenome();
                                        int count = 0;
                                        for (BioSeq seq : symL.getChromosomeList()) {
                                            syms = symL.getChromosome(seq);
                                            DUMP_STREAM.println("processing " + syms.size() + " records for " + file + " on sequence " + seq.getID());
                                            Date start = new Date();
                                            for (SeqSymmetry sym : syms) {
                                                processSym(writer, sym, props, seq, false);
                                                count++;
                                                if (count % SYMS_PER_DOT == 0) {
                                                    DUMP_STREAM.print(".");
                                                    DUMP_STREAM.flush();
                                                }
                                                if (count % SYMS_PER_LINE == 0) {
                                                    Date now = new Date();
                                                    long elapsedSeconds = (now.getTime() - start.getTime()) / 1000;
                                                    double secsPerSym = (double) elapsedSeconds / (double) count;
                                                    long remaining = (long) ((syms.size() - count) * secsPerSym);
                                                    long hours = remaining / 3600;
                                                    remaining = remaining % 3600;
                                                    long minutes = remaining / 60;
                                                    long seconds = remaining % 60;
                                                    DUMP_STREAM.println(" " + (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds + " remaining");
                                                    DUMP_STREAM.flush();
                                                }
                                            }
                                            if (count % SYMS_PER_LINE > 0) {
                                                DUMP_STREAM.println();
                                            }
                                       }
					
					
					

					if (writer != null) {
						writer.close();
						// now get a directory listing in the .dir file
						File listFile = new File(indexPath + ".dir");
						listFile.delete();
						BufferedWriter bw = new BufferedWriter(new FileWriter(listFile));
						File fileDir = new File(indexPath);
						for (String fileName : fileDir.list()) {
							bw.write(fileName);
							bw.newLine();
						}
						bw.close();
					}
					DUMP_STREAM.println("finished indexing " + file);
				}
				catch(Exception x) {
					x.printStackTrace(System.out);
				}
				finally {
					fis.close();
				}
			}
		}
	}
}
