package framework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public record AsyncProxy(Object target) implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!method.isAnnotationPresent(Async.class)) return method.invoke(target, args);
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Run method " + method.getName() + " in AsyncProxy");
                return method.invoke(target, args);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
