package com.pt.devicemanager;

public interface TrackerServiceListener {

    void onServiceBound(final TrackerService.TrackerBinder binder);
    void onServiceUnbound();
}
