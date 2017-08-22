package skynet.config;

import java.io.*;
import java.util.TreeMap; // Guaranties order of keys rather then HashMap
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Memory<Data extends Serializable> {
    private TreeMap<String, Data> m_config;

    public Memory() {
        this.m_config = new TreeMap<String, Data>();
    }

    @SuppressWarnings("unchecked")
    public Memory(Memory<Data> memory) {
        this.m_config = (TreeMap<String, Data>) memory.m_config.clone();
    }

    @SuppressWarnings("unchecked")
    public Memory(File path) throws IllegalArgumentException {
        try (FileInputStream fis = new FileInputStream(path); ObjectInputStream ois = new ObjectInputStream(fis)) {
            this.m_config = (TreeMap<String, Data>) ois.readObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Loading failed");
        }
    }

    public static <Data extends Serializable> Memory<Data> load(File file) {
        if (file.isFile()) {
            try {
                return new Memory<Data>(file);
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

    public Data getValue(String name, Data defaultData) {
        Data value = this.m_config.get(name);
        if (value == null) {
            if (defaultData == null) {
                throw new IllegalArgumentException("DefaultData was null");
            }
            this.m_config.put(name, defaultData);
            return defaultData;
        } else {
            return value;
        }
    }

    public Data setValue(String name, Data defaultData) {
        return this.m_config.put(name, defaultData);
    }

    Map<String, Data> getMap() {
        return this.m_config;
    }
}