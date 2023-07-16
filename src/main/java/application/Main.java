package application;

import framework.FWContext;

public class Main {
    public static void main(String[] args) {
        FWContext fWContext = new FWContext();
        fWContext.scanAndInstantiate("application");

        BankService bankService = (BankService) fWContext.getBean(BankService.class);
        if (bankService != null) bankService.deposit();
    }
}