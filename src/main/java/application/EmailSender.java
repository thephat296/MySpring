package application;

import framework.*;

import java.util.concurrent.CompletableFuture;

@Service
@Profile("prod")
public class EmailSender implements IEmailSender {

    @Value("sender")
    private String sender;

    @Override
    public void sendEmail(String content) {
        System.out.println(sender + " is sending email: " + content);
    }

    @EventListener
    public void onEvent(DepositEvent event) {
        sendEmail(event.getMessage());
    }

//    @Async
//    public CompletableFuture<Integer> addTwoNumber(int a, int b) throws InterruptedException {
//        Thread.sleep(3000);
//        return CompletableFuture.completedFuture(a + b);
//    }
}
