package framework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class AsyncProxy implements InvocationHandler {

    private final Object target;

    public AsyncProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(Async.class)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return method.invoke(target, args);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
        } else {
            return method.invoke(target, args);
        }
    }

    public Object getTarget() {
        return target;
    }
}
