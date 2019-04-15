package com.jairaj.janglegmail.motioneye;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class loading_screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_loading_screen);

        /*Thread welcomeThread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    super.run();
                    sleep(500);
                }
                catch (Exception ignored)
                {
                }
                finally
                {
                    Intent i = new Intent(loading_screen.this, AddCam.class);
                    startActivity(i);
                    finish();
                }
            }
        };
        welcomeThread.start();*/

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(loading_screen.this, AddCam.class);
                startActivity(i);

                // close this activity
                finish();
            }
        }, 300);
    }
}
