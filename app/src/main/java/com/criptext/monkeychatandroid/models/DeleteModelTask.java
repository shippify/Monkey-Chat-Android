package com.criptext.monkeychatandroid.models;

import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;

import java.util.LinkedList;

/**
 * Created by gesuwall on 12/1/16.
 */
public class DeleteModelTask extends AsyncTask<Model, Integer, Integer> {

    @Override
    protected Integer doInBackground(Model... params) {
        ActiveAndroid.beginTransaction();
        try {

            for (Model model : params) {
                try {
                    model.delete();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
        return 1;
    }

}
