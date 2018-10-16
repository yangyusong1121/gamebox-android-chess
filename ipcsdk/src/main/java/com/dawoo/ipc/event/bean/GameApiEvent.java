package com.dawoo.ipc.event.bean;


import java.io.Serializable;

/**
 * archar  天纵神武
 **/
public class GameApiEvent extends BaseIpcEvent implements Serializable{
    private String gameApi;

    public String getGameApi() {
        return gameApi;
    }

    public void setGameApi(String gameApi) {
        this.gameApi = gameApi;
    }
}
