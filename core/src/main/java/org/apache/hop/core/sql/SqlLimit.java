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

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopSqlException;

public class SqlLimit {

  private String limitClause;

  private int limit;
  private int offset;

  public SqlLimit(String limitClause) throws HopException {
    this.limitClause = limitClause;

    parse();
  }

  /** @return The limit of rows to return */
  public int getLimit() {
    return limit;
  }

  /** @param limit The limit of rows to return */
  public void setLimit(int limit) {
    this.limit = limit;
  }

  /** @return The offset of the rows to return */
  public int getOffset() {
    return offset;
  }

  /** @param offset The offset of the rows to return */
  public void setOffset(int offset) {
    this.offset = offset;
  }

  private void parse() throws HopException {
    if (StringUtils.isEmpty(limitClause)) {
      return;
    }

    limitClause = limitClause.replaceAll("\\s+", " ");

    if (limitClause.contains(",")) {
      String[] limitSplit = limitClause.split(",");
      if (limitSplit.length == 2) {
        offset = Integer.valueOf(limitSplit[0].trim());
        limit = Integer.valueOf(limitSplit[1].trim());
      }
      return;
    }

    if (limitClause.toUpperCase().contains("OFFSET")) {
      String[] limitSplit = limitClause.split(" ");
      if (limitSplit.length == 3) {
        offset = Integer.valueOf(limitSplit[2].trim());
        limit = Integer.valueOf(limitSplit[0].trim());
      }
      return;
    }

    try {
      limit = Integer.valueOf(limitClause.trim());
      offset = 0;
    } catch (NumberFormatException nfe) {
      throw new HopException("Invalid limit parameter in : " + limitClause);
    }
  }
}
