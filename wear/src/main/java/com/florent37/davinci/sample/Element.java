package com.florent37.davinci.sample;

public class Element {

    private String title;
    private String text;
    private int color;
    private String url;

    public Element(String title, String text, int color) {
        this.title = title;
        this.text = text;
        this.color = color;
    }

    public Element(String title, String text, String url) {
        this.title = title;
        this.text = text;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
