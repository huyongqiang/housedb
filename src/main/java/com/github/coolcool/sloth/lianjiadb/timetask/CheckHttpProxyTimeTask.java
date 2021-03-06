package com.github.coolcool.sloth.lianjiadb.timetask;

import com.alibaba.fastjson.JSONObject;
import com.github.coolcool.sloth.lianjiadb.common.MyHttpClient;
import com.github.coolcool.sloth.lianjiadb.mapper.HttpProxyMapper;
import com.github.coolcool.sloth.lianjiadb.model.HttpProxy;
import com.github.coolcool.sloth.lianjiadb.spider.CheckIPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.TimerTask;


/**
 * 检测代理服务是否有效, 加入到代理服务器列表
 */
@EnableScheduling
@Service
public class CheckHttpProxyTimeTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(CheckHttpProxyTimeTask.class);

    static boolean running = false;

    @Autowired
    HttpProxyMapper httpProxyMapper;

    @Override
    @Scheduled(cron="0 0/1 * * * ?")   //每1分钟执行一次
    public void run() {
        if(!running){
            running = true;

            addHttpProxyFromDb();

            try {

                for (int i = 0; i < MyHttpClient.allHttpProxyConfigs.size(); i++) {
                    MyHttpClient.HttpProxyConfig httpProxyConfig = MyHttpClient.allHttpProxyConfigs.get(i);
                    if(!CheckIPUtils.checkValidIP(httpProxyConfig.getHost(),httpProxyConfig.getPort())){
                        httpProxyMapper.deleteById(httpProxyConfig.getId());
                        MyHttpClient.removeAvailableHttpProxyConfig(httpProxyConfig);
                    }else{
                        httpProxyConfig.setStatus(1);
                        MyHttpClient.addAvailableHttpProxyConfig(httpProxyConfig);
                    }

                }
            }catch (Throwable t){
                t.printStackTrace();
            }

            running = false;
        }
    }




    private void addHttpProxyFromDb(){
        List<HttpProxy> httpProxies = httpProxyMapper.listAll();
        for(HttpProxy httpProxy : httpProxies){
            String host = httpProxy.getHost();
            boolean isAdd = true;
            for(MyHttpClient.HttpProxyConfig httpProxyConfig : MyHttpClient.allHttpProxyConfigs){
                if(host.equals(httpProxyConfig.getHost())){
                    isAdd = false;
                    break;
                }
            }
            if(isAdd){
                MyHttpClient.HttpProxyConfig httpProxyConfig = new MyHttpClient.HttpProxyConfig();
                httpProxyConfig.setHost(httpProxy.getHost());
                httpProxyConfig.setPort(httpProxy.getPort());
                httpProxyConfig.setId(httpProxy.getId());
                MyHttpClient.allHttpProxyConfigs.add(httpProxyConfig);
                log.info("load httpProxyConfig from DB : "+JSONObject.toJSONString(httpProxyConfig));
            }
        }
    }

    /**
     * 获取代理ip每分钟获取一次
      */
    @Scheduled(cron="0 0/1 * * * ?")
    public void getHttpProxy(){

    }

}
