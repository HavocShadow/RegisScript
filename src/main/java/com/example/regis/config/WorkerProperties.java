package com.example.regis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String python;
    private String script;

    private String links;
    private String accounts;

    private String outputRoot;

    private int maxConcurrency = 50;
    private int maxTimeoutSeconds = 300;


    public String getPython() {
        return python;
    }

    public void setPython(String python) {
        this.python = python;
    }


    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }


    public String getLinks() {
        return links;
    }

    public void setLinks(String links) {
        this.links = links;
    }


    public String getAccounts() {
        return accounts;
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
    }


    public String getOutputRoot() {
        return outputRoot;
    }

    public void setOutputRoot(String outputRoot) {
        this.outputRoot = outputRoot;
    }


    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }


    public int getMaxTimeoutSeconds() {
        return maxTimeoutSeconds;
    }

    public void setMaxTimeoutSeconds(
            int maxTimeoutSeconds
    ) {
        this.maxTimeoutSeconds =
                maxTimeoutSeconds;
    }
}