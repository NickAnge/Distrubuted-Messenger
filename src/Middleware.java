import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.xml.crypto.Data;
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
    HashMap<Integer,GroupMessages> groupMessages;
//    List<UdpMessage> receiveBuffer;
    List<Message> receiveMiddle;
    List<UdpMessage> sendsBuffer;
    List<UdpMessage> resendBuffer;
    List<UdpMessage> whatIsend;
    Thread middlewareThread;
    HashMap<Integer,Message> sendtoAPP;
    int teams ;
    public  final Object lock;
    int seqNumber;
    List<Integer> mids;//exoun paradwthei sto middleware sigoura;
    List<Integer> appids;
    HashMap<Integer,GroupInfo> Groups;
    int receiveMessages;
    int delivered;
    int sended;
    List<Message> mbuf; //FOR FIFO
    List<Integer>mbufids;

    int gSock;
    public Middleware(int seqNumber) {
        Groups = new HashMap<Integer,GroupInfo>();
        InfoManager = new GroupManagerInfo(null);
//        receiveBuffer = new ArrayList<UdpMessage>();
        discoverGroupManager();
        receiveMessages = 0;
        groupMessages = new HashMap<>();
        gSock =0;
        teams = 0;
        delivered =0;
        sended = -1;
        this.seqNumber = seqNumber;
        mbufids = new ArrayList<>();
        sendtoAPP = new HashMap<>();
        mbuf = new ArrayList<>();
        lock = new Object();
        mids = new ArrayList<>();
        appids = new ArrayList<>();
        whatIsend = new ArrayList<>();
        receiveMiddle = new ArrayList<>();
        sendsBuffer = new ArrayList<>();
        resendBuffer = new ArrayList<>();
        middlewareTeamsBuffer = new HashMap<Integer, GroupInfo>();
        middlewareThread = new Thread(new MiddlewareJob());
        middlewareThread.start();
    }

    @Override
    public int grp_join(String grpName, String myId,Message firstView) {
        try {
            String myInformation = new String(grpName +" " + myId + " "+ OursAddress +" " + OurPort);
//            System.out.println("Trying to send My info" + InfoManager.getCommunicationSock());
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
//            System.out.println("JOIN" + gSock);
//            firstView = new Message("Add",middlewareTeamsBuffer.get(gSock));
//            firstView.setType("Add");
            firstView.setView(middlewareTeamsBuffer.get(gSock));
//            System.out.println(middlewareTeamsBuffer.get(gSock).getGroupName());

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

        String msg = "Leave " + gSock;
//        System.out.println(msg);
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

        seqNumber++;
        sended++;
//        System.out.println(msg);
        UdpMessage newMessage = new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort);
        UdpMessage newMessage2= new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort);

        newMessage.setSelfDelivered(sended);
        newMessage2.setSelfDelivered(sended);

        Iterator<EachMemberInfo> it = middlewareTeamsBuffer.get(gSock).getMembers().iterator();

        while(it.hasNext()){
            EachMemberInfo temp = it.next();
            newMessage.getMembersSend().add(temp.getMemberPort());
        }

        whatIsend.add(newMessage);

        BM_send(newMessage,gSock);


        return 0;
    }

    @Override
    public int grp_recv(int gSock, Message receiveMsg, int block) {
        if(groupMessages.containsKey(gSock)){
            if(groupMessages.get(gSock).getMsgs().size() > 0){
                Message nextMsg = groupMessages.get(gSock).getMsgs().get(0);
                groupMessages.get(gSock).getMsgs().remove(0);

                System.out.println("mPHKA EDW" + receiveMsg.getMessage());

                if(nextMsg.getMessage() != null){
                    receiveMsg.getMessage().setSenderPort(nextMsg.getMessage().getSenderPort());
                    receiveMsg.getMessage().setGroupId(nextMsg.getMessage().getGroupId());
                    receiveMsg.getMessage().setMessage(nextMsg.getMessage().getMessage());
                    receiveMsg.getMessage().setSeqNo(nextMsg.getMessage().getSeqNo());
                    receiveMsg.getMessage().setSenderPort(nextMsg.getMessage().getSenderPort());
//                    for(int j =0;j< middlewareTeamsBuffer.get(gSock).getMembers().size();j++){
//                        if(middlewareTeamsBuffer.get(gSock).getMembers().get(j).getMemberPort() == nextMsg.getMessage().getStartingSender()){
//                            receiveMsg.setName(middlewareTeamsBuffer.get(gSock).getMembers().get(j).getName());
//                            break;
//                        }
//                    }
                    receiveMsg.setName(nextMsg.getName());
                    return 1;
                }
                if(nextMsg.getView() != null){
                    receiveMsg.setName(nextMsg.getName());
                    receiveMsg.setType(nextMsg.getType());
                    receiveMsg.setView(nextMsg.getView());
                    receiveMsg.setChangeViewMessage(nextMsg.getChangeViewMessage());

                }
                return 1;
            }
        }
        return 0;
    }


    public Object getViewFromSocket(Socket socket)   {
        Message newView = null;
        try {
            socket.setSoTimeout(500);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//            System.out.println("Middleware receiving new VIEW ");
            newView = (Message) in.readObject();
        }
        catch (SocketTimeoutException ex) {
//            System.out.println("EIMAI EDW");
            Message error = new Message("noMessage",null,(UdpMessage) null);
            return error;

        }
        catch (IOException | ClassNotFoundException e) {
            Message error = new Message("noMessage",null,(UdpMessage) null);
            return null;
        }

        return  newView;
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
                    Message newGroup2 = (Message) getViewFromSocket(InfoManager.getCommunicationSock());
//                    System.out.println(newGroup2);
                    if(!newGroup2.getType().equals("noMessage")){
//                        System.out.println("mesa ");
                        teams = newGroup2.getView().getId();
                        if(!middlewareTeamsBuffer.containsKey(teams)){
                            gSock = teams;
                            GroupMessages group = new GroupMessages();
                            groupMessages.put(gSock,group);
                            System.out.println(groupMessages.get(teams) + " to dhmiourghsa");
                        }
                        else{
                            GroupMessages gro = groupMessages.get(teams);
//                            int size = gro.getViewsOfTheTeam().size();
                            gro.getViewsOfTheTeam().add(0,newGroup2);
                            gro.getMsgs().add(newGroup2);
//                            System.out.println("Mphka sta epomena view");
//                            seqNumber++;
//                            sendtoAPP.put(seqNumber,newGroup2);
//                            receiveMiddle.add(newGroup2);
                        }
                        middlewareTeamsBuffer.put(teams,newGroup2.getView());


//                        System.out.println("NEW VIEW STO MIDDLEWARE BUFFER");
//                    teams = middlewareTeamsBuffer.size();
//                        System.out.println("PAW NA PROSTHRESW TO NEO TEAM"+ teams + middlewareTeamsBuffer.get(teams).getGroupName());


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
                        continue;
                    }
//                    System.out.println("MEGETHOSPRIN" +receiveMiddle.size());
                    if(middlewareTeamsBuffer.size() > 0){
                        try {
                            byte[] newbuffer = new byte[1024];
                            Discovery.setSoTimeout(1000);
                            DatagramPacket packet = new DatagramPacket(newbuffer,newbuffer.length);
                            Discovery.setSoTimeout(1000);
                            Discovery.receive(packet);

                            ByteArrayInputStream baos = new ByteArrayInputStream(newbuffer);
                            ObjectInputStream oos = new ObjectInputStream(baos);

                            UdpMessage receiveMessage = (UdpMessage) oos.readObject();

                            System.out.println("Middleware received message" + receiveMessage.getMessage());

                            BM_deliver(receiveMessage);

                            System.out.println("MIDDLEWARE " +receiveMiddle.size());
                            for(int i =0;i < receiveMiddle.size();i++){
                                if(receiveMiddle.get(i).getType().equals("")){
                                    System.out.println(receiveMiddle.get(i).getMessage().getMessage());
                                }
                                else{
                                    System.out.println("VIEW");
                                }
                            }

                            for(int i =0;i < receiveMiddle.size();i++){

                            }
                        } catch (SocketTimeoutException e) {
//
                            if(whatIsend.size() > 0){
                                GroupInfo lastview = middlewareTeamsBuffer.get(whatIsend.get(0).getGroupId());

                                System.out.println(whatIsend.get(0).getMembersSend().size()+ " SIZE MHNYMATOS");
                                System.out.println(lastview.getMembers().size()+ " SIZE LAST VIEW");

                                if(whatIsend.get(0).getMembersSend().size() > lastview.getMembers().size()){
                                    int k = -1;
                                    for(int i = 0 ; i <sendsBuffer.size();i++){
                                        if(whatIsend.get(0).getSeqNo() == sendsBuffer.get(i).getSeqNo()){
                                            k = i;
                                        }
                                    }
                                    int resend = -1;
                                    if(k >= 0){
                                        for(int j = 0;j < lastview.getMembers().size();j++){
                                            for(int i = 0 ; i <sendsBuffer.get(k).getMembersSend().size();i++){
                                                    if(lastview.getMembers().get(j).getMemberPort() == sendsBuffer.get(k).getMembersSend().get(i)){
                                                        resend = 1;
                                                    }
                                            }
                                        }
                                    }
                                    System.out.println("MPHKA EDW");
                                    if(resend == -1){
                                        whatIsend.remove(0);
                                        sendsBuffer.remove(k);
                                    }
                                    else {
                                        System.out.println("TO KSANASTELNW");
                                        BM_send(whatIsend.get(0),whatIsend.get(0).getGroupId());
                                    }
                                    continue;
                                }
                                else{
                                    System.out.println("TO KSANASTELNW");
                                    BM_send(whatIsend.get(0),whatIsend.get(0).getGroupId());
                                }
                            }

//                            continue;

//                            e.printStackTrace();
                        } catch (IOException | ClassNotFoundException e) {
//                            e.printStackTrace();
                        }
                    }
            }

        }
    }

    public void BM_send(UdpMessage msg,int groupId){
        Iterator<EachMemberInfo> it = middlewareTeamsBuffer.get(groupId).getMembers().iterator();
        DatagramSocket UdpSocket = null;

//        while(it.hasNext()){
//            EachMemberInfo temp = it.next();
////            System.out.println("PORT"+ temp.getMemberPort());
//            msg.getMembersSend().add(temp.getMemberPort());
//        }
//        sendsBuffer.add(msg);
        Iterator<EachMemberInfo> it2 = middlewareTeamsBuffer.get(groupId).getMembers().iterator();
        while(it2.hasNext()){
            EachMemberInfo temp = it2.next();

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(msg);

                byte[] byteMsg = baos.toByteArray();
//                System.out.println("Sennding the Message to "+ temp.getName());
                DatagramPacket packet2 = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName(temp.getMemberAddress()), temp.getMemberPort());
                Discovery.send(packet2);


            } catch (SocketTimeoutException | UnknownHostException e) {
                continue;
            } catch (IOException e) {
                continue;
            }
        }
    }
    public void  BM_deliver(UdpMessage receiveMessage){
        int delete = 0;
        int hold = 0;
//        int flag =0;
        if(!mids.contains(receiveMessage.getSeqNo())) {
//            flag = 1;
            System.out.println("den to wxw to mhnyma");
            int i = 0;
            if (sendsBuffer.size() > 0) {
                for (; i < sendsBuffer.size(); i++) {
                    if (sendsBuffer.get(i).getSeqNo() == receiveMessage.getSeqNo()) {
                        int l = -1;
                        for (int j = 0; j < sendsBuffer.get(i).getMembersSend().size(); j++) {
//                                System.out.println(sendsBuffer.get(i).getMembersSend().get(j));
                            if (sendsBuffer.get(i).getMembersSend().get(j).equals(receiveMessage.getSenderPort())) {
                                l = i;
                                System.out.println("to ebgala");
                                break;
                            }
                        }
                        if (l >=0) {
                            sendsBuffer.get(i).getMembersSend().remove(l);
                        }
                        if (receiveMessage.getSenderPort() != OurPort) {
                            receiveMessage.setSenderPort(OurPort);
                            BM_send(receiveMessage, receiveMessage.getGroupId());
                        }
                        hold = 1;
                        if (sendsBuffer.get(i).getMembersSend().size() == 0) {
//                                hold = i;
                            Message rec = new Message("", receiveMessage);
                            receiveMiddle.add(rec);
                            delete = 1;
                            mids.add(receiveMessage.getSeqNo());
                            break;
                        }
                    }
                }
            }
            if (hold == 0) {
                System.out.println("PRWTH FORA");
                UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender());
//
                for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                    sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
                }
                for(int counter = 0;counter < sendAddition.getMembersSend().size();counter++){
                    System.out.println("Sends "+ sendAddition.getMembersSend().get(counter));
                }
                sendsBuffer.add(sendAddition);

                int size = sendsBuffer.size();
                int p = -1;
                for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                    if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == receiveMessage.getSenderPort()) {
                        System.out.println("PAW NA TO BGALW");
                        p = counter;
                    }
                }

                if (p >= 0) {

                    sendsBuffer.get(size - 1).getMembersSend().remove(p);
                }
                if (receiveMessage.getSenderPort() != OurPort) {
                    receiveMessage.setSenderPort(OurPort);
                    BM_send(receiveMessage, receiveMessage.getGroupId());
                }
                System.out.println(+size+ ""+ sendsBuffer.get(size - 1).getMembersSend().size());
                if (sendsBuffer.get(size - 1).getMembersSend().size() == 0) {
                    System.out.println("Eotimos na to balw middleware");
                    Message rec = new Message("", receiveMessage);
                    receiveMiddle.add(rec);
                    mids.add(receiveMessage.getSeqNo());
                    delete = 1;

                }
            }
            if(delete == 1){
                sendsBuffer.remove(i);
                int p = -1;
                for(int j = 0;j <whatIsend.size();j++){
                    System.out.println(whatIsend.get(j).getMessage());
                    if(whatIsend.get(j).getSeqNo() == receiveMessage.getSeqNo()){
                        p = j;
                    }
                }
                if(p >= 0){
                    System.out.println("P" + p);
                    whatIsend.remove(p);
                }
            }
         }
        if(delete == 1){
            int k = -1;
            for(int counter = 0;counter < receiveMiddle.size();counter++){
                if(receiveMiddle.get(counter).getMessage().getSeqNo() == receiveMessage.getSeqNo()){
                    k = counter;
                }
            }
            if(k>=0){
                receiveMiddle.remove(k);
            }
        }
        if(receiveMessage.getStartingSender() != OurPort){
            if(!appids.contains(receiveMessage.getSeqNo())){
                System.out.println(receiveMessage.getSeqNo());
                appids.add(receiveMessage.getSeqNo());
                Message rec = new Message("", receiveMessage);
                for(int j = 0;j < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                    if(receiveMessage.getStartingSender() == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                        rec.setName(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getName());
                    }
                }
//                rec.setName();
                groupMessages.get(receiveMessage.getGroupId()).getMsgs().add(rec);
            }
            checkFifoBuffer();
            return;
        }
        if(!mbufids.contains(receiveMessage.getSeqNo())){
            mbufids.add(receiveMessage.getSeqNo());
            Message rec = new Message("", receiveMessage);
            mbuf.add(rec);
            for(int j = 0;j < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                if(receiveMessage.getStartingSender() == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                    rec.setName(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getName());
                }
            }
        }
        checkFifoBuffer();
//        if(!appids.contains(receiveMessage.getSeqNo())){
//            System.out.println(receiveMessage.getSeqNo());
//            appids.add(receiveMessage.getSeqNo());
//            Message rec = new Message("", receiveMessage);
//            groupMessages.get(receiveMessage.getGroupId()).getMsgs().add(rec);
//        }
    }
    public void checkFifoBuffer(){
        while(true){
            int j = -1;
            for(int i = 0;i<mbuf.size();i++){
                if(mbuf.get(i).getMessage().getSelfDelivered() == delivered){

                    appids.add(mbuf.get(i).getMessage().getSeqNo());
                    groupMessages.get(mbuf.get(i).getMessage().getGroupId()).getMsgs().add(mbuf.get(i));
                    delivered++;
                    j = i;
                }
            }
            if(j >= 0){
                mbuf.remove(j);
            }
            if(j < 0){
                break;
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