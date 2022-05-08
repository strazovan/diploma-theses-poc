package cz.strazovan.cvut.fel.diploma.messagebox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "database")
public class DatabaseProperties {
    private String driverClassName;
    private String url;
    private String username;
    private String password;
    private String platform;
    private String ddlgeneration;

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDdlgeneration() {
        return ddlgeneration;
    }

    public void setDdlgeneration(String ddlgeneration) {
        this.ddlgeneration = ddlgeneration;
    }
}
