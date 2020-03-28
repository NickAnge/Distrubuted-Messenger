import java.io.*;
import java.net.*;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Middleware implements IApi{
    GroupManagerInfo InfoManager;
    DatagramSocket Discovery;
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
            String myInformation = new String(grpName +" " + myId + " "+ OursAddress +" " + OurPort);
            System.out.println("Trying to send My info");
            sendMsgFromSocket(InfoManager.getCommunicationSock(),myInformation);

//
            GroupInfo newGroup = (GroupInfo) this.getViewFromSocket(InfoManager.getCommunicationSock());

            gSock ++;
            Groups.put(gSock,newGroup);


            System.out.println("Group Name: " + newGroup.getGroupName());
            for(int i = 0;i <newGroup.getMembers().size();i++){
                System.out.println("Name: "+newGroup.getMembers().get(i).getName());
                System.out.println("Address: "+ newGroup.getMembers().get(i).getMemberAddress() );
                System.out.println("Port:"+newGroup.getMembers().get(0).getMemberPort());


            }

            Thread.sleep(20000);

            GroupInfo newGroup2 = (GroupInfo) this.getViewFromSocket(InfoManager.getCommunicationSock());

            System.out.println("Group Name: " + newGroup2.getGroupName());
            for(int i = 0;i <newGroup2.getMembers().size();i++){
                System.out.println("Name: "+newGroup2.getMembers().get(i).getName());
                System.out.println("Address: "+ newGroup2.getMembers().get(i).getMemberAddress() );
                System.out.println("Port:"+newGroup2.getMembers().get(0).getMemberPort());


            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();

        }


        return gSock;
    }
    @Override
    public int grp_leave(int gSock) {

        String msg = "IWantToLeave";

        sendMsgFromSocket(InfoManager.getCommunicationSock(),msg);
        Groups.remove(gSock);
        Set <Integer>  Keys = Groups.keySet();

        for(Integer req : Keys){
            System.out.println(Groups.get(req).getGroupName());
        }

        return 1;
    }

    @Override
    public int grp_send(int gSock, String msg, int len, int total) {
        return 0;
    }

    @Override
    public int grp_recv(int gSock, int type, String msg, int len, int block) {
        return 0;
    }


    public Object getViewFromSocket(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        System.out.println("Middleware receiving new VIEW ");
        GroupInfo newVIew = (GroupInfo) in.readObject();

        return  newVIew;
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

        DatagramPacket packet = null;

        try {
            System.out.println("Trying to discover Group Manager...");
            Discovery = new DatagramSocket();
            System.out.println(Discovery.getLocalPort());
            OurPort = Discovery.getLocalPort();
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
//            Thread.sleep(5000);
            Socket CommunicationChannel = new Socket(packet.getAddress(),Integer.parseInt(splitMsg[1]));

            System.out.println("We will communicate with Group Manager at Address +" + CommunicationChannel.getInetAddress().getHostAddress() + "Port:"+ CommunicationChannel.getPort());

            this.InfoManager.setCommunicationSock(CommunicationChannel);

//            Thread.sleep(10000);
        } catch (IOException  e) {
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