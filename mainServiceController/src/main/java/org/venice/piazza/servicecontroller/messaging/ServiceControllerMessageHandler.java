package org.venice.piazza.servicecontroller.messaging;
// TODO Add license

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.venice.piazza.servicecontroller.data.mongodb.accessors.MongoAccessor;
import org.venice.piazza.servicecontroller.messaging.handlers.ExecuteServiceHandler;
import org.venice.piazza.servicecontroller.messaging.handlers.RegisterServiceHandler;
import org.venice.piazza.servicecontroller.util.CoreLogger;
import org.venice.piazza.servicecontroller.util.CoreServiceProperties;

import com.fasterxml.jackson.databind.ObjectMapper;

import messaging.job.KafkaClientFactory;
import model.job.Job;
import model.job.PiazzaJobType;
import model.job.type.ExecuteServiceJob;
import model.job.type.RegisterServiceJob;
import model.request.PiazzaJobRequest;
/**
 * Purpose of this controller is to register for the Kafka messages and listen for service controller topics.
 * @author mlynum
 * @version 1.0
 */


@Controller
@DependsOn("coreInitDestroy")
public class ServiceControllerMessageHandler implements Runnable {
	// Jobs to listen to
	private static final String DELETE_SERVICE_JOB_TOPIC_NAME = "Delete-Service-Job";
	private static final String EXECUTE_SERVICE_JOB_TOPIC_NAME = "Execute-Service-Job";
	private static final String READ_SERVICE_JOB_TOPIC_NAME = "Read-Service-Job";
	private static final String REGISTER_SERVICE_JOB_TOPIC_NAME = "Register-Service-Job";
	private static final String UPDATE_SERVICE_JOB_TOPIC_NAME = "Update-Service-Job";
	
	
	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceControllerMessageHandler.class);
	
	private String KAFKA_HOST;
	private int KAFKA_PORT;
	private String KAFKA_GROUP;
	/*
	  TODO need to determine how statuses will be sent to update the job  (Call back?)
	 */
	private Producer<String, String> producer;
	private Consumer<String, String> consumer;
	private List<String> topics;
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private RegisterServiceHandler rsHandler;
	private ExecuteServiceHandler esHandler;

	@Autowired
	private MongoAccessor accessor;
	@Autowired
	private CoreServiceProperties coreServiceProperties;
	
	@Autowired
	private CoreLogger coreLogger;

	/**
	 * Constructor
	 */
	public ServiceControllerMessageHandler() {
		topics = Arrays.asList(DELETE_SERVICE_JOB_TOPIC_NAME, EXECUTE_SERVICE_JOB_TOPIC_NAME, 
							   READ_SERVICE_JOB_TOPIC_NAME, REGISTER_SERVICE_JOB_TOPIC_NAME,
							   UPDATE_SERVICE_JOB_TOPIC_NAME);
	    // Initialize the handlers to handle requests from the message queue
		rsHandler = new RegisterServiceHandler(accessor, coreServiceProperties, coreLogger);
		esHandler = new ExecuteServiceHandler(accessor, coreServiceProperties, coreLogger);
	}

	/**+
	 * 
	 */
	@PostConstruct
	public void initialize() {
		// Initialize the Kafka consumer/producer
		KAFKA_PORT = coreServiceProperties.getKafkaPort();
		KAFKA_HOST = coreServiceProperties.getKafkaHost();
		KAFKA_GROUP = coreServiceProperties.getKafkaGroup();
		LOGGER.info("=================================");
		LOGGER.info("The KAFKA Port Properties is " + coreServiceProperties.getKafkaPort());
		LOGGER.info("The KAFKA Host Properties is " + coreServiceProperties.getKafkaHost());
		LOGGER.info("The KAFKA Group Properties is " + coreServiceProperties.getKafkaGroup());
		LOGGER.info("=================================");

		String KAFKA_PORT_STRING = new Integer(KAFKA_PORT).toString();
		producer = KafkaClientFactory.getProducer(KAFKA_HOST, KAFKA_PORT_STRING);
		consumer = KafkaClientFactory.getConsumer(KAFKA_HOST, KAFKA_PORT_STRING, KAFKA_GROUP);
		// Start the runner that will relay Job Creation topics.
		Thread kafkaListenerThread = new Thread(this);
		
		// Subscribe for the topics
		consumer.subscribe(topics);
		kafkaListenerThread.start();
	}

	@Override
	public void run() {
		try {
			
			while (!closed.get()) {
				ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
				// Handle new Messages on this topic.
				for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
					LOGGER.info("Relaying Message " + consumerRecord.topic() + " with key "
							+ consumerRecord.key());
					// Wrap the JobRequest in the Job object
					try {
						ObjectMapper mapper = new ObjectMapper();
						PiazzaJobRequest jobRequest = mapper.readValue(consumerRecord.value(), PiazzaJobRequest.class);
												
						Job job = org.venice.piazza.servicecontroller.data.model.JobFactory.fromJobRequest(jobRequest, consumerRecord.key());
						
						PiazzaJobType jobType = job.jobType;
						// See what type of job was sent
						if (jobType instanceof RegisterServiceJob) {
						   // Handle Register Job
						   rsHandler.handle(jobType);
							
						} else if (jobType instanceof ExecuteServiceJob) {
							esHandler.handle(jobType);
						}
						
					} catch (IOException ex) {
						LOGGER.error(ex.getMessage());
					}
				}

			}
			
			
		} catch (WakeupException ex) {
			LOGGER.error(ex.getMessage());
		}
		
		
	}
    

	
}


