package com.mx.krakensoft.opencv;

import java.util.LinkedList;
import java.util.List;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import android.os.Environment;
import com.mx.krakensoft.opencv.imageProcessing.ColorBlobDetector;
import android.app.Activity;
import android.graphics.SumPathEffect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Size;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.transpose;
import static org.opencv.core.CvType.CV_8UC1;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {




    static {
        System.loadLibrary("opencv_java3");
    }
    private static final String    TAG                 = "HandPose::MainActivity";
    //public static final int        JAVA_DETECTOR       = 0;
    //public static final int        NATIVE_DETECTOR     = 1;

    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat                    mResult;
    private Mat                    mResult1;

    private TessBaseAPI tessBaseAPI;
    private CustomSufaceView       mOpenCvCameraView;
    private static final String DATA_PTAH1 = Environment.
            getExternalStorageDirectory().toString() + "/img";
    private static final String DATA_PTAH = Environment.
            getExternalStorageDirectory().toString() + "/TESS";
    private static final String TESS_DATA = "/tessdata";
    private SeekBar                minTresholdSeekbar = null;
    //private SeekBar                maxTresholdSeekbar = null;
    private TextView resultView;
    private TextView               minTresholdSeekbarText = null;
    private TextView               numberOfFingersText = null;
    public static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};

    double iThreshold = 0;
    private Scalar                 mBlobColorHsv;
    private Scalar                 mBlobColorRgba;
    private ColorBlobDetector      mDetector;
    private Mat                    mSpectrum;
    private boolean				mIsColorSelected = false;
    private boolean                recordEnable = false;

    private float                  distance_to_disconnect = 100;

    private Size                   blur_kernel_size;
    private Size                   open_kernel_size;
    private Size                   close_kernel_size;


    private Size                   SPECTRUM_SIZE;
    private Scalar                 CONTOUR_COLOR;
    private Scalar                 CONTOUR_COLOR_WHITE;
    private List<Point> Writing_Pattern = new LinkedList<Point>(); //to save trace that finger move
    private ArrayList<String>  Writing_letter= new ArrayList<String>();

    final Handler mHandler = new Handler();
    int numberOfFingers = 0;

    final Runnable mUpdateFingerCountResults = new Runnable() {
        public void run() {
            updateNumberOfFingers();
        }
    };

    /**************************************初始化opencv库***********************************************/
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);

                    // 640x480
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /****************************************构造函数**************************************************/
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    private boolean isPermissionGranted(String permission) {
        return ActivityCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /*******************************************获取权限***********************************************/
    private String[] findUnGrantedPermissions(String[] permissions) {
        List<String> unGrantedPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (!isPermissionGranted(permission)) {
                unGrantedPermissionList.add(permission);
            }
        }
        return unGrantedPermissionList.toArray(new String[0]);
    }
    private void requestUnGrantedPermissions(String[] permissions, int requestCode) {
        String[] unGrantedPermissions = findUnGrantedPermissions(permissions);
        if (unGrantedPermissions.length == 0) {
            //System.out.println("-------------->ok all permission granted!");
            //ok start the activity!
        }
        else {
            //System.out.println("-------------->some permission art not granted, need to request!");
            ActivityCompat.requestPermissions(MainActivity.this, unGrantedPermissions,
                    requestCode);
        }
    }
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean allPermissionGranted = true;
        if (requestCode == 1) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionGranted = false;
                        break;
                    }
                }
                if(allPermissionGranted){
                    //System.out.println("----------> request has been all granted!");
                }
                else {
                    //System.out.println("----------> some request still not be granted");
                }
            }
        }
    }

    public String ArrayList2String(ArrayList<String> arrayList) {
        String result = "";
        if (arrayList != null && arrayList.size() > 0) {
            for (String item : arrayList) {
                // 把列表中的每条数据用逗号分割开来，然后拼接成字符串
                result += item ;
            }
        }
        return result;
    }
    /** ********************Called when the activity is first created. ********************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main_surface_view);
        requestUnGrantedPermissions(PERMISSIONS, 1);
        /*if (!OpenCVLoader.initDebug()) {
            Log.e("Test","man");
        }else{
        }*/
        File dir = new File(DATA_PTAH1);
        if (!dir.exists()) {dir.mkdirs();}
        //***************按下按钮一clear则清空所有绘图*****************//
        Button button1=(Button)findViewById(R.id.button_1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Writing_Pattern.clear();
            }
        });
        //***************按下按钮二则进行识别*****************//
        Button button2=(Button)findViewById(R.id.button_2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultView = (TextView) findViewById(R.id.textViewOfResult);
                mResult1=new Mat(mResult.cols()*2,mResult.rows()*2,CV_8UC1);
                for(int i=1;i<Writing_Pattern.size();i++){
                    Point q = Writing_Pattern.get(i);
                    Point w = Writing_Pattern.get(i-1);
                    //if(((w.x - q.x)*(w.x - q.x) + (w.y - q.y)*(w.y - q.y)) < distance_to_disconnect){
                        //这里用欧氏距离简单判断一下前后两个点是否是同一笔画，distance_to_disconnect是阈值，值待定
                        Imgproc.line(mResult,w,q, new Scalar(255),30);
                    //}
                }
                //处理绘制出的轨迹
                //Imgproc.GaussianBlur(mResult, mResult, blur_kernel_size, 1);
                //Mat KERNEL_CLOSE = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, close_kernel_size);
                //Imgproc.morphologyEx(mResult, mResult, Imgproc.MORPH_CLOSE, KERNEL_CLOSE);
                //Mat KERNEL_OPEN = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, open_kernel_size);
                //Imgproc.morphologyEx(mResult, mResult, Imgproc.MORPH_OPEN, KERNEL_OPEN);

                Mat matrotate=Imgproc.getRotationMatrix2D(new Point(mResult.rows()/2,mResult.cols()/2), 270, 1);
                Imgproc.warpAffine(mResult,mResult1,matrotate,mResult1.size());
                Imgcodecs.imwrite(DATA_PTAH+File.separator+"img.jpg",mResult1);
                prepareTessData();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 7;
                Bitmap bitmap = BitmapFactory.decodeFile(DATA_PTAH+File.separator+"img.jpg", options);

                String result = getText(bitmap);
                Writing_letter.add(result);
                result=ArrayList2String(Writing_letter);
                resultView.setText(result);
                Writing_Pattern.clear();
            }
        });
        Button button3=(Button)findViewById(R.id.button_3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Writing_letter.size()>=1){
                Writing_letter.remove(Writing_letter.size()-1);
                String result=ArrayList2String(Writing_letter);
                resultView.setText(result);}
            }});
        Button button4=(Button)findViewById(R.id.button_4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Writing_letter.size()>=1){
                    Writing_letter.add(" ");
                    String result=ArrayList2String(Writing_letter);
                    resultView.setText(result);}
            }});

        mOpenCvCameraView = (CustomSufaceView) findViewById(R.id.main_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        minTresholdSeekbarText = (TextView) findViewById(R.id.textView3);
        numberOfFingersText = (TextView) findViewById(R.id.numberOfFingers);
        minTresholdSeekbar = (SeekBar)findViewById(R.id.seekBar1);
        minTresholdSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            int progressChanged = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
            }
        });
        minTresholdSeekbar.setProgress(20000);
    }


    private void prepareTessData() {
        try {
            File dir = new File(DATA_PTAH + TESS_DATA);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String[] fileList = getAssets().list("");
            //System.out.print("--------->getAsset: " + getAssets().toString());
            //System.out.print("--------->filelist: " + fileList.toString());


            for (String fileName : fileList) {
                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                if(!fileName.endsWith("traineddata")) continue;
                String pathToDataFile = DATA_PTAH + TESS_DATA + File.separator + fileName;
                //System.out.println("--------->input filename:" + pathToDataFile);

                if (!(new File(pathToDataFile)).exists()) {
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }


    /*******************初始化opencv，免opencv manager关键代码**************************/
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /*****************************获取相机预览窗*****************************************/
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mResult = new Mat();
        /*
        mResolutionList = mOpenCvCameraView.getResolutionList();
        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            Log.i(TAG, "Resolution Option ["+Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString()+"]");
        }

        Size resolution = mResolutionList.get(7);
        mOpenCvCameraView.setResolution(resolution);
        resolution = mOpenCvCameraView.getResolution();
        String caption = "Resolution "+ Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
        Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        */
        Camera.Size resolution = mOpenCvCameraView.getResolution();
        String caption = "Resolution "+ Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
        //Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        Camera.Parameters cParams = mOpenCvCameraView.getParameters();
        cParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mOpenCvCameraView.setParameters(cParams);
        //Toast.makeText(this, "Focus mode : "+cParams.getFocusMode(), Toast.LENGTH_SHORT).show();

        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        CONTOUR_COLOR_WHITE = new Scalar(255,255,255,255);

    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            e.printStackTrace();
        }
        tessBaseAPI.init(DATA_PTAH, "eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "no result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            e.printStackTrace();
        }
        tessBaseAPI.end();
        return retStr;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset; //x和y是触摸点在算法中的坐标
        //将实际坐标转化为算法坐标
        //Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();//绘制矩形

        touchedRect.x = (x>5) ? x-5 : 0;
        touchedRect.y = (y>5) ? y-5 : 0;

        touchedRect.width = (x+5 < cols) ? x + 5 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+5 < rows) ? y + 5 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);//把触摸区域所限定的RGB图像转化为HSV图像

        // 计算触碰区域在hsv空间下的平均颜色
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);



        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;
       // Log.i(TAG, "the value of H is" +mBlobColorHsv.val[0]+"");
       // Log.i(TAG, "the value of S is" +mBlobColorHsv.val[1]+"");
        //Log.i(TAG, "the value of V is" +mBlobColorHsv.val[2]+"");


        //对触摸区域的颜色进行判断，若在肤色区间内才进行下一步操作
        //if((mBlobColorHsv.val[0]>15)&&(mBlobColorHsv.val[0]<35)&&(mBlobColorHsv.val[1]>5&&mBlobColorHsv.val[1]<150)&&(mBlobColorHsv.val[2]>75)&&(mBlobColorHsv.val[2]<190))
        //{
            //mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);           //重新转换为RGB空间

            //Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
            //      ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

            mDetector.setHsvColor(mBlobColorHsv);

            Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

            mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        mResult = new Mat(mGray.rows(),mGray.cols(),CV_8UC1);
        Mat dst = new Mat();
        Mat rotateMat = Imgproc.getRotationMatrix2D(new Point(mRgba.rows()/2,mRgba.cols()/2), -90, 1);



        iThreshold = minTresholdSeekbar.getProgress();

        //Imgproc.blur(mRgba, mRgba, new Size(5,5));
        Imgproc.GaussianBlur(mRgba, mRgba, new org.opencv.core.Size(3, 3), 1, 1);
        //Imgproc.medianBlur(mRgba, mRgba, 3);

        if (!mIsColorSelected)
        {
            Imgproc.warpAffine(mRgba, dst, rotateMat, dst.size());
            return dst;}

        List<MatOfPoint> contours = mDetector.getContours();
        mDetector.process(mRgba);

        //Log.d(TAG, "Contours count: " + contours.size());

        if (contours.size() <= 0) {
            Imgproc.warpAffine(mRgba, dst, rotateMat, dst.size());
            return dst;
        }

        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0)	.toArray()));

        double boundWidth = rect.size.width;
        double boundHeight = rect.size.height;
        int boundPos = 0;

        for (int i = 1; i < contours.size(); i++) {
            rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
            if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
                boundWidth = rect.size.width;
                boundHeight = rect.size.height;
                boundPos = i;
            }
        }

        Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));

        //Imgproc.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0 );


        Log.d(TAG,
                " Row start ["+
                        (int) boundRect.tl().y + "] row end ["+
                        (int) boundRect.br().y+"] Col start ["+
                        (int) boundRect.tl().x+"] Col end ["+
                        (int) boundRect.br().x+"]");

        int rectHeightThresh = 0;
        double a = boundRect.br().y - boundRect.tl().y;
        a = a * 1;
        a = boundRect.tl().y + a;

        //Log.d(TAG, " A ["+a+"] br y - tl y = ["+(boundRect.br().y - boundRect.tl().y)+"]");

        //Core.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR, 2, 8, 0 );
        Imgproc.rectangle( mRgba, boundRect.tl(), new Point(boundRect.br().x, a*1.1), CONTOUR_COLOR, 2, 8, 0 );  //给手部区域画上红色方框

        MatOfPoint2f pointMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
        contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

        MatOfInt hull = new MatOfInt();
        MatOfInt4 convexDefect = new MatOfInt4();
        Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);

        if(hull.toArray().length < 3)
        {
            Imgproc.warpAffine(mRgba, dst, rotateMat, dst.size());
            return dst;
        }

        Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos)	.toArray()), hull, convexDefect);

        List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
        List<Point> listPo = new LinkedList<Point>();
        for (int j = 0; j < hull.toList().size(); j++) {
            listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
        }

        MatOfPoint e = new MatOfPoint();
        e.fromList(listPo);
        hullPoints.add(e);

        List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
        List<Point> listPoDefect = new LinkedList<Point>();                //新建list用来储存指尖的点
        for (int j = 0; j < convexDefect.toList().size(); j = j+4) {
            Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j+2));
            Integer depth = convexDefect.toList().get(j+3);
            if(depth > iThreshold && farPoint.y < (a)){
                listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j+2)));
            }
            Log.d(TAG, "defects ["+j+"] " + convexDefect.toList().get(j+3));
        }

        MatOfPoint e2 = new MatOfPoint();
        e2.fromList(listPo);
        defectPoints.add(e2);

        //Log.d(TAG, "hull: " + hull.toList());
        //Log.d(TAG, "defects: " + convexDefect.toList());

        Imgproc.drawContours(mRgba, hullPoints, -1, CONTOUR_COLOR, 3);


        int defectsTotal = (int) convexDefect.total();
        //Log.d(TAG, "Defect total " + defectsTotal);

        this.numberOfFingers = listPoDefect.size();                         //手指数目为检测到指尖数目的多少
        if(this.numberOfFingers > 5) this.numberOfFingers = 5;

        //recordenable为是否开启轨迹记录的布尔变量
        if(!recordEnable && this.numberOfFingers == 1){
            recordEnable = true;
        }
        if(recordEnable && this.numberOfFingers != 1){
            recordEnable = false;
        }

        mHandler.post(mUpdateFingerCountResults);

        //这里的ListPoDefect是检测到的手的位置 这部分代码用来记录指尖的轨迹
        if(recordEnable){
            if(listPoDefect.get(0) != null){
                if (Math.abs(listPoDefect.get(0).x - boundRect.tl().x) < 50) {
                    Writing_Pattern.add(listPoDefect.get(0));
                }//画出图案
                for(int i=1;i<Writing_Pattern.size();i++){
                    Point q=Writing_Pattern.get(i);
                    //Imgproc.circle(mRgba, q, 15, new Scalar(255, 0, 255),-1);
                    //if(i>=1) {
                        Point w = Writing_Pattern.get(i-1);
                        Imgproc.line(mRgba, w, q, new Scalar(255, 0, 255), 30);
                    //}
            }
            }
            else{
                for(int i=1;i<Writing_Pattern.size();i++){
                Point q=Writing_Pattern.get(i);
                Point w = Writing_Pattern.get(i-1);
                Imgproc.line(mRgba, w, q, new Scalar(255, 0, 255), 30);

            }


            }
        }
        else {
            for (Point p : listPoDefect) {
                Imgproc.circle(mRgba, p, 15, new Scalar(255, 0, 255));
            }
            for(int i=1;i<Writing_Pattern.size();i++){
                Point q=Writing_Pattern.get(i);

                Point w = Writing_Pattern.get(i-1);
                Imgproc.line(mRgba, w, q, new Scalar(255, 0, 255), 30);

            }

        }
        //Mat dst = new Mat();
        //Mat rotateMat = Imgproc.getRotationMatrix2D(new Point(mRgba.rows()/2,mRgba.cols()/2), 90, 1);
        Imgproc.warpAffine(mRgba, dst, rotateMat, dst.size());
        return dst;

    }

    public void updateNumberOfFingers(){
        numberOfFingersText.setText(String.valueOf(this.numberOfFingers));
    }
}
