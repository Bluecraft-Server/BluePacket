package top.bluecraft.combatdepot.config;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import top.bluecraft.combatdepot.CombatDepot;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class ConfigManager {
    private static final Logger CL = LogUtils.getLogger();

    public static List<String> getConfigFieldNames() {
        Field[] fields = Config.class.getDeclaredFields();
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    private static @NotNull Properties getDefaultProperties() {
        Properties properties = new Properties();
        List<String> fieldNames = getConfigFieldNames();

        for (String fieldName : fieldNames) {
            try {
                Field field = Config.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                properties.setProperty(fieldName, field.get(null).toString());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                CL.warn("Failed to get default config value for {}: {}", fieldName, e.getMessage());
            }
        }

        return properties;
    }

    private static @NotNull Properties getProperties() {
        Properties properties = new Properties();
        List<String> fieldNames = getConfigFieldNames();
        for (String fieldName : fieldNames) {
            try {
                Field field = Config.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                properties.setProperty(fieldName, field.get(null).toString());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                CL.warn("Failed to get config value for {}: {}", fieldName, e.getMessage());
            }
        }

        return properties;
    }

    public void load() {
        if (!CombatDepot.CONFIGDIR.mkdir()) {
            CL.warn("Failed to create directory: {}", CombatDepot.CONFIGDIR.getAbsolutePath());
        }
        if (!CombatDepot.CONFIG_FILE.exists()) {
            try {
                if (!CombatDepot.CONFIG_FILE.createNewFile()) {
                    CL.warn("Failed to create data file");
                }
            } catch (Exception e) {
                CL.warn("Failed to create data file {}", e.getMessage());
            }
        }
        try (FileReader reader = new FileReader(CombatDepot.CONFIG_FILE)) {
            Properties properties = new Properties();
            properties.load(reader);
            CL.info("Loaded config file: {}", CombatDepot.CONFIG_FILE.getAbsolutePath());

            Properties defaultProperties = getDefaultProperties();
            List<String> fieldNames = getConfigFieldNames();
            boolean label = false;

            for (String fieldName : fieldNames) {
                try {
                    Field field = Config.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    String value = properties.getProperty(fieldName);
                    if (value == null) {
                        value = defaultProperties.getProperty(fieldName);
                        label = true;
                    }
                    Object parsedValue = parseValue(field.getType(), value);
                    field.set(null, convertToFieldType(field.getType(), parsedValue));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    CL.warn("Failed to set config value for {}: {}", fieldName, e.getMessage());
                }
            }
            if (label) {
                save();
            }
        } catch (IOException e) {
            CL.warn("Failed to load config file {}: {}", CombatDepot.CONFIG_FILE.getAbsolutePath(), e.getMessage());
            CL.info("Using default config values.");
            save();
        }
    }

    private Object convertToFieldType(Class<?> fieldType, Object value) {
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(value.toString());
        } else {
            return value;
        }
    }

    public void setConfigValue(String fieldName, String value) {
        try {
            Field field = Config.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object parsedValue = parseValue(field.getType(), value);
            field.set(null, parsedValue);
            save();
        } catch (Exception e) {
            CL.warn("Failed to parse config value for {}: {}", fieldName, e.getMessage());
        }
    }

    private Object parseValue(Class<?> type, String value) {
        if (type == Boolean.class) return Boolean.parseBoolean(value);
        else if (type == Integer.class) return Integer.parseInt(value);
        else if (type == Long.class) return Long.parseLong(value);
        else return value;
    }

    public void save() {
        if (!CombatDepot.CONFIG_FILE.exists()) {
            try {
                if (!CombatDepot.CONFIG_FILE.createNewFile()) {
                    CL.warn("Failed to create config file");
                }
            } catch (IOException e) {
                CL.warn("Failed to create config file {}", e.getMessage());
            }
        }

        try (FileWriter writer = new FileWriter(CombatDepot.CONFIG_FILE)) {
            Properties properties = getProperties();
            properties.store(writer, null);
            CL.info("Saved config file: {}", CombatDepot.CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
            CL.warn("Failed to save config {}", e.getMessage());
        }
    }
}
