/*******************************************************************************
 * Copyright (c) 2012 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip;

import static etherip.protocol.Encapsulation.Command.SendRRData;
import static etherip.protocol.Encapsulation.Command.UnRegisterSession;
import static etherip.types.CNPath.MessageRouter;
import static etherip.types.CNService.Get_Attribute_All;
import static etherip.types.CNService.Get_Attribute_Single;

import etherip.data.CipException;
import etherip.data.ConnectionData;
import etherip.data.EthernetLink;
import etherip.data.Identity;
import etherip.data.InterfaceConfiguration;
import etherip.data.TcpIpInterface;
import etherip.protocol.CIPMultiRequestProtocol;
import etherip.protocol.Connection;
import etherip.protocol.ConnectionDataProtocol;
import etherip.protocol.Encapsulation;
import etherip.protocol.GetConnectionDataProtocol;
import etherip.protocol.GetEthernetLinkProtocol;
import etherip.protocol.GetHexStringDataProtocol;
import etherip.protocol.GetIdentityProtocol;
import etherip.protocol.GetIntAttributeProtocol;
import etherip.protocol.GetInterfaceConfigurationProtocol;
import etherip.protocol.GetPhysicalLinkObjectProtocol;
import etherip.protocol.GetShortAttributeProtocol;
import etherip.protocol.GetStringAttributeProtocol;
import etherip.protocol.GetTcpIpInterfaceProtocol;
import etherip.protocol.ListIdenties;
import etherip.protocol.ListServices;
import etherip.protocol.ListServicesProtocol.Service;
import etherip.protocol.MRChipReadProtocol;
import etherip.protocol.MRChipWriteProtocol;
import etherip.protocol.MessageRouterProtocol;
import etherip.protocol.Protocol;
import etherip.protocol.ProtocolAdapter;
import etherip.protocol.RegisterSession;
import etherip.protocol.SendRRDataProtocol;
import etherip.protocol.TcpConnection;
import etherip.protocol.UdpConnection;
import etherip.protocol.UnconnectedSendProtocol;
import etherip.types.CIPData;
import etherip.types.CNClassPath;
import etherip.types.CNPath;
import etherip.types.CNService;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API for communicating via EtherNet/IP
 *
 * @author Kay Kasemir, László Pataki
 */
@SuppressWarnings("nls")
public class EtherNetIP implements AutoCloseable {
    public static final String version = "1.4";

    public static final Logger logger = Logger.getLogger(EtherNetIP.class.getName());

    private static final int MAX_REQUEST_SIZE = 450;

    private final String address;
    private final int slot;
    protected Connection connection = null;

    /** Initialize
     *  @param address IP address of device
     *  @param slot Slot (0, 1, ...) of the controller on the backplane
     */
    public EtherNetIP(final String address, final int slot) {
        this.address = address;
        this.slot = slot;
    }

    /**
     * Connect to device via TCP, register session
     */
    public void connectTcp() throws Exception {
        this.connection = new TcpConnection(this.address, this.slot);
        this.registerSession();
    }

    /**
     * Connect to device via UDP, register session
     */
    public void connectUdp() throws Exception {
        this.connection = new UdpConnection(this.address, this.slot);
    }

    /**
     * List supported services
     * <p>
     * Queries PLC for supported services. Logs them and checks for the required "Communications" service.
     *
     * @return supported services
     * @throws Exception
     *             on error getting services, or when expected service not supported
     */
    public Service[] listServices() throws Exception {
        final ListServices list_services = new ListServices();
        this.connection.execute(list_services);

        final Service[] services = list_services.getServices();
        if (services == null || services.length < 1) {
            throw new Exception("Device does not support EtherIP services");
        }
        logger.log(Level.FINE, "Service: {0}", services[0].getName());
        if (!services[0].getName().toLowerCase().startsWith("comm")) {
            throw new Exception("Expected EtherIP communication service, got " + services[0].getName());
        }
        return services;
    }

    /**
     * List the identities of the rack
     * <p>
     * Queries PLC rack for identities. Logs them and return the identities.
     *
     * @throws Exception
     *             on error getting services
     */
    public Identity[] listIdentity() throws Exception {
        final ListIdenties list_identities = new ListIdenties();
        this.connection.execute(list_identities);

        final Identity[] identities = list_identities.getIdentities();
        for (final Identity identity : identities) {
            logger.log(Level.FINE, "Identity: ", identity);
        }

        return identities;
    }

    /**
     * Register session
     */
    private void registerSession() throws Exception {
        final RegisterSession register = new RegisterSession();
        this.connection.execute(register);
        this.connection.setSession(register.getSession());
    }

    // Turning off the formatter for the gradual form layout in methods.
    // With this style more readable the structure of the encapsulation in the packet.
    // @formatter:off

    public short getShortAttribute(final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetShortAttributeProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                        (attr_proto = new GetShortAttributeProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public short getShortAttribute(final int slot, final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetShortAttributeProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(slot,
                                        new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                                attr_proto= new GetShortAttributeProtocol()))));
        this.connection.execute(encap);
        return  attr_proto.getValue() ;
    }

    public int getIntAttribute(final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetIntAttributeProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                        (attr_proto = new GetIntAttributeProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public int getIntAttribute(final int slot, final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetIntAttributeProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(slot,
                                        new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                                attr_proto= new GetIntAttributeProtocol()))));
        this.connection.execute(encap);
        return  attr_proto.getValue() ;
    }

    public String getStringAttribute(final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetStringAttributeProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                        (attr_proto = new GetStringAttributeProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public String getStringAttribute(final int slot, final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetStringAttributeProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(slot,
                                        new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                                attr_proto= new GetStringAttributeProtocol()))));
        this.connection.execute(encap);
        return  attr_proto.getValue() ;
    }

    public String getHexStringAttribute(final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetHexStringDataProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                        (attr_proto = new GetHexStringDataProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public String getHexStringAttribute(final int slot, final CNClassPath classPath ,final int instance, final int attr) throws Exception
    {
        final GetHexStringDataProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(slot,
                                        new MessageRouterProtocol(Get_Attribute_Single, classPath.instance(instance).attr(attr),
                                                attr_proto= new GetHexStringDataProtocol()))));
        this.connection.execute(encap);
        return  attr_proto.getValue() ;
    }

    public String getHexStringAttributeAll(final CNClassPath classPath ,final int instance) throws Exception
    {
        final GetHexStringDataProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_All, classPath.instance(instance),
                                        (attr_proto = new GetHexStringDataProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public String getHexStringAttributeAll(final int slot, final CNClassPath classPath ,final int instance) throws Exception
    {
        final GetHexStringDataProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(slot,
                                        new MessageRouterProtocol(Get_Attribute_All, classPath.instance(instance),
                                                attr_proto= new GetHexStringDataProtocol()))));
        this.connection.execute(encap);
        return  attr_proto.getValue() ;
    }

    public Identity getIdentity() throws Exception
    {
        final GetIdentityProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_All, CNPath.Identity(),
                                        (attr_proto = new GetIdentityProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public Identity getSlotIdentity(final int slot) throws Exception
    {
        final GetIdentityProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(slot,
                                        new MessageRouterProtocol(Get_Attribute_All, CNPath.Identity(),
                                                attr_proto = new GetIdentityProtocol()))));
        this.connection.execute(encap);
        return  attr_proto.getValue();
    }

    public ConnectionData getConnectionData() throws Exception
    {
        final GetConnectionDataProtocol attr_proto;

        final Encapsulation encap = new Encapsulation(SendRRData, this.connection.getSession(),
                new SendRRDataProtocol(
                        new ConnectionDataProtocol(
                                attr_proto = new GetConnectionDataProtocol())));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public TcpIpInterface getTcpIpInterface(final int instance) throws Exception
    {
        try
        {
            final GetTcpIpInterfaceProtocol attr_proto;
            final Encapsulation encap =
                    new Encapsulation(SendRRData, this.connection.getSession(),
                            new SendRRDataProtocol(
                                    new MessageRouterProtocol(Get_Attribute_All, CNPath.TcpIpInterface().instance(instance),
                                            (attr_proto = new GetTcpIpInterfaceProtocol()))));
            this.connection.execute(encap);

            return attr_proto.getValue();
        }
        catch(final CipException cipException)
        {
            if(cipException.getStatusCode() == 0x08)
            {
                /**
                 * In case of Get_Attribute_All service not supported. The service is optional, not always implemented.
                 * @see CIP Vol2_1.4: 5-3.3.1
                 */
                return this.getTcpIpInterfaceWithGetAttributeSingle(instance);
            }
            throw cipException;
        }
        catch (final BufferUnderflowException e)
        {
            /**
             * In case of the device not correctly implemented CIP possible BufferUnderFlow exception.
             * In this case may the device will answer correctly with Get_Attribute_Single service.
             * @see CIP Vol2_1.4: 5-3.3.1
             */
            return this.getTcpIpInterfaceWithGetAttributeSingle(instance);
        }
    }

    public TcpIpInterface getTcpIpInterfaceWithGetAttributeSingle(final int instance) throws Exception
    {
        final TcpIpInterface tcpIpInterface = new TcpIpInterface();
        //setInterfaceConfiguration and setHostName must be the first
        tcpIpInterface.setInterfaceConfiguration(this.getInterfaceConfiguration(instance));
        tcpIpInterface.setHostName(this.getStringAttribute(CNPath.TcpIpInterface(), instance, 6));
        tcpIpInterface.setStatus(this.getIntAttribute(CNPath.TcpIpInterface(), instance, 1));
        tcpIpInterface.setConfigurationCapability(this.getIntAttribute(CNPath.TcpIpInterface(), instance, 2));
        tcpIpInterface.setConfigurationControl(this.getIntAttribute(CNPath.TcpIpInterface(), instance, 3));
        tcpIpInterface.setPhysicalLinkObject(this.getPhysicalLinkObject(instance));

        return tcpIpInterface;
    }

    public CNClassPath getPhysicalLinkObject(final int instance) throws Exception
    {
        final GetPhysicalLinkObjectProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_Single, CNPath.TcpIpInterface().instance(instance).attr(4),
                                        (attr_proto = new GetPhysicalLinkObjectProtocol()))));
        this.connection.execute(encap);
        return attr_proto;
    }

    public InterfaceConfiguration getInterfaceConfiguration(final int instance) throws Exception
    {
        final GetInterfaceConfigurationProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_Single, CNPath.TcpIpInterface().instance(instance).attr(5),
                                        (attr_proto = new GetInterfaceConfigurationProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    public EthernetLink getEthernetLink(final int instance) throws Exception
    {
        final GetEthernetLinkProtocol attr_proto;
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new MessageRouterProtocol(Get_Attribute_All, CNPath.EthernetLink().instance(instance),
                                        (attr_proto = new GetEthernetLinkProtocol()))));
        this.connection.execute(encap);
        return attr_proto.getValue();
    }

    /** Read a single scalar tag
     *  @param tag Name of tag
     *  @return Current value of the tag
     *  @throws Exception on error
     */
    public CIPData readTag(final String tag) throws Exception
    {
        return this.readTag(tag, (short) 1);
    }

    public void executeRequest(Protocol request) throws Exception {
        final Encapsulation encap = new Encapsulation(SendRRData, this.connection.getSession(),
                new SendRRDataProtocol(new UnconnectedSendProtocol(slot, request)));
        connection.execute(encap);
    }

    /** Read a single array tag
     *  @param tag Name of tag
     *  @param count Number of array elements to read
     *  @return Current value of the tag
     *  @throws Exception on error
     */
    public CIPData readTag(final String tag, final short count) throws Exception
    {
        final MRChipReadProtocol cip_read = new MRChipReadProtocol(tag, count);
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(this.slot,
                                        cip_read)));
        this.connection.execute(encap);

        return cip_read.getData();
    }


    /** Read multiple scalar tags in one network transaction
     *  @param tags Tag names
     *  @return Current values of the tags
     *  @throws Exception on error
     */
    public CIPData[] readTags(final String... tags) throws Exception
    {
        return readTags(100, tags);
    }

    /** Read multiple strings tags in one network transaction.
     *  Transaction size is adjsuted for the size of string response.
     *  @param tags Tag names
     *  @return Current values of the tags
     *  @throws Exception on error
     */
    public CIPData[] readStringTags(final String... tags) throws Exception
    {
        return readTags(5, tags);
    }

    /** Read multiple strings tags in one network transaction.
     *  Messages are splitted in groups with a maximum number of elements and total request size.
     *  maxGroupSize can be large for small atomic type reads, for large response like string reads, the number should be reduce to make
     *  sure the response fit in a controller total response size limit (about 500 bytes for Logix 5000).
     *  @param maxNumberOfRequestsPerGroup maximum number of requests allowed in a single transaction.
     *  @param tags Tag names
     *  @return Current values of the tags
     *  @throws Exception on error
     */
    public CIPData[] readTags(final int maxNumberOfRequestsPerGroup, final String... tags) throws Exception
    {
        final MRChipReadProtocol[] reads = new MRChipReadProtocol[tags.length];
        for (int i=0; i<reads.length; ++i)
        {
            reads[i] = new MRChipReadProtocol(tags[i]);
        }
        sendMultiMessages(maxNumberOfRequestsPerGroup, reads);

        final CIPData[] results = new CIPData[reads.length];
        for (int i=0; i<results.length; ++i)
        {
            results[i] = reads[i].getData();
        }

        return results;
    }

    /** send multiple messages in a single transaction.
     *  Messages are splitted in groups with a maximum number of elements and total request size.
     *  maxGroupSize can be large for small atomic type reads, for large response like string reads, the number should be reduce to make
     *  sure the response fit in a controller total response size limit (about 500 bytes for Logix 5000).
     *  @param maxNumberOfRequestsPerGroup maximum number of requests allowed in a single transaction.
     *  @param messages messages to send
     *  @throws Exception on error
     */
    public void sendMultiMessages(final int maxNumberOfRequestsPerGroup, final MessageRouterProtocol... messages) throws Exception
    {
        int currentGroupByteCount = 0;
        List<MessageRouterProtocol> groupMessages = new ArrayList<>();

        for (MessageRouterProtocol message : messages)
        {
            int messageSize = message.getRequestSize();
            if (currentGroupByteCount + messageSize > MAX_REQUEST_SIZE || groupMessages.size() == maxNumberOfRequestsPerGroup)
            {
                executeMultiMessages(groupMessages.toArray(new MessageRouterProtocol[groupMessages.size()]));
                currentGroupByteCount = 0;
                groupMessages.clear();
            }
            currentGroupByteCount += messageSize;
            groupMessages.add(message);
        }
        if (groupMessages.size() > 0)
            executeMultiMessages(groupMessages.toArray(new MessageRouterProtocol[groupMessages.size()]));
    }

    /** send multiple messages to controller in a single transaction.
     *  Messages are assumed to fit in controller limits for request and response sizes.
     *  @param messages messages to send
     *  @throws Exception on error
     */
    private void executeMultiMessages(final MessageRouterProtocol[] messages) throws Exception
    {
        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(this.slot,
                                        new MessageRouterProtocol(CNService.CIP_MultiRequest, MessageRouter(),
                                                new CIPMultiRequestProtocol(messages)))));
        this.connection.execute(encap);
    }

    /** Write a tag
     *  @param tag Tag name
     *  @param value Value to write
     *  @throws Exception on error
     */
    public void writeTag(final String tag, final CIPData value) throws Exception
    {
        final MRChipWriteProtocol cip_write = new MRChipWriteProtocol(tag, value);

        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(this.slot,
                                        cip_write)));
        this.connection.execute(encap);
    }

    /** Write multiple tags in one network transaction
     *  @param tags Tag names to write
     *  @param values Values to write
     *  @throws Exception on error
     */
    public void writeTags(final String[] tags, final CIPData[] values) throws Exception
    {
        if (tags.length != values.length)
            throw new IllegalArgumentException("Got " + tags.length + " tags but " + values.length + " values");

        final MRChipWriteProtocol[] writes = new MRChipWriteProtocol[tags.length];
        for (int i=0; i<tags.length; ++i)
            writes[i] = new MRChipWriteProtocol(tags[i], values[i]);

        final Encapsulation encap =
                new Encapsulation(SendRRData, this.connection.getSession(),
                        new SendRRDataProtocol(
                                new UnconnectedSendProtocol(this.slot,
                                        new MessageRouterProtocol(CNService.CIP_MultiRequest, MessageRouter(),
                                                new CIPMultiRequestProtocol(writes)))));
        this.connection.execute(encap);
    }



    //@formatter:on

    /** Unregister session (device will close connection) */
    private void unregisterSession() {
        try {
            if (this.connection.getSession() == 0 || !this.connection.isOpen()) return;

            this.connection.write(
                    new Encapsulation(UnRegisterSession, this.connection.getSession(), new ProtocolAdapter()));
            // Cannot read after this point because PLC will close the connection
        } catch (final Exception ex) {
            logger.log(Level.WARNING, "Error un-registering session: " + ex.getLocalizedMessage(), ex);
        }
    }

    /** Close connection to device */
    @Override
    public void close() throws Exception {
        if (this.connection != null) {
            this.unregisterSession();
            this.connection.close();
        }
    }

    /** @return String representation for debugging */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("EtherIP address '")
                .append(this.address)
                .append("', session 0x")
                .append(Integer.toHexString(this.connection.getSession()))
                .append("\n");
        return buf.toString();
    }

    public Connection getConnection() {
        return this.connection;
    }
}
