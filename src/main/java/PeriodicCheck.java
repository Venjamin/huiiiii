import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxFolder;
import com.google.gson.Gson;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

import static com.sun.corba.se.impl.util.Utility.printStackTrace;

@NoArgsConstructor
class PeriodicCheck extends TimerTask {

    public void run() {
        try {
            Path pathConfigLocal = Paths.get("config.json");
            String localConfigContent = Files.readAllLines(pathConfigLocal).stream().collect(Collectors.joining("\n"));
            AppConfig appConfig = new Gson().fromJson(localConfigContent, AppConfig.class);
            Main.APP_CONFIG = appConfig;
            BoxDeveloperEditionAPIConnection api = Main.getApi();
            BoxFolder logFolder = new BoxFolder(api, Main.LOG_FOLDER_ID);

            ArrayList<AbstractMap.SimpleEntry<String, String>> listDown = new ArrayList<>();
            BoxFolder notification = new BoxFolder(api, Main.NOTIFICATION_FOLDER);
            Map<String, String> map = Main.getFolder(notification, 1);
            FileUtils.deleteQuietly(new File("downloads/notifications.json"));
            for (String key : map.keySet()) {
                System.out.println(map.get("notifications.json"));
                Main.downloadFile(api, map.get("notifications.json"), "downloads/");
                listDown.add(new AbstractMap.SimpleEntry<>(key, map.get(key)));
            }




            Optional.ofNullable(appConfig.getDrones()).ifPresent(drones -> drones.forEach(drone -> {
                String droneType = drone.getDroneType();
                String droneModel = drone.getDroneModel();

                String droneName = Objects.isNull(droneModel) ? droneType : droneType + "-" + droneModel;
                if (Objects.isNull(droneName) || droneName.isEmpty()) {
                    droneName = drone.getUID().replace(" ", "");
                }
                try {
                    doUpdateFlowByType(api, droneName, drone.getSWVersion() + "", drone.getDeviceType());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
//                    Main.uploadLogs(logFolder, api, drone.getProduct(), drone.getUID().replace(" ", ""));
                    Main.uploadLogsTemp(api);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }));

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Flow finished!");
    }

    private void doUpdateFlowByType(BoxDeveloperEditionAPIConnection api, String droneName, String currentVersion, DeviceType deviceType) {
        try {
            ArrayList<AbstractMap.SimpleEntry<String, String>> listDown = new ArrayList<>();
            FileUtils.deleteDirectory(new File("temp/"));

            BoxFolder deviceUpdate = null;
            switch (deviceType) {
                case NANO: {
                    deviceUpdate = new BoxFolder(api, Main.NANO_UPD_FOLDER_ID);
                    break;
                }
                case V2: {
                    deviceUpdate = new BoxFolder(api, Main.V2_UPD_FOLDER_ID);
                    break;
                }
            }
            Main.listFolder(deviceUpdate, 1);
            Map<String, String> map = Main.getFolder(deviceUpdate, 1);

            String folderPrefix = "temp" + File.separator;
            String folderPref1 = ""; // uid
            for (String key : map.keySet()) {
//                if (!key.equals("config.json")) {
//                    System.out.println(key + map.get(key));
//                    BoxFolder updFiles = new BoxFolder(api, map.get(key));
//                    Map<String, String> map1 = Main.getFolder(updFiles, 1);
//                    for (String k : map1.keySet()) {
//                        folderPref1 = "";
//                        listDown.add(new AbstractMap.SimpleEntry<>(key, "f" + map1.get(k)));
//                        folderPref1 += key;
//                    }
//                } else {
                    listDown.add(new AbstractMap.SimpleEntry<>(key, map.get(key)));
//                }
            }
            Files.createDirectories(getUpdatePath(deviceType, folderPrefix, folderPref1));

            Path temp_config = Paths.get("temp" + File.separator + deviceType + File.separator + "config.json");
            Path config = Paths.get("downloads" + File.separator + deviceType + File.separator + "config.json");
            String fileID;
            for (AbstractMap.SimpleEntry<String, String> aListDown : listDown) {
                Path currentFilePath;
//                if (aListDown.getValue().substring(0, 1).equals("f")) {
//                    folderPrefix = "downloads" + File.separator + deviceType + File.separator;
//                    fileID = aListDown.getValue().substring(1);
//                    folderPrefix += folderPref1 + File.separator;
//                    currentFilePath = Paths.get(folderPrefix);
//
//                } else {
                    fileID = aListDown.getValue();
                    folderPrefix = "temp" + File.separator + deviceType + File.separator;
                    currentFilePath = Paths.get(folderPrefix + aListDown.getKey());
//                }
//                if (!currentFilePath.toFile().exists()) {
                Main.downloadFile(api, fileID, folderPrefix);
//                }
            }
            String temp_configContent = Files.readAllLines(temp_config).stream().collect(Collectors.joining("\n"));
            String configContent = "";


            if (new File(String.valueOf(config)).exists()){
                configContent = Files.readAllLines(config).stream().collect(Collectors.joining("\n"));

            }


            if (!temp_configContent.equals(configContent) || configContent.equals("")){
//                System.out.println(111111);
                FileUtils.deleteDirectory(new File("downloads/"));
                String folderPrefixD = "downloads" + File.separator;
                Files.createDirectories(getUpdatePath(deviceType, folderPrefixD, folderPref1));
                System.out.println(folderPrefix);
                FileUtils.copyDirectory(new File(folderPrefix),
                        new File(folderPrefixD + File.separator + deviceType + File.separator));
                DeviceParams UPDATE_CONFIG = new Gson().fromJson(temp_configContent, DeviceParams.class);
                System.out.println(UPDATE_CONFIG.getFWVersion());
                System.out.println(UPDATE_CONFIG.getIsCriticalUpdate());

                // for now if FW version is double

                double fwVersion = UPDATE_CONFIG.getFWVersion();
                double currVerDouble = Double.parseDouble(currentVersion);
                if (currVerDouble < fwVersion) {
                    Main.showNotification("Update required!", "Available new FW version " + UPDATE_CONFIG.getFWVersion() +
                             "\nDrone: " + droneName);
                }


            }

        } catch (AWTException | IOException e) {
            e.printStackTrace();
        }
    }

    private Path getUpdatePath(DeviceType deviceType, String folderName, String version) {
        return Paths.get(folderName + File.separator + deviceType + File.separator + version);
    }
}
