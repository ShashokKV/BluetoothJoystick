package chess.android.arduino.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * A thread that connects to a remote device over Bluetooth, and reads/writes data
 * using message Handlers. A delimiter character is used to parse messages from a stream,
 * and must be implemented on the other side of the connection as well. If the connection
 * fails, the thread exits.
 * <p>
 * Usage:
 * <p>
 * BluetoothThread t = BluetoothThread("00:06:66:66:33:89", new Handler() {
 * Override
 * public void handleMessage(Message message) {
 * String msg = (String) message.obj;
 * do_something(msg);
 * }
 * });
 * <p>
 * Handler writeHandler = t.getWriteHandler();
 * t.start();
 */
public class BluetoothThread extends Thread {

    // Delimiter used to separate messages
    private static final char DELIMITER = '#';

    // UUID that specifies a protocol for generic bluetooth serial communication
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String address = "98:D3:41:F9:2C:B7";

    // Bluetooth socket of active connection
    private BluetoothSocket socket;

    // Streams that we read from and write to
    private OutputStream outStream;

    // Handlers used to pass data between threads
    private Handler readHandler;
    private final Handler writeHandler;


    /**
     * Constructor, takes in the MAC address of the remote Bluetooth device
     * and a Handler for received messages.
     */
    BluetoothThread(Handler handler) {
        this.readHandler = handler;
        writeHandler = new WriteHandler(this);
    }

    /**
     * Return the write handler for this connection. Messages received by this
     * handler will be written to the Bluetooth socket.
     */
    Handler getWriteHandler() {
        return writeHandler;
    }

    void setReadHandler(Handler readHandler) {
        this.readHandler = readHandler;
    }

    /**
     * Connect to a remote Bluetooth socket, or throw an exception if it fails.
     */
    private void connect() throws Exception {
        // Get this device's Bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if ((adapter == null) || (!adapter.isEnabled())) {
            throw new Exception("Bluetooth adapter not found or not enabled!");
        }

        BluetoothDevice remoteDevice = adapter.getRemoteDevice(address);

        if (remoteDevice==null) throw new Exception("Can't find HC-06");

        // Create a socket with the remote device using this protocol
        socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);

        // Make sure Bluetooth adapter is not in discovery mode
        adapter.cancelDiscovery();

        // Connect to the socket
        socket.connect();

        // Get input and output streams from the socket
        outStream = socket.getOutputStream();
    }

    /**
     * Disconnect the streams and socket.
     */
    private void disconnect() {

        if (outStream != null) {
            try {
                outStream.flush();
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Write data to the socket.
     */
    private void write(String s) {
        try {
            // Add the delimiter
            s += DELIMITER;

            // Convert to bytes and write
            outStream.write(s.getBytes());

        } catch (Exception e) {
            sendToReadHandler(e.getLocalizedMessage());
        }
    }

    /**
     * Pass a message to the read handler.
     */
    private void sendToReadHandler(String s) {
        Message msg = Message.obtain();
        msg.obj = s;
        readHandler.sendMessage(msg);
    }

    /**
     * Entry point when thread.start() is called.
     */
    public void run() {
        // Attempt to connect and exit the thread if it failed
        try {
            connect();
            sendToReadHandler("CONNECTED");
        } catch (Exception e) {
            sendToReadHandler("CONNECTION FAILED");
            disconnect();
            return;
        }

        // Loop continuously, reading data, until thread.interrupt() is called
        while (!this.isInterrupted()) {
            // Make sure things haven't gone wrong
            if (outStream == null) {
                break;
            }
        }

        // If thread is interrupted, close connections
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
