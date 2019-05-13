package enu;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.google.api.services.sheets.v4.model.*;
import net.dv8tion.jda.core.JDABuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Main {


    private static LinkedHashMap<String, ArrayList<Object>> realmList = new LinkedHashMap<>(); // list of realms
    private static Discord discord;
    private static GoogleSheet googleSheet;
    private static String url;
    static Thread sheetManager;
    static boolean sheetManagerBoolean;

    public static void main(String[] args) throws InterruptedException, GeneralSecurityException, IOException {
        addFromProperties(loadProperties("src/main/resources/config.properties"));
        new JDABuilder(discord.getToken()).addEventListener(discord).build().awaitReady();
        ValueRange realms = googleSheet.getCellValues("ProcessData!A1:A");
        if (realms.getValues() != null) { //if the google sheet does not connect properly there is no point to continue to run the Code
            wipeData();
            prepareThreads(realms);
        } else {
            discord.stopThread(); //currently not working
        }
    }


    /**
     * Gets data from the properties file and links the with the objects requires
     * Doing this we encapsulate important code like tokens etc from the java files itself and it can be hidden on git.
     * @param prop with all the properties values
     */
    private static void addFromProperties(Properties prop) {
        discord = new Discord(prop.getProperty("discord.token"));
        googleSheet = new GoogleSheet(prop.getProperty("sheets.spreadsheetId"));
        url = prop.getProperty("website.url");
    }


    /**
     * creates a Properties object
     * @param name the name of file
     * @return the properties object
     * @throws IOException if it cant load the file for any reason
     */
    private static Properties loadProperties(String name) throws IOException {
        InputStream input = new FileInputStream(name);
        Properties prop = new Properties();
        prop.load(input);
        return prop;
    }

    private static void prepareThreads(ValueRange realms) {
        realms.getValues().forEach(realm -> realmList.put(realm.toString().substring(1, realm.toString().length() - 1), new ArrayList<>())); // [realm] -> realm
        WipeData wipeSheet1 = createWipeThread(10, 0, 600000); //10 min
        WipeData wipeSheet2 = createWipeThread(23, 30, 3600000); //60 min - should be more readable
        checkIfCloseToWipe(wipeSheet1, wipeSheet2);
    }

    /**
     * clears the realmlist with the realms
     * clears the google sheet fields
     */
    static void wipeData() {
        googleSheet.wipeData("ProcessData!B1:I74");
        realmList.forEach((k, v) -> v.clear());
    }


    /**
     * We would like to not do anything if there is less than 10 min until we are going to wipe the google sheet.
     *
     * @param wipeSheet1 The timer of the first thread created
     * @param wipeSheet2 The timer of the second thread created
     */
    private static void checkIfCloseToWipe(WipeData wipeSheet1, WipeData wipeSheet2) {
        long dif1 = Math.min(getTimeDiff(wipeSheet1.getTimeOfTask()), getTimeDiff(wipeSheet2.getTimeOfTask()));
        if (dif1 > 10) { //if it is more than 10 min to clear we tell the code to start scanning on realms
            startScanProcess();
        }
    }


    /**
     * When the program starts we are checking the current time compared to our wipeData threads.
     * If they are at close proximity we will not checkIfCloseToWipe to scan.
     *
     * @param date is the current time that the thread is supposed to run some code.
     * @return the time in millis between the current time the program starts and when the thread is supposed to invoke the code
     */
    private static long getTimeDiff(Calendar date) {
        long diffInMillis = new Date().getTime() - date.getTime().getTime();
        return Math.abs(TimeUnit.MINUTES.convert(diffInMillis, TimeUnit.MILLISECONDS));
    }

    /**
     * Some times we would like the google sheet to delete all data.
     *
     * @param hour  the current hour
     * @param min   the current minute
     * @param delay after the content is deleted we would like the thread to chill for x amount of time.
     * @return the object itself with some data about when its supposed to invoke the code
     */
    private static WipeData createWipeThread(int hour, int min, long delay) {
        Calendar currentTime = Calendar.getInstance();
        currentTime.set(Calendar.HOUR_OF_DAY, hour);
        currentTime.set(Calendar.MINUTE, min);
        currentTime.set(Calendar.SECOND, 0);
        WipeData wipe = new WipeData(currentTime, delay);
        new Timer().schedule(wipe, currentTime.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)); // period: 1 day
        return wipe;
    }


    /**
     * @param table is a table with different items
     * @return the size of how many items are in the table of items
     * @apiNote this is not coded properly, should be edited
     */
    private static int getItemsSize(HtmlDivision table) {
        if (!(XML.toJSONObject(table.asXml()).getJSONObject("div").get("div") instanceof JSONArray)) {
            return 0;
        }
        JSONObject sizeDiv = XML.toJSONObject(table.asXml()).getJSONObject("div").getJSONArray("div").getJSONObject(0);
        return sizeDiv.has("b") ? sizeDiv.getInt("b") : 0;
    }

    private static HtmlPage getPage(String START_URL) {
        System.out.println(START_URL);
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        setWebClientOptions(webClient);
        try {
            HtmlPage pageContent = webClient.getPage(START_URL);
            pageContent.getWebClient().close();
            return pageContent;
        } catch (IOException ignore) {
        }
        return null;
    }

    /**
     * Creates changes so we can see content after Javascript has been ran on the website.
     *
     * @param webClient is the current webBrowser open
     */
    private static void setWebClientOptions(WebClient webClient) {
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setTimeout(10000);
        webClient.getOptions().setDoNotTrackEnabled(false);
        webClient.setJavaScriptTimeout(8000);
        webClient.waitForBackgroundJavaScript(1000);
    }

    /**
     * Gets the data for a specific realm
     *
     * @param realm is the realm we need for the data to be gathered.
     * @return the data from that website or null
     */
    private static JSONArray getBmahData(String realm) {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        HtmlPage page = getPage(url + URLEncoder.encode(realm, StandardCharsets.UTF_8));
        if (page == null || page.getByXPath("//*[@id=\"w0\"]").isEmpty()) {
            sleepThread(30000); //try again in 30 sec, will update later with unique way to solve the null
            return getBmahData(realm);  //With some recursion we try this until it works
        } else {
            final List<?> itemsDiv = page.getByXPath("//*[@id=\"w0-container\"]");
            return getDataFromTable(page, itemsDiv);
        }
    }


    private static JSONArray getDataFromTable(HtmlPage page, List<?> itemsDiv) {
        int size = getItemsSize((HtmlDivision) page.getByXPath("//*[@id=\"w0\"]").get(0));
        if (size > 0) {
            JSONObject xmlItems = XML.toJSONObject(((HtmlDivision) itemsDiv.get(0)).asXml());
            JSONObject tbody = xmlItems.getJSONObject("div").getJSONObject("table").getJSONObject("tbody");
            return (size == 1 ? new JSONArray(Collections.singletonList(tbody.getJSONObject("tr"))) : tbody.getJSONArray("tr"));
        } else {
            return new JSONArray();
        }
    }

    /**
     * create a specific thread to take over now so it does not interfere with the JDA discord bot
     */
    static void startScanProcess() {
        sheetManagerBoolean = true;
        sheetManager = new Thread(Main::runScan);
        sheetManager.start();
        System.out.println("New scan started");
    }

    /**
     * runs the scan and will stop it if anything changes the boolean sheetManagerBoolean
     */
    static private void runScan() {
        while (sheetManagerBoolean) {
            int column = 1; //bad solution can create object that stores index later
            for (String realm : realmList.keySet()) {
                if (sheetManagerBoolean) {
                    realmScanThreadStarter(realm, column++);
                }
            }
        }
    }

    /**
     * Creates a unique thread for the current realm.
     * there are to many ways a Thread can crash while scraping this specific server, and it can take weeks to debug.
     *
     * @param realm  the name of the current realm
     * @param column the current column
     */
    private static void realmScanThreadStarter(String realm, int column) {
        Thread realmThread = new Thread(() -> addToGoogleSheetData(realm, column));
        realmThread.start();
        while (realmThread.isAlive()) {
            sleepThread(100); // We wont try for a new realm until this one is done
        }
        sleepThread(5000); //try a new realm in 5 seconds
    }


    /**
     * Will only store the data
     *
     * @param realm  the name of the current realm
     * @param column the column that we are going to edit in the google sheet
     */
    private static void addToGoogleSheetData(String realm, int column) {
        getBmahData(realm).forEach(newItem -> {
            if (realmList.get(realm).stream().noneMatch(item -> item.equals(getName(newItem)))) {
                realmList.get(realm).add(getName(newItem));
            }
        });
        googleSheet.insertData("ProcessData!B" + column + ":I", realmList.get(realm));
    }


    /**
     * @param item has multiple information like title, current bid etc
     * @return the the name of the item
     */
    private static String getName(Object item) {
        return (String) ((JSONObject) item).getJSONArray("td").getJSONObject(0).getJSONObject("a").get("title");
    }

    /**
     * since we dont use the Thread.interrupt, Il create my own way to interrupt threads and also use this method to sleep them.
     *
     * @param time how long we would like the thread to sleep in millis
     */
    static void sleepThread(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) {
        }
    }


}
