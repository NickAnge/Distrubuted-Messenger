
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GroupManager {
    private static String MultiCastAddress = "230.0.0.0";
    private static int MultiCastPort = 4321;
    private HashMap<Integer,GroupInfo> ListOfGroupsIntoManager;
    private  int numberOfGroups;

    public  int getNumberOfGroups() {
        return numberOfGroups;
    }

    public  void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public HashMap<Integer, GroupInfo> getListOfGroupsIntoManager() {
        return ListOfGroupsIntoManager;
    }

    public void setListOfGroupsIntoManager(HashMap<Integer, GroupInfo> listOfGroupsIntoManager) {
        ListOfGroupsIntoManager = listOfGroupsIntoManager;
    }

    public GroupManager() {
        ListOfGroupsIntoManager = new HashMap<Integer, GroupInfo>();
        numberOfGroups = 0;
    }

    public static void main(String[] args) {
        MulticastSocket MainThreadSocket;
        GroupManager groupManager = new GroupManager();

        try {
            ServerSocket Tcp = new ServerSocket(0);
            while(true){
                System.out.println("Waiting For new Apps...");
                MainThreadSocket = new MulticastSocket(MultiCastPort);
                InetAddress group = InetAddress.getByName(MultiCastAddress);
                MainThreadSocket.joinGroup(group);
                byte[] msg = new byte[1024];
                DatagramPacket packet = new DatagramPacket(msg,msg.length);
                MainThreadSocket.receive(packet);
                System.out.println("New app request connection...");


                String msg1 = new String(packet.getAddress()+ " "+ Tcp.getLocalPort());
                byte[] bytemsg = msg1.getBytes();
                System.out.println("Sennding the Tcp_info,port" +Tcp.getLocalPort());
                DatagramSocket UdpSocket = new DatagramSocket();
                DatagramPacket packet2 = new DatagramPacket(bytemsg,bytemsg.length,packet.getAddress(),packet.getPort());

                UdpSocket.send(packet2);

                Socket AppCommunicationInfo = Tcp.accept();

                System.out.println("App accepted the communication");
                System.out.println("Adress "+ AppCommunicationInfo.getInetAddress().getHostAddress() + "Port " + AppCommunicationInfo.getPort());
//                Tcp.close();
                UdpSocket.close();

                String MsgRequest = getMsgFromSocket(AppCommunicationInfo);
                String []splitMsg = MsgRequest.split(" ",3);
                EachMemberInfo NewMember = new EachMemberInfo(splitMsg[1],AppCommunicationInfo,splitMsg[2],AppCommunicationInfo.getPort());
                int flag = 0;
                System.out.println("Info of the new member sended...");
                Set<Integer> gSocket  = groupManager.getListOfGroupsIntoManager().keySet();
                for (Integer req:gSocket) {
                    if (groupManager.getListOfGroupsIntoManager().get(req).getGroupName().equals(splitMsg[0])) {
                        groupManager.getListOfGroupsIntoManager().get(req).getMembers().add(NewMember);
                        flag = 0;
                        for(int counter =0; counter <groupManager.getListOfGroupsIntoManager().size(); counter++){
                            //Enhmerwse tous upoloipous
                        }
                        ObjectOutputStream out = new ObjectOutputStream(AppCommunicationInfo.getOutputStream());
                        System.out.println("Trying to send Message Group");
                        out.writeObject(groupManager.getListOfGroupsIntoManager().get(req));
                        out.flush();
                        break;
                    } else {
                        flag = 1;
                    }
                }
                //In case, this Group does not exist.
                if(flag == 1 || groupManager.getListOfGroupsIntoManager().isEmpty()){
                    flag = 0;
                    System.out.println("NEW GROUPPPPPP");
                    GroupInfo NewGroup =  new GroupInfo(splitMsg[0]);
                    NewGroup.getMembers().add(NewMember);
                    int temp = groupManager.getNumberOfGroups();
                    temp++;
                    groupManager.setNumberOfGroups(temp);
                    groupManager.getListOfGroupsIntoManager().put(temp,NewGroup);

                    ObjectOutputStream out = new ObjectOutputStream(AppCommunicationInfo.getOutputStream());
                    System.out.println("Trying to send Message Group");
                    out.writeObject(groupManager.getListOfGroupsIntoManager().get(temp));
                    out.flush();

                }
            }



//            System.out.println("Receive a new App:"+ msg2 + " length "+ packet.getLength() + packet.getAddress());


//            Socket TcpApp = new Socket(packet.getAddress(), Integer.parseInt(msg2));

//            System.out.println("Socket Group port" + TcpApp.getPort() + TcpApp.getLocalPort());
//            String input = "FromGroupManager";
//            PrintWriter out  = new PrintWriter(AppCommunicationInfo.getOutputStream(),true);
//            out.println(input);
//            out.flush();


//            BufferedReader in = new BufferedReader(new InputStreamReader(AppCommunicationInfo.getInputStream()));
//            String data = in.readLine();
//            System.out.println("\r\nMessage from " + data);
//
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static String getMsgFromSocket(Socket socket){
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
    public  static void sendMsgFromSocket(Socket socket,String msg)  {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(msg);
            out.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return ;
    }
}
