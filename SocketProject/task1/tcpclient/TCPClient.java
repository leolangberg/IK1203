package tcpclient;
import java.net.*;
import java.io.*;

/**
*   @param AskServer creates Socket-Connection
*   Socket writes OutputStream recieved from TCPAsk
*   To get InputStream from Server the InputStream is read through 
*   iterations of 1024 bytes (BUFFERSIZE) and then stored in 
*   the Dynamic Byte Array. fromServerLength gives position of actual bytes 
*   in tempByteStorage.
*   Process repeats until fromServerLength == -1 meaning that there
*   are no more bytes to collect from the InputStream.
*/
public class TCPClient {

    private static int BUFFERSIZE = 1024;
    private DynamicByteArray allbytes = new DynamicByteArray();
    
    public TCPClient() {

        allbytes = new DynamicByteArray(); //optional

    }

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {

        Socket clientSocket = new Socket(hostname, port);
        clientSocket.getOutputStream().write(toServerBytes, 0, toServerBytes.length);

        int fromServerLength = 0;
        while (true) 
        {
            byte[] tempByteStorage = new byte[BUFFERSIZE];
            fromServerLength = clientSocket.getInputStream().read(tempByteStorage);

            if(fromServerLength == -1) {
                break;
            }
            allbytes.insertArrayRange(tempByteStorage, fromServerLength);
        }

        clientSocket.close();

        return allbytes.getByteArray();
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
