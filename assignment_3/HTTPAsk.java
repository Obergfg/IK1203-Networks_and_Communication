import java.net.*;
import java.io.*;
import tcpclient.TCPClient;
import java.nio.charset.StandardCharsets;

/**
 * Is a HTTP ask server where the client can ask the server to send a request to
 * a hos at a given URL.
 */
public class HTTPAsk {

    private ServerSocket serverSocket;
    private Socket socket = null;

    /**
     * Initates the program.
     * 
     * @param args contains the servers portnumber if entered.
     */
    public static void main(String[] args) {
        try {

            int portNumber;

            if (0 < args.length) {
                portNumber = Integer.parseInt(args[0]);

                if (0 > portNumber || 65535 < portNumber)
                    throw new NumberFormatException();
            } else
                portNumber = 1234;

            HTTPAsk server = new HTTPAsk();
            server.createServer(portNumber);

        } catch (NumberFormatException e) {
            System.out.println("The value entered was not an integer between 0 and 65535");
        }
    }

    /**
     * Creates a server socket.
     * 
     * @param portNumber is the servers port number.
     */
    private void createServer(int portNumber) {
        try {

            this.serverSocket = new ServerSocket(portNumber);
            createSocket();

        } catch (IOException e) {
            System.out.println("Could not create server socket");
            System.exit(0);
        }
    }

    /**
     * Creates a socket and starts listening for client requests.
     */
    private void createSocket() {
        try {
            while (true) {
                this.socket = this.serverSocket.accept();

                try {
                    readFromClient();

                } catch (IllegalArgumentException e) {
                    // System.out.println("Client entered an invalid URL");
                    clientErrorMessage(400);
                }

                this.socket.close();
            }

        } catch (IOException e) {
            System.out.println("Could not create socket");
            clientErrorMessage(500);
        }
    }

    /**
     * Reads and manages the data sent from the client.
     * 
     * @throws IllegalArgumentException if some arguments has been incorrectly
     *                                  entered from the client.
     */
    private void readFromClient() throws IllegalArgumentException {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

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
            // System.out.println("Could not read from client");
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
    private void validateRequest(String[] requestArray, String ask, String[] hostName, String[] port,
            String[] message) {

        if (requestArray[0].equals("GET") && ask.equals("/ask") && hostName[0].equals("hostname")
                && port[0].equals("port")
                && (requestArray[2].equals("HTTP/1.0") || requestArray[2].equals("HTTP/1.1"))) {

            try {
                int portNumber = Integer.parseInt(port[1]);
                callTCPClient(portNumber, hostName[1], message);

            } catch (NumberFormatException e) {
                System.out.println("Client portnumber error");
                clientErrorMessage(400);
            }

        } else {
            // System.out.println("Invalid client request");
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
    private void callTCPClient(int portNumber, String hostName, String[] message) throws IllegalArgumentException {

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
            // System.out.println("Host not found");
            writeToClient("404 Not Found: " + hostName);
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

            PrintWriter out = new PrintWriter(this.socket.getOutputStream());

            toClientBuffer = decodedString.getBytes();
            contentLength = toClientBuffer.length;

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Length: " + contentLength);
            out.println("Content-Type: text/plain; charset=utf-8");
            out.println("Connection: Closed");
            out.println("");
            out.println(decodedString);
            out.flush();

        } catch (IOException e) {
            // System.out.println("Could not write to client");
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
            PrintWriter out = new PrintWriter(this.socket.getOutputStream());

            out.println("HTTP/1.1 " + errorCode + statusMessage);
            out.println("");
            out.flush();

        } catch (IOException e) {
            // System.out.println("Could not send error message to client");
        }
    }

    /**
     * Decodes a string entered in the browsers URL
     * 
     * @param encoded is the encoded string from the URL
     * @return String is the decoded string - if it could be decoded.
     */
    private String decodeString(String encoded) {

        try {
            return URLDecoder.decode(encoded, StandardCharsets.ISO_8859_1.toString());
        } catch (UnsupportedEncodingException e) {
            // System.out.println("Could not decode string");
            return encoded;
        }
    }
}
