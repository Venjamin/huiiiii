import lombok.*;

import java.util.List;

@Data
public class AppConfig {
    private String rootFolder;
    private Boolean isLicenseApproved;
    private Boolean isFirstStart;
    private Integer syncTimeMin;
    private List<Drone> drones;

    @Data
    @AllArgsConstructor
    public static class Drone {
        private String UID;
        private String Product;
        private Double SWVersion;
        private String DroneType;
        private String DroneModel;
        private String DroneName;

        public DeviceType getDeviceType() {
            if (Product.toLowerCase().contains("nano")) {
                return DeviceType.NANO;
            } else {
                return DeviceType.V2;
            }
        }
    }
}
