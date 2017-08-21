package skynet.config;

import java.io.Serializable;
import java.lang.Cloneable;

public class Range implements Parameter, Serializable, Cloneable {
    private double m_value;
    private final double m_min, m_max, m_steps;

    public Range(double value, double min, double max, double steps) {
        if (value >= max && value <= min) {
            throw new IndexOutOfBoundsException();
        }
        this.m_min = min;
        this.m_max = max;
        this.m_steps = steps;
    }

    public double getValue() {
        return this.m_value;
    }

    public double getMin() {
        return this.m_min;
    }

    public double getMax() {
        return this.m_max;
    }

    public double getSteps() {
        return this.m_steps;
    }

    public boolean setValue(double newValue, Settings currentSettings) {
        if (newValue < this.m_max && newValue > this.m_min) {
            this.m_value = newValue;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object clone() {
        return new Range(this.m_value, this.m_min, this.m_max, this.m_steps);
    }
}