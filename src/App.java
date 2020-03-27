import java.util.Scanner;

public class App{
    public static Middleware middle;
    public  static Thread MiddleWareClient;
    public static  Thread AppClient;

    public static void main(String[] args) {
        MiddleWareClient = new Thread(new AppMiddle());
        AppClient = new Thread(new Application());

        AppClient.start();
    }
}

class Application extends Thread{
    @Override
    public void run() {
        Middleware appMiddleware = new Middleware();// Morfh epikinwnias tou application me to middleware



//        while(true){
//            Scanner in = new Scanner(System.in);
//            System.out.println();
           int Sock =  appMiddleware.grp_join("G1","Fwt");
           System.out.println(Sock);
//        }


    }
}

class AppMiddle extends Thread {
    @Override
    public void run() {

    }
}
