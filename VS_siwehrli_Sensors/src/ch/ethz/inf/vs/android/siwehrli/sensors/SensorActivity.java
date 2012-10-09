package ch.ethz.inf.vs.android.siwehrli.sensors;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class SensorActivity extends Activity {

	private Sensor selectedSensor;
	private SensorManager mySensorManager;
	private SensorEventListener mySensorListener;
	private TextView sensorName;
	private TextView value0Box;
	private TextView value1Box;
	private TextView value2Box;
	private SurfaceView graphSurface;
	private SurfaceHolder surfaceViewHolder;
	private Canvas graphCanvas;
	private Bitmap graphBitmap;
	private Paint graphPaint;
	private long time;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensor);

		mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = mySensorManager
				.getSensorList(Sensor.TYPE_ALL);

		mySensorListener = new SensorEventListener() {

			public void onSensorChanged(SensorEvent event) {
				sensorName.setText(selectedSensor.getName());
				value0Box.setText(Float.toString(event.values[0]));
				value1Box.setText(Float.toString(event.values[1]));
				value2Box.setText(Float.toString(event.values[2]));
				drawCanvas();
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				selectedSensor = sensor;
				sensorName.setText(selectedSensor.getName());
				drawCanvas();
			}
		};
		
		time = SystemClock.currentThreadTimeMillis()-101;
		
		// get GUI elements
		sensorName = (TextView) findViewById(R.id.sensorName);
		value0Box = (TextView) findViewById(R.id.value0Box);
		value1Box = (TextView) findViewById(R.id.value1Box);
		value2Box = (TextView) findViewById(R.id.value2Box);

		//graph Stuff
		graphSurface = (SurfaceView) findViewById(R.id.graphSurface);
		surfaceViewHolder = graphSurface.getHolder();
		
		graphPaint = new Paint();
	    graphPaint.setColor(Color.BLUE);
	    graphPaint.setStrokeWidth(3);

		// get sensor index back
		Intent startIntent = getIntent();
		selectedSensor = sensorList.get(startIntent.getIntExtra("sensorId", 0));
		mySensorManager.registerListener(mySensorListener, selectedSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
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
	
	
	//Draw Logic
	public void drawCanvas()
	{
		//Fix to 30fps
		if((SystemClock.currentThreadTimeMillis()-time) > 33){
			
			graphCanvas = surfaceViewHolder.lockCanvas();
			if(graphCanvas!=null)
			{
				drawGraph();
				surfaceViewHolder.unlockCanvasAndPost(graphCanvas);
			}
			time = SystemClock.currentThreadTimeMillis();
		}
		else
		{
			Log.d("bla","early draw call");
		}
	}
	
	public void drawGraph()
	{
		graphCanvas.drawColor(Color.WHITE);
		graphCanvas.drawLine(20f, 0f, 20f, (float) graphCanvas.getHeight(),graphPaint);
		graphCanvas.drawLine(20f, (float) graphCanvas.getHeight()-20, (float) graphCanvas.getWidth(), (float) graphCanvas.getHeight()-20,graphPaint);
	}
	
	
	@Override
	protected void onPause() {
		mySensorManager.unregisterListener(mySensorListener);
		super.onPause();
	}

	@Override
	protected void onStop() {
		mySensorManager.unregisterListener(mySensorListener);
		super.onStop();
	}

}
