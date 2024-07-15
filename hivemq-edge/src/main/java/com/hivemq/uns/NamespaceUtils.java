package com.hivemq.uns;

import com.hivemq.api.model.uns.NamespaceProfileBean;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;

import java.util.Iterator;
import java.util.List;

public class NamespaceUtils {

    public static void replaceNamespaceProfile(List<NamespaceProfile> profiles, NamespaceProfile profile, boolean markEnabled){
        Iterator<NamespaceProfile> profileIterator = profiles.iterator();
        if(markEnabled){
            profile.setEnabled(true);
        }
        while(profileIterator.hasNext()){
            NamespaceProfile np = profileIterator.next();
            if(np.getName().equals(profile.getName())){
                profileIterator.remove();
                if(!markEnabled){
                    break;
                }
            } else {
                if(markEnabled){
                    np.setEnabled(false);
                }
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
