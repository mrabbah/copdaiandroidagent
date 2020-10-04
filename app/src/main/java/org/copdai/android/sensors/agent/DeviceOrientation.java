package org.copdai.android.sensors.agent;

public class DeviceOrientation {
    public float azimuth;
    public float pitch;
    public float roll;

    public DeviceOrientation() {
    }

    public DeviceOrientation(float azimuth, float pitch, float roll) {
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }
}
