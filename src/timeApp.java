import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class timeApp {
    public static void main(String[] args) {

//        Do start = 1587390019151;
//        long start1 =1587392275818L;
//        long end1 =1587392276110L;
//        long start2 =1587392275818L;
//        long end2 =1587392276110L;
        int mesos= 0;
        List<Long> times = new ArrayList<>();
        float max = 0;
        List<Float> mesosOros = new ArrayList<>();
        try {
            Scanner starts = new Scanner(new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/starts3"));
            Scanner receive1 = new Scanner(new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/receive13"));
            Scanner receive2 = new Scanner(new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/receive23"));
            Scanner receive3 = new Scanner(new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/receive33"));
//            Scanner receive4 = new Scanner(new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/receive4"));
//            Scanner receive5 = new Scanner(new File("/home/aggenikos/katanemhmena/Messenger/src/TestFilesFIfo/checkFiles/receive5"));


            while(starts.hasNext()){
                long start = starts.nextLong();
                System.out.println(start);
                long end1 = receive1.nextLong();
                long end2 = receive2.nextLong();
                long end3 = receive3.nextLong();
//                long end4 = receive4.nextLong();
//                long end5 = receive5.nextLong();
                times.add(end1);
                times.add(end2);
                times.add(end3);
//                times.add(end4);
//                times.add(end5);


                for(int i = 0;i < times.size();i++){
                    float result = (times.get(i) - start)/1000F;
                    System.out.println(result);
                    if(result >= max){
                        max = result;
                    }
                }
                mesosOros.add(max);
                max = 0;
                times.clear();
            }

            float k = 0;
            for(int j =0; j < mesosOros.size();j++){
                System.out.println(mesosOros.get(j));
                k = k + mesosOros.get(j);
            }

            System.out.println(mesosOros.size());
            System.out.println("MESOS XRONOS KATHUSTERHSHS: "+ k/mesosOros.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
