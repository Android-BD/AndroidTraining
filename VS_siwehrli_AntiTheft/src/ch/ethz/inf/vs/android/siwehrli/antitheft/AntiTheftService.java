package ch.ethz.inf.vs.android.siwehrli.antitheft;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

public class AntiTheftService extends IntentService {
	public static final int NOTIFICATION_ACTIVATED_ID = 1;
		
	private boolean activate = MainActivity.ACTIVATE_DEFAULT;
	private int sensitivity =  MainActivity.SENSITIVITY_DEFAULT;
	private int timeout =  MainActivity.TIMEOUT_DEFAULT;

	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public AntiTheftService() {
		super("AntiTheftService");
	}

	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns,
	 * IntentService stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		this.activate = intent.getBooleanExtra(
				"ch.ethz.inf.vs.android.siwehrli.antitheft.activate", MainActivity.ACTIVATE_DEFAULT);
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
		}
	}
}
