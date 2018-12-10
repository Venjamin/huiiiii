import lombok.Data;

import java.util.List;

@Data
public class DeviceParams {
    private String HWVersion;
    private Double FWVersion;
    private Boolean isCriticalUpdate;
    private List<String> Changelog;
    private List<DroneConfig> DroneTypes;

    public DeviceParams(String HWVersion, double FWVersion) {
        this.HWVersion = HWVersion;
        this.FWVersion = FWVersion;
    }

    @Data
    public static class DroneConfig {
        private String name;
        private Double ConfigVersion;
    }
}
