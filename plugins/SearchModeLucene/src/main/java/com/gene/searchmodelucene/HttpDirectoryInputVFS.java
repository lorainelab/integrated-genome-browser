package com.gene.searchmodelucene;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.apache.lucene.store.BufferedIndexInput;

public class HttpDirectoryInputVFS extends BufferedIndexInput {

    private final FileObject httpFile;
    private final RandomAccessContent rac;
    private final String fileUrl;

    public HttpDirectoryInputVFS(String httpURL, String filename) throws IOException {
        super(filename);
        String url = httpURL;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += filename;
        fileUrl = url;
//		HttpFileSystemConfigBuilder.getInstance().setMaxTotalConnections(null, 150);
//		HttpFileSystemConfigBuilder.getInstance().setMaxConnectionsPerHost(null, 25);
        this.httpFile = VFS.getManager().resolveFile(fileUrl);
        rac = ((AbstractFileObject) httpFile).getRandomAccessContent(RandomAccessMode.READ);
    }

    @Override
    public void close() throws IOException {
        httpFile.close();
    }

    @Override
    public long length() {
        long length = 0;

        try {
            length = rac.length();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return length;
    }

    @Override
    protected void readInternal(byte[] b, int offset, int length)
            throws IOException {
        seekInternal(getFilePointer());
        rac.readFully(b, offset, length);
    }

    @Override
    protected void seekInternal(long pos) throws IOException {
        rac.seek(pos);
    }
}
