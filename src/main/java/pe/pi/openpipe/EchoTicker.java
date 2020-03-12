/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.openpipe;

import com.phono.srtplight.Log;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author tim
 */
public class EchoTicker {

    EchoTicker(WebSocketClient wsc, int i) {
        var tick = new Timer();
        var task = new TimerTask() {
            @Override
            public void run() {
                Log.debug("echo message");
                var echo = new LCDMessage();
                echo.ltype = "echo";
                echo.to = wsc.id;
                echo.session = echo.to + "-" + echo.to + "-" + System.currentTimeMillis();
                wsc.send(echo);
            }
        };
        tick.schedule(task, i/10, i);
    }

}
