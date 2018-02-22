package com.authorwjf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import com.authorwjf.R;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.*;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.compatible.*;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
//import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AnalogClock;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.os.Environment;
import android.provider.ContactsContract;

public class Main extends Activity implements SensorEventListener {
	
	TextToSpeech ttobj;
	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized; //variable to seenif sensor is is initialized
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private final float NOISE = (float) 0.5;
    private DebugServer server;
    private boolean fallen = false;
    private boolean fallen2 = false;
    double twosigma = 3, sigma=1.5,th=9.81,th1=6,th2=6,th3=24;
	public static String curr_state,prev_state;
    
	static int BUFF_SIZE=60;
	static public double[] window = new double[BUFF_SIZE];
	static public double[] rawwindow = new double[BUFF_SIZE];
	public float a_norm;
	public float b_norm;
	//public MediaPlayer m1_jump,m1_fall,m2_sit,m3_stand,m4_walk,m5_run,m6_small;
	protected PowerManager.WakeLock wl;
	
	// Get instance of Vibrator from current Context
	Vibrator v;

	
	
	String current_ip;
	private boolean isServer = false;
	private CheckBox serverChkBx;
	private CheckBox muteChkBx;
	private EditText ipTextField;
	private EditText devnameTextField;
	private EditText phonenumTextField;
	private SeekBar fallGravityLimit;
	private SeekBar runThreshold;
	private SeekBar walkThreshold;
	private ToggleButton runstopbutton;
	float vol = 0;
	private double accelerometerForceLimit = 32;
	SmsManager manager = SmsManager.getDefault();

	private int fall_timer_delay;
	private String devicename = Build.MANUFACTURER+"_"+Build.MODEL+"_";
	String old_url = "http://tolgazeybek.ddns.net:8083";
	
	private int speech_response_status = 0;
	private boolean isSpeechRecognitionRequested_for_fall = false;
	
	 Timer fall_timer;
	 
	 Timer speech_response_timer;
	 
	 Timer call_help_timer;
	 
	 
	 boolean fall_timer_expired = false;
	 boolean fall_timer_scheduled = false;
	
	 long fall_timer_rate= 4500; 
	 int computation_delay_constant = 8;
	 
	 private SpeechRecognizer sr;
	 GraphViewSeries xSeries;
	 GraphViewSeries ySeries;
	 GraphViewSeries zSeries;
	 GraphViewSeries vectorsumSeries;
	 GraphViewSeries wscSeries;
	 double graphviewCounter= 0;
	 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null); 
        c.moveToFirst();
        try{
           devicename = devicename.concat(c.getString(c.getColumnIndex("display_name")).replaceAll(" ", "_"));
           Log.w("devicename", c.getString(c.getColumnIndex("display_name")));
        }catch(Exception e)
        {
        	
        	Random r = new Random();
        	devicename=devicename.concat( Integer.toString(r.nextInt(899)+100));   
            e.printStackTrace();
        }
        c.close();
        
        sr = SpeechRecognizer.createSpeechRecognizer(this);       
        sr.setRecognitionListener(new listener());  
        
        fall_timer = new Timer();
        speech_response_timer = new Timer();
        call_help_timer = new Timer();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // init example series data
    	 xSeries = new GraphViewSeries("X-axis", new GraphViewSeriesStyle(Color.rgb(50, 255, 50), 1), new GraphView.GraphViewData[] {
    	    new GraphView.GraphViewData(0, 0.0d)
    	   
    	});
    	//xSeries..getStyle().
    	 ySeries = new GraphViewSeries("Y-axis", new GraphViewSeriesStyle(Color.rgb(255, 50 , 0), 1), new GraphView.GraphViewData[] {
    	    new GraphView.GraphViewData(0, 0.0d)
     	   
    	});
    	 zSeries = new GraphViewSeries("Z-axis", new GraphViewSeriesStyle(Color.rgb(0, 50, 255), 1), new GraphView.GraphViewData[] {
    	    new GraphView.GraphViewData(0, 0.0d)
     	   
    	});
    	vectorsumSeries = new GraphViewSeries("Vector sum", new GraphViewSeriesStyle(Color.rgb(255, 255, 255), 3), new GraphView.GraphViewData[] {
    	    new GraphView.GraphViewData(0, 0.0d)
     	   
    	});
    	 
    	wscSeries = new GraphViewSeries("WSC", new GraphViewSeriesStyle(Color.rgb(150, 100, 0), 2), new GraphView.GraphViewData[] {
    	    new GraphView.GraphViewData(0, 0.0d)
     	   
    	});
    	 
    	
    	GraphView graphView = new LineGraphView(
    			getApplicationContext()// context
    	    , "Realtime Accelerometer Values" // heading
    	);
    	graphView.addSeries(wscSeries); // data
    	graphView.addSeries(vectorsumSeries); // data
    	graphView.addSeries(xSeries); // data
    	graphView.addSeries(ySeries); // data
    	graphView.addSeries(zSeries); // data
    	
    	graphView.setShowLegend(true);
    	graphView.setLegendWidth(65);
    	
        graphView.forceLayout();
        graphView.getGraphViewStyle().setTextSize(6);
        graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.rgb(200, 100, 0));
        graphView.getGraphViewStyle().setVerticalLabelsColor(Color.rgb(230, 130, 0));
        graphView.getGraphViewStyle().setNumHorizontalLabels(7);
        graphView.getGraphViewStyle();
     // set view port, start=2, size=40
        graphView.setViewPort(0, 0.150);
        graphView.setScrollable(true);
        // optional - activate scaling / zooming
        graphView.setScalable(true);
        LinearLayout linelay = (LinearLayout) findViewById(R.id.linelay);
    	linelay.addView(graphView);
        
        //manufacturer specific constants
        if(Build.MODEL.startsWith("A0001"))  //OnePlus A0001 is a very fast device
        {
        	 fall_timer_rate= 6000; 
        	 computation_delay_constant = 24;
        	 accelerometerForceLimit = 32;
        	 linelay.setMinimumHeight(248);
        	
        }else if(Build.MODEL.startsWith("XT1045"))  //MotoG
        {
             fall_timer_rate= 6000; 
        	 computation_delay_constant = 24;
        	 accelerometerForceLimit = 41;
        	 linelay.setMinimumHeight(190);
        }else{
        	
        	fall_timer_rate= 6000; 
       	    computation_delay_constant = 24;
        }
        
        ttobj=new TextToSpeech(getApplicationContext(), 
        	      new TextToSpeech.OnInitListener() {
        	      @Override
        	      public void onInit(int status) {
        	         if(status != TextToSpeech.ERROR){
        	             ttobj.setLanguage(Locale.ENGLISH);
        	            }				
        	         }
        	      });
       // ttobj.setPitch(1.4f);
        ipTextField = (EditText) findViewById(R.id.editText1);
    //    ipTextField.setText("http://localhost:8080");
        ipTextField.setText(old_url);
        
        devnameTextField = (EditText) findViewById(R.id.editText2);
        devnameTextField.setText(devicename);
        phonenumTextField = (EditText) findViewById(R.id.editText3);
        phonenumTextField.setText(null);
        runstopbutton = (ToggleButton) findViewById(R.id.toggleButton1);
        server = new DebugServer(this);
        
        //final Context a = this;
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        current_ip = ipTextField.getText().toString(); 
        devicename = devnameTextField.getText().toString();
        
        runThreshold = ( SeekBar ) findViewById( R.id.seekBar2);
        //Note: Run threshold(th3) is relative to the walk threshold (th2)

        runThreshold.setProgress((int) (runThreshold.getMax()-(th3-th2)));
        runThreshold.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
  		  
    		  @Override
    		  public void onProgressChanged(SeekBar runThreshold, int progresValue, boolean fromUser) {
    			th3 = (runThreshold.getMax()-progresValue) + th2;
              Log.w("Run threshold: ", Double.toString(th3));  			
    		  }

  		@Override
  		public void onStartTrackingTouch(SeekBar seekBar) {
  			// TODO Auto-generated method stub
  			
  		}

  		@Override
  		public void onStopTrackingTouch(SeekBar seekBar) {
  			// TODO Auto-generated method stub
  			
  		}
    		 
    	   });
        
        walkThreshold = ( SeekBar ) findViewById( R.id.seekBar3);
        walkThreshold.setProgress((int)(walkThreshold.getMax()- th2));
        walkThreshold.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
  		  
    		  @Override
    		  public void onProgressChanged(SeekBar walkThreshold, int progresValue, boolean fromUser) {
    			th2 = walkThreshold.getMax()-progresValue;
              Log.w("Walk threshold: ", Double.toString(th2));  			
    		  }

  		@Override
  		public void onStartTrackingTouch(SeekBar seekBar) {
  			// TODO Auto-generated method stub
  			
  		}

  		@Override
  		public void onStopTrackingTouch(SeekBar seekBar) {
  			// TODO Auto-generated method stub
  			
  		}
    		 
    	   });
        
        fallGravityLimit =  ( SeekBar ) findViewById( R.id.seekBar1 );
        fallGravityLimit.setProgress((int)(fallGravityLimit.getMax()- accelerometerForceLimit));
        fallGravityLimit.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		  
  		  @Override
  		  public void onProgressChanged(SeekBar fallGravityLimit, int progresValue, boolean fromUser) {
  			accelerometerForceLimit = fallGravityLimit.getMax()-progresValue;
            Log.w("acc_force_lim", Double.toString(accelerometerForceLimit));  			
  		  }

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
  		 
  	   });
        
        
        serverChkBx = ( CheckBox ) findViewById( R.id.checkBox1 );
        serverChkBx.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                      //is chkbox checked?
              if (((CheckBox) v).isChecked()) {
                               //I am a server
            	  isServer = true;
            	  old_url = ipTextField.getText().toString();
            	  ipTextField.setText("http://localhost:8080");
            	  ipTextField.setEnabled(false);
            	  //TODO: buraya debugserver initialization kodu koy, sonra da ip addresini text fielddan al, eger serversan ignore edip localhost:8080ye cevir burada
            	  //sonra da http client exampleiyle POST requestlerini dose. Hadi masallah
              }
              else
              {//I am a humble client
                  //case 2
            	  isServer = false;
            	  ipTextField.setText(old_url);
                  ipTextField.setEnabled(true);
              }
            }
          });
        
        muteChkBx = ( CheckBox ) findViewById( R.id.muteCheckBox );
        muteChkBx.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                      //is chkbox checked?
              if (((CheckBox) v).isChecked()) {                              
                  vol = 0.4F;
              }
              else
              {
            	  vol = 0;
              }
   /*           m1_jump.setVolume(vol, vol);
              m1_fall.setVolume(vol, vol);
              m2_sit.setVolume(vol, vol);
              m3_stand.setVolume(vol, vol);
              m4_walk.setVolume(vol, vol);
              m5_run.setVolume(vol, vol);
              m6_small.setVolume(vol/2, vol/2);
     */       }
          });
        
        
        runstopbutton.setOnClickListener(new OnClickListener() {
        	 
    		@Override
    		public void onClick(View v) {
                
    			
    		   if(runstopbutton.getText().toString().equalsIgnoreCase("STOP")) //User wants to start the functionality   
    		   {
    			   if(isServer)
    			   {
    			   server = new DebugServer(Main.this);
	    			   try{
	    				   
	    				   try {
	    			            server.start();
	    			        } catch(IOException ioe) {
	    			            Log.w("Httpd", "The server could not start.");
	    			            ttobj.speak("I can't start motion detection due to an I-O error.", TextToSpeech.QUEUE_FLUSH, null);
	    			        }
	    			        Log.w("Httpd", "Web server initialized.");
	    			        ttobj.speak("Using local webserver. . . .", TextToSpeech.QUEUE_FLUSH, null);
	    			        
	    			        
	    			   
	    			       
	    			   }catch(Exception e)
	    			   {
	    				   if(wl.isHeld())wl.release();
	    				   
	    			       if (server != null)
	    			       server.stop();  
	    				   runstopbutton.toggle();
	    				   serverChkBx.setEnabled(true);
	        			   ipTextField.setEnabled(true);
	        			   devnameTextField.setEnabled(true);
	        			   ttobj.speak("I can't start motion detection due to an error.", TextToSpeech.QUEUE_FLUSH, null);
	    				   // mSensorManager.unregisterListener(Main.this);
	    			   }
    			   }else{
    				   ttobj.speak("Using remote webserver. . . .", TextToSpeech.QUEUE_FLUSH, null);  
    			   }
    			   wl.acquire();
    			   mSensorManager.registerListener(Main.this, mAccelerometer , SensorManager.SENSOR_DELAY_GAME); 
    			   current_ip = ipTextField.getText().toString();
    			   devicename = devnameTextField.getText().toString();
    			   serverChkBx.setEnabled(false);
    			   ipTextField.setEnabled(false);
    			   devnameTextField.setEnabled(false);
    			   HttpClient client = new DefaultHttpClient();
    			   HttpPost post = new HttpPost(current_ip ); //just for now
    			 //  ttobj.setSpeechRate(0.95F);
    			   
    			   ttobj.speak("Motion detection process started,,.", TextToSpeech.QUEUE_ADD, null);
                       			  
			       ttobj.speak("Please respond with yes or no  to my future questions.", TextToSpeech.QUEUE_ADD, null);
			       
			  
			       
			     //  ttobj.setSpeechRate(1.05F);
			       call_help_timer.schedule(new TimerTask() {
      			     
     		            @Override
     		            public void run() {
     		                // TODO Auto-generated method stub
     		                runOnUiThread(new Runnable() {
     		                    public void run() {
     		                    	ttobj.speak(" ", TextToSpeech.QUEUE_FLUSH, null);
     		                    	prev_state = "unknown";
     		                    
     		                    }
     		                });

     		            }
     		        }, 7450); // 1000 means start from 1 sec
    			    
			       
			        
			       
    			   
    		   }
    		   else if(runstopbutton.getText().toString().equalsIgnoreCase("RUN")) //User wants to stop the functionality
    		   {
    			   ttobj.speak("Thank you, for choosing me as your preferred Motion Detection Application .", TextToSpeech.QUEUE_FLUSH, null);
    			//   ttobj.speak("I always take your motion detection business very seriously. Now, I can relax... -Well, Until you come back for me. I already missed you.", TextToSpeech.QUEUE_ADD, null);
    			 //  ttobj.setPitch(1.9f);
    			   ttobj.speak("Created by Tolga Zaybeck.", TextToSpeech.QUEUE_ADD, null);
     			  
    			   if(wl.isHeld()) wl.release();
    			   mSensorManager.unregisterListener(Main.this);
			       if (server != null) server.stop();
			       serverChkBx.setEnabled(true);
    			   ipTextField.setEnabled(true);
    			   devnameTextField.setEnabled(true);
    		   }
    		}
     
    	});
        
        
        
        
        
        
        prev_state="none";
		curr_state="none";


      
        mInitialized = false;
        
   /*     m1_jump=MediaPlayer.create(getBaseContext(), R.raw.jump);
        m1_fall=MediaPlayer.create(getBaseContext(), R.raw.fall2);
        m2_sit=MediaPlayer.create(getBaseContext(), R.raw.sitting2);
        m3_stand=MediaPlayer.create(getBaseContext(), R.raw.standing2);
        m4_walk=MediaPlayer.create(getBaseContext(), R.raw.walking2);
        m5_run=MediaPlayer.create(getBaseContext(), R.raw.running);
        m6_small=MediaPlayer.create(getBaseContext(), R.raw.small);
        m1_jump.setVolume(vol, vol);
        m1_fall.setVolume(vol, vol);
        m2_sit.setVolume(vol, vol);
        m3_stand.setVolume(vol, vol);
        m4_walk.setVolume(vol, vol);
        m5_run.setVolume(vol, vol);
        m6_small.setVolume(vol, vol);*/
      //as http client
      //  HttpClient client = new DefaultHttpClient();
      //  HttpPost post = new HttpPost("www.google.com"); //just for now
        
      //as http server using nanohttpd
    
    }
    
 // DON'T FORGET to stop the server
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        ttobj.shutdown();
    	if(wl.isHeld())wl.release();
        mSensorManager.unregisterListener(this);
        if (server != null)
            server.stop();
    }

    @Override
	protected void onResume() {
        super.onResume();
      //  mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
	protected void onPause() {
        super.onPause();
       // mSensorManager.unregisterListener(this);
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// can be safely ignored for this demo
	}
	

	
	
	private void AddData(float ax, float ay, float az, float bx, float by, float bz) {
		// TODO Auto-generated method stub
		 a_norm=(float) Math.sqrt(ax*ax+ay*ay+az*az);
		 b_norm=(float) Math.sqrt(bx*bx+by*by+bz*bz);
		 for(int i=0;i<=BUFF_SIZE-2;i++){
	    	window[i]=window[i+1];
	    	rawwindow[i]=rawwindow[i+1];
	     }
	     window[BUFF_SIZE-1]=a_norm;
	     rawwindow[BUFF_SIZE-1]=b_norm;
	     graphviewCounter+= 0.001;
	     xSeries.appendData(new GraphView.GraphViewData(graphviewCounter, (double)bx), true, 150);
	     ySeries.appendData(new GraphView.GraphViewData(graphviewCounter, (double)by), true, 150);
	     zSeries.appendData(new GraphView.GraphViewData(graphviewCounter, (double)bz), true, 150);
	     vectorsumSeries.appendData(new GraphView.GraphViewData(graphviewCounter, (double)b_norm), true, 150);
	     
	}
     
	
	//WSC: Weighted Step Count
	private int compute_wsc(double[] window2) {
		// TODO Auto-generated method stub
		int count=0;
		for(int i=1;i<=BUFF_SIZE-1;i++){
		
			
			
			
			if(window2[i]>15 && window2[i-1]<= 15)
		       count+=4;
			else if(window2[i]>13 && window2[i-1]<= 13)
			       count+=1;
			else if(window2[i]>24 && window2[i-1]<= 24)
			       count+=2;
		//This is the original logic	
		/*	if((window2[i]-th)<sigma && (window2[i-1]-th)>sigma){
				count=count+2;
			}
			
			if((window2[i]-th)<twosigma && (window2[i-1]-th)>twosigma){
				count=count+3;
			}*/
			
		}
		return count;
	}
	
	
	private double potential_fall(double[] window2)
	{
		double min = 20;
		double max = 0;
		int min_loc = 0;
		int max_loc = 0;
		for(int i=1;i<=BUFF_SIZE-1;i++){
			
			if(window2[i] > max) 
				{
				  max = window2[i];
				}
			if(window2[i] < min)
				{min = window2[i];	
				  min_loc = i;
				}
			
		}
		return max-min;
	}
	
	
	private double potential_rawfall(double[] window2)
	{
		double min = 20;
		double max = 0;
		double gravity =9.81;
		
		int min_loc = 0;
		int max_loc = 0;
		int grav_stat = 0;
		for(int i=1;i<=BUFF_SIZE-1;i++){
			
			if((window2[i] +4) < gravity) 
				{
				  grav_stat = 1;
				}
			if(grav_stat == 1)
			{
				if((window2[i] + 3 ) > accelerometerForceLimit) 
				  grav_stat = 2;
			}
			if(grav_stat == 2)
			{
				if((window2[i] > gravity -2) || (window2[i] < gravity +2))
						grav_stat = 3;
			}
			
		}
		return grav_stat;
	}
	 
	private int walk_detect_num = 0;
	private double th1_bufzone = 1;
	String possible_state = "";
	private void posture_recognition(double[] window2,double ay2) {
		// TODO Auto-generated method stub
		int wsc=compute_wsc(window2);
		wscSeries.appendData(new GraphView.GraphViewData(graphviewCounter, (double)wsc), true, 150);
		if(wsc==0){
			walk_detect_num = 0;
			if(Math.abs(ay2)<th1){
				possible_state="sitting";
			}else if(Math.abs(ay2)>=th1){
				possible_state="standing";
			}
				
		}else{
			if(wsc>th3) possible_state="running";
		    else if(wsc>th2){
		    // 	walk_detect_num++;
		    // 	if(walk_detect_num > 1)//may be needed for now
			//	  {//double, triple, or quadruple check if walking
		    		possible_state="walking";
		    //		walk_detect_num = 0;
			//	  }
			}else{
				possible_state="small+motion";
				
			//TODO: As a trial basis
				//curr_state=prev_state;
				
			}
			
			
			
				
		}
			
		
		
	}
	

	private void SystemState(String curr_state1,String prev_state1) {
		// TODO Auto-generated method stub
	//	    if(prev_state1.equalsIgnoreCase("none")) prev_state1 = last_known_good_state;
	        	//Fall !!
	     	if(!prev_state1.equalsIgnoreCase(curr_state1)){
	     		  Log.e("Current_state",curr_state1);
            	  if(curr_state1.startsWith("fall")||curr_state1.startsWith("FALL")){
            		  sr.stopListening();
            		  
            		  if(curr_state1.contains("verif"))
            			  {ttobj.speak("You verified that you fell, do you need help?", TextToSpeech.QUEUE_FLUSH, null);
            			   
            			  curr_state1="FALL&s=FALL&s=Fall+verified+by+user.";
            			  
            			  }
            		  else ttobj.speak("I think you fell, do you need help?", TextToSpeech.QUEUE_FLUSH, null);
            		  call_help_timer.schedule(new TimerTask() {
            			     
      		            @Override
      		            public void run() {
      		                // TODO Auto-generated method stub
      		                runOnUiThread(new Runnable() {
      		                    public void run() {
      		                    	request_help();
      		                    }
      		                });

      		            }
      		        }, 1000); // 1000 means start from 1 sec
      		     
            	       
            		   String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		  fall_timer_delay = 500;
            	  }
            	  if(curr_state1.equalsIgnoreCase("jumping")){
            		 
            		  ttobj.speak("My bad! You actually jumped.", TextToSpeech.QUEUE_ADD, null);
            		  String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		  fall_timer_delay = 20;
            	  }
            	  else if(curr_state1.equalsIgnoreCase("sitting")){
            		  ttobj.speak("You are sitting.", TextToSpeech.QUEUE_ADD, null);
            		  String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		  fall_timer_delay = 1;
            	  }
            	  else if(curr_state1.equalsIgnoreCase("standing")){
            		 // m3_stand.start();
            		  ttobj.speak("You are standing.", TextToSpeech.QUEUE_ADD, null);
            		  String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		  fall_timer_delay = 1;
            	  }
            	  else if(curr_state1.equalsIgnoreCase("small+motion")){
            		  //m6_small.start();
            		  ttobj.speak("Small motion.", TextToSpeech.QUEUE_ADD, null);
            		  String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		  fall_timer_delay = 1;
            	  }
            	  
            	  else if(curr_state1.equalsIgnoreCase("walking")){
            		  //if(!prev_state1.equalsIgnoreCase("running"))
            		  //{
            		  //m4_walk.start();
            		  ttobj.speak("You are walking.", TextToSpeech.QUEUE_ADD, null);
            		  String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		    
            		  //}
            		  fall_timer_delay = 20;
            	  }
            	  else if(curr_state1.equalsIgnoreCase("running")){
            		 // m5_run.start();
            		  ttobj.speak("Hey, you are running!", TextToSpeech.QUEUE_ADD, null);
            		  String[] param = {current_ip,devicename,curr_state1};
            		  new SendHttpRequestTask().execute(param);
            		  fall_timer_delay = 20;
            	  }
             }	
	 	 
	 	
	}
	
	    private int fall_prob_timer = 0;
	    private int fall_prob = 0;
	    
		private int computation_delay = computation_delay_constant;
//	    private String last_known_good_state = "none";	
	@Override
	
	public void onSensorChanged(SensorEvent event) {
	  if(fall_timer_delay > 0) fall_timer_delay--;
	  
	
	//	ImageView iv = (ImageView)findViewById(R.id.image);
	//	AnalogClock clk = (AnalogClock)findViewById(R.id.analogClock1);
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
		
			mInitialized = true;
	//		clk.setVisibility(View.VISIBLE);
	//		clk.bringToFront();
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);
			if (deltaX < NOISE) deltaX = (float)0.0;
			if (deltaY < NOISE) deltaY = (float)0.0;
			if (deltaZ < NOISE) deltaZ = (float)0.0;
			AddData(deltaX,deltaY,deltaZ,x,y,z);
			mLastX = x;
			mLastY = y;
			mLastZ = z;
		
	//		clk.setVisibility(View.VISIBLE);
	//		clk.bringToFront();
			
			
	      computation_delay--;
			
		if(fall_timer_delay < 1 && computation_delay < 1  && !fall_timer_scheduled) 
		{		
			computation_delay = computation_delay_constant;
			//TODO: computation delay of 8 works fine for MotoG
			//       15 is fine for oneplusone 
			double maxaccelerometerforce = potential_fall(window);
			double rawfall = potential_rawfall(rawwindow);
			
			posture_recognition(rawwindow,y);
            
			int vibraduration = 0;
		    fall_prob = 0;
			if(maxaccelerometerforce > accelerometerForceLimit)
			{
				if(!fallen)
				{
					fall_prob+= 60;
					// Vibrate for 400 milliseconds
					vibraduration+=500;
					Log.e("fallrelated","BIGFALL");
					
				}
				fallen = true;
			}else
			{
				fallen = false;
				
			}
			
			if(rawfall > 2)
			{
				if(!fallen2)
				{
					fall_prob+=40;
					// Vibrate for 400 milliseconds
					vibraduration+=100;
					Log.e("fallrelated","SMALLFALL");
				}
				fallen2 = true;
			}else
			{
				fallen2 = false;
				
			}
			v.vibrate(vibraduration);
			if(fall_prob>0)
			{
				fall_prob_timer = fall_prob;				
			}
			
			
			if(fall_prob_timer > 59){ //make it >60 to use bigfall && smallfall together
		     fall_prob_timer = 0;
		     fall_timer_scheduled = true;
		     ttobj.speak("Oh! Did you fall?", TextToSpeech.QUEUE_FLUSH, null);
		     isSpeechRecognitionRequested_for_fall = recognizeSpeech();
		     fall_timer.schedule(new TimerTask() {
		     
		            @Override
		            public void run() {
		                // TODO Auto-generated method stub
		                runOnUiThread(new Runnable() {
		                    public void run() {
		                    	fall_timer_expired =true;
		                    	fall_timer_scheduled = false;
		                    }
		                });

		            }
		        }, fall_timer_rate); // 1000 means start from 1 sec
		     

		    }
			if(fall_timer_expired)//fall_prob_timer > 0)
			{ fall_timer_expired = false;
			    String user_fall_feedback = null;
			    if( isSpeechRecognitionRequested_for_fall)
			    {  
			    	isSpeechRecognitionRequested_for_fall = false;
			    	if(speech_response_status == 1){
			    		user_fall_feedback = "fall(verified+by+user)";
			    		
			    		
			    	}
			    	else if (speech_response_status == 2) {
			    		user_fall_feedback = "unknown";
			    		ttobj.speak("I apologize for the false alarm.", TextToSpeech.QUEUE_ADD, null);
			    	}
			    	
			    }
			    Log.w("Fallcheck", "fall check started"); 
			    Log.w("Fallcheck", possible_state); 
				if(possible_state.equalsIgnoreCase("walking")||possible_state.equalsIgnoreCase("running"))
				{
					fall_prob_timer += 7;
					ttobj.speak("You probably did not fall.", TextToSpeech.QUEUE_FLUSH, null);
				}else if(possible_state.equalsIgnoreCase("standing"))
				{
					possible_state = "jumping";
					fall_prob_timer = 1;
				}else if(possible_state.equalsIgnoreCase("sitting"))
				{
					possible_state = "FALL";
					fall_prob_timer = 1;
				}
				else if(possible_state.equalsIgnoreCase("small+motion"))
				{
					possible_state = "fall";
					fall_prob_timer = 1;
				}
				
				if(user_fall_feedback != null)
				{
				if(user_fall_feedback.startsWith("unknown"))
					{
						if(possible_state.toLowerCase().startsWith("fall"))
						{
							
						}else
						{user_fall_feedback = possible_state;
						}
					}
				possible_state = user_fall_feedback;
			    }
				if(fall_prob_timer < 8) fall_prob_timer=0;
				else fall_prob_timer-=8;
			}
			
		//	if(!prev_state.equalsIgnoreCase("none")) last_known_good_state = prev_state;
		//	if(!curr_state.equalsIgnoreCase("none")) last_known_good_state = curr_state;
			if(!fall_timer_scheduled){
				curr_state = possible_state;
				SystemState(curr_state,prev_state);
				 if(!prev_state.equalsIgnoreCase(curr_state)){
		            	prev_state=curr_state;
		            }
			}
	      } 
		
		
	
		}
	  
	}
	
	
	
	//----------------------***********************************************_________________________________________
	//http client stuff here:
	String sendHttpRequest(String url, String devicename, String status)
	{
		
		StringBuilder returnstring = new StringBuilder();
		
		StringBuilder design_url = new StringBuilder();
		
		try{
	    design_url.append(url).append("/?devicename=").append(devicename).append("&status=").append(status);
		HttpURLConnection con = (HttpURLConnection) ( new URL(design_url.toString())).openConnection();
		con.setRequestMethod("POST");
		con.setDoInput(true);
		con.setDoOutput(true);
		con.connect();
		InputStream is = con.getInputStream();
		byte[] b = new byte[2];
		while ( is.read(b) != -1)
		{
		  returnstring.append(new String(b));
		  Log.w("inputstream",returnstring.toString());
		}
		con.disconnect();
		}catch(Exception e)
		{;
		
		}
		return returnstring.toString();
	}
	
	//Speech to text recognition STUFF
	 public  boolean isConnected()
	    {
	    	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo net = cm.getActiveNetworkInfo();
		    if (net!=null && net.isAvailable() && net.isConnected()) {
		        return true;
		    } else {
		        return false;
		    }
	    }
	
	 
	 public boolean recognizeSpeech()
	 {
		 if(isConnected()){
			 speech_response_status = 0;
			// for(long j = 0; j < 3200000;j++)j+=(2-2);
			 while(ttobj.isSpeaking());
			 Log.w("Speechrecognition", "passed waiting stage");
        	 Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
         	 intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
         	 RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
         	 intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5); 
         	 speech_response_timer.schedule(new TimerTask() {

	            @Override
	            public void run() {
	                // TODO Auto-generated method stub
	                runOnUiThread(new Runnable() {
	                    public void run() {
	                    	sr.stopListening();
	                    	//ttobj.speak("Please answer yes or no.", TextToSpeech.QUEUE_ADD, null);
	                    }
	                });

	            }
	        }, 2400); // 1000 means start from 1 sec
         	 sr.startListening(intent);
         	// startActivityForResult(intent, 1234); //1234 is the request code
         	 return true;
        			 }
        	else{
        		ttobj.speak("Sorry, I can't listen to your answer without an Internet connection.", TextToSpeech.QUEUE_ADD, null);
        		return false;
        	} 
		 
	 }
	 
	 //inner speech listener class
	 class listener implements RecognitionListener          
	   {
	            public void onReadyForSpeech(Bundle params)
	            {
	            	
	                Log.d("Ready for speech", "onReadyForSpeech");
	            }
	            public void onBeginningOfSpeech()
	            {
	                     Log.d("speech", "onBeginningOfSpeech");
	            }
	            public void onRmsChanged(float rmsdB)
	            {
	                     Log.d("speech", "onRmsChanged");
	            }
	            public void onBufferReceived(byte[] buffer)
	            {
	                     Log.d("speech", "onBufferReceived");
	            }
	            public void onEndOfSpeech()
	            {
	                     Log.d("speech", "onEndofSpeech");
	            }
	            public void onError(int error)
	            {
	                     Log.d("speech",  "error " +  error);
	                    // speech_response_status = error +100;
	                     if(error == 6)
	                     {
	                    	 sr.stopListening();
	                    	 ttobj.speak("You did not answer.", TextToSpeech.QUEUE_ADD, null);
	                    	 
	                     }
	                     else{
	                    	// sr.stopListening();
	                    	// ttobj.speak("I couldn't catch what you said.", TextToSpeech.QUEUE_ADD, null);
	                     }
	            }
	            public void onResults(Bundle results)                   
	            {
	                     String str = new String();
	                     Log.d("speech", "onResults " + results);
	                     ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
	                     for (int i = 0; i < data.size(); i++)
	                     {
	                               Log.d("speech", "result " + data.get(i));
	                               str += data.get(i);
	                     }
	                     if(str.contains("yes"))
	                     {//assume user answered yes
	                    	 sr.stopListening();
	                    	 ttobj.speak("Alright.", TextToSpeech.QUEUE_ADD, null); 
	                    	 speech_response_status = 1;
	                     }else if(str.contains("no"))
	                     {//assume user answered no
	                    	 speech_response_status = 2;
	                    	 sr.stopListening();
	                    	 ttobj.speak("OK.", TextToSpeech.QUEUE_ADD, null);
	                    	 
	                     }else {
	                    	 sr.stopListening();
	                    	 ttobj.speak("Sorry, can't understand your response.", TextToSpeech.QUEUE_ADD, null);
	                    	 speech_response_status = 3;	 
	                     }
	                     
	                    // mText.setText("results: "+String.valueOf(data.size()));        
	            }
	            public void onPartialResults(Bundle partialResults)
	            {
	                     Log.d("speech", "onPartialResults");
	            }
	            public void onEvent(int eventType, Bundle params)
	            {
	                     Log.d("speech", "onEvent " + eventType);
	            }
	   }
	 
	 public void request_help()
	 {
		 
	
		  sr.stopListening();
		  Log.w("Calledspreg", "called speech reconition for HELP");
		  recognizeSpeech();
		  
		  call_help_timer.schedule(new TimerTask() {

	            @Override
	            public void run() {
	                // TODO Auto-generated method stub
	                runOnUiThread(new Runnable() {
	                    public void run() {
	                    	sr.stopListening();
	                    	if(speech_response_status == 1){
	  			    		
	  			    		
	  			    	    String phonenumber = phonenumTextField.getText().toString();
	  			    	    
	  			    	    ttobj.speak("You indicated that you want help. Texting to your emergency contact.", TextToSpeech.QUEUE_ADD, null);
          		     //   Log.w("phonenumber", phonenumber);
	  			    	  String[] param = {current_ip,devicename,"User+requested+help."};
	            		  new SendHttpRequestTask().execute(param);
	            		  
          		        if(phonenumber!=null){
          		    	 try{manager.sendTextMessage(phonenumber, null, "I fell down, please help." , null, null);}catch(Exception e){;}
          		         //TODO: This could be mapped to sent intents and received intents to verify your sms is sent properly.

          		        }
	  			    	}
	  			    	else if (speech_response_status == 2) {
	  			    		//user_fall_feedback = "fall(denied+by+user)";
	  			    		ttobj.speak("You don't want help. Fine.", TextToSpeech.QUEUE_ADD, null);
	  			    	}else{
	  			    		
	  			    	
	                    	
	                    	String phonenumber = phonenumTextField.getText().toString();
	  			    	    ttobj.speak("You did not answer. Texting your situation to the emergency contact.", TextToSpeech.QUEUE_ADD, null);
	  			    	  String[] param = {current_ip,devicename,"User+unresponsive,+may+need+help."};
	            		  new SendHttpRequestTask().execute(param);
          		      //  Log.w("phonenumber", phonenumber);
          		        if(phonenumber!=null){
          		    	 try{manager.sendTextMessage(phonenumber, null, "I may have fallen and unconscious right now, please reach me." , null, null);}catch(Exception e){;}
          		         //TODO: This could be mapped to sent intents and received intents to verify your sms is sent properly.
          		        }
          		      }
	                    }
	                });

	            }
	        }, 6500); // 1000 means start from 1 sec
	 }
	 
/*	 @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	     if (requestCode == 1234 && resultCode == RESULT_OK) {
	    // match_text_dialog = new Dialog(MainActivity.this);
	   //  match_text_dialog.setContentView(R.layout.dialog_matches_frag);
	   //  match_text_dialog.setTitle("Select Matching Text");
	   //  textlist = (ListView)match_text_dialog.findViewById(R.id.list);

	    	 ttobj.speak("You have said " +data.getDataString(), TextToSpeech.QUEUE_ADD, null);
	    	 Log.w("Userspeech", data.getDataString());
	    	// match_text_dialog.hide();
	        

	    // match_text_dialog.show();
	     }
	     super.onActivityResult(requestCode, resultCode, data);
	    }
	 */
	 
	 
	 
	 
	
	//inner class
	private class SendHttpRequestTask extends AsyncTask<String, Void, String>{
		   
		  @Override
		  protected String doInBackground(String... params) {
		   String url = params[0];
		   String name = params[1];
		   String status = params[2];
		   String data = sendHttpRequest(url, name, status);
		   return data;
		  }
		 
		  @Override
		  protected void onPostExecute(String result) {
		   ;
		  }
		}
	
	
	
}