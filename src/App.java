
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


        appMiddleware.grp_join("G1","Nikos");


    }
}

class AppMiddle extends Thread {
    @Override
    public void run() {

    }
}
