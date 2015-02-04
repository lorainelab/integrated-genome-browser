package com.affymetrix.genometry.symloader;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometry.util.GeneralUtils;

import org.broad.tribble.readers.TabixReader;

public class TbiZoomSymLoader extends IndexZoomSymLoader {

    private TabixReader tabixReader;

    public TbiZoomSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
    }

    private URI getFileURI(URI tbiUri) throws Exception {
        String baseUriString = tbiUri.toString().substring(0, tbiUri.toString().length() - ".tbi".length());
        if (!baseUriString.startsWith(FILE_PROTOCOL) && !baseUriString.startsWith(HTTP_PROTOCOL) && !baseUriString.startsWith(HTTPS_PROTOCOL) && !baseUriString.startsWith(FTP_PROTOCOL)) {
            baseUriString = GeneralUtils.getFileScheme() + baseUriString;
        }
        return new URI(baseUriString);
    }

    @Override
    protected SymLoader getDataFileSymLoader() throws Exception {
        URI baseUri = getFileURI(uri);
        return FileTypeHolder.getInstance().getFileTypeHandlerForURI(baseUri.toString()).createSymLoader(baseUri, featureName, group);
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }
        try {
            String uriString;
            if (uri.toString().startsWith(HTTP_PROTOCOL) || uri.toString().startsWith(HTTPS_PROTOCOL)) {
                uriString = uri.toString();
            } else {
                uriString = GeneralUtils.fixFileName(uri.toString());
            }
            uriString = uriString.substring(0, uriString.length() - ".tbi".length());
            tabixReader = new TabixReader(uriString);
            tabixReader.readIndex();
        } catch (Exception x) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                    "Could not read tabix for {0}.",
                    new Object[]{featureName});
            return;
        }
        this.isInitialized = true;
    }

    @Override
    protected Iterator<Map<Integer, List<List<Long>>>> getBinIter(String seq) {
        return getBinIter(getSynonymMap(), seq);
    }

    private List<List<Long>> getChunkList(final Object[] chunks) {
        return new AbstractList<List<Long>>() {
            @Override
            public List<Long> get(int index) {
                Object chunk = chunks[index];
                List<Long> chunkList = new ArrayList<>();
                try {
                    Field privateReaderField = chunk.getClass().getDeclaredField("u");
                    privateReaderField.setAccessible(true);
                    Long u = (Long) privateReaderField.get(chunk);
                    chunkList.add(u);
                    privateReaderField = chunk.getClass().getDeclaredField("v");
                    privateReaderField.setAccessible(true);
                    Long v = (Long) privateReaderField.get(chunk);
                    chunkList.add(v);
                } catch (NoSuchFieldException x) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot read tbi index for " + uri, x);
                } catch (IllegalAccessException x) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot read tbi index for " + uri, x);
                }
                return chunkList;
            }

            @Override
            public int size() {
                return chunks.length;
            }
        };
    }

    private Iterator<Map<Integer, List<List<Long>>>> getBinIterator(final HashMap<Integer, Object[]> bins) {
        return new Iterator<Map<Integer, List<List<Long>>>>() {
            Iterator<Integer> binIter = bins.keySet().iterator();

            @Override
            public boolean hasNext() {
                return binIter.hasNext();
            }

            @Override
            public Map<Integer, List<List<Long>>> next() {
                final Integer binNo = binIter.next();
                return new AbstractMap<Integer, List<List<Long>>>() {
                    @Override
                    public Set<Map.Entry<Integer, List<List<Long>>>> entrySet() {
                        Set<Map.Entry<Integer, List<List<Long>>>> entrySet
                                = new HashSet<>();
                        final Object[] chunks = bins.get(binNo);
                        entrySet.add(new SimpleEntry<>(binNo, getChunkList(chunks)));
                        return entrySet;
                    }
                };
            }

            @Override
            public void remove() {
            }
        };
    }

    private int getRefNo(Map<String, String> synonymMap, String igbSeq) {
        try {
            Field privateReaderField = tabixReader.getClass().getDeclaredField("mChr2tid");
            privateReaderField.setAccessible(true);
            @SuppressWarnings("unchecked")
            HashMap<String, Integer> mChr2tid = (HashMap<String, Integer>) privateReaderField.get(tabixReader);
            for (String chr : mChr2tid.keySet()) {
                String bamSeq = synonymMap.get(chr);
                if (igbSeq.equals(bamSeq)) {
                    return mChr2tid.get(chr);
                }
            }
        } catch (NoSuchFieldException x) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot read tbi index for " + uri, x);
        } catch (IllegalAccessException x) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot read tbi index for " + uri, x);
        }
        return -1;
    }

    private Iterator<Map<Integer, List<List<Long>>>> getBinIter(Map<String, String> synonymMap, String seq) {
        int refno = getRefNo(synonymMap, seq);
        if (refno != -1) {
            try {
                Field privateReaderField = tabixReader.getClass().getDeclaredField("mIndex");
                privateReaderField.setAccessible(true);
                Object[] mIndexArrayValue = (Object[]) privateReaderField.get(tabixReader);
                Object mIndexItemValue = mIndexArrayValue[refno];
                privateReaderField = mIndexItemValue.getClass().getDeclaredField("b");
                privateReaderField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final HashMap<Integer, Object[]> bins = (HashMap<Integer, Object[]>) privateReaderField.get(mIndexItemValue);
                return getBinIterator(bins);
            } catch (NoSuchFieldException x) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot access tbi index for " + uri, x);
            } catch (IllegalAccessException x) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot access tbi index for " + uri, x);
            }
        }
        return null;
    }
}
