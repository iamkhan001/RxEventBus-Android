package com.nstudio.rxjavatest;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.functions.Consumer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        RxBus.subscribe(RxBus.SUBJECT_MY_SUBJECT, this, new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {

                        Data data = (Data) o;
                        Toast.makeText(MainActivity.this,"Event Received\n"+data.getMessage(),Toast.LENGTH_SHORT).show();
                        Log.v("Testing", data.getMessage());
                    }
                }
        );

        findViewById(R.id.btnEvent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    RxBus.publish(RxBus.SUBJECT_MY_SUBJECT,new Data("Hello World!"));
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.unregister(this);
    }
}
