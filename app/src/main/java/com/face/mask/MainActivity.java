package com.face.mask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {


    public static final int OPEN_GALLERY_REQUEST_CODE = 0;
    public static final int REQUEST_LOAD_MODEL = 0;
    public static final int REQUEST_RUN_MODEL = 1;
    public static final int RESPONSE_LOAD_MODEL_FAILED = 1;
    public static final int RESPONSE_LOAD_MODEL_SUCCESSED = 0;
    public static final int RESPONSE_RUN_MODEL_FAILED = 3;
    public static final int RESPONSE_RUN_MODEL_SUCCESSED = 2;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int TAKE_PHOTO_REQUEST_CODE = 1;
    protected String cpuPowerMode = "";
    protected int cpuThreadNum = 1;
    protected String imagePath = "";
    protected String inputColorFormat = "";
    protected float[] inputMean = new float[0];
    protected long[] inputShape = new long[0];
    protected float[] inputStd = new float[0];
    protected ImageView ivInputImage;
    protected String labelPath = "";
    protected String modelPath = "";
    protected ProgressDialog pbLoadModel = null;
    protected ProgressDialog pbRunModel = null;
    protected Predictor predictor = new Predictor();
    protected Handler receiver = null;
    protected float scoreThreshold = 0.5F;
    protected Handler sender = null;
    protected TextView tvInferenceTime;
    protected TextView tvInputSetting;
    protected TextView tvOutputResult;
    protected HandlerThread worker = null;


    private void openGallery() {
        Intent localIntent = new Intent("android.intent.action.PICK", null);
        localIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(localIntent, 0);
    }

    private boolean requestAllPermissions() {
        if ((ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0) && (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == 0)) {
            return true;
        }
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.CAMERA"}, 0);
        return false;
    }

    private void takePhoto() {
        Intent localIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (localIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(localIntent, 1);
        }
    }

    public void loadModel() {
        this.pbLoadModel = ProgressDialog.show(this, "", "Loading model...", false, false);
        this.sender.sendEmptyMessage(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case OPEN_GALLERY_REQUEST_CODE:
                    try {
                        ContentResolver resolver = getContentResolver();
                        Uri uri = data.getData();
                        Bitmap image = MediaStore.Images.Media.getBitmap(resolver, uri);
                        String[] proj = {MediaStore.Images.Media.DATA};
                        Cursor cursor = managedQuery(uri, proj, null, null, null);
                        cursor.moveToFirst();
                        onImageChanged(image);
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    break;

                case TAKE_PHOTO_REQUEST_CODE:
                    Bitmap image = (Bitmap) data.getParcelableExtra("data");
                    onImageChanged(image);

                    break;
                default:
                    break;
            }
        }
    }


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.receiver = new Handler() {
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case RESPONSE_LOAD_MODEL_SUCCESSED:
                        pbLoadModel.dismiss();
                        onLoadModelSuccessed();
                        break;
                    case RESPONSE_LOAD_MODEL_FAILED:
                        pbLoadModel.dismiss();
                        Toast.makeText(MainActivity.this, "Load model failed!", Toast.LENGTH_SHORT).show();
                        onLoadModelFailed();
                        break;
                    case RESPONSE_RUN_MODEL_SUCCESSED:
                        pbRunModel.dismiss();
                        onRunModelSuccessed();
                        break;
                    case RESPONSE_RUN_MODEL_FAILED:
                        pbRunModel.dismiss();
                        Toast.makeText(MainActivity.this, "Run model failed!", Toast.LENGTH_SHORT).show();
                        onRunModelFailed();
                        break;
                    default:
                        break;
                }
            }
        };
        worker = new HandlerThread("Predictor Worker");
        worker.start();
        this.sender = new Handler(this.worker.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REQUEST_LOAD_MODEL:
                        // load model and reload test image
                        if (onLoadModel()) {
                            receiver.sendEmptyMessage(RESPONSE_LOAD_MODEL_SUCCESSED);
                        } else {
                            receiver.sendEmptyMessage(RESPONSE_LOAD_MODEL_FAILED);
                        }
                        break;
                    case REQUEST_RUN_MODEL:
                        // run model if model is loaded
                        if (onRunModel()) {
                            receiver.sendEmptyMessage(RESPONSE_RUN_MODEL_SUCCESSED);
                        } else {
                            receiver.sendEmptyMessage(RESPONSE_RUN_MODEL_FAILED);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        tvInputSetting = findViewById(R.id.tv_input_setting);
        ivInputImage = findViewById(R.id.iv_input_image);
        tvInferenceTime = findViewById(R.id.tv_inference_time);
        tvOutputResult = findViewById(R.id.tv_output_result);
        tvInputSetting.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvOutputResult.setMovementMethod(ScrollingMovementMethod.getInstance());


        // Example of a call to a native method
//        TextView tv = findViewById(R.id.sample_text);
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round, null);
//        Mat src = new Mat();
//        Utils.bitmapToMat(bitmap, src);
//
//        tv.setText(checkMask(src.getNativeObjAddr()));
    }


    public boolean onCreateOptionsMenu(Menu paramMenu) {
        getMenuInflater().inflate(R.menu.menu_action_options, paramMenu);
        return true;
    }

    protected void onDestroy() {
        Predictor localPredictor = this.predictor;
        if (localPredictor != null) {
            localPredictor.releaseModel();
        }
        this.worker.quit();
        super.onDestroy();
    }

    public void onImageChanged(Bitmap paramBitmap) {
        if ((paramBitmap != null) && (this.predictor.isLoaded())) {
            this.predictor.setInputImage(paramBitmap);
            runModel();
        }
    }

    public boolean onLoadModel() {
        return this.predictor.init(this, modelPath, cpuThreadNum, labelPath, inputColorFormat,
                inputShape, inputMean, inputStd, scoreThreshold);
    }

    public void onLoadModelFailed() {
    }

    public void onLoadModelSuccessed() {
        try {
            if (this.imagePath.isEmpty()) {
                return;
            }
            Bitmap localBitmap;
            if (!this.imagePath.substring(0, 1).equals("/")) {
                localBitmap = BitmapFactory.decodeStream(getAssets().open(this.imagePath));
            } else {
                if (!new File(this.imagePath).exists()) {
                    return;
                }
                localBitmap = BitmapFactory.decodeFile(this.imagePath);
            }
            if ((localBitmap != null) && (this.predictor.isLoaded())) {
                this.predictor.setInputImage(localBitmap);
                runModel();
            }
            return;
        } catch (IOException localIOException) {
            Toast.makeText(this, "Load image failed!", Toast.LENGTH_SHORT).show();
            localIOException.printStackTrace();
        }
    }

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        switch (paramMenuItem.getItemId()) {
            default:
                break;
            case R.id.take_photo:
                if (requestAllPermissions()) {
                    takePhoto();
                }
                break;
            case R.id.settings:
                if (requestAllPermissions()) {
                    onSettingsClicked();
                }
                break;
            case R.id.open_gallery:
                if (requestAllPermissions()) {
                    openGallery();
                }
                break;
            case R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(paramMenuItem);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isLoaded = this.predictor.isLoaded();
        menu.findItem(R.id.open_gallery).setEnabled(isLoaded);
        menu.findItem(R.id.take_photo).setEnabled(isLoaded);
        return super.onPrepareOptionsMenu(menu);
    }

    public void onRequestPermissionsResult(int paramInt, String[] paramArrayOfString, int[] paramArrayOfInt) {
        super.onRequestPermissionsResult(paramInt, paramArrayOfString, paramArrayOfInt);
        if ((paramArrayOfInt[0] != 0) || (paramArrayOfInt[1] != 0)) {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean settingsChanged = false;
        String model_path = sharedPreferences.getString(getString(R.string.MODEL_PATH_KEY),
                getString(R.string.MODEL_PATH_DEFAULT));
        String label_path = sharedPreferences.getString(getString(R.string.LABEL_PATH_KEY),
                getString(R.string.LABEL_PATH_DEFAULT));
        String image_path = sharedPreferences.getString(getString(R.string.IMAGE_PATH_KEY),
                getString(R.string.IMAGE_PATH_DEFAULT));
        int cpu_thread_num = Integer.parseInt(sharedPreferences.getString(getString(R.string.CPU_THREAD_NUM_KEY),
                getString(R.string.CPU_THREAD_NUM_DEFAULT)));
        String cpu_power_mode =
                sharedPreferences.getString(getString(R.string.CPU_POWER_MODE_KEY),
                        getString(R.string.CPU_POWER_MODE_DEFAULT));
        String input_color_format =
                sharedPreferences.getString(getString(R.string.INPUT_COLOR_FORMAT_KEY),
                        getString(R.string.INPUT_COLOR_FORMAT_DEFAULT));
        long[] input_shape =
                Utils.parseLongsFromString(sharedPreferences.getString(getString(R.string.INPUT_SHAPE_KEY),
                        getString(R.string.INPUT_SHAPE_DEFAULT)), ",");
        float[] input_mean =
                Utils.parseFloatsFromString(sharedPreferences.getString(getString(R.string.INPUT_MEAN_KEY),
                        getString(R.string.INPUT_MEAN_DEFAULT)), ",");
        float[] input_std =
                Utils.parseFloatsFromString(sharedPreferences.getString(getString(R.string.INPUT_STD_KEY)
                        , getString(R.string.INPUT_STD_DEFAULT)), ",");


        this.modelPath = model_path;
        this.labelPath = label_path;
        this.imagePath = image_path;
        this.cpuThreadNum = cpu_thread_num;
        this.cpuPowerMode = cpu_power_mode;
        this.inputColorFormat = input_color_format;
        this.inputShape = input_shape;
        this.inputMean = input_mean;
        this.inputStd = input_std;
//        this.scoreThreshold = f;
//        localObject1 = this.tvInputSetting;
        tvInputSetting.setText("Model: " + modelPath.substring(modelPath.lastIndexOf("/") + 1) + "\n" + "CPU" +
                " Thread Num: " + Integer.toString(cpuThreadNum) + "\n" + "CPU Power Mode: " + cpuPowerMode);
        tvInputSetting.scrollTo(0, 0);
        loadModel();
        return;

    }

    public boolean onRunModel() {
        return (this.predictor.isLoaded()) && (this.predictor.runModel());
    }

    public void onRunModelFailed() {
    }

    public void onRunModelSuccessed() {
        Object localObject = this.tvInferenceTime;
        StringBuilder localStringBuilder = new StringBuilder();
        localStringBuilder.append("Inference time: ");
        localStringBuilder.append(this.predictor.inferenceTime());
        localStringBuilder.append(" ms");
        ((TextView) localObject).setText(localStringBuilder.toString());
        localObject = this.predictor.outputImage();
        if (localObject != null) {
            this.ivInputImage.setImageBitmap((Bitmap) localObject);
        }
        this.tvOutputResult.setText(this.predictor.outputResult());
        this.tvOutputResult.scrollTo(0, 0);
    }

    public void onSettingsClicked() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void runModel() {
        this.pbRunModel = ProgressDialog.show(this, "", "Running model...", false, false);
        this.sender.sendEmptyMessage(1);
    }


}

