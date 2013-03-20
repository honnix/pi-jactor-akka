package mailbox;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.MailboxType;
import akka.dispatch.MessageQueue;
import com.typesafe.config.Config;
import scala.Option;

public class RingBufferMailbox implements MailboxType {
    public RingBufferMailbox(ActorSystem.Settings settings, Config config) {

    }
    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        return new RingBufferMessageQueue();
    }
}
