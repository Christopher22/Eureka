package eureka.config;

import java.io.Serializable;
import java.lang.Cloneable;

/**
 * A parameter in a specific range, which might get optimized.
 */
public class Range implements Parameter, Serializable, Cloneable {
    private double m_value;
    private final double m_min, m_max, m_steps;

    /**
     * Creates a new range.
     * @param value The default value.
     * @param min The lower border for the value.
     * @param max The upper border for the value.
     * @param steps The steps between lower and upper border which gets tested.
     */
    public Range(double value, double min, double max, double steps) {
        if (value >= max && value <= min) {
            throw new IndexOutOfBoundsException();
        }
        this.m_value = value;
        this.m_min = min;
        this.m_max = max;
        this.m_steps = steps;
    }

    /**
     * Returns the current value of the range.
     * @return the value of the range.
     */
    public double getValue() {
        return this.m_value;
    }

    /**
     * Returns the minimum value of the range.
     * @return the lower border.
     */
    public double getMin() {
        return this.m_min;
    }

    /**
     * Returns the maximal value of the range.
     * @return the upper border.
     */
    public double getMax() {
        return this.m_max;
    }

    /**
     * Returns the steps of the range.
     * @return the steps.
     */
    public double getSteps() {
        return this.m_steps;
    }

    /**
     * Updates the value, if suitable
     * @return true, if the new value is in the range.
     */
    public boolean setValue(double newValue, Memory<Parameter> currentMemory) {
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