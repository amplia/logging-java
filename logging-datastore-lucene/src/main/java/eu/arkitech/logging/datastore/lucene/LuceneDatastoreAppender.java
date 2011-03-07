
package eu.arkitech.logging.datastore.lucene;


import java.io.File;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.AppenderNewInstanceAction;
import eu.arkitech.logback.common.DefaultSerializerAppender;


public class LuceneDatastoreAppender
		extends DefaultSerializerAppender
{
	public LuceneDatastoreAppender ()
	{
		super ();
	}
	
	public String getEnvironmentPath ()
	{
		return (this.environmentPath);
	}
	
	public Long getSyncTimeout ()
	{
		return (this.syncTimeout);
	}
	
	@Override
	public final boolean isDrained ()
	{
		return (true);
	}
	
	public void setEnvironmentPath (final String environmentPath)
	{
		this.environmentPath = environmentPath;
	}
	
	public void setSyncTimeout (final Long syncTimeout)
	{
		this.syncTimeout = syncTimeout;
	}
	
	protected LuceneDatastoreConfiguration buildConfiguration ()
	{
		return (new LuceneDatastoreConfiguration ((this.environmentPath != null) ? new File (this.environmentPath) : null, false, true, this.syncTimeout, this.serializer, this.mutator, this.mutator, this.callbacks, null));
	}
	
	@Override
	protected final void reallyAppend (final ILoggingEvent event)
	{
		this.datastore.store (event);
	}
	
	@Override
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			try {
				if (this.datastore != null)
					throw (new IllegalStateException ());
				this.datastore = new LuceneDatastore (this.buildConfiguration ());
				final boolean succeeded = this.datastore.open ();
				if (!succeeded) {
					this.reallyStop ();
					return (false);
				}
				return (succeeded);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore appender encountered an unknown error while starting; aborting!");
				this.reallyStop ();
				return (false);
			}
		}
	}
	
	@Override
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			try {
				if (this.datastore != null) {
					this.datastore.close ();
					this.datastore = null;
				}
				return (true);
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "bdb datastore appender encountered an unknown error while stopping; ignoring");
				return (false);
			} finally {
				this.datastore = null;
			}
		}
	}
	
	protected String environmentPath;
	protected Long syncTimeout;
	private LuceneDatastore datastore;
	
	public static final class CreateAction
			extends AppenderNewInstanceAction<LuceneDatastoreAppender>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<LuceneDatastoreAppender> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (LuceneDatastoreAppender.class, collector, autoRegister, autoStart);
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<LuceneDatastoreAppender> defaultCollector = null;
	}
}