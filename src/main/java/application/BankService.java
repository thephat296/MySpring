package application;

import framework.Autowired;
import framework.Service;

@Service
public class BankService {
    private EmailSender emailSender;

    @Autowired
    public BankService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }
}