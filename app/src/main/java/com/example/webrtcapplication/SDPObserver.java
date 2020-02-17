package com.example.webrtcapplication;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SDPObserver implements SdpObserver {

    private String tag = this.getClass().getCanonicalName();

    SDPObserver(String tag) {
        this.tag += " " + tag;
    }

    @Override
    public void onCreateSuccess(SessionDescription description) {
        Log.d(tag, "onCreateSucess() called with: session description = [" + description + "]");
    }

    @Override
    public void onSetSuccess() {
        Log.d(tag, "onSetSuccess() called");
    }

    @Override
    public void onCreateFailure(String message) {
        Log.d(tag, "onCreateFailure() called with: message = [" + message + "]");
    }

    @Override
    public void onSetFailure(String message) {
        Log.d(tag, "onSetFailure() called with: message = [" + message + "]");
    }
}
