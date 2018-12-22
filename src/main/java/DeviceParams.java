import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeviceParams {
    private String HWVersion;
    private Double FWVersion;
    private Double ConfigVersion;
    private Boolean isCriticalUpdate;
    private List<String> Changelog;
    private List<DroneConfig> DroneTypes;

    public DeviceParams(String HWVersion, double FWVersion) {
        this.HWVersion = HWVersion;
        this.FWVersion = FWVersion;
        ConfigVersion = 0.0;
        isCriticalUpdate = false;
    }

    @Data
    public static class DroneConfig {
        private String DroneType;
        private String DroneModel;
        private Double ConfigVersion;
        private Map<String, String> Configuration;
    }
}