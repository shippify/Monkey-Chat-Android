package com.criptext.monkeychatandroid.models;

import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;

/**
 * Created by gesuwall on 9/20/16.
 */
class SaveModelTask extends AsyncTask<Model, Integer, Integer> {

    @Override
    protected Integer doInBackground(Model... params) {
        ActiveAndroid.beginTransaction();
        try {

            for (Model model : params) {
                try {
                    model.save();
                } catch (SQLiteConstraintException ex) {
                    Log.e("SaveModelTask", ex.getMessage());
                }
            }

            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return 1;
    }
}
