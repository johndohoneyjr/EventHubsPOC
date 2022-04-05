import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.spring.cloud.context.core.api.Environment;
import com.microsoft.azure.spring.cloud.context.core.storage.StorageConnectionStringProvider;
import com.microsoft.azure.spring.integration.eventhub.converter.EventHubMessageConverter;
import com.microsoft.azure.spring.integration.eventhub.factory.DefaultEventHubClientFactory;
import com.microsoft.azure.spring.integration.eventhub.factory.EventHubConnectionStringProvider;
import com.microsoft.azure.spring.integration.eventhub.impl.EventHubTemplate;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

    public class EventHubsFactory {

	    public static DefaultEventHubClientFactory createClientFactory(
				String storageAccount,
				String storageAccessKey,
				String ehConnString) {
				
	        String storageConnString = 
				StorageConnectionStringProvider
	                .getConnectionString(
							storageAccount,
	                        storageAccessKey,
	                        Environment.GLOBAL);

	        return new DefaultEventHubClientFactory(
	                new EventHubConnectionStringProvider(ehConnString), 
	                storageConnString);
	    }

    }

    public class AvroMessageConverter extends EventHubMessageConverter {

        @Override
        public EventData fromMessage(Message<?> message, Class<EventData> targetClass) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                DatumWriter<Object> datumWriter = 
					new SpecificDatumWriter<>(((GenericContainer) message.getPayload()).getSchema());
                Encoder binaryEncoder = EncoderFactory.get().binaryEncoder(baos, null);
                datumWriter.write(message.getPayload(), binaryEncoder);
                binaryEncoder.flush();
            } catch (IOException e) {}

            return fromByte(baos.toByteArray());
        }

    }

    public class AvroSerializer {
		private String schemaUrl;

        private AvroSerializer(String schemaUrl) {
			this.schemaUrl = schemaUrl;
		}

        public Message<Object> toMessage(
                String schemaName,
                String jsonPayload,
                Map<String, String> messageHeaders)
                        throws Exception {

            SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(schemaUrl, 1000);
            int schemaId = schemaRegistryClient.getLatestSchemaMetadata(schemaName).getId();
            Schema schema = schemaRegistryClient.getById(schemaId);

            Decoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, jsonPayload);
            DatumReader<GenericData.Record> datumReader = new GenericDatumReader<>(schema);
            GenericRecord record = datumReader.read(null, jsonDecoder);

            MessageBuilder<Object> messageBuilder = MessageBuilder.withPayload(record);
            messageBuilder.setHeader(MessageHeaders.CONTENT_TYPE, "application/*+avro");
            messageBuilder.copyHeaders(messageHeaders);
            return messageBuilder.build();
        }

    }

	DefaultEventHubClientFactory ehClientFactory = EventHubsFactory.createClientFactory(args[0], args[1], args[2]);
	EventHubTemplate ehTemplate = new EventHubTemplate(ehClientFactory);
	ehTemplate.setMessageConverter(new AvroMessageConverter());

	String payload = "{\r\n" + 
	        "    \"id\": 1,\r\n" + 
	        "    \"author\": \"John Dohoney\",\r\n" + 
	        "}";

	Message<Object> serializedMessage = 
		new AvroSerializer(args[3]).toMessage("{{my-schema-name}}", payload, null);
	
	ehTemplate
		.sendAsync("{{my-topic-name}}", serializedMessage)
		.get(30, TimeUnit.SECONDS);
	
	ehClientFactory.destroy();

	SampleResult.setResponseData("" + true, "866");
