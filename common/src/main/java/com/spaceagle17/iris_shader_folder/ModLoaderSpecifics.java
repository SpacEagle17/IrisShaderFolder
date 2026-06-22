package com.spaceagle17.iris_shader_folder;

import java.nio.file.Path;

public abstract class ModLoaderSpecifics {

    public static final String FABRIC   = "Fabric";
    public static final String FORGE    = "Forge";
    public static final String NEOFORGE = "NeoForge";

    private static ModLoaderSpecifics instance;

    public static void setInstance(ModLoaderSpecifics impl) {
        instance = impl;
    }

    public static ModLoaderSpecifics getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "[IrisShaderFolder] ModLoaderSpecifics.getInstance() called before setInstance(). " +
                "This indicates a serious initialization error.");
        }
        return instance;
    }

    public abstract String getInstanceName();

    public abstract Path getConfigDirectory();

    public abstract boolean serverCheck();

    public static String getInstanceNameStatic() {
        return getInstance().getInstanceName();
    }

    public static boolean isInstance(String name) {
        return getInstance().getInstanceName().equals(name);
    }

    public static Path configDirectory() {
        return getInstance().getConfigDirectory();
    }

    public static boolean serverCheckStatic() {
        return getInstance().serverCheck();
    }
}
