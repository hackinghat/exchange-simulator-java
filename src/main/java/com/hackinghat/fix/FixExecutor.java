package com.hackinghat.fix;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

import javax.management.JMException;
import javax.management.ObjectName;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.Map.Entry;


public class FixExecutor {
    private static final Logger log = LoggerFactory.getLogger(quickfix.examples.executor.Executor.class);
    private final SocketAcceptor acceptor;
    private final Map<InetSocketAddress, List<TemplateMapping>> dynamicSessionMappings = new HashMap<>();
    private final JmxExporter jmxExporter;
    private final ObjectName connectorObjectName;

    public FixExecutor(SessionSettings settings) throws ConfigError, FieldConvertError, JMException {
        FixApplication application = new FixApplication(settings);
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        this.acceptor = new SocketAcceptor(application, messageStoreFactory, settings, logFactory, messageFactory);
        this.configureDynamicSessions(settings, application, messageStoreFactory, logFactory, messageFactory);
        this.jmxExporter = new JmxExporter();
        this.connectorObjectName = this.jmxExporter.register(this.acceptor);
        log.info("Acceptor registered with JMX, name={}", this.connectorObjectName);
    }

    public static void main(String[] args) throws Exception {
        try {
            InputStream inputStream = getSettingsInputStream(args);
            SessionSettings settings = new SessionSettings(inputStream);
            inputStream.close();
            FixExecutor executor = new FixExecutor(settings);
            executor.start();
            System.out.println("press <enter> to quit");
            System.in.read();
            executor.stop();
        } catch (Exception var4) {
            log.error(var4.getMessage(), var4);
        }

    }

    private static InputStream getSettingsInputStream(String[] args) throws FileNotFoundException {
        InputStream inputStream = null;
        if (args.length == 0) {
            inputStream = quickfix.examples.executor.Executor.class.getResourceAsStream("executor.cfg");
        } else if (args.length == 1) {
            inputStream = new FileInputStream(args[0]);
        }

        if (inputStream == null) {
            System.out.println("usage: " + quickfix.examples.executor.Executor.class.getName() + " [configFile].");
            System.exit(1);
        }

        return inputStream;
    }

    private void configureDynamicSessions(SessionSettings settings, FixApplication application, MessageStoreFactory messageStoreFactory, LogFactory logFactory, MessageFactory messageFactory) throws ConfigError, FieldConvertError {
        Iterator sectionIterator = settings.sectionIterator();

        while (sectionIterator.hasNext()) {
            SessionID sessionID = (SessionID) sectionIterator.next();
            if (this.isSessionTemplate(settings, sessionID)) {
                InetSocketAddress address = this.getAcceptorSocketAddress(settings, sessionID);
                this.getMappings(address).add(new TemplateMapping(sessionID, sessionID));
            }
        }

        for (Entry<InetSocketAddress, List<TemplateMapping>> inetSocketAddressListEntry : this.dynamicSessionMappings.entrySet()) {
            this.acceptor.setSessionProvider(inetSocketAddressListEntry.getKey(), new DynamicAcceptorSessionProvider(settings, inetSocketAddressListEntry.getValue(), application, messageStoreFactory, logFactory, messageFactory));
        }

    }

    private List<TemplateMapping> getMappings(InetSocketAddress address) {
        return this.dynamicSessionMappings.computeIfAbsent(address, (k) -> new ArrayList<>());
    }

    private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        String acceptorHost = "0.0.0.0";
        if (settings.isSetting(sessionID, "SocketAcceptAddress")) {
            acceptorHost = settings.getString(sessionID, "SocketAcceptAddress");
        }

        int acceptorPort = (int) settings.getLong(sessionID, "SocketAcceptPort");
        return new InetSocketAddress(acceptorHost, acceptorPort);
    }

    private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, "AcceptorTemplate") && settings.getBool(sessionID, "AcceptorTemplate");
    }

    private void start() throws RuntimeError, ConfigError {
        this.acceptor.start();
    }

    private void stop() {
        try {
            this.jmxExporter.getMBeanServer().unregisterMBean(this.connectorObjectName);
        } catch (Exception var2) {
            log.error("Failed to unregister acceptor from JMX", var2);
        }

        this.acceptor.stop();
    }
}
