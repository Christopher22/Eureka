package skynet.helper;

public interface Signal {

    public interface Event extends Signal {
    }

    public static class CustomEvent implements Event {
        private robocode.Condition m_condition;

        public CustomEvent(robocode.Condition condition) {
            this.m_condition = condition;
        }

        public robocode.Condition getCondition() {
            return this.m_condition;
        }
    }

    public interface GlobalEvent extends Event {
    }

    public interface Command extends Signal {
    }
}