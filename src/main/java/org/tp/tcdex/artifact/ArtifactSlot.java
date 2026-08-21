package org.tp.tcdex.artifact;

/**
 * 原神圣遗物部位。
 */
public enum ArtifactSlot {
    FLOWER("artifact_flower"),
    PLUME("artifact_plume"),
    SANDS("artifact_sands"),
    GOBLET("artifact_goblet"),
    CIRCLET("artifact_circlet");

    private final String id;

    ArtifactSlot(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
