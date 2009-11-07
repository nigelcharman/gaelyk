package groovyx.gaelyk;


import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.servlet.AbstractHttpServlet;
import groovy.servlet.ServletBinding;
import groovy.servlet.ServletCategory;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.runtime.GroovyCategorySupport;

@SuppressWarnings("serial")
public class TestServlet extends  AbstractHttpServlet {

  /**
   * This servlet will run Groovy scripts as Groovlets.  Groovlets are scripts
   * with these objects implicit in their scope:
   *
   * <ul>
   *  <li>request - the HttpServletRequest</li>
   *  <li>response - the HttpServletResponse</li>
   *  <li>application - the ServletContext associated with the servlet</li>
   *  <li>session - the HttpSession associated with the HttpServletRequest</li>
   *  <li>out - the PrintWriter associated with the ServletRequest</li>
   * </ul>
   *
   * <p>Your script sources can be placed either in your web application's normal
   * web root (allows for subdirectories) or in /WEB-INF/groovy/* (also allows
   * subdirectories).
   *
   * <p>To make your web application more groovy, you must add the GroovyServlet
   * to your application's web.xml configuration using any mapping you like, so
   * long as it follows the pattern *.* (more on this below).  Here is the
   * web.xml entry:
   *
   * <pre>
   *    &lt;servlet>
   *      &lt;servlet-name>Groovy&lt;/servlet-name>
   *      &lt;servlet-class>groovy.servlet.GroovyServlet&lt;/servlet-class>
   *    &lt;/servlet>
   *
   *    &lt;servlet-mapping>
   *      &lt;servlet-name>Groovy&lt;/servlet-name>
   *      &lt;url-pattern>*.groovy&lt;/url-pattern>
   *      &lt;url-pattern>*.gdo&lt;/url-pattern>
   *    &lt;/servlet-mapping>
   * </pre>
   *
   * <p>The URL pattern does not require the "*.groovy" mapping.  You can, for
   * example, make it more Struts-like but groovy by making your mapping "*.gdo".
   *
   * @author Sam Pullara
   * @author Mark Turansky (markturansky at hotmail.com)
   * @author Guillaume Laforge
   * @author Christian Stein
   * @author Marcel Overdijk
   *
   * @see groovy.servlet.ServletBinding
   */


      /**
       * The script engine executing the Groovy scripts for this servlet
       */
      private GroovyScriptEngine gse;

      /**
       * Initialize the GroovyServlet.
       *
       * @throws ServletException
       *  if this method encountered difficulties
       */
      public void init(ServletConfig config) throws ServletException {
          super.init(config);

          // Set up the scripting engine
          gse = createGroovyScriptEngine();

          servletContext.log("Groovy servlet initialized on " + gse + ".");
      }

      /**
       * Handle web requests to the GroovyServlet
       */
      public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

          // Get the script path from the request - include aware (GROOVY-815)
          final String scriptUri = getScriptUri(request);

          // Set it to HTML by default
          response.setContentType("text/html; charset="+encoding);

          // Set up the script context
          final ServletBinding binding = new ServletBinding(request, response, servletContext);
          setVariables(binding);

          // Run the script
          try {
              Closure closure = new Closure(gse) {

                  public Object call() {
                      try {
                          return ((GroovyScriptEngine) getDelegate()).run(scriptUri, binding);
                      } catch (ResourceException e) {
                          throw new RuntimeException(e);
                      } catch (ScriptException e) {
                          throw new RuntimeException(e);
                      }
//                      catch (Throwable t) {
//                        System.err.println("Some other exception");
//                        t.printStackTrace(System.err);
//                        return null;
//                      }
                  }

              };
              GroovyCategorySupport.use(ServletCategory.class, closure);
              /*
               * Set reponse code 200.
               */
              response.setStatus(HttpServletResponse.SC_OK);
          } catch (RuntimeException runtimeException) {
              StringBuffer error = new StringBuffer("GroovyServlet Error: ");
              error.append(" script: '");
              error.append(scriptUri);
              error.append("': ");
              runtimeException.printStackTrace( System.err );
              Throwable e = runtimeException.getCause();

              if (e == null ) {
                decodeRuntimeException(runtimeException, response, scriptUri);
                return;
              }
              /*
               * Null cause?!
               */
              if (e == null) {
                  error.append(" Script processing failed.");
                  error.append(runtimeException.getMessage());
                  if (runtimeException.getStackTrace().length > 0)
                      error.append(runtimeException.getStackTrace()[0].toString());
                  servletContext.log(error.toString());
                  System.err.println(error.toString());
                  runtimeException.printStackTrace(System.err);
                  response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.toString());
                  return;
              }
              /*
               * Resource not found.
               */
              if (e instanceof ResourceException) {
                  error.append(" Script not found, sending 404.");
                  servletContext.log(error.toString());
                  System.err.println(error.toString());
                  response.sendError(HttpServletResponse.SC_NOT_FOUND);
                  return;
              }
              /*
               * Other internal error. Perhaps syntax?!
               */
              servletContext.log("An error occurred processing the request", runtimeException);
              error.append(e.getMessage());
              if (e.getStackTrace().length > 0)
                  error.append(e.getStackTrace()[0].toString());
              servletContext.log(e.toString());
              System.err.println(e.toString());
              runtimeException.printStackTrace(System.err);
              response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
          } finally {
              /*
               * Finally, flush the response buffer.
               */
              response.flushBuffer();
              // servletContext.log("Flushed response buffer.");
          }
      }

      private void decodeRuntimeException(
          RuntimeException runtimeException, HttpServletResponse response,
          String scriptName) throws IOException {

        runtimeException.printStackTrace(System.err);
        System.err.println("script name is " + scriptName);
        String cname = scriptName.substring(1);
        if ( cname.endsWith(".groovy") )
          cname = cname.substring(0, cname.length()-7);

        for( StackTraceElement ste : runtimeException.getStackTrace() ) {
          System.err.println( "class to compare " + ste.getClassName() + " vs " + cname );
          if ( cname.equals(ste.getClassName() ) ) {
            writeErrorMessage(runtimeException, response, scriptName, ste);
            return;
          }
        }

        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, runtimeException.getMessage());
      }

			private void writeErrorMessage(RuntimeException runtimeException,
					HttpServletResponse response, String scriptName, StackTraceElement ste)
					throws IOException {
				StringBuffer sb = new StringBuffer();
				sb.append( "There was an error " );
				sb.append(runtimeException.getMessage());
				sb.append(" on line: ");
				sb.append(ste.getLineNumber());
				sb.append("\n\n");

				try {
				  URLConnection conn = getResourceConnection(scriptName);
				  BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream() ));
				  int lineCount = 1;
				  int minLine = ste.getLineNumber() - 5;
				  int maxLine = ste.getLineNumber() + 5;
				  String line;
				  while ( (line = br.readLine()) != null ) {
				  	sb.append( String.format("%4d ", lineCount ) );
				    if ( lineCount == ste.getLineNumber() )
				      sb.append( ">  ");
				    else
				      sb.append(":  ");
				    if ( lineCount >= minLine && lineCount <= maxLine  ) {
				      sb.append( line ); // should be escaped
				    }
				    sb.append("\n");
				    lineCount ++;
				    if ( lineCount > maxLine )
				      break;
				  }
				} catch (ResourceException e) {
				  e.printStackTrace();
				}
				
				sb.append( "\n\nAbbreviated Stack Trace:\n\n" );
				
				sb.append( runtimeException.getMessage() );
				sb.append( "\n\n" );
				
				for( StackTraceElement st : runtimeException.getStackTrace() ) {
					if ( !st.getClassName().startsWith("org.codehaus.groovy." ) && !st.getClassName().startsWith("groovy.") &&
							 !st.getClassName().startsWith("groovyx.") && !st.getClassName().startsWith("java.") &&
							 !st.getClassName().startsWith("javax.") && !st.getClassName().startsWith("sun.") &&
							 !st.getClassName().startsWith("org.mortbay") && !st.getClassName().startsWith("com.google.")) {
						sb.append( "  " );
					  sb.append( st.toString() );
					  sb.append( "\n" );
					}
				}
				
				sb.append( "\n\n" );

				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				     sb.toString()  );
			}

      /**
       * Hook method to setup the GroovyScriptEngine to use.<br/>
       * Subclasses may override this method to provide a custom
       * engine.
       */
      protected GroovyScriptEngine createGroovyScriptEngine(){
          return new GroovyScriptEngine(this);
      }
  }

