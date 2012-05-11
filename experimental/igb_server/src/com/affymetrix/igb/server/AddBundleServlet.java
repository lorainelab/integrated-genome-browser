package com.affymetrix.igb.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.osgi.impl.bundle.bindex.Index;

public class AddBundleServlet extends HttpServlet {
	private static final long serialVersionUID = 6964476879015971966L;
	public static final String BUNDLE_DIRECTORY = "bundles";
	public static final int BUFF_SIZE = 1024;
	private static final String[] BINDEX_ARGS = new String[]{"-n", "IGB", "-q", "-r", "", ""};

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
		doPost(request, response);
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {
		if (!ServletFileUpload.isMultipartContent(request)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "not a valid file");
			return;
		}
		List<FileItem> items = null;
		try {
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			items = upload.parseRequest(request);
		}
		catch (FileUploadException x) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());
			return;
		}
		// Process the uploaded items
		String jarDirectory = getServletContext().getRealPath("/" + BUNDLE_DIRECTORY + "/");
		for (FileItem item : items) {
			if (item.isFormField()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "form file cannot be uploaded");
				return;
			} else {
			    String fileName = item.getName();
			    if (!fileName.endsWith(".jar")) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "not a jar file");
					return;
			    }
				InputStream uploadedStream = item.getInputStream();
				OutputStream out = new FileOutputStream(jarDirectory + "/" + fileName);
				byte[] buf = new byte[BUFF_SIZE];
				int count = 0;
				while ((count = uploadedStream.read(buf)) >= 0) {
					out.write(buf, 0, count);
				}
				uploadedStream.close();
			    out.flush();
			    out.close();
			}
		}
		try {
			BINDEX_ARGS[4] = getServletContext().getRealPath("/repository.xml");
			BINDEX_ARGS[5] = jarDirectory;
			Index.main(BINDEX_ARGS);
		}
		catch (Exception x) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "rebuild repository failed " + x.getMessage());
			return;
		}
	    response.sendRedirect( response.encodeRedirectURL("done.html") );
//		request.getRequestDispatcher("done.html").forward(request, response);
	}
}