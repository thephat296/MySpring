package application;

import framework.After;
import framework.Aspect;
import framework.Before;

import java.time.LocalDateTime;

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
}
