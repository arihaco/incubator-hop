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

import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.plugins.BasePluginType;
import org.apache.hop.core.plugins.IPluginType;
import org.apache.hop.core.plugins.PluginAnnotationType;
import org.apache.hop.core.plugins.PluginMainClassType;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * This class represents the logging plugin type.
 *
 * @author matt
 */
@PluginMainClassType( ILogTablePlugin.class )
@PluginAnnotationType( LogTablePlugin.class )
public class LogTablePluginType extends BasePluginType implements IPluginType {

  private static LogTablePluginType logTablePluginType;

  private LogTablePluginType() {
    super( LogTablePlugin.class, "LOGTABLE", "Log table plugin" );
    populateFolders( "logtable" );
  }

  public static LogTablePluginType getInstance() {
    if ( logTablePluginType == null ) {
      logTablePluginType = new LogTablePluginType();
    }
    return logTablePluginType;
  }

  /**
   * Scan & register internal logging plugins
   */
  protected void registerNatives() throws HopPluginException {
    // No native
  }

  @Override
  protected String extractCategory( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractDesc( Annotation annotation ) {
    return ( (LogTablePlugin) annotation ).description();
  }

  @Override
  protected String extractID( Annotation annotation ) {
    String id = ( (LogTablePlugin) annotation ).id();

    LogChannel.GENERAL.logBasic( "Logging plugin type found with ID: " + id );

    return id;
  }

  @Override
  protected String extractName( Annotation annotation ) {
    return ( (LogTablePlugin) annotation ).name();
  }

  @Override
  protected String extractImageFile( Annotation annotation ) {
    return null;
  }

  @Override
  protected boolean extractSeparateClassLoader( Annotation annotation ) {
    return false;
  }

  @Override
  protected String extractI18nPackageName( Annotation annotation ) {
    return null;
  }

  @Override
  protected void addExtraClasses( Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation ) {
  }

  @Override
  protected String extractDocumentationUrl( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractCasesUrl( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractForumUrl( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractSuggestion( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractClassLoaderGroup( Annotation annotation ) {
    return ( (LogTablePlugin) annotation ).classLoaderGroup();
  }
}
