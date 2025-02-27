/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.core.sql;

import java.util.List;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopSqlException;
import org.apache.hop.core.jdbc.ThinUtil;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.ValueMetaAndData;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;

public class SqlField {
  private String tableAlias;
  private String field;
  private String alias;
  private SqlAggregation aggregation;
  private IValueMeta valueMeta;
  private boolean countStar;
  private boolean countDistinct;
  private boolean orderField;
  private boolean ascending;
  private String expression;
  private SqlFields selectFields;
  private Object valueData;

  /** IIF function hack */
  private IifFunction iif;

  /** To easily figure out to which index in the select this field belongs */
  private int fieldIndex;

  /**
   * @param field
   * @param alias
   * @param aggregation
   * @param valueMeta
   */
  public SqlField(
      String tableAlias,
      String field,
      String alias,
      SqlAggregation aggregation,
      IValueMeta valueMeta) {
    this.tableAlias = tableAlias;
    this.field = field;
    this.alias = alias;
    this.aggregation = aggregation;
    this.valueMeta = valueMeta;
  }

  public SqlField(String tableAlias, String fieldClause, IRowMeta serviceFields)
      throws HopException {
    this(tableAlias, fieldClause, serviceFields, false);
  }

  public SqlField(String tableAlias, String fieldClause, IRowMeta serviceFields, boolean orderField)
      throws HopException {
    this(tableAlias, fieldClause, serviceFields, orderField, null);
  }

  public SqlField(
      String tableAlias,
      String fieldClause,
      IRowMeta serviceFields,
      boolean orderField,
      SqlFields selectFields)
      throws HopException {
    this.tableAlias = tableAlias;
    this.orderField = orderField;
    this.selectFields = selectFields;

    // The field clause is in the form: <field or aggregate> [as] [alias]
    // Fields can be quoted with "
    //
    List<String> strings = ThinUtil.splitClause(fieldClause, ' ', '"', '(');

    if (strings.size() == 0) {
      throw new HopSqlException("Unable to find a valid field");
    }

    if (strings.size() >= 1) {
      String value = strings.get(0);
      field = ThinUtil.stripQuoteTableAlias(value, tableAlias);
      expression = field;

      if (orderField) {
        if (strings.size() > 2) {
          throw new HopSqlException(
              "Too many elements for an ORDER BY argument: [" + fieldClause + "]");
        }
        if (strings.size() == 2) {
          String ascDesc = strings.get(1);
          if ("ASC".equalsIgnoreCase(ascDesc)) {
            ascending = true;
          } else if ("DESC".equalsIgnoreCase(ascDesc)) {
            ascending = false;
          } else {
            throw new HopSqlException("Unable to recognize sort order [" + ascDesc + "]");
          }
        } else {
          ascending = true;
        }
      } else {

        // see if it's an aggregate like SUM( foo )
        //
        for (SqlAggregation agg : SqlAggregation.values()) {
          if (value.toUpperCase().startsWith(agg.getKeyWord() + "(")) {
            aggregation = agg;
            // also determine the field..;
            //
            int openIndex = value.indexOf('(', agg.getKeyWord().length());
            if (openIndex < 0) {
              throw new HopSqlException(
                  "No opening bracket found after keyword ["
                      + aggregation.getKeyWord()
                      + "] in clause ["
                      + fieldClause
                      + "]");
            }
            int closeIndex = value.lastIndexOf(')');
            if (closeIndex <= openIndex) {
              throw new HopSqlException(
                  "No closing bracket found after keyword [" + aggregation.getKeyWord() + "]");
            }
            field =
                ThinUtil.stripQuotes(Const.trim(value.substring(openIndex + 1, closeIndex)), '"');
            field = ThinUtil.stripQuoteTableAlias(field, tableAlias);
            break;
          }
        }

        if (SqlAggregation.COUNT == aggregation) {

          // COUNT(*)
          //
          if ("*".equals(field)) {
            countStar = true;
          }

          // COUNT(DISTINCT foo)
          //
          if (field.toUpperCase().startsWith("DISTINCT ")) {
            int firstSpaceIndex = field.indexOf(' ');
            field = field.substring(firstSpaceIndex + 1);
            field = ThinUtil.stripQuoteTableAlias(field, tableAlias);

            countDistinct = true;
          }

          alias = Const.NVL(alias, expression);
        }

        if (strings.size() == 2) {
          alias = ThinUtil.stripQuotes(Const.trim(strings.get(1)), '"');
        }
        // Uses the "AS" word in between
        if (strings.size() == 3) {
          if (!"as".equalsIgnoreCase(strings.get(1))) {
            throw new HopSqlException(
                "AS keyword expected between the field and the alias in field clause: ["
                    + fieldClause
                    + "]");
          }
          alias = ThinUtil.unQuote(Const.trim(strings.get(2))).replaceAll("\"\"", "\"");
        }
      }
    }

    if (!countStar) {
      if (orderField) {
        // For order by fields we need to see what the expression was in the select clause
        //
        for (SqlField selectField : selectFields.getFields()) {
          if (selectField.getExpression().equalsIgnoreCase(field)) {
            if (selectField.getAggregation() != null) {

              switch (selectField.getAggregation()) {
                case COUNT:
                  valueMeta =
                      ValueMetaFactory.createValueMeta(field, IValueMeta.TYPE_INTEGER, 15, -1);
                  break;
                case MIN:
                case MAX:
                case AVG:
                case SUM:
                  valueMeta = selectField.getValueMeta();
                  break;
                default:
                  break;
              }
              alias = selectField.getAlias();

            } else {
              // regular field but grab the new name if any, we need it during generation
              //
              field = selectField.getField();
              alias = selectField.getAlias();
            }
          }
        }
      }

      // See if the field is a function...
      // TODO: make generic, for now keep it simple
      //
      if (field.startsWith("IIF(")) {
        String arguments = field.substring(4, field.length() - 1); // skip the closing bracket too
        List<String> argsList = ThinUtil.splitClause(arguments, ',', '\'', '(');
        if (argsList.size() != 3) {
          throw new HopSqlException("The IIF function requires exactly 3 arguments");
        }
        iif =
            new IifFunction(
                tableAlias,
                Const.trim(argsList.get(0)),
                Const.trim(argsList.get(1)),
                Const.trim(argsList.get(2)),
                serviceFields);

      } else if (field.toUpperCase().startsWith("CASE WHEN ")
          && field.toUpperCase().endsWith("END")) {
        // Same as IIF but with a different format.
        //
        String condition = Const.trim(ThinUtil.findClause(field, "WHEN", "THEN"));
        String trueClause = Const.trim(ThinUtil.findClause(field, "THEN", "ELSE"));
        String falseClause = Const.trim(ThinUtil.findClause(field, "ELSE", "END"));
        iif = new IifFunction(tableAlias, condition, trueClause, falseClause, serviceFields);

      } else {
        if (valueMeta == null) {
          field =
              ThinUtil.resolveFieldName(
                  ThinUtil.unQuote(field).replaceAll("\"\"", "\""), serviceFields);
          valueMeta = serviceFields.searchValueMeta(field);
          if (orderField && selectFields != null) {
            // See if this isn't an aliased select field that we're ordering on
            //
            for (SqlField selectField : selectFields.getFields()) {
              if (selectField.getAlias() == null) {
                continue;
              }
              if (field.equalsIgnoreCase(
                  ThinUtil.unQuote(selectField.getAlias()).replaceAll("\"\"", "\""))) {
                valueMeta = selectField.getValueMeta();
                break;
              }
            }
          }
        }

        if (valueMeta == null) {

          // OK, field is not a service field nor an aggregate, not IIF
          // See if it's a constant value...
          //
          ValueMetaAndData vmad = ThinUtil.extractConstant(field);
          if (vmad != null) {
            valueMeta = vmad.getValueMeta();
            valueData = vmad.getValueData();
          } else {
            throw new HopSqlException(
                "The field with name [" + field + "] could not be found in the service output");
          }
        }
      }
    }
  }

  /** @return the name */
  public String getName() {
    return field;
  }

  /** @param name the name to set */
  public void setName(String name) {
    this.field = name;
  }

  /** @return the alias */
  public String getAlias() {
    return alias;
  }

  /** @param alias the alias to set */
  public void setAlias(String alias) {
    this.alias = alias;
  }

  /** @return the aggregation */
  public SqlAggregation getAggregation() {
    return aggregation;
  }

  /** @param aggregation the aggregation to set */
  public void setAggregation(SqlAggregation aggregation) {
    this.aggregation = aggregation;
  }

  /** @return the valueMeta */
  public IValueMeta getValueMeta() {
    return valueMeta;
  }

  /** @param valueMeta the valueMeta to set */
  public void setValueMeta(IValueMeta valueMeta) {
    this.valueMeta = valueMeta;
  }

  /** @return the field */
  public String getField() {
    return field;
  }

  /** @param field the field to set */
  public void setField(String field) {
    this.field = field;
  }

  /** @return the countStar */
  public boolean isCountStar() {
    return countStar;
  }

  /** @param countStar the countStar to set */
  public void setCountStar(boolean countStar) {
    this.countStar = countStar;
  }

  /** @return the countDistinct */
  public boolean isCountDistinct() {
    return countDistinct;
  }

  /** @param countDistinct the countDistinct to set */
  public void setCountDistinct(boolean countDistinct) {
    this.countDistinct = countDistinct;
  }

  /** @return the orderField */
  public boolean isOrderField() {
    return orderField;
  }

  /** @param orderField the orderField to set */
  public void setOrderField(boolean orderField) {
    this.orderField = orderField;
  }

  /** @return the ascending */
  public boolean isAscending() {
    return ascending;
  }

  /** @param ascending the ascending to set */
  public void setAscending(boolean ascending) {
    this.ascending = ascending;
  }

  /** @return the expression */
  public String getExpression() {
    return expression;
  }

  /** @param expression the expression to set */
  public void setExpression(String expression) {
    this.expression = expression;
  }

  /** @return the selectFields */
  public SqlFields getSelectFields() {
    return selectFields;
  }

  /** @param selectFields the selectFields to set */
  public void setSelectFields(SqlFields selectFields) {
    this.selectFields = selectFields;
  }

  /** @return the iif */
  public IifFunction getIif() {
    return iif;
  }

  /** @return the valueData */
  public Object getValueData() {
    return valueData;
  }

  /** @param valueData the valueData to set */
  public void setValueData(Object valueData) {
    this.valueData = valueData;
  }

  /** @param iif the iif to set */
  public void setIif(IifFunction iif) {
    this.iif = iif;
  }

  /** @return the tableAlias */
  public String getTableAlias() {
    return tableAlias;
  }

  /** @param tableAlias the tableAlias to set */
  public void setTableAlias(String tableAlias) {
    this.tableAlias = tableAlias;
  }

  /** @return the fieldIndex */
  public int getFieldIndex() {
    return fieldIndex;
  }

  /** @param fieldIndex the fieldIndex to set */
  public void setFieldIndex(int fieldIndex) {
    this.fieldIndex = fieldIndex;
  }

  public static SqlField searchSQLFieldByFieldOrAlias(List<SqlField> fields, String name) {
    for (SqlField field : fields) {
      if (name.equalsIgnoreCase(field.getField()) || name.equalsIgnoreCase(field.getAlias())) {
        return field;
      }
    }
    return null;
  }
}
