package chess.android.arduino.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * Simple UI demonstrating how to connect to a Bluetooth device,
 * send and receive messages using Handlers, and update the UI.
 */
public class BluetoothActivity extends Activity {

    // Tag for logging
    private static final String TAG = "BluetoothActivity";
    // MAC address of remote Bluetooth device
    // Replace this with the address of your own module
    //private static final String address = "00:06:66:66:33:89";

    private static final int Y_MAX = 255;
    private static final int X_MAX = 90;
    private static final String CONNECTED_STATE = "connectedState";
    private static final String LIGHTS_STATE = "lightsState";
    private static final String BRAKES_STATE = "brakesState";
    private boolean connected = false;
    private boolean lightsOn = false;
    private boolean brakesOn = false;

    // The thread that does all the work
    static BluetoothThread btt;

    // Handler for writing messages to the Bluetooth connection
    Handler writeHandler;

    /**
     * Launch the Bluetooth thread.
     */
    @SuppressLint("SetTextI18n")
    public void connectButtonPressed(View v) {
        Log.v(TAG, "Connect button pressed.");

        // Initialize the Bluetooth thread, passing in a MAC address
        // and a Handler that will receive incoming messages
        if (btt==null) btt = new BluetoothThread(new ReadHandler(this));

        // Get the handler that is used to send messages
        writeHandler = btt.getWriteHandler();

        // Run the thread
        btt.start();

        Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
    }

    /**
     * Kill the Bluetooth thread.
     */
    public void disconnectButtonPressed(View v) {
        Log.v(TAG, "Disconnect button pressed.");

        if(btt != null) {
            btt.interrupt();
            btt = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        ToggleButton lightsToggle = findViewById(R.id.lightsToggleButton);
        if (savedInstanceState!=null) {
            connected = savedInstanceState.getBoolean(CONNECTED_STATE);
            lightsOn = savedInstanceState.getBoolean(LIGHTS_STATE);
            brakesOn = savedInstanceState.getBoolean(BRAKES_STATE);
        }

        findViewById(R.id.connectButton).setEnabled(!connected);
        findViewById(R.id.disconnectButton).setEnabled(connected);

        if (btt!=null) {
            writeHandler = btt.getWriteHandler();
            btt.setReadHandler(new ReadHandler(this));
        }

        lightsToggle.setEnabled(connected);
        lightsToggle.setChecked(lightsOn);
        lightsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Message msg = Message.obtain();
            String lightsStatus;
            if (isChecked) {
                lightsStatus = "L1";
            } else {
                lightsStatus = "L0";
            }
            lightsOn = isChecked;
            msg.obj = lightsStatus;
            writeHandler.sendMessage(msg);
        });

        ToggleButton brakesToggle = findViewById(R.id.brakeToggleButton);
        brakesToggle.setEnabled(connected);
        brakesToggle.setChecked(brakesOn);
        brakesToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Message msg = Message.obtain();
            String brakesStatus;
            if (isChecked) {
                brakesStatus = "B1";
            } else {
                brakesStatus = "B0";
            }
            brakesOn = isChecked;
            msg.obj = brakesStatus;
            writeHandler.sendMessage(msg);
        });

        JoystickView joystick = findViewById(R.id.joystickView);
        joystick.setEnabled(connected);
        joystick.setOnMoveListener((angle, strength) -> {
            Message msg = Message.obtain();
            msg.obj = parseJoystickInput(angle, strength);
            writeHandler.sendMessage(msg);
        });
    }

    private String parseJoystickInput(int angle, int strength) {
        int x, y;
        x = Double.valueOf((strength*X_MAX*Math.cos(Math.toRadians(angle)))/100).intValue()+X_MAX;
        x = Math.max(75, Math.min(105, x));
        y = Double.valueOf((strength*Y_MAX*Math.sin(Math.toRadians(angle)))/100).intValue();

        return "X"+x+"#"+"Y"+y;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(CONNECTED_STATE, connected);
        savedInstanceState.putBoolean(LIGHTS_STATE, lightsOn);
        savedInstanceState.putBoolean(BRAKES_STATE, brakesOn);
        super.onSaveInstanceState(savedInstanceState);
    }

    static class ReadHandler extends Handler {
        private final WeakReference<BluetoothActivity> bActivity;

        ReadHandler(BluetoothActivity activity) {
            bActivity = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message message) {
            String s = (String) message.obj;
            BluetoothActivity bluetoothActivity = bActivity.get();

            // Do something with the message
            if (s.equals("CONNECTED")) {
                Toast.makeText(bluetoothActivity.getApplicationContext(),
                        "Connected.", Toast.LENGTH_SHORT).show();
                bluetoothActivity.findViewById(R.id.joystickView).setEnabled(true);
                bluetoothActivity.findViewById(R.id.brakeToggleButton).setEnabled(true);
                bluetoothActivity.findViewById(R.id.lightsToggleButton).setEnabled(true);
                bluetoothActivity.findViewById(R.id.connectButton).setEnabled(false);
                bluetoothActivity.findViewById(R.id.disconnectButton).setEnabled(true);
                bluetoothActivity.connected = true;

            } else if(s.equals("DISCONNECTED")) {
                Toast.makeText(bluetoothActivity.getApplicationContext(),
                        "Disconnected.", Toast.LENGTH_SHORT).show();
                bluetoothActivity.findViewById(R.id.joystickView).setEnabled(false);
                bluetoothActivity.findViewById(R.id.brakeToggleButton).setEnabled(false);
                bluetoothActivity.findViewById(R.id.lightsToggleButton).setEnabled(false);
                bluetoothActivity.findViewById(R.id.connectButton).setEnabled(true);
                bluetoothActivity.findViewById(R.id.disconnectButton).setEnabled(false);
                bluetoothActivity.connected = false;
                if (btt!=null) btt.interrupt();
                btt = null;
            } else if(s.startsWith("ERROR:")) {
                Toast.makeText(bluetoothActivity.getApplicationContext(),
                        s, Toast.LENGTH_SHORT).show();
                bluetoothActivity.findViewById(R.id.joystickView).setEnabled(false);
                bluetoothActivity.findViewById(R.id.brakeToggleButton).setEnabled(false);
                bluetoothActivity.findViewById(R.id.lightsToggleButton).setEnabled(false);
                bluetoothActivity.findViewById(R.id.connectButton).setEnabled(true);
                bluetoothActivity.findViewById(R.id.disconnectButton).setEnabled(false);
                if (btt!=null) btt.interrupt();
                btt = null;
                bluetoothActivity.connected = false;
            }   else {
                Toast.makeText(bluetoothActivity.getApplicationContext(),
                        s, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
