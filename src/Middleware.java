import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Middleware implements IApi{
    GroupManagerInfo InfoManager;
    String OursAddress;
    int OurPort;
    HashMap<Integer,GroupInfo> Groups;
    int gSock;

    public Middleware() {

        Groups = new HashMap<Integer,GroupInfo>();
        InfoManager = new GroupManagerInfo(null);
        gSock =0;
    }

    @Override
    public int grp_join(String grpName, String myId) {

        //Discovery of the Group Manager  from Middleware;
        if(this.InfoManager.getCommunicationSock() == null) {
            discoverGroupManager();
        }
        try {
            String myInformation = new String(grpName +" " + myId + " "+ OursAddress +" ");
            System.out.println("Trying to send My info");
            sendMsgFromSocket(InfoManager.getCommunicationSock(),myInformation);

            ObjectInputStream in = new ObjectInputStream(InfoManager.getCommunicationSock().getInputStream());
            System.out.println("Trying to recieve Message Group");
            GroupInfo newVIew = (GroupInfo) in.readObject();
            gSock ++;
            Groups.put(gSock,newVIew);
            System.out.println("Group Name: " + newVIew.getGroupName());
            for(int i = 0;i <newVIew.getMembers().size();i++){
                System.out.println("Name: "+newVIew.getMembers().get(i).getName());
                System.out.println("Address: "+ newVIew.getMembers().get(i).getMemberAddress() );
                System.out.println("Port:"+newVIew.getMembers().get(0).getMemberPort());
            }
            Thread.sleep(30000);

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
//        }}
        }


        return gSock;
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



    public  String getMsgFromSocket(Socket socket){
        String data = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            data = in.readLine();
            System.out.println("\r\nMessage from " + socket.getInetAddress().getHostAddress() + ": " + data);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return data;
    }
    public  void sendMsgFromSocket(Socket socket,String msg)  {
        try {
            PrintWriter out = new PrintWriter(this.InfoManager.getCommunicationSock().getOutputStream(), true);

            out.println(msg);
            out.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return ;
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
                Discovery.setSoTimeout(2000);
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
            String []splitMsg = msg1.split(" ",2);
            System.out.println("Discovery was successful"+ splitMsg[0]+" "+ splitMsg[1]);
            OursAddress =splitMsg[0].replace("/","");
            Thread.sleep(5000);
            Socket CommunicationChannel = new Socket(packet.getAddress(),Integer.parseInt(splitMsg[1]));

            System.out.println("We will communicate with Group Manager at Address +" + CommunicationChannel.getInetAddress().getHostAddress() + "Port:"+ CommunicationChannel.getPort());

            this.InfoManager.setCommunicationSock(CommunicationChannel);

            Thread.sleep(10000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class GroupManagerInfo{
    Socket CommunicationSock;

    public Socket getCommunicationSock() {
        return CommunicationSock;
    }

    public void setCommunicationSock(Socket communicationSock) {
        CommunicationSock = communicationSock;
    }

    public GroupManagerInfo(Socket communicationSock) {
        CommunicationSock = communicationSock;
    }
//    InetAddress ManagerAddress;
//    int ManagerPort;
//
//    public GroupManagerInfo(InetAddress managerAddress, int managerPort) {
//        ManagerAddress = managerAddress;
//        ManagerPort = managerPort;
//    }
//
//    public InetAddress getManagerAddress() {
//        return ManagerAddress;
//    }
//
//    public void setManagerAddress(InetAddress managerAddress) {
//        ManagerAddress = managerAddress;
//    }
//
//    public int getManagerPort() {
//        return ManagerPort;
//    }
//
//    public void setManagerPort(int managerPort) {
//        ManagerPort = managerPort;
//    }

}