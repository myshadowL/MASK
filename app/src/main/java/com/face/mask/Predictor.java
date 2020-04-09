package com.face.mask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.baidu.paddle.lite.MobileConfig;
import com.baidu.paddle.lite.PaddlePredictor;
import com.baidu.paddle.lite.PowerMode;
import com.baidu.paddle.lite.Tensor;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Vector;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

public class Predictor {

    private static final String TAG = Predictor.class.getSimpleName();
    public String cpuPowerMode = "LITE_POWER_HIGH";
    public int cpuThreadNum = 1;
    public int inferIterNum = 1;
    protected float inferenceTime = 0.0F;
    protected String inputColorFormat = "RGB";
    protected Context appCtx = null;
    protected Bitmap inputImage = null;
    protected float[] inputMean = {0.485F, 0.456F, 0.406F};
    protected long[] inputShape = {1L, 3L, 320L, 320L};
    protected float[] inputStd = {0.229F, 0.224F, 0.225F};
    public boolean isLoaded = false;
    public String modelName = "";
    public String modelPath = "";
    protected Bitmap outputImage = null;
    protected String outputResult = "";
    protected PaddlePredictor paddlePredictor = null;
    protected float postprocessTime = 0.0F;
    protected float preprocessTime = 0.0F;
    protected float scoreThreshold = 0.5F;
    public int warmupIterNum = 1;
    protected Vector<String> wordLabels = new Vector();

    public String cpuPowerMode() {
        return this.cpuPowerMode;
    }

    public int cpuThreadNum() {
        return this.cpuThreadNum;
    }

    public Tensor getInput(int idx) {
        if (!isLoaded()) {
            return null;
        }
        return paddlePredictor.getInput(idx);
    }

    public Tensor getOutput(int idx) {
        if (!isLoaded()) {
            return null;
        }
        return this.paddlePredictor.getOutput(idx);
    }

    public float inferenceTime() {
        return this.inferenceTime;
    }

    public boolean init(Context appCtx, String model_path, int cpuThreadNum, String labelPath,
                        String inputColorFormat, long[] inputShape, float[] inputMean,
                        float[] inputStd, float paramFloat) {
        if (inputShape.length != 4) {
            Log.i(TAG, "Size of input shape should be: 4");
            return false;
        }
        if (inputMean.length != inputShape[1]) {
            Log.i(TAG, "size of input mean should be: " + Long.toString(inputShape[1]));
            return false;
        }
        if (inputStd.length != inputShape[1]) {
            Log.i(TAG, "size of input std should be: " + Long.toString(inputShape[1]));
            return false;
        }
        if (inputShape[0] != 1) {
            Log.i(TAG, "Only one batch is supported in the image classification demo, you can use any batch size in your Apps!");
            return false;
        }
        if ((inputShape[1] != 1) && (inputShape[1] != 3)) {
            Log.i(TAG, "Only one/three channels are supported in the image classification demo, you can use any channel size in your Apps!");
            return false;
        }
        if ((!inputColorFormat.equalsIgnoreCase("RGB")) && (!inputColorFormat.equalsIgnoreCase("BGR"))) {
            Log.i(TAG, "Only RGB and BGR color format is supported.");
            return false;
        }
        boolean bool = loadModel(appCtx, model_path, cpuThreadNum, cpuPowerMode);
        this.isLoaded = bool;
        if (!bool) {
            return false;
        }
        bool = loadLabel(appCtx, labelPath);
        this.isLoaded = bool;
        if (!bool) {
            return false;
        }
        this.inputColorFormat = inputColorFormat;
        this.inputShape = inputShape;
        this.inputMean = inputMean;
        this.inputStd = inputStd;
        this.scoreThreshold = paramFloat;
        return true;
    }

    public Bitmap inputImage() {
        return this.inputImage;
    }

    public boolean isLoaded() {
        return (this.paddlePredictor != null) && (this.isLoaded);
    }

    protected boolean loadLabel(Context appCtx, String labelPath) {
        this.wordLabels.clear();
        try {
            InputStream assetsInputStream = appCtx.getAssets().open(labelPath);
            int available = assetsInputStream.available();
            byte[] lines = new byte[available];
            assetsInputStream.read(lines);
            assetsInputStream.close();
            String[] contents = new String(lines).split("\n");
            for (String content : contents) {
                wordLabels.add(content);
            }
            Log.i(TAG, "word label size: " + wordLabels.size());
            return true;
        } catch (Exception paramContext) {
            Log.e(TAG, paramContext.getMessage());
        }
        return false;
    }

    protected boolean loadModel(Context appCtx, String modelPath, int cpuThreadNum, String cpuPowerMode) {
        releaseModel();
        if (modelPath.isEmpty()) {
            return false;
        }
        String realPath = modelPath;
        if (!modelPath.substring(0, 1).equals("/")) {
            realPath = appCtx.getCacheDir() + "/" + modelPath;
            Utils.copyDirectoryFromAssets(appCtx, modelPath, realPath);
        }
        if (realPath.isEmpty()) {
            return false;
        }
        MobileConfig config = new MobileConfig();
        config.setModelFromFile(realPath + File.separator + "model.nb");
        config.setThreads(cpuThreadNum);
        if (cpuPowerMode.equalsIgnoreCase("LITE_POWER_HIGH")) {
            config.setPowerMode(PowerMode.LITE_POWER_HIGH);
        } else if (cpuPowerMode.equalsIgnoreCase("LITE_POWER_LOW")) {
            config.setPowerMode(PowerMode.LITE_POWER_LOW);
        } else if (cpuPowerMode.equalsIgnoreCase("LITE_POWER_FULL")) {
            config.setPowerMode(PowerMode.LITE_POWER_FULL);
        } else if (cpuPowerMode.equalsIgnoreCase("LITE_POWER_NO_BIND")) {
            config.setPowerMode(PowerMode.LITE_POWER_NO_BIND);
        } else if (cpuPowerMode.equalsIgnoreCase("LITE_POWER_RAND_HIGH")) {
            config.setPowerMode(PowerMode.LITE_POWER_RAND_HIGH);
        } else if (cpuPowerMode.equalsIgnoreCase("LITE_POWER_RAND_LOW")) {
            config.setPowerMode(PowerMode.LITE_POWER_RAND_LOW);
        } else {
            Log.e(TAG, "unknown cpu power mode!");
            return false;
        }
        paddlePredictor = PaddlePredictor.createPaddlePredictor(config);

        this.cpuThreadNum = cpuThreadNum;
        this.cpuPowerMode = cpuPowerMode;
        this.modelPath = realPath;
        this.modelName = realPath.substring(realPath.lastIndexOf("/") + 1);
        return true;
    }

    public String modelName() {
        return this.modelName;
    }

    public String modelPath() {
        return this.modelPath;
    }

    public Bitmap outputImage() {
        return this.outputImage;
    }

    public String outputResult() {
        return this.outputResult;
    }

    public float postprocessTime() {
        return this.postprocessTime;
    }

    public float preprocessTime() {
        return this.preprocessTime;
    }

    public void releaseModel() {
        this.paddlePredictor = null;
        this.isLoaded = false;
        this.cpuThreadNum = 1;
        this.cpuPowerMode = "LITE_POWER_HIGH";
        this.modelPath = "";
        this.modelName = "";
    }

    public boolean runModel() {
        if (inputImage == null) {
            return false;
        }
        // set input shape
        Tensor inputTensor = getInput(0);
        inputTensor.resize(inputShape);

        Tensor inputTensor1 = getInput(1);
        inputTensor1.resize(new long[]{1L, 2L});

        Date start = new Date();
        int channels = (int) inputShape[1];
        int width = (int) inputShape[3];
        int height = (int) inputShape[2];
        float[] inputData = new float[channels * width * height];
        float f1;
        Object localObject9;
        float f2;
        if (channels == 3) {
            int[] channelIdx = null;
            if (inputColorFormat.equalsIgnoreCase("RGB")) {
                channelIdx = new int[]{0, 1, 2};
            } else if (inputColorFormat.equalsIgnoreCase("BGR")) {
                channelIdx = new int[]{2, 1, 0};
            } else {
                Log.i(TAG, "Unknown color format " + inputColorFormat + ", only RGB and BGR color format is " +
                        "supported!");
                return false;
            }
            int[] channelStride = new int[]{width * height, width * height * 2};
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = inputImage.getPixel(x, y);
                    float[] rgb = new float[]{(float) red(color) / 255.0f, (float) green(color) / 255.0f,
                            (float) blue(color) / 255.0f};
                    inputData[y * width + x] = (rgb[channelIdx[0]] - inputMean[0]) / inputStd[0];
                    inputData[y * width + x + channelStride[0]] = (rgb[channelIdx[1]] - inputMean[1]) / inputStd[1];
                    inputData[y * width + x + channelStride[1]] = (rgb[channelIdx[2]] - inputMean[2]) / inputStd[2];
                }
            }
        } else if (channels == 1) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = inputImage.getPixel(x, y);
                    float gray = (float) (red(color) + green(color) + blue(color)) / 3.0f / 255.0f;
                    inputData[y * width + x] = (gray - inputMean[0]) / inputStd[0];
                }
            }
        } else {
            Log.i(TAG, "Unsupported channel size " + Integer.toString(channels) + ",  only channel 1 and 3 is " +
                    "supported!");
            return false;
        }
        inputTensor.setData(inputData);
        inputTensor1.setData(new int[]{320, 320});
        Date end = new Date();
        preprocessTime = (float) (end.getTime() - start.getTime());
        // Warm up
        for (int i = 0; i < warmupIterNum; i++) {
            paddlePredictor.run();
        }
        // Run inference
        start = new Date();
        for (int i = 0; i < inferIterNum; i++) {
            paddlePredictor.run();
        }
        end = new Date();
        inferenceTime = (end.getTime() - start.getTime()) / (float) inferIterNum;

        // Fetch output tensor
        Tensor outputTensor = getOutput(0);

        // Post-process
        start = new Date();
        long outputShape[] = outputTensor.shape();
        long outputSize = 1;
        for (long s : outputShape) {
            outputSize *= s;
        }

        outputImage = inputImage;
        outputResult = new String();
        Canvas canvas = new Canvas(outputImage);
        Paint rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(1);
        Paint txtPaint = new Paint();
        txtPaint.setTextSize(12);
        txtPaint.setAntiAlias(true);
        int txtXOffset = 4;
        int txtYOffset = (int) (Math.ceil(-txtPaint.getFontMetrics().ascent));
        int imgWidth = outputImage.getWidth();
        int imgHeight = outputImage.getHeight();
        int objectIdx = 0;
        final int[] objectColor = {0xFFFF00CC, 0xFFFF0000, 0xFFFFFF33, 0xFF0000FF, 0xFF00FF00,
                0xFF000000, 0xFF339933};
        for (int i = 0; i < outputSize; i += 6) {
            float score = outputTensor.getFloatData()[i + 1];
            if (score < scoreThreshold) {
                continue;
            }
            int categoryIdx = (int) outputTensor.getFloatData()[i];
            String categoryName = "Unknown";
            if (wordLabels.size() > 0 && categoryIdx >= 0 && categoryIdx < wordLabels.size()) {
                categoryName = wordLabels.get(categoryIdx);
            }
            float rawLeft = outputTensor.getFloatData()[i + 2];
            float rawTop = outputTensor.getFloatData()[i + 3];
            float rawRight = outputTensor.getFloatData()[i + 4];
            float rawBottom = outputTensor.getFloatData()[i + 5];
            float clampedLeft = Math.max(Math.min(rawLeft, 1.f), 0.f);
            float clampedTop = Math.max(Math.min(rawTop, 1.f), 0.f);
            float clampedRight = Math.max(Math.min(rawRight, 1.f), 0.f);
            float clampedBottom = Math.max(Math.min(rawBottom, 1.f), 0.f);
            float imgLeft = clampedLeft * imgWidth;
            float imgTop = clampedTop * imgWidth;
            float imgRight = clampedRight * imgHeight;
            float imgBottom = clampedBottom * imgHeight;
            int color = objectColor[objectIdx % objectColor.length];
            rectPaint.setColor(color);
            txtPaint.setColor(color);
            canvas.drawRect(imgLeft, imgTop, imgRight, imgBottom, rectPaint);
            canvas.drawText(objectIdx + "." + categoryName + ":" + String.format("%.3f", score),
                    imgLeft + txtXOffset, imgTop + txtYOffset, txtPaint);
            outputResult += objectIdx + "." + categoryName + " - " + String.format("%.3f", score) +
                    " [" + String.format("%.3f", rawLeft) + "," + String.format("%.3f", rawTop) + "," + String.format("%.3f", rawRight) + "," + String.format("%.3f", rawBottom) + "]\n";
            objectIdx++;
        }
        end = new Date();
        postprocessTime = (float) (end.getTime() - start.getTime());
        return true;
    }

    public void setInputImage(Bitmap paramBitmap) {
        if (paramBitmap == null) {
            return;
        }
        paramBitmap = paramBitmap.copy(Bitmap.Config.ARGB_8888, true);
        long[] arrayOfLong = this.inputShape;
        this.inputImage = Bitmap.createScaledBitmap(paramBitmap, (int) arrayOfLong[3], (int) arrayOfLong[2], true);
    }
}
