package com.linfaxin.multistyletextview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.linfaxin.multistyletextview.MultiStyleTextView;

/**
 * Created by linfaxin on 14-12-19.
 * Email linfaxin@xiaomashijia.com
 */
public class DemoActivity extends Activity{
    EditText editText;
    MultiStyleTextView multiStyleTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        editText = (EditText) findViewById(R.id.editText);
        multiStyleTextView = (MultiStyleTextView) findViewById(R.id.multiStyleTextView);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                multiStyleTextView.setTextMulti(editText.getText().toString());
            }
        });
    }
}
