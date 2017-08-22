package skynet.helper;

public interface Signal {
    public interface Event extends Signal {}

    public interface Command extends Signal {}
}