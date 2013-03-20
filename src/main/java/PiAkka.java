import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.RoundRobinRouter;
import scala.concurrent.duration.Duration;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;


public class PiAkka implements PiCalculator {
    private static Object obj = new Object();

    static class Calculate {
    }

    static class Work {
        private final int start;
        private final int nrOfElements;

        public Work(int start, int nrOfElements) {
            this.start = start;
            this.nrOfElements = nrOfElements;
        }

        public int getStart() {
            return start;
        }

        public int getNrOfElements() {
            return nrOfElements;
        }
    }


    static class PiApproximation {
        private final BigDecimal pi;
        private final Duration duration;

        public PiApproximation(BigDecimal pi, Duration duration) {
            this.pi = pi;
            this.duration = duration;
        }

        public BigDecimal getPi() {
            return pi;
        }

        public Duration getDuration() {
            return duration;
        }
    }


    public static class Worker extends UntypedActor {

        private BigDecimal calculatePiFor(int start, int nrOfElements) {
            BigDecimal acc = BigDecimal.ZERO;
            for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                acc = acc.add(new BigDecimal(4.0 * (1 - (i % 2) * 2) / (2 * i + 1)));
            }

            return acc;
        }


        public void onReceive(Object message) {
            if (message instanceof Work) {
                Work work = (Work) message;
                BigDecimal result = calculatePiFor(work.getStart(), work.getNrOfElements());
                getSender().tell(result, getSelf());
            } else {
                unhandled(message);
            }
        }
    }


    public static class Master extends UntypedActor {
        private final int nrOfMessages;
        private final int nrOfElements;

        private BigDecimal pi = BigDecimal.ZERO;
        private int nrOfResults;
        private long start;

        private final ActorRef listener;
        private final ActorRef workerRouter;

        public Master(final int nrOfWorkers, int nrOfMessages, int nrOfElements, ActorRef listener) {
            this.nrOfMessages = nrOfMessages;
            this.nrOfElements = nrOfElements;
            this.listener = listener;

            workerRouter = this.getContext().actorOf(new Props(Worker.class).withRouter(new RoundRobinRouter
                    (nrOfWorkers)).withDispatcher("akka.actor" +
                    ".my-thread-pool-dispatcher"),
                    "workerRouter");
            start = System.currentTimeMillis();
        }

        public void onReceive(Object message) {
            if (message instanceof Calculate) {
                for (int start = 0; start < nrOfMessages; start++) {
                    workerRouter.tell(new Work(start, nrOfElements), getSelf());
                }
            } else if (message instanceof BigDecimal) {

                BigDecimal result = (BigDecimal) message;
                pi = pi.add(result);
                nrOfResults += 1;
                if (nrOfResults == nrOfMessages) {
                    // Send the result to the listener
                    Duration duration = Duration.create(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
                    listener.tell(new PiApproximation(pi, duration), getSelf());
                    // Stops this actor and all its supervised children
                    getContext().stop(getSelf());
                }
            } else {
                unhandled(message);
            }
        }
    }


    public static class Listener extends UntypedActor {
        public void onReceive(Object message) {
            if (message instanceof PiApproximation) {
                PiApproximation approximation = (PiApproximation) message;
                System.out.println(String.format("\tPi approximation: \t\t%s\n\tCalculation time: \t%s",
                        approximation.getPi(), approximation.getDuration()));
                getContext().system().shutdown();
                synchronized (obj) {
                    obj.notify();
                }
            } else {
                unhandled(message);
            }
        }
    }


    public void calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages) {
        System.out.println("AKKA - calc - (" + nrOfWorkers + "," + nrOfElements + "," + nrOfMessages + ")");
        // Create an Akka system
        ActorSystem system = ActorSystem.create("PiSystem");

        // create the result listener, which will print the result and shutdown the system
        final ActorRef listener = system.actorOf(new Props(Listener.class).withDispatcher("akka.actor" +
                ".my-thread-pool-dispatcher"), "listener");

        // create the master
        ActorRef master = system.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new Master(nrOfWorkers, nrOfMessages, nrOfElements, listener);
            }
        }), "master");

        // start the calculation
        master.tell(new Calculate());
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
