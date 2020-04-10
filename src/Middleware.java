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
    public  final Object lock2;
    int se;
    int re;
//    List<UdpMessage> receiveBuffer;
    List<Message> receiveMiddle; //for both reliability
    List<UdpMessage> sendsBuffer; //for both
    List<UdpMessage> resend; //for RM messages from coordinator;

    List<UdpMessage> whatIsend; //fifo
    List<UdpMessage> whatIsendTotal; //total
    List<UdpMessage> coordBuffer;

    Thread middlewareThread;
    int teams ;
    public  final Object lock;

    int seqNumber;
//    List<Integer> mids;//exoun paradwthei sto middleware sigoura;
    HashMap <Integer,List<Integer>> mids;

//    List<Integer> rids;//exoun paradwthei sto middleware apo ton coordinator;
        HashMap <Integer,List<Integer>> rids;

//    List<Integer> appids;
        HashMap <Integer,List<Integer>> appids;

    int receiveMessages;
    int delivered;
    int sended;
    int totaldelivered; //gia to TOTAL;
    int amICoordinator;
    int allMessages;
    List<Message> mbuf; //FOR FIFO
    List<Message> totalbuf;//FOR TOTAL
//    List<Integer> mbufids; //for buffer fifo;
        HashMap <Integer,List<Integer>> mbufids;

//    List<Integer> totalbufids;//FOr total
        HashMap <Integer,List<Integer>> totalbufids;


    HashMap<Integer,Integer> sendForEachTeam;
    HashMap <Integer,List<Integer>> idsCoord;
//    List<Integer> idsCoord;

    int gSock;
    public Middleware() {
        lock2 = new Object();
//        Groups = new HashMap<Integer,GroupInfo>();
        InfoManager = new GroupManagerInfo(null);
//        receiveBuffer = new ArrayList<UdpMessage>();
        discoverGroupManager();
        totalbuf=new ArrayList<>();
        sendForEachTeam = new HashMap<>();
        totalbufids = new HashMap<>();
        receiveMessages = 0;
        groupMessages = new HashMap<>();
        rids = new HashMap<>();
//        idsCoord = new ArrayList<>();
        se =0;
        re = 0;
        totaldelivered = 0;
        coordBuffer = new ArrayList<>();
        idsCoord = new HashMap<>();
        gSock =0;
        teams = 0;
        delivered =0;
        sended = -1;
        allMessages = 0;
        amICoordinator = 0;
        whatIsendTotal = new ArrayList<>();
        this.seqNumber = 0;
        resend = new ArrayList<>();
        mbufids = new HashMap<>();
        mbuf = new ArrayList<>();
        lock = new Object();
        mids = new HashMap<>();
        appids = new HashMap<>();
        whatIsend = new ArrayList<>();
        receiveMiddle = new ArrayList<>();
        sendsBuffer = new ArrayList<>();
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

        middlewareTeamsBuffer.remove(gSock);
        groupMessages.remove(gSock);

        return 1;
    }

    @Override
    public int grp_send(int gSock, String msg, int len, int total) {

        UdpMessage newMessage = null;
        seqNumber++;

//        System.out.println("SEIRA APOSTOLHS" + sended);
        if(total == 0){
            sended++;
//            int sen = sendForEachTeam.get(gSock);
//            sen++;

            newMessage = new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort,total,sended,-1);
//            sendForEachTeam.put(gSock,sen);
        }
        else{
            newMessage = new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort,total,-1,-1);
        }
//        UdpMessage newMessage2= new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort);

//        newMessage.setSelfDelivered(sended);
//        newMessage2.setSelfDelivered(sended);

        Iterator<EachMemberInfo> it = middlewareTeamsBuffer.get(gSock).getMembers().iterator();

        while(it.hasNext()){
            EachMemberInfo temp = it.next();
            newMessage.getMembersSend().add(temp.getMemberPort());
//            System.out.println("SEND TO :" +temp.getMemberPort());
        }

        if(total ==0){

            whatIsend.add(newMessage);

        }
        else if(total == 1){
            whatIsendTotal.add(newMessage);
        }
        BM_send(newMessage,gSock);


        return 0;
    }

    @Override
    public int grp_recv(int gSock, Message receiveMsg, int block) {

        if(block ==  1){
            do{
                synchronized (lock2){
                    try {
                        lock2.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            while(groupMessages.get(gSock).getMsgs().size()  < 0);
        }

        if(groupMessages.containsKey(gSock)){
            while (true) {
                if (groupMessages.get(gSock).getMsgs().size() > 0) {
                    Message nextMsg = groupMessages.get(gSock).getMsgs().get(0);
//                    System.out.println("Size of groupMsg" + groupMessages.get(gSock).getMsgs().size());
                    System.out.println("mPHKA EDW" + nextMsg + "seira mhnumatos");

                    if(nextMsg == null){
//                        groupMessages.get(gSock).getMsgs().remove(0);
//                        return 0;
                        continue;
                    }
                    if (nextMsg.getMessage() != null) {
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
                        receiveMsg.getMessage().setTotaldelivered(nextMsg.getMessage().getTotaldelivered());

                        receiveMsg.setName(nextMsg.getName());
                        groupMessages.get(gSock).getMsgs().remove(0);
                        return 1;
                    }
                    if (nextMsg.getView() != null) {
                        receiveMsg.setName(nextMsg.getName());
                        receiveMsg.setType(nextMsg.getType());
                        receiveMsg.setView(nextMsg.getView());
                        receiveMsg.setChangeViewMessage(nextMsg.getChangeViewMessage());
                        groupMessages.get(gSock).getMsgs().remove(0);

                    }
                    return 1;
                }
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
            return error;
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

    public void coordinatorSends(UdpMessage msg,int groupId){
        idsCoord.get(msg.getStartingSender()).add(msg.getSeqNo());
        BM_send(msg,groupId);
    }
    class MiddlewareJob extends Thread {
        @Override
        public void run() {
            while(true){
                if(currentThread().isInterrupted()){
                    System.out.println("The app is closing");
                    return;
                }
                    Message newGroup2 = (Message) getViewFromSocket(InfoManager.getCommunicationSock());

                    if(!newGroup2.getType().equals("noMessage")){

                        teams = newGroup2.getView().getId();

                        if(!middlewareTeamsBuffer.containsKey(teams)){
                            gSock = teams;
                            GroupMessages group = new GroupMessages();

                            for(int counter = 0;counter < newGroup2.getView().getMembers().size();counter++){
                                group.getFifoOrders().put(newGroup2.getView().getMembers().get(counter).getMemberPort(),0);

                                ArrayList<Integer> list = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn
                                mids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list);
                                ArrayList<Integer> list2 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                rids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list2);
                                ArrayList<Integer> list3 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                appids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list3);
                                ArrayList<Integer> list4 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                mbufids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list4);
                                ArrayList<Integer> list5 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn
                                totalbufids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list5);

                                ArrayList<Integer> list6 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn
                                idsCoord.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list6);

                            }
                            groupMessages.put(gSock,group);
//                            System.out.println(groupMessages.get(teams) + " to dhmiourghsa");
                            if(newGroup2.getView().getCoInfo().getCoMember().getMemberPort() == OurPort){
//                                System.out.println("EIMAI O COORDINATOR");
                                amICoordinator = 1;
                            }
//                            sendForEachTeam.put(gSock,-1);
                        }
                        else{
                            GroupMessages gro = groupMessages.get(teams);
                            gro.getViewsOfTheTeam().add(0,newGroup2);
                            gro.getMsgs().add(newGroup2);

                            totaldelivered = 0;

                            for(int counter = 0;counter < newGroup2.getView().getMembers().size();counter++){
                                if(gro.getFifoOrders().containsKey(newGroup2.getView().getMembers().get(counter).getMemberPort())){
                                    continue;
                                }
                                else{
                                    ArrayList<Integer> list = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn
                                    mids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list);
                                    ArrayList<Integer> list2 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                    rids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list2);
                                    ArrayList<Integer> list3 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                    appids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list3);
                                    ArrayList<Integer> list4 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                    mbufids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list4);
                                    ArrayList<Integer> list5 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn
                                    totalbufids.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list5);

                                    gro.getFifoOrders().put(newGroup2.getView().getMembers().get(counter).getMemberPort(),0);

                                    ArrayList<Integer> list6 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn
                                    idsCoord.put(newGroup2.getView().getMembers().get(counter).getMemberPort(),list6);
                                }
                            }
//
                            if(newGroup2.getView().getCoInfo().getCoMember().getMemberPort() == OurPort){
//                                System.out.println("EIMAI O COORDINATOR");
                                ArrayList<Integer> list5 = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                                amICoordinator = 1;

                            }

                        }

//                        System.out.println("paw na balw to view sto middleware " + newGroup2.getView().getCoInfo().getDeliverno());

                        middlewareTeamsBuffer.put(teams,newGroup2.getView());


//                        System.out.println("prin balw to view sto middleware " + middlewareTeamsBuffer.get(teams).getCoInfo().getDeliverno());

//                        System.out.println("PRIN");
//                        for(int i =0; i<sendsBuffer.size();i++){
//                            System.out.println(sendsBuffer.get(i).getMessage());
//                            for(int j = 0;j <sendsBuffer.get(i).getMembersSend().size();j++){
//                                System.out.println(sendsBuffer.get(i).getMembersSend().get(j));
//                            }
//                        }
//                        System.out.println("ALLAKSE TO VIEW");
                        afterchangeview(newGroup2.getView());
//                        System.out.println("ESBHSA AUTA POU DEN XREIAZONTAN");
//                        System.out.println("MEta");

//                        for(int i =0; i<sendsBuffer.size();i++){
//                            System.out.println(sendsBuffer.get(i).getMessage());
//                            for(int j = 0;j <sendsBuffer.get(i).getMembersSend().size();j++){
//                                System.out.println(sendsBuffer.get(i).getMembersSend().get(j));
//                            }
//                        }
//                        System.out.println("NEW VIEW STO MIDDLEWARE BUFFER");
//                    teams = middlewareTeamsBuffer.size();
//                        System.out.println("PAW NA PROSTHRESW TO NEO TEAM"+ teams + middlewareTeamsBuffer.get(teams).getGroupName());


//                        Set<Integer> keys = middlewareTeamsBuffer.keySet();
//                        for(Integer req: keys){
//                            System.out.println(middlewareTeamsBuffer.get(req).getGroupName());
//                            System.out.println(middlewareTeamsBuffer.get(req).getId());
//                            for(int i = 0; i <middlewareTeamsBuffer.get(req).getMembers().size();i++){
//                                System.out.println(middlewareTeamsBuffer.get(req).getMembers().get(i).getName());
//                            }
//                        }
//                        System.out.println("MPHKA STO MIDDLE");

                        synchronized (lock){
                            lock.notify();
                        }
                        continue;
                    }
//                    System.out.println("MEGETHOSPRIN" +receiveMiddle.size());
                    if(middlewareTeamsBuffer.size() > 0){
                        try {
                            byte[] newbuffer = new byte[5000];
//                            Discovery.setSoTimeout(1000);
                            DatagramPacket packet = new DatagramPacket(newbuffer,newbuffer.length,InetAddress.getLocalHost(),OurPort);
                            Discovery.setSoTimeout(50);
                            Discovery.receive(packet);

                            ByteArrayInputStream baos = new ByteArrayInputStream(newbuffer);
                            ObjectInputStream oos = new ObjectInputStream(baos);

                            UdpMessage receiveMessage = (UdpMessage) oos.readObject();

                            afterchangeview(middlewareTeamsBuffer.get(receiveMessage.getGroupId()));

                            if(receiveMessage.getTotal() == 1){
                                BM_deliverTotal1(receiveMessage);
                            }
                            else  if(receiveMessage.getTotal() == 0){
                                BM_deliver(receiveMessage);
                            }

                            if(receiveMessage.getTotal() == 1){
                                if(!rids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
                                    if(!idsCoord.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
                                        if(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getCoInfo().getCoMember().getMemberPort() == OurPort){
                                            int deliver = middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getCoInfo().getDeliverno();
                                            deliver++;
                                            System.out.println("EIMAI O COORDINATOR ///BAZW TTON EPOEMNO SEIRIAKO ARITHMO DELIVER :"+ deliver);
                                            middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getCoInfo().setDeliverno(deliver);
                                            receiveMessage.setTotaldelivered(deliver);
                                            for(int i =0;i < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();i++){
                                                int doesExist = -1;

                                                for(int j = 0; j< receiveMessage.getMembersSend().size();j++){
                                                    if(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(i).getMemberPort() == receiveMessage.getMembersSend().get(j)){
                                                        doesExist = 1;
                                                    }
                                                }
                                                if(doesExist == -1){
                                                    receiveMessage.getMembersSend().add(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(i).MemberPort);
                                                }
                                            }
                                            coordBuffer.add(receiveMessage);
                                            coordinatorSends(receiveMessage,receiveMessage.getGroupId());
                                        }
                                    }
                                }
                            }
//                            System.out.println("MIDDLEWARE " +receiveMiddle.size())
                        }

                        catch (SocketTimeoutException e) {
//                            System.out.println("mphka interr");
                            if(whatIsendTotal.size() > 0){
                                GroupInfo lastview = middlewareTeamsBuffer.get(whatIsendTotal.get(0).getGroupId());
                                if(whatIsendTotal.get(0).getMembersSend().size() > lastview.getMembers().size()){
                                    int k = -1;
                                    for(int i = 0 ; i <sendsBuffer.size();i++){
                                        if(whatIsendTotal.get(0).getSeqNo() == sendsBuffer.get(i).getSeqNo()){
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
//                                    System.out.println("MPHKA EDW");
                                    if(resend == -1){
                                        whatIsendTotal.remove(0);
                                        sendsBuffer.remove(k);
                                    }
                                    else {
//                                        System.out.println("TO KSANASTELNW_TOTAL");
//                                        for(int i = 0;)
                                        BM_send(whatIsendTotal.get(0),whatIsendTotal.get(0).getGroupId());
                                    }
                                    continue;
                                }
                                else{
                                   for(int i =0;i<whatIsendTotal.size();i++){
                                        BM_send(whatIsendTotal.get(i),whatIsendTotal.get(i).getGroupId());

                                    }
                                }
                            }
                            else if(whatIsend.size() > 0){
                                System.out.println("KSANATSEKARW TO what i send");

//                                System.out.println("WHAT I SEND timout exception");
                                GroupInfo lastview = middlewareTeamsBuffer.get(whatIsend.get(0).getGroupId());

//                                System.out.println(whatIsend.get(0).getMembersSend().size()+ " SIZE MHNYMATOS");
//                                System.out.println(lastview.getMembers().size()+ " SIZE LAST VIEW");


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
//                                    System.out.println("MPHKA  STO RESEND DDDDD EDW");
                                    if(resend == -1){
                                        whatIsend.remove(0);
                                        sendsBuffer.remove(k);
                                    }
                                    else {
//                                        System.out.println("TO KSANASTELNW");
                                        BM_send(whatIsend.get(0),whatIsend.get(0).getGroupId());                                    }
                                    continue;
                                }
                                else {
//                                    System.out.println("TO KSANASTELNW");
//                                    for()
//                                    for(int i =0;i<whatIsend.size();i++){
                                        BM_send(whatIsend.get(0),whatIsend.get(0).getGroupId());

//                                    }
//                                    BM_send(whatIsend.get(0),whatIsend.get(0).getGroupId());

                                }
                            }
                            else if(sendsBuffer.size() > 0 ){
//                                System.out.println("KSANATSEKARW TO SENDs");

                                GroupInfo lastview = middlewareTeamsBuffer.get(sendsBuffer.get(0).getGroupId());

                                if(sendsBuffer.get(0).getMembersSend().size() > lastview.getMembers().size()){
//                                    int k =-1;
                                    int resend = -1;
                                    for(int j = 0;j < lastview.getMembers().size();j++){
                                        for(int i = 0 ; i <sendsBuffer.get(0).getMembersSend().size();i++){
                                            if(lastview.getMembers().get(j).getMemberPort() == sendsBuffer.get(0).getMembersSend().get(i)){
                                                resend = 1;
                                            }
                                        }
                                    }
                                    if(resend ==-1){
                                        sendsBuffer.remove(0);
                                    }
                                    else{
                                        sendsBuffer.get(0).setSenderPort(OurPort);
                                        BM_send(sendsBuffer.get(0),sendsBuffer.get(0).getGroupId());
                                    }
                                }
                                else {
                                    System.out.println("sto sendsBUffer");
                                    sendsBuffer.get(0).setSenderPort(OurPort);
                                    BM_send(sendsBuffer.get(0),sendsBuffer.get(0).getGroupId());
                                }
                            }
                            else if(coordBuffer.size() > 0){
                                for(int i = 0; i <coordBuffer.size();i++){
                                    BM_send(coordBuffer.get(0),coordBuffer.get(0).getGroupId());
                                }
                            }
                            else if(resend.size() > 0){
                                int again = -1;
                                GroupInfo lastview = middlewareTeamsBuffer.get(resend.get(0).getGroupId());

                                if(resend.get(0).getMembersSend().size() > lastview.getMembers().size()){
//                                    int k =-1;
                                    for(int j = 0;j < lastview.getMembers().size();j++){
                                        for(int i = 0 ; i <resend.get(0).getMembersSend().size();i++){
                                            if(lastview.getMembers().get(j).getMemberPort() == resend.get(0).getMembersSend().get(i)){
                                                again = 1;
                                            }
                                        }
                                    }
                                    if(again ==-1){
                                        resend.remove(0);
                                    }
                                    else{
                                        resend.get(0).setSenderPort(OurPort);
                                        BM_send(resend.get(0),resend.get(0).getGroupId());
                                    }
                                }
                                else {
                                    System.out.println("sto ResendBuffer"+ resend.get(0).getTotaldelivered());
                                    resend.get(0).setSenderPort(OurPort);
                                    BM_send(resend.get(0),resend.get(0).getGroupId());
                                }
                            }

                        }
                        catch (InterruptedIOException ex){
                            return;
                        } catch (IOException | ClassNotFoundException e) {
//                            e.printStackTrace();
                            return;
                        }
                    }
            }
        }
    }

    public void afterchangeview(GroupInfo view){
        if(sendsBuffer.size() > 0){
            for(int counter = 0; counter < sendsBuffer.size();counter++){
                if(sendsBuffer.get(counter).getGroupId() == view.getId()){
//                    if(sendsBuffer.get(counter).getMembersSend().size() > )
                    int size = view.getMembers().size();
                    int k = 0;
                    int sizeSends = sendsBuffer.size();
                    int l =-1;
                    while(sizeSends != 0){
                        for(int i =0;i<sendsBuffer.get(counter).getMembersSend().size();i++){
                            sizeSends--;
                            for(int j =0;j < view.getMembers().size();j++){
                                if(sendsBuffer.get(counter).getMembersSend().get(i) != view.getMembers().get(j).getMemberPort()){
                                    k++;
//                                  break;
                                }
                            }
                            if(k == size ){
                                k=0;
                                sendsBuffer.get(counter).getMembersSend().remove(i);
                                break;
                            }
                            k=0;
                            if(sizeSends == 0){
                                break;
                            }
                        }
                    }
                }
            }
        }
        if(resend.size() > 0){
            for(int counter = 0; counter < resend.size();counter++){
                if(resend.get(counter).getGroupId() == view.getId()){
                    int size = view.getMembers().size();
                    int k =0;
                    int sizeresends= resend.get(counter).getMembersSend().size();
                    while(sizeresends != 0){
                        for(int i =0;i<resend.get(counter).getMembersSend().size();i++){
                            sizeresends--;
                            for(int j =0;j < view.getMembers().size();j++){
                                if(resend.get(counter).getMembersSend().get(i) != view.getMembers().get(j).getMemberPort()){
                                    k++;
//                                    break;
                                }
                            }
                            if(k == size){
                                k =0;
                                resend.get(counter).getMembersSend().remove(i);
                                break;
                            }
                            k=0;
                            if(sizeresends == 0){
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    public void  BM_send(UdpMessage msg,int groupId){
//        Iterator<EachMemberInfo> it = middlewareTeamsBuffer.get(groupId).getMembers().iterator();
        DatagramSocket UdpSocket = null;

        Iterator<EachMemberInfo> it2 = middlewareTeamsBuffer.get(groupId).getMembers().iterator();

        while(it2.hasNext()){
            EachMemberInfo temp = it2.next();

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(msg);

                byte[] byteMsg = baos.toByteArray();
//                System.out.println("Sennding the Message to "+ temp.getName());
                int send = -1;
                for(int i =0; i < msg.getMembersSend().size();i++){
                    if(msg.getMembersSend().get(i) == temp.getMemberPort()){
                        System.out.println("SEND PORT"+ temp.getMemberPort());
                        DatagramPacket packet2 = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName(temp.getMemberAddress()), temp.getMemberPort());
                        Discovery.send(packet2);
                    }
                }
//                DatagramPacket packet2 = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName(temp.getMemberAddress()), temp.getMemberPort());
//                Discovery.send(packet2);


            } catch (SocketTimeoutException | UnknownHostException e) {
                continue;
            } catch (IOException e) {
                continue;
            }
        }
    }

    public void BM_deliverTotal1(UdpMessage receiveMessage){
        int hold =0;
        int delete = 0;
        int exist = -1;

        if(receiveMessage.getTotaldelivered() < 0 ){
            if(!mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())) {
                int i = 0;
//                System.out.println("Sends Buffer");
                if (sendsBuffer.size() > 0) {
                    for (; i < sendsBuffer.size(); i++) {
                        if (sendsBuffer.get(i).getSeqNo() == receiveMessage.getSeqNo() && sendsBuffer.get(i).getStartingSender() == receiveMessage.getStartingSender()) {
                            int l = -1;
                            for (int j = 0; j < sendsBuffer.get(i).getMembersSend().size(); j++) {
//                                System.out.println(sendsBuffer.get(i).getMembersSend().get(j));
                                if (sendsBuffer.get(i).getMembersSend().get(j).equals(receiveMessage.getSenderPort())) {
                                    l = j;
//                                    System.out.println("to ebgala");
                                    break;
                                }
                            }
                            if (l >=0) {
                                sendsBuffer.get(i).getMembersSend().remove(l);
                            }
                            if (receiveMessage.getSenderPort() != OurPort) {
                                receiveMessage.setSenderPort(OurPort);
                                int z = -1;
                                for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                                    if(receiveMessage.getMembersSend().get(j) == OurPort){
                                        z = j;
                                    }
                                }
                                if(z >=0){
                                    receiveMessage.getMembersSend().remove(z);
                                }
                                BM_send(receiveMessage, receiveMessage.getGroupId());
                            }
                            hold = 1;
                            if (sendsBuffer.get(i).getMembersSend().size() == 0) {
//                                hold = i;
                                se++;

                                Message rec = new Message("", receiveMessage);
                                receiveMiddle.add(rec);
                                delete = 1;
                                mids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                                break;
                            }
                        }
                    }
                }
                if (hold == 0) {
//                    System.out.println("PRWTH FORA");
//                    System.out.println("Sends Buffer");

                    UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),-1);
//
                    for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                        sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
                    }
                    for(int counter = 0;counter < sendAddition.getMembersSend().size();counter++){
//                        System.out.println("Sends "+ sendAddition.getMembersSend().get(counter));
                    }
                    sendsBuffer.add(sendAddition);

                    int size = sendsBuffer.size();
                    int p = -1;
                    for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                        if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == receiveMessage.getSenderPort()) {
//                            System.out.println("PAW NA TO BGALW");
                            p = counter;
                        }
                    }
                    if (p >= 0) {
                        sendsBuffer.get(size - 1).getMembersSend().remove(p);
                    }
                    if (receiveMessage.getSenderPort() != OurPort) {
                        receiveMessage.setSenderPort(OurPort);
                        int z = -1;
                        for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                            if(receiveMessage.getMembersSend().get(j) == OurPort){
                                z = j;
                            }
                        }
                        if(z >=0){
                            receiveMessage.getMembersSend().remove(z);
                        }
                        BM_send(receiveMessage, receiveMessage.getGroupId());
                    }
//                    System.out.println(+size+ ""+ sendsBuffer.get(size - 1).getMembersSend().size());
                    if (sendsBuffer.get(size - 1).getMembersSend().size() == 0) {
//                        System.out.println("Eotimos na to balw middleware");
                        se++;
                        Message rec = new Message("", receiveMessage);
                        receiveMiddle.add(rec);
                        mids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                        delete = 1;

                    }
                }
                if(delete == 1){
                    sendsBuffer.remove(i);
                }
            }

            if(mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
                System.out.println("STO MID");
                System.out.println("OURPORT"+ OurPort);
                System.out.println("RECEIVE" + receiveMessage.getMembersSend().size() +  receiveMessage.getSenderPort() + "ID "+ receiveMessage.getSeqNo() + "starting "+ receiveMessage.getStartingSender()    );
                if(whatIsendTotal.size() == 0 && receiveMessage.getMembersSend().size() != 0 && receiveMessage.getSenderPort() != OurPort){
                    System.out.println("mphka edw");
                    receiveMessage.setSenderPort(OurPort);
                    BM_send(receiveMessage,receiveMessage.getGroupId());
                }
            }
//            return;
        }
        else if(receiveMessage.getTotaldelivered() >= 0){
            if(!rids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
//                System.out.println("RESends Buffer");
                int i=0;
                for(; i < resend.size();i++){
                    if(resend.get(i).getSeqNo() == receiveMessage.getSeqNo() && resend.get(i).getStartingSender() == receiveMessage.getStartingSender()){ // an to sequenve number einai idio kai to Starting sender einai idio tote prokeite gia to idio mhnuma
                        int l = -1;
                        for (int j = 0; j < resend.get(i).getMembersSend().size(); j++) {
                            if (resend.get(i).getMembersSend().get(j).equals(receiveMessage.getSenderPort())) {
                                l = j;
//                                System.out.println("to ebgala");
                                break;
                            }
                        }
                        if (l >=0) {
                            resend.get(i).getMembersSend().remove(l);
                        }
                        if (receiveMessage.getSenderPort() != OurPort) {
                            receiveMessage.setSenderPort(OurPort);
                            int z = -1;
                            for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                                if(receiveMessage.getMembersSend().get(j) == OurPort){
                                    z = j;
                                }
                            }
                            if(z >=0){
                                receiveMessage.getMembersSend().remove(z);
                            }
                            BM_send(receiveMessage, receiveMessage.getGroupId());
                        }
                        hold = 1;
                        if (resend.get(i).getMembersSend().size() == 0) {
//                                hold = i;
                            re++;
                            Message rec = new Message("", receiveMessage);
                            receiveMiddle.add(rec);
                            delete = 1;
                            rids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                            break;
                        }
                    }
                }
                if(hold == 0){
//
                    UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),receiveMessage.getTotaldelivered());
//
                    for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                        sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
                    }
//                    for(int counter = 0;counter < sendAddition.getMembersSend().size();counter++){
//                        System.out.println("Sends "+ sendAddition.getMembersSend().get(counter));
//                    }
                    resend.add(sendAddition);

                    int size = resend.size();
                    int p = -1;
                    for (int counter = 0; counter < resend.get(size - 1).getMembersSend().size(); counter++) {
                        if (resend.get(size - 1).getMembersSend().get(counter) == receiveMessage.getSenderPort()) {
//                            System.out.println("PAW NA TO BGALW");
                            p = counter;
                        }
                    }

                    if (p >= 0) {
                        resend.get(size - 1).getMembersSend().remove(p);
                    }
                    if (receiveMessage.getSenderPort() != OurPort) {
                        receiveMessage.setSenderPort(OurPort);
                        int z = -1;
                        for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                            if(receiveMessage.getMembersSend().get(j) == OurPort){
                                z = j;
                            }
                        }
                        if(z >=0){
                            receiveMessage.getMembersSend().remove(z);
                        }
                        BM_send(receiveMessage, receiveMessage.getGroupId());
                    }
                    if (resend.get(size - 1).getMembersSend().size() == 0) {
                        re++;
                        Message rec = new Message("", receiveMessage);
                        receiveMiddle.add(rec);
                        rids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                        delete = 1;
                    }
                }
                if(delete == 1){
                    resend.remove(i);
                    int p = -1;
                    for(int j = 0;j <whatIsendTotal.size();j++){
                        if(whatIsendTotal.get(j).getSeqNo() == receiveMessage.getSeqNo()){
                            p = j;
                        }
                    }
                    if(p >= 0){
                        whatIsendTotal.remove(p);
                    }
                    int l = -1;
                    for(int j = 0;j <coordBuffer.size();j++){
                        if(coordBuffer.get(j).getSeqNo() == receiveMessage.getSeqNo() && coordBuffer.get(j).getSenderPort() == receiveMessage.getSenderPort()){
                            l = j;
                        }
                    }
                    if(l >= 0){
                        coordBuffer.remove(l);
                    }
                }
            }

            if(rids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
                System.out.println("OURPORT"+ OurPort);
                System.out.println("RECEIVE" +" SIZE: "+ receiveMessage.getMembersSend().size() + " "+ receiveMessage.getSenderPort() + "ID "+ receiveMessage.getSeqNo() + "starting "+ receiveMessage.getStartingSender()    );
                for(int i =0; i < receiveMessage.getMembersSend().size();i++){
                    System.out.println("TA MELH"+ receiveMessage.getMembersSend().get(i));
                }
                if(whatIsendTotal.size() == 0 && receiveMessage.getMembersSend().size() != 0 && receiveMessage.getSenderPort() != OurPort){
                    System.out.println("mphka edw");
                    receiveMessage.setSenderPort(OurPort);
                    for(int i =0; i < receiveMessage.getMembersSend().size();i++){
                        System.out.println(receiveMessage.getMembersSend().get(i));
                    }
                    BM_send(receiveMessage,receiveMessage.getGroupId());
                }

            }
        }

        if(mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo()) || delete == 1){
            int k = -1;
            for(int counter =0;counter < receiveMiddle.size();counter++){
                if(receiveMiddle.get(counter).getMessage().getSeqNo() == receiveMessage.getSeqNo() && receiveMiddle.get(counter).getMessage().getSenderPort() == receiveMessage.getSenderPort()){
                    k = counter;
                }
            }
            if(k >= 0){
                receiveMiddle.remove(k);
            }
        }

        System.out.println("OK TOTAL: " +se);
        System.out.println("OK  RESEND TOTAL: " +re);

        System.out.println("BUffer SIZE TOTAL:"+ sendsBuffer.size());
        System.out.println("BUffer resends TOTAL " + resend.size());
        System.out.println("WHAT I SEND SIZE TOTAL"+ whatIsendTotal.size());
        System.out.println("Coord  SIZE TOTAL"+ coordBuffer.size());


        if(!totalbufids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
//            System.out.println("GOing here");
            totalbufids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
            Message rec = new Message("", receiveMessage);
            totalbuf.add(rec);
            for(int j = 0;j < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                if(receiveMessage.getStartingSender() == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                    rec.setName(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getName());
                }
            }
        }
        else {
            if(receiveMessage.getTotaldelivered() >= 0) {
                System.out.println("Took Total deliver");
                for(int counter = 0;counter < totalbuf.size();counter++){
                    if(receiveMessage.getSeqNo() == totalbuf.get(counter).getMessage().getSeqNo()){
                        totalbuf.get(counter).getMessage().setTotaldelivered(receiveMessage.getTotaldelivered());
                    }
                }
                int p = -1;
                for(int j = 0;j <whatIsendTotal.size();j++){
//                        System.out.println(whatIsendTotal.get(j).getMessage());
                    if(whatIsendTotal.get(j).getSeqNo() == receiveMessage.getSeqNo()){
                        p = j;
                    }
                }
                if(p >= 0){
//                        System.out.println("P" + p);
                    whatIsendTotal.remove(p);
                }
            }
        }


        checkTotalBuffer();
    }
    public void checkTotalBuffer(){
        while(true){
            int j = -1;
            for(int i = 0;i<totalbuf.size();i++){
//                System.out.println("MHNTMATA ME THN SEIRA PARADOSHS: " + totalbuf.get(i).getMessage().getSelfDelivered());
//                if(totaldelivered == 0 ){
//                    System.out.println("TO PROSPERASA");
//                    totaldelivered = totaldelivered + 1;
//                    break;
//                }
//                if(){
//                    continue;
////                }
//                System.out.println("MPhka + totalDleiver" + totaldelivered);
//                System.out.println("μηνυμα + totalDleiver" + totalbuf.get(i).getMessage().getTotaldelivered());

//                System.out.println(totalbuf.get(i).getMessage().getTotaldelivered());
                if(totalbuf.get(i).getMessage().getTotaldelivered() == totaldelivered){
                    System.out.println(totalbuf.get(i).getMessage().getTotaldelivered() + "SEIRA PARADWSHS");
                    appids.get(totalbuf.get(i).getMessage().getStartingSender()).add(totalbuf.get(i).getMessage().getSeqNo());
                    groupMessages.get(totalbuf.get(i).getMessage().getGroupId()).getMsgs().add(totalbuf.get(i));
                    totaldelivered++;
                    j = i;
                    //eidopoiw na ksemplokaroun ta melos
                    synchronized (lock2){
                        lock2.notify();
                    }
                }
            }
            if(j >= 0){
                totalbuf.remove(j);
            }
            if(j < 0){
                break;
            }
        }
    }
    public void  BM_deliver(UdpMessage receiveMessage){
        int delete = 0;
        int hold = 0;
        int exist = -1;


        if(!mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())) {
//            flag = 1;
            int i = 0;
            if (sendsBuffer.size() > 0) {
                for (; i < sendsBuffer.size(); i++) {
                    if (sendsBuffer.get(i).getSeqNo() == receiveMessage.getSeqNo() && sendsBuffer.get(i).getStartingSender() == receiveMessage.getStartingSender()) {
                        int l = -1;
                        for (int j = 0; j < sendsBuffer.get(i).getMembersSend().size(); j++) {
//                                System.out.println(sendsBuffer.get(i).getMembersSend().get(j));
                            if (sendsBuffer.get(i).getMembersSend().get(j).equals(receiveMessage.getSenderPort())) {
                                l = j;
                                break;
                            }
                        }
                        if (l >=0) {
                            sendsBuffer.get(i).getMembersSend().remove(l);

                        }
                        hold = 1;
                        if(l < 0 && sendsBuffer.get(i).getMembersSend().size() == 0){
                            Message rec = new Message("", receiveMessage);
                            se++;
                            System.out.println("Took ack from everyone: "+ receiveMessage.getSeqNo() +" "+ receiveMessage.getMessage() + " " +receiveMessage.getStartingSender());
                            receiveMiddle.add(rec);
                            delete = 1;
                            mids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                            break;
                        }
                        if (receiveMessage.getSenderPort() != OurPort) {
                            receiveMessage.setSenderPort(OurPort);
                            int z = -1;
                            for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                                if(receiveMessage.getMembersSend().get(j) == OurPort){
                                    z = j;
                                }
                            }
                            if(z >=0){
                                receiveMessage.getMembersSend().remove(z);
                            }
//                            UdpMessage resend = new UdpMessage(sendsBuffer.get(i).getMessage(),sendsBuffer.get(i).getSeqNo(),OurPort,sendsBuffer.get(i).getGroupId(),sendsBuffer.get(i).getStartingSender(),sendsBuffer.get(i).getTotal(),sendsBuffer.get(i).getSelfDelivered(),sendsBuffer.get(i).getTotaldelivered());
//                            resend.setMembersSend(sendsBuffer.get(i).getMembersSend());
                            BM_send(receiveMessage, receiveMessage.getGroupId());
                        }
                        hold = 1;
                        if (sendsBuffer.get(i).getMembersSend().size() == 0) {
                            se++;
                            System.out.println("Took ack from everyone: "+ receiveMessage.getSeqNo() +" "+ receiveMessage.getMessage() + " " +receiveMessage.getStartingSender());
                            Message rec = new Message("", receiveMessage);
                            receiveMiddle.add(rec);
                            delete = 1;
                            mids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                            break;
                        }
                    }
                }
            }
            if (hold == 0) {
//                System.out.println("PRWTH FORA");
                UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),-1);
//
                for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                    sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
                }
//                for(int counter = 0;counter < sendAddition.getMembersSend().size();counter++){
//                    System.out.println("Sends "+ sendAddition.getMembersSend().get(counter));
//                }
                sendsBuffer.add(sendAddition);

                int size = sendsBuffer.size();
                int p = -1;
                for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                    if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == receiveMessage.getSenderPort()) {
//                        System.out.println("PAW NA TO BGALW");
                        p = counter;
                    }
                }
                if (p >= 0) {
                    sendsBuffer.get(size - 1).getMembersSend().remove(p);
                }
                int a =-1;
                for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                    if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == OurPort) {
//                        System.out.println("PAW NA TO BGALW");
                        a = counter;
                    }
                }
                if(a >=0){
                    sendsBuffer.get(size - 1).getMembersSend().remove(a);
                }

                if (receiveMessage.getSenderPort() != OurPort) {
                    receiveMessage.setSenderPort(OurPort);
                    int z = -1;
                    for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                        if(receiveMessage.getMembersSend().get(j) == OurPort){
                            z = j;
                        }
                    }
                    if(z >=0){
                        receiveMessage.getMembersSend().remove(z);
                    }
                    BM_send(receiveMessage, receiveMessage.getGroupId());
                }
                if (sendsBuffer.get(size - 1).getMembersSend().size() == 0) {
                    se++;
                    System.out.println("Took ack from everyone: "+ receiveMessage.getSeqNo() +" "+ receiveMessage.getMessage() + " " +receiveMessage.getStartingSender());
                    Message rec = new Message("", receiveMessage);
                    receiveMiddle.add(rec);
                    mids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
                    delete = 1;
                }
            }
            if(delete == 1){
                sendsBuffer.remove(i);
                int p = -1;
                for(int j = 0;j <whatIsend.size();j++){
//                    System.out.println("PSAXNW");
                    if(whatIsend.get(j).getSeqNo() == receiveMessage.getSeqNo() && whatIsend.get(j).getStartingSender() == receiveMessage.getStartingSender()){
                        p = j;
                    }
                }
                if(p >= 0){
                    System.out.println("to brhka to afairw");
//                    System.out.println("TO DIAGRAFW APO TO WHATISEND" + p);
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
//                System.out.println("TO diagrafw apo to middle");
                receiveMiddle.remove(k);
            }
        }


        if(mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
            System.out.println("OURPORT"+ OurPort);
            System.out.println("RECEIVE" + receiveMessage.getMembersSend().size() +  receiveMessage.getSenderPort() + "ID "+ receiveMessage.getSeqNo() + "starting "+ receiveMessage.getStartingSender()    );
            if(sendsBuffer.size() == 0 && whatIsend.size() == 0 && receiveMessage.getMembersSend().size() != 0 && receiveMessage.getSenderPort() != OurPort){
                System.out.println("mphka edw");
                receiveMessage.setSenderPort(OurPort);
                BM_send(receiveMessage,receiveMessage.getGroupId());
            }
        }


        System.out.println("OK: " +se);
        System.out.println("BUffer SIZE :"+ sendsBuffer.size());
        System.out.println("WHAT I SEND SIZE"+ whatIsend.size());
//

        if(!mbufids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
            mbufids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo()); //prostikh ston buffer tou middleware gia auta poy perimenoume na pane sthn efarmogh

//            UdpMessage newUdp = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),-1);
//
//            for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
//                newUdp.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
//            }
            Message rec = new Message("", receiveMessage);
//            System.out.println("TO PROSTHESA STON Buffer");
            mbuf.add(rec);
//            se++;
//            System.out.println(whatIsend.size());
//            System.out.println(se);
            for(int j = 0;j < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                if(receiveMessage.getStartingSender() == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                    rec.setName(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getName());
                }
            }
        }
        GroupMessages group = groupMessages.get(receiveMessage.getGroupId());
//        System.out.println("PAW NA TSEKARW CHECK FIFO BUFFER");
        checkFifoBuffer(group);
    }
    public void checkFifoBuffer(GroupMessages group) {
        int j = -1;
        Set<Integer> keys = group.getFifoOrders().keySet();
        for (Integer temp : keys) {
            while(true){
//                System.out.println("SIZE OF BUFFER"+mbuf.size());
                for (int i = 0; i < mbuf.size(); i++) {
//                System.out.println("MHNTMATA ME THN SEIRA PARADOSHS: " + mbuf.get(i).getMessage().getSelfDelivered());
//                if(delivered == 0 ){
//                    System.out.println("TO PROSPERASA");
//                    delivered = delivered + 1;
//                    break;
//                }
//                    System.out.println("TEMP"+ temp);
//                    System.out.println(("startingSender" + mbuf.get(i).getMessage().getStartingSender()+ " mhnuma " + mbuf.get(i).getMessage().getMessage() + mbuf.get(i).getName() + mbuf.get(i).getMessage().getSeqNo()));
                    if(mbuf.get(i).getMessage().getStartingSender() == temp){
//                        System.out.println("group Getfifo temp"+ temp+ " " + group.getFifoOrders().get(temp));
//                        System.out.println("mbuf.get(i).getMessage().getSelfDelivered()" + mbuf.get(i).getMessage().getSelfDelivered());

                        if (mbuf.get(i).getMessage().getSelfDelivered() == group.getFifoOrders().get(temp)) {
//                            System.out.println(group.getFifoOrders().get(temp) + "SEIRA/ PARADWSHS" + temp);
                            groupMessages.get(mbuf.get(i).getMessage().getGroupId()).getMsgs().add(mbuf.get(i));
//                            System.out.println("MEGETHOS"+ groupMessages.get(mbuf.get(i).getMessage().getGroupId()).getMsgs().size());
//
//                            System.out.println("TO PERIEXOMENO: "+ groupMessages.get(mbuf.get(i).getMessage().getGroupId()).getMsgs().get(0));

                            int l = group.getFifoOrders().get(temp);
                            l++;
                            group.getFifoOrders().put(temp,l);
                            j = i;
                            synchronized (lock2){
                                lock2.notify();
                            }
                            break;
                        }
                    }
                }
                if (j >= 0) {
                    mbuf.remove(j);
                    j = -1;
                }
                else if (j < 0){
//                    System.out.println("MPHKA EDw");
                    break;
                }


            }
        }
    }

//    public UdpMessage removeList()
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