package snacker.mahjongNetClient;

public class Calculate {

    public int panCalculate(int pickedPan){
        switch(pickedPan){
            case 0: return 1;
            case 1: return 2;
            case 2: return 3;
            case 3: return 4;
            case 4: return 5;
            case 5: return 6;
            case 6: return 8;
            case 7: return 11;
            case 8: return 13;
            case 9: return 26;
            case 10: return 39;
            case 11: return 52;
        }
        return 0;
    }

    public int booCalculate(int pickedBoo){
        switch(pickedBoo){
            case 0: return 20;
            case 1: return 25;
            case 2: return 30;
            case 3: return 40;
            case 4: return 50;
            case 5: return 60;
            case 6: return 70;
            case 7: return 80;
        }
        return 0;
    }

    public double baseCalculate(int pan, int boo){ // numberpicker의 array에서 다시 본래 값으로 치환해야 함
        return boo * Math.pow(2, pan+2);
    }

    public int oyaTsumo(int pan, int boo){
        switch(panCalculate(pan)){
            case 1: case 2: case 3: case 4:
                if((int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 2) / 100) * 100) > 4000) return 4000;
                else return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 2) / 100) * 100);
            case 5: return 4000;
            case 6: return 6000;
            case 8: return 8000;
            case 11: return 12000;
            case 13: return 16000;
            case 26: return 32000;
            case 39: return 48000;
            case 52: return 64000;
        }
        return 1000;
    }
    public int childTsumo(int pan, int boo){
        switch(panCalculate(pan)) {
            case 1: case 2: case 3: case 4:
                if((int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo))) / 100) * 100) > 2000) return 2000;
                else return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo))) / 100) * 100);
            case 5: return 2000;
            case 6: return 3000;
            case 8: return 4000;
            case 11: return 6000;
            case 13: return 8000;
            case 26: return 16000;
            case 39: return 24000;
            case 52: return 32000;
        }
        return 1000;
    }

    public int oyaRon(int pan, int boo){
        switch(panCalculate(pan)) {
            case 1: case 2: case 3: case 4:
                if((int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 6) / 100) * 100) > 12000) return 12000;
                else return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 6) / 100) * 100);
            case 5: return 12000;
            case 6: return 18000;
            case 8: return 24000;
            case 11: return 36000;
            case 13: return 48000;
            case 26: return 96000;
            case 39: return 144000;
            case 52: return 192000;
        }
        return 1000;
    }
    public int childRon(int pan, int boo){
        switch(panCalculate(pan)) {
            case 1: case 2: case 3: case 4:
                if((int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 4) / 100) * 100) > 8000) return 8000;
                else return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 4) / 100) * 100);
            case 5: return 8000;
            case 6: return 12000;
            case 8: return 16000;
            case 11: return 24000;
            case 13: return 36000;
            case 26: return 72000;
            case 39: return 108000;
            case 52: return 144000;
        }
        return 1000;
    }

    public int findMax(int arr[]){
        int max = 0;
        for(int i = 1; i < 4; i++){
            if(arr[max] < arr[i]) max = i;
        }
        return max;
    }

    public String findLeastRon(int score[], int me, int target, int round, int extend, int vault){
        int max = findMax(score);
        int pan;
        int boo;
        int tempscore[] = new int[4];
        int calc;

        System.arraycopy(score,0, tempscore, 0, 4);
        if(max == me) return "화료시 1위";
        if((round - 1) % 4 == me) { //내가 친이면?
                for(pan = 0; pan < 4; pan++) {
                    for (boo = 0; boo < 9; boo++) {
                        calc = oyaRon(pan, boo); //오야론으로 쏴야징
                        tempscore[target] -= (calc + extend * 300);
                        tempscore[me] += (calc + extend * 300 + vault);
                        if (findMax(tempscore) == me) {
                            if((pan == 0 && boo == 0)) return "화료시 1위";
                            else if(baseCalculate(pan, boo) > 1920) return "만관 직격";
                            return panCalculate(pan) + "판 " + booCalculate(boo) + "부";
                        }
                        tempscore[target] += (calc + extend * 300);
                        tempscore[me] -= (calc + extend * 300 + vault);
                    }
                }
                for(pan = 4; pan < 12; pan++){
                    calc = oyaRon(pan, 0); //오야론으로 쏴야징
                    tempscore[target] -= (calc + extend * 300);
                    tempscore[me] += (calc + extend * 300 + vault);
                    if (findMax(tempscore) == me) {
                        switch(pan){
                            case 4: return "만관 직격";
                            case 5: return "하네만 직격";
                            case 6: return "배만 직격";
                            case 7: return "삼배만 직격";
                            case 8: return "역만 직격";
                            case 9: return "더블역만";
                            case 10: return "트리플역만";
                            case 11: return "쿼드러플역만";
                        }
                    }
                    tempscore[target] += (calc + extend * 300);
                    tempscore[me] -= (calc + extend * 300 + vault);
            }
        }
        else{ //내가 자면?
                for(pan = 0; pan < 4; pan++) {
                    for (boo = 0; boo < 9; boo++) {
                        calc = childRon(pan, boo); //오야론으로 쏴야징
                        if(calc > 8000) calc = 8000;
                        tempscore[target] -= (calc + extend * 300);
                        tempscore[me] += (calc + extend * 300 + vault);
                        if (findMax(tempscore) == me) {
                            if((pan == 0 && boo == 0)) return "화료시 1위";
                            else if(baseCalculate(panCalculate(pan), booCalculate(boo)) > 1920) return "만관 직격";
                            return panCalculate(pan) + "판 " + booCalculate(boo) + "부";
                        }
                        tempscore[target] += (calc + extend * 300);
                        tempscore[me] -= (calc + extend * 300 + vault);
                    }
                }
                for(pan = 4; pan < 12; pan++){
                    calc = childRon(pan, 0); //오야론으로 쏴야징
                    tempscore[target] -= (calc + extend * 300);
                    tempscore[me] += (calc + extend * 300 + vault);
                    if (findMax(tempscore) == me) {
                        switch(pan){
                            case 4: return "만관 직격";
                            case 5: return "하네만 직격";
                            case 6: return "배만 직격";
                            case 7: return "삼배만 직격";
                            case 8: return "역만 직격";
                            case 9: return "더블역만";
                            case 10: return "트리플역만";
                            case 11: return "쿼드러플역만";
                        }
                    }
                    tempscore[target] += (calc + extend * 300);
                    tempscore[me] -= (calc + extend * 300 + vault);
                }
        }
        return "수정";
    }

    public String findLeastTsumo(int score[], int me, int round, int extend, int vault){
        int max = findMax(score);
        int pan;
        int boo;
        int tempscore[] = new int[4];
        int calc;

        System.arraycopy(score,0, tempscore, 0, 4);
        if(max == me) return "화료시 1위";
        if((round - 1) % 4 == me) { //내가 친이면?
            for(pan = 0; pan < 4; pan++) {
                for (boo = 0; boo < 8; boo++) {
                    calc = oyaTsumo(pan, boo); //오야가 츠모
                    for(int i = 0; i < 4; i++){
                        if(i == me) tempscore[i] += (calc * 3 + extend * 300 + vault);
                        else tempscore[i] -= (calc + extend * 100);
                    }
                    if (findMax(tempscore) == me) {
                        if((pan == 0 && boo == 0)) return "화료시 1위";
                        else if(baseCalculate(pan, boo) > 1920) return "만관 쓰모";
                        return panCalculate(pan) + "판 " + booCalculate(boo) + "부 쓰모";
                    }
                    for(int i = 0; i < 4; i++){
                        if(i == me) tempscore[i] -= (calc * 3 + extend * 300 + vault);
                        else tempscore[i] += (calc + extend * 100);
                    }
                }
            }
            for(pan = 4; pan < 12; pan++){
                calc = oyaTsumo(pan, 0); //오야론으로 쏴야징
                for(int i = 0; i < 4; i++){
                    if(i == me) tempscore[i] += (calc * 3 + extend * 300 + vault);
                    else tempscore[i] -= (calc + extend * 100);
                }
                if (findMax(tempscore) == me) {
                    switch(pan){
                        case 4: return "만관 쓰모";
                        case 5: return "하네만 쓰모";
                        case 6: return "배만 쓰모";
                        case 7: return "삼배만 쓰모";
                        case 8: return "역만 쓰모";
                        case 9: return "더블역만";
                        case 10: return "트리플역만";
                        case 11: return "쿼드러플역만";
                    }
                }
                for(int i = 0; i < 4; i++){
                    if(i == me) tempscore[i] -= (calc * 3 + extend * 300 + vault);
                    else tempscore[i] += (calc + extend * 100);
                }
            }
        }
        else{ //내가 자면?
            int oyacalc;
            for(pan = 0; pan < 4; pan++) {
                for (boo = 0; boo < 8; boo++) {
                    calc = childTsumo(pan, boo); //친이 쓰모했다
                    oyacalc = oyaTsumo(pan, boo);
                    for(int i = 0; i < 4; i++){
                        if(i == me) tempscore[i] += (calc * 2 + oyacalc + extend * 300 + vault);
                        else if(i == (round - 1) % 4) tempscore[i] -= (oyacalc + extend * 100);
                        else tempscore[i] -= (calc + extend * 100);
                    }
                    if (findMax(tempscore) == me) {
                        if((pan == 0 && boo == 0)) return "화료시 1위";
                        else if(baseCalculate(panCalculate(pan), booCalculate(boo)) > 1920) return "만관 쓰모";
                        return panCalculate(pan) + "판 " + booCalculate(boo) + "부 쓰모";
                    }
                    for(int i = 0; i < 4; i++){
                        if(i == me) tempscore[i] -= (calc * 2 + oyacalc + extend * 300 + vault);
                        else if(i == (round - 1) % 4) tempscore[i] += (oyacalc + extend * 100);
                        else tempscore[i] += (calc + extend * 100);
                    }
                }
            }
            for(pan = 4; pan < 12; pan++){
                calc = childTsumo(pan, 0);
                oyacalc = oyaTsumo(pan ,0);
                for(int i = 0; i < 4; i++){
                    if(i == me) tempscore[i] += (calc * 2 + oyacalc + extend * 300 + vault);
                    else if(i == (round - 1) % 4) tempscore[i] -= (oyacalc + extend * 100);
                    else tempscore[i] -= (calc + extend * 100);
                }
                if (findMax(tempscore) == me) {
                    switch(pan){
                        case 4: return "만관 쓰모";
                        case 5: return "하네만 쓰모";
                        case 6: return "배만 쓰모";
                        case 7: return "삼배만 쓰모";
                        case 8: return "역만 쓰모";
                        case 9: return "더블역만";
                        case 10: return "트리플역만";
                        case 11: return "쿼드러플역만";
                    }
                }
                for(int i = 0; i < 4; i++){
                    if(i == me) tempscore[i] -= (calc * 2 + oyacalc + extend * 300 + vault);
                    else if(i == (round - 1) % 4) tempscore[i] += (oyacalc + extend * 100);
                    else tempscore[i] += (calc + extend * 100);
                }
            }
        }
        return "수정";
    }
}
