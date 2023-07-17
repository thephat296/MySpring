package application;

import framework.*;

@Service
public class BankService implements IBankService {

    private IEmailSender emailSender;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    public BankService(IEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setEmailService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    public void deposit() {
//        emailSender.sendEmail("deposit");
        publisher.publishEvent(new DepositEvent("deposit"));
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void sendScheduledEmail() {
        emailSender.sendEmail("Scheduled email");
    }
}