package skynet.config;

import java.io.Serializable;
import java.lang.Cloneable;

public interface Parameter extends Serializable, Cloneable {
    public double getValue();

    public boolean setValue(double value, Memory<Parameter> currentMemory);
}