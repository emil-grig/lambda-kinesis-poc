package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class S3ToKinesisHandler implements RequestHandler<S3Event, String> {

	private final KinesisClient kinesisClient;
	private final String streamName = "my-kinesis-stream";

	public S3ToKinesisHandler() {
		String endpointUrl = System.getenv("AWS_ENDPOINT_URL");
		this.kinesisClient = KinesisClient.builder()
										  .endpointOverride(URI.create(endpointUrl))
										  .build();
	}

	@Override
	public String handleRequest(S3Event event, Context context) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			for (S3Event.S3EventNotificationRecord record : event.getRecords()) {

				System.out.println("file name is:"+record.getS3().getObject().getKey());
//				Map<String, String> data = Map.of("message", "some data from S3");

				String stringToPassToKinesis = objectMapper.writeValueAsString(record.getS3().getObject().getKey());

				PutRecordRequest putRecordRequest = PutRecordRequest.builder()
																	.streamName(streamName)
																	.data(SdkBytes.fromByteBuffer(
																			ByteBuffer.wrap(stringToPassToKinesis.getBytes())))
																	.partitionKey(record.getS3().getObject().getKey())
																	.build();

				PutRecordResponse putRecordResponse = kinesisClient.putRecord(putRecordRequest);
				context.getLogger().log("Successfully put record: " + putRecordResponse);
			}
			return "Success";
		} catch (Exception e) {
			context.getLogger().log("Error putting record: " + e.getMessage());
			return "Error";
		}
	}
}