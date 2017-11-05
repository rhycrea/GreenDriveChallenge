package com.example.aybars.gradleopenxc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.openxc.NoValueException;
import com.openxc.VehicleManager;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.FuelConsumed;
import com.openxc.measurements.FuelLevel;
import com.openxc.measurements.Odometer;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.measurements.VehicleSpeed;
import com.openxc.measurements.AcceleratorPedalPosition;
import com.openxc.measurements.TransmissionGearPosition;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ChallengeActivity extends AppCompatActivity {

    private static final String TAG = "StarterActivity";

    public static VehicleManager mVehicleManager;
    private TextView mEngineSpeedView;
    private TextView mVehicleSpeedView;
    private TextView mOdometerView;
    private TextView mFuelConsumedView;
    private TextView mFuelLevelView;
    private TextView mAcceleratorPedalPositionView;
    private TextView mTransmissionGearPositionView;

    private TextView mText1View;
    private TextView mText2View;
    private TextView mText3View;
    private TextView mScoreDetailsView;

    private TextView mPenaltySpeedView;
    private TextView mPenaltyBoostView;
    private TextView mPenaltyGearView;

    private double speed;
    private double rpm;
    private double odometer;
    private double fuelConsume;
    private double fuelLevel;
    private int pedal;
    private TransmissionGearPosition.GearPosition gear;


    private int countSpeedOverrunSeries;
    private int countPenaltySpeed;
    private int[] latest3Speeds = new int[3];
    private int countPenaltyBoost;
    private int pointPenaltyGear;
    private double odometerStart;
    private double odometerEnd;
    private double fuelConsumeStart;
    private double fuelConsumeEnd;

    private int totalTime;
    private int score;

    final static double SPEEDLIMIT = 40;
    final static double AVERAGE_FUEL_CONSUME = 0.05;
    final static double MIN_RUNTIME = 10;
    final static double OVERRUN_TIME = 30;

    public ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is
        // established, i.e. bound.
        public void onServiceConnected(ComponentName className, IBinder service) {

            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();

            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes
            startChallenge();
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }

    };
    Button buttonShare;
    private void finishChallenge() {
        odometerEnd = odometer;
        fuelConsumeEnd = fuelConsume;
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        int[] penaltyPoint = new int[4];
        score = 100;
        if(totalTime > MIN_RUNTIME) {
            //CASE1: SPEED LIMIT - half of the way above speed limit = -100
            if(countPenaltySpeed == 0 ) {
                penaltyPoint[0] = 0;
            }
            else {
                penaltyPoint[0] = (int) ((countPenaltySpeed/totalTime)*300);
            }
            //CASE2: SPEED BOOST
            if(countPenaltyBoost == 0 ) {
                penaltyPoint[1] = 0;
            }
            else {
                penaltyPoint[1] = countPenaltyBoost;
            }
            //CASE3: GEAR TIMING - below x = recommended value.
            //if driver shifts-up averagely maximum in x+1 speed, he has 0 penaltyi
            //but if he shifts-up averagely in x+4, he has -100 penalty.
            pointPenaltyGear = pointPenaltyGear/(totalTime);
            if(pointPenaltyGear <= 1 ) {
                penaltyPoint[2] = 0;
            }
            else {
                penaltyPoint[2] = pointPenaltyGear*4;
            }
            //CASE4: FUEL EFFICIENCY - wasting 5 times more than average consumption = -100
            double average_wasted_fuel = (fuelConsumeEnd - fuelConsumeStart) / (odometerEnd - odometerStart) - AVERAGE_FUEL_CONSUME;
            if(average_wasted_fuel <= 0) {
                penaltyPoint[3] = 0;
            }
            else {
                penaltyPoint[3] = (int) ((average_wasted_fuel)*50);
            }

            //CALCULATING THE LATEST.
            for(int i=0; i < 4; i++) {
                if(penaltyPoint[i] > 50) {penaltyPoint[i] = 40;}
                score -= penaltyPoint[i];
            }

            if(score > 95) {
                mText1View.setText("SCORE: " + score);
                mText1View.setTextColor();
                mText2View.setText("CONGRATULATIONS! You are a true hero!");
                mText3View.setText("");
            }
            else if(score > 90) {
                mText1View.setText("SCORE: " + score);
                mText2View.setText("GREAT!");
                mText3View.setText("");
            }
            else if(score > 75) {
                mText1View.setText("SCORE: " + score);
                mText2View.setText("GOOD!");
                mText3View.setText("");
            }
            else if(score > 60) {
                mText1View.setText("SCORE: " + score);
                mText2View.setText("Thats not bad, but you need to work harder for a better world.");
                mText3View.setText("Here some advices for you:");
            }
            else  {
                if(score<0) {score=0;}
                mText1View.setText("SCORE: " + score);
                mText2View.setText("Nobody wants to leave a poisoned world to their children.");
                mText3View.setText("Here some advices for you:");
            }
        }
        else {
            mText1View.setText("OOPS!");
            mText2View.setText("Car trips under 10 minutes not count!");
            mText3View.setText("(Press back to start again.)");
        }
        mScoreDetailsView.setText("Penalties:\n" +
                "Speed Limit Overrun above 60 seconds: -" + penaltyPoint[0] + "\n" +
                "Instant Acceleration above %40: -" + penaltyPoint[1] + "\n" +
                "Missing the gear shift-up time: -" + penaltyPoint[0] + "\n" +
                "Fuel Efficiency: -" + penaltyPoint[2]);
        mScoreDetailsView.setVisibility(View.VISIBLE);

        mVehicleSpeedView.setVisibility(View.INVISIBLE);
        mEngineSpeedView.setVisibility(View.INVISIBLE);
        mOdometerView.setVisibility(View.INVISIBLE);
        mFuelConsumedView.setVisibility(View.INVISIBLE);
        mFuelLevelView.setVisibility(View.INVISIBLE);
        mAcceleratorPedalPositionView.setVisibility(View.INVISIBLE);
        mTransmissionGearPositionView.setVisibility(View.INVISIBLE);

        buttonShare.setVisibility(View.VISIBLE);
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "Here is my score on Ford Green Drive Challenge: " + score + ".\n I challenge you too! http://www.ford.com/";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);
        Intent intent = new Intent(this, VehicleManager.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        countSpeedOverrunSeries = 0;
        countPenaltySpeed = 0;
        Arrays.fill(latest3Speeds,-1);
        countPenaltyBoost = 0;
        pointPenaltyGear = 0;

        totalTime = 0;
        score = 0;

        mEngineSpeedView = (TextView) findViewById(R.id.engine_speed);
        mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
        mOdometerView = (TextView) findViewById(R.id.odometer);
        mFuelConsumedView = (TextView) findViewById(R.id.fuel_consumed);
        mFuelLevelView = (TextView) findViewById(R.id.fuel_level);
        mAcceleratorPedalPositionView = (TextView) findViewById(R.id.pedal);
        mTransmissionGearPositionView = (TextView) findViewById(R.id.gear);

        mText1View = (TextView) findViewById(R.id.text1);
        mText2View = (TextView) findViewById(R.id.text2);
        mText3View = (TextView) findViewById(R.id.text3);
        mScoreDetailsView = (TextView) findViewById(R.id.score_details);

        mPenaltySpeedView = (TextView) findViewById(R.id.penalty_speedoverrun);
        mPenaltyBoostView = (TextView) findViewById(R.id.penalty_speedboost);
        mPenaltyGearView = (TextView) findViewById(R.id.penalty_gear);

        mVehicleSpeedView.setVisibility(View.INVISIBLE);
        mEngineSpeedView.setVisibility(View.INVISIBLE);
        mOdometerView.setVisibility(View.INVISIBLE);
        mFuelConsumedView.setVisibility(View.INVISIBLE);
        mFuelLevelView.setVisibility(View.INVISIBLE);
        mAcceleratorPedalPositionView.setVisibility(View.INVISIBLE);
        mTransmissionGearPositionView.setVisibility(View.INVISIBLE);
        mPenaltyGearView.setVisibility(View.INVISIBLE);
        mPenaltySpeedView.setVisibility(View.INVISIBLE);
        mPenaltyBoostView.setVisibility(View.INVISIBLE);

        final Button buttonFinish= (Button) findViewById(R.id.button_finish);
        buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishChallenge();
                buttonFinish.setVisibility(View.INVISIBLE);
            }
        });
        buttonShare = (Button) findViewById(R.id.button_share);
        buttonShare.setVisibility(View.INVISIBLE);
        mScoreDetailsView.setVisibility(View.INVISIBLE);

    }

    private void startChallenge() {
        new Timer().scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run() {
                mHandler.post(mReadValues);
                //CASE1: SPEED LIMIT
                if(countSpeedOverrunSeries >= OVERRUN_TIME) {
                    countPenaltySpeed++;
                }
                if(speed > SPEEDLIMIT) {
                    countSpeedOverrunSeries++;
                }
                else {
                    countSpeedOverrunSeries = 0;
                }

                //CASE2: SPEED BOOST
                if(latest3Speeds[2] != -1) {
                    if(latest3Speeds[0] > latest3Speeds[2]+ 40) {
                        countPenaltyBoost++;
                    }
                }
                latest3Speeds[2] = latest3Speeds[1];
                latest3Speeds[1] = latest3Speeds[0];
                latest3Speeds[0] = pedal;

                //CASE3: GEAR TIMING
                //Below limit values are based on ford focus catalogue. ideal values to shift-up.
                if(gear != null) {
                    switch (gear.toString()) {
                        case "FIRST":
                            if (speed > 24) {
                                pointPenaltyGear += speed - 24;
                            }
                            break;
                        case "SECOND":
                            if (speed > 40) {
                                pointPenaltyGear += speed - 40;
                            }
                            break;
                        case "THIRD":
                            if (speed > 64) {
                                pointPenaltyGear += speed - 64;
                            }
                            break;
                        case "FOURTH":
                            if (speed > 72) {
                                pointPenaltyGear += speed - 72;
                            }
                            break;
                        case "FIFTH":
                            if (speed > 80) {
                                pointPenaltyGear += speed - 80;
                            }
                            break;
                        default:
                            break;
                    }
                }
                //CASE4: FUEL EFFICIENCY: calculating in the end.
                if(totalTime == 1) {
                    odometerStart = odometer;
                    fuelConsumeStart = fuelConsume;
                }
                //PRINT OUT PENALTIES
                mHandler.post(mPrintOutTheScores);
                totalTime++;
            }
        }, 0, 1000);
    }


    private Handler mHandler = new Handler();
    private Runnable mReadValues = new Runnable() {
        public void run() {
            try {
                speed = ((VehicleSpeed) mVehicleManager.get(VehicleSpeed.class)).getValue().doubleValue();
                rpm = ((EngineSpeed) mVehicleManager.get(EngineSpeed.class)).getValue().doubleValue();
                odometer = ((Odometer) mVehicleManager.get(Odometer.class)).getValue().doubleValue();
                fuelConsume = ((FuelConsumed) mVehicleManager.get(FuelConsumed.class)).getValue().doubleValue();
                fuelLevel = ((FuelLevel) mVehicleManager.get(FuelLevel.class)).getValue().doubleValue();
                pedal = ((AcceleratorPedalPosition) mVehicleManager.get(AcceleratorPedalPosition.class)).getValue().intValue();
                gear = ((TransmissionGearPosition) mVehicleManager.get(TransmissionGearPosition.class)).getValue().enumValue();

                //DISPLAY THEM!
//                mVehicleSpeedView.setText("Vehicle Speed: " + speed + "KM/H");
//                mEngineSpeedView.setText("Engine Speed: " + rpm + "RPM");
//                mOdometerView.setText("Odometer: " + odometer + "km");
//                mFuelConsumedView.setText("Fuel Consumed: " + fuelConsume + "L");
//                mFuelLevelView.setText("Fuel Level: " + fuelLevel + "%");
//                mAcceleratorPedalPositionView.setText("Pedal: " + pedal + "%");
//                mTransmissionGearPositionView.setText("Gear: " + gear.toString());

            } catch (NoValueException e) {
                Log.w(TAG, "The vehicle may not have made the measurement yet");
            } catch (UnrecognizedMeasurementTypeException e) {
                Log.w(TAG, "The measurement type was not recognized");
            }
        }
    };

    private Runnable mPrintOutTheScores = new Runnable() {
        public void run() {
//            mPenaltySpeedView.setText("Overruns: " + countSpeedOverrunSeries + ", Penalties: " + countPenaltySpeed);
//            mPenaltyBoostView.setText("Latest Pedal: " + latest3Speeds[0] + "," + latest3Speeds[1] + "," + latest3Speeds[2] + "; Penalties: " + countPenaltyBoost);
//            mPenaltyGearView.setText("Gear Penalty Point: " + pointPenaltyGear);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }
}
