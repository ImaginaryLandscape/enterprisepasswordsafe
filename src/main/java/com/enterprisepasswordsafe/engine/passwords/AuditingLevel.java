package com.enterprisepasswordsafe.engine.passwords;

public enum AuditingLevel {
    NONE("N", "none"),
    LOG_ONLY("L", "log"),
    EMAIL_ONLY("E", "email"),
    FULL("F", "Y", "full"),
    CREATOR_CHOOSE("C");

    private String[] representations;

    AuditingLevel(String... representations) {
        this.representations = representations;
    }

    public boolean shouldTriggerEmail() {
        return this == AuditingLevel.EMAIL_ONLY || this == AuditingLevel.FULL;
    }

    public boolean shouldTriggerLogging() {
        return this == AuditingLevel.LOG_ONLY || this == AuditingLevel.FULL;
    }

    @Override
    public String toString() {
        return representations[0];
    }


    private boolean represents(String representation) {
        for(String knownRepresentation : representations) {
            if (knownRepresentation.equals(representation)) {
                return true;
            }
        }
        return false;
    }

    public static AuditingLevel fromRepresentation(String representation) {
        if (representation == null) {
            return null;
        }

        for(AuditingLevel thisLevel : AuditingLevel.values()) {
            if(thisLevel.represents(representation)) {
                return thisLevel;
            }
        }
        return null;
    }
}