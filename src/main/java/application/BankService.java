package application;

import framework.Autowired;
import framework.Qualifier;
import framework.Service;

@Service
public class BankService {

    @Autowired
    @Qualifier("EmailSender")
    private IEmailSender emailSender;

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }
}