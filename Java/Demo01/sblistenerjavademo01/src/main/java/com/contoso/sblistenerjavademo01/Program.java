package com.contoso.sblistenerjavademo01;

import java.io.File;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.json.*;

public class Program
{
    /***** CREDENTIALS *****/
    static final String CONNECTION_STRING = "";
    static final String TOPIC_PATH = "";
    static final String SUBSCRIPTION = "";

    /***** OTHER CONSTANTS & STATIC FIELDS *****/
    static ISubscriptionClient _subscriptionClient;
    static Date _timeStamp;
    static StopWatch _stopwatch = new StopWatch();
    static IMessage _receivedMessage;
    static String _caseNumber = "";
    static String _eventType = "";
    static String _entityAction = "";
    static String _messageBody = "";
    static Integer _cnt = 1;
    static AtomicInteger _totalReceived = new AtomicInteger(0);
    /**
     * Configure the message handler options in terms of exception handling, number
     * of concurrent messages to deliver, etc. The parameters are:
     * 
     * - maxConcurrentCalls：maximum concurrent calls to the onMessage handler
     * 
     * - autoComplete： true if the pump should automatically complete message a t r
     * ageHandler action is completed. false otherwise.
     * 
     * - maxAutoRenewDuration： Maximum duration the client keeps renewing message
     * lock if the processing of the message is not completed by the handler.
     */
    static MessageHandlerOptions _messageHandlerOptions = new MessageHandlerOptions(1, false, Duration.ofMinutes(1));


    /***** USER CONFIGURABLE FIELDS *****/
    static final String PATH = "Logs"; // Dynamically create a Logs folder for storing the logs
    static ReceiveMode RECEIVE_MODE = ReceiveMode.RECEIVEANDDELETE;

    public static void main( String[] args )
    {
        RunServiceBusListener();
    }

    /**
     * Core implementation of Service Bus listener per
     * https://github.com/Azure/azure-service-bus/blob/master/samples/Java/azure-servicebus/QueuesGettingStarted/src/main/java/com/microsoft/azure/servicebus/samples/queuesgettingstarted/QueuesGettingStarted.java
     */
    static void RunServiceBusListener() {
        try {
            Initialize();
            RegisterEvents();
            RunStopwatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Print Disclaimer, Tip and instantiate Subscription Client.
     * 
     * Reference on Peek Lock Expiration:
     * https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-performance-improvements
     */
    static void Initialize() {
        try {
            String additionalNote = RECEIVE_MODE == ReceiveMode.PEEKLOCK
                                    ? ". By default, the message lock expires after 60 seconds."
                                    : "";

            System.out.println("---------------------------------------------------------------------------------");
            System.out.println(" Microsoft (R)  Windows Azure SDK                                                ");
            System.out.println(" Software Development Kit                                                        ");
            System.out.println("                                                                                 ");
            System.out.println(" Copyright (c) Microsoft Corporation. All rights reserved.                       ");
            System.out.println("                                                                                 ");
            System.out.println(" THIS CODE AND INFORMATION ARE PROVIDED 'AS IS' WITHOUT WARRANTY OF ANY KIND,    ");
            System.out.println(" EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES");
            System.out.println(" OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.                     ");
            System.out.println("---------------------------------------------------------------------------------");
            System.out.println();
            System.out.println("==============================================================================");
            System.out.println("Press any CTRL + C to exit after receiving all the messages");
            System.out.println("Receive Mode: " + RECEIVE_MODE + additionalNote);
            System.out.println("==============================================================================");
            System.out.println();

            // The following credentials shall be obtained from Microsoft tech support.
            _subscriptionClient = new SubscriptionClient(
                    new ConnectionStringBuilder(CONNECTION_STRING, TOPIC_PATH + "/subscriptions/" + SUBSCRIPTION),
                    RECEIVE_MODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register handlers for Service Bus message arrival
     */
    static void RegisterEvents() {
        try {
            // Invoke onMessageAsync upon Service Bus message arrival.
            // Note that OnMessageAsync is of type IMessageHandler
            _subscriptionClient.registerMessageHandler(new IMessageHandler() {
                public CompletableFuture<Void> onMessageAsync(IMessage receivedMessage) {
                    ResetFields();
                    _timeStamp = new Date();
                    _receivedMessage = receivedMessage;
                    ParseMessage();
                    LogMessageToFile();
                    PrintMessageOnScreen();
                    return null;
                }

                public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
                    System.out.printf(exceptionPhase + "-" + throwable.getMessage());
                }
            }, _messageHandlerOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset all message content carrying fields to prevent contamination across
     * different messages.
     */
    static void ResetFields() {
        _receivedMessage = null;
        _caseNumber = "";
        _eventType = "";
        _entityAction = "";
        _messageBody = "";
    }

    /**
     * Extract info from the single received Service Bus message.
     */
    static void ParseMessage() {
        try {
            if (_receivedMessage == null)
                return;

            ParseMessageBody();
            ParseEntityAction();
            _caseNumber = _receivedMessage.getProperties().get("CaseNumber").toString().trim();
            _eventType = _receivedMessage.getProperties().get("EventType").toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Isolate Message Body JSON
     */
    static void ParseMessageBody() {
        try {
            String rawBody = new String(_receivedMessage.getBody(), "UTF-8");
            int startIndex = rawBody.indexOf("{");
            int endIndex = rawBody.lastIndexOf("}") + 1;
            _messageBody = rawBody.substring(startIndex, endIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve Entity Action from either property or body JSON depending on the
     * Service Bus message version.
     */
    static void ParseEntityAction() {
        try {
            // Parse v1 entity actions
            _entityAction = _receivedMessage.getProperties().get("EntityAction");

            // Parse v0 entity actions
            if (_entityAction == null || _entityAction.length() == 0) {
                    JSONObject parsedJson = new JSONObject(_messageBody);
                    _entityAction = parsedJson.getString("EntityAction");
            }
        } catch (Exception ex) {
            // If JSON parsing failed, brute force approach is as below
            int bruteForceStart = _messageBody.indexOf("EntityAction") + 16;
            int bruteForceEnd = _messageBody.indexOf('"', bruteForceStart);
            _entityAction = _messageBody.substring(bruteForceStart, bruteForceEnd - bruteForceStart);
            System.out.println("Entity Action: ------ " + _entityAction + " ------ " + ex);
        }
    }

    /**
     * Log message info to log file on disk.
     */
    static void LogMessageToFile() {
        try{
            if (_receivedMessage == null)
                return;

            // Determine whether the directory exists. If not, create one
            File directory = new File(PATH);

            if (!directory.exists())
                directory.mkdir();

            String logContent = new SimpleDateFormat("HH:mm:ss").format(_timeStamp) + ", Case Number: " + _caseNumber
                    + ", Receive Mode: " + RECEIVE_MODE + "\r\n\r\n";
            String logName = "log_" + new SimpleDateFormat("yyyy_MMdd_HHmm").format(_timeStamp) + "_ServiceBus.txt";
            // in order to implement "write-to-file oneliner", we need to know upfront
            // whether the target file is existent to set proper option flag
            StandardOpenOption standardOpenOption = new File(PATH + "/" + logName).exists() 
                                                        ? StandardOpenOption.APPEND
                                                        : StandardOpenOption.CREATE_NEW;

            // Iterate through and log all properties
            for (Map.Entry<String, String> p : _receivedMessage.getProperties().entrySet())
                logContent += String.valueOf(p.getKey()) + ": " + String.valueOf(p.getValue()) + "\r\n";

            // Log Body JSON
            logContent += "Body:\r\n" + _messageBody + "\r\n\r\n\r\n\r\n";

            Files.write(Paths.get(PATH + "/" + logName), logContent.getBytes(), standardOpenOption);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Print crucial info on the console window.
     */
    static void PrintMessageOnScreen() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

        System.out
                .println(
                    StringUtils.leftPad((_cnt++).toString(), 4, " ") + " - " 
                    + simpleDateFormat.format(_timeStamp) + " " 
                    + _caseNumber + " - " 
                    + StringUtils.leftPad(_eventType, 6, " ") + ": " 
                    + _entityAction);
    }

    /**
     * Keep running Stopwatch and render the elapsed time, until CTRL + C is pressed
     * by user.
     */
    static void RunStopwatch() {
        try {
            _stopwatch.start();

            while (true) {
                Thread.sleep(1);
                System.out.print('\r' + _stopwatch.toString().substring(3, 11) + " ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
