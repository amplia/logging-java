
package eu.arkitech.logback.amqp.consumer;


import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import eu.arkitech.logback.common.Callbacks;
import eu.arkitech.logback.common.ClassNewInstanceAction;
import eu.arkitech.logback.common.DefaultAppenderSink;
import eu.arkitech.logback.common.DefaultContextAwareCallbacks;
import eu.arkitech.logback.common.LoggingEventMutator;
import eu.arkitech.logback.common.LoggingEventPump;
import eu.arkitech.logback.common.Serializer;
import org.slf4j.LoggerFactory;


public class AmqpLoggingInjector
		extends DefaultAppenderSink
{
	public AmqpLoggingInjector ()
	{
		super ();
		this.callbacks = new DefaultContextAwareCallbacks (this);
	}
	
	public String getExchange ()
	{
		return (this.exchange);
	}
	
	public String getHost ()
	{
		return (this.host);
	}
	
	public LoggingEventMutator getMutator ()
	{
		return (this.mutator);
	}
	
	public String getPassword ()
	{
		return (this.password);
	}
	
	public Integer getPort ()
	{
		return (this.port);
	}
	
	public String getQueue ()
	{
		return (this.queue);
	}
	
	public String getRoutingKey ()
	{
		return (this.routingKey);
	}
	
	public Serializer getSerializer ()
	{
		return (this.serializer);
	}
	
	public String getUsername ()
	{
		return (this.username);
	}
	
	public String getVirtualHost ()
	{
		return (this.virtualHost);
	}
	
	public final boolean isDrained ()
	{
		synchronized (this) {
			return ((this.consumer == null) || this.consumer.isDrained ());
		}
	}
	
	public final boolean isRunning ()
	{
		synchronized (this) {
			return ((this.consumer != null) && this.consumer.isRunning ());
		}
	}
	
	public void setExchange (final String exchange)
	{
		this.exchange = exchange;
	}
	
	public void setHost (final String host)
	{
		this.host = host;
	}
	
	public void setMutator (final LoggingEventMutator mutator)
	{
		this.mutator = mutator;
	}
	
	public void setPassword (final String password)
	{
		this.password = password;
	}
	
	public void setPort (final Integer port)
	{
		this.port = port;
	}
	
	public void setQueue (final String queue)
	{
		this.queue = queue;
	}
	
	public void setRoutingKey (final String routingKey)
	{
		this.routingKey = routingKey;
	}
	
	public void setSerializer (final Serializer serializer)
	{
		this.serializer = serializer;
	}
	
	public void setUsername (final String username)
	{
		this.username = username;
	}
	
	public void setVirtualHost (final String virtualHost)
	{
		this.virtualHost = virtualHost;
	}
	
	public void start ()
	{
		if (this.isStarted ())
			return;
		this.reallyStart ();
		super.start ();
	}
	
	public void stop ()
	{
		if (!this.isStarted ())
			return;
		this.reallyStop ();
		super.stop ();
	}
	
	protected void append (final ILoggingEvent event)
	{
		final Logger logger = (Logger) LoggerFactory.getLogger (event.getLoggerName ());
		logger.callAppenders (event);
	}
	
	protected final boolean reallyStart ()
	{
		synchronized (this) {
			final boolean consumerStartSucceeded;
			final boolean pumpStartSucceeded;
			try {
				if ((this.consumer != null) || (this.pump != null))
					throw (new IllegalStateException ());
				this.consumer =
						new AmqpLoggingEventConsumer (
								this.host, this.port, this.virtualHost, this.username, this.password, this.exchange,
								this.queue, this.routingKey, this.mutator, this.serializer, this.callbacks);
				this.pump = new LoggingEventPump (this.consumer, this, this.callbacks);
				consumerStartSucceeded = this.consumer.start ();
				pumpStartSucceeded = this.pump.start ();
			} catch (final Error exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while starting; aborting!");
				try {
					this.reallyStop ();
				} catch (final Error exception1) {}
				throw (exception);
			}
			return (consumerStartSucceeded && pumpStartSucceeded);
		}
	}
	
	protected final boolean reallyStop ()
	{
		synchronized (this) {
			boolean consumerStopSucceeded = false;
			final boolean pumpStopSucceeded = false;
			try {
				if (this.consumer != null)
					this.consumer.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the consumer; ignoring");
				this.consumer = null;
			}
			try {
				if (this.pump != null)
					this.pump.requestStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the pump; ignoring");
				this.pump = null;
			}
			try {
				if (this.consumer != null)
					consumerStopSucceeded = this.consumer.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the consumer; ignoring");
			} finally {
				this.consumer = null;
			}
			try {
				if (this.pump != null)
					consumerStopSucceeded = this.pump.awaitStop ();
			} catch (final Error exception) {
				this.callbacks.handleException (
						exception, "amqp consumer encountered an error while stopping the pump; ignoring");
			} finally {
				this.pump = null;
			}
			return (consumerStopSucceeded && pumpStopSucceeded);
		}
	}
	
	protected final Callbacks callbacks;
	protected String exchange;
	protected String host;
	protected LoggingEventMutator mutator;
	protected String password;
	protected Integer port;
	protected String queue;
	protected String routingKey;
	protected Serializer serializer;
	protected String username;
	protected String virtualHost;
	private AmqpLoggingEventConsumer consumer;
	private LoggingEventPump pump;
	
	public static final class CreateAction
			extends ClassNewInstanceAction<AmqpLoggingInjector>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<AmqpLoggingInjector> collector, final boolean autoStart)
		{
			super (AmqpLoggingInjector.class, collector, autoStart);
		}
		
		public static boolean defaultAutoStart = true;
		public static List<AmqpLoggingInjector> defaultCollector = null;
	}
}
