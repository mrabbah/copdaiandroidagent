package org.copdai.android.sensors.agent;

import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;

public class SensorsUtil {

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    public static final float ANGULAR_VALUE_DRIFT = 0.05f;

    public static final float ACC_ANGULAR_VALUE_DRIFT = 0.02f;

    public static final float ACC_VALUE_DRIFT = 0.7f;

    public static final float GRAVITY_VALUE = 9.9f;

    public static DeviceAcceleration getDeviceAcceleration(float[] mAccelerometerData) {
        DeviceAcceleration deviceAcceleration = new DeviceAcceleration(0f, 0f, 0f);
        mAccelerometerData[2] = mAccelerometerData[2] - GRAVITY_VALUE; // Remove acceleration due to gravity
        if (Math.abs(mAccelerometerData[0]) > ACC_VALUE_DRIFT) {
            deviceAcceleration.x = mAccelerometerData[0];
        }
        if (Math.abs(mAccelerometerData[1]) > ACC_VALUE_DRIFT) {
            deviceAcceleration.y = mAccelerometerData[1];
        }
        if (Math.abs(mAccelerometerData[2]) > ACC_VALUE_DRIFT) {
            deviceAcceleration.z = mAccelerometerData[2];
        }
        return  deviceAcceleration;
    }

    public static DeviceAngularAcceleration getDeviceAngularAcceleration(float[] mGyroscopeData) {
        DeviceAngularAcceleration deviceAngularAcceleration = new DeviceAngularAcceleration(0f, 0f, 0f);
        if (Math.abs(mGyroscopeData[0]) > ACC_ANGULAR_VALUE_DRIFT) {
            deviceAngularAcceleration.x = mGyroscopeData[0];
        }
        if (Math.abs(mGyroscopeData[1]) > ACC_ANGULAR_VALUE_DRIFT) {
            deviceAngularAcceleration.y = mGyroscopeData[1];
        }
        if (Math.abs(mGyroscopeData[2]) > ACC_ANGULAR_VALUE_DRIFT) {
            deviceAngularAcceleration.z = mGyroscopeData[2];
        }
        return  deviceAngularAcceleration;
    }

    public static DeviceOrientation getDeviceOrientation(float[] mAccelerometerData,
                                                         float[] mMagnetometerData,
                                                         Display mDisplay) {
        // Compute the rotation matrix: merges and translates the data
        // from the accelerometer and magnetometer, in the device coordinate
        // system, into a matrix in the world's coordinate system.
        //
        // The second argument is an inclination matrix, which isn't
        // used in this example.
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData);

        // Remap the matrix based on current device/activity rotation.
        float[] rotationMatrixAdjusted = new float[9];
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted);
                break;
        }

        // Get the orientation of the device (azimuth, pitch, roll) based
        // on the rotation matrix. Output units are radians.
        float orientationValues[] = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrixAdjusted,
                    orientationValues);
        }

        // Pull out the individual values from the array.
        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];

        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
        if (Math.abs(pitch) < ANGULAR_VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < ANGULAR_VALUE_DRIFT) {
            roll = 0;
        }
        DeviceOrientation deviceOrientation = new DeviceOrientation();
        deviceOrientation.azimuth = azimuth;
        deviceOrientation.pitch = pitch;
        deviceOrientation.roll = roll;
        return deviceOrientation;
    }

    public static DeviceOrientation getDeviceOrientation(float[] mAccelerometerData,
                                                         float[] mMagnetometerData) {
        // Compute the rotation matrix: merges and translates the data
        // from the accelerometer and magnetometer, in the device coordinate
        // system, into a matrix in the world's coordinate system.
        //
        // The second argument is an inclination matrix, which isn't
        // used in this example.
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData);

        // Remap the matrix based on current device/activity rotation.
        float[] rotationMatrixAdjusted = new float[9];
        rotationMatrixAdjusted = rotationMatrix.clone();

        // Get the orientation of the device (azimuth, pitch, roll) based
        // on the rotation matrix. Output units are radians.
        float orientationValues[] = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrixAdjusted,
                    orientationValues);
        }

        // Pull out the individual values from the array.
        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];

        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
        if (Math.abs(pitch) < ANGULAR_VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < ANGULAR_VALUE_DRIFT) {
            roll = 0;
        }
        DeviceOrientation deviceOrientation = new DeviceOrientation();
        deviceOrientation.azimuth = azimuth;
        deviceOrientation.pitch = pitch;
        deviceOrientation.roll = roll;
        return deviceOrientation;
    }
}
