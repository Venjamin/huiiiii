import lombok.Data;

import java.util.List;

@Data
public class DeviceParams {
    private String HWVersion;
    private String FWVersion;
    private Boolean isCriticalUpdate;
    private List<String> Changelog;
    private List<DroneConfig> DroneTypes;

    public DeviceParams(String HWVersion, String FWVersion) {
        this.HWVersion = HWVersion;
        this.FWVersion = FWVersion;
    }

    @Data
    public static class DroneConfig {
        private String name;
        private Double ConfigVersion;
    }
}
