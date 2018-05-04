package server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @author yudungang
 */
public class PlainNioServer {

    public void serve(int port) throws IOException{
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        ServerSocket ss = serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        ss.bind(address);
        //打开Selector 来处理Channel
        Selector selector = Selector.open();
        //将ServerSocket 注册到Selector 以接受连接
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        final ByteBuffer msg = ByteBuffer.wrap("Hi!\r\n".getBytes());
        for (;;){
            try {
                //等待需要处理的新事件；阻塞将一直持续到下一个传入事件
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            //获取所有接收事件的SelectionKey实例
            Set<SelectionKey> readykeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readykeys.iterator();
            while(iterator.hasNext()){
                SelectionKey key  = iterator.next();
                iterator.remove();

                try {
                    //检查事件是否是一个新的已经就绪可以被接受的连接
                    if(key.isAcceptable()){
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        //接受客户端，并将它注册到选择器
                        client.register(selector,SelectionKey.OP_WRITE | SelectionKey.OP_READ,msg.duplicate());
                        System.out.println("Accepted connection from "+ client);
                    }

                    if(key.isWritable()){//检查套接字是否已经准备好写数据
                        SocketChannel client = (SocketChannel)key.channel();
                        ByteBuffer buffer = (ByteBuffer)key.attachment();
                        while(buffer.hasRemaining()){
                            if(client.write(buffer) == 0){//将数据写到已连接的客户端
                                break;
                            }
                        }
                        client.close();//关闭连接
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        key.channel().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    @Test
    public void testCompositeByteBuf(){
        CompositeByteBuf messageBuf = Unpooled.compositeBuffer();
        ByteBuf headerBuf = Unpooled.copiedBuffer("header".getBytes(CharsetUtil.UTF_8));
        ByteBuf bodyBuf = Unpooled.copiedBuffer("body".getBytes(CharsetUtil.UTF_8));
        messageBuf.addComponents(headerBuf,bodyBuf);
        messageBuf.removeComponent(0);
        for (ByteBuf buf : messageBuf){
            System.out.println(buf.toString(CharsetUtil.UTF_8));
        }
    }

    @Test
    public void testForeachByteBuf() throws IOException {
        ByteBuf buffer = Unpooled.copiedBuffer("hello\r\nlls".getBytes(CharsetUtil.UTF_8));
        int index = buffer.forEachByte(ByteProcessor.FIND_CR);
        System.out.println(buffer.hasArray());
        System.out.println(index);

    }

    @Test
    public void testChannelCount(){
        io.netty.channel.socket.ServerSocketChannel channel = new NioServerSocketChannel();
        ByteBufAllocator alloc = channel.alloc();
        ByteBuf byteBuf = alloc.directBuffer();
        Assert.assertEquals(byteBuf.refCnt(),1);
        System.out.println(byteBuf.refCnt());
        byteBuf.release();

    }
}
