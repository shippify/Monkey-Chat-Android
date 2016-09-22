package com.criptext.monkeychatandroid.models;

import android.os.AsyncTask;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;

/**
 * Created by gesuwall on 9/20/16.
 */
class SaveModelTask extends AsyncTask<Model, Integer, Integer> {

    @Override
    protected Integer doInBackground(Model... params) {
        boolean shouldUseTransaction = params.length > 1;
        try {
            if (shouldUseTransaction)
                ActiveAndroid.beginTransaction();

            for (Model model : params) {
                model.save();
            }

            if (shouldUseTransaction) {
                ActiveAndroid.setTransactionSuccessful();
            }
        } finally {
            if(shouldUseTransaction) {
                ActiveAndroid.endTransaction();
            }
        }
        return 1;
    }
}
