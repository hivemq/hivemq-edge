package com.hivemq.uns.config;

import com.hivemq.uns.config.impl.NamespaceProfileImpl;

import java.util.List;

public interface NamespaceProfile {

    NamespaceProfile PROFILE_ISA_95 = new NamespaceProfileImpl("ISA 95",
            "Some ISA 95 description",
            List.of(NamespaceSegment.of(ISA95.ENTERPRISE, "Your Enterprise"),
                    NamespaceSegment.of(ISA95.SITE, "Your Site"),
                    NamespaceSegment.of(ISA95.AREA, "Your Area"),
                    NamespaceSegment.of(ISA95.PRODUCTION_LINE, "Your Production Line"),
                    NamespaceSegment.of(ISA95.WORK_CELL, "Your Work Cell")));

    String getName();
    boolean getEnabled();
    String getDescription();
    List<NamespaceSegment> getSegments();
    boolean getPrefixAllTopics();
    void setEnabled(boolean enabled);
    void setPrefixAllTopics(boolean setPrefixAllTopics);

}
