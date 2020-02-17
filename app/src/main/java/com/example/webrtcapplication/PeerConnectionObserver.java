package com.example.webrtcapplication;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

public class PeerConnectionObserver implements PeerConnection.Observer {

    private String tag = this.getClass().getCanonicalName();

    PeerConnectionObserver(String tag) {
        this.tag += " " + tag;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState state) {
        Log.d(tag, "onSignalingChange() called with: signaling state = [" + state + "]");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
        Log.d(tag, "onIceConnectionChange() called with: ice connection stats = [" + state + "]");
    }

    @Override
    public void onIceConnectionReceivingChange(boolean value) {
        Log.d(tag, "onIceConnectionReceivingChange() called with: value = [" + value + "]");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
        Log.d(tag, "onIceGatheringChange() called with: ice gathering state = [" + state + "]");
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.d(tag, "onIceCandidate() called with: ice candidate = [" + candidate + "]");
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.d(tag, "onIceCandidatesRemoved() called with: ice candidates = [" + candidates + "]");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {
        Log.d(tag, "onAddTrack() called with: RTP reciever = [" + receiver + "]");
        Log.d(tag, "onAddTrack() called with: media streams = [" + streams + "]");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        Log.d(tag, "onAddStream() called with: media stream = [" + stream + "]");
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.d(tag, "onRemoveStream() called with: media stream = [" + stream + "]");
    }

    @Override
    public void onDataChannel(DataChannel channel) {
        Log.d(tag, "onDataChannel() called with: data channel = [" + channel + "]");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(tag, "onRenegoiationNeeded() called");
    }
}
