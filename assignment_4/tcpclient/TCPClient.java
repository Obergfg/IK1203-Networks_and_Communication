package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {

    private static int BUFFERSIZE = 2048;
    private static byte[] fromUserBuffer = new byte[BUFFERSIZE];
    private static byte[] fromServerBuffer = new byte[BUFFERSIZE];
    private static String decodedString;
    private static int fromUserLength;
    private static int fromServerLength;
    private static Socket clientSocket;

    public static String askServer(String hostName, int port, String toServer) throws IOException {

        if (null == toServer)
            return askServer(hostName, port);

        clientSocket = new Socket(hostName, port);

    
        fromUserLength = toServer.length();
        fromUserBuffer = toServer.getBytes();
        clientSocket.getOutputStream().write(fromUserBuffer, 0, fromUserLength);
        clientSocket.getOutputStream().write('\n');
        
        clientSocket.setSoTimeout(2000);
        
        return getServerResponse();
    }

    public static String askServer(String hostname, int port) throws IOException {

        clientSocket = new Socket(hostname, port);

        clientSocket.setSoTimeout(2000);

        return getServerResponse();
    }

    private static String getServerResponse() throws IOException {

        fromServerLength = clientSocket.getInputStream().read(fromServerBuffer);

        if (clientSocket.isConnected() && 0 < fromServerLength) {
            decodedString = new String(fromServerBuffer, 0, fromServerLength);
            clientSocket.close();
            return decodedString;
        } else {
            clientSocket.close();
            throw new ConnectException("There was no response from the connected server!");
        }
    }
}
