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

package org.apache.hop.core.jdbc;

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.exception.HopSqlException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.ValueMetaAndData;
import org.apache.hop.core.row.value.ValueMetaBigNumber;
import org.apache.hop.core.row.value.ValueMetaBinary;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNone;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.sql.FoundClause;
import org.apache.hop.core.xml.XmlHandler;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThinUtil {

  public static final String DATE_FORMAT_MASK = "yyyy/MM/dd HH:mm:ss.SSS";

  public static String stripNewlines(String sql) {
    if (sql == null) {
      return null;
    }

    StringBuilder sbSql = new StringBuilder(sql);

    for (int i = sbSql.length() - 1; i >= 0; i--) {
      if (sbSql.charAt(i) == '\n' || sbSql.charAt(i) == '\r') {
        sbSql.setCharAt(i, ' ');
      }
    }
    return sbSql.toString();
  }

  public static int getSqlType(IValueMeta valueMeta) {
    switch (valueMeta.getType()) {
      case IValueMeta.TYPE_STRING:
        return java.sql.Types.VARCHAR;
      case IValueMeta.TYPE_DATE:
        return java.sql.Types.TIMESTAMP;
      case IValueMeta.TYPE_INTEGER:
        return java.sql.Types.BIGINT; // TODO: for metadata we don't want a long?
      case IValueMeta.TYPE_BIGNUMBER:
        return java.sql.Types.DECIMAL;
      case IValueMeta.TYPE_NUMBER:
        return java.sql.Types.DOUBLE;
      case IValueMeta.TYPE_BOOLEAN:
        return java.sql.Types.BOOLEAN;
      case IValueMeta.TYPE_BINARY:
        return java.sql.Types.BLOB;
      case IValueMeta.TYPE_NONE:
        return java.sql.Types.OTHER;
      default:
        break;
    }
    return java.sql.Types.VARCHAR;
  }

  public static String getSqlTypeDesc(IValueMeta valueMeta) {
    switch (valueMeta.getType()) {
      case IValueMeta.TYPE_STRING:
        return "VARCHAR";
      case IValueMeta.TYPE_DATE:
        return "TIMESTAMP";
      case IValueMeta.TYPE_INTEGER:
        return "BIGINT"; // TODO: for metadata we don't want a long?
      case IValueMeta.TYPE_NUMBER:
        return "DOUBLE";
      case IValueMeta.TYPE_BIGNUMBER:
        return "DECIMAL";
      case IValueMeta.TYPE_BOOLEAN:
        return "BOOLEAN";
      case IValueMeta.TYPE_BINARY:
        return "BLOB";
      case IValueMeta.TYPE_NONE:
        return "OTHER";
      default:
        break;
    }
    return null;
  }

  public static IValueMeta getValueMeta(String valueName, int sqlType) throws SQLException {
    switch (sqlType) {
      case java.sql.Types.OTHER:
      case java.sql.Types.NULL:
        return new ValueMetaNone(valueName);

      case java.sql.Types.BIGINT:
      case java.sql.Types.INTEGER:
      case java.sql.Types.SMALLINT:
        return new ValueMetaInteger(valueName);

      case java.sql.Types.CHAR:
      case java.sql.Types.VARCHAR:
      case java.sql.Types.CLOB:
        return new ValueMetaString(valueName);

      case java.sql.Types.DATE:
      case java.sql.Types.TIMESTAMP:
      case java.sql.Types.TIME:
        return new ValueMetaDate(valueName);

      case java.sql.Types.DECIMAL:
        return new ValueMetaBigNumber(valueName);

      case java.sql.Types.DOUBLE:
      case java.sql.Types.FLOAT:
        return new ValueMetaNumber(valueName);

      case java.sql.Types.BOOLEAN:
      case java.sql.Types.BIT:
        return new ValueMetaBoolean(valueName);

      case java.sql.Types.BINARY:
      case java.sql.Types.BLOB:
        return new ValueMetaBinary(valueName);

      default:
        throw new SQLException(
            "Don't know how to handle Sql Type: " + sqlType + ", with name: " + valueName);
    }
  }

  public static ValueMetaAndData attemptDateValueExtraction(String string)
      throws HopPluginException {
    if (string.length() > 2 && string.startsWith("[") && string.endsWith("]")) {
      String unquoted = string.substring(1, string.length() - 1);
      if (unquoted.length() >= 9 && unquoted.charAt(4) == '/' && unquoted.charAt(7) == '/') {
        Date date = XmlHandler.stringToDate(unquoted);
        if (date == null) {
          try {
            date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(unquoted);
          } catch (ParseException e1) {
            try {
              date = new SimpleDateFormat("yyyy/MM/dd").parse(unquoted);
            } catch (ParseException e2) {
              date = null;
            }
          }
        }
        if (date != null) {
          IValueMeta valueMeta = ValueMetaFactory.createValueMeta("iif-date", IValueMeta.TYPE_DATE);
          // default conversion mask is used for data saving irrespective of locale differences
          valueMeta.setConversionMask(DATE_FORMAT_MASK);
          return new ValueMetaAndData(valueMeta, date);
        }
      }
    }

    Matcher matcher = Pattern.compile("^([A-Za-z]+) ?'([0-9\\-:\\. ]+)'$").matcher(string);
    if (matcher.find()) {
      if (matcher.groupCount() == 2) {
        String keyword = matcher.group(1);
        String dateString = matcher.group(2);
        String format = null;
        if (keyword.equalsIgnoreCase("TIMESTAMP")) {
          format = "yyyy-MM-dd HH:mm:ss";
        } else if (keyword.equalsIgnoreCase("DATE")) {
          format = "yyyy-MM-dd";
        }
        if (format != null) {
          Date date = null;
          try {
            date = new SimpleDateFormat(format).parse(dateString);
          } catch (ParseException e) {
            // Format does not match
          }
          if (date != null) {
            IValueMeta valueMeta =
                ValueMetaFactory.createValueMeta("iff-date", IValueMeta.TYPE_DATE);
            // default conversion mask is used for data saving irrespective of locale differences
            valueMeta.setConversionMask(DATE_FORMAT_MASK);
            return new ValueMetaAndData(valueMeta, date);
          }
        }
      }
    }

    return null;
  }

  public static ValueMetaAndData attemptIntegerValueExtraction(String string) {
    // Try an Integer
    if (!string.contains(".")) {
      try {
        long l = Long.parseLong(string);
        if (Long.toString(l).equals(string)) {
          ValueMetaAndData value = new ValueMetaAndData();
          IValueMeta valueMeta =
              ValueMetaFactory.createValueMeta("Constant", IValueMeta.TYPE_INTEGER);
          valueMeta.setConversionMask("0");
          valueMeta.setGroupingSymbol(null);
          value.setValueMeta(valueMeta);
          value.setValueData(Long.valueOf(l));
          return value;
        }
      } catch (NumberFormatException | HopPluginException e) {
        // ignore - will return null below
      }
    }
    return null;
  }

  public static ValueMetaAndData attemptNumberValueExtraction(String string) {
    // Try a Number
    try {
      double d = Double.parseDouble(string);
      if (Double.toString(d).equals(string)) {
        ValueMetaAndData value = new ValueMetaAndData();
        IValueMeta valueMeta = ValueMetaFactory.createValueMeta("Constant", IValueMeta.TYPE_NUMBER);
        valueMeta.setConversionMask("0.#");
        valueMeta.setGroupingSymbol(null);
        valueMeta.setDecimalSymbol(".");
        value.setValueMeta(valueMeta);
        value.setValueData(Double.valueOf(d));
        return value;
      }
    } catch (NumberFormatException | HopPluginException e) {
      // ignore - will return null below
    }
    return null;
  }

  public static ValueMetaAndData attemptBigNumberValueExtraction(String string) {
    // Try a BigNumber
    try {
      BigDecimal d = new BigDecimal(string);
      if (d.toString().equals(string)) {
        ValueMetaAndData value = new ValueMetaAndData();
        value.setValueMeta(ValueMetaFactory.createValueMeta("Constant", IValueMeta.TYPE_BIGNUMBER));
        value.setValueData(d);
        return value;
      }
    } catch (NumberFormatException | HopPluginException e) {
      // ignore - will return null below
    }
    return null;
  }

  public static ValueMetaAndData attemptStringValueExtraction(String string)
      throws HopPluginException {
    if (string.startsWith("'") && string.endsWith("'")) {
      String s = string.substring(1, string.length() - 1);

      // Make sure to replace quoted '' occurrences.
      //
      s = s.replace("''", "'");

      ValueMetaAndData value = new ValueMetaAndData();
      value.setValueMeta(ValueMetaFactory.createValueMeta("Constant", IValueMeta.TYPE_STRING));
      value.setValueData(s);
      return value;
    }
    return null;
  }

  public static ValueMetaAndData attemptBooleanValueExtraction(String string)
      throws HopPluginException {
    // Try an Integer
    if ("TRUE".equalsIgnoreCase(string) || "FALSE".equalsIgnoreCase(string)) {
      ValueMetaAndData value = new ValueMetaAndData();
      value.setValueMeta(ValueMetaFactory.createValueMeta("Constant", IValueMeta.TYPE_BOOLEAN));
      value.setValueData(Boolean.valueOf("TRUE".equalsIgnoreCase(string)));
      return value;
    }
    return null;
  }

  public static ValueMetaAndData extractConstant(String string) throws HopPluginException {
    // Try a date
    //
    ValueMetaAndData value = attemptDateValueExtraction(string);
    if (value != null) {
      return value;
    }

    // String
    value = attemptStringValueExtraction(string);
    if (value != null) {
      return value;
    }

    // Boolean
    value = attemptBooleanValueExtraction(string);
    if (value != null) {
      return value;
    }

    // Integer
    value = attemptIntegerValueExtraction(string);
    if (value != null) {
      return value;
    }

    // Number
    value = attemptNumberValueExtraction(string);
    if (value != null) {
      return value;
    }

    // Number
    value = attemptBigNumberValueExtraction(string);
    if (value != null) {
      return value;
    }

    return null;
  }

  public static String stripQuoteTableAlias(String field, String tableAliasPrefix) {
    String result = stripTableAlias(field, tableAliasPrefix);
    if (result.equals(field)) {
      result = ThinUtil.stripQuotes(Const.trim(field), '"');
    }
    return result;
  }

  public static String stripTableAlias(String field, String tableAliasPrefix) {
    if (field.toUpperCase().startsWith((tableAliasPrefix + ".").toUpperCase())) {
      return ThinUtil.stripQuotesIfNoWhitespace(
          field.substring(tableAliasPrefix.length() + 1), '"');
    } else if (field.toUpperCase().startsWith(("\"" + tableAliasPrefix + "\".").toUpperCase())) {
      return ThinUtil.stripQuotesIfNoWhitespace(
          field.substring(tableAliasPrefix.length() + 3), '"');
    } else {
      return field;
    }
  }

  public static int skipChars(String sql, int index, char... skipChars) throws HopSqlException {
    if (index >= sql.length()) {
      return index;
    }

    // Skip over double quotes and quotes
    char c = sql.charAt(index);
    boolean count = false;
    for (char skipChar : skipChars) {
      if (c == skipChar) {
        char nextChar = skipChar;
        if (skipChar == '(') {
          nextChar = ')';
          count = true;
        }
        if (skipChar == '{') {
          nextChar = '}';
          count = true;
        }
        if (skipChar == '[') {
          nextChar = ']';
          count = true;
        }

        if (count) {
          index = findNextBracket(sql, skipChar, nextChar, index);
        } else {
          // Make sure to take escaping into account for single quotes
          //
          index = findNext(sql, nextChar, index, skipChar == '\'' || skipChar == '\"');
        }
        if (index >= sql.length()) {
          break;
        }
        c = sql.charAt(index);
      }
    }

    return index;
  }

  public static int findNext(String sql, char nextChar, int index) throws HopSqlException {
    return findNext(sql, nextChar, index, false);
  }

  public static int findNext(String sql, char nextChar, int index, boolean escape)
      throws HopSqlException {
    int quoteIndex = index;

    while (true) {

      index++;

      if (index >= sql.length()) {
        break;
      }
      boolean quoteMatch = sql.charAt(index) == nextChar;
      if (quoteMatch) {
        boolean escaped = escape && index + 1 < sql.length() && sql.charAt(index + 1) == nextChar;

        if (quoteMatch && !escaped) {
          break;
        }
        if (escaped) {
          index++; // skip one more
        }
      }
    }

    if (index + 1 > sql.length()) {
      throw new HopSqlException(
          "No closing "
              + nextChar
              + " found, starting at location "
              + quoteIndex
              + " in : ["
              + sql
              + "]");
    }
    index++;
    return index;
  }

  public static int findNextBracket(String sql, char skipChar, char nextChar, int index)
      throws HopSqlException {
    return findNextBracket(sql, skipChar, nextChar, index, false);
  }

  public static int findNextBracket(
      String sql, char skipChar, char nextChar, int index, boolean escape) throws HopSqlException {

    int counter = 0;
    for (int i = index; i < sql.length(); i++) {
      i = skipChars(sql, i, '\''); // skip quotes
      if (i >= sql.length()) {
        break;
      }
      char c = sql.charAt(i);
      if (c == skipChar) {
        counter++;
      }
      if (c == nextChar) {
        counter--;
      }
      if (counter == 0) {
        return i;
      }
    }

    throw new HopSqlException(
        "No closing "
            + nextChar
            + " bracket found for "
            + skipChar
            + " at location "
            + index
            + " in : ["
            + sql
            + "]");
  }

  public static String stripQuotes(String string, char... quoteChars) {
    StringBuilder builder = new StringBuilder(string);
    for (char quoteChar : quoteChars) {
      if (countQuotes(builder.toString(), quoteChar) == 2) {
        if (builder.length() > 0
            && builder.charAt(0) == quoteChar
            && builder.charAt(builder.length() - 1) == quoteChar) {
          // If there are quotes in between, don't do it...
          //
          builder.deleteCharAt(builder.length() - 1);
          builder.deleteCharAt(0);
        }
      }
    }
    return builder.toString();
  }

  /**
   * Strips leading and trailing quotes if no whitespace present, and no additional quotes in the
   * string.
   */
  public static String stripQuotesIfNoWhitespace(String string, char... quoteChars) {
    if (string.matches(".*\\s.*")) {
      // whitespace present
      return string;
    }
    return stripQuotes(string, quoteChars);
  }

  private static int countQuotes(String string, char quoteChar) {
    int count = 0;
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) == quoteChar) {
        count++;
      }
    }
    return count;
  }

  public static List<String> splitClause(String fieldClause, char splitChar, char... skipChars)
      throws HopSqlException {
    List<String> strings = new ArrayList<String>();
    int startIndex = 0;
    for (int index = 0; index < fieldClause.length(); index++) {
      index = ThinUtil.skipChars(fieldClause, index, skipChars);
      if (index >= fieldClause.length()) {
        strings.add(fieldClause.substring(startIndex));
        startIndex = -1;
        break;
      }
      // The CASE-WHEN-THEN-ELSE-END Hack // TODO: factor out
      //
      if (fieldClause.substring(index).toUpperCase().startsWith("CASE WHEN ")) {
        // If we see CASE-WHEN then we skip to END
        //
        index = skipOverClause(fieldClause, index, " END");
      }

      if (index < fieldClause.length() && fieldClause.charAt(index) == splitChar) {
        strings.add(fieldClause.substring(startIndex, index));
        while (index < fieldClause.length() && fieldClause.charAt(index) == splitChar) {
          index++;
        }
        startIndex = index;
        index--;
      }
    }
    if (startIndex >= 0) {
      strings.add(fieldClause.substring(startIndex));
    }

    return strings;
  }

  private static int skipOverClause(String fieldClause, int index, String clause)
      throws HopSqlException {
    while (index < fieldClause.length()) {
      index = skipChars(fieldClause, index, '\'', '"');
      if (fieldClause.substring(index).toUpperCase().startsWith(clause.toUpperCase())) {
        return index + clause.length();
      }
      index++;
    }
    return fieldClause.length();
  }

  public static String findClause(String sqlString, String startClause, String... endClauses)
      throws HopSqlException {
    return findClauseWithRest(sqlString, startClause, endClauses).getClause();
  }

  public static FoundClause findClauseWithRest(
      String sqlString, String startClause, String... endClauses) throws HopSqlException {
    if (StringUtils.isEmpty(sqlString)) {
      return new FoundClause(null, null);
    }

    String sql = sqlString.toUpperCase();

    int startIndex = 0;
    while (startIndex < sql.length()) {
      startIndex = ThinUtil.skipChars(sql, startIndex, '"', '\'');
      if (sql.substring(startIndex).startsWith(startClause.toUpperCase())) {
        break;
      }
      startIndex++;
    }

    if (startIndex < 0 || startIndex >= sql.length()) {
      return new FoundClause(null, sqlString);
    }

    startIndex += startClause.length() + 1;
    if (endClauses.length == 0) {
      return new FoundClause(sqlString.substring(startIndex), null);
    }

    // Index of first character of end clause
    int endIndex = sql.length();

    for (String endClause : endClauses) {

      int index = startIndex;
      while (index < endIndex) {
        index = ThinUtil.skipChars(sql, index, '"', '\'');

        // See if the end-clause is present at this location.
        //
        if (sql.substring(index).startsWith(endClause.toUpperCase())) {
          if (index < endIndex) {
            endIndex = index;
          }
        }
        index++;
      }
    }
    String foundClause = Const.trim(sqlString.substring(startIndex, endIndex));
    String rest = null;
    if (endIndex < sql.length()) {
      rest = Const.trim(sqlString.substring(endIndex));
      if (rest.length() == 0) {
        rest = null;
      }
    }
    return new FoundClause(foundClause, rest);
  }

  public static boolean like(String subject, String pattern) {
    return like(pattern).matcher(subject).matches();
  }

  public static Pattern like(String pattern) {
    if (pattern == null) {
      throw new IllegalArgumentException("Pattern cannot be null");
    }

    // Escape regex meta characters
    int len = pattern.length();
    if (len > 0) {
      StringBuilder sb = new StringBuilder(len * 2);
      for (int i = 0; i < len; i++) {
        char c = pattern.charAt(i);
        if ("[](){}.*+?$^|#\\".indexOf(c) != -1) {
          sb.append('\\');
        }
        sb.append(c);
      }
      pattern = sb.toString();
    }

    // Translate LIKE operators to REGEX
    pattern = pattern.replace("_", ".").replace("%", ".*?");

    return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  }

  /**
   * Attempts to resolve the reference to field from the serviceFields value meta list. Will search
   * with both the quoted and unquoted field.
   */
  public static String resolveFieldName(String field, IRowMeta serviceFields) {
    return getIValueMeta(field, serviceFields).map(IValueMeta::getName).orElse(field);
  }

  public static Optional<IValueMeta> getIValueMeta(String field, IRowMeta serviceFields) {
    IValueMeta valueMeta = serviceFields.searchValueMeta(field);
    if (valueMeta == null) {
      valueMeta = serviceFields.searchValueMeta(ThinUtil.stripQuotes(field, '"'));
    }
    return Optional.ofNullable(valueMeta);
  }

  /** Remove first and last quote if string contains double quote */
  public static String unQuote(String str) {
    if (!str.contains("\".\"") && str.length() > 0 && str.startsWith("\"") && str.endsWith("\"")) {
      return str.substring(1, str.length() - 1);
    }
    return str;
  }
}
