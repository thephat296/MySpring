package framework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class AopProxy implements InvocationHandler {
    private final Object target;
    private final Map<String, AspectData> methodAspectDataMap;

    // Aspect bean

    // Map<Class<?>, Map<Method, Set<AspectMethod>>>

    // Map<Method, Set<AspectMethod>>

    public AopProxy(Object target, Map<String, AspectData> methodAspectDataMap) {
        this.target = target;
        this.methodAspectDataMap = methodAspectDataMap;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // check method co phai chay before/after/around ko
        // kiem aspectMethod
        // aspectMethod.invoke(aspectBean)

        if (!methodAspectDataMap.containsKey(method.getName())) {
            return method.invoke(target, args);
        }

        AspectData aspectData = methodAspectDataMap.get(method.getName());
        Set<Method> aspectMethods = aspectData.aspectMethods();
        for (Method mt : aspectMethods) {
            if (mt.isAnnotationPresent(Before.class)) {
                mt.invoke(aspectData.aspectBean());
            }
        }
        Object returnValue = method.invoke(target, args);
        for (Method mt : aspectMethods) {
            if (mt.isAnnotationPresent(After.class)) {
                mt.invoke(aspectData.aspectBean());
            }
        }
        return returnValue;
    }

    public Object getTarget() {
        return target;
    }
}
