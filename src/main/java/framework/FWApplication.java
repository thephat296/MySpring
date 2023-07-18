package framework;

import org.quartz.CronExpression;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class FWApplication {
    private final List<Object> beans = new ArrayList<>(); // bean or proxy bean
    private final List<Object> originalBeans = new ArrayList<>();
    private final List<Class<?>> lazyConstructionClazz = new ArrayList<>();
    private final Properties properties = new Properties();
    private String activeProfile = null;
    private final Map<Class<?>, List<MethodObjectPair>> eventMethodMap = new HashMap<>();

    private FWApplication() {
    }

    public static void run(Class<?> clazz) {
        FWApplication fwApplication = new FWApplication();
        try {
            Object source = clazz.getDeclaredConstructor().newInstance();
            fwApplication.beans.add(source);
            fwApplication.scanAndInstantiate(clazz.getPackageName());
            if (source instanceof Runnable) {
                ((Runnable) source).run();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void scanAndInstantiate(String basePackage) {
        loadProperties();
        setActiveProfile();
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> serviceClasses = reflections.getTypesAnnotatedWith(Service.class);
        injectConstructors(serviceClasses);
        wrapAsyncMethods();
        initEventPublisher();
        originalBeans.addAll(beans.stream().map(this::getOriginalObject).toList());
        performDI();
        initEventListener();
        performScheduled();
    }

    private void injectConstructors(Set<Class<?>> serviceClasses) {
        for (Class<?> serviceClass : serviceClasses) {
            try {
                beans.add(serviceClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                lazyConstructionClazz.add(serviceClass);
            }
        }
        while (!lazyConstructionClazz.isEmpty()) {
            int size = lazyConstructionClazz.size();
            Iterator<Class<?>> iterator = lazyConstructionClazz.iterator();
            while (iterator.hasNext()) {
                injectConstructor(iterator.next());
                iterator.remove();
            }
            if (lazyConstructionClazz.size() == size) {
                throw new RuntimeException("Circular dependency when performing constructor injection");
            }
        }
    }

    private void wrapAsyncMethods() {
        for (int i = 0; i < beans.size(); i++) {
            Object bean = beans.get(i);
            if (bean instanceof AsyncProxy) continue;
            boolean isAsyncAnnotationPresent =
                    Arrays.stream(bean.getClass().getMethods()).anyMatch(method -> method.isAnnotationPresent(Async.class));
            if (!isAsyncAnnotationPresent) continue;
            Class<?> clazz = bean.getClass();
            Object proxyBean = Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), new AsyncProxy(bean));
            beans.set(i, proxyBean);
        }
    }

    private void setActiveProfile() {
        this.activeProfile = this.properties.getProperty("profile.active");
    }

    private void initEventListener() {
        for (Object bean : originalBeans) {
            for (Method method : bean.getClass().getMethods()) {
                if (!method.isAnnotationPresent(EventListener.class)) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1) {
                    throw new RuntimeException("OnEvent method " + method.getName() + " must have maximum one parameter");
                }
                Class<?> eventClazz = params[0];
                eventMethodMap.computeIfAbsent(eventClazz, event -> new ArrayList<>())
                        .add(new MethodObjectPair(method, bean));
            }
        }
    }

    private void initEventPublisher() {
        ApplicationEventPublisher publisher = event -> eventMethodMap.get(event.getClass()).forEach(methodObjectPair -> {
            Method method = methodObjectPair.method;
            Object object = methodObjectPair.object;
            try {
                method.invoke(object, event);
            } catch (Exception e) {
                throw new RuntimeException("Failed to notify event listener of class" + object.getClass().getName(), e);
            }
        });
        beans.add(publisher);
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        } catch (IOException ignored) {
        }
    }

    private void performDI() {
        for (Object bean : originalBeans) {
            injectField(bean);
            injectSetter(bean);
            injectValues(bean);
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
            try {
                dependencies[i] = getBean(parameterTypes[i], constructor.getParameters()[i].getAnnotation(Qualifier.class));
            } catch (NoBeanFoundException e) {
                return;
            }
        }
        try {
            Object bean = constructor.newInstance(dependencies);
            boolean isAsyncAnnotationPresent =
                    Arrays.stream(bean.getClass().getMethods()).anyMatch(method -> method.isAnnotationPresent(Async.class));
            if (isAsyncAnnotationPresent) {
                Object proxyBean = Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), new AsyncProxy(bean));
                beans.add(proxyBean);
            } else {
                beans.add(bean);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform dependency injection for constructor: " + constructor.getName(), e);
        }
    }

    private void injectField(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;
            Qualifier qualifier = field.getAnnotation(Qualifier.class);
            Object dependency;
            try {
                dependency = getBean(field.getType(), qualifier);
            } catch (NoBeanFoundException e) {
                throw new RuntimeException(e);
            }
            field.setAccessible(true);
            try {
                field.set(bean, dependency);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                String message;
                if (dependency instanceof Proxy) {
                    message = "Dependency " + field.getName() + " using @Async annotation must be an interface instead of concrete class";
                } else {
                    message = "Failed to perform dependency injection for field: " + field.getName();
                }
                throw new RuntimeException(message, e);
            }
        }
    }

    private void injectSetter(Object bean) {
        for (Method method : bean.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Autowired.class) || !method.getName().startsWith("set")) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new RuntimeException("Setter method should have exactly one parameter: " + method.getName());
            }
            Object dependency;
            try {
                dependency = getBean(parameterTypes[0], method.getAnnotation(Qualifier.class));
            } catch (NoBeanFoundException e) {
                throw new RuntimeException(e);
            }
            try {
                method.invoke(bean, dependency);
            } catch (Exception e) {
                throw new RuntimeException("Failed to perform dependency injection for setter method: " + method.getName(), e);
            }
        }
    }

    private void injectValues(Object bean) {
        Class<?> clazz = bean.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Value.class)) continue;
            Value valueAnnotation = field.getAnnotation(Value.class);
            String valueKey = valueAnnotation.value();
            String propertyValue = properties.getProperty(valueKey);
            field.setAccessible(true);
            try {
                field.set(bean, propertyValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to perform value injection for field: " + field.getName(), e);
            }
        }
    }

    private void performScheduled() {
        for (Object bean : originalBeans) {
            for (Method method : bean.getClass().getMethods()) {
                if (!method.isAnnotationPresent(Scheduled.class)) continue;
                long fixedRate = method.getDeclaredAnnotation(Scheduled.class).fixedRate();
                if (fixedRate != -1) {
                    scheduleTimerTask(bean, method, Date.from(Instant.now()), fixedRate);
                    continue;
                }
                String cronExpression = method.getDeclaredAnnotation(Scheduled.class).cron();
                if (!cronExpression.isEmpty()) {
                    scheduleTimerTask(bean, method, getNextExecutionTime(cronExpression), getPeriod(cronExpression));
                }
            }
        }
    }

    private void scheduleTimerTask(Object bean, Method method, Date firstTime, long period) {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    method.invoke(bean);
                } catch (Exception e) {
                    throw new RuntimeException("@Scheduled method " + method.getName() + " must not take any arguments or return anything", e);
                }
            }
        }, firstTime, period);
    }

    private Date getNextExecutionTime(String cronExpression) {
        Date now = new Date();
        try {
            return new CronExpression(cronExpression).getNextValidTimeAfter(now);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private long getPeriod(String cronExpression) {
        try {
            Date next = new CronExpression(cronExpression).getNextValidTimeAfter(new Date());
            return next.getTime() - new Date().getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public Object getBean(Class<?> clazz, Qualifier qualifier) throws NoBeanFoundException {
        List<Object> foundBeans = beans.stream()
                .filter(bean -> containsClass(bean, clazz))
                .filter(bean -> {
                    Profile profile = bean.getClass().getDeclaredAnnotation(Profile.class);
                    return activeProfile == null || profile == null || Arrays.stream(profile.value()).collect(Collectors.toSet()).contains(activeProfile);
                })
                .filter(bean -> qualifier == null || qualifier.equals(bean.getClass().getDeclaredAnnotation(Qualifier.class)))
                .toList();
        if (foundBeans.isEmpty()) throw new NoBeanFoundException("No bean found for " + clazz.getName());
        if (foundBeans.size() >= 2) throw new RuntimeException("Multiple beans found for " + clazz.getName());
        return foundBeans.get(0);
    }

    private boolean containsClass(Object object, Class<?> clazz) {
        Object source = getOriginalObject(object);
        return source.getClass().equals(clazz) ||
                Arrays.stream(source.getClass().getInterfaces()).collect(Collectors.toSet()).contains(clazz);
    }

    private Object getOriginalObject(Object object) {
        if (object instanceof Proxy) {
            InvocationHandler handler = Proxy.getInvocationHandler(object);
            return ((AsyncProxy) handler).getTarget();
        }
        return object;
    }

    private record MethodObjectPair(Method method, Object object) {
    }
}
