package com.rockchip.notedemo.painter;

import android.graphics.Bitmap;

/**
 * 手写接口
 */
public interface PainterApi {

    enum BgColor {
        BG_COLOR_WHITE,
        BG_COLOR_GRAY,
        BG_COLOR_BLACK
    }

    enum ClearMode {
        CLEAR_RGB_BUFFER,   // 只清除rgb buffer,不清除底层线条list（再次画线时或刷新，会重绘之前的图形）
        CLEAR_ALL           // 同时清除rgb buffer和底层线条list（完全清空)
    }

    /**
     * 设置画笔
     * @param pen
     */
    void setPen(Pen pen);

    /**
     * 获取画笔
     * @return
     */
    Pen getPen();

    /**
     * 清除
     */
    void clear(ClearMode mode);

    /**
     * 全屏清除, mode: CLEAR_ALL
     */
    void clear();

    /**
     * 撤销
     */
    boolean unDo();

    /**
     * 恢复
     */
    boolean reDo();

    /**
     * 设置背景色
     * @param color
     */
    void setBackgroundColor(BgColor color);

    /**
     * 获取背景色
     * @return
     */
    BgColor getBackgroundColor();

    /**
     * 获取当前绘制的Bitmap
     * @return
     */
    Bitmap getBitmap();

    /**
     * 设置是否为擦除模式
     * @param isEraseEnable
     */
    void setEraseEnable(boolean isEraseEnable);

    /**
     * 是否为擦除模式
     * @return
     */
    boolean isEraseEnable();

    /**
     * 刷新（去除残影）
     */
    void refresh();

    /**
     * 更改背景(Demo自带背景图）
     */
    void changeBackground();

    /**
     * 将当前绘图进行保存
     * @param path
     * @param pictureName
     */
    void savePicture(String path, String pictureName);


    /**
     * overlay图层使能与禁用
     * @param enable
     */
    void setOverlayEnable(boolean enable);

    /**
     * 设置是否可以手写
     * @param enable
     */
    void setHandWriteEnable(boolean enable);

    /**
     * 销毁资源
     */
    void destroy();

    /**
     * Native底层往上层传输的数据方法
     * 可在方法里面获取所需数据
     * @param lastX
     * @param lastY
     * @param x
     * @param y
     * @param pressedValue
     * @param penColor
     * @param penWidth
     * @param action
     * @param eraserEnable
     * @param strokesEnable
     */
    void saveWritingDataEvent(int lastX, int lastY, int x, int y, int pressedValue, int penColor, int penWidth,
                              int action, boolean eraserEnable, boolean strokesEnable);

}
