package ch.ethz.inf.vs.android.siwehrli.sensors;

import java.util.ArrayList;
import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        ListView sensorListView = (ListView) findViewById(R.id.sensorList);
        SensorManager mySensorManager =(SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = mySensorManager.getSensorList(Sensor.TYPE_ALL);
        ArrayList<String> sensText = new ArrayList<String>();
        for (Sensor sensor : sensorList) {
			sensText.add(sensor.getName());
		}
        
        sensorListView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, sensText));
        
        sensorListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                
                String item = ((TextView)view).getText().toString();
                
                Toast.makeText(getBaseContext(), "rhabarber", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
