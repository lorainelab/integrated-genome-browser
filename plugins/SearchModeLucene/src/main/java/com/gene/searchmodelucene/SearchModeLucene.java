package com.gene.searchmodelucene;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.lorainelab.igb.services.search.ISearchModeSym;
import com.lorainelab.igb.services.search.IStatus;
import com.lorainelab.igb.services.search.SearchResults;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

//disabled because it doesn't work as expected and seems to loop endlessly
//@Component(name = SearchModeLucene.COMPONENT_NAME, immediate = true, provide = ISearchMode.class)
public class SearchModeLucene implements ISearchModeSym {

    public static final String COMPONENT_NAME = "SearchModeLucene";
    private static final int SEARCH_ALL_ORDINAL = 1000;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmodelucene");
    private static final int MAX_HITS = 1000;

    private static final LuceneSearch<SeqSymmetry> luceneSearch = new LuceneSearch<SeqSymmetry>() {
        @Override
        public SeqSymmetry processSearch(Document doc) {
            SimpleSymWithProps sym = new SimpleSymWithProps();
            // load properties
            for (Fieldable field : doc.getFields()) {
                String value = doc.get(field.name());
                sym.setProperty(field.name(), value);
            }
            // load span
            String seqName = doc.get("seq");
            BioSeq seq;
            if (GenometryModel.getInstance().getSelectedGenomeVersion() == null) {
                seq = new BioSeq(seqName, 0);
            } else {
                seq = GenometryModel.getInstance().getSelectedGenomeVersion().getSeq(seqName);
            }
            int start = Integer.parseInt(doc.get("start"));
            int end = Integer.parseInt(doc.get("end"));
            sym.addSpan(new SimpleSeqSpan(start, end, seq));
            return sym;
        }
    };

    public SearchModeLucene() {

    }


    /* for testing only */
//    public static void main(String[] args) throws Exception {
//        SearchModeLucene s = new SearchModeLucene(null);
//        String uri = args.length > 1 ? args[0] : "file:/C:/Program Files (x86)/Apache Software Foundation/Apache2.2/htdocs/quickload2/H_sapiens_Feb_2009/IGIS Gene Models.gff.gz";
////		String uri = args.length > 1 ? args[0] : "http://localhost/Bed/bed_01.bed";
//        String searchTerm = args.length > 2 ? args[1] : "ZNF19*";
//        String seqName = args.length > 3 ? args[2] : null;
//        List<SeqSymmetry> results = luceneSearch.searchIndex(uri, searchTerm, MAX_HITS);
//        for (SeqSymmetry result : results) {
//            System.out.println(result.getName() + " @ " + result.getSpan(0));
//        }
//    }
    @Override
    public String getName() {
        return BUNDLE.getString("searchLucene");
    }

    @Override
    public int searchAllUse() {
        return SEARCH_ALL_ORDINAL;
    }

    @Override
    public String getTooltip() {
        return BUNDLE.getString("searchLuceneTooltip");
    }

    @Override
    public boolean useGenomeInSeqList() {
        return true;
    }

    @Override
    public String checkInput(String search_text, BioSeq vseq, String seq) {
        return null;
    }

    @Override
    public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot contSym) {
        return luceneSearch.searchIndex(contSym.getType(), search_text, MAX_HITS);
    }

    @Override
    public SearchResults<SeqSymmetry> search(String search_text, BioSeq chrFilter, IStatus statusHolder, boolean option) {
        List<SeqSymmetry> syms = new ArrayList<>();
        if (search_text != null && !search_text.isEmpty()) {
            GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
            if (genomeVersion != null) {
//                group.getAvailableDataContainers().stream().filter(dataContainer -> dataContainer.getgServer().getServerType() == LocalFilesServerType.getInstance() || dataContainer.getgServer().getServerType() == QuickloadServerType.getInstance()).forEach(dataContainer -> {
//                    for (DataSet feature : dataContainer.getDataSets()) {
//                        if (feature.isVisible() && feature.getSymL() != null) {
//                            if (statusHolder != null) {
//                                statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearching"), feature.getSymL().uri.toString(), search_text));
//                            }
//                            List<SeqSymmetry> results = luceneSearch.searchIndex(feature.getSymL().uri.toString(), search_text, MAX_HITS);
//                            if (results != null) {
//                                syms.addAll(results);
//                            }
//                        }
//                    }
//                });
            }
        }
        String statusStr;
        if (syms.isEmpty()) {
            statusStr = BUNDLE.getString("searchNoResults");
            statusHolder.setStatus(statusStr);
            return new SearchResults<>(getName(), search_text, chrFilter != null ? chrFilter.getId() : "genome", statusStr, null);
        }
        statusStr = MessageFormat.format(BUNDLE.getString("searchSummary"), syms.size());
        statusHolder.setStatus(statusStr);

        return new SearchResults<>(getName(), search_text, chrFilter != null ? chrFilter.getId() : "genome", statusStr, syms);
    }

}
