package com.hivemq.uns.config;

import com.hivemq.uns.config.impl.NamespaceProfileImpl;

import java.util.List;

public interface NamespaceProfile {

    NamespaceProfile PROFILE_ISA_95 = new NamespaceProfileImpl("ISA 95",
            "Some ISA 95 description",
            List.of(
                    NamespaceSegment.of("Enterprise", "Your Enterprise"),
                    NamespaceSegment.of("Site", "Your Site"),
                    NamespaceSegment.of("Area", "Your Area"),
                    NamespaceSegment.of("Production-Line", "Your Production Line"),
                    NamespaceSegment.of("Work-Cell", "Your Work Cell")));

    String getName();
    String getDescription();
    List<NamespaceSegment> getSegments();

}
