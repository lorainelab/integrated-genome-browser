package com.gene.searchmodelucene;

import static com.affymetrix.genometry.tooltip.ToolTipConstants.DESCRIPTION;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.ID;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.NAME;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.TITLE;
import org.lorainelab.igb.services.search.ISearchHints;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.document.Document;

/**
 *
 * @author hiralv
 */
public class SearchHints implements ISearchHints {

    private final LuceneSearch<String> luceneSearch = new LuceneSearch<String>() {
        String[] search_fields = new String[]{ID, NAME, TITLE, DESCRIPTION};

        @Override
        public String processSearch(Document doc) {
            for (String search_field : search_fields) {
                String value = doc.get(search_field);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

//		@Override
//		protected String massageSearchTerm(String searchTerm) {
//			String query = MessageFormat.format("{1}:{0} OR {2}:{0} OR {3}:{0}",
//					searchTerm, search_fields[0], search_fields[1], search_fields[2]);
//			return super.massageSearchTerm(query);
//		}
    };

    public Set<String> search(String search_term) {
        Set<String> syms = new HashSet<>();
//        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
//        group.getAvailableGenomeVersions().stream().filter(dataContainer -> dataContainer.getgServer().getServerType() == LocalFilesServerType.getInstance() || dataContainer.getgServer().getServerType() == QuickloadServerType.getInstance()).forEach(dataContainer -> {
//            for (DataSet feature : dataContainer.getDataSets()) {
//                if (feature.isVisible() && feature.getSymL() != null) {
//                    List<String> results = luceneSearch.searchIndex(feature.getSymL().uri.toString(), search_term, MAX_HITS);
//                    if (results != null) {
//                        syms.addAll(results);
//                    }
//                }
//            }
//        });
        return syms;
    }

}
