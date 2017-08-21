package skynet.config;

import java.io.Serializable;
import java.lang.Cloneable;

public class Constant implements Parameter, Serializable, Cloneable {
    private final double m_data;

    public Constant(double value) {
        this.m_data = value;
    }

    public double getValue() {
        return this.m_data;
    }

    public boolean setValue(double value, Settings currentSettings) {
        return false;
    }
}