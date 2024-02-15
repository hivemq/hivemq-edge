package com.hivemq.uns;

import com.hivemq.api.model.uns.NamespaceProfileBean;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;

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
            if(getNamespaceProfileType(np).equals(getNamespaceProfileType(profile)) &&
                    np.getName().equals(profile.getName())){
                profileIterator.remove();
                break;
            }
        }
        profiles.add(profile);
    }

    public static void setValueAtSegment(NamespaceProfile profile, String name, String value){
        for(NamespaceSegment s : profile.getSegments()){
            if(s.getName().equals(name)){
                s.setValue(value);
                break;
            }
        }
    }
}
