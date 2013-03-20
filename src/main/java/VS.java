//import kotlin.actor.*;

/**
 * @author Daniel Seidler
 * @since 2013/01/04
 */
public class VS {

    public static void main(String args[]) throws Exception {
        PiAkka piAkka = new PiAkka();
        PijActor pijActor = new PijActor();
//        kotlin.actor.PijActor kpijActor = new kotlin.actor.PijActor();
//        run(pijActor);
//        run(piAkka);
//        run(kpijActor);
//        run(pijActor);
//        run(piAkka);
//        run(kpijActor);
        for (int i = 0; i < 20; ++i) {
//            run(pijActor);
            run(piAkka);
            Thread.sleep(1000);
        }
    }

    static void run(PiCalculator picalc) throws Exception {
        int nrOfWorkers = 5;
        int nrOfElements = 10000;
        int nrOfMessages = 10000;

        run(nrOfWorkers, nrOfElements, nrOfMessages, picalc);
    }

    static void run(int nrOfWorkers, int nrOfElements, int nrOfMessages, PiCalculator picalc) throws Exception {
        picalc.calculate(nrOfWorkers, nrOfElements, nrOfMessages);
        System.out.println("=================================");
    }


}
