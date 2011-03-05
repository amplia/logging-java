
package eu.arkitech.logback.amqp.accessors;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;


public final class AmqpRawConsumer
		extends AmqpRawAccessor
{
	public AmqpRawConsumer (final AmqpRawConsumerConfiguration configuration, final BlockingQueue<AmqpRawMessage> buffer)
	{
		super (configuration);
		this.exchange = Preconditions.checkNotNull (((configuration.exchange != null) && !configuration.exchange.isEmpty ()) ? configuration.exchange : AmqpRawConsumerConfiguration.defaultExchange);
		Preconditions.checkArgument (!this.exchange.isEmpty ());
		this.queue = Preconditions.checkNotNull (((configuration.queue != null) && !configuration.queue.isEmpty ()) ? configuration.queue : AmqpRawConsumerConfiguration.defaultQueue);
		this.routingKey = Preconditions.checkNotNull (((configuration.routingKey != null) && !configuration.routingKey.isEmpty ()) ? configuration.routingKey : AmqpRawConsumerConfiguration.defaultRoutingKey);
		this.buffer = (buffer != null) ? buffer : new LinkedBlockingDeque<AmqpRawMessage> ();
	}
	
	public final BlockingQueue<AmqpRawMessage> getBuffer ()
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
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
					if (this.declare ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
				continue loop;
			}
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
					if (this.register ())
						break;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
				continue loop;
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer shoveling inbound messages");
			while (true) {
				synchronized (this.monitor) {
					if (this.shouldStopSoft ())
						break loop;
					if (this.shouldReconnect ())
						continue loop;
				}
				try {
					Thread.sleep (this.waitTimeout);
				} catch (final InterruptedException exception) {}
			}
		}
		synchronized (this.monitor) {
			if (this.isConnected ())
				this.disconnect ();
		}
	}
	
	private final void consume (final Envelope envelope, final BasicProperties properties, final byte[] content)
	{
		final AmqpRawMessage message = new AmqpRawMessage (envelope.getExchange (), envelope.getRoutingKey (), properties.getContentType (), properties.getContentEncoding (), content);
		if (!this.buffer.offer (message))
			this.callbacks.handleLogEvent (Level.ERROR, null, "amqp consumer buffer overrun; ignoring!");
	}
	
	private final boolean declare ()
	{
		final Channel channel = this.getChannel ();
		{
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer declaring the exchange `%s`", this.exchange);
			try {
				channel.exchangeDeclare (this.exchange, "topic", true, false, null);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while declaring the exchange `%s`; aborting!", this.exchange);
				return (false);
			}
		}
		{
			final String queue;
			final boolean unique;
			if ((this.queue == null) || this.queue.isEmpty ()) {
				queue = "";
				unique = true;
			} else {
				queue = this.queue;
				unique = false;
			}
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer declaring the queue `%s`", queue);
			this.queue1 = null;
			try {
				this.queue1 = channel.queueDeclare (queue, true, unique, unique, null).getQueue ();
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while declaring the queue `%s`; aborting!", queue);
				return (false);
			}
		}
		{
			this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer binding the queue `%s` to exchange `%s` with routing key `%s`", this.queue1, this.exchange, this.routingKey);
			try {
				channel.queueBind (this.queue1, this.exchange, this.routingKey, null);
			} catch (final Throwable exception) {
				this.callbacks.handleException (exception, "amqp consumer encountered an error while binding the queue `%s` to exchange `%s` with routing key `%s`; aborting!", this.queue1, this.exchange, this.routingKey);
				return (false);
			}
		}
		return (true);
	}
	
	private final boolean register ()
	{
		this.callbacks.handleLogEvent (Level.INFO, null, "amqp consumer registering the consumer");
		final Channel channel = this.getChannel ();
		try {
			channel.basicConsume (this.queue1, true, this.queue1, true, true, null, new ConsumerCallback ());
			return (true);
		} catch (final Throwable exception) {
			this.callbacks.handleException (exception, "amqp consumer encountered an error while registering the consummer; aborting!");
			return (false);
		}
	}
	
	private final BlockingQueue<AmqpRawMessage> buffer;
	private final String exchange;
	private final String queue;
	private String queue1;
	private final String routingKey;
	
	private final class ConsumerCallback
			implements
				Consumer
	{
		@Override
		public void handleCancelOk (final String consumerTag)
		{}
		
		@Override
		public void handleConsumeOk (final String consumerTag)
		{}
		
		@Override
		public void handleDelivery (final String consumerTag, final Envelope envelope, final BasicProperties properties, final byte[] content)
		{
			AmqpRawConsumer.this.consume (envelope, properties, content);
		}
		
		@Override
		public void handleRecoverOk ()
		{}
		
		@Override
		public void handleShutdownSignal (final String consumerTag, final ShutdownSignalException exception)
		{}
	}
}
