# NetworkLocationService

**主要功能：** 于提供第三方网络定位服务接口，规避无法设置高精度而导致某些测试项无法通过问题。

**main分支：** 对应Android P 版本的 LocationProviderBase，不同版本中的 LocationProviderBase 内容也不一样,要对比当前系统中文件;

com.android.location.provider 包中文件对应的是Android frameworks 对应目录中的文件，aidl 内容也一样，修改时要注意。

内部集成百度SDK，并完成初始化，在系统回调过程中设置高精度模式；(注册开发者 apk key, 也未验证数据返回，参考百度SDKdemo)




参考文档：
1. 百度定位SDKDemo https://lbsyun.baidu.com/index.php?title=android-locsdk/geosdk-android-download  
（1）基础定位：开发包体积最小，但只包含基础定位能力（GPS/Wi-Fi/基站）、基础位置描述能力；    -----此项时我所需要的  
（2）离线定位：在基础定位能力基础之上，提供离线定位能力，可在网络环境不佳时，进行精准定位；  
（3）室内定位：在基础定位能力基础之上，提供室内高精度定位能力，精度可达1-3米；  
（4）全量定位：包含离线定位、室内高精度定位能力，同时提供更人性化的位置描述服务。  

2. githubCode https://github.com/microg/NetworkLocation  
    NetworkLocationService 软件中主要架构来源于此github，但此源码并不是基于AndroidStudio编译，于是小编自己重新整理并加入BaiduSDK 以满足自己需求


