package com.dnpa.grabadora;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private AudioRecord recorder = null;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    private MediaRecorder grabacion;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private Thread recordingThread = null;
    private String archivoSalida = null;
    private Button btn_recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_recorder=findViewById(R.id.btn_rec);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1000);
        }
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();

        recordingInProgress.set(true);

        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        recordingInProgress.set(false);

        recorder.stop();

        recorder.release();

        recorder = null;

        recordingThread = null;
    }

    public void Recorder(View view){
        if (grabacion == null){
            archivoSalida = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Grabacion.mp3";
            grabacion = new MediaRecorder();
            grabacion.setAudioSource(MediaRecorder.AudioSource.MIC);
            grabacion.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            grabacion.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            grabacion.setOutputFile(archivoSalida);
            try{
                grabacion.prepare();
                grabacion.start();
                startRecording();
            }catch (IOException e){

            }

            btn_recorder.setBackgroundResource(R.drawable.rec);
            Toast.makeText(getApplicationContext(), "Grabando....",Toast.LENGTH_SHORT).show();
        } else if(grabacion != null){
            grabacion.stop();
            stopRecording();
            grabacion.release();
            grabacion = null;
            btn_recorder.setBackgroundResource(R.drawable.stop_rec);
            Toast.makeText(getApplicationContext(), "Grabacion finalizada", Toast.LENGTH_SHORT).show();
        }
    }


    public void reproducir(View view){
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(archivoSalida);
            mediaPlayer.prepare();
        }catch (IOException e){
        }

        mediaPlayer.start();
        Toast.makeText(getApplicationContext(), "Reproducir audio", Toast.LENGTH_SHORT).show();
    }

    private class RecordingRunnable implements Runnable {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            final File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }
}
