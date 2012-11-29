package cnc.hx;

import cnc.hx.utils.Constants;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	Button btBuzzer, btAvOnOff, btSettings, btConnectOnOff, btExit;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btBuzzer = (Button) findViewById(R.id.btBuzzer);
        btAvOnOff = (Button) findViewById(R.id.btAvOnOff);
        btSettings = (Button) findViewById(R.id.btSettings);
        btConnectOnOff = (Button) findViewById(R.id.btConnectOnOff);
        btExit = (Button) findViewById(R.id.btExit);
        
        btBuzzer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, VideoConversationActivity.class);
				startActivity(i);
			}
		});
        btAvOnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, WiFiDirectActivity.class);
				i.putExtra(Constants.CALL_INTENT, 2); // IP Camera Video
				startActivity(i);
			}
		});
        btConnectOnOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, WiFiDirectActivity.class));
			}
		});
        btExit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
