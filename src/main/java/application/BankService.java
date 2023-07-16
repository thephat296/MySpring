package application;

import framework.Autowired;
import framework.Service;

@Service
public class BankService {

    @Autowired
//    @Qualifier("Email")
    private IEmailSender emailSender;

    public BankService(IEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }
}