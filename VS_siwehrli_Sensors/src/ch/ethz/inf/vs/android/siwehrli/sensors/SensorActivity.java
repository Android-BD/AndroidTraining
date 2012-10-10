package ch.ethz.inf.vs.android.siwehrli.sensors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class SensorActivity extends Activity {
	
	//sensor stuff
	private Sensor selectedSensor;
	private SensorManager mySensorManager;
	private SensorEventListener mySensorListener;
	
	//other gui elements
	private TextView sensorName;
	private TextView value0Box;
	private TextView value1Box;
	private TextView value2Box;

	// graph stuff
	private SurfaceView graphSurface;
	private SurfaceHolder surfaceViewHolder;
	private Canvas graphCanvas;
	private Paint graphPaint;
	private Paint graphGridPaint;
	private Paint graphGridLightPaint;
	private float max;
	private float divider;
	private static float min_max = 10f;
	private float scaleValue;
	private float yZero;
	private float value;
	private static float leftBorder = 60f;
	private LinkedList<Float> graphYValues;
	private DrawThread drawThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensor);

		mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = mySensorManager
				.getSensorList(Sensor.TYPE_ALL);

		mySensorListener = new SensorEventListener() {

			public void onSensorChanged(SensorEvent event) {
				//write name and sensor values in the textboxes
				sensorName.setText(selectedSensor.getName());
				value0Box.setText(Float.toString(event.values[0]));
				value1Box.setText(Float.toString(event.values[1]));
				value2Box.setText(Float.toString(event.values[2]));

				// take norm of values vector and calculate average sensor value
				value = ((divider - 1) / divider * value)
						+ (FloatMath.sqrt(event.values[0] * event.values[0]
								+ event.values[1] * event.values[1]
								+ event.values[2] * event.values[2]) / divider);
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				selectedSensor = sensor;
				sensorName.setText(selectedSensor.getName());
			}
		};

		// get GUI elements
		sensorName = (TextView) findViewById(R.id.sensorName);
		value0Box = (TextView) findViewById(R.id.value0Box);
		value1Box = (TextView) findViewById(R.id.value1Box);
		value2Box = (TextView) findViewById(R.id.value2Box);

		// graph Stuff
		graphSurface = (SurfaceView) findViewById(R.id.graphSurface);
		surfaceViewHolder = graphSurface.getHolder();

		graphPaint = new Paint();
		graphPaint.setColor(Color.BLUE);
		graphPaint.setStrokeWidth(3);
		graphGridPaint = new Paint(graphPaint);
		graphGridPaint.setColor(Color.BLACK);
		graphGridPaint.setTextSize(20f);
		graphGridLightPaint = new Paint(graphGridPaint);
		graphGridLightPaint.setStrokeWidth(1f);

		max = min_max;
		divider = 1;
		scaleValue = 0f;
		graphYValues = new LinkedList<Float>();

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
		//register listener again and restart drawing thread
		mySensorManager.registerListener(mySensorListener, selectedSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
		drawThread = new DrawThread();
		drawThread.start();
	}

	// Draw Logic
	public void drawCanvas() {
		//get canvas to draw on
		graphCanvas = surfaceViewHolder.lockCanvas();
		if (graphCanvas != null) {
			// add value to be drawn
			graphYValues.addFirst(value);
			// reset divider --> new value
			divider = 1;

			// remove old points
			if (graphYValues.size() > (graphCanvas.getWidth() - leftBorder) / 2) {
				graphYValues.removeLast();
			}

			drawGraph();

			// force canvas to be drawn
			surfaceViewHolder.unlockCanvasAndPost(graphCanvas);

		}
	}

	public void drawGraph() {
		//graph plotting logic
		
		
		
		yZero = (float) graphCanvas.getHeight() - 20;

		// draw graph grid
		// set background
		graphCanvas.drawColor(Color.WHITE);

		// draw vertical line
		graphCanvas.drawLine(leftBorder, 0f, leftBorder,
				(float) graphCanvas.getHeight(), graphGridPaint);

		// draw base horizontal line
		graphCanvas.drawLine(leftBorder, yZero, (float) graphCanvas.getWidth(),
				yZero, graphGridPaint);
		// add number to line
		graphCanvas.drawText("0", 15f, (float) graphCanvas.getHeight() - 12,
				graphGridPaint);

		// draw top scale horizontal line
		float top_scale = Math.round((max * 0.95f) / 10) * 10;
		if (top_scale < 10) {
			top_scale = 10f;
		}
		graphCanvas.drawLine(leftBorder, yZero - (top_scale * scaleValue),
				(float) graphCanvas.getWidth(), yZero
						- (top_scale * scaleValue), graphGridLightPaint);

		// add number to line
		graphCanvas.drawText("" + top_scale, 1f, yZero
				- (top_scale * scaleValue - 20), graphGridPaint);

		// draw mid scale horizontal line
		float mid_scale = top_scale / 2;
		graphCanvas.drawLine(leftBorder, yZero - (mid_scale * scaleValue),
				(float) graphCanvas.getWidth(), yZero
						- (mid_scale * scaleValue), graphGridLightPaint);

		// add number to line
		graphCanvas.drawText("" + mid_scale, 1f, yZero
				- (mid_scale * scaleValue - 10), graphGridPaint);

		// draw bot scale horizontal line
		float bot_scale = top_scale / 4;
		graphCanvas.drawLine(leftBorder, yZero - (bot_scale * scaleValue),
				(float) graphCanvas.getWidth(), yZero
						- (bot_scale * scaleValue), graphGridLightPaint);

		// add number to line
		graphCanvas.drawText("" + bot_scale, 1f, yZero
				- (bot_scale * scaleValue - 10), graphGridPaint);

		// draw upper scale horizontal line
		float upper_scale = 3 * top_scale / 4;
		graphCanvas.drawLine(leftBorder, yZero - (upper_scale * scaleValue),
				(float) graphCanvas.getWidth(), yZero
						- (upper_scale * scaleValue), graphGridLightPaint);

		// add number to line
		graphCanvas.drawText("" + upper_scale, 1f, yZero
				- (upper_scale * scaleValue - 10), graphGridPaint);

		// draw top horizontal line
		graphCanvas.drawLine(leftBorder, 1f, (float) graphCanvas.getWidth(),
				1f, graphGridPaint);
		// add number to line
		graphCanvas.drawText("Max: " + max, leftBorder + 2f, 22f,
				graphGridPaint);

		Iterator<Float> valuesIterator = graphYValues.iterator();

		// fill values array, find max
		float[] values = new float[2 * graphYValues.size()];
		int i = 0;
		
		//reset maximum
		max = min_max;

		float tmp;
		// prepare values array and find next max
		while (valuesIterator.hasNext()) {
			tmp = valuesIterator.next();
			values[i] = graphCanvas.getWidth() - i;
			values[i + 1] = yZero - (tmp * scaleValue);
			if (tmp > max) {
				max = tmp;
			}
			i = i + 2;
		}

		// determine next scaleValue
		float drawHeigth = (float) graphCanvas.getHeight() - 20;
		scaleValue = drawHeigth / max;

		// draw points
		graphCanvas.drawPoints(values, graphPaint);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//stop listening on sensor when paused and stop the drawing thread
		mySensorManager.unregisterListener(mySensorListener);
		drawThread.running = false;
	}

	@Override
	protected void onStop() {
		mySensorManager.unregisterListener(mySensorListener);
		super.onStop();
	}

	// used for drawing on the SurfaceView
	private class DrawThread extends Thread {

		public Boolean running;

		// used to fix to 30fps
		private long sleepTime;
		private long delay = 33;
		private long beforeRender;

		public DrawThread() {
			running = false;
		}

		@Override
		public void run() {
			running = true;
			while (running) {
				beforeRender = SystemClock.currentThreadTimeMillis();
				Log.d("bla",""+SystemClock.currentThreadTimeMillis());
				drawCanvas();
				
				sleepTime = delay
						- (SystemClock.currentThreadTimeMillis() - beforeRender);

				try {
					// sleep until next frame
					if (sleepTime > 0) {
						Log.d("bla","sleeping for "+sleepTime);

						Thread.sleep(sleepTime);
					}
				} catch (InterruptedException ex) {
				}
			}
		}
	}
}
