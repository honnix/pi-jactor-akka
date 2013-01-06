package kotlin.actor

import org.agilewiki.jactor.lpc.JLPCActor
import org.agilewiki.jactor.lpc.Request
import com.sun.org.apache.regexp.internal.RE
import org.agilewiki.jactor.RP
import org.agilewiki.jactor.JAFuture
import org.agilewiki.jactor.Mailbox
import org.agilewiki.jactor.apc.APCRequestSource

/**
 * Created with IntelliJ IDEA.
 * User: daniel
 * Date: 06.01.13
 * Time: 17:32
 * To change this template use File | Settings | File Templates.
 */
public abstract class Actor<reified REQUEST_TYPE, reified RESPONSE_TYPE>(val _mailbox: Mailbox): JLPCActor() {
    {
        this.initialize(_mailbox)
    }

    val req = object : Request<RESPONSE_TYPE, JLPCActor>(){
        public override fun isTargetType(targetActor: org.agilewiki.jactor.Actor?) = true
        public override fun processRequest(targetActor: JLPCActor?, rp: RP<out Any?>?) {
             this@Actor.processRequest(rp as RP<REQUEST_TYPE>)
        }
    }

    public fun send(future: JAFuture): RESPONSE_TYPE? {
        return req.send(future, this)
    }

    public fun send(requestSource : APCRequestSource,response :  RP<RESPONSE_TYPE> ){
         req.send(requestSource, this, response);
    }

    protected abstract fun processRequest(rp : RP<REQUEST_TYPE>);
}