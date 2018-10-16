package com.dawoo.ipc.event.bean;

/**
 * archar  天纵神武
 **/
public class Ping extends BaseIpcEvent {
    private String ping;

    public String getPing() {
        return ping;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }
}
