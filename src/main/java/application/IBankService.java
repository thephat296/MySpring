package application;

import framework.Async;

public interface IBankService {
    @Async
    void deposit();
}
