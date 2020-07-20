package com.example.audifonos;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE=8000;  //FRECUENCIA DE MUESTREO
    private static final int RECORDER_CHANELS= AudioFormat.CHANNEL_IN_MONO; //TIPO DE CANAL
    private static final int RECORDER_AUDIO_ENCODING=AudioFormat.ENCODING_PCM_16BIT; //

    private AudioRecord grabadora=null;
    private Thread grabacionThread=null;
    private boolean estaGrabando=false;
    private  String filePath = null;
    final int REQUEST_PERMISSION_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(tienePermisos()){
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Grabacion2.wav";
            manejarBoton();
            habilitarBotones(false);

            int buffer=AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANELS, RECORDER_AUDIO_ENCODING);
        }
        else {
           pedirPermisos();
        }

    }
    private  boolean tienePermisos(){
        return hasPermission(Manifest.permission.RECORD_AUDIO) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) && hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

    }
    private void pedirPermisos(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_PERMISSION_CODE:
                {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this,"Permission Granted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show();
                }
            break;
        }
    }

    private void manejarBoton() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick); //METODO BTONCLICK --> FUNCION
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);  //METODO BTONCLICK --> FUNCION
    }

    private void enableButton(int id, boolean estaHabilitado) {
        ((Button) findViewById(id)).setEnabled(estaHabilitado);
    }

    private void habilitarBotones (boolean estaGrabando){
        enableButton(R.id.btnStart, !estaGrabando);
    }

    int BufferElements2Rec = 1024;
    int BytesPerElement = 2;

    private void startRecording() {

        grabadora = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        grabadora.startRecording();
        estaGrabando = true;
        grabacionThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        grabacionThread.start();
    }

    //CONVIRTIENDO SHORT A BYTE
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void writeAudioDataToFile() {

        if(!tienePermisos()){
            System.out.println("NO tiene permisos");
            return;
        }
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (estaGrabando) {
            // gets the voice output from microphone to byte format
            grabadora.read(sData, 0, BufferElements2Rec);
            System.out.println("Archivando" + sData.toString());

                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer
                    byte bData[] = short2byte(sData);
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != grabadora) {
            estaGrabando = false;
            grabadora.stop();
            grabadora.release();
            grabadora = null;
            grabacionThread = null;
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    habilitarBotones(true);
                    startRecording();
                    break;
                }
                case R.id.btnStop: {
                    habilitarBotones(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
