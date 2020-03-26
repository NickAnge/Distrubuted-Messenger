import org.omg.CORBA.TIMEOUT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class GroupManager {
    private static String MultiCastAddress = "230.0.0.0";
    private static int MultiCastPort = 4321;


    public static void main(String[] args) {
        MulticastSocket MainThreadSocket;

        try {

            System.out.println("Waiting For new Apps...");
            MainThreadSocket = new MulticastSocket(MultiCastPort);
            InetAddress group = InetAddress.getByName(MultiCastAddress);
            MainThreadSocket.joinGroup(group);
            byte[] msg = new byte[1024];
            DatagramPacket packet = new DatagramPacket(msg,msg.length);
            MainThreadSocket.receive(packet);
            System.out.println("New app request connection...");

            ServerSocket Tcp = new ServerSocket(0);
            String msg1 = new String(""+ Tcp.getLocalPort());
            byte[] bytemsg = msg1.getBytes();
            System.out.println("Sennding the Tcp_info,port" +Tcp.getLocalPort());
            DatagramSocket UdpSocket = new DatagramSocket();
            DatagramPacket packet2 = new DatagramPacket(bytemsg,bytemsg.length,packet.getAddress(),packet.getPort());

            UdpSocket.send(packet2);

            Socket AppCommunicationInfo = Tcp.accept();

            System.out.println("App accepted the communication");
            System.out.println("Adress "+ AppCommunicationInfo.getInetAddress().getHostAddress() + "Port " + AppCommunicationInfo.getPort());
            Tcp.close();
            UdpSocket.close();


//            System.out.println("Receive a new App:"+ msg2 + " length "+ packet.getLength() + packet.getAddress());


//            Socket TcpApp = new Socket(packet.getAddress(), Integer.parseInt(msg2));

//            System.out.println("Socket Group port" + TcpApp.getPort() + TcpApp.getLocalPort());
            String input = "FromGroupManager";
            PrintWriter out  = new PrintWriter(AppCommunicationInfo.getOutputStream(),true);
            out.println(input);
            out.flush();
//
            BufferedReader in = new BufferedReader(new InputStreamReader(AppCommunicationInfo.getInputStream()));
            String data = in.readLine();
            System.out.println("\r\nMessage from " + data);

            Thread.sleep(10000);




        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }



    }
}
