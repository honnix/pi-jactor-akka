package mailbox;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

import akka.actor.ActorRef;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;
import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.YieldingWaitStrategy;

public class RingBufferMessageQueue implements MessageQueue {
    public static class EnvelopeEvent {
        private Envelope envelope;

        public Envelope getEnvelope() {
            return envelope;
        }

        public EnvelopeEvent() {
            super();
        }

        public void setEnvelope(Envelope envelope) {
            this.envelope = envelope;
        }

        public final static EventFactory<EnvelopeEvent> EVENT_FACTORY = new EventFactory<EnvelopeEvent>() {
            public EnvelopeEvent newInstance() {
                return new EnvelopeEvent();
            }
        };
    }

    public static class EnvelopTranslator implements EventTranslator<EnvelopeEvent> {
        private Envelope envelope;

        public EnvelopTranslator(Envelope envelope) {
            this.envelope = envelope;
        }

        @Override
        public void translateTo(EnvelopeEvent event, long sequence) {
            event.setEnvelope(envelope);
        }
    }

    private static final int BUFFER_SIZE = 1024 * 64;

    private RingBuffer<EnvelopeEvent> ringBuffer;

    private SequenceBarrier sequenceBarrier;

    private Sequence sequence;

    public RingBufferMessageQueue() {
        super();

        ringBuffer =
                createSingleProducer(EnvelopeEvent.EVENT_FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());
        sequenceBarrier = ringBuffer.newBarrier();
        sequenceBarrier.clearAlert();
        sequence = new Sequence(-1L);
    }

    /**
     * Try to enqueue the message to this queue, or throw an exception.
     */
    @Override
    public void enqueue(ActorRef receiver, Envelope handle) {
        ringBuffer.publishEvent(new EnvelopTranslator(handle));
    }

    /**
     * Try to dequeue the next message from this queue, return null failing that.
     */
    @Override
    public Envelope dequeue() {
        long nextSequence = sequence.get() + 1L;

        Envelope envelope = null;
        try {
            sequenceBarrier.waitFor(nextSequence);
            envelope = ringBuffer.getPublished(nextSequence).getEnvelope();

            sequence.set(nextSequence);
        } catch (AlertException ex) {
        } catch (InterruptedException e) {
        }

        return envelope;
    }

    /**
     * Should return the current number of messages held in this queue; may
     * always return 0 if no other value is available efficiently. Do not use
     * this for testing for presence of messages, use `hasMessages` instead.
     */
    @Override
    public int numberOfMessages() {
        long nextSequence = sequence.get() + 1L;

        int number = 0;
        try {
            long availableSequence = sequenceBarrier.waitFor(nextSequence);
            number = (int) (availableSequence - nextSequence + 1);
        } catch (AlertException e) {
        } catch (InterruptedException e) {
        }

        return number;
    }

    /**
     * Indicates whether this queue is non-empty.
     */
    @Override
    public boolean hasMessages() {
        return numberOfMessages() > 0;
    }

    /**
     * Called when the mailbox this queue belongs to is disposed of. Normally it
     * is expected to transfer all remaining messages into the dead letter queue
     * which is passed in. The owner of this MessageQueue is passed in if
     * available (e.g. for creating DeadLetters()), “/deadletters” otherwise.
     */
    @Override
    public void cleanUp(ActorRef owner, MessageQueue deadLetters) {
    }
}
