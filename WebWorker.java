/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.nio.file.Files;
import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;
String fileType;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();

      //Retrieve User Requested URL for later methods.
      String input;
      input = readHTTPRequest(is);
      if(input.endsWith(".jpg"))
         fileType = "image/jpg";
      else if (input.endsWith(".gif"))
         fileType = "image/gif";
      else if(input.endsWith(".png"))
         fileType = "image/png";
      else
         fileType = "text/html";
      writeHTTPHeader(os,fileType, input);
      writeContent(os, input);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line, address = " ";
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         
         String addressSpot = line.substring(0,3);
         //Determine if the line is GET
         if(addressSpot.equals("GET")){
            //Retrieve the remainder of the string
            address = line.substring(4);
            address = address.substring(0, address.indexOf(" "));
            System.err.println("Requested file is: " +address);
         }

         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return address;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String input) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));

   File x = new File(input);

   if(x.exists() && !x.isDirectory()){
      os.write("HTTP/1.1 200 OK\n".getBytes());
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Jon's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      
   }
   else{
      os.write("HTTP/1.1 404 Not Found\n".getBytes());   
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Jon's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   }
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String input) throws Exception
{
   //Remove file directory "/" from beginning
   input = input.substring(1);
   
 
   File x = new File(input);

   //Determine if file exists at given input
   //If file exists, read file line by line 
   if(x.exists() && !x.isDirectory()){
      FileInputStream stream = new FileInputStream(input);
      BufferedReader r = new BufferedReader(new InputStreamReader(stream));
      
      if(fileType.startsWith("image/"))
         Files.copy(x.toPath(), os);
      else{
      String filex;
      //Reading file
      while ((filex = r.readLine()) != null){
         //Check for <cs371date> tag & replace
         if(filex.equals("<cs371date>")){
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            Date specific = new Date();
            String finalDay = dateFormat.format(specific);
            os.write(finalDay.getBytes()); 
         }
         //Check for <cs371server> tag & replace
         if(filex.equals("<cs371server>")){
            os.write("Farouk's Server.".getBytes()); 
         }
         os.write(filex.getBytes());
      }
      r.close();
      }
   }
   //else if file does not exist, display "404 Error"
   else{
      os.write("<h3>Error: 404 not Found</h3>".getBytes());
   }
}

} // end class
