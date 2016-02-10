package org.venice.piazza.servicecontroller.messaging.handlers;
// TODO add license


import java.util.ArrayList;
import java.util.List;

import model.job.PiazzaJobType;
import model.job.metadata.ResourceMetadata;
import model.job.type.RegisterServiceJob;
import model.job.type.UpdateServiceJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.venice.piazza.servicecontroller.data.mongodb.accessors.MongoAccessor;
import org.venice.piazza.servicecontroller.util.CoreLogger;
import org.venice.piazza.servicecontroller.util.CoreServiceProperties;
import org.venice.piazza.servicecontroller.util.CoreUUIDGen;


/**
 * Handler for handling registerService requests.  This handler is used 
 * when register-service kafka topics are received or when clients utilize the 
 * ServiceController registerService web service.
 * @author mlynum
 * @version 1.0
 *
 */

public class UpdateServiceHandler implements PiazzaJobHandler {
	private MongoAccessor accessor;
	private CoreLogger coreLogger;
	private CoreUUIDGen coreUuidGen;
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceHandler.class);


	public UpdateServiceHandler(MongoAccessor accessor, CoreServiceProperties coreServiceProp, CoreLogger coreLogger, CoreUUIDGen coreUuidGen){ 
		this.accessor = accessor;
		this.coreLogger = coreLogger;
		this.coreUuidGen = coreUuidGen;
	
	}

    /*
     * Handler for the RegisterServiceJob  that was submitted.  Stores the metadata in
     * MongoDB
     * (non-Javadoc)
     * @see org.venice.piazza.servicecontroller.messaging.handlers.Handler#handle(model.job.PiazzaJobType)
     */
	public ResponseEntity<List<String>> handle (PiazzaJobType jobRequest ) {
		
		LOGGER.debug("Updating a service");
		UpdateServiceJob job = (UpdateServiceJob)jobRequest;
		if (job != null)  {
			// Get the ResourceMetadata
			model.job.metadata.ResourceMetadata rMetadata = job.data;

			String result = handle(rMetadata);
			if (result.length() > 0) {
				String jobId = job.getJobId();
				// TODO Use the result, send a message with the resource ID
				// and jobId
				ArrayList<String> resultList = new ArrayList<String>();
				resultList.add(jobId);
				resultList.add(rMetadata.id);
				ResponseEntity<List<String>> handleResult = new ResponseEntity<List<String>>(resultList,HttpStatus.OK);
				return handleResult;
				
			}
			else {
				LOGGER.error("No result response from the handler, something went wrong");
				ArrayList<String> errorList = new ArrayList<String>();
				errorList.add("UpdateServiceHandler handle didn't work");
				ResponseEntity<List<String>> errorResult = new ResponseEntity<List<String>>(errorList,HttpStatus.METHOD_FAILURE);
				
				return errorResult;
			}
		}
		else {
			return null;
		}
	}//handle
	
	/**
	 * 
	 * @param rMetadata
	 * @return resourceID of the registered service
	 */
	public String handle (ResourceMetadata rMetadata) {

        coreLogger.log("about to update a registered service.", CoreLogger.INFO);

		
		String result = accessor.update(rMetadata);
		LOGGER.debug("The result of the update is " + result);
		if (result.length() > 0) {
		   coreLogger.log("The service " + rMetadata.name + " was updated with id " + result, CoreLogger.INFO);
		} else {
			   coreLogger.log("The service " + rMetadata.name + " was NOT updated", CoreLogger.INFO);
		}
		// If an ID was returned then send a kafka message back updating the job iD 
		// with the resourceID
		return result;
				
	}
	


}
