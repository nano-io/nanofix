package net.nanofix.netty;

import net.nanofix.message.FIXMessageDecoder;
import net.nanofix.session.DefaultFIXLogonManager;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * User: Mark
 * Date: 24/04/12
 * Time: 06:26
 */
public class FIXClientPipelineFactory implements ChannelPipelineFactory {

    @Override
    public ChannelPipeline getPipeline() throws Exception {
//        final ChannelHandler[] handlers = {
//                new IdleStateHandler(HASHEDWHEELTIMER, 1, 1, 1),
//                new FIXFrameDecoder(),
//                STRINGDECODER,//Incoming
//                STRINGENCODER,//Outgoing
//                isDebugOn ? LOGHANDLER : NOOPHANDLER,
//                new FIXSessionProcessor(true,headerFields, trailerFields, logonManager,
//                        sessions, queueFactory, outgoingCallback)
//        };

        ChannelPipeline pipeline = Channels.pipeline(
                new FIXFrameDecoder()
        );
        return pipeline;
    }
}
