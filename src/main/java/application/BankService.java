package application;

import framework.Autowired;
import framework.Scheduled;
import framework.Service;

@Service
public class BankService {

    private IEmailSender emailSender;

    @Autowired
    public BankService(IEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void deposit() {
        emailSender.sendEmail("deposit");
    }

    @Scheduled(fixedRate = 5000)
    public void sendScheduledEmail() {
        emailSender.sendEmail("Scheduled email");
    }
}