package eureka.helper;

/**
 * A signal which is sended between the differnet components and the brain.
 */
public interface Signal {

    /**
     * A marker trait for an event.
     */
    public interface Event extends Signal {
    }

    /**
     * An event which is fired if a custom event happens.
     */
    public static class CustomEvent implements Event {
        private final robocode.Condition m_condition;

        /**
         * Creates an new custom event.
         * @param condition The condition.
         */
        public CustomEvent(robocode.Condition condition) {
            this.m_condition = condition;
        }

        /**
         * Returns the condition of the custom event.
         * @return the condition of the custom event.
         */
        public robocode.Condition getCondition() {
            return this.m_condition;
        }
    }

    /**
     * A global event which was fired from the robocode engine.
     */
    public interface GlobalEvent extends Event {
    }

    /**
     * A command from the brain towards the components.
     */
    public interface Command extends Signal {
    }
}