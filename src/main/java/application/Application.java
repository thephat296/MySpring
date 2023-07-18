package application;

import framework.Autowired;
import framework.FWApplication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Application implements Runnable {

    @Autowired
    private BankService bankService;

//    @Autowired
//    private EmailSender emailSender;

    public static void main(String[] args) {
        FWApplication.run(Application.class);
    }

    @Override
    public void run() {
        bankService.deposit();
//        try {
//            CompletableFuture<Integer> result = emailSender.addTwoNumber(5,6);
//            while (!result.isDone()) {
//                System.out.println("loading");
//            }
//            System.out.println("Result: " + result.get());
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }
    }
}