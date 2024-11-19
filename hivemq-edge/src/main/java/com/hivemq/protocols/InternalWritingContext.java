package com.hivemq.protocols;


import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

// internal view on a writing context.
public interface InternalWritingContext extends WritingContext {

   @NotNull
   FieldMappings getFieldMappings();

}
