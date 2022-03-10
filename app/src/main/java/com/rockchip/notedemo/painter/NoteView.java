package com.rockchip.notedemo.painter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.RequiresApi;

import com.rockchip.notedemo.NoteJNI;
import com.rockchip.notedemo.R;
import com.rockchip.notedemo.eink.EinkManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class NoteView extends View implements PainterApi {
    private static final String TAG = "NoteView";
    private static final int PEN_WIDTH_DEFAULT = 4;
    private static final int ERASER_WIDTH_DEFAULT = 40;
    private static Context mContext;
    private static NoteJNI mNativeJNI;

    private int[] mDrawableIDs = {R.drawable.background1, R.drawable.background2, R.drawable.background3,
            R.drawable.background4, R.drawable.background5, R.drawable.background6,
            R.drawable.background7, R.drawable.background8, R.drawable.background9,
            R.drawable.background10};
    private int mDrawable = -1;

    private Canvas mCanvas = null;
    private static Bitmap mBitmap = null; //绘制当前页面使用
    private Bitmap mNullBitmap = null;
    private Bitmap mRecoveryBitmap = null; //
    private Paint mPaint;//画笔
    private Paint mEraserPaint;//橡皮擦
    private int mEraserLastX = 0;
    private int mEraserLastY = 0;
    private int mBackgroundColor = PointStruct.PEN_WHITE_COLOR;
    private BgColor mBgColor = BgColor.BG_COLOR_WHITE;
    private Pen mPen = PenFactory.createNormalPen();
    private static boolean startDrawLine = false;//start draw point
    private static ArrayList<PointStruct> mPointList;//存储所有点的数据
    private static ArrayList<PointStruct> mPathList;//存储单条线段的数据
    private static ArrayList<ArrayList<PointStruct>> mNoteList;//存储所有线段的数据
    private static ArrayList<ArrayList<PointStruct>> mRevokeNoteList;//存储撤销线段的数据
    private PenThread mPenThread;
    private int mCount = 0;
    private int mSize = 0;
    public static int mViewWidth;
    public static int mViewHeight;
    private static Timer mTimer;
    public static boolean isChangeOverlay = false;
    private boolean isEraserEnable = false;
    public static boolean isRecoveryEnable = false;
    public boolean isUndoEnable = false;
    public boolean isRedoEnable = false;
    public static boolean isUPorOUT = false;
    private static final int CHANGE_BACKGROUND = 0;
    private Rect mRect;

    public NoteView(Context context) {
        super(context);
        mContext = context;
		Log.d(TAG, "NoteView1");
        initView();
    }

    public NoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
		Log.d(TAG, "NoteView2");
        initView();
    }

    private Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_BACKGROUND:
                    Drawable drawable = (Drawable) msg.obj;
                    setBackground(drawable);
                    break;
            }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.d(TAG, "isChangeOverlay: " + isChangeOverlay + ",isEraserEnable: " + isEraserEnable);\
        Log.d(TAG, "onDraw: ");
        if(mBitmap != null) {
            // 这里不用实际绘制出来，由底层绘制即可
//            canvas.drawBitmap(mBitmap, 0, 0, null);
            if (isChangeOverlay) {
                setOverlayEnable(false);
                isChangeOverlay = false;
            }
        }

        /*if(mBitmap != null && mNullBitmap != null) {
            if (isEraserEnable) {
                canvas.drawBitmap(mNullBitmap, 0, 0, null);
                isUndoEnable = false;
            } else if (isRecoveryEnable) {
                canvas.drawBitmap(mRecoveryBitmap, 0, 0, null);
                isRecoveryEnable = false;
            } else {
                Log.d(TAG, "onDraw: ");
                canvas.drawBitmap(mBitmap, 0, 0, null);
            }
            if (isChangeOverlay && !isEraserEnable) {
                mNativeJNI.native_is_overlay_enable(false);
                isChangeOverlay = false;
            }
        }*/
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "hasFocus: " + hasFocus);
        // 这里是为了下拉状态栏时，隐藏手写内容，并禁用手写
        if(!hasFocus) {
            if(!isEraserEnable) {
                isChangeOverlay = true;
                postInvalidate();
            }
            mNativeJNI.native_is_handwriting_enable(false);
        } else {
            if(!isEraserEnable) {
                mNativeJNI.native_is_handwriting_enable(true);
            }
            refresh();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent x:" + event.getX() + ",y:" + event.getY());
//        if(isEraserEnable) {
//            int x = (int)event.getX();
//            int y = (int)event.getY();
//            int pressedValue = (int)event.getPressure();
//            int action = event.getAction();
//            switch (action) {
//                case MotionEvent.ACTION_DOWN:
//                    //Log.d(TAG, "ACTION_DOWN: ");
//                    mEraserLastX = x;
//                    mEraserLastY = y;
//                    break;
//                case MotionEvent.ACTION_UP:
//                    //Log.d(TAG, "ACTION_UP: ");
//                    mEraserLastX = 0;
//                    mEraserLastY = 0;
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    //Log.d(TAG, "ACTION_MOVE: ");
//                    mCanvas.drawLine(mEraserLastX, mEraserLastY, x, y, mEraserPaint);
//                    mEraserLastX = x;
//                    mEraserLastY = y;
//                    postInvalidate();
//                    break;
//            }
//            saveWritingDataEvent(mEraserLastX, mEraserLastY, x, y, pressedValue, PointStruct.PEN_WHITE_COLOR,
//                    ERASER_WIDTH_DEFAULT, action, true, false);
//        }
        return false;
    }

    private void initView() {
        //画笔
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        //mPaint.setAntiAlias(true);
        //mPaint.setDither(true);
        mPaint.setStrokeWidth(PEN_WIDTH_DEFAULT);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(Color.BLACK);
        //橡皮擦
        mEraserPaint = new Paint();
        //采用PorterDuff.Mode.CLEAR，图像覆盖处所有像素点的alpha和color都为0
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mEraserPaint.setStyle(Paint.Style.STROKE);
        //mEraserPaint.setAntiAlias(true);
        //mEraserPaint.setDither(true);
        mEraserPaint.setStrokeWidth(ERASER_WIDTH_DEFAULT);
        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
        //重点 解决擦除残留问题
        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);

        mPointList = new ArrayList<PointStruct>();
        mPathList = new ArrayList<PointStruct>();
        mNoteList = new ArrayList<ArrayList<PointStruct>>();
        mRevokeNoteList = new ArrayList<ArrayList<PointStruct>>();
        mNativeJNI = new NoteJNI(mContext);
        mPenThread = new PenThread();

//        registerLayoutDone();
    }

    private void registerLayoutDone() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "onGlobalLayout");
                int[] position = new int[2];
                getLocationOnScreen(position);
                int mLeft = position[0];
                int mTop = position[1];
                int mRight = mLeft + getWidth();
                int mBottom = mTop + getHeight();

                int mFilterLeft = 0;
                int mFilterTop = 0;
                int mFilterRight = 0;
                int mFilterBottom = 0;

                Log.d(TAG, "initNative: mLeft=" + mLeft + ", mRight=" + mRight + ", mTop=" + mTop +
                        ", mBottom=" + mBottom);
                initNative(new Rect(mLeft, mTop, mRight, mBottom), false,
                        new Rect(mFilterLeft, mFilterTop, mFilterRight, mFilterBottom));
            }
        });
    }

    private void initBitmap() {
        //createBGBitmap();
        createNoteBitmap();
    }

    private void createNoteBitmap() {
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(mViewWidth, mViewHeight, Bitmap.Config.ARGB_8888);
        }
        //空Bitmap
        if (mNullBitmap == null) {
            mNullBitmap = Bitmap.createBitmap(mViewWidth, mViewHeight, Bitmap.Config.ARGB_8888);
        }
        if (mCanvas == null) {
            mCanvas = new Canvas();
            mCanvas.setBitmap(mBitmap);
        }
    }

    /*
     * 初始化jni相关
     * 参数 rect:View边界距离屏幕的距离值px
     * 参数 bkPng:设置手写背景
     */
    public int initNative(Rect rect, boolean isFilterFixedArea, Rect filterRect) {
        Log.d(TAG, "Flash test : +++++++++ initNative() rect = " + rect + ",filterRect = "  + filterRect);
        mRect = rect;
        int initStatus = init(rect, isFilterFixedArea, filterRect);
        return initStatus;
    }

    private int init(Rect rect, boolean isFilterFixedArea, Rect filterRect) {
        if(mPointList != null) {
            mPointList.clear();
        }
        if(mNoteList != null) {
            mNoteList.clear();
        }
        if (mRevokeNoteList != null) {
            mRevokeNoteList.clear();
        }
        mViewWidth = getWidth();
        mViewHeight = getHeight();
        initBitmap();
        int initStatus = mNativeJNI.init(rect, isFilterFixedArea, filterRect);
        mNativeJNI.setPointHandler(mPointHandler);
        mPenThread.start();
        invalidate();
        return initStatus;
    }

    public void exitNativeOnly() {
        mNativeJNI.native_exit();
    }

    @Override
    public void setPen(Pen pen) {
        Log.d(TAG, "setPen");
        mPen = pen;
        if (pen.getCustomColor() == null) {
            setPenColor(pen.getColor());
            mNativeJNI.native_set_pen_color(true, pen.getColor());
        } else {
            Color color = pen.getCustomColor();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mPaint.setColor(color.toArgb());
                mNativeJNI.native_set_pen_color_any((int) color.alpha(), (int) color.red(),
                        (int) color.green(), (int) color.blue());
            }
        }
        mNativeJNI.native_set_pen_width(mPen.getWidth());
        mNativeJNI.native_strokes(pen.isStroke());
    }

    @Override
    public Pen getPen() {
        return mPen;
    }

    @Override
    public void clear(ClearMode mode) {
        if (mode == ClearMode.CLEAR_RGB_BUFFER) {
            mNativeJNI.native_clear(0);
        } else if (mode == ClearMode.CLEAR_ALL) {
            if (mBitmap != null) {
                mBitmap.eraseColor(Color.TRANSPARENT);
            }
            if (mPointList != null) {
                mPointList.clear();
                mCount = 0;
            }
            if (mPointList != null) {
                mNoteList.clear();
            }
            if (mPathList != null) {
                mPathList.clear();
            }
            if(mRevokeNoteList != null) {
                mRevokeNoteList.clear();
            }
            mNativeJNI.native_clear(1);
            refresh();
        }
    }

    @Override
    public void clear() {
        clear(ClearMode.CLEAR_ALL);
    }

    public void drawPoint(PointStruct pointStruct){
        //Log.d(TAG, "pointStruct.action: " + pointStruct.action);

        if (mBitmap == null || mCanvas == null) {
            Log.d(TAG, "mBitmap == null || mCanvas == null ");
        }
        if (pointStruct.action == PointStruct.ACTION_DOWN) {
            Log.d(TAG, "down");
        } else if (pointStruct.action == PointStruct.ACTION_MOVE) {
//            Log.d(TAG, "====move drawPoint: ,lastX:" + pointStruct.lastX + ",lastY:" + pointStruct.lastY + "pointStruct.x: "
//                    + pointStruct.x + ",pointStruct.y:" + pointStruct.y +",pointStruct.action:" + pointStruct.action);
//            Log.d(TAG, "drawPoint: pointStruct.penColor: " + pointStruct.penColor);
//            Log.d(TAG, "drawPoint: pointStruct.penWidth:" + pointStruct.penWidth);

            if (!pointStruct.eraserEnable) {
                mPaint.setStrokeWidth(pointStruct.penWidth);
                setPenColor(pointStruct.penColor);
                mCanvas.drawLine(pointStruct.lastX, pointStruct.lastY, pointStruct.x, pointStruct.y, mPaint);
            } else {
                mEraserPaint.setStrokeWidth(pointStruct.penWidth);
                if (mBackgroundColor != -1) {
                    mEraserPaint.setColor(mBackgroundColor);
                } else {
                    mEraserPaint.setColor(Color.WHITE);
                }
                mCanvas.drawLine(pointStruct.lastX, pointStruct.lastY, pointStruct.x, pointStruct.y, mEraserPaint);
            }
        } else if (pointStruct.action == PointStruct.ACTION_UP) {
            if(!isUndoEnable && !isRedoEnable)
                postInvalidate();
            Log.d(TAG, "up");
            //mNativeJNI.native_change_overlay(0);
        } else if (pointStruct.action == PointStruct.ACTION_OUT) {
            if(!isUndoEnable && !isRedoEnable)
                postInvalidate();
            Log.d(TAG, "out");
        } else if (pointStruct.action == PointStruct.ACTION_TOOL_UP) {
            //Log.d(TAG, "tool up: ");
            //mTimer = new Timer();
            //MyTask myTask = new MyTask();
            //mTimer.schedule(myTask, 2000);
        }
    }


    private class MyTask extends TimerTask {
        @Override
        public void run() {
            try {
                Log.d(TAG, "MyTask: ");
                postInvalidate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Handler mPointHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            if (what == NoteJNI.MSG_FLAG_DRAW) {
                PointStruct pointStruct = (PointStruct) msg.obj;
                if (mPointList.add(pointStruct)) {
                    Log.d(TAG, "---message---x,y,action: " + pointStruct.x + "," +pointStruct.y + "," + pointStruct.action);
                    mSize++;
                }
                if (pointStruct.action == PointStruct.ACTION_DOWN) {

                } else if (pointStruct.action == PointStruct.ACTION_MOVE) {
                    if(isUPorOUT) {
                        mPathList = new ArrayList<PointStruct>();
                        mPathList.add(pointStruct);
                        isUPorOUT = false;
                    } else {
                        mPathList.add(pointStruct);
                    }
                } else if (pointStruct.action == PointStruct.ACTION_UP || pointStruct.action == PointStruct.ACTION_OUT) {
                    if(mPathList.size() > 0) {
                        mPathList.add(pointStruct);
                        mNoteList.add(mPathList);
                        isUPorOUT = true;
                    }
                }
            }
        }
    };

    private class PenThread extends Thread {
        public boolean canRun = true;
        public PenThread() {
        }

        public void setEnable(boolean enable) {
            canRun = enable;
        }

        @Override
        public void run() {
            while (canRun) {
                int size = mPointList.size();
                if (size > mCount) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    while ((mPointList.size() - mCount) > 0) {
                        PointStruct pointStruct = mPointList.get(mCount);
//                        Log.d(TAG, "eraserEnable=" + pointStruct.eraserEnable);
                        if (pointStruct != null) {
//                            Log.d(TAG, "---penThread---x,y,action: " + pointStruct.x + ","
//                                    +pointStruct.y + "," + pointStruct.action);
                            drawPoint(pointStruct);
                        }
                        mCount++;
                    }
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean unDo() {

        if (mNoteList.size() > 0) {
            isUndoEnable = true;
            setHandWriteEnable(false);
            mNativeJNI.native_undo();
            //把最后一条线段数据添加到mRevokeNoteList中
            mRevokeNoteList.add(mNoteList.get(mNoteList.size() - 1));
            //mNoteList删除最后一条数据
            Log.d(TAG, "mRevokeNoteList.size1: " + mRevokeNoteList.size());
            mNoteList.remove(mNoteList.size() - 1);
            Log.d(TAG, "mNoteList.size2: " + mNoteList.size());
            mBitmap.eraseColor(Color.TRANSPARENT);
            //遍历删除后的mNoteList
            for(ArrayList<PointStruct> list : mNoteList) {
                for(PointStruct pointStruct : list) {
                   // Log.d(TAG, "pointStruct.x: " + pointStruct.x + ",pointStruct.y:" + pointStruct.y
                    //+ ",pointStruct.action:" + pointStruct.action);
                    drawPoint(pointStruct);
                }
            }
            setHandWriteEnable(true);
            isUndoEnable = false;
            return true;
        } else {
            Log.w(TAG, "mNoteList.size <= 0 ");
            return false;
        }
    }

    @Override
    public boolean reDo() {
        Log.d(TAG, "mRevokeNoteList.size: " + mRevokeNoteList.size());
        if(mRevokeNoteList.size() > 0) {
            setHandWriteEnable(false);
            mNativeJNI.native_redo();
            isRedoEnable = true;

            mNoteList.add(mRevokeNoteList.get(mRevokeNoteList.size() - 1));
            mRevokeNoteList.remove(mRevokeNoteList.size() - 1);
            mBitmap.eraseColor(Color.TRANSPARENT);
            //遍历删除后的mNoteList
            for(ArrayList<PointStruct> list : mNoteList) {
                for(PointStruct pointStruct : list) {
                    //Log.d(TAG, "pointStruct.x: " + pointStruct.x + ",pointStruct.y:" + pointStruct.y
                    //+ ",pointStruct.action:" + pointStruct.action);
                    drawPoint(pointStruct);
                }
            }
            isRedoEnable = false;
            setHandWriteEnable(true);
            return true;
        } else {
            Log.d(TAG, "mRevokeNoteList.size <= 0 ");
            return false;
        }
    }

    @Override
    public void setBackgroundColor(BgColor color) {
        if (this.mBgColor == color) {
            return;
        }
        this.mBgColor = color;
        switch (color) {
            case BG_COLOR_WHITE:
                setBackgroundColor(Color.WHITE);
                mBackgroundColor = PointStruct.PEN_WHITE_COLOR;
                break;
            case BG_COLOR_GRAY:
                setBackgroundColor(Color.GRAY);
                mBackgroundColor = PointStruct.PEN_GRAY_COLOR;
                break;
            case BG_COLOR_BLACK:
                setBackgroundColor(Color.BLACK);
                mBackgroundColor = PointStruct.PEN_BLACK_COLOR;
                break;
        }
        // 切换背景颜色，需清掉原有的图画，否则擦除会有问题
        clear();
    }

    @Override
    public BgColor getBackgroundColor() {
        return this.mBgColor;
    }

    @Override
    public void changeBackground() {
        mDrawable++;
        Drawable drawable = getResources().getDrawable(mDrawableIDs[mDrawable]);
        Message changeBGMessage = new Message();
        changeBGMessage.what = CHANGE_BACKGROUND;
        changeBGMessage.obj = drawable;
        handler.sendMessage(changeBGMessage);
        if(mDrawable >= 9)
            mDrawable = 0;
    }

    @Override
    public void savePicture(String path, String pictureName) {
        Bitmap newBitmap = getBitmap();
        File appDir = new File(Environment.getExternalStorageDirectory(), path);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = pictureName + ".jpg";
        File file = new File(appDir, fileName);
        Log.d(TAG, "savePicture: file.path=" + file.getAbsolutePath());
        try {
            FileOutputStream fos = new FileOutputStream(file);
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*if(mDrawable == -1) {
            Canvas canvas = new Canvas();
            Paint paint = new Paint();
            canvas.drawBitmap(mBitmap, 0, 0, paint);
            canvas.save();
            // 存储新合成的图片
            canvas.restore();
            File appDir = new File(Environment.getExternalStorageDirectory(), path);
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            String fileName = pictureName + ".jpg";
            File file = new File(appDir, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "save: ");
            Drawable drawable = getResources().getDrawable(mDrawableIDs[mDrawable]);
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            Bitmap drawableBitmap = drawable2Bitmap(drawable).
                    copy(Bitmap.Config.ARGB_8888, true);
            Bitmap bgBitmap = setBitmap(drawableBitmap, drawableWidth, drawableHeight, mViewWidth, mViewHeight);
            Bitmap newBitmap = Bitmap.createBitmap(bgBitmap);
            Canvas canvas = new Canvas(newBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.GRAY);
            paint.setAlpha(20);
            //canvas.drawRect(0, 0, bgBitmap.getWidth(), bgBitmap.getHeight(), paint);
            //paint = new Paint();
            canvas.drawBitmap(mBitmap, 0, 0, paint);
            canvas.save();
            // 存储新合成的图片
            canvas.restore();
            File appDir = new File(Environment.getExternalStorageDirectory(), path);
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            String fileName = pictureName + ".jpg";
            File file = new File(appDir, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    @Override
    public void setOverlayEnable(boolean enable) {
        Log.d(TAG, "setOverlayEnable: enable=" + enable);
        mNativeJNI.native_is_overlay_enable(enable);
    }

    @Override
    public void setHandWriteEnable(boolean enable) {
        Log.d(TAG, "setHandWriteEnable: enable=" + enable);
        mNativeJNI.native_is_handwriting_enable(enable);
    }

    @Override
    public void destroy() {
        clear(ClearMode.CLEAR_ALL);
        exitNativeOnly();
    }

    public void savePointInfo(String path, String pictureName) {
        /*SharedPreferences sp = mContext.getSharedPreferences("SP_PointList", Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonStr = gson.toJson(mPointList);
        Log.d(TAG, "mPointList.size: " + mPointList.size());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("Key_PointList", jsonStr);
        editor.commit();*/
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            File appDir = new File(Environment.getExternalStorageDirectory(), path);
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            String fileName = pictureName + ".txt";
            File file = new File(appDir, fileName);
            fileOutputStream = new FileOutputStream(file.toString());  //新建一个内容为空的文件
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            Log.d(TAG, "mNoteList.size: " + mNoteList.size());
            objectOutputStream.writeObject(mNoteList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (objectOutputStream != null) {
            try {
                objectOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getPointInfo() {
        /*ArrayList<PointStruct> list = new ArrayList<PointStruct>();
        SharedPreferences sp = mContext.getSharedPreferences("SP_PointList", Activity.MODE_PRIVATE);
        String strJson = sp.getString("Key_PointList", "");
        if(strJson != "") {
            Gson gson = new Gson();
            list = gson.fromJson(strJson, new TypeToken<List<PointStruct>>() {}.getType());
        }
        Log.d(TAG, "list.size: " + list.size());*/
        ArrayList<ArrayList<PointStruct>> list = new ArrayList<ArrayList<PointStruct>>();
        ObjectInputStream objectInputStream = null;
        ArrayList<PointStruct> savedArrayList = new ArrayList<>();
        try {
            FileInputStream fs = new FileInputStream("/mnt/sdcard/NoteDemo/picture.txt");
            objectInputStream = new ObjectInputStream(fs);
            list = (ArrayList<ArrayList<PointStruct>>) objectInputStream.readObject();
            Log.d(TAG, "list.size: " + list.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bitmap getBitmap() {
        Bitmap newBitmap = Bitmap.createBitmap(mBitmap);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        // 设置背景色
        if (mBackgroundColor != -1) {
            paint.setColor(parseColor(mBackgroundColor));
        } else {
            paint.setColor(Color.WHITE);
        }
        canvas.drawRect(0, 0, mBitmap.getWidth(), mBitmap.getHeight(), paint);
        paint = new Paint();
        canvas.drawBitmap(mBitmap, 0, 0, paint);
        canvas.save();
        // 存储新合成的图片
        canvas.restore();
        return newBitmap;
    }

    @Override
    public void setEraseEnable(boolean isEraseEnable) {
        this.isEraserEnable = isEraseEnable;
        Log.d(TAG, "setEraseEnable: isEraseEnable=" + isEraseEnable);
        if (isEraseEnable) {
            Log.d(TAG, "setEraseEnable: mBackgroundColor=" + mBackgroundColor);
            mNativeJNI.native_set_pen_color(true, mBackgroundColor);
            mNativeJNI.native_eraser(true);
            mNativeJNI.native_set_pen_width(20);
        } else {
            mNativeJNI.native_set_pen_color(true, mPen.getColor());
            mNativeJNI.native_eraser(false);
            mNativeJNI.native_set_pen_width(mPen.getWidth()); //
        }
    }

    @Override
    public boolean isEraseEnable() {
        return isEraserEnable;
    }

    @Override
    public void refresh() {
        Log.d(TAG, "refresh");
        String mode = EinkManager.getInstance().getMode();
        Log.d(TAG, "originMode=" + mode);
//        EinkManager.getInstance().setMode(EinkManager.EinkMode.EPD_OVERLAY);
        EinkManager.getInstance().sendOneFullFrame();
        postInvalidate();
        // 需延迟调一次底层重绘，刷新后才能显示原图形
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mNativeJNI.native_redraw();
            }
        }.start();
    }

    private void setPenColor(int penColor) {
        mPen.setColor(penColor);
        if (penColor == PointStruct.PEN_BLACK_COLOR) {
            mPaint.setColor(Color.BLACK);
        } else if (penColor == PointStruct.PEN_BLUE_COLOR) {
            mPaint.setColor(Color.BLUE);
        } else if (penColor == PointStruct.PEN_GREEN_COLOR) {
            mPaint.setColor(Color.GREEN);
        } else if (penColor == PointStruct.PEN_RED_COLOR) {
            mPaint.setColor(Color.RED);
        } else if (penColor == PointStruct.PEN_WHITE_COLOR) {
            mPaint.setColor(Color.WHITE);
        } else if (penColor == PointStruct.PEN_GRAY_COLOR) {
            mPaint.setColor(Color.GRAY);
        }
    }

    public int parseColor(int structColor) {
        Log.d(TAG, "parseColor: structColor=" + structColor);
        switch (structColor) {
            case PointStruct.PEN_ALPHA_COLOR:
                return Color.TRANSPARENT;
            case PointStruct.PEN_BLACK_COLOR:
                return Color.BLACK;
            case PointStruct.PEN_WHITE_COLOR:
                return Color.WHITE;
            case PointStruct.PEN_BLUE_COLOR:
                return Color.BLUE;
            case PointStruct.PEN_GREEN_COLOR:
                return Color.GREEN;
            case PointStruct.PEN_RED_COLOR:
                return Color.RED;
            case PointStruct.PEN_GRAY_COLOR:
                return Color.GRAY;
//            case PointStruct.PEN_CUSTOM_COLOR:
//                return mPen.getCustomColor();
        }
        return -1;
    }

    public NoteJNI getNoteJNI() {
        return mNativeJNI;
    }

    public void drawBitmap(int deviceId, int left, int top, int right, int bottom) {
        mNativeJNI.native_draw_bitmap(mBitmap, deviceId, left, top, right, bottom);
    }

    @Override
    public void saveWritingDataEvent(int lastX, int lastY, int x, int y, int pressedValue, int penColor, int penWidth,
                                     int action, boolean eraserEnable, boolean strokesEnable) {
        //Log.d(TAG, "saveWritingDataEvent lastX=" + lastX + ", lastY=" + lastY + ", x=" + x + ", y=" + y);
        PointStruct pointStruct = new PointStruct(lastX, lastY, x, y, pressedValue, penColor,
                penWidth, action, eraserEnable, strokesEnable);
        Message msg = mPointHandler.obtainMessage();
        msg.what = NoteJNI.MSG_FLAG_DRAW;
        msg.obj = (Object) pointStruct;
        msg.sendToTarget();
    }


}
