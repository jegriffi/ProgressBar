package com.codepath.customprogressbar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.codepath.customprogressbar.Widget.GoalProgressBar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.progressBar)
    GoalProgressBar progressBar;
    Switch errorSwitch;
    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (GoalProgressBar) findViewById(R.id.progressBar);
        errorSwitch = (Switch) findViewById(R.id.isError);
        editText = (EditText) findViewById(R.id.editText);

        progressBar.initNumOfFields(5);

        errorSwitch.setChecked(false);
        errorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    progressBar.setErrorState(true);
                } else {
                    progressBar.setErrorState(false);
                }
            }
        });
        ButterKnife.inject(this);
    }

    @OnClick(R.id.prevProgressBtn)
    public void prevProgress() {
        progressBar.setProgress(GoalProgressBar.ProgressState.BACKWARDS);
    }

    @OnClick(R.id.nextProgressBtn)
    public void nextProgress() {
        progressBar.setProgress(GoalProgressBar.ProgressState.FORWARD);
    }

    @OnClick(R.id.halfStepProgress)
    public void halfProgress() {
        progressBar.setProgress(GoalProgressBar.ProgressState.HALF_FORWARD);
    }

    @OnClick(R.id.updateFieldSize)
    public void setFieldNumberDynamically() {
        String text = editText.getText().toString();
        try {
            int newFieldNum = Integer.parseInt(text);
            if (newFieldNum <= 0 || newFieldNum > 20) {
                throw new Exception();
            }
            progressBar.initNumOfFields(newFieldNum);
        } catch (Exception e) {
            Toast.makeText(this, "nice try, Noob", Toast.LENGTH_LONG).show();
        }
    }
}
