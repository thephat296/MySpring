package application;

import framework.Autowired;
import framework.Service;

@Service
public class BankService {

    @Autowired
    private IEmailSender emailSender;

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }
}