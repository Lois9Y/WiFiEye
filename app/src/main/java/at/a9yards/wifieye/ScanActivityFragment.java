package at.a9yards.wifieye;

import android.content.Intent;
import android.graphics.PointF;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.List;

import at.nineyards.anyline.camera.AnylineViewConfig;
import at.nineyards.anyline.camera.FocusConfig;
import at.nineyards.anyline.modules.ocr.AnylineOcrConfig;
import at.nineyards.anyline.modules.ocr.AnylineOcrError;
import at.nineyards.anyline.modules.ocr.AnylineOcrListener;
import at.nineyards.anyline.modules.ocr.AnylineOcrResult;
import at.nineyards.anyline.modules.ocr.AnylineOcrScanView;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScanActivityFragment extends Fragment {

    private final static String LOG_TAG = ScanActivity.class.getSimpleName();
    private AnylineOcrScanView scanView;
    private String ssidRequested;

    public ScanActivityFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ssidRequested = getActivity().getIntent().getExtras().getString(AvailableNetworksFragment.SSID_FOR_SCAN);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //getActivity().setContentView(R.layout.fragment_scan);

        scanView = (AnylineOcrScanView) getActivity().findViewById(R.id.scan_view);

        scanView.setConfig(new AnylineViewConfig(getActivity(), "password_view_config.json"));


        scanView.copyTrainedData("tessdata/eng_no_dict.traineddata", "d142032d86da1be4dbe22dce2eec18d7");
        scanView.copyTrainedData("tessdata/deu.traineddata", "2d5190b9b62e28fa6d17b728ca195776");
        scanView.copyTrainedData("tessdata/Calibri.traineddata", "2d5190b9b62e28fa6d17b728ca195776");

//Configure the OCR for IBANs
        AnylineOcrConfig anylineOcrConfig = new AnylineOcrConfig();
// use the line mode (line length and font may vary)
        anylineOcrConfig.setScanMode(AnylineOcrConfig.ScanMode.LINE);
// set the languages used for OCR
        anylineOcrConfig.setTesseractLanguages("eng", "deu");
// allow only capital letters and numbers
        //anylineOcrConfig.setCharWhitelist("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
// set the height range the text can have
        anylineOcrConfig.setMinCharHeight(30);
        anylineOcrConfig.setMaxCharHeight(60);
// The minimum confidence required to return a result, a value between 0 and 100.
// (higher confidence means less likely to get a wrong result, but may be slower to get a result)
        anylineOcrConfig.setMinConfidence(70);

        anylineOcrConfig.setMinSharpness(62);


// a simple regex for a basic validation of the IBAN, results that don't match this, will not be returned
// (full validation is more complex, as different countries have different formats)
        //anylineOcrConfig.setValidationRegex("^[A-Z]{2}([0-9A-Z]\\s*){13,32}$");
// removes small contours (helpful in this case as no letters with small artifacts are allowed, like iöäü)
        //anylineOcrConfig.setRemoveSmallContours(true);
// removes possible whitespaces in the result
        //anylineOcrConfig.setRemoveWhitespaces(true);

// set the ocr config
        scanView.setAnylineOcrConfig(anylineOcrConfig);

        FocusConfig focusConfig = new FocusConfig.Builder()
                .setDefaultMode(Camera.Parameters.FOCUS_MODE_AUTO) // set default focus mode to be auto focus
                .setAutoFocusInterval(8000) // set an interval of 8 seconds for auto focus
                .setEnableFocusOnTouch(true) // enable focus on touch functionality
                .setEnablePhaseAutoFocus(true)  // enable phase focus for faster focusing on new devices
                .setEnableFocusAreas(true)  // enable focus areas to coincide with the cutout
                .build();
        // set the focus config
        scanView.setFocusConfig(focusConfig);
        // set the highest possible preview fps range
        scanView.setUseMaxFpsRange(true);
        // set sports scene mode to try and bump up the fps count even more
        scanView.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);

        scanView.initAnyline(getString(R.string.anyline_key), new AnylineOcrListener() {
            @Override
            public boolean onTextOutlineDetected(List<PointF> list) {
                return false;
            }

            @Override
            public void onReport(String s, Object o) {

            }

            @Override
            public void onAbortRun(AnylineOcrError anylineOcrError, String s) {
                Intent result = new Intent();
                getActivity().setResult(getActivity().RESULT_CANCELED,result);
                getActivity().finish();
            }

            @Override
            public void onResult(AnylineOcrResult anylineOcrResult) {

                Intent result = new Intent();

                result.putExtra(AvailableNetworksFragment.PASSWORD_SCAN_RESULT, anylineOcrResult.getText().trim());
                result.putExtra(AvailableNetworksFragment.SSID_FOR_SCAN,ssidRequested);
                getActivity().setResult(getActivity().RESULT_OK,result);
                getActivity().finish();

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        scanView.startScanning();
    }

    @Override
    public void onPause() {
        super.onPause();

        scanView.cancelScanning();
        scanView.releaseCameraInBackground();
    }
}
