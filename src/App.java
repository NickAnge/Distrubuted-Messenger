import java.util.Scanner;

public class App{
    public static Middleware middle;
    public  static Thread MiddleWareClient;
    public static  Thread AppClient;
    public static Middleware  appMiddleware;

    public static void main(String[] args) {
        MiddleWareClient = new Thread(new AppMiddle());
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



    @Override
    public void run() {

        System.out.println(RED_BOLD + "Starting the App..");
        System.out.println(RESET);

        App.appMiddleware = new Middleware();// Morfh epikinwnias tou application me to middleware

        if(App.appMiddleware.checkValue == 0){
            System.out.println(RED +"Wait time expired ...Couldn't Connect the App....Try again later");
            return;
        }
        Scanner in = new Scanner(System.in);

        System.out.println(RED_BOLD + "NEW APP");
        System.out.print(CYAN_BOLD + "MY NAME: ");
        String name = in.next();
        String Group;
        int Sock = 0;


        while(true) {
            System.out.println(RED_BOLD + "MENU: ");
            System.out.println(RED_BOLD + "    1) Join into a new group:");
            System.out.println(RED_BOLD + "    2) Chat with a  specific group:");
            System.out.println(RED_BOLD + "    3) Leave from a Group");
            int choice = in.nextInt();
//            int Sock = 0;

            switch (choice) {
                case 1:
                    System.out.print(CYAN_BOLD + "GROUP: ");
                    Group = in.next();
                    System.out.println(RESET);
                    Sock = App.appMiddleware.grp_join(Group, name);
//                    App.MiddleWareClient.start();
                    System.out.println(Sock);


                    break;
                case 2:
                    System.out.print(CYAN_BOLD + "GROUP: ");
                    Group = in.next();
                    break;
                case 3:
                    System.out.println(RED_BOLD + "BYE BYE" + Sock);
                    App.appMiddleware.grp_leave(Sock);
                    return;
//                    break;
            }
//            break;
        }



    }
}

class AppMiddle extends Thread {
    @Override
    public void run() {

        while(true){
            System.out.println("CONINU");
            GroupInfo newGroup2 = (GroupInfo) App.appMiddleware.getViewFromSocket(App.appMiddleware.InfoManager.getCommunicationSock());
//            if(newGroup2 == null){
////                App.AppClient.
//                continue;
//            }
            System.out.println("MPHKA STO MIDDLE");

            System.out.println("Group Name: " + newGroup2.getGroupName());
            for(int i = 0;i <newGroup2.getMembers().size();i++){
                System.out.println("Name: "+newGroup2.getMembers().get(i).getName());
                System.out.println("Address: "+ newGroup2.getMembers().get(i).getMemberAddress() );
                System.out.println("Port:"+newGroup2.getMembers().get(0).getMemberPort());


            }
        }

    }
}
