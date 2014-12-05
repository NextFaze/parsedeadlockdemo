package com.nextfaze.parsedeadlockexample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private Executor executor = new ThreadPoolExecutor(4, 4, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(10));

    private AsyncTask<Void, Integer, Void> task1;
    private AsyncTask<Void, Integer, Void> task2;

    private TextView task1Counter;
    private TextView task2Counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Parse.initialize(getApplicationContext(),
                BuildConfig.PARSE_APP_ID,
                BuildConfig.PARSE_CLIENT_KEY);

        task1Counter = (TextView) findViewById(R.id.task_1_counter);
        task2Counter = (TextView) findViewById(R.id.task_2_counter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        task1 = new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    int i = 0;
                    while (true) {
                        i++;
                        publishProgress(i);
                        ParseUser.getQuery().count();
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "ParseException in task1", e);
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                task1Counter.setText(String.format("counter: %d", values[0]));
            }
        };

        task2 = new AsyncTask<Void, Integer, Void>() {
            volatile ParseUser user = null;

            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "task1: creating anonymous user...");

                ParseAnonymousUtils.logIn(new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        user = parseUser;
                    }
                });

                // wait for user to be created
                while (user == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.i(TAG, "task1: got anonymous user");

                try {
                    int i = 0;
                    while (true) {
                        i++;
                        publishProgress(i);
                        user.fetch();
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "ParseException in task1", e);
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                task2Counter.setText(String.format("counter: %d user objectId=%s", values[0], user.getObjectId()));
            }
        };

        task1.executeOnExecutor(executor);
        task2.executeOnExecutor(executor);
    }

    @Override
    protected void onPause() {
        super.onPause();
        task1.cancel(true);
        task2.cancel(true);
    }
}
