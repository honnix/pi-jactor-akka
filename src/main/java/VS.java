/**
 * @author Daniel Seidler
 * @since 2013/01/04
 */
public class VS {

    public static void main(String args[]) throws Exception {
        PiAkka piAkka = new PiAkka();
        PijActor pijActor = new PijActor();

        run(pijActor);
        run(piAkka);
        run(pijActor);
        run(piAkka);    }

    static void run(PiCalculator picalc) throws Exception {
        int nrOfWorkers = 5;
        int nrOfElements = 30000;
        int nrOfMessages = 30000;

        run(nrOfWorkers, nrOfElements, nrOfMessages, picalc);
    }

    static void run(int nrOfWorkers, int nrOfElements, int nrOfMessages, PiCalculator picalc) throws Exception {
        picalc.calculate(nrOfWorkers, nrOfElements, nrOfMessages);
        System.out.println("=================================");
    }


}
