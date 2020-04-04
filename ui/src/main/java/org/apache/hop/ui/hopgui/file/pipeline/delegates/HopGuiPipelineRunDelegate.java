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

package org.apache.hop.ui.hopgui.file.pipeline.delegates;

import org.apache.hop.cluster.SlaveServer;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.extension.ExtensionPointHandler;
import org.apache.hop.core.extension.HopExtensionPoint;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogLevel;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineExecutionConfiguration;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.debug.PipelineDebugMeta;
import org.apache.hop.pipeline.debug.TransformDebugMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.ui.core.PropsUI;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.hopgui.file.pipeline.HopGuiPipelineGraph;
import org.apache.hop.ui.pipeline.debug.PipelineDebugDialog;
import org.apache.hop.ui.pipeline.dialog.PipelineExecutionConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopGuiPipelineRunDelegate {
  private static Class<?> PKG = HopGui.class; // for i18n purposes, needed by Translator!!

  private HopGuiPipelineGraph pipelineGraph;
  private HopGui hopUi;

  private PipelineExecutionConfiguration pipelineExecutionConfiguration;
  private PipelineExecutionConfiguration pipelinePreviewExecutionConfiguration;
  private PipelineExecutionConfiguration pipelineDebugExecutionConfiguration;

  /**
   * This contains a map between the name of a pipeline and the PipelineMeta object. If the pipeline has no
   * name it will be mapped under a number [1], [2] etc.
   */
  private List<PipelineMeta> pipelineMap;

  /**
   * Remember the debugging configuration per pipeline
   */
  private Map<PipelineMeta, PipelineDebugMeta> pipelineDebugMetaMap;

  /**
   * Remember the preview configuration per pipeline
   */
  private Map<PipelineMeta, PipelineDebugMeta> pipelinePreviewMetaMap;


  /**
   * @param hopUi
   */
  public HopGuiPipelineRunDelegate( HopGui hopUi, HopGuiPipelineGraph pipelineGraph ) {
    this.hopUi = hopUi;
    this.pipelineGraph = pipelineGraph;

    pipelineExecutionConfiguration = new PipelineExecutionConfiguration();
    pipelinePreviewExecutionConfiguration = new PipelineExecutionConfiguration();
    pipelineDebugExecutionConfiguration = new PipelineExecutionConfiguration();

    pipelineMap = new ArrayList<>();
    pipelineDebugMetaMap = new HashMap<>();
    pipelinePreviewMetaMap = new HashMap<>();
  }

  public PipelineExecutionConfiguration executePipeline( final ILogChannel log, final PipelineMeta pipelineMeta, final boolean local, final boolean remote,
                                                         final boolean preview, final boolean debug, final boolean safe, LogLevel logLevel ) throws HopException {

    if ( pipelineMeta == null ) {
      return null;
    }

    // See if we need to ask for debugging information...
    //
    PipelineDebugMeta pipelineDebugMeta = null;
    PipelineExecutionConfiguration executionConfiguration = null;

    if ( preview ) {
      executionConfiguration = getPipelinePreviewExecutionConfiguration();
    } else if ( debug ) {
      executionConfiguration = getPipelineDebugExecutionConfiguration();
    } else {
      executionConfiguration = getPipelineExecutionConfiguration();
    }

    // Set defaults so the run configuration can set it up correctly
    executionConfiguration.setExecutingLocally( true );
    executionConfiguration.setExecutingRemotely( false );

    // Set MetaStore and safe mode information in both the exec config and the metadata
    //
    pipelineMeta.setMetaStore( hopUi.getMetaStore() );

    if ( debug ) {
      // See if we have debugging information stored somewhere?
      //
      pipelineDebugMeta = pipelineDebugMetaMap.get( pipelineMeta );
      if ( pipelineDebugMeta == null ) {
        pipelineDebugMeta = new PipelineDebugMeta( pipelineMeta );
        pipelineDebugMetaMap.put( pipelineMeta, pipelineDebugMeta );
      }

      // Set the default number of rows to retrieve on all selected transforms...
      //
      List<TransformMeta> selectedTransforms = pipelineMeta.getSelectedTransforms();
      if ( selectedTransforms != null && selectedTransforms.size() > 0 ) {
        pipelineDebugMeta.getTransformDebugMetaMap().clear();
        for ( TransformMeta transformMeta : pipelineMeta.getSelectedTransforms() ) {
          TransformDebugMeta transformDebugMeta = new TransformDebugMeta( transformMeta );
          transformDebugMeta.setRowCount( PropsUI.getInstance().getDefaultPreviewSize() );
          transformDebugMeta.setPausingOnBreakPoint( true );
          transformDebugMeta.setReadingFirstRows( false );
          pipelineDebugMeta.getTransformDebugMetaMap().put( transformMeta, transformDebugMeta );
        }
      }

    } else if ( preview ) {
      // See if we have preview information stored somewhere?
      //
      pipelineDebugMeta = pipelinePreviewMetaMap.get( pipelineMeta );
      if ( pipelineDebugMeta == null ) {
        pipelineDebugMeta = new PipelineDebugMeta( pipelineMeta );

        pipelinePreviewMetaMap.put( pipelineMeta, pipelineDebugMeta );
      }

      // Set the default number of preview rows on all selected transforms...
      //
      List<TransformMeta> selectedTransforms = pipelineMeta.getSelectedTransforms();
      if ( selectedTransforms != null && selectedTransforms.size() > 0 ) {
        pipelineDebugMeta.getTransformDebugMetaMap().clear();
        for ( TransformMeta transformMeta : pipelineMeta.getSelectedTransforms() ) {
          TransformDebugMeta transformDebugMeta = new TransformDebugMeta( transformMeta );
          transformDebugMeta.setRowCount( PropsUI.getInstance().getDefaultPreviewSize() );
          transformDebugMeta.setPausingOnBreakPoint( false );
          transformDebugMeta.setReadingFirstRows( true );
          pipelineDebugMeta.getTransformDebugMetaMap().put( transformMeta, transformDebugMeta );
        }
      }
    }

    int debugAnswer = PipelineDebugDialog.DEBUG_CONFIG;

    if ( debug || preview ) {
      PipelineDebugDialog pipelineDebugDialog = new PipelineDebugDialog( hopUi.getShell(), pipelineDebugMeta );
      debugAnswer = pipelineDebugDialog.open();
      if ( debugAnswer != PipelineDebugDialog.DEBUG_CANCEL ) {
        executionConfiguration.setExecutingLocally( true );
        executionConfiguration.setExecutingRemotely( false );
      } else {
        // If we cancel the debug dialog, we don't go further with the execution either.
        //
        return null;
      }
    }

    Map<String, String> variableMap = new HashMap<>();
    variableMap.putAll( executionConfiguration.getVariablesMap() ); // the default

    executionConfiguration.setVariablesMap( variableMap );
    executionConfiguration.getUsedVariables( pipelineMeta );

    executionConfiguration.setLogLevel( logLevel );

    boolean execConfigAnswer = true;

    if ( debugAnswer == PipelineDebugDialog.DEBUG_CONFIG && pipelineMeta.isShowDialog() ) {
      PipelineExecutionConfigurationDialog dialog =
        new PipelineExecutionConfigurationDialog( hopUi.getShell(), executionConfiguration, pipelineMeta );
      execConfigAnswer = dialog.open();
    }

    if ( execConfigAnswer ) {
      pipelineGraph.pipelineGridDelegate.addPipelineGrid();
      pipelineGraph.pipelineLogDelegate.addPipelineLog();
      pipelineGraph.pipelinePreviewDelegate.addPipelinePreview();
      pipelineGraph.pipelineMetricsDelegate.addPipelineMetrics();
      pipelineGraph.pipelinePerfDelegate.addPipelinePerf();
      pipelineGraph.extraViewTabFolder.setSelection( 0 );

      // Set the named parameters
      Map<String, String> paramMap = executionConfiguration.getParametersMap();
      for ( String key : paramMap.keySet() ) {
        pipelineMeta.setParameterValue( key, Const.NVL( paramMap.get( key ), "" ) );
      }
      pipelineMeta.activateParameters();

      // Set the log level
      //
      if ( executionConfiguration.getLogLevel() != null ) {
        pipelineMeta.setLogLevel( executionConfiguration.getLogLevel() );
      }

      // Set the run options
      pipelineMeta.setClearingLog( executionConfiguration.isClearingLog() );

      ExtensionPointHandler.callExtensionPoint( log, HopExtensionPoint.HopGuiPipelineMetaExecutionStart.id, pipelineMeta );
      ExtensionPointHandler.callExtensionPoint( log, HopExtensionPoint.HopUiPipelineExecutionConfiguration.id,
        executionConfiguration );

      try {
        ExtensionPointHandler.callExtensionPoint( log, HopExtensionPoint.HopUiPipelineBeforeStart.id, new Object[] { executionConfiguration, pipelineMeta, pipelineMeta } );
      } catch ( HopException e ) {
        log.logError( e.getMessage(), pipelineMeta.getFilename() );
        return null;
      }

      if ( !executionConfiguration.isExecutingLocally() && !executionConfiguration.isExecutingRemotely() ) {
        if ( pipelineMeta.hasChanged() ) {
          pipelineGraph.showSaveFileMessage();
        }
      }

      // Verify if there is at least one transform specified to debug or preview...
      //
      if ( debug || preview ) {
        if ( pipelineDebugMeta.getNrOfUsedTransforms() == 0 ) {
          MessageBox box = new MessageBox( hopUi.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO );
          box.setText( BaseMessages.getString( PKG, "HopGui.Dialog.Warning.NoPreviewOrDebugTransforms.Title" ) );
          box.setMessage( BaseMessages.getString( PKG, "HopGui.Dialog.Warning.NoPreviewOrDebugTransforms.Message" ) );
          int answer = box.open();
          if ( answer != SWT.YES ) {
            return null;
          }
        }
      }

      // Is this a local execution?
      //
      if ( executionConfiguration.isExecutingLocally() ) {
        if ( debug || preview ) {
          pipelineGraph.debug( executionConfiguration, pipelineDebugMeta );
        } else {
          pipelineGraph.start( executionConfiguration );
        }

        // Are we executing remotely?
        //
      } else if ( executionConfiguration.isExecutingRemotely() ) {
        pipelineGraph.handlePipelineMetaChanges( pipelineMeta );
        if ( pipelineMeta.hasChanged() ) {
          showSavePipelineBeforeRunningDialog( hopUi.getShell() );
        } else if ( executionConfiguration.getRemoteServer() != null ) {
          String carteObjectId =
            Pipeline.sendToSlaveServer( pipelineMeta, executionConfiguration, hopUi.getMetaStore() );
          monitorRemotePipeline( pipelineMeta, carteObjectId, executionConfiguration.getRemoteServer() );

          // TODO: add slave server monitor in different perspective
          // Also: make remote execution just like local execution through execution configurations and plugable engines.
          //hopUi.delegates.slaves.addHopGuiSlave( executionConfiguration.getRemoteServer() );

        } else {
          MessageBox mb = new MessageBox( hopUi.getShell(), SWT.OK | SWT.ICON_INFORMATION );
          mb.setMessage( BaseMessages.getString( PKG, "HopGui.Dialog.NoRemoteServerSpecified.Message" ) );
          mb.setText( BaseMessages.getString( PKG, "HopGui.Dialog.NoRemoteServerSpecified.Title" ) );
          mb.open();
        }
      }
    }
    return executionConfiguration;
  }

  private static void showSavePipelineBeforeRunningDialog( Shell shell ) {
    MessageBox m = new MessageBox( shell, SWT.OK | SWT.ICON_WARNING );
    m.setText( BaseMessages.getString( PKG, "PipelineLog.Dialog.SavePipelineBeforeRunning.Title" ) );
    m.setMessage( BaseMessages.getString( PKG, "PipelineLog.Dialog.SavePipelineBeforeRunning.Message" ) );
    m.open();
  }

  private void monitorRemotePipeline( final PipelineMeta pipelineMeta, final String carteObjectId,
                                   final SlaveServer remoteSlaveServer ) {
    // There is a pipeline running in the background. When it finishes, clean it up and log the result on the
    // console.
    // Launch in a separate thread to prevent GUI blocking...
    //
    Thread thread = new Thread( new Runnable() {
      public void run() {
        remoteSlaveServer.monitorRemotePipeline( hopUi.getLog(), carteObjectId, pipelineMeta.toString() );
      }
    } );

    thread.setName( "Monitor remote pipeline '" + pipelineMeta.getName() + "', carte object id=" + carteObjectId
      + ", slave server: " + remoteSlaveServer.getName() );
    thread.start();
  }


  /**
   * Gets pipelineGraph
   *
   * @return value of pipelineGraph
   */
  public HopGuiPipelineGraph getPipelineGraph() {
    return pipelineGraph;
  }

  /**
   * @param pipelineGraph The pipelineGraph to set
   */
  public void setPipelineGraph( HopGuiPipelineGraph pipelineGraph ) {
    this.pipelineGraph = pipelineGraph;
  }

  /**
   * Gets hopUi
   *
   * @return value of hopUi
   */
  public HopGui getHopUi() {
    return hopUi;
  }

  /**
   * @param hopUi The hopUi to set
   */
  public void setHopUi( HopGui hopUi ) {
    this.hopUi = hopUi;
  }

  /**
   * Gets pipelineExecutionConfiguration
   *
   * @return value of pipelineExecutionConfiguration
   */
  public PipelineExecutionConfiguration getPipelineExecutionConfiguration() {
    return pipelineExecutionConfiguration;
  }

  /**
   * @param pipelineExecutionConfiguration The pipelineExecutionConfiguration to set
   */
  public void setPipelineExecutionConfiguration( PipelineExecutionConfiguration pipelineExecutionConfiguration ) {
    this.pipelineExecutionConfiguration = pipelineExecutionConfiguration;
  }

  /**
   * Gets pipelinePreviewExecutionConfiguration
   *
   * @return value of pipelinePreviewExecutionConfiguration
   */
  public PipelineExecutionConfiguration getPipelinePreviewExecutionConfiguration() {
    return pipelinePreviewExecutionConfiguration;
  }

  /**
   * @param pipelinePreviewExecutionConfiguration The pipelinePreviewExecutionConfiguration to set
   */
  public void setPipelinePreviewExecutionConfiguration( PipelineExecutionConfiguration pipelinePreviewExecutionConfiguration ) {
    this.pipelinePreviewExecutionConfiguration = pipelinePreviewExecutionConfiguration;
  }

  /**
   * Gets pipelineDebugExecutionConfiguration
   *
   * @return value of pipelineDebugExecutionConfiguration
   */
  public PipelineExecutionConfiguration getPipelineDebugExecutionConfiguration() {
    return pipelineDebugExecutionConfiguration;
  }

  /**
   * @param pipelineDebugExecutionConfiguration The pipelineDebugExecutionConfiguration to set
   */
  public void setPipelineDebugExecutionConfiguration( PipelineExecutionConfiguration pipelineDebugExecutionConfiguration ) {
    this.pipelineDebugExecutionConfiguration = pipelineDebugExecutionConfiguration;
  }

  /**
   * Gets pipelineMap
   *
   * @return value of pipelineMap
   */
  public List<PipelineMeta> getPipelineMap() {
    return pipelineMap;
  }

  /**
   * @param pipelineMap The pipelineMap to set
   */
  public void setPipelineMap( List<PipelineMeta> pipelineMap ) {
    this.pipelineMap = pipelineMap;
  }

  /**
   * Gets pipelineDebugMetaMap
   *
   * @return value of pipelineDebugMetaMap
   */
  public Map<PipelineMeta, PipelineDebugMeta> getPipelineDebugMetaMap() {
    return pipelineDebugMetaMap;
  }

  /**
   * @param pipelineDebugMetaMap The pipelineDebugMetaMap to set
   */
  public void setPipelineDebugMetaMap( Map<PipelineMeta, PipelineDebugMeta> pipelineDebugMetaMap ) {
    this.pipelineDebugMetaMap = pipelineDebugMetaMap;
  }

  /**
   * Gets pipelinePreviewMetaMap
   *
   * @return value of pipelinePreviewMetaMap
   */
  public Map<PipelineMeta, PipelineDebugMeta> getPipelinePreviewMetaMap() {
    return pipelinePreviewMetaMap;
  }

  /**
   * @param pipelinePreviewMetaMap The pipelinePreviewMetaMap to set
   */
  public void setPipelinePreviewMetaMap( Map<PipelineMeta, PipelineDebugMeta> pipelinePreviewMetaMap ) {
    this.pipelinePreviewMetaMap = pipelinePreviewMetaMap;
  }
}
