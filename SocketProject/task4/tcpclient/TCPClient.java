package tcpclient;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    private static int BUFFERSIZE = 1024;
    private DynamicByteArray allbytes = new DynamicByteArray();

    boolean shutdown;
    Integer timeout; //milliseconds
    //Integer limit; 

    int fromServerLength = 0;
    int currentBufferSize = 0;
    Integer byteLimit; 
    
    public TCPClient(boolean shutdown, Integer timeout, Integer limit) { 

        //allbytes = new DynamicByteArray();
        this.shutdown = shutdown;
        this.timeout = timeout;
        //this.limit = limit;
        this.byteLimit = limit;

    }


    /**
    *   TASK1
    *   @param AskServer creates Socket-Connection
    *   Socket writes OutputStream recieved from TCPAsk
    *   To get InputStream from Server the InputStream is read through 
    *   iterations of 1024 bytes (BUFFERSIZE) and then stored in 
    *   the Dynamic Byte Array. fromServerLength gives position of actual bytes 
    *   in tempByteStorage.
    *   Process repeats until fromServerLength == -1 meaning that there
    *   are no more bytes to collect from the InputStream.
    * 
    *   TASK2
    *   @param shutdown if true then the outputstream from client to server is shutdown
    *   which lets the server know that client wont send any more information and is now currently
    *   just waiting for inputStream data from server.  
    *   @param timeout is a millisecond timer that once reached will terminate 
    *   the connection.
    *   @param Limit once reached then currentBufferSize = 0 meaning that the tempByteStorage
    *   will be unable to recieve (read) any more data and connection can thus close.
    */
    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {

        try (Socket clientSocket = new Socket(hostname, port)) {
            clientSocket.getOutputStream().write(toServerBytes, 0, toServerBytes.length);
            if(shutdown == true) {
                clientSocket.shutdownOutput();
            } 
            if(timeout != null) {
                clientSocket.setSoTimeout(timeout);
            }

            while (true) 
            {    
            byte[] tempByteStorage = new byte[determineBufferSize()];
                try {
                    fromServerLength = clientSocket.getInputStream().read(tempByteStorage);
                    if(closeConnection() == true) {  
                        break;
                    }
                    allbytes.insertArrayRange(tempByteStorage, fromServerLength);
                } 
                catch(SocketTimeoutException e) {
                    System.out.println("SocketException: " + e);
                    String NotFound = "HTTP/1.1 404 Not Found\r\n\r\n";
                    return NotFound.getBytes(StandardCharsets.UTF_8);
                    
                }
            }

            clientSocket.close();
            return allbytes.getByteArray();
        } 
        catch(UnknownHostException e) {
            System.out.println("UnknownHostException: " + hostname);
            String NotFound = "HTTP/1.1 404 Not Found\r\n\r\n";
            return NotFound.getBytes(StandardCharsets.UTF_8);
        }
        catch(BindException b) {
            System.out.println("BindException: " + b);
            String NotFound = "HTTP/1.1 404 Not Found\r\n\r\n";
            return NotFound.getBytes(StandardCharsets.UTF_8);
        }
        
    }

    private boolean closeConnection() {

        if(fromServerLength == -1) {
            return true;
        }
        if(currentBufferSize == 0) { 
            return true;
        }
        return false;
    }

    private int determineBufferSize() {
        
        if(byteLimit != null && byteLimit < BUFFERSIZE) {
            currentBufferSize = byteLimit;
            byteLimit = 0;
        } 
        else if(byteLimit == null) {
            currentBufferSize = BUFFERSIZE; 
        } 
        else {
            currentBufferSize = BUFFERSIZE; 
            byteLimit = byteLimit - BUFFERSIZE;
        }
        return currentBufferSize;
    }



    /**
    *   @param DynamicByteArray is a simple design to work as final array that 
    *   is returned to TCPAsk. A problem seems to arise where arrays get filled with '0's
    *   this is solved through skimArray(). 
    *   This is instead of using the imported "ByteArrayOutputStream"
    */
    public static class DynamicByteArray {

        byte[] mainarray;
        int pos;
        int length;

        public DynamicByteArray() {

            length = 64;
            mainarray = new byte[length];
        }

        public void add(byte object) {

            mainarray[pos] = object;
            pos++;
            if((pos % length) == 0) {
                arrayextend();
            }
        }

        public byte[] getByteArray() { 

            skimArray();
            return mainarray; 
        }

        public void insertArrayRange(byte[] array, int endpoint) {

                for(int i = 0; i < endpoint; i++)
                {
                    add(array[i]);
                }
        }

        private void skimArray() {
            
            int p;
            boolean zeroExist = false;
            for(p = 0; p < mainarray.length; p++)
            {
                if(mainarray[p] == 0) {
                    zeroExist = true;
                    break;
                }
            }
            if(zeroExist) {
                byte[] copy = new byte[p];
                for(int i = 0; i < p; i++)
                {
                    copy[i] = mainarray[i];
                }
                mainarray = copy;
                length = p;
            }
        }

        private void arrayextend() {

            byte[] copy = new byte[length * 2];
            for(int i = 0; i < length; i++)
            {
                copy[i] = mainarray[i];
            }
            mainarray = copy;
            length = length * 2;
        }

        public void printArray() {

            for(int i = 0; i < mainarray.length; i++)
            {
                    System.out.println(mainarray[i]);
            }
        }
    }
}
