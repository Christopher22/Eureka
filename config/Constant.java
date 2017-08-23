package eureka.config;

import java.io.Serializable;
import java.lang.Cloneable;

/**
 * A constant which might be saved in the list of parameters to be easy editable. Currently not used.
 */
public class Constant implements Parameter, Serializable, Cloneable {
    private final double m_data;

    /**
     * Creates a new constant.
     * @param value The value of the constant.
     */
    public Constant(final double value) {
        this.m_data = value;
    }

    /**
     * Returns the value of the constant.
     * @return the value of the constant.
     */
    public double getValue() {
        return this.m_data;
    }

    /**
     * Makes the constant as unchangeable.
     * @return false.
     */
    public boolean setValue(final double value, final Memory<Parameter> currentMemory) {
        return false;
    }
}