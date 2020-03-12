package com.example.final3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends Activity {
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    ImageView mBlueIv;
    final byte delimiter = 33;
    int readBufferPosition = 0;

    public void sendBtMsg(String msg2send) {
        //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            }

            String msg = msg2send;
            //msg += "\n";
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //message

    @Override
    protected void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
        //mBlueIv=findViewById(R.id.bluetoothI);
        //mBlueIv.setImageResource(R.drawable.ic_action_name);
            //main code + variables

        final Handler handler =new Handler();
        final Button VoltD = (Button) findViewById(R.id.VoltM);
        final TextView label= (TextView) findViewById(R.id.Label);
        //final TextView label1= (TextView) findViewById(R.id.Label1);
        //final TextView label2= (TextView) findViewById(R.id.Label2);
        mBlueIv=findViewById(R.id.bluetoothI);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //mBlueIv.setImageResource(R.drawable.ic_action_name);



        //Thread New Creation to prevent pi break
        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                sendBtMsg(btMsg);
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    boolean workDone = false;
                    try {
                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e("Aquarium recv bt","bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    //The variable data now contains our full command
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {

                                            label.setText("Voltage: "+data +" mV");
                                        }
                                    });
                                    workDone = true;
                                    break;
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                            if (workDone == true){
                                mmSocket.close();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }; //end of Multi thread bluetooth RFCOMM channel



        //asking pi for value : Command is Voltage
        VoltD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                (new Thread(new workerThread("voltage"))).start();
            }
            });

        if(!mBluetoothAdapter.isEnabled())
        {
            mBlueIv.setImageResource(R.drawable.ic_action_off);
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
            showToast("Making Device Discoverable");

        }
        if(mBluetoothAdapter.isEnabled())
        {
            mBlueIv.setImageResource(R.drawable.ic_action_name);
            showToast("Making Device Discoverable");

        }

        //Device List

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("raspberrypi-3")) //Note, you will need to change this to match the name of your device
                {
                    Log.e("Voltage",device.getName());
                    mmDevice = device;
                    break;
                }
            }
        }

    } //end of oncreate


    private void showToast(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }




} //end of file
