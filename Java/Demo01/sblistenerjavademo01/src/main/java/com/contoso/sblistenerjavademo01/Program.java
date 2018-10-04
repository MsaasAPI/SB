package com.contoso.sblistenerjavademo01;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

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
    static String _caseNumber = "";
    static String _eventType = "";
    static String _entityAction = "";
    static Integer _cnt = 1;

    /***** USER CONFIGURABLE FIELDS *****/
    static ReceiveMode RECEIVE_MODE = ReceiveMode.PEEKLOCK;

    public static void main( String[] args )
    {
        Initialize();
        RunStopwatch();
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
                Thread.sleep(10);
                System.out.print('\r' + _stopwatch.toString().substring(3, 11) + " ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
