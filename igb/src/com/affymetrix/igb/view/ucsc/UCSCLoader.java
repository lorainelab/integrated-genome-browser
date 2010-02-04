package com.affymetrix.igb.view.ucsc;

import com.affymetrix.genoviz.util.GeneralUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Returns location image string from the UCSC genome browser.
 * 
 * 
 * @author Ido M. Tamir
 */
public class UCSCLoader {
   private static final Pattern fileNamePattern = Pattern.compile("(hgt_genome.*gif)");
   

   /**
	*
	* @param url the UCSC genome/region url
	* @param userId the UCSC userId (hguid cookie value)
	* @return url of the image of the region
	*/
   public String getImageUrl(String url, String userId) {
    URL request_url = null;
    HttpURLConnection request_con = null; 
    InputStream input_stream = null;
    BufferedReader in = null;
    try {
        request_url = new URL(url);
        request_con = (HttpURLConnection) request_url.openConnection();
		request_con.setConnectTimeout(120*1000); //2min
        request_con.setUseCaches(false);
        request_con.addRequestProperty("Cookie", "hguid=" + userId);
        input_stream = request_con.getInputStream();
        in = new BufferedReader(new InputStreamReader(input_stream));
        String inputLine = "";
        while ((inputLine = in.readLine()) != null){
             Matcher m = fileNamePattern.matcher(inputLine);
             if(m.find() && m.groupCount() == 1){
                 Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "found fileName " + inputLine);
                 String fileName = m.group(1);
                 return "http://genome.ucsc.edu/trash/hgt/" + fileName;
             }
        }
	}catch(SocketException e){
		Logger.getLogger(UCSCLoader.class.getName()).log(Level.SEVERE, null, e);
		return("Error: the UCSC Browser was not able to return the answer in the appropriate time");
    }catch (IOException e) {
        Logger.getLogger(UCSCLoader.class.getName()).log(Level.SEVERE, null, e);    
    }
    finally {
		GeneralUtils.safeClose(input_stream);
		GeneralUtils.safeClose(in);
        if( request_con != null) try { request_con.disconnect(); }catch(Exception e){}
    } 
    return "Error: Could not find image in " + url;
  }


   
   
}
