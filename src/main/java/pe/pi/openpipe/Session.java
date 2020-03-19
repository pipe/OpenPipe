/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.openpipe;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.util.function.Consumer;
import pe.pi.client.small.SliceConnect;
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;

/**
 *
 * @author tim
 */
public class Session {

    String session;
    private OneOffCert cert;
    private final SliceConnect connect;
    private final AssociationListener assocListener;

    Session(String session, OneOffCert cert) {
        this.session = session;
        this.cert = cert;
        this.connect = new SliceConnect(cert);
        assocListener = new AssociationListener() {
            @Override
            public void onAssociated(Association asctn) {
                Log.debug("associated");
            }

            @Override
            public void onDisAssociated(Association asctn) {
                Log.debug("disassociated");
            }

            @Override
            public void onDCEPStream(SCTPStream stream, String string, int i) throws Exception {
                Log.debug("Got open on " + stream.getLabel());
                stream.setSCTPStreamListener(new SCTPStreamListener() {
                    @Override
                    public void onMessage(SCTPStream stream, String message) {
                        Log.debug("Got message on " + stream.getLabel() + " -> " + message);
                        try {
                            stream.send(message);
                        } catch (Exception ex) {
                            Log.error("Cant send message on" + stream.getLabel());
                        }
                    }

                    @Override
                    public void close(SCTPStream stream) {
                        Log.debug("Got close on " + stream.getLabel());

                    }
                });
            }

            @Override
            public void onRawStream(SCTPStream stream) {
                Log.debug("ignoring raw stream open  on " + stream.getLabel());
            }

        };
    }

    String stripFinger(String fi) {
        String ret = "nope";

        if (fi != null) {
            var bits = fi.split(" ");
            if (bits.length == 2) {
                if (bits[0].equalsIgnoreCase("sha-256")) {
                    ret = bits[1].replace(":", "");
                }
            }
        }
        return ret;
    }

    LCDMessage onOffer(LCDMessage mess) {
        LCDMessage answer = null;
        Log.debug("dealing with an offer");

        if (mess.fingerprint != null) {
            connect.setFarFingerprint(stripFinger(mess.fingerprint));
            connect.setOfferer(false);
            connect.setDtlsClientRole(false);
            connect.setAssociationListener(assocListener);
            if ((mess.ufrag != null) && (mess.upass != null)) {
                try {
                    connect.buildIce(mess.ufrag, mess.upass);
                    answer = mkAnswer(mess);
                } catch (Exception x) {
                    Log.error("failed to build Ice " + x.getMessage());
                }
            } else {
                Log.error("Missing ufrag or upass from offer message");
            }
        } else {
            Log.error("Missing fingerprint from offer message");
        }
        return answer;
    }

    LCDMessage onCandidate(LCDMessage mess) {
        LCDMessage ret = null;
        if ((mess.session.equals(session)) && (mess.ltype.equalsIgnoreCase("candidate"))) {

            var line = mess.candidate;
            String params[] = line.substring(line.indexOf(":") + 1).split(" ");
            if (params.length >= 6) {
                String ctype = "";
                String generation, username, password, raddr = null, rport = null;
                var foundation = params[0];
                var component = params[1];
                var protocol = params[2].toLowerCase();
                var priority = params[3];
                var ip = params[4];
                var port = params[5];
                int index = 6;
                while (index + 1 <= params.length) {
                    String val = params[index + 1];

                    switch (params[index]) {
                        case "typ":
                            ctype = val;
                            break;
                        case "generation":
                            generation = val;
                            break;
                        case "username":
                            username = val;
                            break;
                        case "password":
                            password = val;
                            break;
                        case "raddr":
                            raddr = val;
                            break;
                        case "rport":
                            rport = val;
                            break;
                    }
                    index += 2;
                }
                connect.addCandidate(foundation, component, protocol, priority, ip, port, ctype, raddr, rport);
            }
        }
        return ret;
    }

    private LCDMessage mkAnswer(LCDMessage offer) {
        var answer = new LCDMessage();
        answer.to = offer.from;
        answer.session = offer.session;
        answer.ltype = "answer";
        answer.upass = connect.getPass();
        answer.ufrag = connect.getUfrag();
        answer.fingerprint = connect.getPrint();
        return answer;
    }

    public void startIce(Consumer<Object> cons) {
        connect.startCandidateTrickle(cons);
        connect.startIce();
    }

}
