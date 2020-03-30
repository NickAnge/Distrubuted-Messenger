import java.io.*;
import java.net.*;
import java.security.acl.Group;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Middleware implements IApi{
    GroupManagerInfo InfoManager;
    DatagramSocket Discovery;
    String OursAddress;
    int checkValue;
    int OurPort;
    HashMap<Integer,GroupInfo> middlewareTeamsBuffer;
    List<Message> receiveBuffer;
    List<Message> sendBuffer;
    Thread middlewareThread;
    int teams ;
    public  final Object lock;

    HashMap<Integer,GroupInfo> Groups;

    int gSock;
    public Middleware() {

        Groups = new HashMap<Integer,GroupInfo>();
        InfoManager = new GroupManagerInfo(null);
        receiveBuffer = new ArrayList<Message>();
        discoverGroupManager();
        gSock =0;
        teams = 0;
        lock = new Object();
        middlewareTeamsBuffer = new HashMap<Integer, GroupInfo>();
        middlewareThread = new Thread(new MiddlewareJob());
        middlewareThread.start();
    }

    @Override
    public int grp_join(String grpName, String myId,Message firstView) {
        try {
            String myInformation = new String(grpName +" " + myId + " "+ OursAddress +" " + OurPort);
            System.out.println("Trying to send My info" + InfoManager.getCommunicationSock());
            sendMsgFromSocket(InfoManager.getCommunicationSock(),myInformation);
//            gSock++;

            while(true){
                if(gSock != 0){
                    break;
                }
                synchronized (lock) {
                    lock.wait();
                }
            }
            System.out.println("JOIN" + gSock);
            firstView = new Message("Add",middlewareTeamsBuffer.get(gSock));
            System.out.println(middlewareTeamsBuffer.get(gSock).getGroupName());

            int returValue = gSock;
            gSock = 0;
            return returValue;
        } catch (Exception  e ) {
            e.printStackTrace();

        }

        return gSock;
    }
    @Override
    public int grp_leave(int gSock) {

        String msg = "Leave";

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


    public Object getViewFromSocket(Socket socket)   {

        GroupInfo group = null;
        try {
//            socket.setSoTimeout(500);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Middleware receiving new VIEW ");
            Message newView = (Message) in.readObject();
            group = newView.getView();
        } catch (IOException | ClassNotFoundException e) {
//            return null;
        }

        return  group;
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
                checkValue =0;
                return ;
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
        checkValue =1;
        return ;
    }

    class MiddlewareJob extends Thread {


        @Override
        public void run() {
            while(true){
                    System.out.println("CONINU");
                    GroupInfo newGroup2 = (GroupInfo) getViewFromSocket(InfoManager.getCommunicationSock());
//                    Set<Integer> keys = middlewareTeamsBuffer.keySet();
                    teams = newGroup2.getId();
                    if(!middlewareTeamsBuffer.containsKey(teams)){
                        gSock = teams;
                    }
                    middlewareTeamsBuffer.put(teams,newGroup2);


                    System.out.println("NEW VIEW STO MIDDLEWARE BUFFER");
//                    teams = middlewareTeamsBuffer.size();
                    System.out.println("PAW NA PROSTHRESW TO NEO TEAM"+ teams + middlewareTeamsBuffer.get(teams).getGroupName());

                    Set<Integer> keys = middlewareTeamsBuffer.keySet();
                    for(Integer req: keys){
                        System.out.println(middlewareTeamsBuffer.get(req).getGroupName());
                        System.out.println(middlewareTeamsBuffer.get(req).getId());
                        for(int i = 0; i <middlewareTeamsBuffer.get(req).getMembers().size();i++){
                            System.out.println(middlewareTeamsBuffer.get(req).getMembers().get(i).getName());
                        }
                    }
                    System.out.println("MPHKA STO MIDDLE");

                    synchronized (lock){
                        lock.notify();
                    }
            }

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
}