package com.rockchip.notedemo.painter;

/**
 * 笔型工厂
 */
public class PenFactory {

    /**
     * 普通笔示例
     * @return
     */
    public static Pen createNormalPen() {
        Pen pen = new Pen(Pen.PenType.PEN_TYPE_NORMAL, 4, PointStruct.PEN_BLACK_COLOR);
        return pen;
    }

    /**
     * 钢笔示例
     * @return
     */
    public static Pen createFountainPen() {
        Pen pen = new Pen(Pen.PenType.PEN_TYPE_FOUNTAIN, PointStruct.PEN_BLACK_COLOR, true);
        return pen;
    }

    /**
     * 马克笔示例
     * @return
     */
    public static Pen createMarkPen() {
        Pen pen = new Pen(Pen.PenType.PEN_TYPE_MARK, 50, PointStruct.PEN_GRAY_COLOR);
        return pen;
    }

}
