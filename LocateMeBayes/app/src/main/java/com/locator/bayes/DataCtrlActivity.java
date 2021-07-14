package com.locator.bayes;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DataCtrlActivity extends AppCompatActivity {

    private Button buttonExpRaw, buttonImpRaw, buttonExpDict, buttonErase;
    private TextView textDataCtrlStat;

    private int delPressCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_ctrl);

        DataHolder app = (DataHolder) getApplicationContext();

        textDataCtrlStat = (TextView) findViewById(R.id.textDataStat);

        buttonExpRaw = (Button) findViewById(R.id.buttonExpRaw);
        // Set listener for the button.
        buttonExpRaw.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                delPressCount = 0;
                if (app.exportRaw()) {
                    textDataCtrlStat.setText("Exported raw data.");
                }
                else {
                    textDataCtrlStat.setText("Failed to export raw data.");
                }
            }
        });

        buttonImpRaw = (Button) findViewById(R.id.buttonImpRaw);
        // Set listener for the button.
        buttonImpRaw.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                delPressCount = 0;
                if (app.importRaw()) {
                    textDataCtrlStat.setText("Imported Raw Data.");
                }
                else {
                    textDataCtrlStat.setText("Failed to import raw data.");
                }
            }
        });

        buttonExpDict = (Button) findViewById(R.id.buttonExpDict);
        // Set listener for the button.
        buttonExpDict.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                delPressCount = 0;
                if (app.exportDictStr()) {
                    textDataCtrlStat.setText("Exported python dict.");
                }
                else {
                    textDataCtrlStat.setText("Failed to export dict.");
                }
            }
        });


        buttonErase = (Button) findViewById(R.id.buttonClearData);
        // Set listener for the button.
        buttonErase.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (++delPressCount == 2) {
                    app.clearTrainingData();
                    textDataCtrlStat.setText("Cleared data.");
                    delPressCount = 0;
                }
                else {
                    textDataCtrlStat.setText("Press again to clear data.");
                }
            }
        });
    }
}