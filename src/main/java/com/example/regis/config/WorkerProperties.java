package com.example.regis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String python;

    private String script;

    private String links;

    /*
     * Directory account.
     *
     * application.properties:
     *
     * worker.accounts=/home/vortexis/RegisV8_Fix/accounts
     */
    private String accounts;

    private int maxConcurrency = 50;

    private int maxTimeoutMinutes = 30;


    // ============================================================
    // PYTHON
    // ============================================================

    public String getPython() {
        return python;
    }

    public void setPython(String python) {
        this.python = python;
    }


    // ============================================================
    // SCRIPT
    // ============================================================

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }


    // ============================================================
    // LINKS
    // ============================================================

    public String getLinks() {
        return links;
    }

    public void setLinks(String links) {
        this.links = links;
    }


    // ============================================================
    // ACCOUNTS
    // ============================================================

    public String getAccounts() {
        return accounts;
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
    }


    /*
     * COMPATIBILITY:
     *
     * AccountService dan AccountFileService
     * Anda masih menggunakan getAccountsDir().
     *
     * Jangan ubah kedua service tersebut.
     */

    public String getAccountsDir() {
        return accounts;
    }

    public void setAccountsDir(String accountsDir) {
        this.accounts = accountsDir;
    }


    // ============================================================
    // MAX CONCURRENCY
    // ============================================================

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }


    // ============================================================
    // MAX TIMEOUT MINUTES
    // ============================================================

    public int getMaxTimeoutMinutes() {
        return maxTimeoutMinutes;
    }

    public void setMaxTimeoutMinutes(int maxTimeoutMinutes) {
        this.maxTimeoutMinutes = maxTimeoutMinutes;
    }
}