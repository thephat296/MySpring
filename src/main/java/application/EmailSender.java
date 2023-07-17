package application;

import framework.Qualifier;
import framework.Service;
import framework.Value;

@Service
@Qualifier("EmailSender")
public class EmailSender implements IEmailSender {

    @Value("sender")
    private String sender;

    @Override
    public void sendEmail(String content) {
        System.out.println(sender + " is sending email: " + content);
    }
}
