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

package org.apache.hop.pipeline.transforms.selectvalues;

import org.apache.hop.core.Const;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.RowSet;
import org.apache.hop.core.exception.HopConversionException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaBase;
import org.apache.hop.core.row.value.ValueMetaBigNumber;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.junit.rules.RestoreHopEngineEnvironment;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.TransformMockUtil;
import org.apache.hop.pipeline.transforms.mock.TransformMockHelper;
import org.apache.hop.pipeline.transforms.selectvalues.SelectValuesMeta.SelectField;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Khayrutdinov
 */
public class SelectValuesTest {
  @ClassRule public static RestoreHopEngineEnvironment env = new RestoreHopEngineEnvironment();

  private static final String SELECTED_FIELD = "field";

  private final Object[] inputRow = new Object[] { "a string" };

  private SelectValues transform;
  private TransformMockHelper<SelectValuesMeta, ITransformData> helper;

  @BeforeClass
  public static void initHop() throws Exception {
    HopEnvironment.init();
  }

  @Before
  public void setUp() throws Exception {
    helper = TransformMockUtil.getTransformMockHelper( SelectValuesMeta.class, "SelectValuesTest" );
    when( helper.transformMeta.isDoingErrorHandling() ).thenReturn( true );

    transform = new SelectValues( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform = spy( transform );
    doReturn( inputRow ).when( transform ).getRow();
    doNothing().when( transform )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    RowMeta inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaString( SELECTED_FIELD ) );
    transform.setInputRowMeta( inputRowMeta );
  }

  @After
  public void cleanUp() {
    helper.cleanUp();
  }

  @Test
  public void testPDI16368() throws Exception {
    // This tests that the fix for PDI-16388 doesn't get re-broken.
    //

    SelectValuesHandler transform2 = null;
    Object[] inputRow2 = null;
    RowMeta inputRowMeta = null;
    SelectValuesMeta transformMeta = null;
    SelectValuesData transformData = null;
    IValueMeta vmi = null;
    // First, test current behavior (it's worked this way since 5.x or so)
    //
    transform2 = new SelectValuesHandler( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform2 = spy( transform2 );
    inputRow2 = new Object[] { new BigDecimal( "589" ) }; // Starting with a BigDecimal (no places)
    doReturn( inputRow2 ).when( transform2 ).getRow();
    doNothing().when( transform2 )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    transform2.setInputRowMeta( inputRowMeta );
    transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_INTEGER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask.

    transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;
    transform2.processRow();

    vmi = transform2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_BIG_NUMBER_FORMAT_MASK, vmi.getConversionMask() );

    transform2 = new SelectValuesHandler( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform2 = spy( transform2 );
    doReturn( inputRow2 ).when( transform2 ).getRow();
    doNothing().when( transform2 )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    transform2.setInputRowMeta( inputRowMeta );
    transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_NUMBER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask for Double.

    transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;
    transform2.processRow();

    vmi = transform2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_BIG_NUMBER_FORMAT_MASK, vmi.getConversionMask() );


    transform2 = new SelectValuesHandler( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform2 = spy( transform2 );
    inputRow2 = new Object[] { new Long( "589" ) }; // Starting with a Long
    doReturn( inputRow2 ).when( transform2 ).getRow();
    doNothing().when( transform2 )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaInteger( SELECTED_FIELD ) );
    transform2.setInputRowMeta( inputRowMeta );
    transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    // no specified conversion type so should have default conversion mask for BigNumber
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_BIGNUMBER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null );

    transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;
    transform2.processRow();

    vmi = transform2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_INTEGER_FORMAT_MASK, vmi.getConversionMask() );

    // Now, test that setting the variable results in getting the default conversion mask
    transform2 = new SelectValuesHandler( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform2.setVariable( Const.HOP_COMPATIBILITY_SELECT_VALUES_TYPE_CHANGE_USES_TYPE_DEFAULTS, "Y" );
    transform2 = spy( transform2 );
    inputRow2 = new Object[] { new BigDecimal( "589" ) }; // Starting with a BigDecimal (no places)
    doReturn( inputRow2 ).when( transform2 ).getRow();
    doNothing().when( transform2 )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    transform2.setInputRowMeta( inputRowMeta );
    transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_INTEGER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask.

    transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;
    transform2.processRow();

    vmi = transform2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_INTEGER_FORMAT_MASK, vmi.getConversionMask() );

    transform2 = new SelectValuesHandler( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform2.setVariable( Const.HOP_COMPATIBILITY_SELECT_VALUES_TYPE_CHANGE_USES_TYPE_DEFAULTS, "Y" );
    transform2 = spy( transform2 );
    doReturn( inputRow2 ).when( transform2 ).getRow();
    doNothing().when( transform2 )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaBigNumber( SELECTED_FIELD ) );
    transform2.setInputRowMeta( inputRowMeta );
    transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_NUMBER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null ); // no specified conversion type so should have default conversion mask for Double.

    transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;
    transform2.processRow();

    vmi = transform2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_NUMBER_FORMAT_MASK, vmi.getConversionMask() );


    transform2 = new SelectValuesHandler( helper.transformMeta, helper.iTransformData, 1, helper.pipelineMeta, helper.pipeline );
    transform2.setVariable( Const.HOP_COMPATIBILITY_SELECT_VALUES_TYPE_CHANGE_USES_TYPE_DEFAULTS, "Y" );
    transform2 = spy( transform2 );
    inputRow2 = new Object[] { new Long( "589" ) }; // Starting with a Long
    doReturn( inputRow2 ).when( transform2 ).getRow();
    doNothing().when( transform2 )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), anyString(),
        anyString() );

    inputRowMeta = new RowMeta();
    inputRowMeta.addValueMeta( new ValueMetaInteger( SELECTED_FIELD ) );
    transform2.setInputRowMeta( inputRowMeta );
    transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    // no specified conversion type so should have default conversion mask for BigNumber
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_BIGNUMBER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null );

    transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;
    transform2.processRow();

    vmi = transform2.rowMeta.getValueMeta( 0 );
    assertEquals( ValueMetaBase.DEFAULT_BIG_NUMBER_FORMAT_MASK, vmi.getConversionMask() );

  }

  @Test
  public void errorRowSetObtainsFieldName() throws Exception {
    SelectValuesMeta transformMeta = new SelectValuesMeta();
    transformMeta.allocate( 1, 0, 1 );
    transformMeta.getSelectFields()[ 0 ] = new SelectField();
    transformMeta.getSelectFields()[ 0 ].setName( SELECTED_FIELD );
    transformMeta.getMeta()[ 0 ] =
      new SelectMetadataChange( transformMeta, SELECTED_FIELD, null, IValueMeta.TYPE_INTEGER, -2, -2,
        IValueMeta.STORAGE_TYPE_NORMAL, null, false, null, null, false, null, null, null );

    SelectValuesData transformData = new SelectValuesData();
    transformData.select = true;
    transformData.metadata = true;
    transformData.firstselect = true;
    transformData.firstmetadata = true;

    transform.processRow();

    verify( transform )
      .putError( any( IRowMeta.class ), any( Object[].class ), anyLong(), anyString(), eq( SELECTED_FIELD ),
        anyString() );


    // additionally ensure conversion error causes HopConversionError
    boolean properException = false;
    try {
      transform.metadataValues( transform.getInputRowMeta(), inputRow );
    } catch ( HopConversionException e ) {
      properException = true;
    }
    assertTrue( properException );
  }

  public class SelectValuesHandler extends SelectValues {
    private Object[] resultRow;
    private IRowMeta rowMeta;
    private RowSet rowset;

    public SelectValuesHandler( TransformMeta transformMeta, ITransformData data, int copyNr, PipelineMeta pipelineMeta,
                                Pipeline pipeline ) {
      super( transformMeta, meta, data, copyNr, pipelineMeta, pipeline );
    }

    @Override
    public void putRow( IRowMeta rm, Object[] row ) throws HopTransformException {
      resultRow = row;
      rowMeta = rm;
    }

    /**
     * Find input row set.
     *
     * @param sourceTransform the source transform
     * @return the row set
     * @throws org.apache.hop.core.exception.HopTransformException the kettle transform exception
     */
    @Override
    public RowSet findInputRowSet( String sourceTransform ) throws HopTransformException {
      return rowset;
    }

  }
}
