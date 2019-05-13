package enu;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Having all the parts of the googlesheet code into file.
 *
 * Has methods that will input or read data from a sheet
 * also stores multiple variables required to connect to the gooogle sheet.
 */
class GoogleSheet {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    final String spreadsheetId;
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private Sheets service;

    String getSpreadsheetId() {
        return spreadsheetId;
    }


    GoogleSheet(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
        buildService();
    }

    private void buildService() {
        try {
            final NetHttpTransport HTTP_TRANSPORT;
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName("enu - Infamous - bmAH").build();
        } catch (GeneralSecurityException | IOException ignored) {
        }


    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) {
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH)));
            FileDataStoreFactory cred = new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(cred).setAccessType("offline").build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (IOException ignored) {
            return null;
        }
    }

    Sheets getService() {
        return service;
    }

    void wipeData(String range) {
        try {
            service.spreadsheets().values().clear(spreadsheetId, range, new ClearValuesRequest()).execute(); //i74 can be realmlist.length
        } catch (IOException ignored) {
        }
    }

    ValueRange getCellValues(String range) {
        try {
            BatchGetValuesResponse readResult = service.spreadsheets().values().batchGet(spreadsheetId).setRanges(Collections.singletonList(range)).execute();
            return readResult.getValueRanges().get(0);
        } catch (IOException ignored) {
        }
        return null;
    }

    public void insertData(String range, ArrayList<Object> items) {
        try {
            ValueRange body = new ValueRange().setValues(Collections.singletonList(items));
            this.getService().spreadsheets().values().update(spreadsheetId, range, body).setValueInputOption(
                    "RAW").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
