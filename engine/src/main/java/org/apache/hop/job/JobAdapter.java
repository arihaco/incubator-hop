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

package org.apache.hop.job;

import org.apache.hop.core.exception.HopException;

/**
 * Utility class to allow only certain methods of IJobListener to be overridden.
 *
 * @author Marc
 */

public class JobAdapter implements IJobListener {

  @Override
  public void jobFinished( Job job ) throws HopException {
    // NoOp

  }

  @Override
  public void jobStarted( Job job ) throws HopException {
    // NoOp

  }

}
