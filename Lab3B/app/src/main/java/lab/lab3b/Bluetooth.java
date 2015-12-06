package lab.lab3b;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Rami on 2015-12-04.
 */
public class Bluetooth extends AsyncTask<Void, Void, String> {
    private BluetoothDevice pulseDevice;
    private BluetoothAdapter adapter;
    private MainActivity main;
    private BluetoothAdapter bluetoothAdapter = null;

    protected Bluetooth(MainActivity main, BluetoothDevice pulseDevice) {
        this.main = main;
        this.pulseDevice = pulseDevice;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected String doInBackground(Void... params) {
        String output = "";

        // an ongoing discovery will slow down the connection
        adapter.cancelDiscovery();

        BluetoothSocket socket = null;
        try {
            socket = pulseDevice
                    .createRfcommSocketToServiceRecord(STANDARD_SPP_UUID);
            socket.connect();

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            os.write(FORMAT);
            os.flush();
            byte[] reply = new byte[1];
            is.read(reply);

            if (reply[0] == ACK) {
                byte[] frame = new byte[4]; // this -obsolete- format specifies
                // 4 bytes per frame
                is.read(frame);
                int value1 = unsignedByteToInt(frame[1]);
                int value2 = unsignedByteToInt(frame[2]);
                output = value1 + "; " + value2 + "\r\n";
            }
        } catch (Exception e) {
            output = e.getMessage();
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e) {
            }
        }

        return output;
    }

    @Override
    protected void onPostExecute(String output) {
        main.displayData(output);
    }

    private static final UUID STANDARD_SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final byte[] FORMAT = {0x02, 0x70, 0x04, 0x02, 0x08, 0x00, (byte) 0x7E, 0x03};
    private static final byte ACK = 0x06;

    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}
