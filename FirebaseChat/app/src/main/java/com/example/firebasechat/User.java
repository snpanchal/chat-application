package com.example.firebasechat;

public class User {

    private String name;
    private String imageLink;
    private String status;
    private String thumbImageLink;

    public User() {}

    public User(String name, String imageLink, String status, String thumbImageLink) {
        this.name = name;
        this.imageLink = imageLink;
        this.status = status;
        this.thumbImageLink = thumbImageLink;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThumbImageLink() {
        return thumbImageLink;
    }

    public void setThumbImageLink(String thumbImageLink) {
        this.thumbImageLink = thumbImageLink;
    }
}
