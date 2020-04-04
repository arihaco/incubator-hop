/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * http://www.project-hop.org
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.www.jaxrs;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.HopLogStore;
import org.apache.hop.core.logging.LoggingObjectType;
import org.apache.hop.core.logging.SimpleLoggingObject;
import org.apache.hop.core.util.Utils;
import org.apache.hop.job.Job;
import org.apache.hop.job.JobConfiguration;
import org.apache.hop.job.JobExecutionConfiguration;
import org.apache.hop.job.JobMeta;
import org.apache.hop.www.HopServerObjectEntry;
import org.apache.hop.www.HopServerSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path( "/carte/job" )
public class JobResource {

  public JobResource() {
  }

  @GET
  @Path( "/log/{id : .+}" )
  @Produces( { MediaType.TEXT_PLAIN } )
  public String getJobLog( @PathParam( "id" ) String id ) {
    return getJobLog( id, 0 );
  }

  @GET
  @Path( "/log/{id : .+}/{logStart : .+}" )
  @Produces( { MediaType.TEXT_PLAIN } )
  public String getJobLog( @PathParam( "id" ) String id, @PathParam( "logStart" ) int startLineNr ) {
    int lastLineNr = HopLogStore.getLastBufferLineNr();
    Job job = HopServerResource.getJob( id );
    String logText =
      HopLogStore.getAppender().getBuffer(
        job.getLogChannel().getLogChannelId(), false, startLineNr, lastLineNr ).toString();
    return logText;
  }

  @GET
  @Path( "/status/{id : .+}" )
  @Produces( { MediaType.APPLICATION_JSON } )
  public JobStatus getJobStatus( @PathParam( "id" ) String id ) {
    JobStatus status = new JobStatus();
    // find job
    Job job = HopServerResource.getJob( id );
    HopServerObjectEntry entry = HopServerResource.getCarteObjectEntry( id );

    status.setId( entry.getId() );
    status.setName( entry.getName() );
    status.setStatus( job.getStatus() );

    return status;
  }

  // change from GET to UPDATE/POST for proper REST method
  @GET
  @Path( "/start/{id : .+}" )
  @Produces( { MediaType.APPLICATION_JSON } )
  public JobStatus startJob( @PathParam( "id" ) String id ) {
    Job job = HopServerResource.getJob( id );
    HopServerObjectEntry entry = HopServerResource.getCarteObjectEntry( id );
    if ( job.isInitialized() && !job.isActive() ) {
      // Re-create the job from the jobMeta
      //

      // Create a new job object to start from a sane state. Then replace
      // the new job in the job map
      //
      synchronized ( this ) {
        JobConfiguration jobConfiguration = HopServerSingleton.getInstance().getJobMap().getConfiguration( entry );

        String carteObjectId = UUID.randomUUID().toString();
        SimpleLoggingObject servletLoggingObject =
          new SimpleLoggingObject( getClass().getName(), LoggingObjectType.CARTE, null );
        servletLoggingObject.setContainerObjectId( carteObjectId );

        Job newJob = new Job( job.getJobMeta(), servletLoggingObject );
        newJob.setLogLevel( job.getLogLevel() );

        // Discard old log lines from the old job
        //
        HopLogStore.discardLines( job.getLogChannelId(), true );

        HopServerSingleton.getInstance().getJobMap().replaceJob( entry, newJob, jobConfiguration );
        job = newJob;
      }
    }
    job.start();

    return getJobStatus( id );
  }

  @GET
  @Path( "/stop/{id : .+}" )
  @Produces( { MediaType.APPLICATION_JSON } )
  public JobStatus stopJob( @PathParam( "id" ) String id ) {
    Job job = HopServerResource.getJob( id );
    job.stopAll();
    return getJobStatus( id );
  }

  @GET
  @Path( "/remove/{id : .+}" )
  public Response removeJob( @PathParam( "id" ) String id ) {
    Job job = HopServerResource.getJob( id );
    HopServerObjectEntry entry = HopServerResource.getCarteObjectEntry( id );
    HopLogStore.discardLines( job.getLogChannelId(), true );
    HopServerSingleton.getInstance().getJobMap().removeJob( entry );
    return Response.ok().build();
  }

  @PUT
  @Path( "/add" )
  @Produces( { MediaType.APPLICATION_JSON } )
  public JobStatus addJob( String xml ) {

    // Parse the XML, create a job configuration
    //
    JobConfiguration jobConfiguration;
    try {
      jobConfiguration = JobConfiguration.fromXML( xml.toString() );
      JobMeta jobMeta = jobConfiguration.getJobMeta();
      JobExecutionConfiguration jobExecutionConfiguration = jobConfiguration.getJobExecutionConfiguration();
      jobMeta.setLogLevel( jobExecutionConfiguration.getLogLevel() );
      jobMeta.injectVariables( jobExecutionConfiguration.getVariablesMap() );

      String carteObjectId = UUID.randomUUID().toString();
      SimpleLoggingObject servletLoggingObject =
        new SimpleLoggingObject( getClass().getName(), LoggingObjectType.CARTE, null );
      servletLoggingObject.setContainerObjectId( carteObjectId );
      servletLoggingObject.setLogLevel( jobExecutionConfiguration.getLogLevel() );

      // Create the pipeline and store in the list...
      //
      final Job job = new Job( jobMeta, servletLoggingObject );

      // Setting variables
      //
      job.initializeVariablesFrom( null );
      job.getJobMeta().setInternalHopVariables( job );
      job.injectVariables( jobConfiguration.getJobExecutionConfiguration().getVariablesMap() );

      // Also copy the parameters over...
      //
      job.copyParametersFrom( jobMeta );
      job.clearParameters();
      String[] parameterNames = job.listParameters();
      for ( int idx = 0; idx < parameterNames.length; idx++ ) {
        // Grab the parameter value set in the job entry
        //
        String thisValue = jobExecutionConfiguration.getParametersMap().get( parameterNames[ idx ] );
        if ( !Utils.isEmpty( thisValue ) ) {
          // Set the value as specified by the user in the job entry
          //
          jobMeta.setParameterValue( parameterNames[ idx ], thisValue );
        }
      }
      jobMeta.activateParameters();

      job.setSocketRepository( HopServerSingleton.getInstance().getSocketRepository() );

      HopServerSingleton.getInstance().getJobMap().addJob( job.getJobname(), carteObjectId, job, jobConfiguration );

      return getJobStatus( carteObjectId );
    } catch ( HopException e ) {
      e.printStackTrace();
    }
    return null;
  }
}
