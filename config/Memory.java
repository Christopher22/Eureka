package eureka.config;

import java.io.*;
import java.util.TreeMap; // Guaranties order of keys rather then HashMap
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import robocode.RobocodeFileOutputStream;

/**
 * A serizable memory for the storage of data.
 */
public class Memory<Data extends Serializable> {
    private TreeMap<String, Data> m_config;

    /**
     * Creates a new memory.
     */
    public Memory() {
        this.m_config = new TreeMap<String, Data>();
    }

    /**
     * Copies an existing memory.
     */
    @SuppressWarnings("unchecked")
    public Memory(Memory<Data> memory) {
        this.m_config = (TreeMap<String, Data>) memory.m_config.clone();
    }

    /**
     * Loads the memory from a file.
     * @param file The path to the file.
     */
    @SuppressWarnings("unchecked")
    public Memory(final File path) throws IllegalArgumentException {
        // Guarantee closing of file by using try(closeable)
        try (FileInputStream fis = new FileInputStream(path); ObjectInputStream ois = new ObjectInputStream(fis)) {
            this.m_config = (TreeMap<String, Data>) ois.readObject();
        } catch (Exception e) {
            System.err.printf("[Error] Unable to load memory '%s' (%s)\n", path.getAbsolutePath(), e.getMessage());
            throw new IllegalArgumentException("Loading failed");
        }
    }

    /**
     * Tries to load the memory.
     * @param file The file which might be loaded.
     * @return the Model or 'null' on failure.
     */
    public static <Data extends Serializable> Memory<Data> load(final File file) {
        // Check the file exists
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

    /**
     * Saves the memory on the drive.
     * @param name The file which might be loaded.
     */
    public void save(final File name) throws IOException {
        // Guarantee closing of file by using try(closeable)
        try (RobocodeFileOutputStream fos = new RobocodeFileOutputStream(name);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this.m_config);
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    /**
     * Gets the value or a default value, which is instead of insert into the database.
     * @param name The key of the data which is to be searched.
     * @param defaultData The data which is to be inserted if there is no existing one.
     * @return the data or the default value.
     */
    public Data getValue(final String name, final Data defaultData) {
        final Data value = this.m_config.get(name);

        // Checks if value exits...
        if (value == null) {
            if (defaultData == null) {
                throw new IllegalArgumentException("DefaultData was null");
            }
            // ... or put in else.
            this.m_config.put(name, defaultData);
            return defaultData;
        } else {
            return value;
        }
    }

    /**
     * Sets a specific value.
     * @param name The key of the data which is to be searched.
     * @param data The data which is to be set.
     * @return the data which is to be replaced.
     */
    public Data setValue(final String name, final Data data) {
        return this.m_config.put(name, data);
    }

    /**
     * Returns the inner list, only accessable for code in this module.
     * @return the map.
     */
    Map<String, Data> getMap() {
        return this.m_config;
    }
}