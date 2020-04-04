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

package org.apache.hop.pipeline.transforms.setvalueconstant;

import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.injection.Injection;
import org.apache.hop.core.injection.InjectionDeep;
import org.apache.hop.core.injection.InjectionSupported;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.iVariables;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.TransformMetaInterface;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

@InjectionSupported( localizationPrefix = "SetValueConstant.Injection.", groups = { "FIELDS", "OPTIONS" } )
public class SetValueConstantMeta extends BaseTransformMeta implements TransformMetaInterface {
  private static Class<?> PKG = SetValueConstantMeta.class; // for i18n purposes, needed by Translator!!

  @InjectionDeep
  private List<Field> fields = new ArrayList<>();

  public Field getField( int i ) {
    return fields.get( i );
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields( List<Field> fields ) {
    this.fields = fields;
  }

  @Injection( name = "USE_VARIABLE", group = "OPTIONS" )
  private boolean usevar;

  public SetValueConstantMeta() {
    super(); // allocate BaseTransformMeta
  }

  public void loadXML( Node transformNode, IMetaStore metaStore ) throws HopXMLException {
    readData( transformNode, metaStore );
  }

  public void setUseVars( boolean usevar ) {
    this.usevar = usevar;
  }

  public boolean isUseVars() {
    return usevar;
  }

  private void readData( Node transformNode, IMetaStore metaStore ) throws HopXMLException {
    try {
      usevar = "Y".equalsIgnoreCase( XMLHandler.getTagValue( transformNode, "usevar" ) );
      Node fields = XMLHandler.getSubNode( transformNode, "fields" );
      int nrFields = XMLHandler.countNodes( fields, "field" );
      List<Field> fieldList = new ArrayList<>();
      for ( int i = 0; i < nrFields; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
        Field field = new Field();
        field.setFieldName( XMLHandler.getTagValue( fnode, "name" ) );
        field.setReplaceValue( XMLHandler.getTagValue( fnode, "value" ) );
        field.setReplaceMask( XMLHandler.getTagValue( fnode, "mask" ) );
        String emptyString = XMLHandler.getTagValue( fnode, "set_empty_string" );
        field.setEmptyString( !Utils.isEmpty( emptyString ) && "Y".equalsIgnoreCase( emptyString ) );
        fieldList.add( field );
      }
      setFields( fieldList );
    } catch ( Exception e ) {
      throw new HopXMLException( "It was not possible to load the metadata for this transform from XML", e );
    }
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder();
    retval.append( "   " + XMLHandler.addTagValue( "usevar", usevar ) );
    retval.append( "    <fields>" + Const.CR );
    fields.forEach( field -> {
      retval.append( "      <field>" + Const.CR );
      retval.append( "        " + XMLHandler.addTagValue( "name", field.getFieldName() ) );
      retval.append( "        " + XMLHandler.addTagValue( "value", field.getReplaceValue() ) );
      retval.append( "        " + XMLHandler.addTagValue( "mask", field.getReplaceMask() ) );
      retval.append( "        " + XMLHandler.addTagValue( "set_empty_string", field.isEmptyString() ) );
      retval.append( "        </field>" + Const.CR );
    } );
    retval.append( "      </fields>" + Const.CR );

    return retval.toString();
  }

  public void setDefault() {
    usevar = false;
  }

  public void check( List<CheckResultInterface> remarks, PipelineMeta pipelineMeta, TransformMeta transformMeta,
                     IRowMeta prev, String[] input, String[] output, IRowMeta info, iVariables variables,
                     IMetaStore metaStore ) {
    CheckResult cr;
    if ( prev == null || prev.size() == 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(
          PKG, "SetValueConstantMeta.CheckResult.NotReceivingFields" ), transformMeta );
      remarks.add( cr );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SetValueConstantMeta.CheckResult.TransformRecevingData", prev.size() + "" ), transformMeta );
      remarks.add( cr );

      String error_message = "";
      boolean error_found = false;

      // Starting from selected fields in ...
      for ( int i = 0; i < fields.size(); i++ ) {
        int idx = prev.indexOfValue( fields.get( i ).getFieldName() );
        if ( idx < 0 ) {
          error_message += "\t\t" + fields.get( i ).getFieldName() + Const.CR;
          error_found = true;
        }
      }
      if ( error_found ) {
        error_message =
          BaseMessages.getString( PKG, "SetValueConstantMeta.CheckResult.FieldsFound", error_message );

        cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, transformMeta );
        remarks.add( cr );
      } else {
        if ( Utils.isEmpty( fields ) ) {
          cr =
            new CheckResult( CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(
              PKG, "SetValueConstantMeta.CheckResult.NoFieldsEntered" ), transformMeta );
        } else {
          cr =
            new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
              PKG, "SetValueConstantMeta.CheckResult.AllFieldsFound" ), transformMeta );
        }
        remarks.add( cr );
      }

    }

    // See if we have input streams leading to this transform!
    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SetValueConstantMeta.CheckResult.TransformRecevingData2" ), transformMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SetValueConstantMeta.CheckResult.NoInputReceivedFromOtherTransforms" ), transformMeta );
    }
    remarks.add( cr );
  }

  public ITransform getTransform( TransformMeta transformMeta, ITransformData data, int cnr, PipelineMeta tr,
                                Pipeline pipeline ) {
    return new SetValueConstant( transformMeta, this, data, cnr, tr, pipeline );
  }

  public ITransformData getTransformData() {
    return new SetValueConstantData();
  }

  public boolean supportsErrorHandling() {
    return true;
  }

  public static class Field {

    @Injection( name = "FIELD_NAME", group = "FIELDS" )
    private String fieldName;

    @Injection( name = "REPLACE_VALUE", group = "FIELDS" )
    private String replaceValue;

    @Injection( name = "REPLACE_MASK", group = "FIELDS" )
    private String replaceMask;

    @Injection( name = "EMPTY_STRING", group = "FIELDS" )
    private boolean setEmptyString;

    public String getFieldName() {
      return fieldName;
    }

    public void setFieldName( String fieldName ) {
      this.fieldName = fieldName;
    }

    public String getReplaceValue() {
      return replaceValue;
    }

    public void setReplaceValue( String replaceValue ) {
      this.replaceValue = replaceValue;
    }

    public String getReplaceMask() {
      return replaceMask;
    }

    public void setReplaceMask( String replaceMask ) {
      this.replaceMask = replaceMask;
    }

    public boolean isEmptyString() {
      return setEmptyString;
    }

    public void setEmptyString( boolean setEmptyString ) {
      this.setEmptyString = setEmptyString;
    }

    @Override
    public boolean equals( Object obj ) {
      return fieldName.equals( ( (Field) obj ).getFieldName() )
        && replaceValue.equals( ( (Field) obj ).getReplaceValue() )
        && replaceMask.equals( ( (Field) obj ).getReplaceMask() )
        && setEmptyString == ( (Field) obj ).isEmptyString();
    }
  }
}
