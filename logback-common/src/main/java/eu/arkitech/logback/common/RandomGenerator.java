
package eu.arkitech.logback.common;


import java.util.List;
import java.util.Random;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.LoggerFactory;


public class RandomGenerator
		extends ContextAwareBase
{
	public RandomGenerator ()
	{
		this (RandomGenerator.class.getName (), null);
	}
	
	public RandomGenerator (final Object source)
	{
		this (source.getClass ().getName (), null);
	}
	
	public RandomGenerator (final String fqdn, final Logger logger)
	{
		super ();
		this.random = new Random ();
		this.fqdn = fqdn;
		this.logger = logger;
		this.count = RandomGenerator.defaultCount;
		this.interval = RandomGenerator.defaultInterval;
	}
	
	public void append ()
	{
		final ILoggingEvent event = this.generate ();
		if (event != null)
			((Logger) LoggerFactory.getLogger (event.getLoggerName ())).callAppenders (event);
	}
	
	public ILoggingEvent generate ()
	{
		final String fqdn = this.getFqdn ();
		if (fqdn == null)
			return (null);
		final Logger logger = this.getLogger ();
		if (logger == null)
			return (null);
		final float levelDice = this.random.nextFloat ();
		final float exceptionDice = this.random.nextFloat ();
		final float exception2Dice = this.random.nextFloat ();
		final String message = UUID.randomUUID ().toString ();
		final Level level;
		if (levelDice < 0.1)
			level = Level.TRACE;
		else if (levelDice < 0.3)
			level = Level.DEBUG;
		else if (levelDice < 0.7)
			level = Level.INFO;
		else if (levelDice < 0.85)
			level = Level.WARN;
		else if (levelDice < 1.0)
			level = Level.ERROR;
		else
			throw (new AssertionError ());
		final Throwable exception;
		if (exceptionDice < 0.1)
			if (exception2Dice < 0.2)
				exception = new Throwable (UUID.randomUUID ().toString (), new Throwable (UUID.randomUUID ().toString ()));
			else
				exception = new Throwable (UUID.randomUUID ().toString ());
		else
			exception = null;
		return (new LoggingEvent (fqdn, logger, level, message, exception, null));
	}
	
	public long getCount ()
	{
		return (this.count);
	}
	
	public String getFqdn ()
	{
		return (this.fqdn);
	}
	
	public long getInterval ()
	{
		return (this.interval);
	}
	
	public Logger getLogger ()
	{
		final Logger logger_ = this.logger;
		final Logger logger;
		if (logger_ != null)
			logger = logger_;
		else {
			final String fqdn = this.getFqdn ();
			if (fqdn != null)
				try {
					logger = (Logger) LoggerFactory.getLogger (fqdn);
				} catch (final ClassCastException exception) {
					return (null);
				}
			else
				logger = null;
		}
		return (logger);
	}
	
	public void setCount (final long count)
	{
		this.count = count;
	}
	
	public void setFqdn (final String fqdn)
	{
		this.fqdn = fqdn;
	}
	
	public void setInterval (final long interval)
	{
		this.interval = interval;
	}
	
	public void setLogger (final Logger logger)
	{
		this.logger = logger;
	}
	
	public Thread start ()
	{
		return (this.start (this.count, this.interval));
	}
	
	public Thread start (final long count, final long interval)
	{
		final Thread thread = new Thread () {
			@Override
			public void run ()
			{
				for (long index = 0; index < count; index++) {
					RandomGenerator.this.append ();
					try {
						Thread.sleep (interval);
					} catch (final InterruptedException exception) {
						break;
					}
				}
			}
		};
		thread.setName (String.format ("%s@%x@%x", this.getClass ().getName (), System.identityHashCode (this), System.identityHashCode (thread)));
		thread.setDaemon (true);
		thread.start ();
		return (thread);
	}
	
	protected long count;
	protected String fqdn;
	protected long interval;
	protected Logger logger;
	protected final Random random;
	
	public static final long defaultCount = 10 * 60 * 2;
	public static final long defaultInterval = 500;
	
	public static final class CreateAction
			extends ClassNewInstanceAction<RandomGenerator>
	{
		public CreateAction ()
		{
			this (CreateAction.defaultCollector, CreateAction.defaultAutoRegister, CreateAction.defaultAutoStart);
		}
		
		public CreateAction (final List<RandomGenerator> collector, final boolean autoRegister, final boolean autoStart)
		{
			super (RandomGenerator.class, collector, autoRegister, autoStart);
		}
		
		@Override
		protected void startObject ()
		{
			this.object.start ();
		}
		
		public static boolean defaultAutoRegister = true;
		public static boolean defaultAutoStart = true;
		public static List<RandomGenerator> defaultCollector = null;
	}
}
