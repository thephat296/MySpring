package application;

import framework.Profile;
import framework.Service;
import framework.Value;

@Service
@Profile("prod")
public class EmailSender implements IEmailSender {

    @Value("sender")
    private String sender;

    @Override
    public void sendEmail(String content) {
        System.out.println(sender + " is sending email: " + content);
    }
}
