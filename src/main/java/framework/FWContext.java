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
            try {
                beans.add(serviceClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        performDI();
    }

    private void performDI() {
        for (Object bean : beans) {
            injectField(bean);
        }
    }

    private void injectField(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Qualifier qualifier = null;
                try {
                    qualifier = field.getAnnotation(Qualifier.class);
                } catch (NullPointerException ignored) {
                }

                Object instance = getBean(field.getType(), qualifier);
                field.setAccessible(true);
                try {
                    field.set(bean, instance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
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
        if (foundBeans.isEmpty()) throw new RuntimeException("No bean found!");
        if (foundBeans.size() >= 2) throw new RuntimeException("Multiple beans found!");
        return foundBeans.get(0);
    }
}
