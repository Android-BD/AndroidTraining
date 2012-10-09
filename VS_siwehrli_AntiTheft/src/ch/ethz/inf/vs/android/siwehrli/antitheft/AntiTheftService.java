package ch.ethz.inf.vs.android.siwehrli.antitheft;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
	private int timeout = MainActivity.TIMEOUT_DEFAULT;

	private SensorEventListener listener;
	private SensorManager sensorManager;
	
	private long lastSignificantSensorChange=-1;
	private float[] lastValues;
	
	@Override
	public void onCreate() {
		listener = new SensorEventListener() {

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				float[] values = event.values;
				Log.d("Service", ""+values[0]);
			}
		};

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		Sensor sensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
				.get(0);
		sensorManager.registerListener(listener, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.activate = intent.getBooleanExtra(
				"ch.ethz.inf.vs.android.siwehrli.antitheft.activate",
				MainActivity.ACTIVATE_DEFAULT);
		if (activate) {
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
}
