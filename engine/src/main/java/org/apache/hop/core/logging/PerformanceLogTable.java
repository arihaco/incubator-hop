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

package org.apache.hop.core.logging;

import org.apache.hop.core.Const;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.performance.PerformanceSnapShot;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * This class describes a transform performance logging table
 *
 * @author matt
 */
public class PerformanceLogTable extends BaseLogTable implements Cloneable, ILogTable {

  private static Class<?> PKG = PerformanceLogTable.class; // for i18n purposes, needed by Translator!!

  public static final String XML_TAG = "perf-log-table";

  public enum ID {

    ID_BATCH( "ID_BATCH" ), SEQ_NR( "SEQ_NR" ), LOGDATE( "LOGDATE" ), PIPELINE_NAME( "PIPELINE_NAME" ), TRANSFORM_NAME(
      "TRANSFORM_NAME" ), TRANSFORM_COPY( "TRANSFORM_COPY" ), LINES_READ( "LINES_READ" ), LINES_WRITTEN( "LINES_WRITTEN" ),
    LINES_UPDATED( "LINES_UPDATED" ), LINES_INPUT( "LINES_INPUT" ), LINES_OUTPUT( "LINES_OUTPUT" ),
    LINES_REJECTED( "LINES_REJECTED" ), ERRORS( "ERRORS" ), INPUT_BUFFER_ROWS( "INPUT_BUFFER_ROWS" ),
    OUTPUT_BUFFER_ROWS( "OUTPUT_BUFFER_ROWS" );

    private String id;

    private ID( String id ) {
      this.id = id;
    }

    public String toString() {
      return id;
    }
  }

  private String logInterval;

  private PerformanceLogTable( IVariables variables, IMetaStore metaStore ) {
    super( variables, metaStore, null, null, null );
  }

  @Override
  public Object clone() {
    try {
      PerformanceLogTable table = (PerformanceLogTable) super.clone();
      table.fields = new ArrayList<LogTableField>();
      for ( LogTableField field : this.fields ) {
        table.fields.add( (LogTableField) field.clone() );
      }
      return table;
    } catch ( CloneNotSupportedException e ) {
      return null;
    }
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder();

    retval.append( "      " ).append( XMLHandler.openTag( XML_TAG ) ).append( Const.CR );
    retval.append( "        " ).append( XMLHandler.addTagValue( "connection", connectionName ) );
    retval.append( "        " ).append( XMLHandler.addTagValue( "schema", schemaName ) );
    retval.append( "        " ).append( XMLHandler.addTagValue( "table", tableName ) );
    retval.append( "        " ).append( XMLHandler.addTagValue( "interval", logInterval ) );
    retval.append( "        " ).append( XMLHandler.addTagValue( "timeout_days", timeoutInDays ) );
    retval.append( super.getFieldsXML() );
    retval.append( "      " ).append( XMLHandler.closeTag( XML_TAG ) ).append( Const.CR );

    return retval.toString();
  }

  public void loadXML( Node node, List<TransformMeta> transforms ) {
    connectionName = XMLHandler.getTagValue( node, "connection" );
    schemaName = XMLHandler.getTagValue( node, "schema" );
    tableName = XMLHandler.getTagValue( node, "table" );
    logInterval = XMLHandler.getTagValue( node, "interval" );
    timeoutInDays = XMLHandler.getTagValue( node, "timeout_days" );

    super.loadFieldsXML( node );
  }

  @Override
  public void replaceMeta( ILogTableCore logTableInterface ) {
    if ( !( logTableInterface instanceof PerformanceLogTable ) ) {
      return;
    }

    PerformanceLogTable logTable = (PerformanceLogTable) logTableInterface;
    super.replaceMeta( logTable );
  }

  public static PerformanceLogTable getDefault( IVariables variables, IMetaStore metaStore ) {
    PerformanceLogTable table = new PerformanceLogTable( variables, metaStore );

    //CHECKSTYLE:LineLength:OFF
    table.fields.add( new LogTableField( ID.ID_BATCH.id, true, false, "ID_BATCH", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.BatchID" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.BatchID" ), IValueMeta.TYPE_INTEGER, 8 ) );
    table.fields.add( new LogTableField( ID.SEQ_NR.id, true, false, "SEQ_NR", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.SeqNr" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.SeqNr" ), IValueMeta.TYPE_INTEGER, 8 ) );
    table.fields.add( new LogTableField( ID.LOGDATE.id, true, false, "LOGDATE", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LogDate" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LogDate" ), IValueMeta.TYPE_DATE, -1 ) );
    table.fields.add( new LogTableField( ID.PIPELINE_NAME.id, true, false, "PIPELINE_NAME", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.PipelineName" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.PipelineName" ), IValueMeta.TYPE_STRING, 255 ) );
    table.fields.add( new LogTableField( ID.TRANSFORM_NAME.id, true, false, "TRANSFORM_NAME", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.TransformName" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.TransformName" ), IValueMeta.TYPE_STRING, 255 ) );
    table.fields.add( new LogTableField( ID.TRANSFORM_COPY.id, true, false, "TRANSFORM_COPY", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.TransformCopy" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.TransformCopy" ), IValueMeta.TYPE_INTEGER, 8 ) );
    table.fields.add( new LogTableField( ID.LINES_READ.id, true, false, "LINES_READ", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LinesRead" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LinesRead" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.LINES_WRITTEN.id, true, false, "LINES_WRITTEN", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LinesWritten" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LinesWritten" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.LINES_UPDATED.id, true, false, "LINES_UPDATED", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LinesUpdated" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LinesUpdated" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.LINES_INPUT.id, true, false, "LINES_INPUT", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LinesInput" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LinesInput" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.LINES_OUTPUT.id, true, false, "LINES_OUTPUT", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LinesOutput" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LinesOutput" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.LINES_REJECTED.id, true, false, "LINES_REJECTED", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.LinesRejected" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.LinesRejected" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.ERRORS.id, true, false, "ERRORS", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.Errors" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.Errors" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.INPUT_BUFFER_ROWS.id, true, false, "INPUT_BUFFER_ROWS", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.InputBufferRows" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.InputBufferRows" ), IValueMeta.TYPE_INTEGER, 18 ) );
    table.fields.add( new LogTableField( ID.OUTPUT_BUFFER_ROWS.id, true, false, "OUTPUT_BUFFER_ROWS", BaseMessages.getString( PKG, "PerformanceLogTable.FieldName.OutputBufferRows" ),
      BaseMessages.getString( PKG, "PerformanceLogTable.FieldDescription.OutputBufferRows" ), IValueMeta.TYPE_INTEGER, 18 ) );

    table.findField( ID.ID_BATCH.id ).setKey( true );
    table.findField( ID.LOGDATE.id ).setLogDateField( true );
    table.findField( ID.PIPELINE_NAME.id ).setNameField( true );

    return table;
  }

  /**
   * Sets the logging interval in seconds. Disabled if the logging interval is <=0.
   *
   * @param logInterval The log interval value. A value higher than 0 means that the log table is updated every 'logInterval'
   *                    seconds.
   */
  public void setLogInterval( String logInterval ) {
    this.logInterval = logInterval;
  }

  /**
   * Get the logging interval in seconds. Disabled if the logging interval is <=0. A value higher than 0 means that the
   * log table is updated every 'logInterval' seconds.
   *
   * @return The log interval,
   */
  public String getLogInterval() {
    return logInterval;
  }

  /**
   * This method calculates all the values that are required
   *
   * @param status  the log status to use
   * @param subject
   * @param parent
   */
  public RowMetaAndData getLogRecord( LogStatus status, Object subject, Object parent ) {
    if ( subject == null || subject instanceof PerformanceSnapShot ) {
      PerformanceSnapShot snapShot = (PerformanceSnapShot) subject;

      RowMetaAndData row = new RowMetaAndData();

      for ( LogTableField field : fields ) {
        if ( field.isEnabled() ) {
          Object value = null;
          if ( subject != null ) {
            switch ( ID.valueOf( field.getId() ) ) {

              case ID_BATCH:
                value = new Long( snapShot.getBatchId() );
                break;
              case SEQ_NR:
                value = new Long( snapShot.getSeqNr() );
                break;
              case LOGDATE:
                value = snapShot.getDate();
                break;
              case PIPELINE_NAME:
                value = snapShot.getParentName();
                break;
              case TRANSFORM_NAME:
                value = snapShot.getComponentName();
                break;
              case TRANSFORM_COPY:
                value = new Long( snapShot.getCopyNr() );
                break;
              case LINES_READ:
                value = new Long( snapShot.getLinesRead() );
                break;
              case LINES_WRITTEN:
                value = new Long( snapShot.getLinesWritten() );
                break;
              case LINES_INPUT:
                value = new Long( snapShot.getLinesInput() );
                break;
              case LINES_OUTPUT:
                value = new Long( snapShot.getLinesOutput() );
                break;
              case LINES_UPDATED:
                value = new Long( snapShot.getLinesUpdated() );
                break;
              case LINES_REJECTED:
                value = new Long( snapShot.getLinesRejected() );
                break;
              case ERRORS:
                value = new Long( snapShot.getErrors() );
                break;
              case INPUT_BUFFER_ROWS:
                value = new Long( snapShot.getInputBufferSize() );
                break;
              case OUTPUT_BUFFER_ROWS:
                value = new Long( snapShot.getOutputBufferSize() );
                break;
              default:
                break;
            }
          }

          row.addValue( field.getFieldName(), field.getDataType(), value );
          row.getRowMeta().getValueMeta( row.size() - 1 ).setLength( field.getLength() );
        }
      }

      return row;
    } else {
      return null;
    }
  }

  public String getLogTableCode() {
    return "PERFORMANCE";
  }

  public String getLogTableType() {
    return BaseMessages.getString( PKG, "PerformanceLogTable.Type.Description" );
  }

  public String getConnectionNameVariable() {
    return Const.HOP_PIPELINE_PERFORMANCE_LOG_DB;
  }

  public String getSchemaNameVariable() {
    return Const.HOP_PIPELINE_PERFORMANCE_LOG_SCHEMA;
  }

  public String getTableNameVariable() {
    return Const.HOP_PIPELINE_PERFORMANCE_LOG_TABLE;
  }

  public List<IRowMeta> getRecommendedIndexes() {
    List<IRowMeta> indexes = new ArrayList<IRowMeta>();
    return indexes;
  }

  @Override
  public void setAllGlobalParametersToNull() {
    boolean clearGlobalVariables = Boolean.valueOf( System.getProperties().getProperty( Const.HOP_GLOBAL_LOG_VARIABLES_CLEAR_ON_EXPORT, "false" ) );
    if ( clearGlobalVariables ) {
      super.setAllGlobalParametersToNull();

      logInterval = isGlobalParameter( logInterval ) ? null : logInterval;
    }
  }
}
