package com.walrusone.skywarsreloaded.nms;

public enum CompatibleNMSVersion {

    // 1.21+ only
    v1_21_R1(21, "v1_21_R1"),
    v1_21_R2(21, "v1_21_R1"),
    v1_21_R3(21, "v1_21_R1"),
    v1_21_R4(21, "v1_21_R1"),
    ;

    private final int featureVersion;
    private final String nmsImplVersion;

    CompatibleNMSVersion(int featureVersion, String nmsImplVersion) {
        this.featureVersion = featureVersion;
        this.nmsImplVersion = nmsImplVersion;
    }

    public String getNmsImplVersion() {
        return nmsImplVersion;
    }

    static CompatibleNMSVersion getLatestSupported(Integer currentFeatureVersion) {
        // Only support 1.21+
        if (currentFeatureVersion != null && currentFeatureVersion >= 21) {
            for (int i = CompatibleNMSVersion.values().length - 1; i >= 0; i--) {
                CompatibleNMSVersion version = CompatibleNMSVersion.values()[i];
                if (version.getFeatureVersion() <= currentFeatureVersion) {
                    return version;
                }
            }
        }

        // Return the first (and only) version - v1_21_R1
        return CompatibleNMSVersion.values()[0];
    }

    public int getFeatureVersion() {
        return this.featureVersion;
    }
}
