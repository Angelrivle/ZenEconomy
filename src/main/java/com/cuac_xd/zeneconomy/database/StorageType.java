package com.cuac_xd.zeneconomy.database;

public enum StorageType {
    SQLITE,
    MYSQL,
    MARIADB;

    public static StorageType fromString(String name) {
        try {
            return StorageType.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return SQLITE;
        }
    }
}
