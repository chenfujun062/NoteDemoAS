package com.rockchip.notedemo;

import static com.rockchip.notedemo.painter.PainterApi.BgColor.BG_COLOR_BLACK;
import static com.rockchip.notedemo.painter.PainterApi.BgColor.BG_COLOR_GRAY;
import static com.rockchip.notedemo.painter.PainterApi.BgColor.BG_COLOR_WHITE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import static java.lang.Thread.sleep;

import androidx.annotation.RequiresApi;

import com.rockchip.notedemo.painter.PainterApi;
import com.rockchip.notedemo.painter.NoteView;
import com.rockchip.notedemo.painter.Pen;
import com.rockchip.notedemo.painter.PenFactory;
import com.rockchip.notedemo.eink.EinkManager;
import com.rockchip.notedemo.painter.PointStruct;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static Context mContext;
    private Handler mHandler;

    public NoteView mView;
    public PainterApi mPainterApi;
    private Button mTestBtn;
    private Button mSaveBtn;
    private Button mBackgroundBtn;
    private Button mRecoveryBtn;
    private Button mCancelBtn;
    private Button mDumpBtn;
    private Button mUndoBtn;
    private Button mRedoBtn;
    private Button mClearBtn;
    private Spinner mPenColorSp;
    private Spinner mPenTypeSp;
    private Spinner mBgColorSp;
    private CheckBox mStrokesCheck;
    private CheckBox mEraserCheck;
    private Button mRefreshBtn;
    private Button mfreshmodeBtn;

    private static int mLeft;
    private static int mTop;
    private static int mRight;
    private static int mBottom;
    private static int mFilterLeft;
    private static int mFilterTop;
    private static int mFilterRight;
    private static int mFilterBottom;

    private boolean buttonLock = false;
    private static final int USB_ATTACHED = 1;
    private BroadcastReceiver mBatInfoReceiver;
    private static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    private boolean mLastConnectStatus = true;
    private boolean mConnectStatus = false;
    private boolean initFlag = false;

    private Pen mNormalPen = PenFactory.createNormalPen();
    private Pen mFountainPen = PenFactory.createFountainPen();
    private Pen mMarkPen = PenFactory.createMarkPen();


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case USB_ATTACHED:
                    Toast toast = Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG);
                    toast.show();
                    break;
            }
        }
    };

    private Runnable mRunnable = new Runnable() {
        public void run() {
            int count = 0;
            while (mView.getHeight() <= 0) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (count++ > 40) {
                    Log.d(TAG, "Flash test : ++++++++ removeCallbacks");
                    mHandler.removeCallbacks(mRunnable);
                    System.exit(0);
                }
                Log.d(TAG, "Flash test : ++++++++ mView.getHeight() = " + mView.getHeight() + ", count = " + count);
            }

            mFilterLeft = 0;
            mFilterTop = 0;
            mFilterRight = 0;
            mFilterBottom = 0;

            int[] position = new int[2];
            mView.getLocationOnScreen(position);
            //左上角坐标
            mLeft = position[0];
            mTop = position[1];
            //右下角坐标
            mRight = mLeft + mView.getWidth();
            mBottom = mTop + mView.getHeight();
            Log.d(TAG, "initNative: mLeft=" + mLeft + ", mRight=" + mRight + ", mTop=" + mTop +
                    ", mBottom=" + mBottom);
            mView.initNative(new Rect(mLeft, mTop, mRight, mBottom), false,
                    new Rect(mFilterLeft, mFilterTop, mFilterRight, mFilterBottom));
            initFlag = true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Flash test : +++++++ onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;
        mView = (NoteView) findViewById(R.id.note_view);
        mPainterApi = (PainterApi) mView;

        //设置竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mBackgroundBtn = (Button) findViewById(R.id.background);
        mSaveBtn = (Button) findViewById(R.id.save);
        mCancelBtn = (Button) findViewById(R.id.cancel);
        mUndoBtn = (Button) findViewById(R.id.undo);
        mRedoBtn = (Button) findViewById(R.id.redo);
        mClearBtn = (Button) findViewById(R.id.clear);
        mRefreshBtn = (Button) findViewById(R.id.refresh);
        mfreshmodeBtn = (Button) findViewById(R.id.freshmode);

        mBackgroundBtn.setOnClickListener(this);
        mSaveBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        mUndoBtn.setOnClickListener(this);
        mRedoBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);
        mRefreshBtn.setOnClickListener(this);
        mfreshmodeBtn.setOnClickListener(this);

        mStrokesCheck = (CheckBox) findViewById(R.id.strokes);
        mStrokesCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Pen pen = mView.getPen();
                pen.setStroke(isChecked);
                mView.setPen(pen);
            }
        });

        mEraserCheck = (CheckBox) findViewById(R.id.eraser);
        mEraserCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mView.setEraseEnable(isChecked);
            }
        });

        initPenColorSpanner();
        initPenTypeSpanner();
        initBgColorSpanner();

        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 100);

        IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        //注册home键广播
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //注册USB插入广播
        filter.addAction(ACTION_USB_STATE);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "onReceive");
                String action = intent.getAction();
                //Log.d(TAG, "action: " + action);
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "screen off");
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Log.d(TAG, "screen unlock");
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                    Log.d(TAG, "Home,Task");
                    //mNativeJNI.native_clear(1);
                    //mView.postInvalidate();
                } /*else if (ACTION_USB_STATE.equals(action)) {
                    mConnectStatus = intent.getExtras().getBoolean("connected");
                    if(mConnectStatus && !mLastConnectStatus) {
                        Message message = new Message();
                        message.what = USB_ATTACHED;
                        message.obj = (String)"usb is connected";
                        handler.sendMessage(message);
                        //mView.postInvalidate();
                        mNativeJNI.native_change_overlay(0);
                        //mNativeJNI.native_set_is_drawing(1);
                    } else if (!mConnectStatus && mLastConnectStatus) {
                        Message message = new Message();
                        message.what = USB_ATTACHED;
                        message.obj = (String)"usb is disconnected";
                        handler.sendMessage(message);
                        //mNativeJNI.native_set_is_drawing(0);
                    }
                    mLastConnectStatus = mConnectStatus;
                    //mNativeJNI.native_change_overlay(0);
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    Message message = new Message();
                    message.what = USB_ATTACHED;
                    message.obj = (String)"usb is attached";
                    handler.sendMessage(message);
                }*/
            }
        };
        registerReceiver(mBatInfoReceiver, filter);

/*        mTestBtn = (Button) findViewById(R.id.test);
        mTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        if(!buttonLock) {
                            buttonLock = true;
                            //需要在子线程中处理的逻辑
                            mNativeJNI.native_clear(1);
                            mView.drawBitmap(0, mLeft, mTop, mRight, mBottom);
                            mView.clear();
                            buttonLock = false;
                        }
                    }
                }.start();
            }
        });*/
        /*mTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        if(!buttonLock) {
                            buttonLock = true;
                            //需要在子线程中处理的逻辑
                            mNativeJNI.native_set_is_drawing(1);
                            if(canTestDraw) {
                                Log.d(TAG, "canTestDraw: " + canTestDraw);
                                canTestDraw = false;
                                mNativeJNI.native_test_draw(true);
                            } else {
                                Log.d(TAG, "canTestDraw: " + canTestDraw);
                                canTestDraw = true;
                                mNativeJNI.native_test_draw(false);
                            }
                            mNativeJNI.native_set_is_drawing(0);
                            buttonLock = false;
                        }
                    }
                }.start();
            }
        });*/
        /*mDumpBtn = (Button) findViewById(R.id.dump);
        mDumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        if(!buttonLock) {
                            buttonLock = true;
                            //需要在子线程中处理的逻辑
                            mNativeJNI.native_dump();
                            buttonLock = false;
                        }
                    }
                }.start();
            }
        });*/

    }

    private void initPenTypeSpanner() {
        mPenTypeSp = findViewById(R.id.pen_type_spinner);
        mPenTypeSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                switch (pos) {
                    case 0:
                        mView.setPen(mNormalPen);
                        break;
                    case 1:
                        mView.setPen(mFountainPen);
                        break;
                    case 2:
                        mView.setPen(mMarkPen);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void initBgColorSpanner() {
        mBgColorSp = (Spinner) findViewById(R.id.bg_color_spinner);
        mBgColorSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                Log.d(TAG, "bgcolorBtn click");
                mView.clear();
                PainterApi.BgColor color = BG_COLOR_WHITE;
                switch (pos) {
                    case 0:
                        color = BG_COLOR_WHITE;
                        break;
                    case 1:
                        color = BG_COLOR_BLACK;
                        break;
                    case 2:
                        color = BG_COLOR_GRAY;
                        break;
                }
                mView.setBackgroundColor(color);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void initPenColorSpanner() {

        mPenColorSp = (Spinner) findViewById(R.id.pen_color_spinner);
        mPenColorSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (mView.isEraseEnable()) {
                    mView.setEraseEnable(false);
                    mEraserCheck.setChecked(false);
                }
                mView.clear();

                Pen pen = mView.getPen();
                switch (pos) {
                    case 0:
                        if (initFlag) {
                            pen.setColor(PointStruct.PEN_BLACK_COLOR);
                        }
                        break;
                    case 1:
                        pen.setColor(PointStruct.PEN_GRAY_COLOR);
                        break;
                    case 2:
                        pen.setColor(PointStruct.PEN_WHITE_COLOR);
                        break;
                    case 3:
                        pen.setColor(PointStruct.PEN_BLUE_COLOR);
                        break;
                    case 4:
                        pen.setColor(PointStruct.PEN_GREEN_COLOR);
                        break;
                    case 5:
                        pen.setColor(PointStruct.PEN_RED_COLOR);
                        break;
                    case 6://set any A R G B
                        pen.setCustomColor(Color.valueOf(0x39, 0x83, 0xFF, 0xFF));
                        break;
                }
                mView.setPen(pen);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Flash test : +++++++ onResume()");
        if (initFlag) {
            mView.setHandWriteEnable(true);
            mView.setOverlayEnable(true);
            refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Flash test : +++++++ onPause()");
        if (initFlag) {
            mView.setHandWriteEnable(false);
            mView.setOverlayEnable(false);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Flash test : +++++++ onDestroy()");
        if (mHandler != null) {
            Log.d(TAG, "mHandler != null");
            mHandler.removeCallbacks(mRunnable);
            mHandler = null;
        }
        mView.destroy();
        unregisterReceiver(mBatInfoReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "keyCode: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            /*NoteView.isChangeOverlay = true;
            mView.postInvalidate();*/
            mView.setOverlayEnable(false);
            //mNativeJNI.native_change_overlay(0);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.clear:
                mView.clear();
                break;
            case R.id.undo:
                unDo();
                break;
            case R.id.redo:
                reDo();
                break;
            case R.id.refresh:
                refresh();
                break;
            case R.id.background:
                changeBackground();
                break;
            case R.id.save:
                save();
                break;
            case R.id.cancel:
                MainActivity.this.finish();
                break;
            case R.id.freshmode:
                EinkManager.getInstance().setMode(EinkManager.EinkMode.EPD_A2_DITHER);
                v.postInvalidate();
                break;
        }
    }

    private void unDo() {
        new Thread() {
            @Override
            public void run() {
                if (!buttonLock) {
                    buttonLock = true;
                    //需要在子线程中处理的逻辑
                    mView.unDo();
                    buttonLock = false;
                }
            }
        }.start();
    }

    private void reDo() {
        new Thread() {
            @Override
            public void run() {
                if (!buttonLock) {
                    buttonLock = true;
                    //需要在子线程中处理的逻辑
                    mView.reDo();
                    buttonLock = false;
                }
            }
        }.start();
    }

    private void refresh() {
        mView.refresh();
    }

    private void changeBackground() {
        new Thread() {
            @Override
            public void run() {
                if (!buttonLock) {
                    buttonLock = true;
                    //需要在子线程中处理的逻辑
                    mView.changeBackground();
                    buttonLock = false;
                }
            }
        }.start();
    }

    private void save() {
        new Thread() {
            @Override
            public void run() {
                if (!buttonLock) {
                    buttonLock = true;
                    //需要在子线程中处理的逻辑
                    mView.savePicture("NoteDemo", "picture");
                    mView.savePointInfo("NoteDemo", "picture");
                    buttonLock = false;
                }
            }
        }.start();
    }

/*    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int)event.getRawX();
        int y = (int)event.getRawY();
        Log.d(TAG, "x:" + x + ",y:" + y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN: ");
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP: ");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "ACTION_MOVE: ");
                break;
        }
        return super.dispatchTouchEvent(event);
    }*/


}
