import com.apple.eawt.Application;
import com.box.sdk.*;
import com.google.gson.Gson;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;


public final class Main {

    private static final int MAX_DEPTH = 1;
    private static final String FILE = "717438__config.json";
    public static final String NANO_UPD_FOLDER_ID = "61187224949";
    public static final String V2_UPD_FOLDER_ID = "61183179245";
    public static final String NANO_LOG_FOLDER_ID = "61826054892";
    public static final String V2_LOG_FOLDER_ID = "61833216057";
    public static final String LOG_Mavic = "61825258272";
    public static final String LOG_Phantom = "61821440993";
    public static final String LOG_M200 = "66412007725";
    public static final String LOG_M600 = "66416905755";
    public static final String LOG_Other = "61829831872";
    public static final String LOG_FOLDER_ID = "61181527968";
    public static final String NOTIFICATION_FOLDER = "61843014957";
    public static TrayIcon trayIcon;
    public static OSType osType;
    public static AppConfig APP_CONFIG;
    public static String localLogFolder = "devices/";

    public static void main(String[] args) throws InterruptedException, IOException, AWTException {
//        System.setProperty("apple.awt.UIElement", "true");
        Security.setProperty("crypto.policy", "unlimited");
        final String os = System.getProperty("os.name", "nix").toLowerCase();
        // OS-specific
        if (os.startsWith("win")) {
            osType = OSType.WIN;
        } else if (os.startsWith("mac")) {
            osType = OSType.MAC;
        }
        showNotification("Start background service", "Service started");
        File file = new File("config.json");
        int a = 0;
        while (a == 0){
            sleep( 5 * 1000);
            if (file.exists()) {
                a = 1;
            }
        }
//        File file = new File("config.json");
//        if (!file.exists()) {
//            AppConfig defaultConfig = new AppConfig();
//            defaultConfig.setIsLicenseApproved(false);
//            defaultConfig.setIsFirstStart(true);
//            defaultConfig.setSyncTimeMin(1);
//            defaultConfig.setRootFolder(Paths.get("").toAbsolutePath().toString());
//            saveAppConfig(defaultConfig);
//        }



        String configContent = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
        APP_CONFIG = new Gson().fromJson(configContent, AppConfig.class);


        Timer timer = new Timer(false);
        timer.schedule(new PeriodicCheck(), 0, APP_CONFIG.getSyncTimeMin() * 60 * 1000);

        // Turn off logging to prevent polluting the output.
        Logger.getLogger("com.box.sdk").setLevel(Level.OFF);
        BoxDeveloperEditionAPIConnection api = getApi();


        BoxUser.Info userInfo = BoxUser.getCurrentUser(api).getInfo();
        System.out.format("Welcome, %s <%s>!\n\n", userInfo.getName(), userInfo.getLogin());

//        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        BoxFolder logFolder = new BoxFolder(api, LOG_FOLDER_ID);


        listFolder(logFolder, 0);


//        uploadLogs(logFolder, api);

        while (true) {
            sleep(24 * 60 * 60 * 1000);
        }
    }

    public static List<String> getListDirNames(String path) {
        File direktory = new File(path);
        String[] directories = direktory.list((current, name) -> new File(current, name).isDirectory());

        return new ArrayList<>(Arrays.asList(directories));
    }

    public static List<String> getListFileNames(String path) {
        File direktory = new File(path);
        String[] directories = direktory.list((current, name) -> new File(current, name).isFile());
        System.out.println(Arrays.toString(directories));

        return new ArrayList<>(Arrays.asList(directories));
    }

    public static void uploadLogsTemp(BoxDeveloperEditionAPIConnection api) throws IOException {

        for (int i=0; i < APP_CONFIG.getDrones().size(); i++) {
            String DronType = APP_CONFIG.getDrones().get(i).getDroneType();
            String DronModel = APP_CONFIG.getDrones().get(i).getDroneModel();
            String DroneUID = APP_CONFIG.getDrones().get(i).getUID().replace(" ", "");
            String path = "devices/SmartAir Nano/" + DroneUID + "/";
            String folderId = "";
            BoxFolder typeFolder;
            List<String> filesToUploaad = getListFileNames(path);
            List<String> listFoldersToUpload = getListDirNames(path);


            switch (DronType) {
                case "Mavic": {
                    typeFolder = new BoxFolder(api, LOG_Mavic);
                    break;
                }
                case "Phantom": {
                    typeFolder = new BoxFolder(api, LOG_Phantom);
                    break;
                }
                case "M200": {
                    typeFolder = new BoxFolder(api, LOG_M200);
                    DronModel = "M200";
                    break;
                }
                case "M600": {
                    typeFolder = new BoxFolder(api, LOG_M600);
                    DronModel = "M600";
                    break;
                }
                default: {
                    typeFolder = new BoxFolder(api, LOG_Other);
                    DronModel = "Other";
                    break;
                }
            }
            folderId = createFolderAndGetId(typeFolder, DronModel);
            listFolder(typeFolder, 1);
            System.out.println(folderId);
            BoxFolder DronModelFolder = new BoxFolder(api, folderId);
            folderId = createFolderAndGetId(DronModelFolder, DroneUID);
            BoxFolder logsUploadFolder = new BoxFolder(api, folderId);
            for (String aFilesToUploaad : filesToUploaad) {
                uploadFile(logsUploadFolder, path + aFilesToUploaad, aFilesToUploaad);
            }
            HashMap<String, String> existingFolders = getFolder(logsUploadFolder, 1);
            for (String key : existingFolders.keySet()) {
                listFoldersToUpload.remove(key);
            }

            for (String anListFoldersToUpload : listFoldersToUpload) {
                uploadFolder(logsUploadFolder, anListFoldersToUpload, api, path);
            }
        }

    }

    public static void uploadLogs(BoxFolder logFolder, BoxDeveloperEditionAPIConnection api, String localDeviceFolder, String localDeviceIdsFolder) throws IOException {

        //-----------Check folder with computer name------------------------------------------
//        String hostName = getHostName();
        String folderId = "";
//        folderId = createFolderAndGetId(logFolder, hostName);


        BoxFolder compFolder = new BoxFolder(api, folderId);
        folderId = createFolderAndGetId(compFolder, localDeviceFolder);

//        String localDeviceFolder = "SmartAir Nano";
//        String localDeviceIdsFolder = "003B003A3137511938323937";
        List<String> deviceFolder = getListDirNames(localLogFolder);
        for (String aDeviceFolder : deviceFolder) {
            if (aDeviceFolder.equals(localDeviceFolder)) {
                List<String> idsFolder = getListDirNames(localLogFolder + localDeviceFolder + "/");
                for (String anIdsFolder : idsFolder) {
                    if (anIdsFolder.equals(localDeviceIdsFolder)) {
                        List<String> filesToUploaad = getListFileNames(localLogFolder + localDeviceFolder + "/" + localDeviceIdsFolder + "/");
                        List<String> listFoldersToUpload = getListDirNames(localLogFolder + localDeviceFolder + "/" + localDeviceIdsFolder + "/");
                        BoxFolder deviceIdsFolder = new BoxFolder(api, folderId);
                        folderId = createFolderAndGetId(deviceIdsFolder, localDeviceIdsFolder);
                        BoxFolder logsUploadFolder = new BoxFolder(api, folderId);
                        for (String aFilesToUploaad : filesToUploaad) {
                            uploadFile(logsUploadFolder, localLogFolder + localDeviceFolder + "/" +
                                    localDeviceIdsFolder + "/" + aFilesToUploaad, aFilesToUploaad);
                        }
                        System.out.println(111);

                        HashMap<String, String> existingFolders = getFolder(logsUploadFolder, 1);
                        for (String key : existingFolders.keySet()) {
                            listFoldersToUpload.remove(key);
                        }
                        String path = localLogFolder + localDeviceFolder + "/" + localDeviceIdsFolder + "/";
                        for (String anListFoldersToUpload : listFoldersToUpload) {
                            uploadFolder(logsUploadFolder, anListFoldersToUpload, api, path);
                        }
                    }
                }
            }
        }


    }

    private static void uploadFolder(BoxFolder parentFolder, String name, BoxDeveloperEditionAPIConnection api, String path) throws IOException {
        String folderId = createFolderAndGetId(parentFolder, name);
        List<String> filesToUploaad = getListFileNames(path + name + "/");
        BoxFolder logsUploadFolder = new BoxFolder(api, folderId);
        for (String aFilesToUploaad : filesToUploaad) {
            uploadFile(logsUploadFolder, path + name + "/" + aFilesToUploaad, aFilesToUploaad);
        }
    }

    private static String createFolderAndGetId(BoxFolder folder, String name) {
        String folderId = "";
        createFolder(folder, name);
        HashMap<String, String> map = getFolder(folder, 1);
        for (String k : map.keySet()) {
            if (k.equals(name)) {
                folderId = map.get(k);
            }
        }
        return folderId;
    }

    public static BoxDeveloperEditionAPIConnection getApi() {
        BoxAppSettings boxAppSettings = readJSON(FILE);
        if (Objects.isNull(boxAppSettings)) {
            throw new RuntimeException("Can't get boxAppSettings");
        }
        JWTEncryptionPreferences jwtPreferences = new JWTEncryptionPreferences();
        jwtPreferences.setPublicKeyID(boxAppSettings.getPublicKeyID());
        jwtPreferences.setPrivateKeyPassword(boxAppSettings.getPassphrase());
        jwtPreferences.setPrivateKey(boxAppSettings.getPrivateKey());
        jwtPreferences.setEncryptionAlgorithm(EncryptionAlgorithm.RSA_SHA_256);

        BoxConfig boxConfig = new BoxConfig(boxAppSettings.getClientID(), boxAppSettings.getClientSecret(), boxAppSettings.getEnterpriseID(), jwtPreferences);

        BoxDeveloperEditionAPIConnection api = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(boxConfig);
        return api;
    }


    public static boolean checkFolderExist(String path) {
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    public static void saveAppConfig(AppConfig appConfig) {
        File file = new File("config.json");
        try {
            Files.write(file.toPath(), new Gson().toJson(appConfig).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void moveFile(String filename, String filepath) {
        File a = new File(filename);
        a.renameTo(new File(filepath + a.getName()));
        a.delete();
    }

    public static void downloadFile(BoxDeveloperEditionAPIConnection api, String fileId, String path) throws IOException {
        BoxFile file = new BoxFile(api, fileId);
        BoxFile.Info info = file.getInfo();

        if (!Paths.get(path + info.getName()).toFile().exists()) {
            Files.createDirectories(Paths.get(path));
            FileOutputStream stream = new FileOutputStream(path + info.getName());
            file.download(stream);
            stream.close();
        }
    }

    public static void uploadFile(BoxFolder folder, String file, String name) throws IOException {

        FileInputStream stream = new FileInputStream(file);
        try {
            BoxFile.Info newFileInfo = folder.uploadFile(stream, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stream.close();
    }


    public static void createFolder(BoxFolder parentFolder, String folderName) {
        try {
            BoxFolder.Info childFolderInfo = parentFolder.createFolder(folderName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getHostName() {
        String hostname = "Unknown";

        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Hostname can not be resolved");
        }
        return hostname;
    }


    public static BoxAppSettings readJSON(String file) {
        JSONParser parser = new JSONParser();

        try {

//            Object obj = parser.parse(new FileReader(file));
//
//            JSONObject jsonObject = (JSONObject) obj;
//
//            String enterpriseID = (String) jsonObject.get("enterpriseID");
//            JSONObject boxAppSettings = (JSONObject) jsonObject.get("boxAppSettings");
//            String clientID = (String) boxAppSettings.get("clientID");
//            String clientSecret = (String) boxAppSettings.get("clientSecret");
//
//            JSONObject appAuth = (JSONObject) boxAppSettings.get("appAuth");
//            String publicKeyID = (String) appAuth.get("publicKeyID");
//            String privateKey = (String) appAuth.get("privateKey");
//            String passphrase = (String) appAuth.get("passphrase");

            String clientID = "mfk6ua7av9hm8qprupb69pq2ul9vxy5b";
            String clientSecret = "8hCkhfz78kEuojeLsuX5EBU1Qaqf2OeB";
            String publicKeyID = "erutna1d";
            String privateKey = "-----BEGIN ENCRYPTED PRIVATE KEY-----\nMIIFDjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIzfOLQihwnwUCAggA\nMBQGCCqGSIb3DQMHBAhdCAC2sTMhMQSCBMhRJpBlEGdXyd/To8jIest8H+S6yIgd\nch9Ti52bV2vTnq0mGLyHiuVvJuJehJ5W0Sr7aSc5JoIcpxBWQlvOHbBLYDho8iaN\nehpq15Czz4qbWgiVw06mzo4igZj8uraB2LmhWjCKcEWbkLfjrdLLtiWPExZXx3Yr\nquJJ1jLgE0vYOZzjDeOnCZ9Dc5LSEa4FSvd3SVzGwg7yH83UnJ1ddycTqwDmGmu6\nbIupI95qFc8f/pQUnUPxJQVVJ/qE+ieXIcqg5i7uiDvPJnJo5xfqE5arCAFzzY73\nFF88a/28HreLc5mhv/xqlmndN69CQMj7MTmwsBs2+mlGi1lgontp4AYQbuRrPbNw\nOobsoZpuoqN+Nd6yogQEyAuV4PWWAWWBanvoHarnJ8EEs0dK6mVr7VF6KbSoSHnv\nzbUmOn2cxZrtl2QQuU3O65qRgWs0J0z6h7w38SzoKCvmj322xUtahgsSYOV+SoBV\nWoujCtPLiLbIBDxyoScwJ0koa9Beb2rxFnd0rTSMWn03vejQf4wNu/59zv67K2qW\nTnlZaTeqyQtMTFvfEV4HFQzbwzLNAqSV38FgBCAK730eYqukyvbBlmDT3lJaM7xW\nZKvYh39C4REhoonoGqcuglZEIfR8FDD6Vlbeid8AdCR54dwLS9uBUDkOUEv3Miui\nWtyKR/JTA2v5Ss1ufSy11kKW/1EUabmIsHGPvu6Pevtsnxj+0s8reSz/MoOSAmeq\n8ReUp85EZ4BtwwOh90pEPtrCZ4UMEHJnOp6TjowLktbCj7RPQRU8pxAQXKNtHjSB\n9Pi62YQkROeHxaes7aZid3XGCkt9ebqzAcfi+f09DpRjZfxgcowJNWpsvZZ8qkl2\nzcTWG7tvJEwe5GFr/sXmchGQjARJ/F5MpCHn/izM53etDWtNpyepigFMY00+E97V\nhk3E7bpwhiGVbWZDbEEC0DkcnMW1fWU4EQrWMYPGzEj4xcSCc1hvPOZutOFyoFW1\njfBlNmJ9947tV9ZJhZrchc6IrtMsaOLUSEjoMU13DSrAxZHoeJGAXgYxdopkbcKy\n+fKq7+rzizo7UYhWF4eTS39x/QJPm6y6df4MS2J8XIG9SearxOtxCRk3mTC8B7OF\njkt9rSJJ59KSIV1OdZ1iTA4m4VP02y4bB8U1zy6lUVaaU2BThG+BpCrkBZGM44lV\nhltMIxtKtLMJ6UrSOEUBkarb1LuH/NETwO3zZywCyOXUo4hEPCFTQauxqU/C5QO0\nVmaQee79GoTa6pH8b9diOYSk45kM4LvnZjFjoZ/OQc33HmSH/DCha4Bzqo2GGZMZ\nLnSHkh8YNRZde5NsGfkdqiO8qc1rz/2+oY4ZtAlKUpKLHhPHm2UEKaaxq9x7Iab9\nwxG9b7EFfHyXhg/3cspShrnqv2iy6hxoi0KQG6VccYMbDy+NPBDHgrxec3zUmm6J\nIZ3aBON2t74uChGyj2GlKdIo8ObhBaq3DSs+mSOMCrw6YlqRtw8P8DiJX3je3Nef\nYDbPh4zZVdd7vpnqeE4z02bB4N6qfjglo/RDprZ8mNtfZJ4aeuK16UVRo1q62Ixq\nVbFXzNIWGQ+ILhvPYwQOezVXlIeSKM7CvadrZDqGwEskgwt5dI+6h4GIJ6aeishy\n+IY=\n-----END ENCRYPTED PRIVATE KEY-----\n";
            String passphrase = "68e03ddcb31650d5daca901548798fdd";
            String enterpriseID = "717438";


            BoxAppSettings boxSettings = new BoxAppSettings(clientID, clientSecret, publicKeyID, privateKey,
                    passphrase, enterpriseID);
            return boxSettings;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void listFolder(BoxFolder folder, int depth) {
        for (BoxItem.Info itemInfo : folder) {
            String indent = "";
            for (int i = 0; i < depth; i++) {
                indent += "    ";
            }

            System.out.println(indent + itemInfo.getName());
            System.out.println(indent + itemInfo.getID());
            if (itemInfo instanceof BoxFolder.Info) {
                BoxFolder childFolder = (BoxFolder) itemInfo.getResource();
                if (depth < MAX_DEPTH) {
                    listFolder(childFolder, depth + 1);
                }
            }
        }
    }

    public static HashMap<String, String> getFolder(BoxFolder folder, int depth) {
        HashMap<String, String> map = new HashMap<>();
        for (BoxItem.Info itemInfo : folder) {
            String indent = "";
            for (int i = 0; i < depth; i++) {
                indent += "    ";
            }

            map.put(itemInfo.getName(), itemInfo.getID());


        }
        return map;
    }


    public static void showNotification(String title, String msg) throws AWTException {
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("parazero-logo-final.png"));
        if (osType.equals(OSType.MAC)) {
            Application application = Application.getApplication();
            application.setDockIconImage(image);
        }
        if (Objects.isNull(trayIcon)) {
            trayIcon = new TrayIcon(image);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("ParaZero service v1.0.17");
            tray.add(trayIcon);
        }
        trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO);


    }

}

