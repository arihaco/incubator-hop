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

package org.apache.hop.pipeline.transforms.ldapinput;

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.LogChannelInterface;
import org.apache.hop.core.variables.iVariables;
import org.apache.hop.pipeline.transforms.ldapinput.store.CustomSocketFactory;

import javax.naming.NamingException;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class LdapTlsProtocol extends LdapSslProtocol {
  private StartTlsResponse startTlsResponse;

  public LdapTlsProtocol( LogChannelInterface log, iVariables variables, LdapMeta meta,
                          Collection<String> binaryAttributes ) {
    super( log, variables, meta, binaryAttributes );
  }

  @Override
  protected String getConnectionPrefix() {
    return "ldap://";
  }

  public static String getName() {
    return "LDAP TLS";
  }

  @Override
  protected void doConnect( String username, String password ) throws HopException {
    super.doConnect( username, password );
    StartTlsRequest tlsRequest = new StartTlsRequest();
    try {
      this.startTlsResponse = (StartTlsResponse) getCtx().extendedOperation( tlsRequest );
      /* Starting TLS */
      this.startTlsResponse.negotiate( CustomSocketFactory.getDefault() );
    } catch ( NamingException e ) {
      throw new HopException( e );
    } catch ( IOException e ) {
      throw new HopException( e );
    }
  }

  @Override
  protected void configureSslEnvironment( Map<String, String> env ) {
    // noop
  }

  @Override
  public void close() throws HopException {
    if ( startTlsResponse != null ) {
      try {
        startTlsResponse.close();
      } catch ( IOException e ) {
        throw new HopException( e );
      } finally {
        startTlsResponse = null;
      }
    }
    super.close();
  }
}
