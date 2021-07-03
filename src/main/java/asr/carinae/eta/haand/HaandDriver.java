package asr.carinae.eta.haand;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

/**
 * Created by moses gichangA on 6/9/2016.
 */
public class HaandDriver {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private int counter;
    private volatile boolean stopWorker;

    private Vector<DataListener> listeners = new Vector<DataListener>();
    static HaandDriver haandDriver = null;

    private HaandDriver() {
        this.findBT();
        try {
            this.openBT();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static protected HaandDriver getHaandDriver() {
        if (haandDriver == null)
            HaandDriver.haandDriver = new HaandDriver();
        return HaandDriver.haandDriver;
    }

    private void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) {
            MainInterface.getInstance().enableBluetooth();
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("RNBT-34F3")) {
                    mmDevice = device;
                    System.out.println(device.toString());
                    break;
                }
            }
        }
        System.out.println("Haand device identified...");
    }

    /*private void pairBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) return;
        if(!mBluetoothAdapter.isEnabled()) {
            MainInterface.getInstance().enableBluetooth();
        }

        //Set<BluetoothDevice> pairedDevices =
                mBluetoothAdapter.startDiscovery();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("RNBT-34F3")) {
                    mmDevice = device;
                    System.out.println(device.toString());
                    break;
                }
            }
        }
        System.out.println("Haand device identified...");
    }*/

    private void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        listenForData();
        System.out.println("Haand device opened. Listening for data from the device...");
    }

    private void listenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            fireDataArrived(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                    try {
                        Thread.sleep(480);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        workerThread.start();
    }

    protected void addDataListener(DataListener listener) {
        this.listeners.add(listener);
    }

    private void fireDataArrived(String data) {
        for (DataListener ls : this.listeners)
            ls.dataReceived(data);
    }

    protected void sendData(String msg) throws IOException {
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
    }

    private void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
    }
}

interface DataListener {
    void dataReceived(String data);
}

class HaandBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            ;
            ;
            ;
            System.out.println(device.getName());
        }
    }
}