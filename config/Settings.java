package skynet.config;

import java.io.*;
import java.util.TreeMap; // Guaranties order of keys rather then HashMap
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Settings {
    private TreeMap<String, Parameter> m_config;

    public Settings() {
        this.m_config = new TreeMap<>();
    }

    @SuppressWarnings("unchecked")
    public Settings(Settings settings) {
        this.m_config = (TreeMap<String, Parameter>) settings.m_config.clone();
    }

    @SuppressWarnings("unchecked")
    public Settings(File path) throws IllegalArgumentException {
        try (FileInputStream fis = new FileInputStream(path); ObjectInputStream ois = new ObjectInputStream(fis)) {
            this.m_config = (TreeMap<String, Parameter>) ois.readObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Loading failed");
        }
    }

    public static Settings load(File file) {
        if (file.isFile()) {
            try {
                return new Settings(file);
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    public void save(File name) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(name, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this.m_config);
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    public boolean setValue(String name, double value) {
        return this.m_config.get(name).setValue(value, this);
    }

    public double getValue(String name, Parameter defaultParameter) {
        Parameter value = this.m_config.get(name);
        if (value == null) {
            if(defaultParameter == null) {
                throw new IllegalArgumentException("DefaultParameter was null");
            }
            this.m_config.put(name, defaultParameter);
            return defaultParameter.getValue();
        } else {
            return value.getValue();
        }
    }

    Map<String, Parameter> getMap() {
        return this.m_config;
    }
}