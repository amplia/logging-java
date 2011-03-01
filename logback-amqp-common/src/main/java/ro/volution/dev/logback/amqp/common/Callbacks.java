
package ro.volution.dev.logback.amqp.common;


import ch.qos.logback.classic.Level;


public interface Callbacks
{
	public abstract void handleException (
			final Throwable exception, final String messageFormat, final Object ... messageArguments);
	
	public abstract void handleLogEvent (
			final Level level, final Throwable exception, final String messageFormat, final Object ... messageArguments);
}