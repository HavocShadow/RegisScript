package com.example.regis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String python;

    private String script;

    private String links;

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
    // ACCOUNTS DIRECTORY
    // ============================================================

    public String getAccounts() {
        return accounts;
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
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
    // MAX TIMEOUT
    // ============================================================

    public int getMaxTimeoutMinutes() {
        return maxTimeoutMinutes;
    }

    public void setMaxTimeoutMinutes(int maxTimeoutMinutes) {
        this.maxTimeoutMinutes = maxTimeoutMinutes;
    }
}