package framework;

import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FWContext {
    private final List<Object> beans = new ArrayList<>();
    private final List<Class<?>> lazyConstructionClazz = new ArrayList<>();

    public FWContext() {
    }

    public void scanAndInstantiate(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);
        for (Class<?> serviceClass : serviceTypes) {
            try {
                beans.add(serviceClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                lazyConstructionClazz.add(serviceClass);
            }
        }
        performDI();
    }

    private void performDI() {
        for (Class<?> clazz : lazyConstructionClazz) {
            injectConstructor(clazz);
        }
        for (Object bean : beans) {
            injectField(bean);
            injectSetter(bean);
        }
    }

    private void injectConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1 || !constructors[0].isAnnotationPresent(Autowired.class)) {
            throw new RuntimeException("Failed to perform dependency injection for service class: " + clazz.getName());
        }
        Constructor<?> constructor = constructors[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] dependencies = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            dependencies[i] = getBean(parameterTypes[i], constructor.getParameters()[i].getAnnotation(Qualifier.class));
        }
        try {
            beans.add(constructor.newInstance(dependencies));
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform dependency injection for constructor: " + constructor.getName(), e);
        }
    }

    private void injectField(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                Object dependency = getBean(field.getType(), qualifier);
                field.setAccessible(true);
                try {
                    field.set(bean, dependency);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to perform dependency injection for field: " + field.getName(), e);
                }
            }
        }
    }

    private void injectSetter(Object bean) {
        for (Method method : bean.getClass().getMethods()) {
            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new RuntimeException("Setter method should have exactly one parameter: " + method.getName());
                }
                Object dependency = getBean(parameterTypes[0], method.getAnnotation(Qualifier.class));
                try {
                    method.invoke(bean, dependency);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to perform dependency injection for setter method: " + method.getName(), e);
                }
            }
        }
    }

    public Object getBean(Class<?> clazz) {
        return getBean(clazz, null);
    }

    public Object getBean(Class<?> clazz, Qualifier qualifier) {
        List<Object> foundBeans = beans.stream()
                .filter(bean -> bean.getClass().equals(clazz) ||
                        Arrays.stream(bean.getClass().getInterfaces()).collect(Collectors.toSet()).contains(clazz))
                .filter(bean -> qualifier == null || qualifier.equals(bean.getClass().getDeclaredAnnotation(Qualifier.class)))
                .toList();
        if (foundBeans.isEmpty()) throw new RuntimeException("No bean found for " + clazz.getName());
        if (foundBeans.size() >= 2) throw new RuntimeException("Multiple beans found for " + clazz.getName());
        return foundBeans.get(0);
    }
}
