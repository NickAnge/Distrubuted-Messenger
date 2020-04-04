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
    public Application(){
        teams = new ArrayList<>();
        AllViews = new HashMap<>();
    }


    @Override
    public void run() {
        Scanner in = new Scanner(System.in);

        System.out.println(RED_BOLD + "Starting the App..");
        System.out.print(RED_BOLD + "Give Starting seq Number..");
        int seq = in.nextInt();
         System.out.println(RESET);

        App.appMiddleware = new Middleware(seq);// Morfh epikinwnias tou application me to middleware
        // System.out.println(RESET);

        if(App.appMiddleware.checkValue == 0){
            System.out.println(RED +"Wait time expired ...Couldn't Connect the App....Try again later");
            return;
        }
        System.out.println(RED_BOLD + "NEW APP");
        System.out.print(CYAN_BOLD + "MY NAME: ");
        String name = in.next();
        String Group;
        int Sock = 0;
//        App.MiddleWareClient.start();

        while(true) {
            System.err.println(RED_BOLD + "MENU: ");
            System.err.println(RED_BOLD + "    1) Join into a new group:");
            System.err.println(RED_BOLD + "    2) Send a message to a specific Group:");
            System.err.println(RED_BOLD + "    3) Receive a message from a specific Group:");
            System.err.println(RED_BOLD + "    4) Leave from a Group");
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
                    System.out.println(RED_BOLD + "GROUPS ");
////                    System.out.println(RED_BOLD + "WHICH ONE");
//                    in.nextLine();
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }

                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    System.out.println("START CONVERSATION");
                    in.nextLine();

                    String msg= "";
                    while(!msg.equals("Bye")){
                        msg = in.nextLine();
                        System.err.println("My msg :" +msg);
                        App.appMiddleware.grp_send(Integer.parseInt(Group),msg,0,0);
                    }
                    break;
                case 3:
                    System.out.println(RED_BOLD + "GROUPS ");
////                    System.out.println(RED_BOLD + "WHICH ONE");
//                    in.nextLine();
                    for(int i= 0 ; i <teams.size(); i++){
                        System.out.println(CYAN_BOLD + "Code: "+ teams.get(i)+ ", Team Name: "+ AllViews.get(teams.get(i)).getGroupName());
                    }

                    System.out.print(RED_BOLD + "CHOOSE GROUP: ");
                    Group = in.next();
                    UdpMessage udp = new UdpMessage();

                    System.out.println(GREEN +"App received msg");
                    GroupInfo gp = new GroupInfo();
                    Message ms = new Message("",gp,udp);
                    int returnVal = App.appMiddleware.grp_recv(Integer.parseInt(Group),ms,0);

                    if(returnVal == 0){
                        break;
                    }
                    int flag = 0;
                    while(!ms.getType().equals("")){
                        System.out.println(ms.getChangeViewMessage());
                        AllViews.put(ms.getView().getId(),ms.getView());
                        UdpMessage udp1 = new UdpMessage();
                        GroupInfo gr = new GroupInfo();
                        ms = new Message("",gr,udp1);
                        int re = App.appMiddleware.grp_recv(Integer.parseInt(Group),ms,0);
                        if(re == 0){
                            flag =1;
                            break;
                        }
                    }
                    if(flag ==1 ){
                        break;
                    }
                    System.out.println(""+ms.getName() +": "+ ms.getMessage().getMessage());
                    break;
                case 4:
                    if(teams.size() == 0){
                        System.out.println("NO GROUPS EXIST ... TRY TO CONNECT TO ONE");
                        break;
                    }
                    System.out.println(RED_BOLD + "WHICH ONE");

                    Group = in.next();
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
            }
//            break;
        }



    }
}
