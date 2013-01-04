import org.agilewiki.jactor.*;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

/**
 * @author Daniel Seidler
 * @since 2013/01/04
 */
public class PijActor implements PiCalculator{
    public void calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages) throws Exception {
        System.out.println("JACTOR - calc - ("+nrOfWorkers+","+nrOfElements+","+nrOfMessages+")");
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(nrOfWorkers);
        Mailbox mailbox = mailboxFactory.createMailbox();
        Master master = new Master();
        master.initialize(mailbox);
        JAFuture future = new JAFuture();
        Start.init(nrOfElements,nrOfMessages).send(future,master);
        mailboxFactory.close();
    }
}

class Start extends Request<Object, Master> {
    private int nrOfElements;
    private int nrOfMessages;

    private Start(int nrOfElements, int nrOfMessages){
       this.nrOfElements = nrOfElements;
        this.nrOfMessages = nrOfMessages;
    }

    public static Start init(int nrOfElements, int nrOfMessages){
      return new Start(nrOfElements, nrOfMessages);
    }

    public void processRequest(JLPCActor targetActor, final RP rp)
            throws Exception {
        Master a = (Master) targetActor;
        RP<BigDecimal> prp = new RP<BigDecimal>() {
            final long start = System.currentTimeMillis();
            boolean pending = true;
            BigDecimal pi = BigDecimal.ZERO;
            int nrOfResults = 0;

            public void processResponse(BigDecimal result) throws Exception {
                pi = pi.add(result);
                nrOfResults += 1;
                if (nrOfResults == nrOfMessages) {
                    long duration = System.currentTimeMillis() - start;
                    System.out.println(String.format("\tPi approximation: \t\t%s\n\tCalculation time: \t%s milliseconds", pi, duration));
                    rp.processResponse(null);
                }
            }
        };
        a.calculate(nrOfMessages,nrOfElements, prp);
    }

    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof Master;
    }
}


class Master extends JLPCActor {
    public void calculate(final int nrOfMessages, int nrOfElements, final RP rp) throws Exception {
        for (int start = 0; start < nrOfMessages; start++) {
            Worker worker = new Worker();
            worker.initialize(getMailboxFactory().createAsyncMailbox());
            (new Work(start, nrOfElements)).send(this, worker, rp);
        }
    }
}


class Worker extends JLPCActor {
    public BigDecimal calculatePiFor(int start, int nrOfElements) {
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
            acc = acc.add(new BigDecimal(4.0 * (1 - (i % 2) * 2) / (2 * i + 1)));
        }
        return acc;
    }
}


class Work extends Request<BigDecimal, Worker> {
    private int start;
    private int nrOfElements;

    public Work(int start, int nrOfElements) {
        this.start = start;
        this.nrOfElements = nrOfElements;
    }

    @Override
    public boolean isTargetType(Actor actor) {
        return actor instanceof Worker;

    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        Worker worker = (Worker) targetActor;
        BigDecimal result = ((Worker) targetActor).calculatePiFor(start, nrOfElements);
        rp.processResponse(result);
    }
}





