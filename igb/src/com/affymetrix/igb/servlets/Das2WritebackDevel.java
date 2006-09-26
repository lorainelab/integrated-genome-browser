package com.affymetrix.igb.servlets;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.xml.sax.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.parsers.*;

public class Das2WritebackDevel extends HttpServlet  {

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static AnnotatedSeqGroup group = gmodel.addSeqGroup("writeback_test_group");
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    String path_info = request.getPathInfo();
    String query = request.getQueryString();
    System.out.println("Das2WritebackDevel received POST request: ");
    System.out.println("   path: " + path_info);
    System.out.println("   query: " + query);

    InputStream istr = request.getInputStream();
    /*
    BufferedReader reader = new BufferedReader(new InputStreamReader(istr));
    String line;
    while ((line = reader.readLine()) != null)  {
        System.out.println(line);
    }
    */

   try  {
       BufferedInputStream bis = new BufferedInputStream(istr);
       Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
       parser.parse(new InputSource(bis), "writeback_test", group, false);
   }
   catch (Exception ex)  {
       ex.printStackTrace();
   }
  }


}
