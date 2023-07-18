package framework;

import java.lang.reflect.Method;
import java.util.Set;

public record AspectData(Object aspectBean, Set<Method> aspectMethods) {
}
