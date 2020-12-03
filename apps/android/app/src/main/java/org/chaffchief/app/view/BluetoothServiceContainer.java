package org.chaffchief.app.view;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;

public class BluetoothServiceContainer {

    private static BluetoothServiceContainer instance = null;

    public BluetoothSPP bt;

    private String m_roaster_name;
    private String m_roaster_address;
    private boolean m_ackReceived = false;

    private BluetoothServiceContainer() {
        //
    }

    public static BluetoothServiceContainer getInstance() {
        if (instance == null) {
            instance = new BluetoothServiceContainer();
        }
        return instance;
    }

    public String getRoasterName() {
        return m_roaster_name;
    }

    public void setRoasterName(String m_roaster_name) {
        this.m_roaster_name = m_roaster_name;
    }

    public String getRoasterAddress() {
        return m_roaster_address;
    }

    public void setRoasterAddress(String m_roaster_address) {
        this.m_roaster_address = m_roaster_address;
    }

    public boolean isAckReceived() {
        return m_ackReceived;
    }

    public void setAckReceived(boolean m_ackReceived) {
        this.m_ackReceived = m_ackReceived;
    }
}
