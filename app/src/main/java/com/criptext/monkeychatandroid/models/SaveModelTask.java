package com.criptext.monkeychatandroid.models;

import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by gesuwall on 9/20/16.
 */
public class SaveModelTask extends AsyncTask<Model, Integer, Model[]> {

    OnQueryReturnedListener onQueryReturnedListener = null;

    @Override
    protected Model[] doInBackground(Model... params) {
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
        return params;
    }

    @Override
    protected void onPostExecute(Model[] models) {
        if(this.onQueryReturnedListener != null)
            onQueryReturnedListener.onQueryReturned(models);
    }

    public interface OnQueryReturnedListener {
        void onQueryReturned(Model[] storedModels);
    }
}
