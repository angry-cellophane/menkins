package org.ka.menkins.app.init;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

@AllArgsConstructor
class PropertiesHolder {

    @Value
    static class PropertyNames {
        @NonNull String envName;
        @NonNull String propertyName;
    }

    public static PropertyNames name(String envVarName, String propertyName) {
        return new PropertyNames(envVarName, propertyName);
    }

    public static class PropertyNotFoundException extends RuntimeException {
        public PropertyNotFoundException(PropertyNames names) {
            super("Env var " + names.getEnvName() + " nor " + names.getPropertyName() + " not set. Cannot initialize the app");
        }
    }


    Map<String, String> env;
    Properties properties;

    String getValue(PropertyNames names) {
        var value = env.get(names.getEnvName());
        if (value != null) return value;

        value = properties.getProperty(names.getPropertyName());
        if (value != null) return value;

        throw new PropertyNotFoundException(names);
    }

    String getValue(PropertyNames names, Supplier<String> defaultValue) {
        var value = env.get(names.getEnvName());
        if (value != null) return value;

        value = properties.getProperty(names.getPropertyName());
        if (value != null) return value;

        return defaultValue.get();
    }

    PropertiesHolder merge(PropertiesHolder that) {
        var env = new HashMap<>(this.env);
        env.putAll(that.env);

        var props = new Properties(this.properties);
        props.putAll(that.properties);

        return new PropertiesHolder(env, props);
    }
}
