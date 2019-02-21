package com.criptext.monkeychatandroid.dialogs;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.criptext.lib.MKDelegateActivity;
import com.criptext.monkeychatandroid.R;

/**
 * Created by gesuwall on 8/19/16.
 */
public class NewGroupDialog extends DialogFragment {

    /**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    public static NewGroupDialog newInstance() {
        NewGroupDialog f = new NewGroupDialog();
        /*
        // Supply num input as an argument.
        Bundle args = new Bundle();
        f.setArguments(args);
        */

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.new_group));
        View v = inflater.inflate(R.layout.dialog_new_group, container, false);

        final EditText et = (EditText) v.findViewById(R.id.edittext);

        Button button = (Button)v.findViewById(R.id.createbtn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // When button is clicked, call up to owning activity.
                final String groupName = et.getText().toString();
                if(!groupName.isEmpty()) {
                    ((MKDelegateActivity) getActivity()).createGroup("", groupName, null);
                    dismiss();
                    Toast.makeText(getActivity(), "Creating group " + et.getText(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return v;
    }
}
