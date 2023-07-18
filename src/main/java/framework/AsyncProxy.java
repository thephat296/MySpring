package framework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class AsyncProxy implements InvocationHandler {
    private final Object target;

    public AsyncProxy(Object target) { this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // invoke the method on the target
        Object returnValue;
        if (method.isAnnotationPresent(Async.class)) {
            returnValue = CompletableFuture.supplyAsync(() -> {
                try {
                    return method.invoke(target, args);
                } catch (Exception e) {
                    throw new RuntimeException("Can't invoke async method " + method.getName(), e);
                }
            });
        } else {
            returnValue = method.invoke(target, args);
        }
        return returnValue;
    }
}
