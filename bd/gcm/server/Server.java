package it.polito.ws_vs_gcm.bd.gcm.server;

public class Server {
private static final Logger logger = Logger.getLogger("SmackCcsTime");
private static final String GCM_SERVER = "gcm.googleapis.com";
private static final int GCM_PORT = 5235;
private static final String GCM_ELEMENT_NAME = "gcm";
private static final String GCM_NAMESPACE = "google:mobile:data";
static int msgId;
public static String from;
public static boolean flag=false;
private static final String characters = "abcdefghijklmnopqrstuvwxyz";
static FileWriter writer;
static PrintWriter out;

static {
        ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
                                             new PacketExtensionProvider() {
                        // @Override
                        public PacketExtension parseExtension(XmlPullParser parser)
                        throws Exception {
                                String json = parser.nextText();
                                return new GcmPacketExtension(json);
                        }
                });
}

private XMPPConnection connection;
protected volatile boolean connectionDraining = false;
public boolean sendDownstreamMessage(String jsonRequest)
throws SmackException.NotConnectedException {
        if (!connectionDraining) {
                send(jsonRequest);
                return true;
        }
        logger.info("Dropping downstream message since the connection is draining");
        return false;
}

public String nextMessageId() {
        return "m-" + UUID.randomUUID().toString();
}

protected void send(String jsonRequest) throws SmackException.NotConnectedException {
        Packet request = new GcmPacketExtension(jsonRequest).toPacket();
        connection.sendPacket(request);
}

protected void handleUpstreamMessage(Map<String, Object> jsonObject,
                                     String id) throws SQLException, IOException {
        // PackageName of the application that sent this message.

        String category = (String) jsonObject.get("category");
        String from = (String) jsonObject.get("from");
        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>)jsonObject
                                      .get("data");
        payload.put("ECHO", "Application: " + category);
        payload.put("EmbeddedMessageId", id);
        String echo = createJsonMessage(from, nextMessageId(), payload, "echo:CollapseKey", null, false);

        try {
                sendDownstreamMessage(echo);
        } catch (SmackException.NotConnectedException e) {
                logger.log(Level.WARNING,
                           "Not connected anymore, echo message is not sent", e);
        }
}

protected void handleAckReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");
        String from = (String) jsonObject.get("from");
        logger.log(Level.INFO, "handleAckReceipt() from: " + from
                   + ", messageId: " + messageId);
}

protected void handleNackReceipt(Map<String, Object> jsonObject) {
        String messageId = (String) jsonObject.get("message_id");
        String from = (String) jsonObject.get("from");
        logger.log(Level.INFO, "handleNackReceipt() from: " + from
                   + ", messageId: " + messageId);
}

protected void handleControlMessage(Map<String, Object> jsonObject) {
        logger.log(Level.INFO, "handleControlMessage(): " + jsonObject);
        String controlType = (String) jsonObject.get("control_type");
        if ("CONNECTION_DRAINING".equals(controlType)) {
                connectionDraining = true;
        } else {
                logger.log(Level.INFO,
                           "Unrecognized control type: %s. This could happen if new features are "
                           + "added to the CCS protocol.", controlType);
        }
}

public static String createJsonMessage(String to, String messageId,
                                       Map<String, String> payload, String collapseKey, Long timeToLive,
                                       Boolean delayWhileIdle) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("to", to);
        if (collapseKey != null) {
                message.put("collapse_key", collapseKey);
        }
        if (timeToLive != null) {
                message.put("time_to_live", timeToLive);
        }
        if (delayWhileIdle != null && delayWhileIdle) {
                message.put("delay_while_idle", false);
        }
        message.put("message_id", messageId);
        message.put("data", payload);
        return JSONValue.toJSONString(message);
}

protected static String createJsonAck(String to, String messageId) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("message_type", "ack");
        message.put("to", to);
        message.put("message_id", messageId);
        return JSONValue.toJSONString(message);
}

public void connect(long senderId, String apiKey) throws XMPPException,
IOException, SmackException {
        ConnectionConfiguration config = new ConnectionConfiguration(
                GCM_SERVER, GCM_PORT);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        config.setReconnectionAllowed(true);
        config.setRosterLoadedAtLogin(false);
        config.setSendPresence(false);
        config.setDebuggerEnabled(true);
        config.setSocketFactory(SSLSocketFactory.getDefault());
        Runnable r = new Send_messages();
        new Thread(r).start();
        connection = new XMPPTCPConnection(config);
        connection.connect();
        connection.addConnectionListener(new LoggingConnectionListener());
        connection.addPacketListener(new PacketListener() {
                        // @Override
                        public void processPacket(Packet packet) {
                                Message incomingMessage = (Message) packet;
                                GcmPacketExtension gcmPacket = (GcmPacketExtension) incomingMessage
                                                               .getExtension(GCM_NAMESPACE);
                                String json = gcmPacket.getJson();
                                try {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> jsonObject = (Map<String, Object>)JSONValue
                                                                         .parseWithException(json);
                                        Object messageType = jsonObject.get("message_type");
                                        Map messageData = (Map) jsonObject.get("data");
                                        if (messageType == null) {
                                                String messageId = (String) jsonObject.get("message_id");
                                                String payload1 = (String) messageData.get("PAYLOAD");
                                                if (payload1.equals("BATTERY") == true) {
                                                        String payload2 = (String) messageData.get("BATTERY_INFO");
                                                        String path = "/battery_consumption_GCM_tablet_orange.txt";
                                                        File file = new File(path);
                                                        try {
                                                                if (file.exists()) {
                                                                        System.out.println("The file " + path + " exists");
                                                                } else if (file.createNewFile()) {
                                                                        System.out.println("The file " + path + " has been created.");
                                                                } else {
                                                                        System.out.println("The file " + path + " cannot be created.");
                                                                }
                                                        } catch (IOException e) {
                                                                e.printStackTrace();
                                                                System.out.println("File non creato");
                                                        }
                                                        try {
                                                                FileWriter fw = new FileWriter(file, true);
                                                                fw.write(payload2);
                                                                fw.close();
                                                        } catch (IOException e) {
                                                                e.printStackTrace();
                                                        }
                                                }

                                                if (payload1.equals("START") == true) {
                                                        from = (String) jsonObject.get("from");
                                                        String ack = createJsonAck(from, messageId);
                                                        send(ack);
                                                        flag=true;
                                                }
                                        } else if ("ack".equals(messageType.toString())) {
                                                // Process Ack
                                                handleAckReceipt(jsonObject);
                                        } else if ("nack".equals(messageType.toString())) {
                                                // Process Nack
                                                handleNackReceipt(jsonObject);
                                        } else if ("control".equals(messageType.toString())) {
                                                // Process control message
                                                handleControlMessage(jsonObject);
                                        } else {
                                                logger.log(Level.WARNING,
                                                           "Unrecognized message type (%s)",
                                                           messageType.toString());
                                        }
                                } catch (ParseException e) {
                                        logger.log(Level.SEVERE, "Error parsing JSON " + json, e);
                                } catch (Exception e) {
                                        logger.log(Level.SEVERE, "Failed to process packet", e);
                                }

                        }
                }, new PacketTypeFilter(Message.class));

        connection.addPacketInterceptor(new PacketInterceptor() {
                        @Override
                        public void interceptPacket(Packet packet) {
                                logger.log(Level.INFO, "Sent: {0}", packet.toXML());
                        }
                }, new PacketTypeFilter(Message.class));

        connection.login(senderId + "@gcm.googleapis.com", apiKey);
}

public static String generateRandomString(int length) {
        Random rn = new Random();
        final StringBuffer sb = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
                sb.append(characters.charAt(rn.nextInt(25)));
        }
        return sb.toString();
}

public class Send_messages implements Runnable {

public Send_messages() {

}

public void run() {
        while (true) {
                if (flag == true) {
                        Map<String, String> payload = new HashMap<String, String>();
                        String msgID = String.valueOf(msgId++);
                        String sequence_of_characters = generateRandomString(132);
                        payload.put("ack_msgid", msgID);
                        payload.put("ack_payload", sequence_of_characters);
                        String collapseKey = "pip";
                        Long timeToLive = 60l;// seconds

                        String ack_to_client = createJsonMessage(from, msgID, payload, null, timeToLive, false);
                        try {
                                send(ack_to_client);
                        } catch (SmackException.NotConnectedException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
                try {
                        Thread.sleep(500);
                } catch (InterruptedException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
}
}

public static void main(String[] args) throws Exception {
        final long senderId = 1L; // PUT GCM SENDER ID HERE
        final String password = "1"; // PUT GCM API KEY HERE
        msgId = 1;
        Server ccsClient = new Server();
        ccsClient.connect(senderId, password);
        String toRegId = "1"; // PUT GCM CLIENT REG KEY HERE
        String messageId = UUID.randomUUID().toString();
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("MESSAGE", "THE SERVER IS NOW IN SERVICE.");
        payload.put("EmbeddedMessageId", messageId);
        String collapseKey = "pip";
        Long timeToLive = 10l;// seconds
        String message = createJsonMessage(toRegId, messageId, payload,
                                           null, timeToLive, false);
        ccsClient.sendDownstreamMessage(message);

        while (true) {
                Thread.sleep(1000);
        }

}

private static final class GcmPacketExtension extends
        DefaultPacketExtension {

private final String json;

public GcmPacketExtension(String json) {
        super(GCM_ELEMENT_NAME, GCM_NAMESPACE);
        this.json = json;
}

public String getJson() {
        return json;
}

@Override
public String toXML() {
        return String.format("<%s xmlns=\"%s\">%s</%s>", GCM_ELEMENT_NAME,
                             GCM_NAMESPACE, StringUtils.escapeForXML(json),
                             GCM_ELEMENT_NAME);
}

public Packet toPacket() {
        Message message = new Message();
        message.addExtension(this);
        return message;
}
}

private static final class LoggingConnectionListener implements
        ConnectionListener {

@Override
public void connected(XMPPConnection xmppConnection) {
        logger.info("Connected.");
}

@Override
public void authenticated(XMPPConnection xmppConnection) {
        logger.info("Authenticated.");
}

@Override
public void reconnectionSuccessful() {
        logger.info("Reconnecting..");
}

@Override
public void reconnectionFailed(Exception e) {
        logger.log(Level.INFO, "Reconnection failed.. ", e);
}

@Override
public void reconnectingIn(int seconds) {
        logger.log(Level.INFO, "Reconnecting in %d secs", seconds);
}

@Override
public void connectionClosedOnError(Exception e) {
        logger.info("Connection closed on error.");
}

@Override
public void connectionClosed() {
        logger.info("Connection closed.");
}
}
}
