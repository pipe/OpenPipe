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
    WebSocket ws;
    private final OneOffCert cert;
    HashMap<String, Session> sessions;
    private String sessionId;
    private Consumer<Object> candidateSender;

    public WebSocketClient(CountDownLatch latch, String id) throws Exception {
        this.latch = latch;
        this.id = id;
        this.cert = new OneOffCert();
        sessions = new HashMap();
        candidateSender = (c) -> {
            var m = new LCDMessage();
            m.candidate = ((RTCIceCandidate) c).toString();
            m.from = id;
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
        var tick = new EchoTicker(this, 10000);
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        Log.debug("onText received " + data);
        var json = data.toString();
        var mess = LCDMessage.fromJson(json);
        if ((mess.to != null) && (mess.to.equals(this.id))) {
            if (mess.session != null) {
                Session session = sessions.get(mess.session);
                if (session == null) {
                    session = new Session(mess.session, cert);
                    sessions.put(mess.session, session);
                    sessionId = mess.session;
                    // this leaks sessions - they should die at some point...
                }
                Log.debug("parsed message of type " + mess.ltype);
                if (mess.ltype != null) {
                    var act = mess.ltype.toLowerCase();
                    var startIce = false;
                    LCDMessage reply = null;
                    switch (act) {
                        case "echo":
                            break;
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
                            session.startIce(candidateSender);
                        }
                    }
                }
            }
        } else {
            Log.warn("Message wasn't for us " + mess.to);
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (error != null){
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
