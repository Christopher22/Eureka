package eureka.config;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.io.File;

import robocode.control.*;
import robocode.control.events.*;

public class Trainer {
    public static String TRAINING_FILENAME = "training.ser";

    /*private static int optimalScore;
    private static Memory<Parameter> optimalMemory;
    
    public static class TrainingResult {
        private final Memory<Parameter> m_optimal;
        private final int m_score;
    
        public TrainingResult(final Memory<Parameter> Memory, final int score) {
            this.m_optimal = Memory;
            this.m_score = score;
        }
    
        public int getScore() {
            return this.m_score;
        }
    
        public Memory getOptimalMemory() {
            return this.m_optimal;
        }
    }
    
    public static TrainingResult optimize(final String robocodeDir, final Memory<Parameter> defaultMemory, final int rounds,
            final BattlefieldSpecification battlefield, final String robotName, final String enemyNames) {
    
        Trainer.optimalMemory = null;
        Trainer.optimalScore = 0;
    
        // Generate all possible values and assign labels to them
        final Map<String, Parameter> parameterMap = defaultMemory.getMap();
        String[] parameters = new String[parameterMap.size()];
        final List<List<Double>> parameterValues = generateAllParameter(parameterMap, parameters);
    
        // Start robocode
        RobocodeEngine.setLogMessagesEnabled(false);
        RobocodeEngine.setLogErrorsEnabled(true);
        RobocodeEngine engine = new RobocodeEngine(new java.io.File(robocodeDir));
        engine.setVisible(false);
    
        // Prepare the battles
        final RobotSpecification[] robots = engine
                .getLocalRepository(new StringBuilder(enemyNames).append(",").append(robotName).toString());
        final int robotId = robots.length - 1;
        final File trainingFile = createDataFilePath(robots[robotId], TRAINING_FILENAME);
        final BattleSpecification specification = new BattleSpecification(rounds, battlefield, robots);
    
        // Generate all valid parameter permutations
        permute(parameterValues, (permutation -> {
    
            // Create and save new Memory
            Memory<Parameter> newMemory = new Memory<Parameter>(defaultMemory);
            for (int i = 0; i < permutation.size(); i++) {
                if(!newMemory.getMap().get(parameters[i]).setValue(permutation.get(i), newMemory)) {
                    return;
                }
            }
    
            try {
                newMemory.save(trainingFile);
            } catch (Exception ex) {
                System.err.println("Could not write training file");
                return;
            }
    
            // Update optimal parameter, if found
            IBattleListener listener = new BattleAdaptor() {
                @Override
                public void onBattleCompleted(BattleCompletedEvent event) {
                    int score = (event.getIndexedResults()[robotId]).getScore();
                    if (score > Trainer.optimalScore) {
                        Trainer.optimalScore = score;
                        Trainer.optimalMemory = newMemory;
                    }
                }
            };
    
            // Run the battle
            engine.addBattleListener(listener);
            engine.runBattle(specification, true);
            engine.removeBattleListener(listener);
        }));
    
        // Update optimal Memory and clean up
        engine.close();
    
        return new TrainingResult(Trainer.optimalMemory, Trainer.optimalScore);
    }
    
    private static File createDataFilePath(RobotSpecification robot, String filename) {
        File jar = robot.getJarFile();
        File dataFolder = new File(jar.getParentFile(), new StringBuilder()
                .append(jar.getName().substring(0, jar.getName().lastIndexOf('.'))).append(".data").toString());
        dataFolder.mkdir();
        return new File(dataFolder, filename);
    }
    
    private static List<List<Double>> generateAllParameter(Map<String, Parameter> Memory, String[] labels) {
        List<List<Double>> parameterValues = new ArrayList<>(Memory.size());
        int parameterIndex = 0;
    
        for (Map.Entry<String, Parameter> entry : Memory.entrySet()) {
            // Ignore constants
            if (entry.getValue() instanceof Range) {
                Range range = (Range) entry.getValue();
                ArrayList<Double> tmpValues = new ArrayList<>();
                for (double value = range.getMin(); value <= range.getMax(); value += range.getSteps()) {
                    tmpValues.add(value);
                }
    
                labels[parameterIndex] = entry.getKey();
                parameterValues.add(tmpValues);
                ++parameterIndex;
            }
    
        }
        return parameterValues;
    }*/

    /**
     * Creates all permutations of an list of lists.
     * Adapted from https://stackoverflow.com/questions/29172066/generate-all-permutations-of-several-lists-in-java by "dhke"
     */
    /*private static <T> void permute(final List<List<T>> lists, final Consumer<List<T>> consumer) {
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
                    break;
                } else if (i < last_index) {
                    index_pos[i] = 0;
                }
            }
        }
    }*/
}