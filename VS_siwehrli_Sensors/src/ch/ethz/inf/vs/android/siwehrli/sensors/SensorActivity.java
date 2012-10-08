package ch.ethz.inf.vs.android.siwehrli.sensors;

import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SensorActivity extends Activity {

	private int sensorAccuracy;
	private Sensor selectedSensor;
	private SensorManager mySensorManager;
	private SensorEventListener mySensorListener;
	private ListView valueList;
	private ListView valueDescriptionList;
	private ArrayAdapter<Float> valueAdapter;
	private ArrayAdapter<String> valueDescriptionAdapter;
	private TextView sensorName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensor);

		mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = mySensorManager
				.getSensorList(Sensor.TYPE_ALL);

		mySensorListener = new SensorEventListener() {

			public void onSensorChanged(SensorEvent event) {
				valueAdapter.clear();
				for (float value : event.values) {
					valueAdapter.add(value);
				}
				sensorName.setText(selectedSensor.getName());
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				selectedSensor = sensor;
				sensorAccuracy = accuracy;
				sensorName.setText(selectedSensor.getName());

			}
		};
		
		// get GUI elements
		sensorName = (TextView) findViewById(R.id.sensorName);
		

		valueList = (ListView) findViewById(R.id.valueList);
		valueDescriptionList = (ListView) findViewById(R.id.valueDescriptionList);

		// get sensor index back
		Intent startIntent = getIntent();
		selectedSensor = sensorList.get(startIntent.getIntExtra("sensorId", 0));
		mySensorManager.registerListener(mySensorListener, selectedSensor,
				SensorManager.SENSOR_DELAY_FASTEST);

		valueAdapter = new ArrayAdapter<Float>(this,
				android.R.layout.simple_list_item_1);
		valueDescriptionAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		
		valueDescriptionAdapter.add("Values");
		
		valueDescriptionList.setAdapter(valueDescriptionAdapter);
		valueList.setAdapter(valueAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_sensor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mySensorManager.registerListener(mySensorListener, selectedSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	protected void onStop() {
		mySensorManager.unregisterListener(mySensorListener);
		super.onStop();
	}
}
