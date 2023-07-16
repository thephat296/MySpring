package framework;

import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FWContext {
    private final List<Object> beans = new ArrayList<>();

    public FWContext() {
    }

    public void scanAndInstantiate(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);
        for (Class<?> serviceClass : serviceTypes) {
            Object instance = null;
            try {
                instance = serviceClass.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
            }
            beans.add(instance);
        }
        performDI();
    }

    private void performDI() {
        for (Object bean : beans) {
//            injectConstructor(clazz, bean);
            injectField(bean);
//            injectSetter(clazz, bean);
        }
    }

//    private void injectConstructor(Class<?> clazz, Object bean) {
//        Constructor<?>[] constructors = clazz.getConstructors();
//        if (constructors.length == 1 && constructors[0].isAnnotationPresent(Autowired.class)) {
//            Constructor<?> constructor = constructors[0];
//            Class<?>[] parameterTypes = constructor.getParameterTypes();
//            Object[] dependencies = new Object[parameterTypes.length];
//            for (int i = 0; i < parameterTypes.length; i++) {
//                dependencies[i] = resolveDependency(parameterTypes[i], constructor.getParameters()[i].getAnnotation(Qualifier.class));
//                if (dependencies[i] == null) {
//                    throw new RuntimeException("Dependency not found for constructor parameter: " + constructor.getName());
//                }
//            }
//            try {
//                bean = constructor.newInstance(dependencies);
//                beans.put(clazz, bean);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to perform dependency injection for constructor: " + constructor.getName(), e);
//            }
//        }
//    }

    private void injectField(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Object instance = getBean(field.getType());
                field.setAccessible(true);
                try {
                    field.set(bean, instance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object getBean(Class<?> interfaceClass) {
        return beans.stream()
                .filter(bean -> bean.getClass().equals(interfaceClass) ||
                        Arrays.stream(bean.getClass().getInterfaces()).collect(Collectors.toSet()).contains(interfaceClass))
                .findFirst()
                .orElse(null);
    }

//    private void injectSetter(Class<?> clazz, Object bean) {
//        for (Method method : clazz.getMethods()) {
//            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set")) {
//                Class<?>[] parameterTypes = method.getParameterTypes();
//                if (parameterTypes.length != 1) {
//                    throw new RuntimeException("Setter method should have exactly one parameter: " + method.getName());
//                }
//                Object dependency = resolveDependency(parameterTypes[0], method.getAnnotation(Qualifier.class));
//                if (dependency == null) {
//                    throw new RuntimeException("Dependency not found for setter method: " + method.getName());
//                }
//                try {
//                    method.invoke(bean, dependency);
//                } catch (Exception e) {
//                    throw new RuntimeException("Failed to perform dependency injection for setter method: " + method.getName(), e);
//                }
//            }
//        }
//    }

    /*private Object resolveDependency(Class<?> dependencyType, Qualifier qualifierAnnotation) {
        if (qualifierAnnotation != null) {
            String qualifierName = qualifierAnnotation.value();
            try {
                return beans.values().stream()
                        .filter(bean -> bean != null && dependencyType.isAssignableFrom(bean.getClass()))
                        .filter(bean -> {
                            try {
                                return bean.getClass().getDeclaredAnnotation(Qualifier.class).value().equals(qualifierName);
                            } catch (NullPointerException ignored) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);
            } catch (NullPointerException e) {
                throw null;
            }
        } else {
            return beans.values().stream()
                    .filter(bean -> bean != null && dependencyType.isAssignableFrom(bean.getClass()))
                    .findFirst()
                    .orElse(null);
        }
    }*/
}
