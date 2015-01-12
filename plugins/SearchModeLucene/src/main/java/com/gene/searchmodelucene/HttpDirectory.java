package com.gene.searchmodelucene;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.NoSuchDirectoryException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * read only directory for http URL for Lucene
 */
public class HttpDirectory extends Directory {

    private String httpURL;
    private String[] fileList;
    private Map<String, HttpDirectoryInputSeekableStream> httpFiles = new HashMap<>();

    public HttpDirectory(String url) {
        httpURL = url;
    }

    /*
     * list all the files in the directory via a premade .dir file with the
     * directory listing
     */
    @Override
    public String[] listAll() throws IOException {
        if (fileList == null) {
            if (FileUtil.getInstance().isIndexName(httpURL)) {
                try {
                    if (httpURL.startsWith("http")) {
                        URL url = new URL(httpURL + ".dir");
                        URLConnection conn = url.openConnection();
                        conn.connect();
                        List<String> flist;
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                            String line;
                            flist = new ArrayList<>();
                            while ((line = br.readLine()) != null) {
                                flist.add(line);
                            }
                        }
                        fileList = flist.toArray(new String[]{});
                    } else {
                        File file = new File(httpURL);
                        fileList = file.list();
                    }
                } catch (Exception x) {
                    throw new NoSuchDirectoryException("directory '" + httpURL + "' cannot be loaded");
                }
            }
        }
        return fileList;
    }

    /**
     * get the HttpDirectoryInput for a file in the directory
     *
     * @param name the name of the file
     * @return the HttpDirectoryInput for the file
     */
    private HttpDirectoryInputSeekableStream getHttpDirectoryInput(String name) {
        try {
            HttpDirectoryInputSeekableStream input = httpFiles.get(name);
            if (input == null) {
                input = new HttpDirectoryInputSeekableStream(httpURL, name);
                httpFiles.put(name, input);
            }
            return input;
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    @Override
    public boolean fileExists(String name) throws IOException {
        return (Arrays.asList(listAll()).contains(name));
    }

    @Override
    public long fileLength(String name) throws IOException {
        return getHttpDirectoryInput(name).length();
    }

    @Override
    public void close() throws IOException {
        httpFiles.clear();
        fileList = null;
    }

    @Override
    public IndexInput openInput(String name) throws IOException {
        if (getHttpDirectoryInput(name) == null) {
            throw new IOException("HttpDirectory could not create IndexInput");
        }
        return getHttpDirectoryInput(name);
    }

// the following methods are not implemented since this is a read only implementation
    @Override
    public IndexOutput createOutput(String name) throws IOException {
        return null;
    }

    @Override
    public void deleteFile(String name) throws IOException {
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public void touchFile(String name) throws IOException {
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public long fileModified(String name) throws IOException {
        return 0;
    }

    @Override
    public Lock makeLock(String lockName) {
        return NoLockFactory.getNoLockFactory().makeLock(lockName);
    }
}
