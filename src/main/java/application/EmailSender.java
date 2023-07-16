package application;

import framework.Service;

@Service
public class EmailSender {
    public void sendEmail(String content) {
        System.out.println("sending email: " + content);
    }
}
