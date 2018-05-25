package snacker.mahjongNetClient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    String addr = "210.99.167.73";
    int port = 9502;

    public static String winds[] = {"동", "남", "서", "북"};
    public static String IDs[] = {"","","",""};

    int connection;
    boolean isConnecting = false;
    public static String ID;

    Button b;
    EditText t;
    TextView status;
    NumberPicker nb;
    Handler mHandler;

    public WaitThread wait;
    SendThread send;

    static Socket socket;
    public static BufferedReader in;
    public static PrintWriter out;
    public int alertCode = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        b = findViewById(R.id.cnt);
        t = findViewById(R.id.IDinput);
        nb = findViewById(R.id.positionPicker);
        status = findViewById(R.id.statusText);

        try {
            FileInputStream fis = openFileInput("id.txt");
            byte[] id = new byte[fis.available()];
            while (fis.read(id) != -1){;}
            fis.close();
            t.setText(new String(id));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler = new Handler();

        nb.setMinValue(0);
        nb.setMaxValue(3);
        nb.setDisplayedValues(winds);


        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(b.getText().equals("연결")) {
                    ID = t.getText().toString();
                    try {
                        FileOutputStream fos = openFileOutput("id.txt", Context.MODE_PRIVATE);
                        fos.write(ID.getBytes());
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    DisconnectSocket disThread = new DisconnectSocket();
                    disThread.start(); // clean first
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConnectSocket cntThread = new ConnectSocket();
                            cntThread.start();
                        }
                    }, 300);
                    mHandler.postDelayed(new Runnable(){
                        public void run(){
                            wait = new WaitThread();
                            send = new SendThread();
                            wait.start();
                            send.start();
                        }
                    }, 500);
                    t.setEnabled(false);
                    t.setFocusable(false);
                    mHandler.post(changeButton);
                }
                else if(b.getText().equals("취소")){
                    DisconnectSocket disThread = new DisconnectSocket();
                    disThread.start();
                    t.setEnabled(true);
                    t.setFocusable(true);
                    mHandler.post(changeButton);
                }
            }
        });

    }



    public class ConnectSocket extends Thread{
        @Override
        public void run() {
            try {
                socket = new Socket(addr, port);
                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                isConnecting = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class DisconnectSocket extends Thread{
        @Override
        public void run() {
            try {
                out.close();
                in.close();
                socket.close();
                isConnecting = false;
                mHandler.post(changeText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public class WaitThread extends Thread{
        @Override
        public void run(){
            try{
                String msg;
                String[] split;

                while(true){
                    msg = in.readLine();
                    Log.d("Main", msg);
                    split = msg.split(">");

                    if(split[0].equals("Server")){
                        if(split[1].equals("kick")){
                            break;
                        }
                        else if(split[1].equals("Refused")){
                            alertCode = 0;
                            mHandler.post(serverAlert);
                            break;
                        }
                        else if(split[1].equals("Taken")){
                            alertCode = 1;
                            mHandler.post(serverAlert);
                            break;
                        }
                        else{
                            IDs = split[1].split("#%");
                            Log.d("0",IDs[0]);
                            Log.d("1",IDs[1]);
                            Log.d("2",IDs[2]);
                            Log.d("3",IDs[3]);
                        }
                    }
                    else if(msg.equals("0") ||msg.equals("1") || msg.equals("2") || msg.equals("3") || msg.equals("4")){
                        connection = Integer.parseInt(msg);
                        isConnecting = true;
                        mHandler.post(changeText);
                        if(msg.equals("4")){
                            break;
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public class SendThread extends Thread{
        @Override
        public void run(){
            try{
                String msg;
                out.println("pos195727" + nb.getValue());
                out.println("ID195727" + ID);
                out.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    public Runnable changeButton = new Runnable(){
      public void run(){
          if(b.getText().equals("연결")) {
              b.setText("취소");
          }
          else if(b.getText().equals("취소")){
              b.setText("연결");
          }
      }
    };

    public Runnable changeText = new Runnable(){
        public void run(){
            if(isConnecting == false){
                status.setText("Not connecting");
            }
            else if(connection != 0) {
                status.setText("Waiting connection...(" + connection + "/4)");
                if(connection == 4){
                        b.setText("연결");

                        Intent intent = new Intent(MainActivity.this, Board.class);
                        intent.putExtra("myWind", nb.getValue());
                        startActivity(intent);
                }
            }

        }
    };

    public Runnable serverAlert = new Runnable(){
        public void run(){
            AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
            switch(alertCode) {
                case 0:
                    bld.setTitle("Connection refused");
                    bld.setMessage("Server is full.");
                    break;
                case 1:
                    bld.setTitle("Connection refused");
                    bld.setMessage(winds[nb.getValue()] + " is already taken.");
            }
            b.setText("연결");
            bld.show();
        }
    };

    public static synchronized Socket getSocket(){
        return socket;
    }
    public static synchronized BufferedReader getBufferedReader(){
        return in;
    }
    public static synchronized PrintWriter getPrintWriter(){
        return out;
    }
}
