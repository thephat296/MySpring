package application;

import framework.After;
import framework.Around;
import framework.Aspect;
import framework.Before;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Aspect
public class TraceAdvice {
    @Before(pointcut = "BankService.deposit")
    public void traceBeforeDeposit() {
        System.out.println("Time before deposit: " + LocalDateTime.now());
    }

    @After(pointcut = "BankService.deposit")
    public void traceAfterDeposit() {
        System.out.println("Time after deposit: " + LocalDateTime.now());
    }

    @Around(pointcut = "BankService.deposit")
    public void traceAroundDeposit(Supplier<Object> supplier) {
        System.out.println("Time around - before deposit: " + LocalDateTime.now());
        supplier.get();
        System.out.println("Time around - after deposit: " + LocalDateTime.now());
    }
}
