/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.openpipe;

import com.phono.srtplight.Log;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author tim
 */
public class Camera {

    public static void main(String[] args) throws Exception {
        java.security.Security.insertProviderAt(new BouncyCastleProvider(), 0);
        Log.setLevel(Log.VERB);
        CountDownLatch latch = new CountDownLatch(1);
        String id = "testcameratest";
        if (args.length >0){
            id = args[1];
        }
        Log.info("My id is "+id);
        WebSocket ws = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("wss://pi.pe/websocket/?finger="+id), new WebSocketClient(latch,id))
                .join();
        latch.await();
    }

   
}
