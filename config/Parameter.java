package eureka.config;

import java.io.Serializable;
import java.lang.Cloneable;

/**
 * A serializable parameter which might get stored in the memory.
 */
public interface Parameter extends Serializable, Cloneable {
    /**
     * Returns the value of the parameter.
     */
    public double getValue();

    /**
     * Tries to set the value of the parameter.
     * @param value The new value.
     * @param currentMemory The memory of this parameter, wich might be used to check for sideeffects.
     * @return the success of the update.
     */
    public boolean setValue(double value, Memory<Parameter> currentMemory);
}