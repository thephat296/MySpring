package framework;

import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FWContext {
    private final Map<Class<?>, Object> beans = new HashMap<>();

    public void scanAndInstantiate(String basePackage) {
        try {
            Reflections reflections = new Reflections(basePackage);
            Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);
            for (Class<?> serviceClass : serviceTypes) {
                beans.put(serviceClass, serviceClass.getDeclaredConstructor().newInstance());
            }
            performDI();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void performDI() {
        try {
            for (Object bean : beans.values()) {
                for (Field field : bean.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        field.setAccessible(true);
                        field.set(bean, beans.get(field.getType()));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Object getBean(Class<?> myClass) {
        return beans.get(myClass);
    }


}
