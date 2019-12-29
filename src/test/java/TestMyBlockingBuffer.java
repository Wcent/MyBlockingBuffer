import org.cent.util.MyBlockingBuffer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * 使用生产者-消费者模型测试MyBlockingBuffer使用
 * @author Vincent
 * @version 1.0 2019/12/29
 */
public class TestMyBlockingBuffer {

    @Test
    public void testBlockingBuffer() {
        MyBlockingBuffer<String> buffer = new MyBlockingBuffer<String>(10);
        CountDownLatch countDownLatch = new CountDownLatch(2);

        // 模拟消费者
        Thread consumer = new Thread(() -> {
            System.out.println("Waiting to consume:");
            try {
                while (true) {
                    if (Thread.interrupted()) // 响应中断
                        throw new InterruptedException();
                    System.out.println(Thread.currentThread().getName() + " take: " + buffer.take());
//                    if (buffer.size() == 0)
//                        Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                System.out.println("Consuming Interrupted");
                Thread.currentThread().interrupt();
            }
        }, "Thread-Consumer");
        consumer.start();

        // 模拟两个生产者
        for (int i=0; i<2; i++) {
            new Thread(() -> {
                Random random = new Random();
                for (int j=0; j<100; j++) {
                    try {
                        buffer.put(Thread.currentThread().getName()+" put: "+j);
                        Thread.sleep(random.nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                countDownLatch.countDown(); // 通知数据生产完毕
            }, "Thread-Producer-"+i).start();
        }

        try {
            countDownLatch.await(); // 阻塞等待生产者全部写入数据到缓冲区
            while (buffer.size() > 0) {} // 自旋检查缓冲区数据量，消费完毕则中断消费者
            consumer.interrupt();
            Assert.assertEquals( 0, buffer.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
