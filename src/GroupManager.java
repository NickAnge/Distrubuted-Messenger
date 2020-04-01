
import java.io.*;
import java.net.*;
import java.util.*;

public class GroupManager {
    public String MultiCastAddress = "230.0.0.0";
    public int MultiCastPort = 4321;


    //    private HashMap<Integer,GroupInfo> ListOfGroupsIntoManager;
    private List<GroupInfo> ListOfGroupsIntoManager;
    private int numberOfGroups;
    private List<Socket> activeMembers;

    public List<Socket> getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(List<Socket> activeMembers) {
        activeMembers = activeMembers;
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public List<GroupInfo> getListOfGroupsIntoManager() {
        return ListOfGroupsIntoManager;
    }

    public void setListOfGroupsIntoManager(List<GroupInfo> listOfGroupsIntoManager) {
        ListOfGroupsIntoManager = listOfGroupsIntoManager;
    }

    public GroupManager() {
        ListOfGroupsIntoManager = new ArrayList<GroupInfo>();
        numberOfGroups = 0;
        activeMembers = new ArrayList<Socket>();
    }
//    public static void main(String[] args) {
//        MulticastSocket MainThreadSocket;
//        GroupManager groupManager = new GroupManager();
//
//        NewApps = new Thread(new RecieveNewApps());
////        ReceiveErrorsFromClients = new Thread(new Recieve)
//
//
//        try {
//            while(true){
//                System.out.println("Waiting For new Apps...");
//                MainThreadSocket = new MulticastSocket(MultiCastPort);
//                InetAddress group = InetAddress.getByName(MultiCastAddress);
//                MainThreadSocket.joinGroup(group);
//                byte[] msg = new byte[1024];
//                DatagramPacket packet = new DatagramPacket(msg,msg.length);
//                MainThreadSocket.setSoTimeout(1000);
//                try{
//                    ServerSocket Tcp = new ServerSocket(0);
//                    MainThreadSocket.receive(packet);
//                    System.out.println("New app request connection...");
//                    String msg1 = new String(packet.getAddress()+ " "+ Tcp.getLocalPort());
//                    byte[] bytemsg = msg1.getBytes();
//                    System.out.println("Sennding the Tcp_info,port" +Tcp.getLocalPort());
//                    DatagramSocket UdpSocket = new DatagramSocket();
//                    DatagramPacket packet2 = new DatagramPacket(bytemsg,bytemsg.length,packet.getAddress(),packet.getPort());
//
//                    UdpSocket.send(packet2);
//
//                    Socket AppCommunicationInfo = Tcp.accept();
//                    System.out.println("App accepted the communication");
//                    System.out.println("Adress "+ AppCommunicationInfo.getInetAddress().getHostAddress() + "Port " + AppCommunicationInfo.getPort());
////                    System.out.println("Port of communication with other members1" + packet2.getPort());
//
//                    String MsgRequest = groupManager.getMsgFromSocket(AppCommunicationInfo);
//
//                    if(MsgRequest != null){
//                        groupManager.addMemberToGroups(AppCommunicationInfo,MsgRequest);
//                    }
//                    else {
//                        groupManager.getNoTeamYet().add(AppCommunicationInfo);
//                    }
//                    System.out.println("No info about app came, checking the storage");
//                    UdpSocket.close();
//                }
//                catch (SocketTimeoutException ex){
//                    System.out.println("Didn't come new App -->> going to check for other Messages");
//
//                }
//
//                if(groupManager.getNoTeamYet().size() != 0){
//                    for(int counter = 0;counter < groupManager.getNoTeamYet().size();counter++){
////                    Message MsgRequest = (Message) groupManager.getNewMessageFromSocket(groupManager.getNoTeamYet().get(counter));
//
//                        String MsgRequest = groupManager.getMsgFromSocket(groupManager.getNoTeamYet().get(counter));
//                        if(MsgRequest == null){
//                            continue;
//                        }
//                        else if(MsgRequest.equals("Leave")){
//                            groupManager.getNoTeamYet().remove(counter);
//
//                        }
//                        else {
//                            groupManager.addMemberToGroups(groupManager.getNoTeamYet().get(counter),MsgRequest);
//                            groupManager.getNoTeamYet().remove(counter);
//                        }
//                    }
//                }
//
//                Iterator<GroupInfo> it = groupManager.getListOfGroupsIntoManager().iterator();
//
//                int tim = groupManager.getListOfGroupsIntoManager().size();
//                for(int j = 0; j<tim ; j++) {
//                    GroupInfo temp = groupManager.getListOfGroupsIntoManager().get(j);
//                    for (int i = 0; i < temp.getMembers().size(); i++) {
//                        String data = groupManager.getMsgFromSocket(temp.getMembers().get(i).getAppSocket());
//                        if (data == null) {
//                            continue;
//                        } else if (data.equals("Leave")) {
//                            EachMemberInfo member = temp.getMembers().get(i);
//                            temp.getMembers().remove(member);
//                            if(member.getAppSocket().isConnected())
//                            groupManager.getNoTeamYet().add(member.getAppSocket());
//                            if (temp.getMembers().size() == 0) {
//                                groupManager.ListOfGroupsIntoManager.remove(temp);
//                                break;
//                            } else {
//                                Message newView = new Message("GroupView", temp);
//                                groupManager.informTheGroup(temp, newView, member.getName());
//                            }
//                        } else {
//                            System.out.println(temp.getMembers().get(i).getName() + "Want to be added into new Group");
//                            groupManager.addMemberToGroups(temp.getMembers().get(i).getAppSocket(), data);
//                        }
//                    }
//                }
//                groupManager.printList(groupManager.ListOfGroupsIntoManager);
//
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }




    public  String getMsgFromSocket(Socket socket){
        String data = "NoMessage";

        try {
            socket.setSoTimeout(500);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            data = in.readLine();
            System.out.println("\r\nMessage from " + socket.getInetAddress().getHostAddress() + ": " + data);
        } catch (SocketTimeoutException e) {
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
    public   void sendMsgFromSocket(Socket socket,String msg)  {
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

    public  void sendNewMessageToSocket(Socket socket,Object object){
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

    public Object getNewMessageFromSocket(Socket socket){
        ObjectInputStream in = null;
        try {
            socket.setSoTimeout(500);
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Middleware receiving new VIEW ");
            GroupInfo newVIew = (GroupInfo) in.readObject();
            return  newVIew;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public  void addMemberToGroups(Socket socket ,String Member){

        String []splitMsg = Member.split(" ",4);
        EachMemberInfo NewMember = new EachMemberInfo(splitMsg[1],socket,splitMsg[2],Integer.parseInt(splitMsg[3]));
        int flag = 0;
        System.out.println("Info of the new member sended...");


//        Set<Integer> gSocket  = this.ListOfGroupsIntoManager.keySet();
        Iterator<GroupInfo> it = this.ListOfGroupsIntoManager.iterator();
        while(it.hasNext()){
            GroupInfo temp = it.next();
            if (temp.getGroupName().equals(splitMsg[0])) {
                temp.getMembers().add(NewMember);
                flag = 0;
                System.out.println("send GroupView");
                Message newView = new Message("Add",temp);
                this.informTheGroup(temp,newView,NewMember.getName());
//                Message mesg = new Message("Add",temp);
                sendNewMessageToSocket(NewMember.getAppSocket(),newView);
                break;
            } else {
                flag = 1;
            }
        }
        //In case, this Group does not exist.
        if(flag == 1 || ListOfGroupsIntoManager.isEmpty()){
            flag = 0;
            System.out.println("NEW GROUPPPPPP");
            int gsock =getNumberOfGroups();
            System.out.println("GSOCK:"+ gsock);
            setNumberOfGroups(++gsock);
            GroupInfo NewGroup =  new GroupInfo(splitMsg[0],getNumberOfGroups());

            NewGroup.getMembers().add(NewMember);

            ListOfGroupsIntoManager.add(NewGroup);
            Message mesg = new Message("Add",NewGroup);

//            this.ListOfGroupsIntoManager.put(temp,NewGroup);//Prosthikh kainourgias omadas sthn apothikh
            sendNewMessageToSocket(socket,mesg);//apostolh new View
        }

    }

    public void printList(List<GroupInfo> groups){

        for(int i = 0; i <groups.size();i++){
            System.out.println("GroupName: " + groups.get(i).getGroupName());
            System.out.println("ID : " + groups.get(i).getId());
            for(int j =0 ;j <groups.get(i).getMembers().size();j++){
                System.out.println("MemberName: " +groups.get(i).getMembers().get(j).getName());
                System.out.println("MemberAddress: "+ groups.get(i).getMembers().get(j).getMemberAddress());
                System.out.println("MemberPort: "+ groups.get(i).getMembers().get(j).getMemberPort());
            }

        }
    }
    public  void informTheGroup(GroupInfo group,Message newView,String NoSend){

        for(int i = 0; i < group.getMembers().size();i++){
            if(group.getMembers().get(i).getName().equals(NoSend)){
                continue;
            }
            System.out.println("Sends new VIew to Members");
            this.sendNewMessageToSocket(group.getMembers().get(i).getAppSocket(),newView);
        }
    }

    public GroupInfo findMembersGroup(int idOfGroup){
        Iterator<GroupInfo> it = ListOfGroupsIntoManager.iterator();

        while(it.hasNext()){
            GroupInfo temp = it.next();

            if(temp.getId() == idOfGroup){
                return temp;
            }
        }
        return null;
    }

    public  void removeFromAllGroups(Socket specificSocket){
        System.out.println("MPHKA mesa sto remove all");

        Iterator<GroupInfo> it = ListOfGroupsIntoManager.iterator();
        int tim2 = ListOfGroupsIntoManager.size();

        for(int j =0;j<tim2;j++){
            GroupInfo temp = ListOfGroupsIntoManager.get(j);
            if(temp == null){
                break;
            }
            int tim = temp.getMembers().size();
            for(int i =0;i<tim;i++){
                EachMemberInfo member = temp.getMembers().get(i);
                if(member.getAppSocket().equals(specificSocket)){
                    temp.getMembers().remove(member);
                    System.out.println("MPHKA mesa sto remove all");

                    if(temp.getMembers().size() == 0){
                        ListOfGroupsIntoManager.remove(temp);
                        break;
                    }
                    Message view = new Message("Error",temp);
                    informTheGroup(temp,view,member.getName());
                    break;
                }
            }
            if(ListOfGroupsIntoManager.size() < tim2) {
                tim2--;
                j--;
            }
        }



        return;
    }
}


