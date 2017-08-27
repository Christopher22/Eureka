import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.io.File;

import robocode.control.*;
import robocode.control.events.*;

import eureka.*;
import eureka.config.*;

/**
 * An class to train a robot using a "supervised" training approach between battles.
 */
public class Trainer {
    private static int optimalScore;
    private static Memory<Parameter> optimalMemory;

    /**
     * The results of a training.
     */
    public static class TrainingResult {
        private final Memory<Parameter> m_optimal;
        private final int m_score;

        /**
         * Creates a new result.
         * @param memory The optimal parameter set.
         * @param score The total score.
         */
        public TrainingResult(final Memory<Parameter> memory, final int score) {
            this.m_optimal = memory;
            this.m_score = score;
        }

        /**
         * Returns the total score.
         * @return the total score.
         */
        public int getScore() {
            return this.m_score;
        }

        /**
         * Returns the optimal set of parameter.
         * @return the optimal set of parameter.
         */
        public Memory<Parameter> getOptimalMemory() {
            return this.m_optimal;
        }
    }

    /**
     * Trains a default set of parameters to get optimal.
     * @param robocodeDir The directory with the robocode.jar
     * @param defaultMemory The default set of parameters.
     * @param rounds The number of rounds each battle consists of.
     * @param battlefield The size of the battlefield.
     * @param robotName The full name of the robot to be trained.
     * @param enemyNames The names of the enemies, separated by comma.
     * @return the optimal set of parameters.
     */
    public static TrainingResult optimize(final String robocodeDir, final Memory<Parameter> defaultMemory,
            final int rounds, final BattlefieldSpecification battlefield, final String robotName,
            final String enemyNames) {

        // Disable security to run the robot
        System.setProperty("NOSECURITY", "true");

        // Clean up
        Trainer.optimalMemory = null;
        Trainer.optimalScore = 0;

        // Generate all possible values and assign labels to them
        final Map<String, Parameter> parameterMap = defaultMemory.getMap();
        List<String> parameters = new ArrayList<>(parameterMap.size());
        final List<List<Double>> parameterValues = generateAllParameter(parameterMap, parameters);

        // Start robocode
        RobocodeEngine.setLogMessagesEnabled(false);
        RobocodeEngine.setLogErrorsEnabled(true);
        RobocodeEngine engine = new RobocodeEngine(new java.io.File(robocodeDir));
        engine.setVisible(false);

        // Prepare the battles and load the robots
        final RobotSpecification[] robots = engine
                .getLocalRepository(new StringBuilder(enemyNames).append(",").append(robotName).toString());
        final int robotId = robots.length - 1;

        if (robots.length != enemyNames.split(",").length + 1) {
            throw new IllegalArgumentException("Enemies or own robot invalid!");
        }

        final File trainingFile = createDataFilePath(robots[robotId], Brain.TRAINING_FILENAME);
        final BattleSpecification specification = new BattleSpecification(rounds, battlefield, robots);

        // Generate all valid parameter permutations and test them.
        permute(parameterValues, (permutation -> {

            // Create and save new Memory
            Memory<Parameter> newMemory = new Memory<Parameter>(defaultMemory);
            for (int i = 0; i < permutation.size(); i++) {
                // Check if value is setable
                if (!newMemory.getMap().get(parameters.get(i)).setValue(permutation.get(i), newMemory)) {
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
                    // Update the best score, if possible
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

    /**
     * Creates a file in the data directory of a robot.
     * @param robot The robot.
     * @param filename The name of the file which is to be generated.
     * @return a file in the date folder
     */
    private static File createDataFilePath(RobotSpecification robot, String filename) {
        // Locate jar file
        File jar = robot.getJarFile();
        File dataFolder = new File(jar.getParentFile(), new StringBuilder()
                .append(jar.getName().substring(0, jar.getName().lastIndexOf('.'))).append(".data").toString());

        // Create directory, if not already existing
        dataFolder.mkdir();

        // Return the new file path
        return new File(dataFolder, filename);
    }

    /**
     * Translates a set of parameters into a list of lists with all valid values.
     * @param memory The set of parameter.
     * @param labels A list of labels, matching indexes to strings.
     * @return a list of lists with all values.
     */
    private static List<List<Double>> generateAllParameter(final Map<String, Parameter> memory, List<String> labels) {
        List<List<Double>> parameterValues = new ArrayList<>(memory.size());

        // Generate for each range parameter a list of its values.
        for (Map.Entry<String, Parameter> entry : memory.entrySet()) {
            // Ignore constants
            if (entry.getValue() instanceof Range) {
                // Generate the values
                Range range = (Range) entry.getValue();
                ArrayList<Double> tmpValues = new ArrayList<>();
                for (double value = range.getMin(); value <= range.getMax(); value += range.getSteps()) {
                    tmpValues.add(value);
                }
                parameterValues.add(tmpValues);

                // Fill list of labels
                labels.add(entry.getKey());
            }
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
                    break;
                } else if (i < last_index) {
                    index_pos[i] = 0;
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 8) {
            System.out.println(
                    "USAGE: heureka.java robocodeDir defaultParams rounds battlefieldWidth battlefieldHeight robotName enemies");
            return;
        }

        final File parameterFile = new File(args[2]);
        Memory<Parameter> parameters = Memory.load(parameterFile);
        if (parameters == null) {
            System.err.println("[ERROR] Unable to load default parameter");
            return;
        }

        final int battlefieldWidth, battlefieldHeight;
        try {
            battlefieldWidth = Integer.parseInt(args[4]);
            battlefieldHeight = Integer.parseInt(args[5]);
        } catch (Exception e) {
            System.err.println("[ERROR] Invalid size parameters");
            return;
        }

        Trainer.TrainingResult result = Trainer.optimize(args[1], parameters, Integer.parseInt(args[3]),
                new robocode.control.BattlefieldSpecification(battlefieldWidth, battlefieldHeight), args[6], args[7]);

        try {
            result.getOptimalMemory().save(parameterFile);
        } catch (Exception e) {
            System.err.println("Saving failed!");
        }
    }
}