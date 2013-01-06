package kotlin.actor

import java.math.BigDecimal
import org.agilewiki.jactor.lpc.Request
import org.agilewiki.jactor.lpc.JLPCActor
import org.agilewiki.jactor.RP
import org.agilewiki.jactor.JAMailboxFactory
import org.agilewiki.jactor.JAFuture
import akka.dispatch.BoundedMailbox
import org.agilewiki.jactor.Mailbox

/**
 * Created with IntelliJ IDEA.
 * User: daniel
 * Date: 06.01.13
 * Time: 18:00
 * To change this template use File | Settings | File Templates.
 */

fun main(args: Array<String>) {
    PijActor().calculate(5,1000,1000)
}
public class PijActor: PiCalculator{
    public override fun calculate(nrOfWorkers : Int, nrOfElements: Int, nrOfMessages: Int){
        System.out.println("JACTOR - calc - (" + nrOfWorkers + "," + nrOfElements + "," + nrOfMessages + ")");
        val mailboxFactory = JAMailboxFactory.newMailboxFactory(nrOfWorkers!!)!!;
        val mailbox = mailboxFactory.createMailbox()!!
        val future = JAFuture();
        MasterA(nrOfElements, nrOfMessages, mailbox).send(future);
        mailboxFactory.close();
    }
}

class MasterA(val nrOfElements: Int, val nrOfMessages: Int,mailbox: Mailbox) : Actor<BigDecimal, Any>(mailbox){
    protected override fun processRequest(rp: RP<BigDecimal>) {
        val prp = object: RP<BigDecimal>() {
            val start = System.currentTimeMillis();
            var pi = BigDecimal.ZERO
            var  nrOfResults = 0;
            public override fun processResponse(result: BigDecimal?) {
                pi = pi.add(result!!);
                nrOfResults += 1;
                if (nrOfResults == nrOfMessages) {
                    val duration = System.currentTimeMillis() - start;
                    System.out.println(java.lang.String.format("\tPi approximation: \t\t%s\n\tCalculation time: \t%s milliseconds", pi, duration));
                    (rp as RP<Any>).processResponse(null);
                }
            }
        };
        0..(nrOfMessages-1) forEach {
            val mailbox = getMailboxFactory()!!.createAsyncMailbox()!!;
            WorkerA(it,nrOfElements, mailbox).send(this,prp)
        }
    }
}

class WorkerA(val start: Int, val nrOfElements: Int,mailbox: Mailbox) : Actor<BigDecimal, BigDecimal>(mailbox){
    protected override fun processRequest(rp: RP<BigDecimal>) {
        var acc = BigDecimal.ZERO
        ((start * nrOfElements)..((start + 1) * nrOfElements - 1)) forEach {
            acc = acc.add(BigDecimal(4.0 * (1 - (it % 2) * 2) / (2 * it + 1)))
        }
        rp.processResponse(acc)
    }
}