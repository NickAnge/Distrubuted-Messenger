import org.omg.CORBA.TIMEOUT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class GroupManager {
    private static String MultiCastAddress = "230.0.0.0";
    private static int MultiCastPort = 4321;


    public static void main(String[] args) {
        MulticastSocket MainThreadSocket;

        try {
            MainThreadSocket = new MulticastSocket(MultiCastPort);
            InetAddress group = InetAddress.getByName(MultiCastAddress);
            MainThreadSocket.joinGroup(group);
            byte[] msg = new byte[1024];
            DatagramPacket packet = new DatagramPacket(msg,msg.length);
            MainThreadSocket.receive(packet);

            String msg2 = new String(packet.getData(), packet.getOffset(), packet.getLength());

            System.out.println("Receive a new App:"+ msg2 + " length "+ packet.getLength() + packet.getAddress());


            Socket TcpApp = new Socket(packet.getAddress(), Integer.parseInt(msg2));

            System.out.println("Socket Group port" + TcpApp.getPort() + TcpApp.getLocalPort());
            String input = "Ante gamhsou";
            PrintWriter out  = new PrintWriter(TcpApp.getOutputStream(),true);
            out.println(input);
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(TcpApp.getInputStream()));
            String data = in.readLine();
            System.out.println("\r\nMessage from " + data);

            Thread.sleep(10000);




        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }



    }
}
