package ch.ethz.inf.vs.android.siwehrli.sensors;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

public class ActuatorsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actuators);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_actuators, menu);
        return true;
    }
    
    //gets executed when the play sound button is clicked
    public void onPlaySoundButtonClick (View v){
    	//use media player to play the sound
    	MediaPlayer mp = MediaPlayer.create(this, R.raw.sentry_mode);
    	mp.setVolume(1.0f, 1.0f);
    	mp.start();
    }
    
    //gets executed when the vibrate button is pressed
    public void onVibrateButtonClick (View v){
    	SeekBar seeker = (SeekBar) findViewById(R.id.vibrateLengthBar);
    	int duration = seeker.getProgress(); //saves current seeker position into duration
    	
    	Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    	long[] pattern = { 0, duration }; //0 idle time, duration long vibration
    	vib.vibrate(pattern, -1);//vibrate
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
