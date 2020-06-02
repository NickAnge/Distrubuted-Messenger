import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class App {
    public static Middleware middle;
    public static Thread MiddleWareClient;
    public static Thread AppClient;
    public static Middleware appMiddleware;

    public static void main(String[] args) {
        // MiddleWareClient = new Thread(new AppMiddle());
        AppClient = new Thread(new Application());

        AppClient.start();
        // MiddleWareClient.start();
        return;
    }
}

class Application extends Thread {
    public final String GREEN = "\033[0;32m";
    public static final String RED_BOLD = "\033[1;31m"; // RED
    public static final String CYAN_BOLD = "\033[1;36m"; // CYAN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String WHITE_BOLD = "\033[1;37m"; // WHITE
    public static final String WHITE = "\033[0;37m"; // WHITE
    public static final String RESET = "\033[0m"; // Text Reset
    public static final String RED = "\033[0;31m"; // RED

    public List<Integer> teams;
    HashMap<Integer, GroupInfo> AllViews;
    String name;

    public Application() {
        teams = new ArrayList<>();
        AllViews = new HashMap<>();
    }

    @Override
    public void run() {
        Scanner in = new Scanner(System.in);

        System.out.println(RED_BOLD + "Starting the App..");

        System.out.println(RESET);

        App.appMiddleware = new Middleware();// Morfh epikinwnias tou application me to middleware

        if (App.appMiddleware.checkValue == 0) {
            System.out.println(RED + "Wait time expired ...Couldn't Connect the App....Try again later");
            App.appMiddleware.middlewareThread.interrupt();
            return;
        }
        System.out.println(RED_BOLD + "NEW APP");
        // in.nextLine();
        System.out.print(CYAN_BOLD + "MY NAME: ");
        name = in.next();
        String Group;
        int Sock = 0;

        while (true) {
            System.out.println(RED_BOLD + "MENU: ");
            System.out.println(RED_BOLD + "    1) Join into a new group:");
            System.out.println(RED_BOLD + "    2) Show all Groups in App:");
            System.out.println(RED_BOLD + "    3) Leave from a Group:");
            System.out.println(RED_BOLD + "    4) Exit App");
            System.out.println(RED_BOLD + "    5) Send messages to a specific Group: ");
            System.out.println(RED_BOLD + "    6) Receive messages from a specific Group(TERMINAL)");
            System.out.println(RED_BOLD + "    7) Send/receive (file/file ) of messages(FIFO)");
            // System.out.println(RED_BOLD + " 8) Send/receive (file/file ) of
            // messages(TOTAL)");
            System.out.println(RED_BOLD + "    9) Receive messages from a specific Group(File)");
            System.out.println(RED_BOLD + "    10) Receive message(ONE) from a specific Group(Terminal)");

            if (App.appMiddleware.counterUDP > 0) {
                System.out.println(RED_BOLD + "   11) Counter UDP/IP");
            }

            int choice = in.nextInt();

            switch (choice) {
                case 1:
                    System.err.print(CYAN_BOLD + "GROUP: ");
                    Group = in.next();
                    System.err.println(RESET);

                    Message newView = new Message("", null, (UdpMessage) null);
                    Sock = App.appMiddleware.grp_join(Group, name, newView);
                    teams.add(Sock);
                    AllViews.put(Sock, newView.getView());
                    break;
                case 2:
                    System.out.println(RED_BOLD + "GROUPS ");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    break;
                case 3:
                    if (teams.size() == 0) {
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "WHICH ONE");

                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    int group = in.nextInt();
                    int k = -1;
                    for (int i = 0; i < teams.size(); i++) {
                        if (group == teams.get(i)) {
                            k = i;
                        }
                    }
                    if (k >= 0) {
                        teams.remove(k);
                    }
                    AllViews.remove(group);
                    App.appMiddleware.grp_leave(group);
                    break;
                case 4:
                    System.out.println("Exit from App");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println("Leave from team : Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                        App.appMiddleware.grp_leave(teams.get(i));
                        AllViews.remove(teams.get(i));
                    }
                    int j = 0;
                    while (teams.size() > 0) {
                        teams.remove(j);
                    }
                    // System.out.println("INTERRUPT");
                    App.appMiddleware.middlewareThread.interrupt();
                    return;
                // break;
                case 5:
                    if (teams.size() == 0) {
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();

                    System.out.print(RED_BOLD + "CHOOSE FIFO(0)");
                    int total = in.nextInt();
                    System.out.println("START CONVERSATION");
                    in.nextLine();

                    String msg = in.nextLine();
                    while (!msg.equals("")) {
                        System.out.println(msg);
                        App.appMiddleware.grp_send(Integer.parseInt(Group), msg, 0, total);
                        msg = in.nextLine();
                        if (msg.equals("")) {
                            App.appMiddleware.grp_send(Integer.parseInt(Group), msg, 0, total);
                        }
                    }
                    break;
                case 6:
                    if (teams.size() == 0) {
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    System.out.print(RED_BOLD + "CHOOSE FIFO(0) ");
                    int read1 = in.nextInt();

                    String readMsg = " ";
                    while (!readMsg.equals("Bye")) {
                        int flag = 0;
                        UdpMessage udpReceive = new UdpMessage();
                        GroupInfo gpReceive = new GroupInfo();
                        Message mReceive = new Message("", gpReceive, udpReceive);

                        int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, 1);

                        if (returnVal == 0) {
                            readMsg = " ";
                            continue;
                        }

                        while (!mReceive.getType().equals("")) {
                            if (read1 == 0) {
                                System.out.println("Screen:" + name + " Group"
                                        + AllViews.get(mReceive.getView().getId()).getGroupName() + " "
                                        + mReceive.getChangeViewMessage());

                            } else if (read1 == 1) {
                                System.out.println("Screen:" + name + " Group"
                                        + AllViews.get(mReceive.getView().getId()).getGroupName() + " "
                                        + mReceive.getChangeViewMessage());
                            }
                            AllViews.put(mReceive.getView().getId(), mReceive.getView());
                            UdpMessage udp1 = new UdpMessage();
                            GroupInfo gr = new GroupInfo();
                            mReceive = new Message("", gr, udp1);
                            int re = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, 1);
                            if (re == 0) {
                                flag = 1;
                                break;
                            }
                        }
                        if (flag == 1) {
                            readMsg = " ";
                            continue;
                        }
                        if (read1 == 0) {
                            System.out.println("Screen:" + name + " Group"
                                    + AllViews.get(mReceive.getMessage().getGroupId()).getGroupName() + " "
                                    + mReceive.getName() + ": " + mReceive.getMessage().getSelfDelivered() + " "
                                    + mReceive.getMessage().getMessage());

                        }

                        readMsg = mReceive.getMessage().getMessage();
                    }
                    break;
                case 7:
                    if (teams.size() == 0) {
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    System.out.println("START CONVERSATION");
                    in.nextLine();
                    System.out.print(RED_BOLD + "Name of File: ");
                    String NameFile = in.next();
                    File file = new File("./TestFilesFIfo/" + NameFile);
                    sendMessages(App.appMiddleware, file, Integer.parseInt(Group), 0);

                    String read2Msg = " ";
                    while (!read2Msg.equals("Bye")) {
                        int flag = 0;
                        UdpMessage udpReceive = new UdpMessage();
                        GroupInfo gpReceive = new GroupInfo();
                        Message mReceive = new Message("", gpReceive, udpReceive);

                        int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, 0);

                        if (returnVal == 0) {
                            read2Msg = " ";
                            continue;
                        }

                        while (!mReceive.getType().equals("")) {
                            creationOfFilesFifo(mReceive, 1, Integer.parseInt(Group));
                            AllViews.put(mReceive.getView().getId(), mReceive.getView());
                            UdpMessage udp1 = new UdpMessage();
                            GroupInfo gr = new GroupInfo();
                            mReceive = new Message("", gr, udp1);
                            int re = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, 0);
                            if (re == 0) {
                                flag = 1;
                                break;
                            }
                        }
                        if (flag == 1) {
                            read2Msg = " ";
                            continue;
                        }
                        creationOfFilesFifo(mReceive, 0, Integer.parseInt(Group));

                        read2Msg = mReceive.getMessage().getMessage();
                    }
                    break;
                case 8:
                    // if(teams.size() == 0){
                    // System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                    // break;
                    // }
                    // System.out.println(RED_BOLD + "GROUPS ");
                    // for(int i= 0 ; i <teams.size(); i++){
                    // System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+
                    // AllViews.get(teams.get(i)).getGroupName());
                    // }
                    // System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    // Group = in.next();

                    // System.out.println("START CONVERSATION");
                    // in.nextLine();
                    // System.out.print(RED_BOLD + "Name of File: ");
                    // String NameFile2 = in.next();
                    // File file2 = new
                    // File("./TestFilesFIfo/"+NameFile2);
                    // sendMessages(App.appMiddleware,file2,Integer.parseInt(Group),1);

                    // String read3Msg = " ";
                    // while(!read3Msg.equals("Bye")) {
                    // int flag = 0;
                    // UdpMessage udpReceive = new UdpMessage();
                    // GroupInfo gpReceive = new GroupInfo();
                    // Message mReceive = new Message("", gpReceive, udpReceive);

                    // int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive,
                    // 0);

                    // if (returnVal == 0) {
                    // read3Msg = " ";
                    // continue;
                    // }

                    // while (!mReceive.getType().equals("")) {
                    // creationOfFilesTotal(mReceive,1,Integer.parseInt(Group));
                    // AllViews.put(mReceive.getView().getId(), mReceive.getView());
                    // UdpMessage udp1 = new UdpMessage();
                    // GroupInfo gr = new GroupInfo();
                    // mReceive = new Message("", gr, udp1);
                    // int re = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, 1);
                    // if (re == 0) {
                    // flag = 1;
                    // break;
                    // }
                    // }
                    // if (flag == 1) {
                    // read3Msg = " ";
                    // continue;
                    // }
                    // creationOfFilesTotal(mReceive,0,Integer.parseInt(Group));
                    // read3Msg = mReceive.getMessage().getMessage();

                    // }
                    // break;
                case 9:
                    if (teams.size() == 0) {
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    System.out.print(RED_BOLD + "CHOOSE FIFO(0)");
                    int read = in.nextInt();
                    System.out.print(RED_BOLD + "BLOCK(1) or NON-BLOCKING(0): ");
                    int blocking = in.nextInt();

                    String read4Msg = " ";
                    while (!read4Msg.equals("Bye")) {
                        int flag = 0;
                        UdpMessage udpReceive = new UdpMessage();
                        GroupInfo gpReceive = new GroupInfo();
                        Message mReceive = new Message("", gpReceive, udpReceive);

                        int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, blocking);

                        if (returnVal == 0) {
                            read4Msg = " ";
                            continue;
                        }

                        while (!mReceive.getType().equals("")) {
                            if (read == 1) {
                                creationOfFilesTotal(mReceive, 1, Integer.parseInt(Group));
                            } else {
                                creationOfFilesFifo(mReceive, 1, Integer.parseInt(Group));

                            }
                            AllViews.put(mReceive.getView().getId(), mReceive.getView());
                            UdpMessage udp1 = new UdpMessage();
                            GroupInfo gr = new GroupInfo();
                            mReceive = new Message("", gr, udp1);
                            int re = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, blocking);
                            if (re == 0) {
                                flag = 1;
                                break;
                            }
                            // flag = 1;
                        }
                        if (flag == 1) {
                            read4Msg = " ";
                            continue;
                        }
                        if (read == 1) {
                            creationOfFilesTotal(mReceive, 0, Integer.parseInt(Group));
                        } else {
                            creationOfFilesFifo(mReceive, 0, Integer.parseInt(Group));

                        }
                        read4Msg = mReceive.getMessage().getMessage();
                    }
                    break;
                case 10:
                    if (teams.size() == 0) {
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "GROUPS ");
                    for (int i = 0; i < teams.size(); i++) {
                        System.out.println(CYAN_BOLD + "Code: " + teams.get(i) + ", Team Name: "
                                + AllViews.get(teams.get(i)).getGroupName());
                    }
                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();

                    System.out.print(RED_BOLD + "BLOCK(1) or NON-BLOCKING(0): ");
                    int block = in.nextInt();
                    int flag = 0;
                    UdpMessage udpReceive = new UdpMessage();
                    GroupInfo gpReceive = new GroupInfo();
                    Message mReceive = new Message("", gpReceive, udpReceive);
                    int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, block);
                    if (returnVal == 0) {
                        break;
                    }
                    while (!mReceive.getType().equals("")) {
                        System.out.println("Group" + AllViews.get(mReceive.getView().getId()).getGroupName() + " "
                                + mReceive.getChangeViewMessage());
                        AllViews.put(mReceive.getView().getId(), mReceive.getView());
                        UdpMessage udp1 = new UdpMessage();
                        GroupInfo gr = new GroupInfo();
                        mReceive = new Message("", gr, udp1);
                        int re = App.appMiddleware.grp_recv(Integer.parseInt(Group), mReceive, 0);
                        if (re == 0) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 1) {
                        break;
                    }
                    System.out.println("Group" + AllViews.get(mReceive.getMessage().getGroupId()).getGroupName() + " "
                            + mReceive.getName() + ": " + mReceive.getMessage().getSeqNo() + " "
                            + mReceive.getMessage().getMessage());
                    break;
                case 11:
                    System.out.println("Number of UDP/Ips Messages" + App.appMiddleware.counterUDP);
                    break;
                default:
                    System.out.println(RED + "WRONG CHOICE>>>TRY AGAIN");
                    break;
            }
        }
    }//

    public void sendMessages(Middleware middleware, File file, int Group, int total) {
        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNext()) {
                String msg = sc.next();
                App.appMiddleware.grp_send(Group, msg, 0, total);
            }
            // System.out.println("BGHKA APO THN WHILE");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void creationOfFilesFifo(Message msg, int view, int gSock) {

        BufferedWriter out = null;
        if (view == 1) {
            try {
                File file = new File("./TestFilesFIfo/checkFiles/" + name + "_" + name);
                if (file.createNewFile()) {
                    // System.out.println("File created: " + file.getName());
                    FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                    out = new BufferedWriter(fstream);
                } else {
                    FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                    out = new BufferedWriter(fstream);
                    // System.out.println("File already exists.");
                }
                String viewString = new String("Group" + AllViews.get(msg.getView().getId()).getGroupName() + " "
                        + msg.getChangeViewMessage());
                System.out.println(viewString);
                // out.append(viewString);
                // out.append("\n");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            if (msg.getName() == null) {
                return;
            }
            File file = new File("./TestFilesFIfo/checkFiles/" + name + "_" + msg.getName());
            if (file.createNewFile()) {
                // System.out.println("File created: " + file.getName());
                FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                out = new BufferedWriter(fstream);
            } else {
                FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                out = new BufferedWriter(fstream);
                // System.out.println("File already exists.");
            }
            String output = new String("Group" + AllViews.get(msg.getMessage().getGroupId()).getGroupName() + " "
                    + msg.getName() + ": " + msg.getMessage().getSeqNo() + " " + msg.getMessage().getMessage());
            // System.out.println(output);

            out.append(output);
            out.append("\n");
            out.close();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void creationOfFilesTotal(Message msg, int view, int gSock) {
        BufferedWriter out = null;
        if (view == 1) {
            try {
                File file = new File("./TestFilesFIfo/checkFiles/" + name);
                if (file.createNewFile()) {
                    // System.out.println("File created: " + file.getName());
                    FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                    out = new BufferedWriter(fstream);
                } else {
                    FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                    out = new BufferedWriter(fstream);
                    // System.out.println("File already exists.");
                }
                String viewString = new String("Group" + AllViews.get(msg.getView().getId()).getGroupName() + " "
                        + msg.getChangeViewMessage());
                System.out.println(viewString);
                // out.append(viewString);
                // out.append("\n");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            File file = new File("./TestFilesFIfo/checkFiles/" + name);
            if (file.createNewFile()) {
                // System.out.println("File created: " + file.getName());
                FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                out = new BufferedWriter(fstream);
            } else {
                FileWriter fstream = new FileWriter(file, true); // true tells to append data.
                out = new BufferedWriter(fstream);
                // System.out.println("File already exists.");
            }
            String output = new String(
                    "Group" + AllViews.get(msg.getMessage().getGroupId()).getGroupName() + " " + msg.getName() + ": "
                            + msg.getMessage().getTotaldelivered() + " " + msg.getMessage().getMessage());
            // System.out.println(output);

            out.append(output);
            out.append("\n");
            out.close();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
}
