import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFolder;
import com.google.gson.Gson;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.stream.Collectors;

class PereodikCheck extends TimerTask {
    public void run() {
        String version = "1.18";
        ArrayList<String> listDown = new ArrayList<String>();
        BoxDeveloperEditionAPIConnection api = Main.getApi();
        BoxFolder nanoUpdate = new BoxFolder(api, Main.UPD_FOLDER_ID);
        Main.listFolder(nanoUpdate, 1);
        HashMap<String, String> map = Main.getFolder(nanoUpdate, 1);
        String folderPref = "downloads/";
        String folderPref1 = "";
        for ( String key : map.keySet() ) {
            if (!key.equals("config.json")){
                System.out.println(key + map.get(key));
                BoxFolder updFiles = new BoxFolder(api, map.get(key));
                HashMap<String, String> map1 = Main.getFolder(updFiles, 1);
                for (String k : map1.keySet()){
                    folderPref1 = "";
                    listDown.add("f" + map1.get(k));
                    folderPref1 += key;


                }
            }
            else {
                listDown.add(map.get(key));
            }
        }
        new File(folderPref + folderPref1).mkdir();
        String fileID = "";


        File config = new File("downloads/config.json");

        try {
            for (String aListDown : listDown) {
                if (aListDown.substring(0,1).equals("f")){
                    folderPref = "downloads/";
                    fileID = aListDown.substring(1);
                    folderPref += folderPref1 + "/";

                }
                else {
                    fileID = aListDown;
                    folderPref = "downloads/";
                }

                Main.downloadFile(api, fileID, folderPref);
            }
            String configContent = Files.readAllLines(config.toPath()).stream().collect(Collectors.joining("\n"));
            DeviceParams UPDATE_CONFIG = new Gson().fromJson(configContent, DeviceParams.class);
            System.out.println(UPDATE_CONFIG.getFWVersion());
            System.out.println(UPDATE_CONFIG.getIsCriticalUpdate());
            if (!UPDATE_CONFIG.getFWVersion().equals(version)){
                Main.showNotification("Update required!", "Available new version " + UPDATE_CONFIG.getFWVersion());
            }
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }



        System.out.println("Hello World!");
    }
}
