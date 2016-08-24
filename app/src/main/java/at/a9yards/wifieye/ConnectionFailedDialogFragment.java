package at.a9yards.wifieye;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class ConnectionFailedDialogFragment extends DialogFragment {

    public static final String PASSWORD_ARGUMENT = "password_argument";
    public static final String SSID_ARGUMENT = "ssid_argument";

    String password;
    String ssid;

    @Override
    public void setArguments(Bundle args) {
        password = args.getString(PASSWORD_ARGUMENT);
        ssid = args.getString(SSID_ARGUMENT);
    }





    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View rootView = inflater.inflate(R.layout.fragment_password_dialog, null);

        final EditText passwordView = (EditText) rootView.findViewById(R.id.dialog_password_edit_view);
        passwordView.setText(password);
        passwordView.requestFocus();
        //TextView messageView = (TextView) rootView.findViewById(R.id.dialog_ssid_text_view);
        //messageView.setText(getString(R.string.dialog_message_stub)+" "+ssid);

        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent result = new Intent();

                        result.putExtra(AvailableNetworksFragment.PASSWORD_SCAN_RESULT, passwordView.getText().toString());
                        result.putExtra(AvailableNetworksFragment.SSID_FOR_SCAN, ssid);

                        getTargetFragment().onActivityResult(AvailableNetworksFragment.SCAN_REQUEST_CODE, getActivity().RESULT_OK, result);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConnectionFailedDialogFragment.this.getDialog().cancel();
                    }
                }).setTitle(getString(R.string.dialog_title) + " " + ssid);
        return builder.create();
    }
}
