import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;
import java.net.*;
import java.util.Iterator;

public class GroupThreads {
    public static GroupManager groupManager;
    public static Thread NewApps;
    public static Thread ReceiveErrorsFromClients;
    public static final Object lock = new Object();

    public static void main(String[] args) {
        groupManager = new GroupManager();
        NewApps = new Thread(new RecieveNewApps(groupManager));

        NewApps.start();

        while (true) {
            System.out.println("Checking if message came");
            if (groupManager.getActiveMembers().size() > 0) {
                int tim = groupManager.getActiveMembers().size();
                for (int counter = 0; counter < tim; counter++) {
                    String MsgRequest = groupManager.getMsgFromSocket(groupManager.getActiveMembers().get(counter));

                    if (MsgRequest == null) {
                        System.out.println("This App disconnected so we close it");
                        groupManager.removeFromAllGroups(groupManager.getActiveMembers().get(counter));
                        groupManager.getActiveMembers().remove(groupManager.getActiveMembers().get(counter));
                        groupManager.printList(groupManager.getListOfGroupsIntoManager());

                        if (groupManager.getActiveMembers().size() != tim) {
                            break;
                        }
                        continue;

                    } else if (MsgRequest.equals("NoMessage")) {
                        continue;
                    }
                    String[] MsgSplit = MsgRequest.split(" ", 2);
                    System.out.println(MsgSplit[0]);
                    if (MsgSplit[0].equals("Leave")) {
                        int idGroup = Integer.parseInt(MsgSplit[1]);

                        GroupInfo group = groupManager.findMembersGroup(idGroup);

                        EachMemberInfo member = null;
                        for (int i = 0; i < group.getMembers().size(); i++) {
                            if (group.getMembers().get(i).getAppSocket()
                                    .equals(groupManager.getActiveMembers().get(counter))) {
                                member = group.getMembers().get(i);
                            }
                        }
                        if (member == null) {
                            continue;
                        }
                        String leaveMsg = new String("This member left the Group: " + member.getName());
                        // TSEKARW AN EINAI COORDINATOR AUTOS POU FEugei GIA NA TO ALAKSW
                        if (group.getCoInfo().getCoMember().getMemberPort() == member.getMemberPort()) {
                            group.getMembers().remove(member);
                            group = groupManager.changeCoordinator(group);
                        } else {
                            group.getMembers().remove(member);
                            // Message newView = new Message("Leave",group,leaveMsg);
                            // groupManager.informTheGroup(group, newView, member.getName());
                            // break;
                        }
                        // String msg =
                        // groupManager.getMsgFromSocket(groupManager.getActiveMembers().get(counter));
                        // if(msg == null){
                        // groupManager.getActiveMembers().remove(groupManager.getActiveMembers().get(counter));
                        // }
                        if (group.getMembers().size() == 0) {
                            groupManager.getListOfGroupsIntoManager().remove(group);
                            break;
                        } else {
                            Message newView = new Message("Leave", group, leaveMsg);
                            groupManager.informTheGroup(group, newView, member.getName());
                            break;
                        }
                    } else {
                        System.out.println(MsgRequest);
                        groupManager.addMemberToGroups(groupManager.getActiveMembers().get(counter), MsgRequest);
                    }
                }
                groupManager.printList(groupManager.getListOfGroupsIntoManager());
            }
            if (groupManager.getActiveMembers().size() == 0) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
        }
    }

    static class RecieveNewApps extends Thread {

        MulticastSocket MainThreadSocket;
        public String MultiCastAddress = "230.0.0.0";
        public int MultiCastPort = 4321;
        GroupManager groupManager;

        public RecieveNewApps(GroupManager Manager) {
            this.groupManager = Manager;
        }

        @Override
        public void run() {
            try {
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
                    DatagramPacket packet2 = new DatagramPacket(bytemsg, bytemsg.length, packet.getAddress(),
                            packet.getPort());

                    UdpSocket.send(packet2);

                    Tcp.setSoTimeout(1000);
                    try {
                        Socket AppCommunicationInfo = Tcp.accept();
                        System.out.println("App accepted the communication");
                        // System.out.println("Adress " +
                        // AppCommunicationInfo.getInetAddress().getHostAddress() + "Port " +
                        // AppCommunicationInfo.getPort());
                        groupManager.getActiveMembers().add(AppCommunicationInfo);

                        synchronized (lock) {
                            lock.notify();
                        }
                    } catch (SocketTimeoutException ex) {
                        System.out.println("Propably App didnt receive the packet ... connection Lost");
                        continue;
                    }
                }
            } catch (IOException ex) {

                ex.printStackTrace();
            }
        }
    }
}
