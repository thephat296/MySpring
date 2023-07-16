package application;

import framework.Autowired;
import framework.Service;

@Service
public class BankService {
    @Autowired
    private EmailSender emailSender;

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }
}