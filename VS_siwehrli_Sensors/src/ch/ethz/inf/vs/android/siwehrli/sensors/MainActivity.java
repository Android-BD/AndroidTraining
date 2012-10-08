package ch.ethz.inf.vs.android.siwehrli.sensors;

import java.util.ArrayList;
import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        ListView sensorListView = (ListView) findViewById(R.id.sensorList);
        SensorManager mySensorManager =(SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = mySensorManager.getSensorList(Sensor.TYPE_ALL);
        ArrayList<String> sensorText = new ArrayList<String>();
        for (Sensor sensor : sensorList) {
			sensorText.add(sensor.getName());
		}
        
        sensorListView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, sensorText));
        
        sensorListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
            	//create Intent to launch other Activity
                Intent sensorDetails = new Intent(view.getContext(), SensorActivity.class);
                
                //put in Argument
                sensorDetails.putExtra("sensorId", position);
                //switch to other Activity
                startActivity(sensorDetails);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onActuatorButtonClick(View v)
    {
    	Intent actuatorsIntent = new Intent(this, ActuatorsActivity.class);
    	startActivity(actuatorsIntent);
    }
    
}
