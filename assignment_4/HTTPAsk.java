import java.net.*;
import java.io.*;
import tcpclient.TCPClient;
import java.nio.charset.StandardCharsets;

/**
 * Is a HTTP ask server where the client can ask the server to send a request to
 * a hos at a given URL.
 */
public class HTTPAsk implements Runnable {

    private Socket askSocket;


    /**
     * Empty main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        
    }

    /**
     * Creates a HTTPAsk object
     * 
     * @param askSocket is the socket of the HTTPAsk server.
     */
    HTTPAsk(Socket askSocket) throws SocketException {
        this.askSocket = askSocket;
        this.askSocket.setSoTimeout(2000);
    }

    /**
     * Is called upon from a threads start method when a connection has been made between the socket and a client.
     */
    public void run() {

        try {
            readFromClient();
        } catch (IllegalArgumentException e) {
            System.out.println("Client entered an invalid URL");
            clientErrorMessage(400);
        }
    }

    /**
     * Reads and manages the data sent from the client.
     * 
     * @throws IllegalArgumentException if some arguments has been inceorrectly
     *                                  entered from the client.
     */
    private void readFromClient() throws IllegalArgumentException {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(this.askSocket.getInputStream()));

            String GETRequest = in.readLine();

            if (null == GETRequest)
                throw new IllegalArgumentException();

            String[] requestArray = GETRequest.split(" ");

            if (3 > requestArray.length)
                throw new IllegalArgumentException();

            String[] ask = requestArray[1].split("\\?");

            if (2 > ask.length)
                throw new IllegalArgumentException();

            String[] ampersand = ask[1].split("&");
            String[] hostName = ampersand[0].split("\\=");
            String[] port = ampersand[1].split("\\=");
            String[] message = null;

            if (2 < ampersand.length)
                message = ampersand[2].split("\\=");

            validateRequest(requestArray, ask[0], hostName, port, message);

        } catch (IOException e) {
            System.out.println("Could not read from client");
            clientErrorMessage(500);
        }
    }

    /**
     * Validates the request from the client
     * 
     * @param requestArray contains the three fields of the GET-message.
     * @param ask          is the ask part of the URL.
     * @param hostName     is the hostname part of the URL.
     * @param port         is the port number part of the URL.
     * @param message      is the message the client wants to send to the host of
     *                     interest.
     */
    private void validateRequest(String[] requestArray, String ask, String[] hostName, String[] port, String[] message) {

        if (requestArray[0].equals("GET") && 
            ask.equals("/ask") && 
            hostName[0].equals("hostname") && 
            port[0].equals("port") && 
            (requestArray[2].equals("HTTP/1.0") || requestArray[2].equals("HTTP/1.1"))) {

            try {
                int portNumber = Integer.parseInt(port[1]);
                callTCPClient(portNumber, hostName[1], message);

            } catch (NumberFormatException e) {
                System.out.println("Client portnumber error");
                clientErrorMessage(400);
            }

        } else {
            System.out.println("Invalid client request");
            clientErrorMessage(400);
        }
    }

    /**
     * Calls the TCP client to handle the request from the HTTP client.
     * 
     * @param portNumber is th eportnumber of the page of interest.
     * @param hostName   is the URL adress of the host of interest.
     * @param message    is the message being sent to the hos of interest - null if
     *                   not existent.
     * @throws IllegalArgumentException if some arguments where intered incorrectly
     *                                  from the client.
     */
    private void callTCPClient(int portNumber, String hostName, String[] message) throws IllegalArgumentException{

        String clientResponse;

        try {

            if (null == message)
                clientResponse = TCPClient.askServer(hostName, portNumber);
            else if (message[0].equals("string"))
                clientResponse = TCPClient.askServer(hostName, portNumber, message[1]);
            else
                throw new IllegalArgumentException();
           
            writeToClient(clientResponse);

        } catch (IOException e) {
            System.out.println("Host not found");
            writeToClient("Could not connect to the host: " + hostName);  
        }
    }

    /**
     * Sends a response message to the client if the URL was entered correctly.
     * 
     * @param clientResponse
     */
    private void writeToClient(String clientResponse) {
        try {

            String decodedString = decodeString(clientResponse);
            byte[] toClientBuffer;
            int contentLength;

            PrintWriter out = new PrintWriter(this.askSocket.getOutputStream());

            toClientBuffer = decodedString.getBytes();
            contentLength = toClientBuffer.length;

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Length: " + contentLength);
            out.println("Content-Type: text/plain; charset=utf-8");
            out.println("Connection: Closed");
            out.println("");
            out.println(decodedString);
            out.flush();

            this.askSocket.close();

        } catch (IOException e) {
            System.out.println("Could not write to client");
            clientErrorMessage(400);
        }
    }

    /**
     * Sends an error return message to the client if som error has occured.
     * 
     * @param errorCode is the error code.
     */
    private void clientErrorMessage(int errorCode) {

        String statusMessage;

        if (400 == errorCode)
            statusMessage = " Bad Request";
        else if (404 == errorCode)
            statusMessage = " Not Found";
        else
            statusMessage = " Internal Server Error";

        try {
            PrintWriter out = new PrintWriter(this.askSocket.getOutputStream());

            out.println("HTTP/1.1 " + errorCode + statusMessage);
            out.println("");
            out.flush();

            this.askSocket.close();

        } catch (IOException e) {
            System.out.println("Could not send error message to client");
        }
    }

    /**
     * Decodes a string entered in the browsers URL
     * 
     * @param encoded is the encoded string from th eURL
     * @return String is the decoded string - if it could be decoded.
     */
    private String decodeString(String encoded) {

        try {
            return URLDecoder.decode(encoded, StandardCharsets.ISO_8859_1.toString());
        } catch (UnsupportedEncodingException e) {
            System.out.println("Could not decode string");
            return encoded;
        }
    }
}
