import java.io.IOException;
import java.net.*;
import java.util.Iterator;

public class GroupThreads {
    public static  GroupManager groupManager;
    public  static Thread NewApps;
    public static  Thread ReceiveErrorsFromClients;
    public static final Object lock = new Object();

    public static void main(String[] args) {
        groupManager = new GroupManager();
        NewApps = new Thread(new RecieveNewApps(groupManager));
//        ReceiveErrorsFromClients = new Thread(new Recieve)

        NewApps.start();
        while(true) {
            System.out.println("Checking if message came");
            if (groupManager.getNoTeamYet().size() > 0) {
                int tim = groupManager.getNoTeamYet().size();
                for (int counter = 0; counter < tim; counter++) {
//                    Message MsgRequest = (Message) groupManager.getNewMessageFromSocket(groupManager.getNoTeamYet().get(counter));

                    String MsgRequest = groupManager.getMsgFromSocket(groupManager.getNoTeamYet().get(counter));
                    if (MsgRequest == null) {
                        continue;
                    } else if (MsgRequest.equals("Leave")) {
                        groupManager.getNoTeamYet().remove(counter);

                    } else {
                        groupManager.addMemberToGroups(groupManager.getNoTeamYet().get(counter), MsgRequest);
                        groupManager.getNoTeamYet().remove(counter);
                    }
                }
            }
            else if( groupManager.getNoTeamYet().size() == 0 && groupManager.getListOfGroupsIntoManager().size() == 0){
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }

            int tim2 = groupManager.getListOfGroupsIntoManager().size();

            for (int j = 0; j < tim2; j++) {
                GroupInfo temp = groupManager.getListOfGroupsIntoManager().get(j);
                if (temp == null) {
                    break;
                }
                for (int i = 0; i < temp.getMembers().size(); i++) {
                    String data = groupManager.getMsgFromSocket(temp.getMembers().get(i).getAppSocket());
                    if (data == null) {
                        continue;
                    } else if (data.equals("Leave")) {
                        EachMemberInfo member = temp.getMembers().get(i);
                        temp.getMembers().remove(member);
                        if (temp.getMembers().size() == 0) {
                            groupManager.getListOfGroupsIntoManager().remove(temp);
                            break;
                        } else {
                            Message newView = new Message("Leave", temp);
                            groupManager.informTheGroup(temp, newView, member.getName());
                        }
                    } else {
                        System.out.println(temp.getMembers().get(i).getName() + "Want to be added into new Group");
                        groupManager.addMemberToGroups(temp.getMembers().get(i).getAppSocket(), data);
                    }
                }
            }
            groupManager.printList(groupManager.getListOfGroupsIntoManager());
        }
    }

    static class RecieveNewApps extends Thread {

        MulticastSocket MainThreadSocket;
        public  String MultiCastAddress = "230.0.0.0";
        public   int MultiCastPort = 4321;
        GroupManager groupManager;

        public  RecieveNewApps(GroupManager Manager){
            this.groupManager = Manager;
        }

        @Override
        public void run() {
            try{
                ServerSocket Tcp = new ServerSocket(0);

                while (true) {
                    System.out.println("Waiting For new Apps...");
                    MainThreadSocket = new MulticastSocket(MultiCastPort);
                    InetAddress group = InetAddress.getByName(MultiCastAddress);
                    MainThreadSocket.joinGroup(group);
                    byte[] msg = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(msg, msg.length);


                    MainThreadSocket.receive(packet);

                    System.out.println("New app request connection...");
                    String msg1 = new String(packet.getAddress() + " " + Tcp.getLocalPort());
                    byte[] bytemsg = msg1.getBytes();
                    System.out.println("Sennding the Tcp_info,port" + Tcp.getLocalPort());
                    DatagramSocket UdpSocket = new DatagramSocket();
                    DatagramPacket packet2 = new DatagramPacket(bytemsg, bytemsg.length, packet.getAddress(), packet.getPort());

                    UdpSocket.send(packet2);


                    Tcp.setSoTimeout(1000);
                    try{
                        Socket AppCommunicationInfo = Tcp.accept();
                        System.out.println("App accepted the communication");
                        System.out.println("Adress " + AppCommunicationInfo.getInetAddress().getHostAddress() + "Port " + AppCommunicationInfo.getPort());
                        groupManager.getNoTeamYet().add(AppCommunicationInfo);

                        synchronized (lock){
                            lock.notify();
                        }
                    } catch (SocketTimeoutException ex){
                        System.out.println("Propably App didnt receive the packet ... connection Lost");
                        continue;
                    }
                }
            }
            catch (IOException ex){

             ex.printStackTrace();
            }
        }
    }


}
