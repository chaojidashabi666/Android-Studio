package com.qrs.maincarcontrolapp.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Matrix;
import android.os.Bundle;

import com.qrs.maincarcontrolapp.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PredictActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

//        Bitmap bitmap = null;
//        Module module = null;
//        try {
//            // creating bitmap from packaged into app android asset 'image.jpg',
//            // app/src/main/assets/image.jpg
//            bitmap = BitmapFactory.decodeStream(getAssets().open("2.jpg"));
////            bitmap = Bitmap.createScaledBitmap(bitmap,224,224,false);
////            bitmap = centerCrop(bitmap, 224);
//            // loading serialized torchscript module from packaged into app android asset model.pt,
//            // app/src/model/assets/model.pt
//            module = Module.load(assetFilePath(this, "model.pt"));
//        } catch (IOException e) {
//            Log.e("PytorchHelloWorld", "Error reading assets", e);
//            finish();
//        }
//
//        // showing image on UI
//        ImageView imageView = findViewById(R.id.image);
//        imageView.setImageBitmap(bitmap);
//
//        // preparing input tensor
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
//
//        // running the model
//        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
//
//        // getting tensor content as java array of floats
//        float[] scores = outputTensor.getDataAsFloatArray();
//
//        scores = softmax(scores);
//
////        System.out.println(scores);
////         使用 for 循环打印数组数据
//        for (int i = 0; i < scores.length; i++) {
//            System.out.println(scores[i]);
//        }
//
//
//        // searching for the index with maximum score
//        float maxScore = -Float.MAX_VALUE;
//        int maxScoreIdx = -1;
//        for (int i = 0; i < scores.length; i++) {
//            if (scores[i] > maxScore) {
//                maxScore = scores[i];
//                maxScoreIdx = i;
//            }
//        }
//
//        System.out.println(maxScore);
//        System.out.println(maxScoreIdx);
//        String className = ImageNetClasses.IMAGENET_CLASSES[maxScoreIdx];
//
//
//        String str = String.format("这个东西是%s的概率最大,概率为%f", className, maxScore);
//        // showing className on UI
//        TextView textView = findViewById(R.id.text);
//        textView.setText(str);
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }


    public static Bitmap centerCrop(Bitmap bitmap, int size) {
        // 获取原始图像的宽度和高度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 计算裁剪后的图像的宽度和高度
        int cropWidth = size;
        int cropHeight = size;

        // 计算裁剪区域的左上角坐标
        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;

        // 创建一个新的矩阵
        Matrix matrix = new Matrix();

        // 将裁剪区域移动到图像的左上角
        matrix.setTranslate(-x, -y);

        // 缩放图像
        matrix.postScale(cropWidth / width, cropHeight / height);

        // 创建一个新的位图
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        // 返回裁剪后的图像
        return croppedBitmap;
    }

    public static float[] softmax(float[] input) {
        // 计算 exp(x)
        float[] exp = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            exp[i] = (float) Math.exp(input[i]);
        }

        // 计算 exp(x) 的总和
        float sumExp = 0;
        for (float x : exp) {
            sumExp += x;
        }

        // 将 exp(x) 除以 exp(x) 的总和
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sumExp;
        }

        return exp;
    }

    public static class ImageNetClasses {
        public static String[] IMAGENET_CLASSES = new String[]{
                "green",
                "red",
                "yellow"
        };
    }
}