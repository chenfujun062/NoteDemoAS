package com.rockchip.notedemo.painter;

import android.graphics.Color;

public class Pen {

    enum PenType {
        PEN_TYPE_NORMAL,    // 普通笔
        PEN_TYPE_FOUNTAIN,  // 钢笔
        PEN_TYPE_MARK       // 马克笔
    }

    private PenType type = PenType.PEN_TYPE_NORMAL;
    private int width = 4;
    private int color = PointStruct.PEN_BLACK_COLOR;
    private boolean stroke = false;
    // 自定义颜色，用于灰色 不同灰度值设置
    private Color customColor;

    public Pen(PenType type, int width, int color) {
        this.type = type;
        this.width = width;
        this.color = color;
    }

    public Pen(PenType type, int color, boolean stroke) {
        this.type = type;
        this.color = color;
        this.stroke = stroke;
    }

    void setPenType(PenType penType) {
        type = penType;
    }

    void setPenWidth(int width) {
        this.width = width;
    }

    public PenType getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isStroke() {
        return stroke;
    }

    /**
     * 设置画笔是否为压力笔触（如钢笔为true)
     * @param stroke
     */
    public void setStroke(boolean stroke) {
        this.stroke = stroke;
    }

    /**
     * 自定义颜色
     * @return
     */
    public Color getCustomColor() {
        return customColor;
    }

    /**
     *
     * @param customColor
     */
    public void setCustomColor(Color customColor) {
        this.customColor = customColor;
    }

}
