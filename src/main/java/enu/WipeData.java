package enu;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Creates a unique thread for deleting data at a specific time.
 * It will then wait x amount of time before start scanning a server.
 * This thread only invokes the task itself every 24 hour.
 */
public class WipeData extends TimerTask {


    private Boolean sameDay;
    private Calendar timeOfTask;
    private long delay;


    WipeData(Calendar today, long delay) {
        this.delay = delay;
        this.timeOfTask = today;
        sameDay = new Date().after(today.getTime());
    }

    public String getThreadData() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String formattedDate = dateFormat.format(timeOfTask.getTime());
        return "New Thread clear the google sheet at [" + formattedDate + "] then delay " + TimeUnit.MILLISECONDS.toMinutes(delay) + " min and then scan";
    }

    // Add your task here
    public void run() {
        System.out.println(new Date() + " > ");
        if (!sameDay) {
            if (Main.sheetManager != null && Main.sheetManager.isAlive()) {
                System.out.println("lets stop the data");
                Main.sheetManagerBoolean = false;
                while (Main.sheetManager.isAlive()) {
                    System.out.println("lets hope i am interrupting the data");
                    Main.sleepThread(100);
                }
            }
            System.out.println("lets wipe the data");
            Main.wipeData();
            Main.sleepThread((int) delay); // 10 min
            Main.startScanProcess();
        } else {
            sameDay = true;
        }
    }

    public Calendar getTimeOfTask() {
        return timeOfTask;
    }
}
