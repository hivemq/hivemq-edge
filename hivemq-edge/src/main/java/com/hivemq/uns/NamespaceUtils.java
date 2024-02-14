package com.hivemq.uns;

import com.hivemq.uns.config.NamespaceProfile;

public class NamespaceUtils {

    public static String getNamespaceProfileType(NamespaceProfile profile){
        return profile.getName().toLowerCase().replaceAll("//s", "");
    }
}
