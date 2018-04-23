package snacker.mahjongNetClient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class ConnectPopup extends AppCompatActivity {

    public Button cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_popup);
        cancel = (Button) findViewById(R.id.cancelBtn);

    }
}
