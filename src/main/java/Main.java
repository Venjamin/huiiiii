import com.apple.eawt.Application;
import com.box.sdk.*;
import com.google.gson.Gson;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;


public final class Main {
    private static final int MAX_DEPTH = 1;
    private static final String FILE = "717438__config.json";
    public static final String NANO_UPD_FOLDER_ID = "60790193719";
    public static final String V2_UPD_FOLDER_ID = "";
    public static final String LOG_FOLDER_ID = "60790087023";
    public static TrayIcon trayIcon;
    public static OSType osType;
    public static AppConfig APP_CONFIG;
    public static String localLogFolder = "devices/";


    private Main() {
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        final String os = System.getProperty("os.name", "nix").toLowerCase();
        // OS-specific
        if (os.startsWith("win")) {
            osType = OSType.WIN;
        } else if (os.startsWith("mac")) {
            osType = OSType.MAC;
        }

        File file = new File("config.json");
        if (!file.exists()) {
            AppConfig defaultConfig = new AppConfig();
            defaultConfig.setIsLicenseApproved(false);
            defaultConfig.setIsFirstStart(true);
            defaultConfig.setSyncTimeMin(60 * 24);
            defaultConfig.setRootFolder(Paths.get("").toAbsolutePath().toString());
            saveAppConfig(defaultConfig);
        }
        String configContent = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
        APP_CONFIG = new Gson().fromJson(configContent, AppConfig.class);

//        Timer timer = new Timer(false);
//        timer.schedule(new PeriodicCheck(), 0, APP_CONFIG.getSyncTimeMin() * 1000);

        // Turn off logging to prevent polluting the output.
        Logger.getLogger("com.box.sdk").setLevel(Level.OFF);
        BoxDeveloperEditionAPIConnection api = getApi();


        BoxUser.Info userInfo = BoxUser.getCurrentUser(api).getInfo();
        System.out.format("Welcome, %s <%s>!\n\n", userInfo.getName(), userInfo.getLogin());

        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
//        BoxFolder logFolder = new BoxFolder(api, LOG_FOLDER_ID);



        listFolder(rootFolder, 0);


//        uploadLogs(logFolder, api);

        while (true) {
            sleep(24 * 60 * 60 * 1000);
        }
    }

    public static ArrayList getListDirNames(String path) {
        ArrayList<String> listDir = new ArrayList<>();
        File direktory = new File(path);
        String[] directories = direktory.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        for (int i = 0; i < directories.length; i++) {
            listDir.add(directories[i]);
        }

        return listDir;
    }

    public static ArrayList getListFileNames(String path) {
        ArrayList<String> listDir = new ArrayList<>();
        File direktory = new File(path);
        String[] directories = direktory.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isFile();
            }
        });
        System.out.println(Arrays.toString(directories));
        for (int i = 0; i < directories.length; i++) {
            listDir.add(directories[i]);
        }

        return listDir;
    }

    public static void uploadLogs(BoxFolder logFolder, BoxDeveloperEditionAPIConnection api, String localDeviceFolder, String localDeviceIdsFolder) throws IOException {

        //-----------Check folder with computer name------------------------------------------
        String hostName = getHostName();
        String folderId = "";
        folderId = createFolderAndGetId(logFolder, hostName);


        BoxFolder compFolder = new BoxFolder(api, folderId);
        folderId = createFolderAndGetId(compFolder, "Smart Air Nano");

//        String localDeviceFolder = "SmartAir Nano";
//        String localDeviceIdsFolder = "003B003A3137511938323937";
        ArrayList<String> deviceFolder = getListDirNames(localLogFolder);
        for (String aDeviceFolder : deviceFolder) {
            if (aDeviceFolder.equals(localDeviceFolder)) {
                ArrayList<String> idsFolder = getListDirNames(localLogFolder + localDeviceFolder + "/");
                for (String anIdsFolder : idsFolder) {
                    if (anIdsFolder.equals(localDeviceIdsFolder)) {
                        ArrayList<String> filesToUploaad = getListFileNames(localLogFolder + localDeviceFolder + "/" + localDeviceIdsFolder + "/");
                        ArrayList<String> listFoldersToUpload = getListDirNames(localLogFolder + localDeviceFolder + "/" + localDeviceIdsFolder + "/");
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
        ArrayList<String> filesToUploaad = getListFileNames(path + name + "/");
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

        FileOutputStream stream = new FileOutputStream(path + info.getName());
        file.download(stream);
        stream.close();
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

            Object obj = parser.parse(new FileReader(file));

            JSONObject jsonObject = (JSONObject) obj;

            String enterpriseID = (String) jsonObject.get("enterpriseID");
            JSONObject boxAppSettings = (JSONObject) jsonObject.get("boxAppSettings");
            String clientID = (String) boxAppSettings.get("clientID");
            String clientSecret = (String) boxAppSettings.get("clientSecret");

            JSONObject appAuth = (JSONObject) boxAppSettings.get("appAuth");
            String publicKeyID = (String) appAuth.get("publicKeyID");
            String privateKey = (String) appAuth.get("privateKey");
            String passphrase = (String) appAuth.get("passphrase");


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
            tray.add(trayIcon);
        }
        trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO);


    }

}

