package com.authorwjf;
 
import android.content.Context;
import android.util.Log;

import com.authorwjf.NanoHTTPD;
import com.authorwjf.ServerRunner;
 
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.util.InetAddressUtils;


public class DebugServer extends NanoHTTPD {
	
	//static keyword means all instances of the same class share the same variable
	private static float my_x = 0;
	private static float my_y = 0;
	private static float my_z = 0;
	private static String my_status = "Waiting for value  ";
	private static boolean value_added_successfully = false;
	private static CommentsDataSource datasource;
	private boolean show_all = false;
	
	static SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMM dd, yyyy  'at' HH:mm:ss z");
	private static String startDateandTime = sdf.format(new Date());
	private static String currentDateandTime = sdf.format(new Date());
	
	private static String my_ipv4 = getIPAddress(true);
	private static String my_ipv6 = getIPAddress(false);
	List<Comment> values;
	
    public DebugServer(Context context) {
        super(8080);
        datasource = new CommentsDataSource(context);
        datasource.open();
        values = datasource.getAllComments(20);
        
    }
    
    public static void changeValues(float x, float y, float z, String status)
    {
    	my_x = x;
    	my_y = y;
    	my_z = z;
    	my_status = status;
    	currentDateandTime = sdf.format(new Date());
    }
 
    public static void main(String[] args) {
        ServerRunner.run(DebugServer.class);
    }
 
    @Override public Response serve(IHTTPSession session) {
        Map<String, List<String>> decodedQueryParameters =
            decodeParameters(session.getQueryParameterString());
       
       /*
        * Serve properly depending on request type: we expect 
        * devicename=value2&status=value3
        * 
        * 
        * 
        * 
        */
        show_all = false;
        value_added_successfully = false;
        StringBuilder sb = new StringBuilder();
        if(decodedQueryParameters.get("devicename")!=null)
        {
        	
        	String client_ip = "Unknown";
        	if(session.getHeaders().get("remote-addr")!=null)
        		client_ip = session.getHeaders().get("remote-addr");
        	Log.w("httppost", decodedQueryParameters.get("devicename").get(0) );
        	//Log.w("httppost", decodedQueryParameters.get("status").get(0) );
        	currentDateandTime = sdf.format(new Date());
        	
        	
        	if(decodedQueryParameters.get("s")!=null)
        	{	
	        	for(int j = 0 ; j < decodedQueryParameters.get("s").size(); j++)
	        	{	
	        		String tempstatus = "";
	        		if(decodedQueryParameters.get("s").get(j).startsWith("0")) tempstatus = "unknown";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("1")) tempstatus = "sitting";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("2")) tempstatus = "standing";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("3")) tempstatus = "small motion";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("4")) tempstatus = "walking";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("5")) tempstatus = "running";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("6")) tempstatus = "fall";
	        		else if(decodedQueryParameters.get("s").get(j).startsWith("7")) tempstatus = "jumping";    		
	        		else tempstatus = decodedQueryParameters.get("s").get(j);
	            	datasource.createComment(decodedQueryParameters.get("devicename").get(0), tempstatus, client_ip, currentDateandTime);	
	            	value_added_successfully = true;	            	
	        	}
        	}else{
        		datasource.createComment(decodedQueryParameters.get("devicename").get(0),  decodedQueryParameters.get("status").get(0), client_ip, currentDateandTime);	
            	value_added_successfully = true;        		
        	}
            sb.append("ok");
            
        }
        else
        {
        	if(decodedQueryParameters.get("item")!=null)
            {
            	if(decodedQueryParameters.get("item").get(0).toString().equalsIgnoreCase("all")) show_all = true;   	
            }
        	
        	
        	
	        sb.append("<html>");
	        sb.append("<head><meta http-equiv=\"refresh\" content=\"6\" ><title>TWL Motion Detection Database</title></head>");
	        sb.append("<body style=\"background-color:#ffefde; font-family: Arial, Helvetica, sans-serif; font-size:14px; color:black\">");
	        sb.append("<span style=\"font-family: Arial, Helvetica, sans-serif; font-size:14px; color:black\"><h1><font face=\"arial\" color=\"#312b26\">TWL Motion Detection Database</font></h1>");
	 
	   //     sb.append("<p><blockquote><b>URI</b> = ").append(
	   //         String.valueOf(session.getUri())).append("<br />");
	 
	   //     sb.append("<b>Method</b> = ").append(
	   //         String.valueOf(session.getMethod())).append("</blockquote></p>");
	        
	     //   sb.append("<b>_____________________________________________________  </b>").append("</blockquote></p>");
	        
	        
	        sb.append("<br><h3><font face=\"arial\" color=\"tomato\"> Motion Information </font></h3>   ");
	        sb.append("<div style=\"background-color:burlywood; text-align:center; font-family: Arial, Helvetica, sans-serif; font-size:18px; color:floralwhite; margin-left:auto;  margin-right:auto; padding:20px;\">");
	        
	        
	        
	        
	        if(show_all)sb.append("<br><button style=\"font-size:20px; width:250;height:40\" onClick=\"window.location.href='?item=less'\">Show Less</button>");
	        else sb.append("<br><button style=\"font-size:20px; width:250; height:40\" onClick=\"window.location.href='?item=all'\">Show All </button>");
	            
	        sb.append("<table border=\"1\" style=\" background-color:#312b26; color:lightcoral; margin:20px; padding:20px; width:95%\">")
	        .append("<caption style=\"font-size:28px; color: crimson\"><b>Historical List of Motion Detection</b> </caption>")
	        .append("<tr style=\"background-color:#993B2B; font-size:20px; color: gold\">")
	        .append("<th>Record ID</th>")
	        .append("<th>Device Name</th>") 
	        .append("<th>Status</th>")
	        .append("<th>Client IP</th>")
	        .append("<th>Timestamp</th>")
	        .append("</tr>");
	        
	        
             if(show_all==true) values = datasource.getAllComments(0);        
             else values = datasource.getAllComments(20);
             
	         int size = values.size();
	        
	         for(int i=0; i<size; i++)
	         {
	        	 //sit stand walk run fall  ::::> colors
	        	 //#fef2f2 #f9cccc #F4a6a6 #F08080 #ff3300
	        	 
	        	 String temp_color = "#fef2f2";
	        	 Comment temp = values.get(i);
	        	 if(temp.getStatus().equalsIgnoreCase("sitting"))temp_color = "#fef2f2";
	        	 else if(temp.getStatus().equalsIgnoreCase("standing"))temp_color = "#f9cccc";
	        	 else if(temp.getStatus().equalsIgnoreCase("small motion"))temp_color = "#f2bdbd";
	        	 else if(temp.getStatus().equalsIgnoreCase("walking"))temp_color = "#F4a6a6";
	        	 else if(temp.getStatus().equalsIgnoreCase("running"))temp_color = "#F08080";
	        	 else if(temp.getStatus().equalsIgnoreCase("fall"))temp_color = "#df0000";
	        	 else if(temp.getStatus().equalsIgnoreCase("jumping"))temp_color = "#f04040";
	        	 else if(temp.getStatus().equalsIgnoreCase("unknown"))temp_color = "#7f7f7f";
	        	 sb.append("<tr style=\"color:").append(temp_color).append("\">")
	             .append("<td>"+temp.getId()+ "</td>")
	             .append("<td>"+temp.getDeviceName()+ "</td>")
	             .append("<td>"+temp.getStatus()+ "</td>")
	             .append("<td>"+temp.getClientIp()+ "</td>")
	             .append("<td>"+temp.getDateTime()+ "</td>")
	             .append("</tr>");
	        	 
	         }
	         
	        sb.append("</table>");
	        

	        
	        if(show_all)sb.append("<br><button style=\"font-size:20px;width:250;height:40\" onClick=\"window.location.href='?item=less'\">Show Less</button>");
	        else sb.append("<br><button style=\"font-size:20px;width:250;height:40\" onClick=\"window.location.href='?item=all'\">Show All </button>");
	        sb.append("</div>");
	        sb.append("<br><h3><font face=\"arial\" color=\"tomato\"> Server IP Addresses</font></h3>   ");
	        sb.append("<div style=\"background-color:burlywood; color:black; font-family: Arial, Helvetica, sans-serif; font-size:12px; margin-left:auto;  margin-right:auto; padding:20px;\">");
	        sb.append("<b>   IPv4</b> = ").append(my_ipv4)
	        .append("<br><b>   IPv6</b> = ").append(my_ipv6).append("</b><br>");
	        sb.append("<br>Server running since ").append(startDateandTime).append("</blockquote></p>")            ;                                 
	        sb.append("</div><br>");
	
	     
	        sb.append("<h3><font face=\"arial\" color=\"tomato\">Local Values for Debugging</h3>   ");
	        sb.append("<div style=\"background-color:burlywood; color:black; font-family: Arial, Helvetica, sans-serif; font-size:12px; margin-left:auto;  margin-right:auto; padding:20px;\">");
		      
	        sb.append("<h3>Latest Acceleration Information</h3>   ").append("<b>   X-axis</b> = ").append(my_x).append("</blockquote></p>")
	                                                         .append("<b>   Y-axis</b> = ").append(my_y).append("</blockquote></p>")
	                                                         .append("<b>   Z-axis</b> = ").append(my_z).append("</blockquote></p>")
	                                                         .append("<b>   Significant Direction</b> = ").append(my_status).append("</blockquote></p>")
	                                                         .append("<b>   Timestamp</b> = ").append(currentDateandTime).append("</blockquote></p>")
	                                                         .append(" <b>______________________________________________ </b> ").append("</blockquote></p>")
	                                                         ;
	       
	
	        
	        sb.append("<h3>Headers</h3><p><blockquote>").
	            append(toString(session.getHeaders())).append("</blockquote></p>");
	 
	        sb.append("<h3>Parms</h3><p><blockquote>").
	            append(toString(session.getParms())).append("</blockquote></p>");
	 
	        sb.append("<h3>Parms (multi values?)</h3><p><blockquote>").
	            append(toString(decodedQueryParameters)).append("</blockquote></p>");
	 
	        try {
	            Map<String, String> files = new HashMap<String, String>();
	            session.parseBody(files);
	            sb.append("<h3>Files</h3><p><blockquote>").
	                append(toString(files)).append("</blockquote></p>");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        sb.append("</div>");
	        sb.append("</span></body>");
	        sb.append("</html>");
	        
      }
        return new Response(sb.toString());
    }
 
    private String toString(Map<String, ? extends Object> map) {
        if (map.size() == 0) {
            return "";
        }
        return unsortedList(map);
    }
 
    private String unsortedList(Map<String, ? extends Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (Map.Entry entry : map.entrySet()) {
            listItem(sb, entry);
        }
        sb.append("</ul>");
        return sb.toString();
    }
 
    private void listItem(StringBuilder sb, Map.Entry entry) {
        sb.append("<li><code><b>").append(entry.getKey()).
            append("</b> = ").append(entry.getValue()).append("</code></li>");
    }
    
    
    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                        if (useIPv4) {
                            if (isIPv4) 
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                              // int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                             //   return delim<0 ? sAddr : sAddr.substring(0, delim);
                            	return sAddr;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
    
    
}