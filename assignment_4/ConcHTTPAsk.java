import java.io.IOException;
import java.net.*;

public class ConcHTTPAsk {

    private ServerSocket serverSocket;

    public static void main(String[] args) {

        try {

            int portNumber;

            if (0 < args.length) {
                portNumber = Integer.parseInt(args[0]);

                if (0 > portNumber || 65535 < portNumber)
                    throw new NumberFormatException();
            } else
                portNumber = 1234;

            ConcHTTPAsk concHTTPAsk = new ConcHTTPAsk();

            concHTTPAsk.createServer(portNumber);

        } catch (NumberFormatException e) {
            System.out.println("The value entered was not an integer between 0 and 65535");
        }
    }

    private void createServer(int portNumber) {

        try {
            this.serverSocket = new ServerSocket(portNumber);
            Socket socket = this.serverSocket.accept();

            while (true) {

                (new Thread(new HTTPAsk(socket))).start();

                socket = this.serverSocket.accept();

            }
        } catch (IOException e) {
            System.out.println("Could not create server");
        }
    }
}