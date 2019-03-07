package com.mark.autoinstall;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static String updateURL = "https://dummyApp.surge.sh/UpdatedApp.apk";

    private ProgressBar progressBar;
    private ProgressBar progressIcon;
    private Button btnDoUpdate;
    private Button btnCancel;
    private TextView stateLabel;
    private UpdateWork updateWork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AskForPermission();

        progressBar = findViewById(R.id.ProgressBar);
        progressIcon = findViewById(R.id.ProgressIcon);
        stateLabel = findViewById(R.id.StateLabel);
        btnDoUpdate = findViewById(R.id.BtnDoUpdate);
        btnDoUpdate.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {
                Reset();
                DoUpdate();
            }
        });

        btnCancel = findViewById(R.id.BtnCancel);
        btnCancel.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                DoCancel();
            }
        });
    }

    private  void ResetProgressBar(){
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressIcon.setVisibility(View.VISIBLE);
    }

    private void AskForPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int REQUEST_CODE = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE);
                    return;
                }
            }
        }
    }

    private void DoUpdate(){
        updateWork = new UpdateWork();
        updateWork.execute(updateURL);
    }

    private  void DoCancel(){
        if(updateWork!=null){
            updateWork.cancel(false);
        }
    }

    private  void SetStateLabel(String msg){

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis()) ; // 獲取當前時間

        String str = formatter.format(curDate);

        stateLabel.setText(str+":"+msg);
    }

    private void Reset(){
        stateLabel.setText("");
        progressBar.setProgress(0);
    }

    private void DoInstall(){

        File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadPath.getPath(), "UpdatedApp.apk");

        Uri fileUri = Uri.fromFile(file);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

        this.startActivity(intent);
    }

    class UpdateWork extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ResetProgressBar();
            SetStateLabel(getResources().getString(R.string.downloadWaiting));
        }

        @Override
        protected String doInBackground(String... params) {
            int count;
            try{
                URL url = new URL(params[0]);
                HttpURLConnection conection = (HttpURLConnection) url.openConnection();
                conection.setUseCaches(false);
                conection.setRequestMethod("GET");
                conection.setDoOutput(true);
                conection.connect();

                File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File outputFile = new File(downloadPath.getPath(), "UpdatedApp.apk");
                if(outputFile.exists()){
                    outputFile.delete();
                }

                /*
                FileOutputStream fos = new FileOutputStream(outputFile);
                InputStream is = conection.getInputStream();
                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                    publishProgress(100);
                }
                fos.close();
                is.close();
                */

                //Android 6下載就發生問題，待解決
                Log.d("mark","hih already readed 1");
                int lenghtOfFile = conection.getContentLength();
                Log.d("mark","hih already readed 1-1");
                InputStream input = new BufferedInputStream(url.openStream(),1024);
                //InputStream input = conection.getInputStream();
                Log.d("mark","hih already readed 1-2");
                OutputStream output = new FileOutputStream(downloadPath.toString() + "/UpdatedApp.apk");
                Log.d("mark","hih already readed 1-3");
                byte data[] = new byte[1024];
                Log.d("mark","hih already readed 2");
                long current = 0;
                while ((count = input.read(data)) != -1) {
                    current += count;
                    publishProgress((int)current/(lenghtOfFile*100));
                    output.write(data, 0, count);
                }
                output.flush();
                Log.d("mark","hih already readed 3");
                output.close();
                input.close();
            }catch (Exception e){
                Log.d("mark", "Download Error:"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            SetStateLabel(getResources().getString(R.string.downloadComplete));
            progressIcon.setVisibility(View.INVISIBLE);
            DoInstall();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressIcon.setVisibility(View.INVISIBLE);
            SetStateLabel(getResources().getString(R.string.downloadCancel));
        }
    }

}
