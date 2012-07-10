package ru.st1ng.vk.activity;

import java.io.File;
import java.util.ArrayList;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.PhotoAttach;
import ru.st1ng.vk.model.ServerUploadFile;
import ru.st1ng.vk.model.Attachment.Type;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.GetProfileImageUploadServer;
import ru.st1ng.vk.network.async.SaveUploadProfileImageTask;
import ru.st1ng.vk.network.async.UploadProfilePhotoToServer;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.UploadProfilePhotoToServer.UploadAsyncCallback;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.ImageView;

//Hack class for use with tabHost, because we cant get result of running activity, when activity in tabhost
public class ChangePhotoActivity extends Activity {

    private String photoOutput;
    public static final String EXTRA_TAKE_PHOTO = "takephoto";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_uploading);
        ImageView updating = (ImageView) findViewById(R.id.updatingImage);
        ((AnimationDrawable)updating.getDrawable()).start();
        if (!getIntent().getBooleanExtra(EXTRA_TAKE_PHOTO, false)) {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1);

        } else {
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            photoOutput = VKApplication.getInstance().getCameraDir() + "/" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(photoOutput);
            if (!outFile.getParentFile().exists())
                outFile.getParentFile().mkdirs();
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(photoOutput)));
            startActivityForResult(takePhotoIntent, 0);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (photoOutput == null)
                return;
            File photoFile = new File(photoOutput);
            if (!photoFile.exists())
                return;
            uploadProfilePhoto(photoOutput);
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            uploadProfilePhoto(filePath);
        } else
        {
            ChangePhotoActivity.this.finish();
        }
    };

    private void uploadProfilePhoto(final String photo) {
        new GetProfileImageUploadServer(new AsyncCallback<String>() {

            @Override
            public void OnSuccess(String str) {
                final ArrayList<String> files = new ArrayList<String>();
                files.add(str);
                files.add(photo);

                new UploadProfilePhotoToServer(new UploadAsyncCallback<ServerUploadFile[]>() {

                    @Override
                    public void OnSuccess(ServerUploadFile[] str) {

                        new SaveUploadProfileImageTask(new AsyncCallback<String>() {

                            @Override
                            public void OnSuccess(String str) {
                                VKApplication.getInstance().runUpdateUser(VKApplication.getInstance().getCurrentUser());
                                VKApplication.getInstance().getCurrentUser().photo_medium = str;
                                ChangePhotoActivity.this.finish();
                            }

                            @Override
                            public void OnError(ErrorCode errorCode) {

                            }
                        }).execute(str[0]);
                    }

                    @Override
                    public void OnError(ErrorCode errorCode) {

                    }

                    @Override
                    public void OnProgress(int percent, int fileCount) {

                    }

                }).execute(files.toArray(new String[files.size()]));
            }

            @Override
            public void OnError(ErrorCode errorCode) {
                // TODO Auto-generated method stub

            }
        }).execute();
    }
}
