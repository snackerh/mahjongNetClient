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
import android.widget.CheckBox;
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

    TextView[] windText;
    TextView[] riichiText;
    TextView[] scoreText;
    TextView[] changeText;

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
    int myWindval;

    //유국 전용
    boolean waitingDraw = false;
    int waitdraws = 0;
    int tenpais = 0;

    int winnerWind = -1;
    int loserWind = -1;
    int pan = -1;
    int boo = -1;
    // 더블론 전용
    int doublewinner = -1;
    int doublepan = -1;
    int doubleboo = -1;

    int jackpot; // 자 계산
    int oyajackpot; // 친 계산

    int maxround = 8;
    int maxroundmod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //TODO: 친이 튕겼다가 재접속했을시 대국이 초기화되는 현상 수정

        mHandler = new Handler();

        maxroundmod = (maxround + 3) / 4 * 4 + 1; // modified
            ConnectSocket connect = new ConnectSocket();
            connect.start();

            mHandler.postDelayed(new Runnable() {
                public void run() {
                    wait = new RecieveThread();
                    wait.start();
                }
            }, 1000);

        Intent intent = getIntent();
        myWindval = intent.getIntExtra("myWind", 0);

        windText[myWindval] = findViewById(R.id.myWind);
        windText[(myWindval + 1) % 4] = findViewById(R.id.lowerWind);
        windText[(myWindval + 2) % 4] = findViewById(R.id.faceWind);
        windText[(myWindval + 3) % 4] = findViewById(R.id.upperWind);

        riichiText[myWindval] = findViewById(R.id.myLight);
        riichiText[(myWindval + 1) % 4] = findViewById(R.id.lowerLight);
        riichiText[(myWindval + 2) % 4] = findViewById(R.id.faceLight);
        riichiText[(myWindval + 3) % 4] = findViewById(R.id.upperLight);

        scoreText[myWindval] = findViewById(R.id.myScore);
        scoreText[(myWindval + 1) % 4] = findViewById(R.id.lowerScore);
        scoreText[(myWindval + 2) % 4]= findViewById(R.id.faceScore);
        scoreText[(myWindval + 3) % 4] = findViewById(R.id.upperScore);

        changeText[myWindval] = findViewById(R.id.meCompare);
        changeText[(myWindval + 1) % 4] = findViewById(R.id.lowerCompare);
        changeText[(myWindval + 2) % 4] = findViewById(R.id.faceCompare);
        changeText[(myWindval + 3) % 4] = findViewById(R.id.upperCompare);

        statusBoard = findViewById(R.id.middle);
        compare = findViewById(R.id.compare);

        riichi = findViewById(R.id.riichi);
        tsumo = findViewById(R.id.tsumo);
        ron = findViewById(R.id.ron);
        riichi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (riichi.isEnabled()) {
                    if(scores[myWindval] < 1000) Toast.makeText(Board.this, "리치 불가",Toast.LENGTH_SHORT).show();
                    else {
                        scores[myWindval] -= 1000;
                        change[myWindval] = -1000;
                        vault += 1000;
                        isRiichi[(myWindval)] = true;

                        riichi.setEnabled(false);
                        if (myWindval == 0) {
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

        for(int i = 1; i < 4; i++) {
            scoreText[(myWindval + i) % 4].setOnClickListener(mClick);
            windText[(myWindval + i) % 4].setOnClickListener(mClick);
        }

            //compare.setOnTouchListener(mTouch);
        compare.setOnClickListener(imageClick);
        compare.setClickable(true);

            if(myWindval == 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: 이부분을 고쳐서 대국에 복귀할시에 상황이 초기화되지않도록 할것
                        SendStatus send = new SendStatus(6);
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
                    bld.setMessage("유국을 선택하셨습니다.\n당신은 텐파이입니까?\n(도중유국일 경우 전부 '네'를 선택하십시오.)");
                    bld.setNegativeButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            isRiichi[myWindval] = true;
                            waitingDraw = true;
                            waitdraws++;
                            tenpais++;
                            SendDraw send = new SendDraw();
                            send.start();
                        }
                    });
                    bld.setPositiveButton("아니오", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichbutton) {
                            isRiichi[myWindval] = false;
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
                if(!waitingDraw && myWindval == 0){
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
                    Toast.makeText(Board.this, "해당 기능은 첫 친에게 문의하십시오.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.revertMiddle:
                if(!waitingDraw && myWindval == 0){
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
                    Toast.makeText(Board.this, "해당 기능은 첫 친에게 문의하십시오.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.revertFar:
                if(!waitingDraw && myWindval == 0){
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
                    Toast.makeText(Board.this, "해당 기능은 첫 친에게 문의하십시오.", Toast.LENGTH_SHORT).show();
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
            for(int i = 0; i < 4; i++){
                scoreText[i].setTextSize(36);
            }
            updateBoardFunc();
        }
    };


    public void changeUpdate(){
        for(int i = 0; i < 4; i++){
            changeText[i].setTextColor(Color.WHITE);
            changeText[i].setText(MainActivity.IDs[i]);
        }
        Arrays.fill(change, 0);
    }

    public void updateBoardFunc(){
        Log.d("upd", ""+ scores[myWindval]);

        switch(boardToggle){
            case 0:
                for(int i = 0; i < 4; i++){
                    scoreText[i].setTextSize(36);
                    scoreText[i].setText("" + scores[myWindval]);
                }
                break;
            case 1:
                scoreText[myWindval].setText("" + scores[myWindval]);

                for(int i = 1; i < 4; i++){
                    scoreText[(myWindval + i) % 4].setText("" + (scores[myWindval] - scores[(myWindval + i) % 4]));
                    if((scores[myWindval] - scores[(myWindval + 1) % 4]) < 0) scoreText[(myWindval + i) % 4].setTextColor(Color.RED);
                    else if((scores[myWindval] - scores[(myWindval + 1) % 4]) > 0) scoreText[(myWindval + i) % 4].setTextColor(Color.GREEN);
                    else { scoreText[(myWindval + i) % 4].setTextColor(Color.WHITE); scoreText[(myWindval + i) % 4].setText("±0");}
                }
                break;
            case 2:
                scoreText[myWindval].setTextSize(18);
                scoreText[myWindval].setText(calc.findLeastTsumo(scores, myWindval, round, extend, vault));

                for(int i = 1; i < 4; i++) {
                    scoreText[(myWindval + i) % 4].setTextColor(Color.WHITE);
                    scoreText[(myWindval + i) % 4].setTextSize(18);
                    scoreText[(myWindval + i) % 4].setText(calc.findLeastRon(scores, myWindval, (myWindval + 1) % 4, round, extend, vault));
                }
        }

        for(int i = 0; i < 4; i++) {
            windText[(myWindval + i) % 4].setText(MainActivity.winds[(myWindval + maxroundmod + i - round) % 4]);
        }

        statusBoard.setText((round <= 4 ? "동 " : round <= 8  ? "남 " : "서 ") + ((round % 4) == 0 ? 4 : (round % 4)) + "국\n" + extend + (oyaextend ? "연장" : "본장") + "\n공탁:" + vault); // 중앙판
        if(oyaextend) statusBoard.setBackgroundResource(R.color.colorAccent);
        else statusBoard.setBackgroundResource(R.color.lightBackground);

        for(int i = 0; i < 4; i++){
            if(change[(myWindval + i) % 4] > 0){
                changeText[(myWindval + i) % 4].setTextColor(Color.GREEN);
                changeText[(myWindval + i) % 4].setText("+" + change[(myWindval + i) % 4]);
            } else if(change[(myWindval + i) % 4] < 0){
                changeText[(myWindval + i) % 4].setTextColor(Color.RED);
                changeText[(myWindval + i) % 4].setText("-" + change[(myWindval + i) % 4]);
            }
        }
        mHandler.postDelayed(updateChange, 2000);

        for(int i = 0; i < 4; i++) { //오야 찾기
            if(i == (round % 4)) windText[i].setBackgroundColor(Color.RED);
            else windText[i].setBackgroundColor(Color.TRANSPARENT);
        }

        if(waitingDraw){
            riichi.setEnabled(false);
            tsumo.setEnabled(false);
            ron.setEnabled(false);
        }
        else {
            for(int i = 0; i < 4; i += 2){
                if (isRiichi[(myWindval + i) % 4]) riichiText[(myWindval + i) % 4].setBackgroundResource(R.drawable.riichih); //가로(나,대면) 리치봉
                else riichiText[(myWindval + i) % 4].setBackgroundColor(Color.TRANSPARENT);
            }
            for(int i = 1; i < 4; i += 2){
                if (isRiichi[(myWindval + i) % 4]) riichiText[(myWindval + i) % 4].setBackgroundResource(R.drawable.riichiv); //세로(상가,하가) 리치봉
                else riichiText[(myWindval + i) % 4].setBackgroundColor(Color.TRANSPARENT);
            }

            if (round == (maxround + 1) || (round == maxround && isTop(scores) && extend >= 1) || isTobi(scores)) {
                if(vault != 0){
                    scores[findTop(scores)] += vault;
                    vault = 0;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
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
                if (!isRiichi[(myWindval)]) riichi.setEnabled(true);
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

    public int findTop(int[] arr){
        int top = 0;
        for(int i = 1; i < 4; i++){
            if(arr[i] > arr[top]) top = i;
        }
        return top;
    }

    public void recover(String[] st){
        round = Integer.parseInt(st[0]);
        extend = Integer.parseInt(st[1]);
        oyaextend = Boolean.parseBoolean(st[2]);
        for(int i = 0; i < 4; i++){
            scores[i] = Integer.parseInt(st[3 + i]);
            isRiichi[i] = Boolean.parseBoolean(st[7 + i]);
        }
        vault = Integer.parseInt(st[11]);
        for(int i = 0; i < 12; i++) {
            Log.d("st", st[i]);
        }
    }

    public boolean stillOyaExtend(){
        return extend == 1 || (extend >= 2 && oyaextend);
    }

    /* 쓰모, 론, 유국 시 계산 */
    //TODO: 모든 작업 완료 후 제대로 승자변수 초기화시키기
    public void resetWind(){
        winnerWind = -1;
        loserWind = -1;
        doublewinner = -1;
        pan = -1;
        boo = -1;
        doublepan = -1;
        doubleboo = -1;
    }

    public void changeScore(int pos, int val) {
        scores[pos] += val;
        change[pos] += val;
    }

    public void tsumoFunc(int winner){ //winner = 변수기준
        if(((winner + maxroundmod - round) % 4) == 0){ // 친이 쯔모
            oyajackpot = calc.oyaTsumo(pan, boo) + (100 * extend);
            changeScore(winner, ((3 * oyajackpot) + vault));
            for(int i = 1; i < 4; i++){
                changeScore(((winner + i) % 4), -oyajackpot);
            }
            extend++;
            oyaextend = stillOyaExtend();
            Arrays.fill(isRiichi,false);
            vault = 0;
            oyajackpot = 0;
        }
        else{
            oyajackpot = calc.oyaTsumo(pan, boo) + (100 * extend);
            jackpot = calc.childTsumo(pan, boo) + (100 * extend);
            Log.d("pan", "" + pan);
            Log.d("boo", "" + boo);
            Log.d("oyajackpot", ""+ oyajackpot);
            Log.d("jackpot", ""+ jackpot);

            changeScore((winner % 4), (oyajackpot + (2 * jackpot) + vault));

            for(int i = 1; i < 4; i++){
                if((winner + i + maxroundmod - round) % 4 == 0){ changeScore((winner + i) % 4, -oyajackpot); }
                else changeScore((winner + i) % 4, -jackpot);
            }

            round++;
            extend = 0;
            oyaextend = false;
            vault = 0;
            Arrays.fill(isRiichi,false);
            oyajackpot = 0;
            jackpot = 0;
        }
        if(myWindval == 0) {
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
        if(((winner + maxroundmod - round) % 4) == 0){ // 친이 론
            oyajackpot = calc.oyaRon(pan, boo) + (300 * extend);
            changeScore(winner, oyajackpot + vault);
            changeScore(loser, -oyajackpot);
            extend++;
            oyaextend = stillOyaExtend();
            vault = 0;
            oyajackpot = 0;
            Arrays.fill(isRiichi,false);
        }
        else{
            jackpot = calc.childRon(pan, boo) + (300 * extend);
            changeScore(winner, jackpot + vault);
            changeScore(loser, -jackpot);
            round++;
            extend = 0;
            oyaextend = false;
            vault = 0;
            jackpot = 0;
            Arrays.fill(isRiichi,false);
        }
        if(myWindval == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SendStatus send = new SendStatus(1);
                    send.start();
                }
            });
        }
    }

    public void doubleronFunc(int winner, int secondwinner, int loser){
        boolean didOya = false;
        change[(loser + myWindval) % 4] = 0;
        if(((winner + myWindval + maxroundmod - round) % 4) == 0){ // 첫번째, 친이 론
            oyajackpot = calc.oyaRon(pan, boo);
            scores[(winner + myWindval) % 4] += (oyajackpot + (300 * extend));
            change[(winner + myWindval) % 4] = (oyajackpot + (300 * extend));
            scores[(loser + myWindval) % 4] -= oyajackpot + (300 * extend);
            change[(loser + myWindval) % 4] -= (oyajackpot + (300 * extend));
            didOya = true;
        }
        else{
            jackpot = calc.childRon(pan, boo);
            scores[(winner + myWindval) % 4] += (jackpot + (300 * extend));
            change[(winner + myWindval) % 4] = (jackpot + (300 * extend));
            scores[(loser + myWindval) % 4] -= jackpot + (300 * extend);
            change[(loser + myWindval) % 4] -= (jackpot + (300 * extend));
        }

        if(((secondwinner + myWindval + maxroundmod - round) % 4) == 0){ // 두번째, 친이 론
            oyajackpot = calc.oyaRon(doublepan, doubleboo);
            scores[(secondwinner + myWindval) % 4] += (oyajackpot + (300 * extend));
            change[(secondwinner + myWindval) % 4] = (oyajackpot + (300 * extend));
            scores[(loser + myWindval) % 4] -= oyajackpot + (300 * extend);
            change[(loser + myWindval) % 4] -= (oyajackpot + (300 * extend));
            didOya = true;
        }
        else{
            jackpot = calc.childRon(doublepan, doubleboo);
            scores[(secondwinner + myWindval) % 4] += (jackpot + (300 * extend));
            change[(secondwinner + myWindval) % 4] = (jackpot + (300 * extend));
            scores[(loser + myWindval) % 4] -= jackpot + (300 * extend);
            change[(loser + myWindval) % 4] -= (jackpot + (300 * extend));
        }

        //누가 공탁금을 가져가지?
        if(((winner + 4 - loser) % 4) < ((secondwinner + 4 - loser) % 4)){ //방총자기준 선하네
            scores[(winner + myWindval) % 4] += vault;
            change[(winner + myWindval) % 4] += vault;
        }
        else{
            scores[(secondwinner + myWindval) % 4] += vault;
            change[(secondwinner + myWindval) % 4] += vault;
        }
        vault = 0;

        if(didOya){
            extend++;
            oyaextend = stillOyaExtend();
            oyajackpot = 0;
            Arrays.fill(isRiichi,false);
        } else {
            round++;
            extend = 0;
            oyaextend = false;
            jackpot = 0;
            Arrays.fill(isRiichi,false);
        }

        if(myWindval == 0) {
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
                    if(isRiichi[i]) { changeScore(i, 3000); }
                    else { changeScore(i, -1000); }
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 2:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) { changeScore(i, 1500);}
                    else { changeScore(i, -1500); }
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 3:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) { changeScore(i, 1000); }
                    else { changeScore(i, -3000); }
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

        if(myWindval == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SendStatus send = new SendStatus(1);
                    send.start();
                }
            });
        }
    }

    public void chonboFunc(int player){ // 변수 기준 위치
        if((player + maxroundmod - round) % 4 == 0){
            changeScore(player, -12000);
            for(int i = 1; i < 4; i++) {
                changeScore((player + i) % 4, 4000);
            }
        }
        else{
            changeScore(player, -8000);
            for(int i = 1; i < 4; i++) {
                if((player + i + maxroundmod - round) % 4 == 0) {
                    changeScore((player + i) % 4, 4000);
                }
                else {
                    changeScore((player + i) % 4, 2000);
                }
            }
        }

        for(int i = 0; i < 4; i++){
            if(isRiichi[i]) {
                changeScore(i, 1000);
                vault -= 1000;
            }
        }
        Arrays.fill(isRiichi, false);

        if(myWindval == 0) {
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
                        if(winnerWind == -1) winnerWind = 0;
                        else doublewinner = 0;
                        ronPressed = true;
                        updateBoardFunc();
                    }
                    else{
                        if(doublewinner != -1) doublewinner = -1;
                        else winnerWind = -1;
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
        SendStatus send = new SendStatus(5);
        send.start();
    }

    //TODO: 더블론 기능 추가하기
    //TODO: 서버쪽도 관련 기능 업데이트하기

    public void showDialog(){
        final String[] panarr = {"1판","2판","3판","4판","만관","하네만","배만","삼배만","역만","더블역만","트리플역만","쿼드러플역만"};
        final String[] booarr = {"20부","25부","30부","40부","50부","60부","70부","80부"};
        LayoutInflater dialog = LayoutInflater.from(this);
        final View dialoglayout = dialog.inflate(R.layout.activity_score_dialog, null);
        final Dialog call = new Dialog(this);

        call.setContentView(dialoglayout);
        final NumberPicker panPicker = call.findViewById(R.id.panPicker);
        final NumberPicker booPicker = call.findViewById(R.id.booPicker);
        final CheckBox doubldchk = call.findViewById(R.id.doublecheck);
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
                if(pan == -1) {
                    pan = panPicker.getValue();
                    boo = booPicker.getValue();
                }
                else{
                    doublepan = panPicker.getValue();
                    doubleboo = booPicker.getValue();
                }

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
                else {
                    if (doubldchk.isChecked()) {
                        if (winnerWind == -1 || winnerWind == 0){
                            winnerWind = 0;
                        }
                        else{
                            doublewinner = 0;
                            doubleronFunc(winnerWind, doublewinner, loserWind);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    resetWind();
                                }
                            }, 1000);
                        }
                        mHandler.post(new Runnable(){
                            @Override
                            public void run() {
                                SendDoubleRon s = new SendDoubleRon();
                                s.start();
                            }
                        });

                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                SendRon s = new SendRon();
                                s.start();
                            }
                        });
                        ronFunc(winnerWind, loserWind);
                    }

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
                        // TODO: 서버쪽 코드를 수정해서 접속시 상황에 맞게 현재 상황을 보내줄것
                        // TODO: 만약 새로 대국을 시작해야 할 경우 서버에서 새로운 데이터를 보내줄것
                        if(split[1].equals("prev") || split[1].equals("middle") || split[1].equals("near") || split[1].equals("curr")){ //revert or recover
                            String[] recoverStatus = split[2].split("::");
                            recover(recoverStatus);
                        }
                        Log.d("wgat","what");
                        mHandler.post(updateBoard);
                        continue;
                    }
                    int who = findID(IDs, split[0]);
                    int whofromme = (who - myWindval + 4) % 4;
                    switch(whofromme){ //내 기준
                        case 0:
                            Log.d("match", MainActivity.ID);
                            mHandler.post(updateBoard);
                            break;
                        case 1: case 2: case 3: //순서대로 하가대가상가
                            switch (split[1]) {
                                case "riichi":
                                    scores[(myWindval + whofromme) % 4] -= 1000;
                                    change[(myWindval + whofromme) % 4] = -1000;
                                    isRiichi[(myWindval + whofromme) % 4] = true;
                                    vault += 1000;
                                    if (myWindval == 0) {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                SendStatus send = new SendStatus(0);
                                                send.start();
                                            }
                                        });
                                    }
                                    resetWind();
                                    break;
                                case "tsumo":
                                    pan = Integer.parseInt(split[2]);
                                    boo = Integer.parseInt(split[3]);
                                    tsumoFunc(whofromme);
                                    resetWind();
                                    break;
                                case "ron":
                                    pan = Integer.parseInt(split[2]);
                                    boo = Integer.parseInt(split[3]);
                                    loserWind = Integer.parseInt(split[4]);
                                    ronFunc(whofromme, (loserWind + whofromme) % 4);
                                    resetWind();
                                    break;
                                case "doubleron":
                                    if (loserWind == -1) loserWind = (whofromme + Integer.parseInt(split[4])) % 4;
                                    if (winnerWind == -1) {
                                        winnerWind = whofromme;
                                        pan = Integer.parseInt(split[2]);
                                        boo = Integer.parseInt(split[3]);
                                    } else {
                                        doublewinner = whofromme;
                                        doublepan = Integer.parseInt(split[2]);
                                        doubleboo = Integer.parseInt(split[3]);
                                        doubleronFunc(winnerWind, doublewinner, loserWind);
                                        resetWind();
                                    }
                                    break;
                                case "Tenpai":
                                    isRiichi[(myWindval + whofromme) % 4] = true;
                                    waitdraws++;
                                    tenpais++;
                                    break;
                                case "noTenpai":
                                    isRiichi[(myWindval + whofromme) % 4] = false;
                                    waitdraws++;
                                    break;
                                case "chonbo":
                                    chonboFunc(whofromme);
                                    resetWind();
                                    break;
                            }
                            break;
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
                out = new PrintWriter(socket.getOutputStream(),true);
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
                resetWind();
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
                resetWind();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public class SendDoubleRon extends Thread{
        @Override
        public void run(){
            try{
                out.println("doubleron>" + pan + ">" + boo + ">" + loserWind);
                //resetWind();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public class SendDraw extends Thread{
        @Override
        public void run(){
            try{
                if(!isRiichi[myWindval]) out.println("noTenpai");
                else out.println("Tenpai");
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
                resetWind();
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
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public class SendStatus extends Thread{
        int code;

        SendStatus(int c){
                code = c;
        }

        @Override
        public void run(){
            try{
                String status = round + "::" + extend + "::" + oyaextend + "::" + scores[0] + "::" + scores[1] + "::" + scores[2] + "::" + scores[3] + "::" +
                        isRiichi[0] + "::" + isRiichi[1] + "::" + isRiichi[2] + "::" + isRiichi[3] + "::" + vault;
                switch(code){
                    case 0: // 현재 상태 저장
                        out.println("curr####" + status + "::nonew");
                        break;
                    case 1: // 국 초기 상태 저장
                        out.println("new####" + status + "::new");
                        break;
                    case 2: // revert far
                        out.println("prev");
                        break;
                    case 3: //revert near
                        out.println("near");
                        break;
                    case 4: //revert to start of the round
                        out.println("middle");
                        break;
                    case 5: //대국종료 확인
                        out.println("end####" + status + "::end");
                        break;
                    case 6:
                        out.println("start####" + status + "::new");
                        break;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
