package com.example.regis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String python;

    private String script;

    private String accountsDir;

    private String links;

    /*
     * Maksimum jumlah worker Python
     * yang dapat berjalan bersamaan.
     */
    private int maxConcurrency = 50;

    /*
     * Timeout dalam MENIT.
     */
    private int maxTimeoutMinutes = 30;


    // =========================================================
    // PYTHON
    // =========================================================

    public String getPython() {

        return python;
    }


    public void setPython(
            String python
    ) {

        this.python = python;
    }


    // =========================================================
    // SCRIPT
    // =========================================================

    public String getScript() {

        return script;
    }


    public void setScript(
            String script
    ) {

        this.script = script;
    }


    // =========================================================
    // ACCOUNTS DIRECTORY
    // =========================================================

    public String getAccountsDir() {

        return accountsDir;
    }


    public void setAccountsDir(
            String accountsDir
    ) {

        this.accountsDir = accountsDir;
    }


    // =========================================================
    // LINKS
    // =========================================================

    public String getLinks() {

        return links;
    }


    public void setLinks(
            String links
    ) {

        this.links = links;
    }


    // =========================================================
    // MAX CONCURRENCY
    // =========================================================

    public int getMaxConcurrency() {

        return maxConcurrency;
    }


    public void setMaxConcurrency(
            int maxConcurrency
    ) {

        this.maxConcurrency = maxConcurrency;
    }


    // =========================================================
    // MAX TIMEOUT MINUTES
    // =========================================================

    public int getMaxTimeoutMinutes() {

        return maxTimeoutMinutes;
    }


    public void setMaxTimeoutMinutes(
            int maxTimeoutMinutes
    ) {

        this.maxTimeoutMinutes = maxTimeoutMinutes;
    }
}