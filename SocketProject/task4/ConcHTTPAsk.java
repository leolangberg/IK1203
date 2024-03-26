import java.net.*;
import java.io.*;
import tcpclient.TCPClient;
import java.nio.charset.StandardCharsets;

public class ConcHTTPAsk {


    static int BUFFERSIZE = 1024;
    static int port = 3333;

    public static void main( String[] args) throws Exception {
        // Your code here
        /**
        *   1. RECIEVE INPUT STRING
        *   2. PARSE STRING TO GET HOSTNAME PORT ETC
        *   3. SENT NEW INFO TO TCPCLIENT
        *   4. RETURN RESPONSE FROM TCPCLIENT
        *
         */

        String test = "GET /ask?hostname=localhost&port=10003&string=The_time_is_Tue_Mar__5_21-36-36_2024&shutdown=true HTTP/1.1\n" +
                    "Host: localhost:8891";
        port = Integer.parseInt(args[0]);

        try(ServerSocket welcomeSocket = new ServerSocket(port)) {
            
            //System.out.println("SERVER START: " + port + " (port)");
            
            while(true) 
            {
                try {
        
                  Runnable operation = new MyRunnable(welcomeSocket.accept());
                  new Thread(operation).start();

               } catch (IOException e) { 
               
                  System.err.println(e);
               }
            }
            
        } catch(IOException e) {

            System.err.println(e);
			System.exit(1);
        }
    }


   private static class MyRunnable implements Runnable {
 
      Socket connectionSocket;

      public MyRunnable(Socket connectionSocket) { this.connectionSocket = connectionSocket; }

      public void run() {

	    //System.out.println("Thread is Running: " + Thread.currentThread().getName()); 
    
        try {
            byte[] HTTPmessage = new byte[BUFFERSIZE];
            
            int fromClientLength = connectionSocket.getInputStream().read(HTTPmessage);
            String clientSentence = new String(HTTPmessage, 0, fromClientLength, StandardCharsets.UTF_8);
            //System.out.println(clientSentence);
            netNode data = parser(clientSentence);
            //System.out.println("hostname: " + data.hostname + "  port: " + data.port +  "  shutdown: " + data.shutdown + "  limit: " + data.limit + "  timeout: " + data.timeout + "  valid: " + data.valid + " userInputBytes: " + new String(data.userInputBytes));
            

            if(data.valid == true)
            {
                TCPClient tcpClient = new TCPClient(data.shutdown, data.limit, data.timeout);
                byte[] clientreturn = tcpClient.askServer(data.hostname, data.port, data.userInputBytes);

                connectionSocket.getOutputStream().write(OKresponseMsg(clientreturn));
                //System.out.println("TCPCLIENT: " + new String(clientreturn));
            }
            else {
                String BadRequest = "HTTP/1.1 400 Bad Request\r\n\r\n";
                connectionSocket.getOutputStream().write(BadRequest.getBytes());
            }
            
            connectionSocket.close();
            //System.out.println("Thread complete: " + Thread.currentThread().getName());

            } catch(IOException e) {
            
                System.err.println(e);
                return;
            }    
        }


        private static class netNode {

           String hostname = null;
           Integer port = 0;
           byte[] userInputBytes = new byte[0];

           boolean shutdown = false;
           Integer limit = null;
           Integer timeout = null;

           boolean valid = false; //badrequest
           //String responseMsg = null;

           public netNode() {}
        }



        private static netNode parser(String clientSentence) {

           netNode data = new netNode();
           String[] lines = clientSentence.split("\n");
           String[] askmsg = lines[0].split(" "); //will be same as askmsg
 
           if(askmsg[0].equals("GET") && askmsg[2].trim().equals("HTTP/1.1")) {  //validate GET + HTTP/1.1
               //System.out.println("GET FOUND");
               data = HTTPGETParse(lines[0], data);
               data.valid = true;
           } else {
               //System.out.println("GET NOT FOUND");
               data.valid = false;
           }
           //System.out.println("hostname: " + data.hostname + "  port: " + data.port +  "  shutdown: " + data.shutdown + "  limit: " + data.limit + "  timeout: " + data.timeout + " userInputBytes: " + new String(data.userInputBytes));
           return data;
        }

        private static netNode HTTPGETParse(String getline, netNode data) {
           String[] askmsg = getline.split(" ");
           String[] args = askmsg[1].split("&");
        
           for(int i = 0; i < args.length; i++) {
               if(args[i].startsWith("/ask?hostname")) { // /ask?hostname=nost.gov --> nost.gov
                   data.hostname = args[i].substring(14);
               }
               if(args[i].startsWith("shutdown")) {
                   if(args[i].substring(9).equals("true")) {  //shutdown=true --> true
                       data.shutdown = true;
                   } 
               } 
               if(args[i].startsWith("limit")) {
                   data.limit = Integer.parseInt(args[i].substring(6)); //limit=1200 --> 1200
               }
               if(args[i].startsWith("timeout")) {
                   data.timeout = Integer.parseInt(args[i].substring(8)); //timeout=300 --> 300
               }
               if(args[i].startsWith("port")) {
                   data.port = Integer.parseInt(args[i].substring(5)); //port=13 --> 13
               }
               if(args[i].startsWith("string")) { //string=kth.se --> kth.se
                   data.userInputBytes = args[i].substring(7).getBytes(StandardCharsets.UTF_8);
               }
           }
           return data;
        }

        private static byte[] OKresponseMsg(byte[] tcpResponse) {

          String ok = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + tcpResponse.length + "\r\n\r\n" + new String(tcpResponse, StandardCharsets.UTF_8);
          return ok.getBytes(StandardCharsets.UTF_8);
        }




  }



    
}

