package application;

import framework.Profile;
import framework.Service;

@Service
@Profile("mock")
public class MockEmailSender implements IEmailSender {
    @Override
    public void sendEmail(String content) {
        System.out.println("Mocker is sending email: " + content);
    }
}
