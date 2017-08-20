package skynet.config;

import java.io.*;
import java.util.TreeMap; // Guaranties order of keys rather then HashMap
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import robocode.control.*;
import robocode.control.events.*;

public class Settings {

    public static final String TRAINING_FILE = "config_test.dat";

    private TreeMap<String, Parameter> m_config;

    private static int optimalHeuristics = 0;
    private static Settings optimalSettings;

    public Settings() {
        this.m_config = new TreeMap<>();
    }

    @SuppressWarnings("unchecked")
    public Settings(Settings settings) {
        this.m_config = (TreeMap<String, Parameter>)settings.m_config.clone();
    }

    @SuppressWarnings("unchecked")
    public Settings(String path) throws IllegalArgumentException {
        try (FileInputStream fis = new FileInputStream(path); ObjectInputStream ois = new ObjectInputStream(fis)) {
            this.m_config = (TreeMap<String, Parameter>) ois.readObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Loading failed");
        }
    }

    public static Settings tryLoadTrainingFile() {
        if(new File(TRAINING_FILE).isFile()) {
            try {
                return new Settings(TRAINING_FILE);
            } catch(Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    public void save(String name) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(name); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this.m_config);
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    public boolean setValue(String name, double value) {
        return this.m_config.get(name).setValue(value);
    }

    public int optimize(String robocodeDir, BattleSpecification specification, int robotId) {

        // Generate all possible values and assign labels to them
        String[] parameters = new String[m_config.size()];
        List<List<Double>> parameterValues = generateAllParameter(parameters);

        // Start robocode
        RobocodeEngine.setLogMessagesEnabled(false);
        RobocodeEngine engine = new RobocodeEngine(new java.io.File(robocodeDir));
        engine.setVisible(false);

        // Generate all valid permutation
        permute(parameterValues, (permutation -> {

            // Create and save new settings
            Settings newSettings = new Settings(this);
            for(int i = 0; i < permutation.size(); i++) {
                if(!newSettings.setValue(parameters[i], permutation.get(i))) {
                    return;
                }
            }

            try {
                newSettings.save(TRAINING_FILE);
            } catch(Exception ex) {
                return;
            }
            
            // Update optimal parameter, if found
            IBattleListener listener = new BattleAdaptor() {
                @Override public void onBattleCompleted(BattleCompletedEvent event) {
                    int score = (event.getIndexedResults()[robotId]).getScore();
                    if(score > optimalHeuristics) {
                        optimalHeuristics = score;
                        optimalSettings = newSettings;
                    }
                }
            };

            // Run the battle
            engine.addBattleListener(listener);
            engine.runBattle(specification, true);
            engine.removeBattleListener(listener);
        }));

        // Update optimal settings and clean up
        engine.close();
        this.m_config = optimalSettings.m_config;
        optimalSettings = null;
        optimalHeuristics = 0;

        return optimalHeuristics;
    }

    private List<List<Double>> generateAllParameter(String[] labels) {
        List<List<Double>> parameterValues = new ArrayList<>(m_config.size());
        int parameterIndex = 0;

        for (Map.Entry<String,Parameter> entry : this.m_config.entrySet()) {
            ArrayList<Double> tmpValues = new ArrayList<>();
            for (double value = entry.getValue().getMin(); value <= entry.getValue().getMax(); value += entry.getValue().getSteps()) {
                tmpValues.add(value);
            }

            labels[parameterIndex] = entry.getKey();
            parameterValues.add(tmpValues);
            ++parameterIndex;
        }
        return parameterValues;
    }

    /**
     * Creates all permutations of an list of lists.
     * Adapted from https://stackoverflow.com/questions/29172066/generate-all-permutations-of-several-lists-in-java by "dhke"
     */
    private static <T> void permute(final List<List<T>> lists, final Consumer<List<T>> consumer) {
        final int[] index_pos = new int[lists.size()];

        final int last_index = lists.size() - 1;
        final List<T> permuted = new ArrayList<T>(lists.size());

        for (int i = 0; i < lists.size(); ++i) {
            permuted.add(null);
        }

        while (index_pos[last_index] < lists.get(last_index).size()) {
            for (int i = 0; i < lists.size(); ++i) {
                permuted.set(i, lists.get(i).get(index_pos[i]));
            }
            consumer.accept(permuted);

            for (int i = 0; i < lists.size(); ++i) {
                ++index_pos[i];
                if (index_pos[i] < lists.get(i).size()) {
                    /* stop at first element without overflow */
                    break;
                } else if (i < last_index) {
                    index_pos[i] = 0;
                }
            }
        }
    }
}