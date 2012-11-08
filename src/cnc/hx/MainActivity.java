package cnc.hx;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	Button btWifi, btNfc;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btWifi = (Button) findViewById(R.id.btWifi);
        btNfc = (Button) findViewById(R.id.btNfc);

        btWifi.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, WiFiDirectActivity.class));
			}
		});
        
        btNfc.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
			}
		});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
