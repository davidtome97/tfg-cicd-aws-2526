package com.sistemagestionapp.model.dto;

public class ProgresoDespliegue {
    private final long ok;
    private final int total;

    public ProgresoDespliegue(long ok, int total) {
        this.ok = ok;
        this.total = total;
    }

    public long getOk() { return ok; }
    public int getTotal() { return total; }
}
