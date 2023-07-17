package framework;

@FunctionalInterface
public interface ApplicationEventPublisher {
    void publishEvent(Object event);
}
