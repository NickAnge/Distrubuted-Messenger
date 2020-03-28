
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
                System.out.println("Port of communication with other members1" + packet2.getPort());
//                Tcp.close();
                UdpSocket.close();


                BufferedReader in = new BufferedReader(new InputStreamReader(AppCommunicationInfo.getInputStream()));
                String MsgRequest = in.readLine();
//                String MsgRequest = getMsgFromSocket(AppCommunicationInfo);
                String []splitMsg = MsgRequest.split(" ",4);
                EachMemberInfo NewMember = new EachMemberInfo(splitMsg[1],AppCommunicationInfo,splitMsg[2],Integer.parseInt(splitMsg[3]));

                int flag = 0;
                System.out.println("Info of the new member sended...");
                Set<Integer> gSocket  = groupManager.getListOfGroupsIntoManager().keySet();
                for (Integer req:gSocket) {
                    if (groupManager.getListOfGroupsIntoManager().get(req).getGroupName().equals(splitMsg[0])) {
                        groupManager.getListOfGroupsIntoManager().get(req).getMembers().add(NewMember);
                        flag = 0;
                        for(int counter =0; counter <groupManager.getListOfGroupsIntoManager().size(); counter++){
                            if(groupManager.getListOfGroupsIntoManager().get(req).getMembers().get(counter).getName().equals(NewMember.getName())){
                                continue;
                            }
                            else{
                                sendNewViewFromSocket(groupManager.getListOfGroupsIntoManager().get(req).getMembers().get(counter).getAppSocket(),groupManager.getListOfGroupsIntoManager().get(req));
                            }
                            //Enhmerwse tous upoloipous
                        }
                        sendNewViewFromSocket(AppCommunicationInfo,groupManager.getListOfGroupsIntoManager().get(req));
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
                    groupManager.getListOfGroupsIntoManager().put(temp,NewGroup);//Prosthikh kainourgias omadas sthn apothikh
                    sendNewViewFromSocket(AppCommunicationInfo,groupManager.getListOfGroupsIntoManager().get(temp));//apostolh new View
                }
                //Elegxos mhpws mou hrthan mhnumata
                Set<Integer> keys = groupManager.getListOfGroupsIntoManager().keySet();

                for(Integer req : keys){
                    int times = groupManager.getListOfGroupsIntoManager().get(req).getMembers().size();

                    for(int i = 0; i <times; i++){
                        String data = getMsgFromSocket(groupManager.getListOfGroupsIntoManager().get(req).getMembers().get(i).getAppSocket());
                        if(data == null){
                            continue;
                        }
                        if(data.equals("IWantToLeave")){
                            groupManager.getListOfGroupsIntoManager().get(times).getMembers().remove(i);
                            if(groupManager.getListOfGroupsIntoManager().get(req).getMembers().size() == 0){
                                break;
                            }
                            else {
                                //enhmerwse tous upoloipous
                            }
                        }
                    }
//                    if(groupManager.getListOfGroupsIntoManager().get(req).)
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
            socket.setSoTimeout(500);

//            socket.setSoTimeout(500);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            data = in.readLine();
            System.out.println("\r\nMessage from " + socket.getInetAddress().getHostAddress() + ": " + data);
//            socket.setSoTimeout(0);
        } catch (SocketTimeoutException e) {
            return null;
//            e.printStackTrace();
        } catch (IOException e) {
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

    public static void sendNewViewFromSocket(Socket socket,Object object){
        ObjectInputStream in = null;
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Trying to send Message Group");
            out.writeObject(object);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ;
    }
}
