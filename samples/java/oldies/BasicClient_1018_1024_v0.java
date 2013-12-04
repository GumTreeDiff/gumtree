package com.google.code.facebookapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Basic client taking care of rest call mechanics (signing, etc) to facebook. Little api knowledge, no response interpretation is planned.
 */
public class BasicClient {

	protected static Log log = LogFactory.getLog( BasicClient.class );

	protected static final String CRLF = "\r\n";
	protected static final String PREF = "--";
	protected static final int UPLOAD_BUFFER_SIZE = 1024;

	protected URL serverUrl;
	protected URL serverUrlHttps;
	private CommunicationStrategy communicationStrategy;

	protected final String apiKey;
	protected final String secret;
	protected boolean sessionSecret;

	protected String sessionKey;

	protected boolean batchMode;
	protected List<BatchQuery> queries;
	protected String permissionsApiKey;

	public boolean isBatchMode() {
		return batchMode;
	}

	protected List<BatchQuery> getQueries() {
		return queries;
	}

	protected BasicClient( String apiKey, String secret, boolean sessionSecret ) {
		this( apiKey, secret, sessionSecret, null );
	}

	protected BasicClient( String apiKey, String secret, boolean sessionSecret, String sessionKey ) {
		this( null, null, apiKey, secret, sessionSecret, sessionKey, new DefaultCommunicationStrategy() );
	}

	protected BasicClient( URL serverUrl, URL serverUrlHttps, String apiKey, String secret, boolean sessionSecret, String sessionKey,
			CommunicationStrategy communicationStrategy ) {
		this.sessionKey = sessionKey;
		this.apiKey = apiKey;
		this.secret = secret;
		this.sessionSecret = sessionSecret || secret.endsWith( "__" );
		this.serverUrl = ( null != serverUrl ) ? serverUrl : FacebookApiUrls.getDefaultServerUrl();
		this.serverUrlHttps = ( null != serverUrlHttps ) ? serverUrlHttps : FacebookApiUrls.getDefaultHttpsServerUrl();

		this.communicationStrategy = communicationStrategy;
		this.batchMode = false;
		this.queries = new ArrayList<BatchQuery>();
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getSecret() {
		return secret;
	}

	public boolean isSessionSecret() {
		return sessionSecret;
	}

	public void beginPermissionsMode( String apiKey ) {
		this.permissionsApiKey = apiKey;
	}

	public void endPermissionsMode() {
		this.permissionsApiKey = null;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey( String cacheSessionKey ) {
		this.sessionKey = cacheSessionKey;
	}


	public void setServerUrl( String newUrl ) {
		String base = newUrl;
		if ( base.startsWith( "http" ) ) {
			base = base.substring( base.indexOf( "://" ) + 3 );
		}
		try {
			String url = "http://" + base;
			serverUrl = new URL( url );
		}
		catch ( MalformedURLException ex ) {
			throw BasicClientHelper.runtimeException( ex );
		}
	}

	public void setServerUrlHttps( String newUrl ) {
		String base = newUrl;
		if ( base.startsWith( "https" ) ) {
			base = base.substring( base.indexOf( "://" ) + 3 );
		}
		try {
			String url = "https://" + base;
			serverUrlHttps = new URL( url );
		}
		catch ( MalformedURLException ex ) {
			throw BasicClientHelper.runtimeException( ex );
		}
	}

	/**
	 * Call the specified method, with the given parameters, and return a DOM tree with the results.
	 * 
	 * @param method
	 *            the fieldName of the method
	 * @param paramPairs
	 *            a list of arguments to the method
	 * @throws Exception
	 *             with a description of any errors given to us by the server.
	 */
	protected String callMethod( String responseFormat, IFacebookMethod method, Pair<String,CharSequence>... paramPairs ) throws FacebookException {
		return callMethod( responseFormat, method, Arrays.asList( paramPairs ), null, null );
	}

	/**
	 * Call the specified method, with the given parameters, and return a DOM tree with the results.
	 * 
	 * @param method
	 *            the fieldName of the method
	 * @param paramPairs
	 *            a list of arguments to the method
	 * @throws Exception
	 *             with a description of any errors given to us by the server.
	 */
	public String callMethod( String responseFormat, IFacebookMethod method, Collection<Pair<String,CharSequence>> paramPairs ) throws FacebookException {
		return callMethod( responseFormat, method, paramPairs, null, null );
	}

	protected SortedMap<String,String> prepareRequestParams( String responseFormat, IFacebookMethod method, Collection<Pair<String,CharSequence>> paramPairs )
			throws FacebookException {
		SortedMap<String,String> params = new TreeMap<String,String>();

		if ( permissionsApiKey != null ) {
			params.put( "call_as_apikey", permissionsApiKey );
		}

		if ( sessionSecret ) {
			params.put( "ss", "1" );
		}

		params.put( "method", method.methodName() );
		params.put( "api_key", apiKey );
		params.put( "v", IFacebookRestClient.TARGET_API_VERSION );

		if ( responseFormat != null ) {
			params.put( "format", responseFormat );
		}

		params.put( "call_id", Long.toString( System.currentTimeMillis() ) );
		boolean includeSession = !method.requiresNoSession() && sessionKey != null;
		if ( includeSession ) {
			params.put( "session_key", sessionKey );
		}

		for ( Pair<String,CharSequence> p : paramPairs ) {
			final String key = p.first;
			if ( !"sig".equals( "sig" ) ) {
				CharSequence oldVal = params.put( key, BasicClientHelper.toString( p.second ) );
				if ( oldVal != null ) {
					log.warn( String.format( "For parameter %s, overwrote old value %s with new value %s.", key, oldVal, p.second ) );
				}
			}
		}

		String signature = FacebookSignatureUtil.generateSignature( params, secret );
		params.put( "sig", signature );

		return params;
	}

	public String callMethod( String responseFormat, IFacebookMethod method, Collection<Pair<String,CharSequence>> paramPairs, String fileName, InputStream fileStream )
			throws FacebookException {
		SortedMap<String,String> params = prepareRequestParams( responseFormat, method, paramPairs );
		final boolean fileCall = fileName != null || fileStream != null;
		if ( batchMode ) {
			if ( fileCall ) {
				throw new FacebookException( ErrorCode.GEN_INVALID_PARAMETER, "File upload API calls cannot be batched:  " + method.methodName() );
			}
			// if we are running in batch mode, don't actually execute the query now, just add it to the list
			boolean addToBatch = true;
			// FIXME what the heck is going on here??
			if ( method.methodName().equals( FacebookMethod.USERS_GET_LOGGED_IN_USER.methodName() ) ) {
				Exception trace = new Exception();
				StackTraceElement[] traceElems = trace.getStackTrace();
				int index = 0;
				for ( StackTraceElement elem : traceElems ) {
					if ( elem.getMethodName().indexOf( "_" ) != -1 ) {
						StackTraceElement caller = traceElems[index + 1];
						final boolean calledFromSelf = caller.getClassName().equals( BasicClient.class.getName() );
						final boolean calledFromAuth = caller.getMethodName().startsWith( "auth_" );
						if ( calledFromSelf && !calledFromAuth ) {
							addToBatch = false;
						}
						break;
					}
					index++ ;
				}
			}
			if ( addToBatch ) {
				queries.add( new BatchQuery( method, params ) );
				// should return null be here or below
				// return null;
			}
			return null;
		}

		try {
			// FIXME when to use https?
			// when called from desktop, some methods require https
			boolean doHttps = FacebookMethod.AUTH_GET_SESSION.equals( method ) && "true".equals( params.get( "generate_session_secret" ) );
			URL url = ( doHttps ) ? serverUrlHttps : serverUrl;
			if ( fileCall ) {
				if ( log.isDebugEnabled() ) {
					log.debug( method.methodName() + ": POST-FILE: " + url.toString() + ": " + params );
				}
				return communicationStrategy.postRequest( url, params, fileName, fileStream );
			} else {
				if ( log.isDebugEnabled() ) {
					log.debug( method.methodName() + ": POST: " + url.toString() + ": " + params );
				}
				return communicationStrategy.postRequest( url, params );
			}
		}
		catch ( IOException ex ) {
			throw BasicClientHelper.runtimeException( ex );
		}
	}

	public void beginBatch() {
		batchMode = true;
		queries = new ArrayList<BatchQuery>();
	}

	/**
	 * Returns a list of String raw responses which will be further broken down by the adapters into the actual individual responses. One string is returned per 20
	 * methods in the batch.
	 */
	public List<String> executeBatch( boolean serial ) throws FacebookException {
		batchMode = false;
		final List<BatchQuery> q = queries;
		queries = null;

		final int BATCH_LIMIT = 20;
		final List<String> result = new ArrayList<String>();
		List<BatchQuery> buffer = new ArrayList<BatchQuery>( BATCH_LIMIT );
		while ( !q.isEmpty() ) {
			buffer.add( q.remove( 0 ) );
			boolean batchFull = buffer.size() >= BATCH_LIMIT;
			if ( batchFull || ( q.isEmpty() ) ) {
				List<String> batchRawResponse = batch_run( encodeMethods( buffer ), serial );
				result.addAll( batchRawResponse );
				if ( batchFull ) {
					buffer = new ArrayList<BatchQuery>( BATCH_LIMIT );
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	protected List<String> batch_run( String methods, boolean serial ) throws FacebookException {
		final String call = callMethod( "json", FacebookMethod.BATCH_RUN, Pairs.newPair( "method_feed", methods ), Pairs.newPair10( "serial_only", serial ) );
		try {
			JSONArray arr = new JSONArray( call );
			List<String> out = new ArrayList<String>();
			int l = arr.length();
			for ( int i = 0; i < l; i++ ) {
				out.add( arr.getString( i ) );
			}
			return out;
		}
		catch ( JSONException ex ) {
			throw BasicClientHelper.runtimeException( ex );
		}
	}

	protected static String encodeMethods( List<BatchQuery> queryList ) throws FacebookException {
		JSONArray result = new JSONArray();
		for ( BatchQuery query : queryList ) {
			if ( query.getMethod().takesFile() ) {
				throw new FacebookException( ErrorCode.GEN_INVALID_PARAMETER, "File upload API calls cannot be batched:  " + query.getMethod().methodName() );
			}
			result.put( BasicClientHelper.delimit( query.getParams().entrySet(), "&", "=", true ) );
		}
		return result.toString();
	}

}
