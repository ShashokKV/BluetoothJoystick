package chess.android.arduino.bluetooth;

import android.os.Handler;
import android.os.Message;

public class KeepAliveThread extends Thread {
    // Handler for writing messages to the Bluetooth connection
    private Handler writeHandler;

    KeepAliveThread(Handler writeHandler) {
        this.writeHandler = writeHandler;
    }

    public void run() {
        while (!this.isInterrupted()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
            Message message = Message.obtain();
            message.obj = "T";
            writeHandler.sendMessage(message);
        }
    }
}
