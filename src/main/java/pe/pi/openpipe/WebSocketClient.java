/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.openpipe;

import com.ipseorama.slice.ORTC.RTCIceCandidate;
import com.phono.srtplight.Log;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 *
 * @author tim
 */
public class WebSocketClient implements WebSocket.Listener {

    private final CountDownLatch latch;
    String id;
    String to;
    WebSocket ws;
    private final OneOffCert cert;
    HashMap<String, Session> sessions;
    private String sessionId;
    private Consumer<Object> candidateSender;
    private EchoTicker tick;
    
    public WebSocketClient(CountDownLatch latch, String id) throws Exception {
        this.latch = latch;
        this.id = id;
        this.cert = new OneOffCert();
        sessions = new HashMap();
        candidateSender = (c) -> {
            var candyS = ((RTCIceCandidate) c).toString();
            Log.debug("candidate to send is "+candyS);
            var m = new LCDMessage();
            m.candidate = candyS;
            m.from = id;
            m.to = to;
            m.session = sessionId;
            m.ltype = "candidate";
            send(m);
        };
    }

    public void send(LCDMessage m) {
        m.from = id;
        var json = m.toString();
        if ((ws != null) && (!ws.isOutputClosed())) {
            ws.sendText(json, true);
            Log.debug("sent " + json);
        } else {
            Log.debug("not sending message ");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        ws = webSocket;
        Log.debug("onOpen using subprotocol " + webSocket.getSubprotocol());
        tick = new EchoTicker(this, 10000);
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        Log.debug("onText received " + data);
        var json = data.toString();
        var mess = LCDMessage.fromJson(json);
        if ((mess.to != null) && (mess.to.equals(this.id))) {

            Log.debug("parsed message of type " + mess.ltype);
            if (mess.ltype != null) {
                var act = mess.ltype.toLowerCase();
                var startIce = false;
                LCDMessage reply = null;
                Session session = null;
                if (mess.session != null) {
                    session = sessions.get(mess.session);
                    if (session == null) {
                        session = new Session(mess.session, cert);
                        sessions.put(mess.session, session);
                        sessionId = mess.session;
                        // this leaks sessions - they should die at some point...
                    }
                    switch (act) {
                        case "offer":
                            reply = session.onOffer(mess);
                            startIce = true;
                            break;
                        case "candidate":
                            reply = session.onCandidate(mess);
                            break;
                    }
                    if (reply != null) {
                        send(reply);
                        if (startIce) {
                            to = reply.to;
                            session.startIce(candidateSender);
                        }
                    }
                } else {
                     if (act.equals("echo")){
                           Log.debug("echo received.");
                           tick.seen();
                     }
                }
            }
        } else {
            Log.warn("Message wasn't for us " + mess.to);
        }
        return WebSocket.Listener.super.onText(webSocket,  data,  last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (error != null) {
            Log.debug("Bad day! " + error.getMessage());
            error.printStackTrace();
        } else {
            Log.debug("on error with null error ?!?");
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket,
            int statusCode,
            String reason) {
        Log.debug("onClose received " + reason);
        latch.countDown();
        return null;
    }
}
