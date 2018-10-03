package com.contoso.sblistenerjavademo01;

import org.apache.commons.lang3.time.StopWatch;

public class Program 
{
    static StopWatch _stopwatch = new StopWatch();
    
    public static void main( String[] args )
    {
        RunStopwatch();
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
