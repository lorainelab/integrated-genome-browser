package com.affymetrix.genometryImpl.util;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import net.sf.samtools.seekablestream.SeekableStream;

/**
 * User: jrobinso
 * Date: Apr 13, 2010
 */
public class SeekableFTPStream extends SeekableStream{

    static final Logger log = Logger.getLogger(SeekableFTPStream.class.getName());

    private long position = 0;
    private String host;
    private String path;
    FTPClient ftp = null;

    public SeekableFTPStream(URL url) throws IOException {
        this.host = url.getHost();
        this.path = url.getPath();
        ftp = openClient(host);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
    }

    public void seek(long position) {
        this.position = position;
    }

    public long position() {
        return position;
    }
 
	@Override
    public long skip(long n) throws IOException {
        long bytesToSkip = n;
        position += bytesToSkip;
        return bytesToSkip;
    }

    public int read(byte[] buffer, int offset, int len) throws IOException {

        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }


        int n = 0;
        InputStream is = null;

        try {

            ftp.setRestartOffset(position);
            is = ftp.retrieveFileStream(path);
   
            while (n < len) {
                int count = is.read(buffer, offset + n, len - n);
                if (count < 0) {
                    if (n == 0) {
                        return -1;
                    } else {
                        break;
                    }
                }
                n += count;
            }

            position += n;

            return n;

        }

        catch (EOFException e) {
            if (n < 0) {
                return -1;
            } else {
                position += n;
                return n;
            }

        }

        finally {
            if (is != null) {
                is.close();
            }
            ftp.completePendingCommand();
        }
    }


    public void close() throws IOException {
        // Nothing to do
    }

    public int read() throws IOException {
        throw new UnsupportedOperationException("read() is not supported on SeekableHTTPStream.  Must read in blocks.");
    }


    private FTPClient openClient(String host) throws IOException {

        FTPClient temp_ftp = new FTPClient();
        temp_ftp.connect(host);

        // After connection attempt, you should check the reply code to verify
        // success.
        int reply = temp_ftp.getReplyCode();

        if (!FTPReply.isPositiveCompletion(reply)) {
            temp_ftp.disconnect();
            System.err.println("FTP server refused connection.");
            throw new RuntimeException("FTP server refused connection.");
        }

        boolean success = temp_ftp.login("anonymous", "igb");
        if (!success) {
            System.err.println("FTP login failed " + temp_ftp.getReplyString());
            throw new RuntimeException("FTP login failed " + temp_ftp.getReplyString());
        }

        // Use passive mode as default because most of us are
        // behind firewalls these days.
        temp_ftp.enterLocalPassiveMode();


        return temp_ftp;
    }

	public long length(){
		 throw new UnsupportedOperationException("length() is not supported on SeekableFTPStream.");
	}
	
	public boolean eof() throws IOException {
		throw new UnsupportedOperationException("eof() is not supported on SeekableFTPStream.");
	}
	
	public String getSource(){
		return path;
	};
}

