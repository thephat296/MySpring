package framework;

import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FWContext {
    private final Map<Class<?>, Object> beans = new HashMap<>();

    public void scanAndInstantiate(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);
        for (Class<?> serviceClass : serviceTypes) {
            Object instance = null;
            try {
                instance = serviceClass.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
            }
            beans.put(serviceClass, instance);
        }
        performDI();
    }

    private void performDI() {
        try {
            for (Class<?> clazz : beans.keySet()) {
                Object bean = beans.get(clazz);
                /* constructor injection */
                Constructor<?>[] constructors = clazz.getConstructors();
                if (constructors.length == 1 && constructors[0].isAnnotationPresent(Autowired.class)) {
                    Constructor<?> constructor = constructors[0];
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    Object[] dependencies = new Object[parameterTypes.length];
                    for (int i = 0; i < parameterTypes.length; i++) {
                        dependencies[i] = resolveDependency(parameterTypes[i], constructor.getParameters()[i].getAnnotation(Qualifier.class));
                        if (dependencies[i] == null) {
                            throw new RuntimeException("Dependency not found for constructor parameter: " + constructor.getName());
                        }
                    }
                    try {
                        bean = constructor.newInstance(dependencies);
                        beans.put(clazz, bean);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to perform dependency injection for constructor: " + constructor.getName(), e);
                    }
                }

                /* field injection */
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        field.setAccessible(true);
                        field.set(bean, beans.get(field.getType()));
                    }
                }

                /* setter injection */
                for (Method method : clazz.getMethods()) {
                    if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set")) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length != 1) {
                            throw new RuntimeException("Setter method should have exactly one parameter: " + method.getName());
                        }
                        Object dependency = resolveDependency(parameterTypes[0], method.getAnnotation(Qualifier.class));
                        if (dependency == null) {
                            throw new RuntimeException("Dependency not found for setter method: " + method.getName());
                        }
                        try {
                            method.invoke(bean, dependency);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to perform dependency injection for setter method: " + method.getName(), e);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Object resolveDependency(Class<?> dependencyType, Qualifier qualifierAnnotation) throws ClassNotFoundException {
        if (qualifierAnnotation != null) {
            String className = qualifierAnnotation.value();
            return beans.get(Class.forName(className));
        } else {
            return beans.values().stream()
                    .filter(bean -> bean != null && dependencyType.isAssignableFrom(bean.getClass()))
                    .findFirst()
                    .orElse(null);
        }
    }

    public Object getBean(Class<?> myClass) {
        return beans.get(myClass);
    }


}
