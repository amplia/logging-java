
package eu.arkitech.logback.amqp.appender;


import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventAppenderSink;
import eu.arkitech.logback.amqp.accessors.AmqpLoggingEventRouter;
import eu.arkitech.logback.common.DefaultLoggingEventMutator;


public class AmqpAppender
		extends AmqpLoggingEventAppenderSink
		implements
			AmqpLoggingEventRouter
{
	public AmqpAppender ()
	{
		super ();
		this.exchangeLayout = new PatternLayout ();
		this.routingKeyLayout = new PatternLayout ();
		this.exchangeLayout.setPattern (AmqpAppender.defaultExchangeKeyPattern);
		this.routingKeyLayout.setPattern (AmqpAppender.defaultRoutingKeyPattern);
		this.mutator = new DefaultLoggingEventMutator ();
	}
	
	public String generateExchange (final ILoggingEvent event)
	{
		return (this.exchangeLayout.doLayout (event));
	}
	
	public String generateRoutingKey (final ILoggingEvent event)
	{
		return (this.routingKeyLayout.doLayout (event));
	}
	
	public String getExchangePattern ()
	{
		return (this.exchangeLayout.getPattern ());
	}
	
	public String getRoutingKeyPattern ()
	{
		return (this.routingKeyLayout.getPattern ());
	}
	
	public final boolean isDrained ()
	{
		synchronized (this) {
			return ((this.publisher == null) || this.publisher.isDrained ());
		}
	}
	
	public final boolean isRunning ()
	{
		synchronized (this) {
			return ((this.publisher != null) && this.publisher.isRunning ());
		}
	}
	
	public void setContext (final Context context)
	{
		super.setContext (context);
		this.exchangeLayout.setContext (context);
		this.routingKeyLayout.setContext (context);
	}
	
	public void setExchangePattern (final String pattern)
	{
		this.exchangeLayout.setPattern (pattern);
	}
	
	public void setRoutingKeyPattern (final String pattern)
	{
		this.routingKeyLayout.setPattern (pattern);
	}
	
	protected final void reallyAppend (final ILoggingEvent event)
			throws Throwable
	{
		this.publisher.push (event);
	}
	
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean publisherStartSucceeded;
			try {
				if (this.publisher != null)
					throw (new IllegalStateException ());
				this.publisher =
						new AmqpLoggingEventPublisher (
								this.host, this.port, this.virtualHost, this.username, this.password, this, this.serializer,
								this.mutator, this.callbacks);
				publisherStartSucceeded = this.publisher.start ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp appender encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			if (publisherStartSucceeded) {
				this.exchangeLayout.start ();
				this.routingKeyLayout.start ();
			}
			return (publisherStartSucceeded);
		}
	}
	
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean publisherStopSucceeded = false;
			try {
				if (this.publisher != null)
					this.publisher.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp appender encountered an error while stopping the publisher; ignoring");
				this.publisher = null;
			}
			try {
				if (this.publisher != null)
					publisherStopSucceeded = this.publisher.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp appender encountered an error while stopping the publisher; ignoring");
			} finally {
				this.publisher = null;
			}
			if (publisherStopSucceeded) {
				this.exchangeLayout.stop ();
				this.routingKeyLayout.stop ();
			}
			return (publisherStopSucceeded);
		}
	}
	
	protected PatternLayout exchangeLayout;
	protected PatternLayout routingKeyLayout;
	private AmqpLoggingEventPublisher publisher;
	
	public static final String defaultExchangeKeyPattern = "logging%nopex";
	public static final String defaultRoutingKeyPattern = "logging.event.%level%nopex";
}
