package com.example.webrtcapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.kurento.client.Continuation;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.IceComponentStateChangeEvent;
import org.kurento.client.IceGatheringDoneEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaStateChangedEvent;
import org.kurento.client.NewCandidatePairSelectedEvent;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

/**
 * Basic WebRTC video streaming implementation with Kurento Media Server backend.
 *
 * Kurento Client Documentation: https://doc-kurento.readthedocs.io/en/stable/_static/client-javadoc/index.html
 */
public class MainActivity extends AppCompatActivity {

    private final String tag = this.getClass().getCanonicalName();

    private final String kurentoMediaServerURL = "ws://192.168.1.219:8888/kurento";
    private final String kurentoPlayerEndpointURL = "rtsp://admin:12345678test@192.168.1.108:554/cam/realmonitor?channel=1&subtype=1";
    //private String kurentoPlayerEndpointURL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    private final EglBase eglBase = EglBase.create();

    private PeerConnection peer = null;
    private PeerConnectionFactory factory = null;

    private AudioTrack localAudioTrack = null;
    private VideoTrack localVideoTrack = null;
    private MediaStream localMediaStream = null;

    private KurentoClient kurentoClient = null;
    private WebRtcEndpoint webRTCEndpoint = null;
    private PlayerEndpoint playerEndpoint = null;

    private SurfaceViewRenderer localVideoView = null;
    private SurfaceViewRenderer remoteVideoView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createPeerFactory();
        createVideoViews();
        createPeerConnection();
        createKurentoClient();

        negotiateSDPOffer();
    }

    private void createPeerFactory() {
        PeerConnectionFactory.InitializationOptions options =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions();

        PeerConnectionFactory.initialize(options);

        factory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .createPeerConnectionFactory();
    }

    private void createVideoViews() {
        localVideoView = findViewById(R.id.surface_view);
        localVideoView.setZOrderMediaOverlay(true);
        localVideoView.setEnableHardwareScaler(true);
        localVideoView.init(eglBase.getEglBaseContext(), null);

        localMediaStream = factory.createLocalMediaStream("media-stream");
        localAudioTrack = factory.createAudioTrack("audio-track", factory.createAudioSource(new MediaConstraints()));
        localMediaStream.addTrack(localAudioTrack);

        localVideoTrack = factory.createVideoTrack("video-track", factory.createVideoSource(false));
        localVideoTrack.addSink(localVideoView);
        localMediaStream.addTrack(localVideoTrack);

        remoteVideoView = findViewById(R.id.remote_surface_view);
        remoteVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setEnableHardwareScaler(true);
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
    }

    private void createPeerConnection() {
        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(new ArrayList<>());
        configuration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peer = factory.createPeerConnection(configuration, new PeerConnectionObserver("Local Peer Connection Observer") {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                super.onIceCandidate(candidate);
                webRTCEndpoint.addIceCandidate(new org.kurento.client.IceCandidate(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex));
            }

            @Override
            public void onAddStream(MediaStream stream) {
                super.onAddStream(stream);
                remoteVideoView.setVisibility(View.VISIBLE);
                stream.videoTracks.get(0).addSink(remoteVideoView);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
                Log.d(tag, "onIceGatheringChange called with: ice gathering state = [" + state + "]");
            }
        });

        if (null == peer) {
            Log.d(tag, "Failed to create peer connection.");
            System.exit(1);
        }

        peer.addTrack(localAudioTrack);
        peer.getTransceivers().get(0).setDirection(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY);

        peer.addTrack(localVideoTrack);
        peer.getTransceivers().get(1).setDirection(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY);
    }

    private void createKurentoClient() {
        try {
            kurentoClient = KurentoClient.create(kurentoMediaServerURL);
        } catch (KurentoException ke) {
            Log.d(tag, "Failed to establish connection to Kurento Media Server: " + ke.getMessage());
            System.exit(1);
        }

        MediaPipeline pipeline = kurentoClient.createMediaPipeline();
        createWebRTCEndpoint(pipeline);
        createPlayerEndpoint(pipeline);
    }

    private void createWebRTCEndpoint(MediaPipeline pipeline) {
        webRTCEndpoint = new WebRtcEndpoint.Builder(pipeline).recvonly().build();
        webRTCEndpoint.addMediaStateChangedListener((MediaStateChangedEvent event) ->
                Log.d(tag, "addMediaStateChangedListener called with: event = [" + event + "]"));
        webRTCEndpoint.addOnIceCandidateListener((OnIceCandidateEvent event) ->
                Log.d(tag, "webRTCEndpoint.addOnIceCandidateListener called with: event = [" + event + "]"));
        webRTCEndpoint.addIceGatheringDoneListener((IceGatheringDoneEvent event) ->
                Log.d(tag, "webRTCEndpoint.addIceGatheringDoneListener called with: event = [" + event + "]"));
        webRTCEndpoint.addIceComponentStateChangeListener((IceComponentStateChangeEvent event) ->
                Log.d(tag, "webRTCEndpoint.addIceComponentStateChangeListener called with: event = [" + event + "]"));
        webRTCEndpoint.addNewCandidatePairSelectedListener((NewCandidatePairSelectedEvent event) ->
                Log.d(tag, "webRTCEndpoint.addNewCandidatePairSelectedListener called with: event = [" + event + "]"));
        webRTCEndpoint.addIceCandidateFoundListener((IceCandidateFoundEvent event) ->
                Log.d(tag, "webRTCEndpoint.addIceCandidateFoundListener called with: ice candidate found event = [" + event + "]"));
    }

    private void createPlayerEndpoint(MediaPipeline pipeline) {
        playerEndpoint = new PlayerEndpoint.Builder(pipeline, kurentoPlayerEndpointURL)
                .withNetworkCache(0)
                .build();
        playerEndpoint.addErrorListener((ErrorEvent event) ->
                Log.d(tag, "playerEndpoint.addErrorListener: " + event.getDescription()));
        playerEndpoint.addEndOfStreamListener((EndOfStreamEvent event) ->
                Log.d(tag, "playerEndpoint.addEndOfStreamListener: " + event.getTimestamp()));
        playerEndpoint.connect(webRTCEndpoint);
    }

    private void negotiateSDPOffer() {
        peer.createOffer(new SDPObserver("Create Offer SDP Observer") {
            @Override
            public void onCreateSuccess(SessionDescription offer) {
                super.onCreateSuccess(offer);
                Log.d(tag, "SDP Offer: " + offer.description);

                peer.setLocalDescription(new SDPObserver("Set Local Description Observer") {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();

                        webRTCEndpoint.processOffer(offer.description, new Continuation<String>() {
                            @Override
                            public void onSuccess(String answer) {
                                Log.d(tag, "SDP Answer = " + answer);

                                peer.setRemoteDescription(new SDPObserver("Set Remote Description Observer") {
                                    @Override
                                    public void onSetSuccess() {
                                        webRTCEndpoint.gatherCandidates();

                                        Log.d(tag, "Peer Signaling State " + peer.signalingState().name());
                                        Log.d(tag, "Peer Connection State " + peer.connectionState().name());
                                        Log.d(tag, "Ice Gathering State " + peer.iceGatheringState().name());
                                        Log.d(tag, "Ice Connection State " + peer.iceConnectionState().name());

                                        playerEndpoint.play();
                                    }
                                }, new SessionDescription(SessionDescription.Type.PRANSWER, answer));
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                Log.d(tag, "onError() => " + throwable);
                            }
                        });
                    }
                }, offer);
            }
        }, createSDPConstraints());
    }

    private MediaConstraints createSDPConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
        return constraints;
    }
}
