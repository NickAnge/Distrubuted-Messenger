import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Middleware implements IApi{
    Socket CommunicationChannel;

    @Override
    public int grp_join(String grpName, String myId) {

        discoverGroupManager();

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
        MulticastSocket MainThreadSocket;
        try {

            ServerSocket Tcp = new ServerSocket(0);
            System.out.println("Received message For Group" + " "+Tcp.getLocalPort() + " "+ Tcp.getInetAddress());
            MainThreadSocket = new MulticastSocket(MultiCastPort);
            MainThreadSocket.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(MultiCastGroupAddress);
            MainThreadSocket.joinGroup(group);
            String message = new String("" +Tcp.getLocalPort());

            byte[] msg = message.getBytes();

            DatagramPacket packet = new DatagramPacket(msg,msg.length,group,MultiCastPort);
            MainThreadSocket.send(packet);

            System.out.println("Sending packet");
            System.out.println("Wait Group Manager to Answer");

            CommunicationChannel = Tcp.accept();
            String clientAddress = CommunicationChannel.getInetAddress().getHostAddress();
            System.out.println("\r\nNew connection from " + CommunicationChannel.getPort());

            Tcp.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(CommunicationChannel.getInputStream()));

            String data = in.readLine();
            System.out.println("\r\nMessage from " + clientAddress + ": " + data);

            String input = "From Server";
            PrintWriter out  = new PrintWriter(CommunicationChannel.getOutputStream(),true);
//            PrintWriter out = new PrintWriter(Tcp.getOutputStream(), true);
            out.println(input);
            out.flush();
            Thread.sleep(10000);
//            CommunicationChannel.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}

class GroupInfo {
    String groupName;
    List<EachMemberInfo> Members;

    public GroupInfo(String groupName) {
        this.groupName = groupName;
        Members = new ArrayList<EachMemberInfo>();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<EachMemberInfo> getMembers() {
        return Members;
    }
}

class EachMemberInfo {
    String Name;
    InetAddress MemberAddress;
    int MemberPort;

    public EachMemberInfo(String name, InetAddress memberAddress, int memberPort) {
        Name = name;
        MemberAddress = memberAddress;
        MemberPort = memberPort;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public InetAddress getMemberAddress() {
        return MemberAddress;
    }

    public void setMemberAddress(InetAddress memberAddress) {
        MemberAddress = memberAddress;
    }

    public int getMemberPort() {
        return MemberPort;
    }

    public void setMemberPort(int memberPort) {
        MemberPort = memberPort;
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