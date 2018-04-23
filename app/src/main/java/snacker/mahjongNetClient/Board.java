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
    TextView lowerWind; // 하가
    TextView lowerLight; // 하가 리치상태
    TextView lowerScore; // 하가 점수
    TextView faceWind; // 대가
    TextView faceLight; // 대가 리치상태
    TextView faceScore; // 대가 점수
    TextView upperWind; // 상가
    TextView upperLight; // 상가 리치상태
    TextView upperScore; // 상가 점수
    TextView statusBoard; // 중앙상황판
    ImageView compare; // 좌측상단점점점

    Handler mHandler;
    RecieveThread wait;
    Calculate calc = new Calculate();

    int round = 1;
    int extend = 0;
    int[] scores = {30000, 30000, 30000, 30000};
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

            riichi = findViewById(R.id.riichi);
            tsumo = findViewById(R.id.tsumo);
            ron = findViewById(R.id.ron);
            riichi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (riichi.isEnabled()) {
                        scores[intentWind] -= 1000;
                        vault += 1000;
                        isRiichi[(intentWind)] = true;

                        riichi.setEnabled(false);

                        SendAgain send = new SendAgain();
                        send.start();
                        updateBoardFunc();
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

            compare.setOnTouchListener(mTouch);
            compare.setClickable(true);

            Intent intent = getIntent();
            intentWind = intent.getIntExtra("myWind", 0);

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
                            chonboFunc(intentWind);
                            SendChonbo send = new SendChonbo();
                            send.start();
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
        }
        return false;
    }



    /* 보드 갱신 */

    public Runnable updateBoard = new Runnable() { // 모든 작업이 끝난후 업데이트할것
        @Override
        public void run() {
            updateBoardFunc();
        }
    };

    public void updateBoardFunc(){
        myScore.setText("" + scores[intentWind]); //점수상태
        lowerScore.setText("" + scores[(intentWind + 1) % 4]);
        faceScore.setText("" + scores[(intentWind + 2) % 4]);
        upperScore.setText("" + scores[(intentWind + 3) % 4]);
        myWind.setText(MainActivity.winds[(intentWind + 9 - round) % 4]); //각자의 바람
        lowerWind.setText(MainActivity.winds[(intentWind + 10 - round) % 4]);
        faceWind.setText(MainActivity.winds[(intentWind + 11 - round) % 4]);
        upperWind.setText(MainActivity.winds[(intentWind + 12 - round) % 4]);
        statusBoard.setText((round <= 4 ? "동 " : "남 ") + ((round % 4) == 0 ? 4 : (round % 4)) + "국\n" + extend + "본장\n공탁:" + vault); // 중앙판

        switch((intentWind + 9 - round) % 4){ //오야가 누구야
            case 0:
                myWind.setBackgroundColor(Color.RED);
                upperWind.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 1:
                upperWind.setBackgroundColor(Color.RED);
                faceWind.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 2:
                faceWind.setBackgroundColor(Color.RED);
                lowerWind.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 3:
                lowerWind.setBackgroundColor(Color.RED);
                myWind.setBackgroundColor(Color.TRANSPARENT);
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


            if (round == 9 || isTobi(scores)) {
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



    /* 쓰모, 론, 유국 시 계산 */

    public void tsumoFunc(int winner){ //winner = 자신 기준 상대하가
        if(((winner + intentWind + 13 - round) % 4) == 0){ // 친이 쯔모
            oyajackpot = calc.oyaTsumo(pan, boo);
            scores[((winner + intentWind) % 4)] += ((3 * oyajackpot) + (300 * extend) + vault);
            scores[((winner + intentWind + 1) % 4)] -= oyajackpot + (100 * extend);
            scores[((winner + intentWind + 2) % 4)] -= oyajackpot + (100 * extend);
            scores[((winner + intentWind + 3) % 4)] -= oyajackpot + (100 * extend);
            extend++;
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
                scores[((winner + intentWind + 1) % 4)] -= jackpot + (100 * extend); //서가
                scores[((winner + intentWind + 2) % 4)] -= jackpot + (100 * extend); //북가
                scores[((winner + intentWind + 3) % 4)] -= oyajackpot + (100 * extend); //오야
            }
            if(((winner + intentWind + 13 - round) % 4) == 2){ // 서가 쯔모
                scores[((winner + intentWind) % 4)] += (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                scores[((winner + intentWind + 1) % 4)] -= jackpot + (100 * extend); //북가
                scores[((winner + intentWind + 2) % 4)] -= oyajackpot + (100 * extend); //오야
                scores[((winner + intentWind + 3) % 4)] -= jackpot + (100 * extend); //남가
            }
            if(((winner + intentWind + 13 - round) % 4) == 3){ // 북가 쯔모
                scores[((winner + intentWind) % 4)] += (oyajackpot + (2 * jackpot) + (300 * extend) + vault);
                scores[((winner + intentWind + 1) % 4)] -= oyajackpot + (100 * extend); //오야
                scores[((winner + intentWind + 2) % 4)] -= jackpot + (100 * extend); //남가
                scores[((winner + intentWind + 3) % 4)] -= jackpot + (100 * extend); //서가
            }
            round++;
            extend = 0;
            vault = 0;
            Arrays.fill(isRiichi,false);
            oyajackpot = 0;
            jackpot = 0;
        }
    }

    public void ronFunc(int winner, int loser){
        if(((winner + intentWind + 13 - round) % 4) == 0){ // 친이 론
            oyajackpot = calc.oyaRon(pan, boo);
            scores[(winner + intentWind) % 4] += (oyajackpot + (300 * extend) + vault);
            scores[(loser + intentWind) % 4] -= oyajackpot + (300 * extend);
            extend++;
            vault = 0;
            oyajackpot = 0;
            Arrays.fill(isRiichi,false);
        }
        else{
            jackpot = calc.childRon(pan, boo);
            scores[(winner + intentWind) % 4] += (jackpot + (300 * extend) + vault);
            scores[(loser + intentWind) % 4] -= jackpot + (300 * extend);
            round++;
            extend = 0;
            vault = 0;
            jackpot = 0;
            Arrays.fill(isRiichi,false);
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
                    if(isRiichi[i]) scores[i] += 3000;
                    else scores[i] -= 1000;
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 2:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) scores[i] += 1500;
                    else scores[i] -= 1500;
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 3:
                for(int i = 0; i < 4; i++){
                    if(isRiichi[i]) scores[i] += 1000;
                    else scores[i] -= 3000;
                }
                if(!isRiichi[(round - 1) % 4]) round++;
                break;
            case 4:
                break;
        }

        Arrays.fill(isRiichi, false);
        waitdraws = 0;
        tenpais = 0;
        waitingDraw = false;
    }

    public void chonboFunc(int player){ // 자신 기준 위치
        if((player + intentWind + 9 - round) % 4 == 0){
            scores[(player + intentWind) % 4] -= 12000;
            for(int i = 1; i < 4; i++) {
                scores[(player + intentWind + i) % 4] += 4000;
            }
        }
        else{
            scores[(player + intentWind) % 4] -= 8000;
            for(int i = 1; i < 4; i++) {
                if((player + intentWind + i + 9 - round) % 4 == 0) {
                    scores[(player + intentWind + i) % 4] += 4000;
                }
                else {
                    scores[(player + intentWind + i) % 4] += 2000;
                }
            }
        }
        Arrays.fill(isRiichi, false);
    }



    @Override
    public void onBackPressed(){
        if(!socket.isClosed()){ Toast.makeText(Board.this, "대국중에는 나가면 안 돼", Toast.LENGTH_SHORT).show(); }
        else{
            super.onBackPressed();
        }
    }



    /* 리스너 관련 메서드들 */

    View.OnTouchListener mTouch = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent action){
            switch(action.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lowerScore.setText("" + (scores[intentWind] - scores[intentWind + 1]));
                    if((scores[intentWind] - scores[intentWind + 1]) < 0) lowerScore.setTextColor(Color.RED);
                    else if((scores[intentWind] - scores[intentWind + 1]) > 0) lowerScore.setTextColor(Color.GREEN);
                    faceScore.setText(""+ (scores[intentWind] - scores[intentWind + 2]));
                    if((scores[intentWind] - scores[intentWind + 2]) < 0) faceScore.setTextColor(Color.RED);
                    else if((scores[intentWind] - scores[intentWind + 2]) > 0) faceScore.setTextColor(Color.GREEN);
                    upperScore.setText(""+ (scores[intentWind] - scores[intentWind + 3]));
                    if((scores[intentWind] - scores[intentWind + 3]) < 0) upperScore.setTextColor(Color.RED);
                    else if((scores[intentWind] - scores[intentWind + 3]) > 0) upperScore.setTextColor(Color.GREEN);
                    break;
                case MotionEvent.ACTION_UP:
                    lowerScore.setTextColor(Color.WHITE);
                    faceScore.setTextColor(Color.WHITE);
                    upperScore.setTextColor(Color.WHITE);
                    updateBoardFunc();
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
        bld.setMessage("동(" + IDs[0] + "): " + scores[0] + "\n"
                + "남(" + IDs[1] + "): " + scores[1] + "\n"
                +"서(" + IDs[2] + "): " + scores[2] + "\n"
                + "북(" + IDs[3] + "): " + scores[3]);
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
                            SendTsumo s = new SendTsumo();
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
                    split = msg.split(">");
                    if(split[0].equals("Server")){
                        continue;
                    }
                    if(split.length >= 2 && split[0].equals(MainActivity.ID)){
                        mHandler.post(updateBoard);
                        continue;
                    }

                    int who = findID(IDs, split[0]);
                    switch((who - intentWind + 4) % 4){
                        case 1: // 하가
                            if(split[1].equals("riichi")){
                                scores[(intentWind + 1) % 4] -= 1000;
                                isRiichi[(intentWind + 1) % 4] = true;
                                vault += 1000;
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
                                isRiichi[(intentWind + 2) % 4] = true;
                                vault += 1000;
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
                                isRiichi[(intentWind + 3) % 4] = true;
                                vault += 1000;
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

                    mHandler.post(updateBoard);

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
}
