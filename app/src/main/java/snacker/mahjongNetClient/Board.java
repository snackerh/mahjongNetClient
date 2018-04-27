package snacker.mahjongNetClient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

import static snacker.mahjongNetClient.MainActivity.IDs;

public class Board extends AppCompatActivity {
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    Button riichi;
    Button tsumo;
    Button ron;

    TextView myWind; // 자신
    TextView myLight; // 자기 리치상태
    TextView myScore; // 자기 점수
    TextView myChange; // 자기 변동
    TextView lowerWind; // 하가
    TextView lowerLight; // 하가 리치상태
    TextView lowerScore; // 하가 점수
    TextView lowerChange; // 하가 변동
    TextView faceWind; // 대가
    TextView faceLight; // 대가 리치상태
    TextView faceScore; // 대가 점수
    TextView faceChange; // 대가 변동
    TextView upperWind; // 상가
    TextView upperLight; // 상가 리치상태
    TextView upperScore; // 상가 점수
    TextView upperChange; // 상가 변동
    TextView statusBoard; // 중앙상황판
    ImageView compare; // 좌측상단점점점

    Handler mHandler;
    RecieveThread wait;
    Calculate calc = new Calculate();

    int boardToggle = 0;

    int round = 1;
    int extend = 0;
    boolean oyaextend = false; //연장본장 구분
    int[] scores = {30000, 30000, 30000, 30000};
    int[] change = {0, 0, 0, 0};
    boolean[] isRiichi = {false, false, false, false};
    boolean ronPressed = false;
    int vault = 0;
    int intentWind;

    //유국 전용
    boolean waitingDraw = false;
    int waitdraws = 0;
    int tenpais = 0;

    int winnerWind;
    int loserWind;
    int pan;
    int boo;

    int jackpot; // 자 계산
    int oyajackpot; // 친 계산

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHandler = new Handler();

            ConnectSocket connect = new ConnectSocket();
            connect.start();

            mHandler.postDelayed(new Runnable() {
                public void run() {
                    wait = new RecieveThread();
                    wait.start();
                }
            }, 1000);

            myScore = findViewById(R.id.myScore);
            lowerScore = findViewById(R.id.lowerScore);
            faceScore = findViewById(R.id.faceScore);
            upperScore = findViewById(R.id.upperScore);
            myWind = findViewById(R.id.myWind);
            lowerWind = findViewById(R.id.lowerWind);
            faceWind = findViewById(R.id.faceWind);
            upperWind = findViewById(R.id.upperWind);
            myLight = findViewById(R.id.myLight);
            lowerLight = findViewById(R.id.lowerLight);
            faceLight = findViewById(R.id.faceLight);
            upperLight = findViewById(R.id.upperLight);
            statusBoard = findViewById(R.id.middle);
            compare = findViewById(R.id.compare);
            myChange = findViewById(R.id.meCompare);
            lowerChange = findViewById(R.id.lowerCompare);
            faceChange = findViewById(R.id.faceCompare);
            upperChange = findViewById(R.id.upperCompare);

            riichi = findViewById(R.id.riichi);
            tsumo = findViewById(R.id.tsumo);
            ron = findViewById(R.id.ron);
            riichi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (riichi.isEnabled()) {
                        if(scores[intentWind] < 1000) Toast.makeText(Board.this, "리치 불가",Toast.LENGTH_SHORT).show();
                        else {
                            scores[intentWind] -= 1000;
                            change[intentWind] = -1000;
                            vault += 1000;
                            isRiichi[(intentWind)] = true;

                            riichi.setEnabled(false);
                            if (intentWind == 0) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        SendStatus send = new SendStatus(0);
                                        send.start();
                                    }
                                });
                            }
                            SendAgain send = new SendAgain();
                            send.start();
                            //updateBoardFunc();
                        }
                    }
                }
            });
            tsumo.setOnClickListener(mClick);
            ron.setOnClickListener(mClick);

            lowerScore.setOnClickListener(mClick);
            faceScore.setOnClickListener(mClick);
            upperScore.setOnClickListener(mClick);
            lowerWind.setOnClickListener(mClick);
            faceWind.setOnClickListener(mClick);
            upperWind.setOnClickListener(mClick);

            //compare.setOnTouchListener(mTouch);
            compare.setOnClickListener(imageClick);
            compare.setClickable(true);

            Intent intent = getIntent();
            intentWind = intent.getIntExtra("myWind", 0);

            if(intentWind == 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SendStatus send = new SendStatus(1);
                        send.start();
                    }
                });
            }

            updateBoardFunc();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.draw:
                if(!waitingDraw) {
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("확인");
                    bld.setMessage("유국을 선택하셨습니다.\n당신은 텐파이입니까?");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            isRiichi[intentWind] = true;
                            waitingDraw = true;
                            waitdraws++;
                            tenpais++;
                            SendDraw send = new SendDraw();
                            send.start();
                        }
                    });
                    bld.setPositiveButton("아니오", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            isRiichi[intentWind] = false;
                            waitingDraw = true;
                            waitdraws++;
                            SendDraw send = new SendDraw();
                            send.start();
                        }
                    });
                    bld.setNeutralButton("취소", null);
                    bld.show();
                }
                else{
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("안내");
                    bld.setMessage("현재 상대편으로부터 텐파이 여부를 입력받고 있습니다.\n잠시만 기다려 주십시오.");
                    bld.show();
                }
                break;
            case R.id.chonbo:
                if(!waitingDraw){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("확인");
                    bld.setMessage("정말 촌보를 저지르셨습니까?");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            chonboFunc(0);
                            SendChonbo send = new SendChonbo();
                            send.start();
                            updateBoardFunc();
                        }
                    });
                    bld.setPositiveButton("아니오", null);
                    bld.show();
                } else{
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("안내");
                    bld.setMessage("현재 상대편으로부터 텐파이 여부를 입력받고 있습니다.\n잠시만 기다려 주십시오.");
                    bld.show();
                }
                break;
            case R.id.revertOnce:
                if(!waitingDraw && intentWind == 0){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("확인");
                    bld.setMessage("정말 이전 상태로 되돌아가시겠습니까?");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            SendStatus send = new SendStatus(3);
                            send.start();
                            updateBoardFunc();
                        }
                    });
                    bld.setPositiveButton("아니오", null);
                    bld.show();
                } else if(waitingDraw){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("안내");
                    bld.setMessage("현재 상대편으로부터 텐파이 여부를 입력받고 있습니다.\n잠시만 기다려 주십시오.");
                    bld.show();
                } else{
                    Toast.makeText(Board.this, "해당 기능은 친에게 문의하십시오.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.revertMiddle:
                if(!waitingDraw && intentWind == 0){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("확인");
                    bld.setMessage("정말 이번 국의 처음으로 되돌아가시겠습니까?");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            SendStatus send = new SendStatus(4);
                            send.start();
                            updateBoardFunc();
                        }
                    });
                    bld.setPositiveButton("아니오", null);
                    bld.show();
                } else if(waitingDraw){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("안내");
                    bld.setMessage("현재 상대편으로부터 텐파이 여부를 입력받고 있습니다.\n잠시만 기다려 주십시오.");
                    bld.show();
                } else{
                    Toast.makeText(Board.this, "해당 기능은 친에게 문의하십시오.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.revertFar:
                if(!waitingDraw && intentWind == 0){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("확인");
                    bld.setMessage("정말 이전 국의 처음으로 되돌아가시겠습니까?");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            SendStatus send = new SendStatus(2);
                            send.start();
                            updateBoardFunc();
                        }
                    });
                    bld.setPositiveButton("아니오", null);
                    bld.show();
                } else if(waitingDraw){
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("안내");
                    bld.setMessage("현재 상대편으로부터 텐파이 여부를 입력받고 있습니다.\n잠시만 기다려 주십시오.");
                    bld.show();
                } else{
                    Toast.makeText(Board.this, "해당 기능은 친에게 문의하십시오.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.recovery:
                    AlertDialog.Builder bld = new AlertDialog.Builder(this);
                    bld.setTitle("확인");
                    bld.setMessage("현재 상태로 보드를 갱신하겠습니까?");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            SendRecovery send = new SendRecovery();
                            send.start();
                            updateBoardFunc();
                        }
                    });
                    bld.setPositiveButton("아니오", null);
                    bld.show();
                break;
        }
        return false;
    }



    /* 보드 갱신 */

    public Runnable updateChange = new Runnable() { // 모든 작업이 끝난후 업데이트할것
        @Override
        public void run() {
            changeUpdate();
        }
    };

    public Runnable updateBoard = new Runnable() { // 모든 작업이 끝난후 업데이트할것
        @Override
        public void run() {
            lowerScore.setTextSize(36);
            faceScore.setTextSize(36);
            upperScore.setTextSize(36);
            myScore.setTextSize(36);
            updateBoardFunc();
        }
    };


    public void changeUpdate(){
        myChange.setTextColor(Color.WHITE);
        lowerChange.setTextColor(Color.WHITE);
        faceChange.setTextColor(Color.WHITE);
        upperChange.setTextColor(Color.WHITE);
        myChange.setText(MainActivity.IDs[intentWind]);
        lowerChange.setText(MainActivity.IDs[(intentWind + 1) % 4]);
        faceChange.setText(MainActivity.IDs[(intentWind + 2) % 4]);
        upperChange.setText(MainActivity.IDs[(intentWind + 3) % 4]);
        Arrays.fill(change, 0);
    }

    public void updateBoardFunc(){
        Log.d("upd", ""+ scores[intentWind]);

        switch(boardToggle){
            case 0:
                lowerScore.setTextSize(36);
                faceScore.setTextSize(36);
                upperScore.setTextSize(36);
                myScore.setTextSize(36);
                myScore.setText("" + scores[intentWind]); //점수상태
                lowerScore.setText("" + scores[(intentWind + 1) % 4]);
                faceScore.setText("" + scores[(intentWind + 2) % 4]);
                upperScore.setText("" + scores[(intentWind + 3) % 4]);
                break;
            case 1:
                lowerScore.setText("" + (scores[intentWind] - scores[(intentWind + 1) % 4]));
                if((scores[intentWind] - scores[(intentWind + 1) % 4]) < 0) lowerScore.setTextColor(Color.RED);
                else if((scores[intentWind] - scores[(intentWind + 1) % 4]) > 0) lowerScore.setTextColor(Color.GREEN);
                else { lowerScore.setTextColor(Color.WHITE); lowerScore.setText("±0");}
                faceScore.setText(""+ (scores[intentWind] - scores[(intentWind + 2) % 4]));
                if((scores[intentWind] - scores[(intentWind + 2) % 4]) < 0) faceScore.setTextColor(Color.RED);
                else if((scores[intentWind] - scores[(intentWind + 2) % 4]) > 0) faceScore.setTextColor(Color.GREEN);
                else { faceScore.setTextColor(Color.WHITE); faceScore.setText("±0");}
                upperScore.setText(""+ (scores[intentWind] - scores[(intentWind + 3) % 4]));
                if((scores[intentWind] - scores[(intentWind + 3) % 4]) < 0) upperScore.setTextColor(Color.RED);
                else if((scores[intentWind] - scores[(intentWind + 3) % 4]) > 0) upperScore.setTextColor(Color.GREEN);
                else { upperScore.setTextColor(Color.WHITE);  upperScore.setText("±0");}
                break;
            case 2:
                lowerScore.setTextColor(Color.WHITE);
                faceScore.setTextColor(Color.WHITE);
                upperScore.setTextColor(Color.WHITE);
                lowerScore.setTextSize(18);
                faceScore.setTextSize(18);
                upperScore.setTextSize(18);
                myScore.setTextSize(18);
                lowerScore.setText(calc.findLeastRon(scores, intentWind, (intentWind + 1) % 4, round, extend, vault));
                faceScore.setText(calc.findLeastRon(scores, intentWind, (intentWind + 2) % 4, round, extend, vault));
                upperScore.setText(calc.findLeastRon(scores, intentWind, (intentWind + 3) % 4, round, extend, vault));
                myScore.setText(calc.findLeastTsumo(scores, intentWind, round, extend, vault));
        }
        myWind.setText(MainActivity.winds[(intentWind + 9 - round) % 4]); //각자의 바람
        lowerWind.setText(MainActivity.winds[(intentWind + 10 - round) % 4]);
        faceWind.setText(MainActivity.winds[(intentWind + 11 - round) % 4]);
        upperWind.setText(MainActivity.winds[(intentWind + 12 - round) % 4]);

        statusBoard.setText((round <= 4 ? "동 " : "남 ") + ((round % 4) == 0 ? 4 : (round % 4)) + "국\n" + extend + (oyaextend ? "연장" : "본장") + "\n공탁:" + vault); // 중앙판
        if(oyaextend) statusBoard.setBackgroundResource(R.color.colorAccent);
        else statusBoard.setBackgroundResource(R.color.lightBackground);

        if(change[intentWind] > 0){
            myChange.setTextColor(Color.GREEN);
            myChange.setText("+" + change[intentWind]);
        } else if(change[intentWind] < 0){
            myChange.setTextColor(Color.RED);
            myChange.setText("-" + change[intentWind]);
        }
        if(change[(intentWind + 1) % 4] > 0){
            lowerChange.setTextColor(Color.GREEN);
            lowerChange.setText("+" + change[(intentWind + 1) % 4]);
        } else if(change[(intentWind + 1) % 4] < 0){
            lowerChange.setTextColor(Color.RED);
            lowerChange.setText("-" + change[(intentWind + 1) % 4]);
        }
        if(change[(intentWind + 2) % 4] > 0){
            faceChange.setTextColor(Color.GREEN);
            faceChange.setText("+" + change[(intentWind + 2) % 4]);
        } else if(change[(intentWind + 2) % 4] < 0){
            faceChange.setTextColor(Color.RED);
            faceChange.setText("-" + change[(intentWind + 2) % 4]);
        }
        if(change[(intentWind + 3) % 4] > 0){
            upperChange.setTextColor(Color.GREEN);
            upperChange.setText("+" + change[(intentWind + 3) % 4]);
        } else if(change[(intentWind + 3) % 4] < 0){
            upperChange.setTextColor(Color.RED);
            upperChange.setText("-" + change[(intentWind + 3) % 4]);
        }
        mHandler.postDelayed(updateChange, 2000);

        switch((intentWind + 9 - round) % 4){ //오야가 누구야
            case 0:
                myWind.setBackgroundColor(Color.RED);
                upperWind.setBackgroundColor(Color.TRANSPARENT);
                faceWind.setBackgroundColor(Color.TRANSPARENT);
                lowerWind.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 1:
                myWind.setBackgroundColor(Color.TRANSPARENT);
                upperWind.setBackgroundColor(Color.RED);
                faceWind.setBackgroundColor(Color.TRANSPARENT);
                lowerWind.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 2:

                myWind.setBackgroundColor(Color.TRANSPARENT);
                upperWind.setBackgroundColor(Color.TRANSPARENT);
                faceWind.setBackgroundColor(Color.RED);
                lowerWind.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 3:
                myWind.setBackgroundColor(Color.TRANSPARENT);
                upperWind.setBackgroundColor(Color.TRANSPARENT);
                faceWind.setBackgroundColor(Color.TRANSPARENT);
                lowerWind.setBackgroundColor(Color.RED);
                break;
        }

        if(waitingDraw){
            riichi.setEnabled(false);
            tsumo.setEnabled(false);
            ron.setEnabled(false);
        }
        else {
            if (isRiichi[(intentWind)]) myLight.setBackgroundResource(R.drawable.riichih); //각자의 리치봉
            else myLight.setBackgroundColor(Color.TRANSPARENT);
            if (isRiichi[(intentWind + 1) % 4])
                lowerLight.setBackgroundResource(R.drawable.riichiv);
            else lowerLight.setBackgroundColor(Color.TRANSPARENT);
            if (isRiichi[(intentWind + 2) % 4]) faceLight.setBackgroundResource(R.drawable.riichih);
            else faceLight.setBackgroundColor(Color.TRANSPARENT);
            if (isRiichi[(intentWind + 3) % 4])
                upperLight.setBackgroundResource(R.drawable.riichiv);
            else upperLight.setBackgroundColor(Color.TRANSPARENT);


            if (round == 9 || (round == 8 && isTop(scores)) || isTobi(scores)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            out.flush();
                            socket.close();
                            in.close();
                            out.close();
                            riichi.setEnabled(false);
                            tsumo.setEnabled(false);
                            ron.setEnabled(false);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                finalResult();
                return;
            }


            if (ronPressed) {
                ron.setBackgroundColor(Color.RED);
                ron.setText("취소");
                tsumo.setEnabled(false);
                riichi.setEnabled(false);
                statusBoard.setText("방총당한\n 사람은?");
            } else {
                ron.setBackgroundResource(android.R.drawable.btn_default);
                ron.setText("론");
                ron.setEnabled(true);
                tsumo.setEnabled(true);
                if (!isRiichi[(intentWind)]) riichi.setEnabled(true);
            }
        }

    }


    public int findID(String[] arr, String id){
        for(int i = 0; i < 4; i++){
            if(arr[i].equals(id)) return i;
        }
        return -1;
    }

    public boolean isTobi(int[] arr){
        for(int i = 0; i < 4; i++){
            if(arr[i] < 0) return true;
        }
        return false;
    }

    public boolean isTop(int[] arr){
        for(int i = 0; i < 3; i++){
            if(arr[i] > arr[3]) return false;
        }
        return true;
    }

    public void recover(String[] st){
        round = Integer.parseInt(st[0]);
        extend = Integer.parseInt(st[1]);
        oyaextend = Boolean.parseBoolean(st[2]);
        scores[0] = Integer.parseInt(st[3]);
        scores[1] = Integer.parseInt(st[4]);
        scores[2] = Integer.parseInt(st[5]);
        scores[3] = Integer.parseInt(st[6]);
        isRiichi[0] = Boolean.parseBoolean(st[7]);
        isRiichi[1] = Boolean.parseBoolean(st[8]);
        isRiichi[2] = Boolean.parseBoolean(st[9]);
        isRiichi[3] = Boolean.parseBoolean(st[10]);
        vault = Integer.parseInt(st[11]);
        for(int i = 0; i < 12; i++) {
            Log.d("st", st[i]);
        }
    }

    public boolean stillOyaExtend(){
        if(extend == 1 || (extend  >= 2 && oyaextend == true)) return true;
        else return false;
    }

    /* 쓰모, 론, 유국 시 계산 */

    public void tsumoFunc(int winner){ //winner = 자신 기준 상대하가
        if(((winner + intentWind + 13 - round) % 4) == 0){ // 친이 쯔모
            oyajackpot = calc.oyaTsumo(pan, boo);
            scores[((winner + intentWind) % 4)] += ((3 * oyajackpot) + (300 * extend) + vault);
            change[((winner + intentWind) % 4)] = ((3 * oyajackpot) + (300 * extend) + vault);
            scores[((winner + intentWind + 1) % 4)] -= oyajackpot + (100 * extend);
            change[((winner + intentWind + 1) % 4)] = -(oyajackpot + (100 * extend));
            scores[((winner + intentWind + 2) % 4)] -= oyajackpot + (100 * extend);
            change[((winner + intentWind + 2) % 4)] = -(oyajackpot + (100 * extend));
            scores[((winner + intentWind + 3) % 4)] -= oyajackpot + (100 * extend);
            change[((winner + intentWind + 2) % 4)] = -(oyajackpot + (100 * extend));
            extend++;
            oyaextend = stillOyaExtend();
            Arrays.fill(isRiichi,false);
            vault = 0;
            oyajackpot = 0;
        }
        else{
            oyajackpot = calc.oyaTsumo(pan, boo);
            jackpot = calc.childTsumo(pan, boo);
            Log.d("pan", "" + pan);
            Log.d("boo", "" + boo);
            Log.d("oyajackpot", ""+ oyajackpot);
            Log.d("jackpot", ""+ jackpot);
            if(((winner + intentWind + 13 - round) % 4) == 1){ // 남가 쯔모
                scores[((winner + intentWind) % 4)] += (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                change[((winner + intentWind) % 4)] = (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                scores[((winner + intentWind + 1) % 4)] -= jackpot + (100 * extend); //서가
                change[((winner + intentWind + 1) % 4)] = -(jackpot + (100 * extend));
                scores[((winner + intentWind + 2) % 4)] -= jackpot + (100 * extend); //북가
                change[((winner + intentWind + 2) % 4)] = -(jackpot + (100 * extend));
                scores[((winner + intentWind + 3) % 4)] -= oyajackpot + (100 * extend); //오야
                change[((winner + intentWind + 3) % 4)] = -(oyajackpot + (100 * extend));
            }
            if(((winner + intentWind + 13 - round) % 4) == 2){ // 서가 쯔모
                scores[((winner + intentWind) % 4)] += (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                change[((winner + intentWind) % 4)] = (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                scores[((winner + intentWind + 1) % 4)] -= jackpot + (100 * extend); //북가
                change[((winner + intentWind + 1) % 4)] = -(jackpot + (100 * extend));
                scores[((winner + intentWind + 2) % 4)] -= oyajackpot + (100 * extend); //오야
                change[((winner + intentWind + 2) % 4)] = -(oyajackpot + (100 * extend));
                scores[((winner + intentWind + 3) % 4)] -= jackpot + (100 * extend); //남가
                change[((winner + intentWind + 3) % 4)] = -(jackpot + (100 * extend));
            }
            if(((winner + intentWind + 13 - round) % 4) == 3){ // 북가 쯔모
                scores[((winner + intentWind) % 4)] += (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                change[((winner + intentWind) % 4)] = (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                scores[((winner + intentWind + 1) % 4)] -= oyajackpot + (100 * extend); //오야
                change[((winner + intentWind + 1) % 4)] = -(oyajackpot + (100 * extend));
                scores[((winner + intentWind + 2) % 4)] -= jackpot + (100 * extend); //남가
                change[((winner + intentWind + 2) % 4)] = -(jackpot + (100 * extend));
                scores[((winner + intentWind + 3) % 4)] -= jackpot + (100 * extend); //서가
                change[((winner + intentWind + 3) % 4)] = -(jackpot + (100 * extend));
            }
            round++;
            extend = 0;
            oyaextend = false;
            vault = 0;
            Arrays.fill(isRiichi,false);
            oyajackpot = 0;
            jackpot = 0;
        }
        if(intentWind == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SendStatus send = new SendStatus(1);
                    send.start();
                }
            });
        }
    }

    public void ronFunc(int winner, int loser){
        if(((winner + intentWind + 13 - round) % 4) == 0){ // 친이 론
            oyajackpot = calc.oyaRon(pan, boo);
            scores[(winner + intentWind) % 4] += (oyajackpot + (300 * extend) + vault);
            change[(winner + intentWind) % 4] = (oyajackpot + (300 * extend) + vault);
            scores[(loser + intentWind) % 4] -= oyajackpot + (300 * extend);
            change[(loser + intentWind) % 4] = -(oyajackpot + (300 * extend));
            extend++;
            oyaextend = stillOyaExtend();
            vault = 0;
            oyajackpot = 0;
            Arrays.fill(isRiichi,false);
        }
        else{
            jackpot = calc.childRon(pan, boo);
            scores[(winner + intentWind) % 4] += (jackpot + (300 * extend) + vault);
            change[(winner + intentWind) % 4] = (jackpot + (300 * extend) + vault);
            scores[(loser + intentWind) % 4] -= jackpot + (300 * extend);
            change[(loser + intentWind) % 4] = -(jackpot + (300 * extend));
            round++;
            extend = 0;
            oyaextend = false;
            vault = 0;
            jackpot = 0;
            Arrays.fill(isRiichi,false);
        }
        if(intentWind == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SendStatus send = new SendStatus(1);
                    send.start();
                }
            });
        }
    }

    public void ryugukFunc(){
        extend++;

        switch(tenpais){
            case 0:
                round++;
                break;
            case 1:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) {
                        scores[i] += 3000;
                        change[i] = 3000;
                    }
                    else {
                        scores[i] -= 1000;
                        change[i] = -1000;
                    }
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 2:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) {
                        scores[i] += 1500;
                        change[i] = 1500;
                    }
                    else {
                        scores[i] -= 1500;
                        change[i] = -1500;
                    }
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 3:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) {
                        scores[i] += 1000;
                        change[i] = 1000;
                    }
                    else {
                        scores[i] -= 3000;
                        change[i] = -3000;
                    }
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 4:
                break;
        }

        Arrays.fill(isRiichi, false);
        oyaextend = false;
        waitdraws = 0;
        tenpais = 0;
        waitingDraw = false;

        if(intentWind == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SendStatus send = new SendStatus(1);
                    send.start();
                }
            });
        }
    }

    public void chonboFunc(int player){ // 자신 기준 위치
        if((player + intentWind + 9 - round) % 4 == 0){
            scores[(player + intentWind) % 4] -= 12000;
            change[(player + intentWind) % 4] = -12000;
            for(int i = 1; i < 4; i++) {
                scores[(player + intentWind + i) % 4] += 4000;
                change[(player + intentWind + i) % 4] = 4000;
            }
        }
        else{
            scores[(player + intentWind) % 4] -= 8000;
            change[(player + intentWind) % 4] = -8000;
            for(int i = 1; i < 4; i++) {
                if((player + intentWind + i + 9 - round) % 4 == 0) {
                    scores[(player + intentWind + i) % 4] += 4000;
                    change[(player + intentWind + i) % 4] = 4000;
                }
                else {
                    scores[(player + intentWind + i) % 4] += 2000;
                    change[(player + intentWind + i) % 4] = 2000;
                }
            }
        }

        for(int i = 0; i < 4; i++){
            if(isRiichi[i]) {
                scores[i] += 1000;
                change[i] += 1000;
                vault -= 1000;
            }
        }
        Arrays.fill(isRiichi, false);

        if(intentWind == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SendStatus send = new SendStatus(1);
                    send.start();
                }
            });
        }
    }



    @Override
    public void onBackPressed(){
        if(!socket.isClosed()){ Toast.makeText(Board.this, "대국중에는 나가면 안 돼", Toast.LENGTH_SHORT).show(); }
        else{
            super.onBackPressed();
        }
    }



    /* 리스너 관련 메서드들 */

    View.OnClickListener imageClick = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            boardToggle = (boardToggle+1) % 3;
            updateBoardFunc();
        }
    };

    View.OnTouchListener mTouch = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent action){
            switch(action.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lowerScore.setText("" + (scores[intentWind] - scores[(intentWind + 1) % 4]));
                    if((scores[intentWind] - scores[(intentWind + 1) % 4]) < 0) lowerScore.setTextColor(Color.RED);
                    else if((scores[intentWind] - scores[(intentWind + 1) % 4]) > 0) lowerScore.setTextColor(Color.GREEN);
                    faceScore.setText(""+ (scores[intentWind] - scores[(intentWind + 2) % 4]));
                    if((scores[intentWind] - scores[(intentWind + 2) % 4]) < 0) faceScore.setTextColor(Color.RED);
                    else if((scores[intentWind] - scores[(intentWind + 2) % 4]) > 0) faceScore.setTextColor(Color.GREEN);
                    upperScore.setText(""+ (scores[intentWind] - scores[(intentWind + 3) % 4]));
                    if((scores[intentWind] - scores[(intentWind + 3) % 4]) < 0) upperScore.setTextColor(Color.RED);
                    else if((scores[intentWind] - scores[(intentWind + 3) % 4]) > 0) upperScore.setTextColor(Color.GREEN);
                    break;
                case MotionEvent.ACTION_UP:
                    lowerScore.setTextColor(Color.WHITE);
                    faceScore.setTextColor(Color.WHITE);
                    upperScore.setTextColor(Color.WHITE);
                    lowerScore.setTextSize(18);
                    faceScore.setTextSize(18);
                    upperScore.setTextSize(18);
                    myScore.setTextSize(18);
                    lowerScore.setText(calc.findLeastRon(scores, intentWind, (intentWind + 1) % 4, round, extend, vault));
                    faceScore.setText(calc.findLeastRon(scores, intentWind, (intentWind + 2) % 4, round, extend, vault));
                    upperScore.setText(calc.findLeastRon(scores, intentWind, (intentWind + 3) % 4, round, extend, vault));
                    myScore.setText(calc.findLeastTsumo(scores, intentWind, round, extend, vault));
                    mHandler.postDelayed(updateBoard, 2000);
                    break;
            }
            return false;
        }
    };

    Button.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch(view.getId()){
                case R.id.tsumo:
                    winnerWind = 0;
                    loserWind = 0;
                    showDialog();
                    break;
                case R.id.ron:
                    if(!ronPressed) {
                        winnerWind = 0;
                        ronPressed = true;
                        updateBoardFunc();
                    }
                    else{
                        ronPressed = false;
                        updateBoardFunc();
                    }
                    break;
                case R.id.lowerScore: case R.id.lowerWind:
                    if(ronPressed){
                        loserWind = 1;
                        showDialog();
                        break;
                    }
                case R.id.faceScore: case R.id.faceWind:
                    if(ronPressed) {
                        loserWind = 2;
                        showDialog();
                        break;
                    }
                case R.id.upperScore: case R.id.upperWind:
                    if(ronPressed){
                        loserWind = 3;
                        showDialog();
                        break;
                    }
            }
        }
    };




    public void finalResult(){
        AlertDialog.Builder bld = new AlertDialog.Builder(Board.this);
        bld.setTitle("대국종료");
        bld.setMessage("동(" + IDs[0] + "): " + (scores[0] - 30000) + "\n"
                + "남(" + IDs[1] + "): " + (scores[1] - 30000) + "\n"
                +"서(" + IDs[2] + "): " + (scores[2] - 30000) + "\n"
                + "북(" + IDs[3] + "): " + (scores[3] - 30000));
        bld.show();
    }

    public void showDialog(){
        final String[] panarr = {"1판","2판","3판","4판","만관","하네만","배만","삼배만","역만","더블역만","트리플역만","쿼드러플역만"};
        final String[] booarr = {"20부","25부","30부","40부","50부","60부","70부","80부"};
        LayoutInflater dialog = LayoutInflater.from(this);
        final View dialoglayout = dialog.inflate(R.layout.activity_score_dialog, null);
        final Dialog call = new Dialog(this);

        call.setContentView(dialoglayout);
        final NumberPicker panPicker = (NumberPicker) call.findViewById(R.id.panPicker);
        final NumberPicker booPicker = (NumberPicker) call.findViewById(R.id.booPicker);
        Button ok = call.findViewById(R.id.dialogOk);
        Button cancel = call.findViewById(R.id.dialogCancel);

        panPicker.setMinValue(0);
        panPicker.setMaxValue(panarr.length - 1);
        panPicker.setDisplayedValues(panarr);
        booPicker.setMinValue(0);
        booPicker.setMaxValue(booarr.length - 1);
        booPicker.setDisplayedValues(booarr);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pan = panPicker.getValue();
                boo = booPicker.getValue();

                if(winnerWind == loserWind){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            SendTsumo s = new SendTsumo();
                            s.start();
                        }
                    });
                    tsumoFunc(winnerWind);

                }
                else{
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            SendRon s = new SendRon();
                            s.start();
                        }
                    });
                    ronFunc(winnerWind, loserWind);
                }

                call.dismiss();
                ronPressed = false;
                updateBoardFunc();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                call.cancel();
            }
        });
        call.show();
    }





    /* 여기서부터 통신관련 클래스, 메서드 */

    public class RecieveThread extends Thread {
        @Override
        public void run(){
            super.run();
            try{
                String msg;
                String[] split;
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(true){
                    msg = in.readLine();
                    Log.d("msg", msg);
                    Arrays.fill(change,0);
                    split = msg.split(">");
                    if(split[0].equals("Server")){
                        if(split[1].equals("prev") || split[1].equals("middle") || split[1].equals("near") || split[1].equals("curr")){ //revert or recover
                            String[] recoverStatus = split[2].split("::");
                            recover(recoverStatus);
                        }
                        Log.d("wgat","what");
                        mHandler.post(updateBoard);
                        continue;
                    }
                    int who = findID(IDs, split[0]);
                    switch((who - intentWind + 4) % 4){
                        case 0:
                            Log.d("match", MainActivity.ID);
                            mHandler.post(updateBoard);
                            continue;
                        case 1: // 하가
                            if(split[1].equals("riichi")){
                                scores[(intentWind + 1) % 4] -= 1000;
                                change[(intentWind + 1) % 4] = -1000;
                                isRiichi[(intentWind + 1) % 4] = true;
                                vault += 1000;
                                if(intentWind == 0) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            SendStatus send = new SendStatus(0);
                                            send.start();
                                        }
                                    });
                                }
                            }
                            else if(split[1].equals("tsumo")){
                                pan = Integer.parseInt(split[2]);
                                boo = Integer.parseInt(split[3]);
                                tsumoFunc(1);
                            }
                            else if(split[1].equals("ron")){
                                pan = Integer.parseInt(split[2]);
                                boo = Integer.parseInt(split[3]);
                                loserWind = Integer.parseInt(split[4]);
                                ronFunc(1,(loserWind + 1) % 4);
                            }
                            else if(split[1].equals("Tenpai")){
                                isRiichi[(intentWind + 1) % 4] = true;
                                waitdraws++;
                                tenpais++;
                            }
                            else if(split[1].equals("noTenpai")){
                                isRiichi[(intentWind + 1) % 4] = false;
                                waitdraws++;
                            }
                            else if(split[1].equals("chonbo")){
                                chonboFunc(1);
                            }
                            break;
                        case 2: // 대가
                            if(split[1].equals("riichi")){
                                scores[(intentWind + 2) % 4] -= 1000;
                                change[(intentWind + 2) % 4] = -1000;
                                isRiichi[(intentWind + 2) % 4] = true;
                                vault += 1000;
                                if(intentWind == 0) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            SendStatus send = new SendStatus(0);
                                            send.start();
                                        }
                                    });
                                }
                            }
                            else if(split[1].equals("tsumo")){
                                pan = Integer.parseInt(split[2]);
                                boo = Integer.parseInt(split[3]);
                                tsumoFunc(2);
                            }
                            else if(split[1].equals("ron")){
                                pan = Integer.parseInt(split[2]);
                                boo = Integer.parseInt(split[3]);
                                loserWind = Integer.parseInt(split[4]);
                                ronFunc(2,(loserWind + 2) % 4);
                            }
                            else if(split[1].equals("Tenpai")){
                                isRiichi[(intentWind + 2) % 4] = true;
                                waitdraws++;
                                tenpais++;
                            }
                            else if(split[1].equals("noTenpai")){
                                isRiichi[(intentWind + 2) % 4] = false;
                                waitdraws++;
                            }
                            else if(split[1].equals("chonbo")){
                                chonboFunc(2);
                            }
                            break;
                        case 3: // 상가
                            if(split[1].equals("riichi")){
                                scores[(intentWind + 3) % 4] -= 1000;
                                change[(intentWind + 3) % 4] = -1000;
                                isRiichi[(intentWind + 3) % 4] = true;
                                vault += 1000;
                                if(intentWind == 0) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            SendStatus send = new SendStatus(0);
                                            send.start();
                                        }
                                    });
                                }
                            }
                            else if(split[1].equals("tsumo")){
                                pan = Integer.parseInt(split[2]);
                                boo = Integer.parseInt(split[3]);
                                tsumoFunc(3);
                            }
                            else if(split[1].equals("ron")){
                                pan = Integer.parseInt(split[2]);
                                boo = Integer.parseInt(split[3]);
                                loserWind = Integer.parseInt(split[4]);
                                ronFunc(3,(loserWind + 3) % 4);
                            }
                            else if(split[1].equals("Tenpai")){
                                isRiichi[(intentWind + 3) % 4] = true;
                                waitdraws++;
                                tenpais++;
                            }
                            else if(split[1].equals("noTenpai")){
                                isRiichi[(intentWind + 3) % 4] = false;
                                waitdraws++;
                            }
                            else if(split[1].equals("chonbo")){
                                chonboFunc(3);
                            }
                            break;
                            //본인은 상관하지 않아도 됨
                    }

                    if(waitdraws == 4){
                        ryugukFunc();
                    }

                    mHandler.postDelayed(updateBoard, 300);

                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public class ConnectSocket extends Thread{
        @Override
        public void run() {
            try {
                socket = MainActivity.getSocket();
                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class SendAgain extends Thread{
        @Override
        public void run(){
            try{
                out.println("riichi");
                out.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public class SendTsumo extends Thread{
        @Override
        public void run(){
            try{
                out.println("tsumo>" + pan + ">" + boo);
                out.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public class SendRon extends Thread{
        @Override
        public void run(){
            try{
                out.println("ron>" + pan + ">" + boo + ">" + loserWind);
                out.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public class SendDraw extends Thread{
        @Override
        public void run(){
            try{
                if(!isRiichi[intentWind]) out.println("noTenpai");
                else out.println("Tenpai");

                out.flush();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public class SendChonbo extends Thread{
        @Override
        public void run(){
            try{
                out.println("chonbo");
                out.flush();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public class SendRecovery extends Thread{
        @Override
        public void run(){
            try{
                out.println("recover");
                out.flush();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public class SendStatus extends Thread{
        int code = 0;

        SendStatus(int c){
                code = c;
        };

        @Override
        public void run(){
            try{
                String status = round + "::" + extend + "::" + oyaextend + "::" + scores[0] + "::" + scores[1] + "::" + scores[2] + "::" + scores[3] + "::" +
                        isRiichi[0] + "::" + isRiichi[1] + "::" + isRiichi[2] + "::" + isRiichi[3] + "::" + vault;
                switch(code){
                    case 0: // 현재 상태 저장
                        out.println("curr195727" + status + "::nonew");
                        break;
                    case 1: // 국 초기 상태 저장
                        out.println("new195727" + status + "::new");
                        break;
                    case 2: // revert far
                        out.println("prev");
                        break;
                    case 3:
                        out.println("near");
                        break;
                    case 4:
                        out.println("middle");
                        break;
                }
                out.flush();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
