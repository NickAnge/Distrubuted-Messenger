import java.io.*;
import java.util.*;

public class App{
    public static Middleware middle;
    public  static Thread MiddleWareClient;
    public static  Thread AppClient;
    public static Middleware  appMiddleware;


    public static void main(String[] args) {
//        MiddleWareClient = new Thread(new AppMiddle());
        AppClient = new Thread(new Application());

        AppClient.start();
//        MiddleWareClient.start();
    }
}

class Application extends Thread{
    public  final String GREEN = "\033[0;32m";
    public static final String RED_BOLD = "\033[1;31m";    // RED
    public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String WHITE_BOLD = "\033[1;37m";  // WHITE
    public static final String WHITE = "\033[0;37m";   // WHITE
    public static final String RESET = "\033[0m";  // Text Reset
    public static final String RED = "\033[0;31m";     // RED

    public List<Integer> teams;
    HashMap<Integer,GroupInfo> AllViews;
    String name;
    public Application(){
        teams = new ArrayList<>();
        AllViews = new HashMap<>();
    }


    @Override
    public void run() {
        Scanner in = new Scanner(System.in);

        System.out.println(RED_BOLD + "Starting the App..");
//        System.out.print(RED_BOLD + "Give Starting seq Number..");
//        int seq = in.nextInt();
         System.out.println(RESET);

        App.appMiddleware = new Middleware();// Morfh epikinwnias tou application me to middleware
        // System.out.println(RESET);

        if(App.appMiddleware.checkValue == 0){
            System.out.println(RED +"Wait time expired ...Couldn't Connect the App....Try again later");
            return;
        }
        System.out.println(RED_BOLD + "NEW APP");
        System.out.print(CYAN_BOLD + "MY NAME: ");
        name = in.next();
        String Group;
        int Sock = 0;
//        App.MiddleWareClient.start();

        while(true) {
            System.err.println(RED_BOLD + "MENU: ");
            System.err.println(RED_BOLD + "    1) Join into a new group:");
            System.err.println(RED_BOLD + "    2) Send a message to a specific Group:");
            System.err.println(RED_BOLD + "    3) Send/receive A file of messages(FIFO)");  // TODO: 9/4/20  na kleinei meta apo orismeno arithmo mhnymatwn gia nea epilogh 
            System.err.println(RED_BOLD + "    4) Send/receive A file of messages(TOTAL)");
            System.err.println(RED_BOLD + "    5) Receive only from a Group");
            System.err.println(RED_BOLD + "    6) Receive a message from a specific Group:");
            System.err.println(RED_BOLD + "    7) Leave from a Group");
            int choice = in.nextInt();


            switch (choice) {
                case 1:
                    System.err.print(CYAN_BOLD + "GROUP: ");
                    Group = in.next();
                    System.err.println(RESET);
                    Message newView = new Message("",null,(UdpMessage)null);
                    Sock = App.appMiddleware.grp_join(Group, name,newView);
                    teams.add(Sock);

                    AllViews.put(Sock,newView.getView());
//                    Set<Integer> keys = AllViews.keySet();
//                    for(Integer req: keys){
//                        System.out.println(App.appMiddleware.middlewareTeamsBuffer.get(req).getGroupName());
//                    }
//                    System.out.println(Sock);

                    break;
                case 2:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }

                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    System.out.print(RED_BOLD + "CHOOSE FIFO(0) or TOTAL (1): ");
                    int total = in.nextInt();
                    System.out.println("START CONVERSATION");
                    in.nextLine();



                    String msg= "";
                    while(!msg.equals("Bye")){
                        msg = in.nextLine();
                        System.err.println("My msg :" +msg);
                        App.appMiddleware.grp_send(Integer.parseInt(Group),msg,0,total);
                    }
                    break;
                case 3:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    File file = new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/testfifo");

                    sendMessages(App.appMiddleware,file,Integer.parseInt(Group),0);

                    while(true){
//                        System.out.println("Mphka sthn read");
                        int flag = 0;
                        UdpMessage udpReceive = new UdpMessage();
                        GroupInfo gpReceive = new GroupInfo();
                        Message mReceive = new Message("",gpReceive,udpReceive);

                        int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group),mReceive,0);
                        if(returnVal == 0){
                            continue;
                        }

                        if(!mReceive.getType().equals("")) {
                            creationOfFilesFifo(mReceive, 1, Integer.parseInt(Group));
                            System.out.println("Screen:" + name + " Group" + AllViews.get(mReceive.getView().getId()).getGroupName() + " " + mReceive.getChangeViewMessage());
                            AllViews.put(mReceive.getView().getId(), mReceive.getView());
                            UdpMessage udp1 = new UdpMessage();
                            GroupInfo gr = new GroupInfo();
                            mReceive = new Message("", gr, udp1);
                            continue;
                        }
//                        if(flag == 1){
//                            continue;
//                        }
                        creationOfFilesFifo(mReceive,0,Integer.parseInt(Group));
                        System.out.println( "Screen:" +name+ " Group" + AllViews.get(mReceive.getMessage().getGroupId()).getGroupName() + " "+mReceive.getName() + ": "+ mReceive.getMessage().getSeqNo()+ " " + mReceive.getMessage().getMessage());
//                        System.out.println(""+mReceive.getName() +": "+ mReceive.getMessage().getMessage());
                    }
//                    break;
                case 4:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    File file2 = new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/testfifo");

                    sendMessages(App.appMiddleware,file2,Integer.parseInt(Group),1);
                    break;
                case 5:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();

                    while(true){
//                        System.out.println("Mphka sthn read");
                        int flag = 0;
                        UdpMessage udpReceive = new UdpMessage();
                        GroupInfo gpReceive = new GroupInfo();
                        Message mReceive = new Message("",gpReceive,udpReceive);

                        int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group),mReceive,0);
                        if(returnVal == 0){
                            continue;
                        }

                        while(!mReceive.getType().equals("")){
                            System.out.println("Screen:" +name+ " Group" + AllViews.get(mReceive.getView().getId()).getGroupName() + " "+ mReceive.getChangeViewMessage());
                            AllViews.put(mReceive.getView().getId(),mReceive.getView());
                            UdpMessage udp1 = new UdpMessage();
                            GroupInfo gr = new GroupInfo();
                            mReceive = new Message("",gr,udp1);
                            int re = App.appMiddleware.grp_recv(Integer.parseInt(Group),mReceive,0);
                            if(re == 0){
                                flag =1;
                                break;
                            }
//                            flag = 1;
                        }
                        if(flag == 1){
                            continue;
                        }
                        System.out.println( "Screen:" +name+ " Group" + AllViews.get(mReceive.getMessage().getGroupId()).getGroupName() + " "+mReceive.getName() + ": "+ mReceive.getMessage().getSeqNo()+ " " + mReceive.getMessage().getMessage());
//                        System.out.println(""+mReceive.getName() +": "+ mReceive.getMessage().getMessage());
                    }
                case 6:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
////                    System.out.println(RED_BOLD + "WHICH ONE");
//                    in.nextLine();
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }

                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    System.out.println(RED_BOLD + "block(1)/No-blocking(0)");
                    int block = in.nextInt();

                    UdpMessage udp = new UdpMessage();

//                    System.out.println(GREEN +"App received msg");
                    GroupInfo gp = new GroupInfo();
                    Message ms = new Message("",gp,udp);
                    int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group),ms,block);

                    if(returnVal == 0){
                        System.out.println("No message");
                        break;
                    }
                    int flag = 0;
                    while(!ms.getType().equals("")){
                        System.out.println(ms.getChangeViewMessage());
                        AllViews.put(ms.getView().getId(),ms.getView());
                        UdpMessage udp1 = new UdpMessage();
                        GroupInfo gr = new GroupInfo();
                        ms = new Message("",gr,udp1);
                        int re = App.appMiddleware.grp_recv(Integer.parseInt(Group),ms,block);
                        if(re == 0){
                            flag =1;
                            break;
                        }
                    }
                    if(flag ==1 ){
                        break;
                    }
                    System.out.println( "Screen:" +name+ " Group" + AllViews.get(ms.getMessage().getGroupId()).getGroupName() + " "+ms.getName() + ": " + ms.getMessage().getMessage());

//                    System.out.println(""+ms.getName() +": "+ ms.getMessage().getMessage());
                    break;
                case 7:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "WHICH ONE");

                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println("Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }
                    int group = in.nextInt();
                    int k = -1;
                    for(int i = 0; i <teams.size(); i++){
                        if(group == teams.get(i)){
                            k = i;
                        }
                    }
                    if(k >=0){
                        teams.remove(k);
                    }
                    AllViews.remove(group);
                    App.appMiddleware.grp_leave(group);
                    break;
                default:
                    System.out.println(RED + "WRONG CHOICE>>>TRY AGAIN");
                    break;
            }
//            break;
        }



    }
    public void sendMessages(Middleware middleware,File file,int Group,int total){
        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNext()){
                String msg = sc.next();
                App.appMiddleware.grp_send(Group,msg,0,total);
            }
            System.out.println("BGHKA APO THN WHILE");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void creationOfFilesFifo(Message msg,int view,int gSock){


        BufferedWriter out = null;
        if(view == 1){
            for(int i =0; i < AllViews.get(gSock).getMembers().size();i++){
                try {
                    File file = new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/"+name+"_"+AllViews.get(gSock).getMembers().get(i).getName());
                    if (file.createNewFile()) {
                        System.out.println("File created: " + file.getName());
                        FileWriter fstream = new FileWriter(file, true); //true tells to append data.
                        out = new BufferedWriter(fstream);
                    } else {
                        FileWriter fstream = new FileWriter(file, true); //true tells to append data.
                        out = new BufferedWriter(fstream);
                        System.out.println("File already exists.");
                    }
                    String viewString = new String("Screen:" +name+ " Group" + AllViews.get(msg.getView().getId()).getGroupName() + " "+ msg.getChangeViewMessage());
                    System.out.println();
                    out.append(viewString);
                    out.append("\n");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        try {
            File file = new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/"+name+"_"+msg.getName());
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
                FileWriter fstream = new FileWriter(file, true); //true tells to append data.
                out = new BufferedWriter(fstream);
            } else {
                FileWriter fstream = new FileWriter(file, true); //true tells to append data.
                out = new BufferedWriter(fstream);
                System.out.println("File already exists.");
            }
            String output = new String("Screen:" +name+ " Group" + AllViews.get(msg.getMessage().getGroupId()).getGroupName() + " "+msg.getName() + ": "+ msg.getMessage().getSeqNo()+ " " + msg.getMessage().getMessage());
            System.out.println(output);

            out.append(output);
            out.append("\n");
            out.close();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
