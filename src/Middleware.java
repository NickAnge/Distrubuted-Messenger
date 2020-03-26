import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Middleware implements IApi{
    Socket CommunicationChannel;
    List<GroupInfo> Groups ;

    public Middleware() {
        Groups = new ArrayList<GroupInfo>();
    }

    @Override
    public int grp_join(String grpName, String myId) {

        discoverGroupManager();


        try {
//            System.out.println(InetAddress.getLocalHost().);
            String myInformation = new String(grpName +" " + myId + " "+ InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return 0;
    }
    @Override
    public int grp_leave(int gSock) {
        return 0;
    }

    @Override
    public int grp_send(int gSock, String msg, int len, int total) {
        return 0;
    }

    @Override
    public int grp_recv(int gSock, int type, String msg, int len, int block) {
        return 0;
    }


    public void  discoverGroupManager() {
        DatagramSocket Discovery;
        DatagramPacket packet = null;
        try {
            System.out.println("Trying to discover Group Manager...");

            Discovery = new DatagramSocket();
            int i = 0;
            while(i!=4){
                byte[] msg =  new byte[1024];
                packet = new DatagramPacket(msg,msg.length, InetAddress.getByName(MultiCastGroupAddress),MultiCastPort);
                Discovery.send(packet);
                System.out.println("Sending packet");
                System.out.println("Wait Group Manager to Answer");
                Discovery.setSoTimeout(500);
                try {
                    Discovery.receive(packet);
                    break;
                }catch (SocketTimeoutException ex){
                    System.out.println("Trying again..");
                    i++;
                }
            }
            if(i == 4){
                return;
            }
            String msg1 = new String(packet.getData(), packet.getOffset(), packet.getLength());
            System.out.println("Discovery was successful"+ msg1);

            CommunicationChannel = new Socket(packet.getAddress(),Integer.parseInt(msg1));

            String clientAddress = CommunicationChannel.getInetAddress().getHostAddress();

            System.out.println("We will communicate with Group Manager at Address +" +clientAddress + "Port:"+ CommunicationChannel.getPort());

            BufferedReader in = new BufferedReader(new InputStreamReader(CommunicationChannel.getInputStream()));
            Thread.sleep(10000);

            String data = in.readLine();
            System.out.println("\r\nMessage from " + clientAddress + ": " + data);

            Thread.sleep(10000);
            String input = "From Server";
            PrintWriter out  = new PrintWriter(CommunicationChannel.getOutputStream(),true);

            out.println(input);
            out.flush();
            Thread.sleep(10000);
//            CommunicationChannel.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class GroupManagerInfo{
    InetAddress ManagerAddress;
    int ManagerPort;

    public GroupManagerInfo(InetAddress managerAddress, int managerPort) {
        ManagerAddress = managerAddress;
        ManagerPort = managerPort;
    }

    public InetAddress getManagerAddress() {
        return ManagerAddress;
    }

    public void setManagerAddress(InetAddress managerAddress) {
        ManagerAddress = managerAddress;
    }

    public int getManagerPort() {
        return ManagerPort;
    }

    public void setManagerPort(int managerPort) {
        ManagerPort = managerPort;
    }
}