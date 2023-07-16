package application;

import framework.Service;

@Service
//@Qualifier("EmailSender")
public class EmailSender implements IEmailSender {
    @Override
    public void sendEmail(String content) {
        System.out.println("sending email: " + content);
    }
}
