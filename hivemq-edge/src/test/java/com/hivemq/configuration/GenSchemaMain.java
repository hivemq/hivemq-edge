package com.hivemq.configuration;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.SchemaOutputResolver;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class GenSchemaMain {
    public static void main(String[] args) throws Exception{
        JAXBContext context = JAXBContext.newInstance(HiveMQConfigEntity.class);

        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                File file = new File("/tmp/schema.xsd");
                StreamResult result = new StreamResult(file);
                result.setSystemId(file.toURI().toString());
                return result;
            }
        });
    }
}
