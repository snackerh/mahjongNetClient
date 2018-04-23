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
                return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 2) / 100) * 100);
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
                return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo))) / 100) * 100);
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
                return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 6) / 100) * 100);
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
                return (int) (Math.ceil((baseCalculate(panCalculate(pan), booCalculate(boo)) * 4) / 100) * 100);
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
}
