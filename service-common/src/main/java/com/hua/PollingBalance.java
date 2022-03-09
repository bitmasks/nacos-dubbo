package com.hua;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance;
import org.springframework.util.StringUtils;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PollingBalance extends AbstractLoadBalance {

    //根据配置文件，指定中心
    public static final String NAME = "pollingBalance";

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        //获取nacos中，元数据中的本地地址  localCluster
        String invokeCluster = url.getParameters().get("invokeCluster");

        int length = 0;
        try {
            if (!StringUtils.isEmpty(invokeCluster)) {
                Iterator<Invoker<T>> iterator = invokers.iterator();
                while (iterator.hasNext()) {
                    Invoker<T> invoker = iterator.next();
                    Class<? extends Invoker> aClass = invoker.getClass();
                    Field providerUrl = aClass.getDeclaredField("providerUrl");
                    providerUrl.setAccessible(true);
                    URL u = (URL) providerUrl.get(invoker);
                    String localCluster = u.getParameters().get("localCluster");
                    if (!invokeCluster.equals(localCluster)) {
                        iterator.remove();
                    }
                }
            }
            length = invokers.size();
            boolean sameWeight = true;
            int[] weights = new int[length];
            int firstWeight = getWeight((Invoker)invokers.get(0), invocation);
            weights[0] = firstWeight;
            int totalWeight = firstWeight;

            int offset;
            int i;
            for(offset = 1; offset < length; ++offset) {
                i = getWeight((Invoker)invokers.get(offset), invocation);
                weights[offset] = i;
                totalWeight += i;
                if (sameWeight && i != firstWeight) {
                    sameWeight = false;
                }
            }

            if (totalWeight > 0 && !sameWeight) {
                offset = ThreadLocalRandom.current().nextInt(totalWeight);

                for(i = 0; i < length; ++i) {
                    offset -= weights[i];
                    if (offset < 0) {
                        return (Invoker)invokers.get(i);
                    }
                }
            }
            return (Invoker)invokers.get(ThreadLocalRandom.current().nextInt(length));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("指定中心未查到实例！");
            return null;
        }
    }

    int getWeight(Invoker<?> invoker, Invocation invocation) {
        URL url = invoker.getUrl();
        int weight;
        if ("org.apache.dubbo.registry.RegistryService".equals(url.getServiceInterface())) {
            weight = url.getParameter("registry.weight", 100);
        } else {
            weight = url.getMethodParameter(invocation.getMethodName(), "weight", 100);
            if (weight > 0) {
                long timestamp = invoker.getUrl().getParameter("timestamp", 0L);
                if (timestamp > 0L) {
                    long uptime = System.currentTimeMillis() - timestamp;
                    if (uptime < 0L) {
                        return 1;
                    }

                    int warmup = invoker.getUrl().getParameter("warmup", 600000);
                    if (uptime > 0L && uptime < (long)warmup) {
                        weight = calculateWarmupWeight((int)uptime, warmup, weight);
                    }
                }
            }
        }

        return Math.max(weight, 0);
    }

    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        int ww = (int)((float)uptime / ((float)warmup / (float)weight));
        return ww < 1 ? 1 : Math.min(ww, weight);
    }
}
