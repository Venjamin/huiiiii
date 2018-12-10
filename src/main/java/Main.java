import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.box.sdk.*;

import java.util.Timer;
import com.apple.eawt.Application;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;




public final class Main {
    private static final int MAX_DEPTH = 1;
    private static final String FILE = "130221054_h0cwr96v_config.json";



    private Main() { }

    public static void main(String[] args) throws AWTException, IOException {
        Timer timer = new Timer(true);
        timer.schedule(new SayHello(), 0, 50*1000);
        // Turn off logging to prevent polluting the output.
        Logger.getLogger("com.box.sdk").setLevel(Level.OFF);
        BoxAppSettings boxAppSettings = new BoxAppSettings();
        boxAppSettings = readJSON(FILE);


        JWTEncryptionPreferences jwtPreferences = new JWTEncryptionPreferences();
        jwtPreferences.setPublicKeyID(boxAppSettings.getPublicKeyID());
        jwtPreferences.setPrivateKeyPassword(boxAppSettings.getPassphrase());
        jwtPreferences.setPrivateKey(boxAppSettings.getPrivateKey());
        jwtPreferences.setEncryptionAlgorithm(EncryptionAlgorithm.RSA_SHA_256);

        BoxConfig boxConfig = new BoxConfig(boxAppSettings.getClientID(), boxAppSettings.getClientSecret(), boxAppSettings.getEnterpriseID(), jwtPreferences);

        BoxDeveloperEditionAPIConnection api = BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(boxConfig);




        BoxUser.Info userInfo = BoxUser.getCurrentUser(api).getInfo();
        System.out.format("Welcome, %s <%s>!\n\n", userInfo.getName(), userInfo.getLogin());

        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        String folderName = "test_create1";
        BoxFolder test = new BoxFolder(api, "59900368423");
        createFolder(test, folderName);
        listFolder(rootFolder, 0);
        String hostName = getHostName();
        System.out.println(hostName);
        try {
            uploadFile(test, "/Users/i344537/IdeaProjects/BoxTest/parazero-logo-final.png");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        String fileId = "361128535082";
        downloadFile(api, fileId);
        moveFile("logo", "downloads/");
        Boolean isDirectory = checkFolderExist("downloads/");
        System.out.println(isDirectory);

        showNotification("Test", "Test message!");

        }



    private static boolean checkFolderExist(String path) {
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
            return true;
        }
        else {
            return false;
        }
    }

    private static void moveFile(String filename, String filepath) {
        File a = new File(filename);
        a.renameTo(new File(filepath + a.getName()));
        a.delete();
    }

    private static void downloadFile(BoxDeveloperEditionAPIConnection api, String fileId) throws IOException {
        BoxFile file = new BoxFile(api, fileId);
        BoxFile.Info info = file.getInfo();

        FileOutputStream stream = new FileOutputStream(info.getName());
        file.download(stream);
        stream.close();
    }

    private static void uploadFile(BoxFolder folder, String file) throws IOException {

        FileInputStream stream = new FileInputStream(file);
        BoxFile.Info newFileInfo = folder.uploadFile(stream, "logo.png");
        stream.close();
    }


    private static void createFolder(BoxFolder parentFolder, String folderName) {
        try {
            BoxFolder.Info childFolderInfo = parentFolder.createFolder(folderName);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String getHostName() {
        String hostname = "Unknown";

        try
        {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        }
        catch (UnknownHostException ex)
        {
            System.out.println("Hostname can not be resolved");
        }
        return hostname;
    }


    private static BoxAppSettings readJSON(String file) {
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

    private static void listFolder(BoxFolder folder, int depth) {
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

    private static void showNotification(String title, String msg) throws AWTException {
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("parazero-logo-final.png"));
        Application application = Application.getApplication();
        application.setDockIconImage(image);
        TrayIcon trayIcon = new TrayIcon(image);
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);
        trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO);




    }

}

class SayHello extends TimerTask {
    public void run() {
        System.out.println("Hello World!");
    }
}