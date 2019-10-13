package org.ka.menkins.mesos;

public class Paths {

    public static String join(String prefix, String suffix) {
        if (prefix.endsWith("/"))   prefix = prefix.substring(0, prefix.length()-1);
        if (suffix.startsWith("/")) suffix = suffix.substring(1, suffix.length());

        return prefix + '/' + suffix;
    }

    private Paths() {}
}
