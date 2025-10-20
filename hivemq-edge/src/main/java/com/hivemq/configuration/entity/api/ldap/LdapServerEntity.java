package com.hivemq.configuration.entity.api.ldap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import org.jetbrains.annotations.NotNull;

@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("NotNullFieldNotInitialized")
public class LdapServerEntity {

    /*        <ldap-server>
                <host>host</host>
                <port>389</port>
              </ldap-server>
    */
    @XmlElement(name = "host", required = true)
    private @NotNull String host;

    @XmlElement(name = "port", defaultValue = "389")
    private int port = 389;

    public LdapServerEntity() {
    }

    public LdapServerEntity(final @NotNull String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
