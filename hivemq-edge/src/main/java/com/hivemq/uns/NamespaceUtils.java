package com.hivemq.uns;

import com.hivemq.uns.config.NamespaceProfile;

import java.util.Iterator;
import java.util.List;

public class NamespaceUtils {

    public static String getNamespaceProfileType(NamespaceProfile profile){
        return profile.getName().toLowerCase().replaceAll("//s", "");
    }

    public static void replaceNamespaceProfile(List<NamespaceProfile> profiles, NamespaceProfile profile){
        Iterator<NamespaceProfile> profileIterator = profiles.iterator();
        while(profileIterator.hasNext()){
            NamespaceProfile np = profileIterator.next();
            if(getNamespaceProfileType(np).equals(getNamespaceProfileType(profile)) && np.getName().equals(profile.getName())){
                profileIterator.remove();
                break;
            }
        }
        profiles.add(profile);
    }
}
