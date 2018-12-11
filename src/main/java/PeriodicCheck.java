import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFolder;
import com.google.gson.Gson;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
class PeriodicCheck extends TimerTask {
    private String uid;
    private String currentVersion;

    public PeriodicCheck(String uid, String currentVersion) {
        this.uid = uid;
        this.currentVersion = currentVersion;
    }

    public void run() {
        try {
        File file = new File("config.json");
        String localConfigContent = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
            AppConfig appConfig = new Gson().fromJson(localConfigContent, AppConfig.class);
            Main.APP_CONFIG = appConfig;
            currentVersion = "1.18";

        ArrayList<String> listDown = new ArrayList<>();
        BoxDeveloperEditionAPIConnection api = Main.getApi();
        BoxFolder nanoUpdate = new BoxFolder(api, Main.UPD_FOLDER_ID);
        Main.listFolder(nanoUpdate, 1);
        Map<String, String> map = Main.getFolder(nanoUpdate, 1);
        File downloadsDir = new File("downloads");
        if (!downloadsDir.exists()) {
            downloadsDir.mkdir();
        }

        String folderPref = "downloads" + File.separator;
        String folderPref1 = uid;
        for (String key : map.keySet()) {
            if (!key.equals("config.json")) {
                System.out.println(key + map.get(key));
                BoxFolder updFiles = new BoxFolder(api, map.get(key));
                Map<String, String> map1 = Main.getFolder(updFiles, 1);
                for (String k : map1.keySet()) {
                    folderPref1 = "";
                    listDown.add("f" + map1.get(k));
                    folderPref1 += key;
                }
            } else {
                listDown.add(map.get(key));
            }
        }
        new File(folderPref + folderPref1).mkdir();
        String fileID = "";


        File config = new File("downloads" + File.separator + "config.json");


            for (String aListDown : listDown) {
                if (aListDown.substring(0, 1).equals("f")) {
                    folderPref = "downloads" + File.separator;
                    fileID = aListDown.substring(1);
                    folderPref += folderPref1 + File.separator;

                } else {
                    fileID = aListDown;
                    folderPref = "downloads" + File.separator;
                }

                Main.downloadFile(api, fileID, folderPref);
            }
            String configContent = Files.readAllLines(config.toPath()).stream().collect(Collectors.joining("\n"));
            DeviceParams UPDATE_CONFIG = new Gson().fromJson(configContent, DeviceParams.class);
            System.out.println(UPDATE_CONFIG.getFWVersion());
            System.out.println(UPDATE_CONFIG.getIsCriticalUpdate());
            if (!UPDATE_CONFIG.getFWVersion().equals(currentVersion)) {
                Main.showNotification("Update required!", "Available new version " + UPDATE_CONFIG.getFWVersion());
            }
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }


        System.out.println("Hello World!");
    }
}
