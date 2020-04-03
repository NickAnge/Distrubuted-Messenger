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
//    List<Message> views;
    Thread middlewareThread;
    HashMap<Integer,Message> sendtoAPP;
    int teams ;
    public  final Object lock;
    int seqNumber;
    List<Integer> mids;//exoun paradwthei sto middleware sigoura;
    List<Integer> appids;
    HashMap<Integer,GroupInfo> Groups;
    int receiveMessages;


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
        this.seqNumber = seqNumber;
        sendtoAPP = new HashMap<>();
        lock = new Object();
        mids = new ArrayList<>();
        appids = new ArrayList<>();
//        views = new ArrayList<>();
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
            firstView = new Message("Add",middlewareTeamsBuffer.get(gSock));
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
//        System.out.println(msg);
        UdpMessage newMessage = new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort);


        BM_send(newMessage,gSock);


        return 0;
    }

    @Override
    public int grp_recv(int gSock, Message receiveMsg, int block) {
        if(groupMessages.containsKey(gSock)){
            if(groupMessages.get(gSock).getViewsOfTheTeam().size() > 0){
                Message nextMsg = groupMessages.get(gSock).getViewsOfTheTeam().get(0);
                groupMessages.get(gSock).getViewsOfTheTeam().remove(0);

//            UdpMessage udpMsg = new UdpMessage(
                receiveMsg.setName(nextMsg.getName());
                receiveMsg.setType(nextMsg.getType());
                receiveMsg.setView(nextMsg.getView());
                receiveMsg = new Message(nextMsg.getType(),nextMsg.getView());
                return 1;
            }
            if(groupMessages.get(gSock).getMsgs().size() > 0){
                Message nextMsg = groupMessages.get(gSock).getMsgs().get(0);
                groupMessages.get(gSock).getMsgs().remove(0);

//                UdpMessage udpMsg = new UdpMessage(nextMsg.getMessage().getMessage(),nextMsg.getMessage().getSeqNo(),nextMsg.getMessage().getSenderPort(),nextMsg.getMessage().getGroupId(),nextMsg.getMessage().getStartingSender());
                System.out.println("mPHKA EDW" + receiveMsg.getMessage());
//                receiveMsg = new Message(nextMsg.getType(),nextMsg.getView(),udpMsg);

//                receiveMsg = new Message(nextMsg.getMessage().getSenderPort(),)
                receiveMsg.getMessage().setSenderPort(nextMsg.getMessage().getSenderPort());
                receiveMsg.getMessage().setGroupId(nextMsg.getMessage().getGroupId());
                receiveMsg.getMessage().setMessage(nextMsg.getMessage().getMessage());
                receiveMsg.getMessage().setSeqNo(nextMsg.getMessage().getSeqNo());
                receiveMsg.getMessage().setSenderPort(nextMsg.getMessage().getSenderPort());
                for(int j =0;j< middlewareTeamsBuffer.get(gSock).getMembers().size();j++){
                        if(middlewareTeamsBuffer.get(gSock).getMembers().get(j).getMemberPort() == nextMsg.getMessage().getStartingSender()){
                            receiveMsg.setName(middlewareTeamsBuffer.get(gSock).getMembers().get(j).getName());
                            break;
                        }
                }


//                receiveMsg = new Message(nextMsg.getType(),udpMsg);
//

                return 1;
            }
        }





        return 0;
    }

//        if(sendtoAPP.size() >0){
////            UdpMessage k  = receiveMiddle.get(0).getMessage();
////            System.out.println("ΛΑΜΒΑΝΩ ΜΗΝΥΜΑ : " + receiveMiddle);
////            receiveMsg = new Message("",receiveMiddle.get(0).getView(),receiveMiddle.get(0).getMessage());
//
//            Set<Integer> kes = sendtoAPP.keySet();
//            int i =-1;
//            for(Integer req: kes){
//                Message temp = sendtoAPP.get(req);
//
//                if(temp.getMessage() != null){
//                    receiveMsg.setMessage(temp.getMessage());
//                    for(int j =0;j< middlewareTeamsBuffer.get(gSock).getMembers().size();j++){
//                        if(middlewareTeamsBuffer.get(gSock).getMembers().get(j).getMemberPort() == temp.getMessage().getStartingSender()){
//                            receiveMsg.setName(middlewareTeamsBuffer.get(gSock).getMembers().get(j).getName());
//                            break;
//                        }
//                    }
//                }
//                else if(temp.getView()!=null){
//                    receiveMsg.setView(temp.getView());
//                }
//                receiveMsg.setType(temp.getType());
//                i = req;
//                break;
//            }
//            if(i> 0){
//                sendtoAPP.remove(i);
//                return 1;
//            }
//            else {
//                return 0;
//            }
////            if(receiveMiddle.get(0).getMessage() != null){
////                receiveMsg.setMessage(receiveMiddle.get(0).getMessage());
////            }
////            else if (receiveMiddle.get(0).getView() != null){
////                receiveMsg.setView(receiveMiddle.get(0).getView());
////            }
//
////            receiveMsg.setType(receiveMiddle.get(0).getType());
////            System.out.println(receiveMsg.getMessage().getMessage());
////            receiveMiddle.remove(0);
////            return  1;
//        }
//
//        return 0;



    public Object getViewFromSocket(Socket socket)   {
        Message newView = null;
        try {
            socket.setSoTimeout(500);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//            System.out.println("Middleware receiving new VIEW ");
            newView = (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Message error = new Message("noMessage",null,null);
            return error;
        }
//        System.out.println("Persasa apo edw "+newView.getType());

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
                            int size = gro.getViewsOfTheTeam().size();
                            gro.getViewsOfTheTeam().add(size,newGroup2);
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
//                        DatagramSocket UdpSocket = null;
                        try {
//                            UdpSocket = new DatagramSocket();
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

                        } catch (SocketTimeoutException e) {
//                            System.out.println("PERASA APO");

                            continue;
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

        while(it.hasNext()){
            EachMemberInfo temp = it.next();
//            System.out.println("PORT"+ temp.getMemberPort());
            msg.getMembersSend().add(temp.getMemberPort());
        }
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

//    public void BM_deliver(UdpMessage receiveMessage){
//        int flag = 0;
//        int hold = 0;
//        int delete = 0;
//        if(!mids.contains(receiveMessage.getSeqNo())){
//            if(receiveMessage.getStartingSender() == OurPort){
////                System.out.println("Starting Sender"+ receiveMessage.getStartingSender());
////                System.out.println("ELSE SENDER" + receiveMessage.getSenderPort());
//                int i =0;
////                System.out.println("Size of sends BUffer" + sendsBuffer.size());
//                for(;i<sendsBuffer.size();i++){
////                    System.out.println("Mpainw na to bgalw");
//                    if(sendsBuffer.get(i).getSeqNo() == receiveMessage.getSeqNo()){
//                        int l = -1;
//                        for(int j= 0;j < sendsBuffer.get(i).getMembersSend().size();j++){
//
////                            System.out.println(sendsBuffer.get(i).getMembersSend().get(j));
//                            if(sendsBuffer.get(i).getMembersSend().get(j).equals(receiveMessage.getSenderPort())){
//                                l = i;
////                                System.out.println("to ebgala");
//                                break;
//                            }
//                        }
//                        if(l != -1) {
//                            sendsBuffer.get(i).getMembersSend().remove(l);
//                        }
//                        hold = 1;
//                        if(receiveMessage.getSenderPort() != OurPort){
//                            receiveMessage.setSenderPort(OurPort);
////                            System.out.println("PAW NA STEILW");
//                            BM_send(receiveMessage,receiveMessage.getGroupId());
////                            System.out.println("Esteila");
//
//                        }
//                        if(sendsBuffer.get(i).getMembersSend().size() == 0){
////                        hold = i;
//                            Message rec = new Message("",receiveMessage);
//                            receiveMiddle.add(rec);
//                            mids.add(receiveMessage.getSeqNo());
//                            break;
//                        }
//                    }
//                }
//                if(hold == 0){
//                    UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(),receiveMessage.getSeqNo(),receiveMessage.getSenderPort(),receiveMessage.getGroupId(),receiveMessage.getStartingSender());
//
//                    for(int counter = 0;counter < receiveMessage.getMembersSend().size();counter++){
//                        sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
//                    }
////                    for(int counter = 0;counter < sendAddition.getMembersSend().size();counter++){
//////                        System.out.println("Sends "+ sendAddition.getMembersSend().get(counter));
////                    }
//                    sendsBuffer.add(sendAddition);
////                int l = sendsBuffer.indexOf(receiveMessage);
//                    int p = sendsBuffer.get(0).getMembersSend().indexOf(receiveMessage.getSenderPort());
//
//                    sendsBuffer.get(0).getMembersSend().remove(p);
//
//                    if(receiveMessage.getSenderPort() != OurPort){
//                        receiveMessage.setSenderPort(OurPort);
//                        BM_send(receiveMessage,receiveMessage.getGroupId());
//                    }
//                    if(sendsBuffer.get(0).getMembersSend().size() == 0){
//                        Message rec = new Message("",receiveMessage);
//                        receiveMiddle.add(rec);
//                        mids.add(receiveMessage.getSeqNo());
//                    }
//                }
//                hold = 0;
//                if(i != sendsBuffer.size()){
//                    if(sendsBuffer.get(i) != null) {
//                        if (sendsBuffer.get(i).getMembersSend().size() == 0) {
////                            System.out.println("Mpainw na to bgalw");
//                            sendsBuffer.remove(i);
//                        }
//                    }
//                }
//            }
//            else {
////                System.out.println("Perasa ap oedw");
////                System.out.println(receiveMiddle.size());
//                int h = 0;
//                for (; h < resendBuffer.size(); h++) {
////                    System.out.println("MPHKA GAMW TO SPITI");
//                    if (resendBuffer.get(h).getSeqNo() == receiveMessage.getSeqNo()) {
//                        int l = -1;
//                        for (int j = 0; j < resendBuffer.get(h).getMembersSend().size(); j++) {
////                            System.out.println(resendBuffer.get(h).getMembersSend().get(j));
//                            if (resendBuffer.get(h).getMembersSend().get(j).equals(receiveMessage.getSenderPort())) {
//                                l = h;
//                            }
//                        }
//                        if (l != -1) {
//                            resendBuffer.get(h).getMembersSend().remove(l);
//                        }
//                        flag = 1;
//
//                        if (receiveMessage.getSenderPort() != OurPort) {
//                            receiveMessage.setSenderPort(OurPort);
//                            BM_send(receiveMessage, receiveMessage.getGroupId());
//                        }
////                        System.out.println("Mphka edw" + resendBuffer.get(h).getMembersSend().size() + h );
//                        if (resendBuffer.get(h).getMembersSend().size() == 0) {
//                            Message rec = new Message("", receiveMessage);
//                            receiveMiddle.add(rec);
//                            delete = 1;
//                            mids.add(receiveMessage.getSeqNo());
//                            break;
//                        }
//                    }
//                }
//                if (delete == 1) {
//                    resendBuffer.remove(h);
//                }
//
//
////                System.out.println("Perasa ap oedw");
//                if (flag == 0) {
////                    System.out.println(receiveMiddle.size());
////                    System.out.println("Hrthe prwto mhnyma apo ton:"+ receiveMessage.getSenderPort());
////                    System.out.println(receiveMessage.getMessage());
////                    System.out.println(receiveMessage.getSeqNo());
////                    System.out.println(receiveMessage.getSenderPort());
////                    System.out.println(receiveMessage.getGroupId());
//
////                    System.out.println(receiveMessage.getStartingSender());
//
//                    UdpMessage addition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender());
//
//                    for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
//                        addition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
//                    }
//
//                    resendBuffer.add(addition);
////                    int p = resendBuffer.get(0).getMembersSend().indexOf(receiveMessage.getSenderPort());
////                    System.out.println("Prin"+resendBuffer.get(0).getMembersSend().size());
//                    int p = -1;
//                    for(int i = 0; i<resendBuffer.get(0).getMembersSend().size();i++){
//                        if(resendBuffer.get(0).getMembersSend().get(i).equals(receiveMessage.getSenderPort())){
//                            p =i;
//                        }
//                    }
//                    if(p>=0){
//                        resendBuffer.get(0).getMembersSend().remove(p);
//
//                    }
////                    resendBuffer.get(0).getMembersSend().remove(p);
////                    for (int i = 0; i < resendBuffer.get(0).getMembersSend().size(); i++) {
//////                        System.out.println(resendBuffer.get(0).getMembersSend().get(i));
////                    }
////                System.out.println(resendBuffer.get(l).getMembersSend().size() + resendBuffer.get(l).getMembersSend().get(0));
//                    receiveMessage.setSenderPort(OurPort);
//
//                    BM_send(receiveMessage, receiveMessage.getGroupId());
//
////
////                    for(int i = 0;i < resendBuffer.get(0).getMembersSend().size();i++){
////                        System.out.println(resendBuffer.get(0).getMembersSend().get(i));
////                    }
//
////                    System.out.println(receiveMiddle.size());
//                }
//                flag = 0;
//            }
//            Message rec = new Message("",receiveMessage);
//            groupMessages.get(receiveMessage.getGroupId()).getMsgs().add(rec);
//
//        }
//
//
//
////        if(!mids.contains(receiveMessage.getSeqNo())){
////            mids.add(receiveMessage.getSeqNo());
////            System.out.println(OurPort);
////            if(receiveMessage.getSenderPort() != OurPort){
//////                receiveMessage.setSenderPort(OurPort);
////                BM_send(receiveMessage,receiveMessage.getGroupId());
////            }
////
////            Message rec = new Message("",receiveMessage);
////            receiveMiddle.add(rec);
//////            receiveBuffer.add(receiveMessage);
////        }
////
////        System.out.println(receiveMessage.getSenderPort());
//    }


    public void  BM_deliver(UdpMessage receiveMessage){
        int delete = 0;
        int hold = 0;
        int flag =0;
        if(!mids.contains(receiveMessage.getSeqNo())) {
            flag = 1;
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
                        if (l != -1) {
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
            }
         }
        if(flag == 0){
            int k = -1;
            for(int counter = 0;counter < receiveMiddle.size();counter++){
                if(receiveMiddle.get(counter).getMessage().getSeqNo() == receiveMessage.getSeqNo()){
                    k = counter;
                }
            }
            receiveMiddle.remove(k);
        }
        if(!appids.contains(receiveMessage.getSeqNo())){
            System.out.println(receiveMessage.getSeqNo());
            appids.add(receiveMessage.getSeqNo());
            Message rec = new Message("", receiveMessage);
            groupMessages.get(receiveMessage.getGroupId()).getMsgs().add(rec);
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