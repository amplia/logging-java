
package eu.arkitech.logback.amqp.accessors;


import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Level;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;


public final class AmqpRawPublisher
		extends AmqpRawAccessor
{
	public AmqpRawPublisher (final AmqpRawPublisherConfiguration configuration, final BlockingDeque<AmqpRawMessage> buffer)
	{
		super (configuration);
		this.buffer = (buffer != null) ? buffer : new LinkedBlockingDeque<AmqpRawMessage> ();
	}
	
	public final BlockingDeque<AmqpRawMessage> getBuffer ()
	{
		return (this.buffer);
	}
	
	@Override
	protected final void loop ()
	{
		loop : while (true) {
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.reconnect ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp publisher shoveling outbound messages");
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.shouldReconnect ())
						break;
				}
				final AmqpRawMessage message;
				try {
					message = this.buffer.poll (this.waitTimeout, TimeUnit.MILLISECONDS);
				} catch (final InterruptedException exception) {
					continue;
				}
				if (message == null)
					continue;
				synchronized (this.monitor) {
					if (!this.publish (message))
						this.buffer.addFirst (message);
				}
			}
		}
		if (this.isConnected ())
			this.disconnect ();
	}
	
	@Override
	protected final boolean shouldStopSoft ()
	{
		return (this.buffer.isEmpty () && super.shouldStopSoft ());
	}
	
	private final boolean publish (final AmqpRawMessage message)
	{
		final Channel channel = this.getChannel ();
		final AMQP.BasicProperties properties = new AMQP.BasicProperties ();
		properties.setContentType (message.contentType);
		properties.setContentEncoding (message.contentEncoding);
		properties.setDeliveryMode (2);
		try {
			channel.basicPublish (message.exchange, message.routingKey, false, false, properties, message.content);
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp publisher encountered an error while publishing the message; requeueing!");
			this.disconnect ();
			return (false);
		}
		return (true);
	}
	
	private final BlockingDeque<AmqpRawMessage> buffer;
}
