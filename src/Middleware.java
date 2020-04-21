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
    int globalchoice;
    int globalchoiceWhatI;
    int globalchoicewhatItotal;
    int globalchoiceresends;
    int globalchoicecoord;

    List<Message> receiveMiddle; //for both reliability
    List<UdpMessage> sendsBuffer; //for both RM Messages
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
    HashMap <Integer,List<Integer>> rids;
    HashMap <Integer,List<Integer>> appids;
    int receiveMessages;
    int delivered;
    int sended;
    int totaldelivered; //gia to TOTAL;
    int amICoordinator;
    int allMessages;
    List<Message> mbuf; //FOR FIFO endiamesos buffer
    List<Message> totalbuf;//FOR TOTAL endiamesos buffer
    HashMap <Integer,List<Integer>> mbufids; //ta ids tou endiamesou buffer
    HashMap <Integer,List<Integer>> totalbufids;
    HashMap <Integer,List<Integer>> idsCoord;
    HashMap <Integer,Integer> sendedFIFO;

    int counterUDP;
    int gSock;
    public Middleware() {
        lock2 = new Object();
        InfoManager = new GroupManagerInfo(null);
        globalchoice = 0;
        globalchoicewhatItotal = 0;
        globalchoicecoord =0;
        globalchoiceresends = 0;
        totalbuf=new ArrayList<>();
        counterUDP = 0;
        totalbufids = new HashMap<>();
        receiveMessages = 0;
        groupMessages = new HashMap<>();
        rids = new HashMap<>();
        discoverGroupManager();
        se =0;
        globalchoiceWhatI = 0;
        re = 0;
        totaldelivered = 0;
        coordBuffer = new ArrayList<>();
        idsCoord = new HashMap<>();
        sendedFIFO = new HashMap<>();
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
            sendMsgFromSocket(InfoManager.getCommunicationSock(),myInformation);

            while(true){
                if(gSock != 0){
                    break;
                }
                synchronized (lock) {
                    lock.wait();
                }
            }
//            S
            firstView.setView(middlewareTeamsBuffer.get(gSock));

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

        sendMsgFromSocket(InfoManager.getCommunicationSock(),msg);

        middlewareTeamsBuffer.remove(gSock);
        groupMessages.remove(gSock);

        return 1;
    }

    @Override
    public int grp_send(int gSock, String msg, int len, int total) {

        UdpMessage newMessage = null;
        seqNumber++;
        //For Measurements
//        long start = System.currentTimeMillis();
//        System.out.println("START " + seqNumber+ "  :" + start);
//        File file = new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/starts4");
//
//        try {
//            FileWriter fstream = new FileWriter(file, true);
//            BufferedWriter out = new BufferedWriter(fstream);
//            String news = new String(String.valueOf(start));
//            System.out.println(news);
//            out.append(news+ "\n");
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if(total == 0){
            int num = sendedFIFO.get(gSock);
            num++;
            newMessage = new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort,total,num,-1);
            sendedFIFO.put(gSock,num);
        }
        else{
            newMessage = new UdpMessage(msg,seqNumber,OurPort,gSock,OurPort,total,-1,-1);
        }


        Iterator<EachMemberInfo> it = middlewareTeamsBuffer.get(gSock).getMembers().iterator();

        while(it.hasNext()){
            EachMemberInfo temp = it.next();
            newMessage.getMembersSend().add(temp.getMemberPort());
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
            while(groupMessages.get(gSock).getMsgs().size()  <= 0) {
                synchronized (lock2) {
                    try {
                        lock2.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(groupMessages.containsKey(gSock)){
            while (true) {
                if (groupMessages.get(gSock).getMsgs().size() > 0) {
                    Message nextMsg = groupMessages.get(gSock).getMsgs().get(0);

                    if(nextMsg == null){
                        continue;
                    }
                    if (nextMsg.getMessage() != null) {
                        receiveMsg.getMessage().setSenderPort(nextMsg.getMessage().getSenderPort());
                        receiveMsg.getMessage().setGroupId(nextMsg.getMessage().getGroupId());
                        receiveMsg.getMessage().setMessage(nextMsg.getMessage().getMessage());
                        receiveMsg.getMessage().setSeqNo(nextMsg.getMessage().getSeqNo());
                        receiveMsg.getMessage().setSenderPort(nextMsg.getMessage().getSenderPort());
//
                        receiveMsg.getMessage().setSelfDelivered(nextMsg.getMessage().getSelfDelivered());
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
                else {
                    break;
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
            newView = (Message) in.readObject();
        }
        catch (SocketTimeoutException ex) {
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
//            System.out.println("\r\nMessage from " + socket.getInetAddress().getHostAddress() + ": " + data);
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
//            System.out.println("Trying to discover Group Manager...");
            Discovery = new DatagramSocket();
//            System.out.println(Discovery.getLocalPort());
            OurPort = Discovery.getLocalPort();
            int i = 0;
            while(i!=4){
                byte[] msg =  new byte[1024];
                packet = new DatagramPacket(msg,msg.length, InetAddress.getByName(MultiCastGroupAddress),MultiCastPort);
                Discovery.send(packet);
//                System.out.println("Sending packet");
//                System.out.println("Wait Group Manager to Answer");
                Discovery.setSoTimeout(2000);
                try {
                    Discovery.receive(packet);
                    break;
                }catch (SocketTimeoutException ex){
//                    System.out.println("Trying again..");
                    i++;
                }
            }
            if(i == 4){
                checkValue =0;
                return ;
            }
            String msg1 = new String(packet.getData(), packet.getOffset(), packet.getLength());
            String []splitMsg = msg1.split(" ",2);
//            System.out.println("Discovery was successful"+ splitMsg[0]+" "+ splitMsg[1]);
            OursAddress =splitMsg[0].replace("/","");
//            Thread.sleep(5000);
            Socket CommunicationChannel = new Socket(packet.getAddress(),Integer.parseInt(splitMsg[1]));

//            System.out.println("We will communicate with Group Manager at Address +" + CommunicationChannel.getInetAddress().getHostAddress() + "Port:"+ CommunicationChannel.getPort());

            this.InfoManager.setCommunicationSock(CommunicationChannel);

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
        public static final String RESET = "\033[0m";  // Text Reset

        @Override
        public void run() {
            System.out.println(RESET);
            while(true){
                if(currentThread().isInterrupted()){
                    System.out.println("The app is closing");
                    return;
                }
                    Message newGroup2 = (Message) getViewFromSocket(InfoManager.getCommunicationSock());

                    if(!newGroup2.getType().equals("noMessage")){

                        teams = newGroup2.getView().getId();

                        if(!middlewareTeamsBuffer.containsKey(teams)){
                            //Se periptwsh pou erthei neo view dhmiourgoume tous antistoixous buffers pou xreiazomaste
                            gSock = teams;
                            GroupMessages group = new GroupMessages();
//                            ArrayList<Integer> fifo = new ArrayList<>();//gia ton kathena prosthetei lista me ta ids twn mhnymatwn

                            sendedFIFO.put(teams,-1);

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
                            if(newGroup2.getView().getCoInfo().getCoMember().getMemberPort() == OurPort){
                                amICoordinator = 1;
                            }
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
                                    //Buffers poy kratane ta id twn paketwn pou exoume parei
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


                        }
                        synchronized (lock2){
                            lock2.notify();
                        }
                        middlewareTeamsBuffer.put(teams,newGroup2.getView());


                        afterchangeview(newGroup2.getView()); // Afou erthei allagh sto view koitame mhpws kapoios efyge apo ta paketa pou exoume hdh parei gia na ton svhsoume
//
                        int getout = -1;
                        while(getout != 0){
                            getout = 0;
                            for(int counter =0;counter < sendsBuffer.size();counter++){
                                if(sendsBuffer.get(counter).getMembersSend().size() == 0){
                                    getout = -1;
                                    sendsBuffer.remove(counter);
                                    break;
                                }
                            }
                            if(sendsBuffer.size() == 0) {
                                getout = 0;
                            }
                        }

                        synchronized (lock){
                            lock.notify();
                        }
                    }
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
                            int l =-1;
                            for(int i = 0;i < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();i++){
                                if(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(i).getMemberPort() == receiveMessage.getSenderPort()){
                                    l++;
                                }
                            }
                            if(l<0){
//                                System.out.println("LATHOS APOSTOLEAS");
                                continue;
                            }

                            if(receiveMessage.getMembersSend().size() > middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size()){
                                int k =-1;
                                int size = 0;
//                                System.out.println("PERAF"+ receiveMessage.getMembersSend().size());
                                for(int i =0;i < receiveMessage.getMembersSend().size(); i++ ){
                                    for(int j = 0; j <middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                                        if(receiveMessage.getMembersSend().get(i) != middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                                            size++;
                                        }
                                    }

                                    if(size == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size()){
                                        k = i;
                                        break;
                                    }
                                    size =0;
                                }

                                if(k >=0){
//                                    System.out.println("MPHKA EDW");
                                    receiveMessage.getMembersSend().remove(k);
                                }
                            }

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
//                                            System.out.println("EIMAI O COORDINATOR ///BAZW TTON EPOEMNO SEIRIAKO ARITHMO DELIVER :"+ deliver);
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
                        }

                        catch (SocketTimeoutException e) {
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
                                        BM_send(whatIsendTotal.get(0),whatIsendTotal.get(0).getGroupId());

                                    }
                                    continue;
                                }
                                else{
                                    if(globalchoicewhatItotal >= whatIsendTotal.size()){
                                        globalchoicewhatItotal = 0;
                                    }
                                    BM_send(whatIsendTotal.get(globalchoicewhatItotal),whatIsendTotal.get(globalchoicewhatItotal).getGroupId());
                                    globalchoicewhatItotal++;
                                }
                            }
                            else if(whatIsend.size() > 0){
//                                System.out.println("KSANATSEKARW TO what i send");

//                                System.out.println("WHAT I SEND timout exception");
                                GroupInfo lastview = middlewareTeamsBuffer.get(whatIsend.get(0).getGroupId());

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
                                    if(globalchoiceWhatI >= whatIsend.size()){
                                        globalchoiceWhatI = 0;
                                    }

                                    BM_send(whatIsend.get(globalchoiceWhatI),whatIsend.get(globalchoiceWhatI).getGroupId());
                                    globalchoiceWhatI++;
                                }
                            }
                            else if(sendsBuffer.size() > 0 ){

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
//                                    System.out.println("sto sendsBUffer");
                                    if(globalchoice >= sendsBuffer.size()){
                                        globalchoice = 0;
                                    }
                                    sendsBuffer.get(globalchoice).setSenderPort(OurPort);
                                    System.out.println(sendsBuffer.get(globalchoice).getSeqNo());
                                    BM_send(sendsBuffer.get(globalchoice),sendsBuffer.get(globalchoice).getGroupId());
                                    globalchoice++;
                                }
                            }
                            else if(coordBuffer.size() > 0){
//                                System.out.println("stelnw pali coord");
                                if(globalchoicecoord >= coordBuffer.size()){
                                    globalchoicecoord = 0;
                                }
                                coordBuffer.get(globalchoicecoord).setSenderPort(OurPort);
                                BM_send(coordBuffer.get(globalchoicecoord),coordBuffer.get(globalchoicecoord).getGroupId());
                                globalchoicecoord++;
//                                for(int i = 0; i <coordBuffer.size();i++){
//                                    BM_send(coordBuffer.get(i),coordBuffer.get(i).getGroupId());
//                                }
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
//                                    System.out.println("sto ResendBuffer"+ resend.get(0).getTotaldelivered());
                                    if(globalchoiceresends >= resend.size()){
                                        globalchoiceresends = 0;
                                    }
                                    resend.get(globalchoiceresends).setSenderPort(OurPort);
                                    BM_send(resend.get(globalchoiceresends),resend.get(globalchoiceresends).getGroupId());
                                    globalchoiceresends++;
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
                    int size = view.getMembers().size();
                    int k = 0;
                    int sizeSends = sendsBuffer.get(counter).getMembersSend().size();
//                    System.out.println("Mphke" + sizeSends);

                    while(sizeSends >= 0){
                        int delete = -1;
                        for(int i = 0; i<sendsBuffer.get(counter).getMembersSend().size();i++) {
                            sizeSends--;
                            for (int j = 0; j < view.getMembers().size(); j++) {
                                if (sendsBuffer.get(counter).getMembersSend().get(i) != view.getMembers().get(j).getMemberPort()) {
                                    k++;
                                }
                            }
                            if (k == size) {
                                k = 0;
                                sendsBuffer.get(counter).getMembersSend().remove(i);
                                if (sendsBuffer.get(counter).getMembersSend().size() == 0) {
                                    se++;
//                                    System.out.println("Took ack from everyone: " + sendsBuffer.get(counter).getSeqNo() + " " + sendsBuffer.get(counter).getMessage() + " " + sendsBuffer.get(counter).getStartingSender());
                                    Message rec = new Message("", sendsBuffer.get(counter));
                                    receiveMiddle.add(rec);
                                    delete = 1;
                                    mids.get(sendsBuffer.get(counter).getStartingSender()).add(sendsBuffer.get(counter).getSeqNo());
                                }
                                if (delete == 1) {
                                    int p = -1;
                                    for (int j = 0; j < whatIsend.size(); j++) {
                                        if (whatIsend.get(j).getSeqNo() == sendsBuffer.get(counter).getSeqNo() && whatIsend.get(j).getStartingSender() == sendsBuffer.get(counter).getStartingSender()) {
                                            p = j;
                                        }
                                    }
                                    if (p >= 0) {
                                        whatIsend.remove(p);
                                    }
                                }
                            }
                            if(delete == 1){
                                sizeSends = -1;
                                break;
                            }
                            k = 0;
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
                    int sizeresends = resend.get(counter).getMembersSend().size();
                    while(sizeresends >= 0){
                        int delete = -1;
                        for(int i =0;i<resend.get(counter).getMembersSend().size();i++){
                            sizeresends--;
                            for(int j =0;j < view.getMembers().size();j++){
                                if(resend.get(counter).getMembersSend().get(i) != view.getMembers().get(j).getMemberPort()){
                                    k++;
                                }
                            }
                            if(k == size){
                                k =0;
                                resend.get(counter).getMembersSend().remove(i);
                                if (resend.get(i).getMembersSend().size() == 0) {
//                                hold = i;
                                    re++;
                                    Message rec = new Message("", resend.get(counter));
                                    receiveMiddle.add(rec);
                                    delete = 1;
                                    rids.get(resend.get(counter).getStartingSender()).add(resend.get(counter).getSeqNo());
                                }
                            }
                            if (delete == 1) {
                                int p = -1;
                                for (int j = 0; j < whatIsendTotal.size(); j++) {
                                    if (whatIsendTotal.get(j).getSeqNo() == resend.get(counter).getSeqNo() && whatIsendTotal.get(j).getStartingSender() == resend.get(counter).getStartingSender()) {
                                        p = j;
                                    }
                                }
                                if (p >= 0) {
                                    whatIsendTotal.remove(p);
                                }
                            }
                            k=0;
                            if(delete == 1){
                                sizeresends = -1;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    public void  BM_send(UdpMessage msg,int groupId){
        DatagramSocket UdpSocket = null;

        Iterator<EachMemberInfo> it2 = middlewareTeamsBuffer.get(groupId).getMembers().iterator();

        while(it2.hasNext()){
            EachMemberInfo temp = it2.next();

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(msg);

                byte[] byteMsg = baos.toByteArray();
                int send = -1;
                for(int i =0; i < msg.getMembersSend().size();i++){
                    if(msg.getMembersSend().get(i) == temp.getMemberPort()){
//                        System.out.println("SEND PORT"+ temp.getMemberPort());
                        DatagramPacket packet2 = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName(temp.getMemberAddress()), temp.getMemberPort());
                        counterUDP++;
                        Discovery.send(packet2);
                    }
                }

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

        if(receiveMessage.getTotaldelivered() < 0 ){
            if(!mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())) {
                int i = 0;
                if (sendsBuffer.size() > 0) {
                    for (; i < sendsBuffer.size(); i++) {
                        if (sendsBuffer.get(i).getSeqNo() == receiveMessage.getSeqNo() && sendsBuffer.get(i).getStartingSender() == receiveMessage.getStartingSender()) {
                            int l = -1;
                            for (int j = 0; j < sendsBuffer.get(i).getMembersSend().size(); j++) {
                                if (sendsBuffer.get(i).getMembersSend().get(j) == receiveMessage.getSenderPort()) {
                                    l = j;
                                    break;
                                }
                            }
                            if (l >=0) {
                                sendsBuffer.get(i).getMembersSend().remove(l);
                            }

                            if (receiveMessage.getSenderPort() != OurPort) {
                                  receiveMessage.setSenderPort(OurPort);
                                if(sendsBuffer.get(i).getMembersSend().size() == 0){
                                    receiveMessage.setDontSendAgain(1);
                                }
                                int our = -1;
                                for (int counter =0; counter < receiveMessage.getMembersSend().size();counter++){
                                    if(receiveMessage.getMembersSend().get(counter) == OurPort){
                                        our = counter;
                                    }
                                }
                                if(our >= 0){
                                    receiveMessage.getMembersSend().remove(our);
                                }
                                int sender = -1;
                                for (int counter =0; counter < receiveMessage.getMembersSend().size();counter++){
                                    if(receiveMessage.getMembersSend().get(counter) == receiveMessage.getSenderPort()){
                                        sender = counter;
                                    }
                                }
                                if(sender >= 0){
                                    receiveMessage.getMembersSend().remove(sender);
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
                    int a =-1;
                    for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                        if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == OurPort) {
                            a = counter;
                        }
                    }
                    if(a >=0){
                        sendsBuffer.get(size - 1).getMembersSend().remove(a);
                    }
                    if (receiveMessage.getSenderPort() != OurPort) {
                        receiveMessage.setSenderPort(OurPort);
//
                        if(sendsBuffer.get(i).getMembersSend().size() == 0){
                            receiveMessage.setDontSendAgain(1);
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
                    int p = -1;
                    for(int j = 0;j <whatIsendTotal.size();j++){
                        if(whatIsendTotal.get(j).getSeqNo() == receiveMessage.getSeqNo() && whatIsendTotal.get(j).getStartingSender() == receiveMessage.getStartingSender()){
                            p = j;
                        }
                    }
                    if(p >= 0){
                        whatIsendTotal.remove(p);
                    }
                }
            }
            else if(mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())) {
                if (receiveMessage.getSenderPort() != OurPort && receiveMessage.getDontSendAgain() == -1) {
                    int z = -1;
                    for (int j = 0; j < receiveMessage.getMembersSend().size(); j++) {
                        if (receiveMessage.getMembersSend().get(j) == OurPort) {
                            z = j;
                        }
                    }
                    if (z >= 0) {
                        receiveMessage.getMembersSend().remove(z);

                    }
                    while (receiveMessage.getMembersSend().size() > 0) {
                        receiveMessage.getMembersSend().remove(0);
                    }
                    receiveMessage.getMembersSend().add(receiveMessage.getSenderPort());
                    receiveMessage.setSenderPort(OurPort);
                    receiveMessage.setDontSendAgain(1);
                    BM_send(receiveMessage, receiveMessage.getGroupId());
                }
            }
        }
        else if(receiveMessage.getTotaldelivered() >= 0){
            if(!rids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
                int i=0;
                for(; i < resend.size();i++){
                    if(resend.get(i).getSeqNo() == receiveMessage.getSeqNo() && resend.get(i).getStartingSender() == receiveMessage.getStartingSender()){ // an to sequenve number einai idio kai to Starting sender einai idio tote prokeite gia to idio mhnuma
                        int l = -1;
                        for (int j = 0; j < resend.get(i).getMembersSend().size(); j++) {
                            if (resend.get(i).getMembersSend().get(j) == receiveMessage.getSenderPort()) {
                                l = j;
                                break;
                            }
                        }
                        if (l >=0) {
                            resend.get(i).getMembersSend().remove(l);
                        }

                        if (receiveMessage.getSenderPort() != OurPort) {
                            receiveMessage.setSenderPort(OurPort);
//
                            if(resend.get(i).getMembersSend().size() == 0){
                                receiveMessage.setrMessageDontSend(1);
                            }
                            int our = -1;
                            for (int counter =0; counter < receiveMessage.getMembersSend().size();counter++){
                                if(receiveMessage.getMembersSend().get(counter) == OurPort){
                                    our = counter;
                                }
                            }
                            if(our >= 0){
                                receiveMessage.getMembersSend().remove(our);
                            }
                            int sender = -1;
                            for (int counter =0; counter < receiveMessage.getMembersSend().size();counter++){
                                if(receiveMessage.getMembersSend().get(counter) == receiveMessage.getSenderPort()){
                                    sender = counter;
                                }
                            }
                            if(sender >= 0){
                                receiveMessage.getMembersSend().remove(sender);
                            }
                            BM_send(receiveMessage, receiveMessage.getGroupId());
                        }
                        hold = 1;
                        if (resend.get(i).getMembersSend().size() == 0) {
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
                    UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),receiveMessage.getTotaldelivered());

                    for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                        sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
                    }

                    resend.add(sendAddition);

                    int size = resend.size();
                    int p = -1;
                    for (int counter = 0; counter < resend.get(size - 1).getMembersSend().size(); counter++) {
                        if (resend.get(size - 1).getMembersSend().get(counter) == receiveMessage.getSenderPort()) {
                            p = counter;
                        }
                    }

                    if (p >= 0) {
                        resend.get(size - 1).getMembersSend().remove(p);
                    }
                    int a =-1;
                    for (int counter = 0; counter < resend.get(size - 1).getMembersSend().size(); counter++) {
                        if (resend.get(size - 1).getMembersSend().get(counter) == OurPort) {
                            a = counter;
                        }
                    }
                    if(a >=0){
                        resend.get(size - 1).getMembersSend().remove(a);
                    }
                    if (receiveMessage.getSenderPort() != OurPort) {
                        receiveMessage.setSenderPort(OurPort);
//
                        if(resend.get(i).getMembersSend().size() == 0){
                            receiveMessage.setrMessageDontSend(1);
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

                    int l = -1;
                    for(int j = 0;j <coordBuffer.size();j++){
                        if(coordBuffer.get(j).getSeqNo() == receiveMessage.getSeqNo() && coordBuffer.get(j).getStartingSender() == receiveMessage.getStartingSender()){
                            l = j;
                        }
                    }
                    if(l >= 0){
                        coordBuffer.remove(l);
                    }
                }
            }
            else if(rids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())) {
                if (receiveMessage.getSenderPort() != OurPort && receiveMessage.getrMessageDontSend() == -1) {
                    int z = -1;
                    for (int j = 0; j < receiveMessage.getMembersSend().size(); j++) {
                        if (receiveMessage.getMembersSend().get(j) == OurPort) {
                            z = j;
                        }
                    }
                    if (z >= 0) {
                        receiveMessage.getMembersSend().remove(z);

                    }
                    while (receiveMessage.getMembersSend().size() > 0) {
                        receiveMessage.getMembersSend().remove(0);
                    }
                    receiveMessage.getMembersSend().add(receiveMessage.getSenderPort());
                    receiveMessage.setSenderPort(OurPort);
                    receiveMessage.setrMessageDontSend(1);
                    BM_send(receiveMessage, receiveMessage.getGroupId());
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

//        System.out.println("OK TOTAL: " +se);
//        System.out.println("OK  RESEND TOTAL: " +re);
//
//        System.out.println("Buffer sendsBuffer TOTAL:"+ sendsBuffer.size());
//        System.out.println("BUffer resend TOTAL " + resend.size());
//        System.out.println("WHATISEND  TOTAL"+ whatIsendTotal.size());
//        System.out.println("Coord  SIZE TOTAL"+ coordBuffer.size());

        if(!totalbufids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
            totalbufids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo());
//            System.out.println("receive id " +receiveMessage.getSeqNo()+"rece"+receiveMessage.getTotaldelivered());
            UdpMessage newAddi = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),receiveMessage.getTotaldelivered());
            for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                newAddi.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
            }

            Message rec = new Message("", newAddi);
            totalbuf.add(rec);
            for(int j = 0;j < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                if(receiveMessage.getStartingSender() == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                    rec.setName(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getName());
                }
            }
        }
         if(totalbufids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
            if(receiveMessage.getTotaldelivered() >= 0) {
                for(int counter = 0;counter < totalbuf.size();counter++){
                    if(receiveMessage.getSeqNo() == totalbuf.get(counter).getMessage().getSeqNo() && receiveMessage.getStartingSender() == totalbuf.get(counter).getMessage().getStartingSender()){
                        totalbuf.get(counter).getMessage().setTotaldelivered(receiveMessage.getTotaldelivered());
                    }
                }
                int p = -1;
                for(int j = 0;j <whatIsendTotal.size();j++){
                    if(whatIsendTotal.get(j).getSeqNo() == receiveMessage.getSeqNo()){
                        p = j;
                    }
                }
                if(p >= 0){
                    whatIsendTotal.remove(p);
                }
            }
        }
        System.out.println("TOTAL DELIVER"+totaldelivered);
        checkTotalBuffer();
    }
    public void checkTotalBuffer(){
        while(true){
            int j = -1;
            for(int i = 0;i<totalbuf.size();i++){
                if(totalbuf.get(i).getMessage().getTotaldelivered() == totaldelivered){
                    appids.get(totalbuf.get(i).getMessage().getStartingSender()).add(totalbuf.get(i).getMessage().getSeqNo());
                    groupMessages.get(totalbuf.get(i).getMessage().getGroupId()).getMsgs().add(totalbuf.get(i));
                    totaldelivered++;
                    j = i;
                    synchronized (lock2){
                        lock2.notify();
                    }
                    break;
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

        if(!mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())) {
            int i = 0;
            if (sendsBuffer.size() > 0) {
                for (; i < sendsBuffer.size(); i++) {
                    if (sendsBuffer.get(i).getSeqNo() == receiveMessage.getSeqNo() && sendsBuffer.get(i).getStartingSender() == receiveMessage.getStartingSender()) {
                        int l = -1;
                        for (int j = 0; j < sendsBuffer.get(i).getMembersSend().size(); j++) {
                            if (sendsBuffer.get(i).getMembersSend().get(j) == receiveMessage.getSenderPort()) {
                                l = j;
                                break;
                            }
                        }
                        if (l >=0) {
                            sendsBuffer.get(i).getMembersSend().remove(l);
                        }
                        hold = 1;

                        if (receiveMessage.getSenderPort() != OurPort) {
                            receiveMessage.setSenderPort(OurPort);

                            if(sendsBuffer.get(i).getMembersSend().size() == 0){
                                receiveMessage.setDontSendAgain(1);
                            }
                            int our = -1;
                            for (int counter =0; counter < receiveMessage.getMembersSend().size();counter++){
                                if(receiveMessage.getMembersSend().get(counter) == OurPort){
                                    our = counter;
                                }
                            }
                            if(our >= 0){
                                receiveMessage.getMembersSend().remove(our);
                            }
                            int sender = -1;
                            for (int counter =0; counter < receiveMessage.getMembersSend().size();counter++){
                                if(receiveMessage.getMembersSend().get(counter) == receiveMessage.getSenderPort()){
                                    sender = counter;
                                }
                            }
                            if(sender >= 0){
                                receiveMessage.getMembersSend().remove(sender);
                            }
                            BM_send(receiveMessage, receiveMessage.getGroupId());
                        }
                        hold = 1;
                        if (sendsBuffer.get(i).getMembersSend().size() == 0) {
                            se++;
//                            System.out.println("Took ack from everyone: mesa sthn bm_deliver"+ receiveMessage.getSeqNo() +" "+ receiveMessage.getMessage() + " " +receiveMessage.getStartingSender());
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
                UdpMessage sendAddition = new UdpMessage(receiveMessage.getMessage(), receiveMessage.getSeqNo(), receiveMessage.getSenderPort(), receiveMessage.getGroupId(), receiveMessage.getStartingSender(),receiveMessage.getTotal(),receiveMessage.getSelfDelivered(),-1);

                for (int counter = 0; counter < receiveMessage.getMembersSend().size(); counter++) {
                    sendAddition.getMembersSend().add(receiveMessage.getMembersSend().get(counter));
                }

                sendsBuffer.add(sendAddition);

                int size = sendsBuffer.size();
                int p = -1;
                for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                    if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == receiveMessage.getSenderPort()) {
                        p = counter;
                    }
                }
                if (p >= 0) {
                    sendsBuffer.get(size - 1).getMembersSend().remove(p);
                }
                int a =-1;
                for (int counter = 0; counter < sendsBuffer.get(size - 1).getMembersSend().size(); counter++) {
                    if (sendsBuffer.get(size - 1).getMembersSend().get(counter) == OurPort) {
                        a = counter;
                    }
                }
                if(a >=0){
                    sendsBuffer.get(size - 1).getMembersSend().remove(a);
                }

                if (receiveMessage.getSenderPort() != OurPort) {
                    receiveMessage.setSenderPort(OurPort);

                    if(sendsBuffer.get(size - 1).getMembersSend().size() == 0){
                        receiveMessage.setDontSendAgain(1);
                    }

                    if(!mbufids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
                        BM_send(receiveMessage, receiveMessage.getGroupId());
                    }
                }
                if (sendsBuffer.get(size - 1).getMembersSend().size() == 0) {
                    se++;
//                    System.out.println("Took ack from everyone: sthn prwth"+ receiveMessage.getSeqNo() +" "+ receiveMessage.getMessage() + " " +receiveMessage.getStartingSender());
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
                    if(whatIsend.get(j).getSeqNo() == receiveMessage.getSeqNo() && whatIsend.get(j).getStartingSender() == receiveMessage.getStartingSender()){
                        p = j;
                    }
                }
                if(p >= 0){
                    whatIsend.remove(p);
                }
            }
        }
        else if(mids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
            if(receiveMessage.getSenderPort() != OurPort && receiveMessage.getDontSendAgain() == -1){
                int z = -1;
                for(int j = 0;j <receiveMessage.getMembersSend().size();j++){
                    if(receiveMessage.getMembersSend().get(j) == OurPort){
                        z = j;
                    }
                }
                if(z >=0){
                    receiveMessage.getMembersSend().remove(z);

                }
                while(receiveMessage.getMembersSend().size()>0){
                    receiveMessage.getMembersSend().remove(0);
                }
                receiveMessage.getMembersSend().add(receiveMessage.getSenderPort());
                receiveMessage.setSenderPort(OurPort);
                receiveMessage.setDontSendAgain(1);
                BM_send(receiveMessage,receiveMessage.getGroupId());
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

//        System.out.println("OK: " +se);
//        System.out.println("BUffer SIZE :"+ sendsBuffer.size());
//        System.out.println("WHAT I SEND SIZE"+ whatIsend.size());
//

        if(!mbufids.get(receiveMessage.getStartingSender()).contains(receiveMessage.getSeqNo())){
            mbufids.get(receiveMessage.getStartingSender()).add(receiveMessage.getSeqNo()); //prostikh ston buffer tou middleware gia auta poy perimenoume na pane sthn efarmogh

            Message rec = new Message("", receiveMessage);
            mbuf.add(rec);
//
            for(int j = 0;j < middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().size();j++){
                if(receiveMessage.getStartingSender() == middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getMemberPort()){
                    rec.setName(middlewareTeamsBuffer.get(receiveMessage.getGroupId()).getMembers().get(j).getName());
                }
            }
        }
        GroupMessages group = groupMessages.get(receiveMessage.getGroupId());

        checkFifoBuffer(group);
    }
    public void checkFifoBuffer(GroupMessages group) {
        int j = -1;
        Set<Integer> keys = group.getFifoOrders().keySet();
        for (Integer temp : keys) {
            while(true){
                for (int i = 0; i < mbuf.size(); i++) {
                    if(mbuf.get(i).getMessage().getStartingSender() == temp){
                        if (mbuf.get(i).getMessage().getSelfDelivered() == group.getFifoOrders().get(temp)) {
                            groupMessages.get(mbuf.get(i).getMessage().getGroupId()).getMsgs().add(mbuf.get(i));

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
                    break;
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