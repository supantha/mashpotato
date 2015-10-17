package com.login.seedit.seedit;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by tushar on 17/10/15.
 */
public class obj {

    String title;
    String address;
    String location;
    LatLng latlong;
    String distance;

    public obj(String title, String address, String location, LatLng latlong, String distance) {
        this.title = title;
        this.address = address;
        this.location = location;
        this.latlong = latlong;
        this.distance = distance;
    }

    public String getDistance() {

        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public obj(String title, String address, String location, LatLng latlong) {
        this.title = title;
        this.address = address;
        this.location = location;
        this.latlong = latlong;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LatLng getLatlong() {
        return latlong;
    }

    public void setLatlong(LatLng latlong) {
        this.latlong = latlong;
    }
}
