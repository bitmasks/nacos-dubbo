#SpringBoot项目，集成了nacos-dubbo

##service-api
这个模块，主要是提供对外暴露接口的。

##provider
这个模块，主要是消费者。

##consumer
这个模块，主要是生产者

##service-common
这个模块，主要是项目所使用的插件之类的公共组件
1、PollingBalance是通过dubbo的SPI方式拓展的根据dubbo.application.parameters的配置，选择指定的数据中心。实现就近距离访问。