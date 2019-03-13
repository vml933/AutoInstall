package com.mark.autoinstall;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private String updateURL;

    private ProgressBar progressBar;
    private ProgressBar progressIcon;
    private Button btnDownloadAndUpdate;
    private Button btnUpdateLocal;
    private Button btnCancel;
    private TextView stateLabel;
    private DpdateWork downloadWork;
    private String appFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateURL = this.getResources().getString(R.string.apkUpdateUrl);

        appFileName = GetFileNameFromUrl(updateURL);

        progressBar = findViewById(R.id.ProgressBar);
        progressIcon = findViewById(R.id.ProgressIcon);
        stateLabel = findViewById(R.id.StateLabel);
        btnDownloadAndUpdate = findViewById(R.id.BtnDownloadAndUpdate);
        btnDownloadAndUpdate.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {

                if(appFileName==null || appFileName=="")
                    return;

                ResetDownloadUI();
                DoUpdate();
            }
        });

        btnUpdateLocal = findViewById(R.id.BtnUpdateLocal);
        btnUpdateLocal.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {
                DoInstall();
            }
        });

        btnCancel = findViewById(R.id.BtnCancel);
        btnCancel.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                DoCancel();
            }
        });

        AskForPermission();
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

    private void ResetDownloadUI(){
        stateLabel.setText("");
        progressBar.setMax(100);
        progressBar.setProgress(0);
        SwitchBusyIcon(true);
    }

    private void SwitchBusyIcon(boolean flag){
        int visible = flag ? View.VISIBLE: View.INVISIBLE;
        progressIcon.setVisibility(visible);
    }

    private void DoUpdate(){
        downloadWork = new DpdateWork();
        downloadWork.execute(updateURL);
    }

    private  void DoCancel(){
        if(downloadWork !=null){
            downloadWork.cancel(true);
        }
    }

    private  void SetStateLabel(String msg){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis()) ;
        String str = formatter.format(curDate);
        stateLabel.setText(str+":"+msg);
    }

    private void DoInstall(){

        File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadPath.getPath(), appFileName);
        if(!file.exists()){
            SetStateLabel(getResources().getString(R.string.fileNotExist));
            return;
        }

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

    public String GetFileNameFromUrl(String urlString) {
        return urlString.substring(urlString.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0];
    }

    class DpdateWork extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SetStateLabel(getResources().getString(R.string.downloadWaiting));
        }

        @Override
        protected String doInBackground(String... params) {

            try{

                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches(false);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                }

                File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File outputFile = new File(downloadPath.getPath(), appFileName);
                if(outputFile.exists()){
                    outputFile.delete();
                }

                int lenghtOfFile = connection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(),1024);
                OutputStream output = new FileOutputStream(downloadPath.toString() + "/"+ appFileName);
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    int percent = (int)(total*100/lenghtOfFile); //0~100
                    publishProgress(percent);
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            }catch (IOException e){
                Log.d("mark", "Download io Error:"+e.getMessage());
            }catch (SecurityException e){
                Log.d("mark", "Download security Error:"+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result==null){
                SetStateLabel(getResources().getString(R.string.downloadComplete));
                SwitchBusyIcon(false);

                DoInstall();
            }else{
                SetStateLabel(result);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            SwitchBusyIcon(false);
            SetStateLabel(getResources().getString(R.string.downloadCancel));
        }
    }

}
