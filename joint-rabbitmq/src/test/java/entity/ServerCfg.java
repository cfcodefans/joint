package entity;


import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Entity
@Table(name = "server_cfg")
@Cacheable(false)
public class ServerCfg extends Configuration {
    private static final long serialVersionUID = 1L;

    @Basic
    private String host = "localhost";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id = -1;

    // log file configuration
    // TODO need better abstraction
    @Basic
    private String logFilePath;

    @Basic
    private String maxFileSize = String.valueOf("2GB");

    @Basic
    private String password = "guest";

    @Basic
    private int port = 5672;

    @Basic
    private String userName = "guest";

    @Basic
    private String virtualHost = "/";

    @Basic
    private boolean enabled = true;

    @Basic
    private String mails;

    public String getMails() {
        return mails;
    }

    public void setMails(String mails) {
        this.mails = mails;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ServerCfg))
            return false;
        ServerCfg other = (ServerCfg) obj;

        if (id != -1 && other.id != -1) {
            return id.equals(other.id);
        }

        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;

        if (port != other.port)
            return false;
        if (userName == null) {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        if (virtualHost == null) {
            if (other.virtualHost != null)
                return false;
        } else if (!virtualHost.equals(other.virtualHost))
            return false;
        return true;
    }

    public String getHost() {
        return host;
    }

    public Integer getId() {
        return id;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        if (id != -1)
            return result;

        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        result = prime * result + ((virtualHost == null) ? 0 : virtualHost.hashCode());
        return result;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{class:\"ServerCfg\", id:'").append(id).append("', host:'").append(host).append("', port:'").append(port).append("', userName:'").append(userName)
            .append("', password:'").append(password).append("', virtualHost:'").append(virtualHost).append("', logFilePath:'").append(logFilePath).append("', maxFileSize:'")
            .append(maxFileSize).append("'}");
        return builder.toString();
    }
}
