package application;

import framework.Autowired;
import framework.Qualifier;
import framework.Service;

@Service
public class BankService {


    private IEmailSender emailSender;

    @Autowired
    public BankService(@Qualifier("EmailSender") IEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }
}