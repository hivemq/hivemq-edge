package com.hivemq.edge;

public interface HiveMQEdgeConstants {

    String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    String CLIENT_AGENT_PROPERTY_VALUE = "HiveMQ-Edge; %s";

    String DEVELOPMENT_MODE = "hivemq.edge.workspace.modules";
    String CONFIG_FILE_NAME = "hivemq.edge.config.xml";
    String MUTABLE_CONFIGURAION_ENABLED = "mutable.configuration.enabled";
    String CONFIGURATION_EXPORT_ENABLED = "configuration.export.enabled";
    String VERSION_PROPERTY = "version";
    String CLIENT_AGENT_PROPERTY = "client-agent";

    int MAX_ID_LEN = 500;
    int MAX_NAME_LEN = 256;
    String NAME_REGEX = "^[A-Za-z0-9-_](?:[A-Za-z0-9_ -]*[A-Za-z0-9_-])$"; //-- alpha-num with spaces, underscore and hyphen (but NOT starting or ending with spaces)
    int MAX_UINT16 = 65535;
    String MAX_UINT16_String = "65535";
    String HOSTNAME_REGEX = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

    //credits: Regular Expressions Cookbook by Steven Levithan, Jan Goyvaerts
    String IPV4_REGEX = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    //credits: Regular Expressions Cookbook by Steven Levithan, Jan Goyvaerts
    String IPV6_REGEX = "(?i)^(?:(?:(?:[A-F0-9]{1,4}:){6}|(?=(?:[A-F0-9]{0,4}:){0,6}(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$)(([0-9A-F]{1,4}:){0,5}|:)((:[0-9A-F]{1,4}){1,5}:|:))(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|(?:[A-F0-9]{1,4}:){7}[A-F0-9]{1,4}|(?=(?:[A-F0-9]{0,4}:){0,7}[A-F0-9]{0,4}$)(([0-9A-F]{1,4}:){1,7}|:)((:[0-9A-F]{1,4}){1,7}|:))$";
}
