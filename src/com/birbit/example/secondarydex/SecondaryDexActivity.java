package com.birbit.example.secondarydex;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.birbit.example.secondarydex.big.base.IDateParser;
import com.birbit.example.secondarydex.big.base.JodaLoader;
import com.birbit.example.secondarydex.big.base.exception.MyIllegalArgumentException;
import com.birbit.example.secondarydex.big.base.exception.MyUnsupportedOperationException;

import java.util.Date;

public class SecondaryDexActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private static final String TAG = "SecondaryDexActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        IDateParser dateParser;
        try {
            dateParser = new JodaLoader(this).loadDateParser();
            Date date = dateParser.parseDate(dateParser.format(new Date()));
            ((TextView) findViewById(R.id.text_view)).setText(date.toLocaleString());
        } catch (MyIllegalArgumentException e) {
            Log.e(TAG, "",e);
        } catch (MyUnsupportedOperationException e) {
            Log.e(TAG, "",e);
        } catch (Throwable t) {
            Log.e(TAG, "unable to load date parser", t);
        }
    }
}
