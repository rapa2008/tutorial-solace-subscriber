package com.solace.subscriber.controller;

import com.solacesystems.jcsmp.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by RA371996 on 3/9/2020.
 */
@RestController
public class Controller {

    JCSMPProperties properties = null;


    private String host;

    private String userName;

    private String vpnName;

    private String password;

    JCSMPSession session = null;

    Topic topic;

    List<String> list;

    public Controller(@Value("${host}") String host, @Value("${my-user-name}") String userName,
                      @Value("${vpn-name}") String vpnName, @Value("${password}") String password) throws JCSMPException {

        properties =  new JCSMPProperties();
        System.out.println("host " + host + "\nusernme "+userName + "\npassword" + password+ "\nvpn "+vpnName);
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.USERNAME, userName);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpnName);
        properties.setProperty(JCSMPProperties.PASSWORD, password);
        properties.setProperty(JCSMPProperties.IGNORE_DUPLICATE_SUBSCRIPTION_ERROR, true);
        session= JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        topic = JCSMPFactory.onlyInstance().createTopic("tutorial/topic");
        list = new ArrayList<>();

    }

    @GetMapping("/message/all")
    public List<String> getAllMessages(){
        return list;
    }

    @GetMapping("/message")
    public List<String> receiveMessage() throws JCSMPException {

        final CountDownLatch latch = new CountDownLatch(1);

        final XMLMessageConsumer cons = session.getMessageConsumer(new XMLMessageListener() {

            @Override
            public void onReceive(BytesXMLMessage msg) {
                System.out.println("Inside onReceive " + msg);
                if (msg instanceof TextMessage) {
                    System.out.printf("TextMessage received: '%s'%n",
                            ((TextMessage)msg).getText());
                    String str = ((TextMessage)msg).getText();
                    list.add(str);
                } else {
                    System.out.println("Message received.");
                }
                System.out.printf("Message Dump:%n%s%n",msg.dump());
                latch.countDown();  // unblock main thread
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n",e);
                latch.countDown();  // unblock main thread
            }
        });
        session.addSubscription(topic);
        cons.start();

        try {
            latch.await(); // block here until message received, and latch will flip
        } catch (InterruptedException e) {
            System.out.println("I was awoken while waiting");
        }
        return list;
    }

/*    @PostMapping("/message/{message}")
    public void sendMessage(@PathVariable String message) throws JCSMPException {
        XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {

            @Override
            public void responseReceived(String messageID) {
                System.out.println("Producer received response for msg: " + messageID);
            }

            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                System.out.printf("Producer received error for msg: %s@%s - %s%n",
                        messageID,timestamp,e);
            }
        });

        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        msg.setText(message);
        prod.send(msg,topic);
        System.out.println("Prodcuer Message sent");
    }*/
}
