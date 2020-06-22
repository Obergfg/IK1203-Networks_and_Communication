import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

public class HTTPEcho {

    private byte[] toClientBuffer;
    private String nextString;
    private int contentLength;
    private ServerSocket serverSocket;
    private Socket socket;
    private StringBuilder clientString;
    private BufferedReader in;
    private PrintWriter out;

    public static void main(String[] args) {
        try {
            int portNumber = Integer.parseInt(args[0]);

            if (0 > portNumber || 65535 < portNumber)
                throw new NumberFormatException();

            HTTPEcho server = new HTTPEcho();
            server.createServer(portNumber);

        } catch (NumberFormatException e) {
            System.out.println("The value entered was not an integer between 0 and 65535");
        }
    }

    private void createServer(int portNumber) {
        try {
            this.serverSocket = new ServerSocket(portNumber);
            
            createSocket();

        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    private void createSocket() {
        try {
            while (true) {
                this.socket = this.serverSocket.accept();

                readFromClient();
                writeToClient();

                if (this.clientString.toString().equals("QUITTHEPROGRAM"))
                    break;
            }

            this.serverSocket.close();

        } catch (IOException e) {
            System.out.println(e);
            clientErrorMessage();
            createSocket();
        }
    }

    private void readFromClient() {
        try {
            this.clientString = new StringBuilder();
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            this.nextString = this.in.readLine();
            this.clientString.append(this.nextString);

            while (this.in.ready() && (this.nextString = this.in.readLine()) != "")
                this.clientString.append("\r\n" + this.nextString);

        } catch (IOException e) {
            System.out.println(e);
            clientErrorMessage();
            createSocket();
        }
    }

    private void writeToClient() {
        try {
            this.out = new PrintWriter(this.socket.getOutputStream());
            this.toClientBuffer = this.clientString.toString().getBytes(StandardCharsets.UTF_8);
            this.contentLength = this.toClientBuffer.length;

            this.out.println("HTTP/1.1 200 OK");
            this.out.println("Content-Length: " + this.contentLength);
            this.out.println("Content-Type: text/plain");
            this.out.println("Connection: Closed");
            this.out.println("");
            this.out.println(this.clientString.toString());
            this.out.flush();

            this.socket.close();

        } catch (IOException e) {
            System.out.println(e);
            clientErrorMessage();
            createSocket();
        }
    }

    private void clientErrorMessage() {
        try {
            this.out = new PrintWriter(this.socket.getOutputStream());
            this.out.println("HTTP/1.1 500 Internal Server Error");
            this.out.println("");
            this.out.flush();

            this.socket.close();

        } catch (IOException e) {
            System.out.println(e);
        }
    }
}