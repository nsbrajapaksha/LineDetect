package com.example.nsbr.linedetect;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "nsbr";
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    public static final String FILE_NAME = "temp.jpg";

    private ImageView mMainImage;
    private int mCannyThresh1 = 10;
    private int mCannyThresh2 = 210;
    private Bitmap mBitmap = null;
    private TextView tv1, tv2, tv3;
    private Mat rectImg, img, cannyImg, cannyBlured;
    private Button processButton;
    private ProgressBar progressBar;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMainImage = (ImageView)findViewById(R.id.iv);
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);
        processButton = (Button) findViewById(R.id.btn_process);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        SeekBar seekBar1 = (SeekBar) findViewById(R.id.sb_thresh1);
        SeekBar seekBar2 = (SeekBar) findViewById(R.id.sb_thresh2);
        seekBar1.setProgress(mCannyThresh1);
        seekBar2.setProgress(mCannyThresh2);


        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                rectImg =null;
                processButton.setEnabled(true);
                tv1.setText(String.valueOf(progress));
                mCannyThresh1 = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBitmap != null) {
                    getCannyImage(mCannyThresh1, mCannyThresh2);
                }

            }
        });

        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                rectImg =null;
                processButton.setEnabled(true);
                tv2.setText(String.valueOf(progress));
                mCannyThresh2 = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBitmap != null) {
                    getCannyImage(mCannyThresh1, mCannyThresh2);
                }

            }
        });
        tv1.setText(String.valueOf(seekBar1.getProgress()));
        tv2.setText(String.valueOf(seekBar2.getProgress()));


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage("Choose a picture")
                        .setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGalleryChooser();
                            }
                        })
                        .setNegativeButton("Camera", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startCamera();
                            }
                        });
                builder.create().show();
            }
        });
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            mBitmap = null;
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                mMainImage.setImageBitmap(mBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            mBitmap = null;
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                mMainImage.setImageBitmap(mBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getCannyImage(final int thresh1, final int thresh2){

        new AsyncTask<Object, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Object... params) {
                if (mBitmap != null) {
                    Mat image = new Mat();
                    Utils.bitmapToMat(mBitmap, image);
                    img = image.clone();
                    Mat grayImg = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);
                    Imgproc.cvtColor(image, grayImg, Imgproc.COLOR_RGB2GRAY);

                    Mat thresholdImg = new Mat();
                    Imgproc.adaptiveThreshold(grayImg, thresholdImg, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 105, 5);
                    //Imgproc.threshold(grayImg, thresholdImg, 0, 255, Imgproc.THRESH_BINARY);

                    //TODO - remove later
                    //saveImage(thresholdImg);

                    Mat bluredImg = new Mat();
                    //Imgproc.medianBlur(thresholdImg, bluredImg, 5);
                    //Imgproc.blur(thresholdImg, bluredImg, new Size(3, 3));
                    Imgproc.GaussianBlur(thresholdImg, bluredImg, new Size(3, 3), 20);
                    //saveImage(bluredImg);

                    //Imgproc.dilate(bluredImg, bluredImg, Imgproc.getStructuringElement(MORPH_ELLIPSE,new  Size( 3, 3 ), new Point( 1, 1 )));
                    cannyImg = new Mat();
                    Imgproc.Canny(bluredImg, cannyImg, thresh1, thresh2);

                    //TODO - remove later
                    //saveImage(cannyImg);

                    cannyBlured = new Mat();
                    Imgproc.blur(cannyImg, cannyBlured, new Size(3, 3));

                    //detectLine(cannyBlured);

                    return convertMatToBitmap(cannyImg);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mMainImage.setImageBitmap(bitmap);
            }
        }.execute();


    }

    private Bitmap convertMatToBitmap(Mat mat){
        Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        return bmp;
    }

    /*private void detectLine(Mat cannyImg) {
        Mat LinesMat = new Mat();
        double length = 0;
        double skewAngle = 0;
        Imgproc.HoughLinesP(cannyImg, LinesMat, 1, Math.PI / 180, 40, 30, 1);
        for (int i = 0; i < LinesMat.rows(); i++) {
            double[] l = LinesMat.get(i, 0);
            Imgproc.line(img, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 0), 3);
            double angle = Math.atan2(l[3] - l[1], l[2] - l[0]) * 180.0 / Math.PI;
            if (angle < 50 && angle > -50) {
                double lineLength = (l[3] - l[1]) * (l[3] - l[1]) + (l[2] - l[0]) * (l[2] - l[0]);
                Imgproc.line(img, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 0), 3);
                *//*if (lineLength > length) {
                    length = lineLength;
                    skewAngle = angle;
                    //Imgproc.line( mImg, new Point(l[0], l[1]), new Point(l[2], l[3]), COLOR_RED, 2);
                }*//*
            }
        }
        //saveImage(img);
    }*/

    /*private void detectRects(Mat canny)
    {
        Mat hierar = new Mat();
        MatOfInt hulll = new MatOfInt();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(canny, contours, hierar, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int i = 0; i < contours.size(); i++)
        {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < 3500)
                continue;

            Imgproc.convexHull(contours.get(i), hulll);

            MatOfPoint hullContour = hull2Points(hulll, contours.get(i));
            Rect box = Imgproc.boundingRect(hullContour);
            int x1 = (int) box.tl().x;
            int y1 = (int) box.tl().y;
            int x2 = (int) box.br().x;
            int y2 = (int) box.br().y;
            Rect segRect = new Rect(x1, y1, x2, y2);

            if (box.width < 280 && box.height < 150 && box.width > 170 && box.height > 115) {

                if (y2 > canny.height()*0.77){
                    Imgproc.rectangle(img, new Point(x1, y1), new Point(x2, y2), new Scalar(255, 0, 0), 3);
                }

            }

        }
        rectImg = img.clone();

        //TODO - remove later
        saveImage(img);
    }*/

    private MatOfPoint hull2Points(MatOfInt hull, MatOfPoint contour) {
        List<Integer> indexes = hull.toList();
        List<Point> points = new ArrayList<>();
        MatOfPoint point = new MatOfPoint();
        for (Integer index : indexes) {
            points.add(contour.toList().get(index));
        }
        point.fromList(points);

        return point;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_image) {
            if (mBitmap != null) {
                mMainImage.setImageBitmap(mBitmap);
            }else Toast.makeText(MainActivity.this, "choose a picture!", Toast.LENGTH_SHORT).show();

            return true;
        }

        if (id == R.id.action_canny) {
            if (mBitmap != null) {
                getCannyImage(mCannyThresh1, mCannyThresh2);

            }else Toast.makeText(MainActivity.this, "choose a picture!", Toast.LENGTH_SHORT).show();

            return true;
        }

        if (id == R.id.action_rects_detected) {
            if (rectImg != null) {
                mMainImage.setImageBitmap(convertMatToBitmap(rectImg));
            }else
                Toast.makeText(this, "Process the image to get rect detected image!", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void saveImage(Mat mat){
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bm);
        storeImage(bm);
    }

    private File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Pictures");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        File mediaFile;
        String mImageName="MI"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public void process(View view){
        Log.i(TAG, "processing....");

        new AsyncTask<Object, Integer, Void>() {

            @Override
            protected Void doInBackground(Object... params) {
                if (cannyBlured != null) {
                    saveImage(cannyImg);
                    publishProgress(10);

                    Mat hierar = new Mat();
                    MatOfInt hulll = new MatOfInt();
                    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                    Imgproc.findContours(cannyBlured, contours, hierar, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                    publishProgress(25);
                    for (int i = 0; i < contours.size(); i++)
                    {
                        double area = Imgproc.contourArea(contours.get(i));
                        if (area < 3500)
                            continue;

                        Imgproc.convexHull(contours.get(i), hulll);

                        MatOfPoint hullContour = hull2Points(hulll, contours.get(i));
                        Rect box = Imgproc.boundingRect(hullContour);
                        int x1 = (int) box.tl().x;
                        int y1 = (int) box.tl().y;
                        int x2 = (int) box.br().x;
                        int y2 = (int) box.br().y;
                        Rect segRect = new Rect(x1, y1, x2, y2);

                        if (box.width < 280 && box.height < 150 && box.width > 170 && box.height > 115) {

                            if (y2 > cannyBlured.height()*0.77){
                                Imgproc.rectangle(img, new Point(x1, y1), new Point(x2, y2), new Scalar(255, 0, 0), 3);
                            }

                        }

                    }
                    publishProgress(70);
                    rectImg = img.clone();

                    //TODO - remove later
                    saveImage(img);

                    publishProgress(99);
                }
                return null;
            }

            @Override
            protected void onPreExecute() {
                if (cannyBlured != null) {
                    tv3.setText("Processing");
                    processButton.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                }else
                    Toast.makeText(MainActivity.this, "choose a picture!", Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (cannyBlured != null) {
                    progressBar.setVisibility(View.GONE);
                    tv3.setText("Process Finished");
                }

            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressBar.setProgress(values[0]);
            }

        }.execute();

    }
}
