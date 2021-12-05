package com.hcmoon.badgerai;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

//색상표
//https://material.io/resources/color/#!/?view.left=0&view.right=0&primary.color=1A237E
//조이스틱
//https://github.com/controlwear/virtual-joystick-android
//레이아웃 테두리
//https://stickode.com/detail.html?no=1171
//버튼 둥글게
//https://threeidiotscoding.tistory.com/m/34
//alert dialog
//https://lktprogrammer.tistory.com/155
//블루투스
//https://bugwhale.tistory.com/entry/android-bluetooth-application
//http://blog.moramcnt.com/?p=1427
//https://www.hardcopyworld.com/?p=3126
//https://popcorn16.tistory.com/195
//https://popcorn16.tistory.com/196

public class MainActivity extends AppCompatActivity {
    private boolean checked[]= {true, true, true, false};
    private boolean SYSTEM_LOG_FLAG = checked[0];
    private boolean BLUETOOTH_LOG_FLAG = checked[1];
    private boolean BLUETOOTH_RECEIVE_LOG_FLAG = checked[2];
    private boolean MOVE_LOG_FLAG = checked[3];


    public static final int MOVE_LOG_INTERVAL = 700;
    public static final int MOVE_STOP = 0;
    public static final int MOVE_DIAGONAL_ATTEN = 2;

    public static final int BT_REQUEST_ENABLE = 1;
    public static final int BT_MESSAGE_READ = 2;
    public static final int BT_CONNECTING_STATUS = 3;
    public static final int BLUETOOTH_SEND_INTERVAL = 100;

    private BluetoothAdapter blueAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private List<String> mListPairedDevices;
    private String device_id = "";
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private Handler mBluetoothHandler;
    private ConnectedBluetoothThread mThreadConnectedBluetooth;
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private TextView textView_log;
    private ScrollView scrollView_log;
    private EditText editText_log;
    private Button button_log;

    private Vibrator vibrator;
    private ToneGenerator toneGenerator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        //Log Initialize
        textView_log = findViewById(R.id.logbox);
        scrollView_log = findViewById(R.id.log_scroll);
        editText_log = findViewById(R.id.logedit);
        button_log = (Button)findViewById(R.id.logsend);

        button_log.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("[BLUETOOTH] Send Message : " + editText_log.getText(), BLUETOOTH_LOG_FLAG);
                mThreadConnectedBluetooth.write(editText_log.getText().toString());
                editText_log.setText("");
            }
        });

        // Bluetooth Adapter Initialize
        blueAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){

                    String readMessage = null;

                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        log("[BLUETOOTH] RECEIVE ERROR !" , BLUETOOTH_RECEIVE_LOG_FLAG);
                        e.printStackTrace();
                    }

                    if(readMessage != null) {
                        log("[WARNING] OUT OF LANE " + readMessage, SYSTEM_LOG_FLAG);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                        else
                            vibrator.vibrate(1000);
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000);
                    }
                }
            }
        };

        // Joy
        JoystickView joystick = (JoystickView) findViewById(R.id.joystick);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if (mBluetoothSocket != null) {
                    if (strength == 0) {
                        log("[Move] Direction: NONE / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(MOVE_STOP + " " + MOVE_STOP);
                    } else if (75 >= angle && angle >= 15) {
                        log("[Move] Direction: RIGHT & UP / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(strength + " " + (strength / MOVE_DIAGONAL_ATTEN));
                    } else if (105 >= angle && angle >= 75) {
                        log("[Move] Direction: UP / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(strength + " " + strength);
                    } else if (165 >= angle && angle >= 105) {
                        log("[Move] Direction: LEFT & UP / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write((strength / MOVE_DIAGONAL_ATTEN) + " " + strength);
                    } else if (195 >= angle && angle >= 165) {
                        log("[Move] Direction: LEFT / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(MOVE_STOP + " " + strength);
                    } else if (255 >= angle && angle >= 195) {
                        log("[Move] Direction: LEFT & DOWN / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(MOVE_STOP + " " + MOVE_STOP);
                    } else if (285 >= angle && angle >= 255) {
                        log("[Move] Direction: DOWN / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(MOVE_STOP + " " + MOVE_STOP);
                    } else if (345 >= angle && angle >= 285) {
                        log("[Move] Direction: RIGHT & DOWN / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(MOVE_STOP + " " + MOVE_STOP);
                    } else {
                        log("[Move] Direction: RIGHT / Strength : " + strength, MOVE_LOG_FLAG);
                        mThreadConnectedBluetooth.write(strength + " " + MOVE_STOP);
                    }
                }
            }
        }, MOVE_LOG_INTERVAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.bluetooth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.search:
                log("[BLUETOOTH] Search Bluetooth Device", BLUETOOTH_LOG_FLAG);
                if (blueAdapter.isEnabled()) {
                    mPairedDevices = blueAdapter.getBondedDevices();

                    if (mPairedDevices.size() > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("SELECT DEVICE");

                        mListPairedDevices = new ArrayList<String>();
                        for (BluetoothDevice device : mPairedDevices) {
                            mListPairedDevices.add(device.getName());
                            //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                        }
                        final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                        mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                device_id = items[item].toString();
                                log("[SYSTEM] SELECTED Device ID : " + device_id, SYSTEM_LOG_FLAG);
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        log("[BLUETOOTH] No paired device found", BLUETOOTH_LOG_FLAG);
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Turn on Bluetooth", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.connect:
                if(device_id == "") {
                    log("[BLUETOOTH] Search Bluetooth First", BLUETOOTH_LOG_FLAG);
                    break;
                }
                log("[BLUETOOTH] Connect Bluetooth Device", BLUETOOTH_LOG_FLAG);
                for(BluetoothDevice tempDevice : mPairedDevices) {
                    if (device_id.equals(tempDevice.getName())) {
                        log("[BLUETOOTH] Paired Device : " + tempDevice.getName(), BLUETOOTH_LOG_FLAG);
                        mBluetoothDevice = tempDevice;
                        break;
                    }
                }
                try {
                    mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                    mBluetoothSocket.connect();
                    log("[BLUETOOTH] Paired", BLUETOOTH_LOG_FLAG);
                    mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
                    mThreadConnectedBluetooth.start();
                    mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
                } catch (IOException e) {
                    log("[BLUETOOTH] Pair ERROR", BLUETOOTH_LOG_FLAG);
                }
                break;

            case R.id.onoff:
                if (blueAdapter == null) {
                    log("[BLUETOOTH] Bluetooth ERROR", BLUETOOTH_LOG_FLAG);
                    makeToast("Bluetooth ERROR");
                } else{
                    if(!blueAdapter.isEnabled()) {
                        blueAdapter.enable();
                        log("[BLUETOOTH] Bluetooth ON", BLUETOOTH_LOG_FLAG);
                        item.setTitle("Bluetooth OFF");
                        makeToast("Bluetooth ON");
                    } else {
                        blueAdapter.disable();
                        log("[BLUETOOTH] Bluetooth OFF", BLUETOOTH_LOG_FLAG);
                        item.setTitle("Bluetooth ON");
                        makeToast("Bluetooth OFF");
                    }
                }
                break;

            case R.id.log_init :
                textView_log.setText("Badger AI Log\n");
                break;

            case R.id.log_onoff:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final ArrayList<String> selectedItems = new ArrayList<String>();
                final String[] items = getResources().getStringArray(R.array.LOG);

                builder.setTitle("LOG SETTING");
                builder.setMultiChoiceItems(R.array.LOG, checked, new DialogInterface.OnMultiChoiceClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int pos, boolean isChecked)
                    {
                        if(isChecked == true)
                            if(items[pos].equals("SYSTEM")) {
                                SYSTEM_LOG_FLAG = true;
                                checked[0] = true;
                            } else if(items[pos].equals("BLUETOOTH")){
                                BLUETOOTH_LOG_FLAG = true;
                                checked[1] = true;
                            } else if(items[pos].equals("BLUETOOTH_RECEIVE")) {
                                BLUETOOTH_RECEIVE_LOG_FLAG = true;
                                checked[2] = true;
                            } else if(items[pos].equals("MOVE")) {
                                MOVE_LOG_FLAG = true;
                                checked[3] = true;
                            } else
                                log("[LOG] LOG SETTING ERROR", true);
                        else
                            if(items[pos].equals("SYSTEM")) {
                                SYSTEM_LOG_FLAG = false;
                                checked[0] = false;
                            } else if(items[pos].equals("BLUETOOTH")) {
                                BLUETOOTH_LOG_FLAG = false;
                                checked[1] = false;
                            } else if(items[pos].equals("BLUETOOTH RECEIVE")) {
                                BLUETOOTH_RECEIVE_LOG_FLAG = false;
                                checked[2] = false;
                            } else if(items[pos].equals("MOVE")) {
                                MOVE_LOG_FLAG = false;
                                checked[3] = false;
                            } else
                                log("[LOG] LOG SETTING ERROR", true);
                    }
                });

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int pos)
                    {
                        log("[LOG] DONE", true);
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void makeToast(String str) {
        Toast.makeText(getApplicationContext(),str, Toast.LENGTH_LONG).show();
    }

    private void log(String str, boolean flag) {
        if(flag)
            textView_log.setText(textView_log.getText() + str + "\n");
        scrollView_log.fullScroll(View.FOCUS_DOWN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThreadConnectedBluetooth.cancel();
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(BLUETOOTH_SEND_INTERVAL);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
