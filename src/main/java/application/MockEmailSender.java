package application;

import framework.EventListener;
import framework.Profile;
import framework.Service;

@Service
@Profile("mock")
public class MockEmailSender implements IEmailSender {
    @Override
    public void sendEmail(String content) {
        System.out.println("Mocker is sending email: " + content);
    }

    @EventListener
    public void onEvent(DepositEvent event) {
        sendEmail(event.message());
    }
}
