package application;

import framework.Autowired;
import framework.FWApplication;

public class Application implements Runnable {

    @Autowired
    private IBankService bankService;

    public static void main(String[] args) {
        FWApplication.run(Application.class);
    }

    @Override
    public void run() {
        bankService.deposit();
    }
}