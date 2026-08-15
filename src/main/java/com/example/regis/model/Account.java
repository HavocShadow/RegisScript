package com.example.regis.model;

public class Account {

    private String userId;
    private String password;
    private String fullName;

    private String bankType;
    private String bankCode;

    private String accountNumber;


    public Account() {
    }


    public Account(
            String userId,
            String password,
            String fullName,
            String bankType,
            String bankCode,
            String accountNumber
    ) {

        this.userId = userId;
        this.password = password;
        this.fullName = fullName;
        this.bankType = bankType;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }


    public String getBankType() {
        return bankType;
    }

    public void setBankType(String bankType) {
        this.bankType = bankType;
    }


    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }


    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(
            String accountNumber
    ) {
        this.accountNumber = accountNumber;
    }


    /*
     * Format yang dibaca oleh Python:
     *
     * username|password|fullname|banktype|bankname|banknumber
     */

    public String toAccountFileLine() {

        return String.join(
                "|",

                userId,

                password,

                fullName,

                bankCode,

                bankType,

                accountNumber
        );
    }
}
