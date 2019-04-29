package chess.android.arduino.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BluetoothThread extends Thread {

    private static final char DELIMITER = '#';
    //private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FC");
    private static final String address = "00:18:91:D6:9F:83";

    private BluetoothSocket socket;
    private OutputStream outStream;
    private Handler readHandler;
    private final Handler writeHandler;

    BluetoothThread(Handler handler) {
        this.readHandler = handler;
        writeHandler = new WriteHandler(this);
    }

    Handler getWriteHandler() {
        return writeHandler;
    }

    void setReadHandler(Handler readHandler) {
        this.readHandler = readHandler;
    }

    private void connect() throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if ((adapter == null) || (!adapter.isEnabled())) {
            throw new Exception("Bluetooth adapter not found or not enabled!");
        }

        BluetoothDevice remoteDevice = adapter.getRemoteDevice(address);
        if (remoteDevice == null) throw new Exception("Can't find HC-06");

        socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
        adapter.cancelDiscovery();
        socket.connect();
        outStream = socket.getOutputStream();
    }

    private void disconnect() {

        if (outStream != null) {
            try {
                outStream.flush();
                outStream.close();
            } catch (Exception e) {
                sendErrorToReadHandler(e.getLocalizedMessage());
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                sendErrorToReadHandler(e.getLocalizedMessage());
            }
        }
    }

    private void write(String s) {
        try {
            s += DELIMITER;
            outStream.write(s.getBytes());
        } catch (Exception e) {
            sendErrorToReadHandler(e.getLocalizedMessage());
        }
    }

    private void sendToReadHandler(String s) {
        Message msg = Message.obtain();
        msg.obj = s;
        readHandler.sendMessage(msg);
    }

    private void sendErrorToReadHandler(String errorText) {
        Message msg = Message.obtain();
        msg.obj = "ERROR: " + errorText;
        readHandler.sendMessage(msg);
    }

    public void run() {
        try {
            connect();
            sendToReadHandler("CONNECTED");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorToReadHandler(e.getLocalizedMessage());
            disconnect();
            return;
        }

        KeepAliveThread keepAliveThread = new KeepAliveThread(writeHandler);
        keepAliveThread.start();

        while (!this.isInterrupted()) {
            if (outStream == null) {
                break;
            }
        }

        keepAliveThread.interrupt();
        disconnect();
        sendToReadHandler("DISCONNECTED");
    }

    static class WriteHandler extends Handler {
        private final WeakReference<BluetoothThread> btThread;

        WriteHandler(BluetoothThread btThread) {
            this.btThread = new WeakReference<>(btThread);
        }

        @Override
        public void handleMessage(Message message) {
            BluetoothThread bluetoothThread = btThread.get();
            bluetoothThread.write((String) message.obj);
        }
    }
}
