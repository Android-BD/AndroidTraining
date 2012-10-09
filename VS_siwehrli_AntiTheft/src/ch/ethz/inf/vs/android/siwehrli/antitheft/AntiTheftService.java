package ch.ethz.inf.vs.android.siwehrli.antitheft;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class AntiTheftService extends Service {
	public static final int NOTIFICATION_ACTIVATED_ID = 1;

	private boolean activate = MainActivity.ACTIVATE_DEFAULT;
	private int sensitivity = MainActivity.SENSITIVITY_DEFAULT;
	private int timeout = MainActivity.TIMEOUT_DEFAULT; // in seconds!

	private SensorEventListener listener;
	private SensorManager sensorManager;
	private Sensor sensor;
	private BroadcastReceiver receiver;

	private int unsigCounter = 0;
	private static final int UNSIG_COUNTER_THRESHHOLD = 10;
	private static final double CHANGE_100_PERCENT = 4; // defined by
														// examination of
														// average sensor
														// values
	private static final long ACCIDENTIAL_MOVEMENT_MAX_TIME = 5000; // arbitrarily
																	// defined
																	// by
																	// exercise
																	// sheet
	private long lastUnsignificantSensorChange = 0;
	private long lastCheckpoint = 0;
	private float[] lastValues = { 0, 0, 0 };
	
	private boolean timeoutStarted = false;
	private long timeoutStartTime;

	@Override
	public void onCreate() {
		// initialize sensor
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getSensorList(
				Sensor.TYPE_ACCELEROMETER).get(0);

		listener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {

			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				long now = System.currentTimeMillis();

				// calculate the norm of the relative change of 3 component
				// sensor data
				float[] values = event.values;
				double change = Math.pow(values[0] - lastValues[0], 2)
						+ Math.pow(values[1] - lastValues[1], 2)
						+ Math.pow(values[2] - lastValues[2], 2);
				change = Math.sqrt(change);

				// check if change is not significant (dependent on sensitivity
				// settings)
				boolean sig = true;
				if (change < (double) (100 - sensitivity) / 100d
						* CHANGE_100_PERCENT) { // change is not significant
					lastUnsignificantSensorChange = now;
					sig = false;

					if (unsigCounter < UNSIG_COUNTER_THRESHHOLD) {
						unsigCounter++;
					} else {
						lastCheckpoint = lastUnsignificantSensorChange;
						unsigCounter = 0;
					}
				}

				// check if movement is more then an accidential movement (more
				// than 5 seconds movement)
				if (Math.abs(lastCheckpoint - now) > ACCIDENTIAL_MOVEMENT_MAX_TIME) {
					Log.d("AntiTheftService", "START TIMEOUT because delta = "
							+ Math.abs(lastCheckpoint - now)+" ------------------------------------------");
					startTimeout(now);
				}
				
				checkTimeout(now);

				lastValues = values;
				Log.d("AntiTheftService", change + "   Significant: " + sig);

			}
		};
		
		receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				//sensorManager.unregisterListener(listener);
				sensorManager.registerListener(listener, sensor,
						SensorManager.SENSOR_DELAY_NORMAL);
			}

		};

		// register broadcastreceiver for screen changes
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(receiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.activate = intent.getBooleanExtra(
				"ch.ethz.inf.vs.android.siwehrli.antitheft.activate",
				MainActivity.ACTIVATE_DEFAULT);
		if (activate) {
			// start sensor
			sensorManager.registerListener(listener, sensor,
					SensorManager.SENSOR_DELAY_NORMAL);

			// show notification
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			int icon = R.drawable.notification_icon;
			CharSequence tickerText = getResources().getString(
					R.string.ticker_text);
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_NO_CLEAR
					| Notification.FLAG_ONGOING_EVENT;

			Context context = getApplicationContext();
			CharSequence contentTitle = getResources().getString(
					R.string.ticker_text);
			CharSequence contentText = getResources().getString(
					R.string.notification_comment);
			Intent notificationIntent = new Intent(this, MainActivity.class);
			notificationIntent.putExtra("activate", false);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText,
					contentIntent);

			mNotificationManager
					.notify(NOTIFICATION_ACTIVATED_ID, notification);

			// read configuration options out of intent
			this.sensitivity = intent.getIntExtra(
					"ch.ethz.inf.vs.android.siwehrli.antitheft.sensitivity",
					MainActivity.SENSITIVITY_DEFAULT);
			this.timeout = intent.getIntExtra(
					"ch.ethz.inf.vs.android.siwehrli.antitheft.timeout",
					MainActivity.TIMEOUT_DEFAULT);

			// save current time for reference
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		sensorManager.unregisterListener(listener);
	}
	
	private void startTimeout(long now)
	{
		this.timeoutStarted=true;
		this.timeoutStartTime=now;
	}
	
	private void startAlarm()
	{
		
	}
	
	private void checkTimeout(long now)
	{
		if(Math.abs(now-this.timeoutStartTime)>this.timeout)
		{
			this.startAlarm();
		}
	}
}
