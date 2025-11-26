# 局域网 http对接协议
## 说明：此文档适用于标准版本，定制版本差异请咨询技术人员 更新日志
| 更新日期 | 更新说明 | 备注 |
| --- | --- | --- |
| 2020-08 | 首发版本 |  |
| 2021-04-12 | 1、  增加时间段支持 2、  增加设备照片检验接口 3、增加了重提记录的接口 |  |
| 2021-05-10 | 1、  新增人证比对模式 仅刷身份证和验证身份证 2、  增加记录上传中的模板照片 |  |
| 2021-05-13 | 1、增加设备提示文字 |  |
| 2021-05-13 | 增加 RTSP视频流切换功能 |  |
| 2021-05-24 | 增加不需要照片录入名单功能 |  |
| 2021-06-23 | 补充获取视频流方法 |  |
| 2021-07-08 | 增加设备数据传输加密接口 |  |
| 2021-08-09 | 增加健康码数据推送 |  |
| 2021-11-24 | 增加了韦根类型等参数 |  |
| 2021-12-07 | 修改时间组设置方式（不影响原来的设置方式） |  |
| 2022-03-24 | 增加设置 UI接口 |  |
| 2023-02-20 | 增加抓拍设备人脸照片 （可用于添加白名单） |  |
| 2023-03-20 | 增加一人多卡参数、添加部分设备参数 |  |
| 2023-12-26 | 增加获取网络参数接口 |  |
| 2024-09-26 | 获取设备参数接口增加经纬度 |  |

# 1      设备协议概述
## 1.1    简述
该文档是我司基于人脸识别设备端的开放协议，主要提供给需要使用 HTTP协议对接设 备的客户，支持局域网内的 http协议开发。接口主要包括人员管理、照片管理、设备信息配 置、记录上传等相关的核心业务。
## 1.2    接口规范
协议类型：使用标准 HTTP协议，Content类型可以包含 Json数据和二进制数据，具体参 照相关协议，请务必严格按照提供的接口参数和类型填写。
接口地址：http://<ip>:8091/<URL>
接口安全：初次调用接口需要先使用用户和密码登录，后续调用任何接口都需要传入密码 作为接口安全校验秘钥。
格式说明：数据通信的时候可能会涉及到中文字符，统一采用的是 utf-8格式，使用 post 传输参数时，将参数采用 json格式放到 body中。
## 1.3    接口说明
默认启动端口号为 8091端口 ；密码默认为 123456
# 2    协议接口定义
## 2.1  登录和心跳消息
### 2.1.1 登录设备
Url 地址：http://deviceAddress:port/deviceLogin
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 用户密码 默认123456 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; |

接受消息应答数据内容
示例
![id-23.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-23.png?sign=1766572729-65b7cccd7c-0-8e32bc906b707b979709aa350cc06f178697283b5dba6778507b269a8913c848)
### 2.1.2 心跳消息
由用户【设置设备参数】设置的 heartBeatIp地址 方法一： ，设备主动上传心跳信息。
支持 post 方法
具体查看设备参数
http://deviceAddress:port/s 接口地址 etDeviceParameter
（接口地址是配置设备参数接口）
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 表示当前消息体的数据 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| int | heartBeatEnable | 0开 1关 |
| String | heartBeatIp | 心跳地址 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | currentTime | 时间 |
| String | ip | 设备的 ip地址 |
| int | personCount | 人脸数 |
| String | SN | 设备 sn |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 非 0 异常 |

方法二：主动请求设备心跳
Url 地址：http://deviceAddress:port/heartBeat
请求方式：post
发送消息的内容
Data消息体的内容
接受消息应答数据内容
配置后设备的心跳请求如下：（心跳间隔 30S）
设备发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 用户密码默认123456 |
| String | ip | 设备的ip地址 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

接受消息应答数据内容
示例
1 2 3 4 5 6 7 url：http://192.168.1.135:8091/heartBeat 发送： {"password":"123456","ip":"192.168.1.135"} 返回： {"message" :"HeartBeat Success","result" : 0}
2.2  设备运行信息
### 2.2.1 获取设备参数
http://deviceAddress:port/g Url 地址： etDeviceParameter
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 用户密码 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |
| Object | data | 表示当前消息体的数据 |

设置参数的 data消息体的内容：
（data中有健康码设置参数，不用管此参数）
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | devicename | 名称 |
| String | Mac_addr | 设备唯一 mac地址 |
| String | SN | 设备唯一序列号 |
| String | location | 位置 |
| int | inout | 出入 0出口   1入口 |
| String | pwd | 密码 6位数字 |
| int | whitevalue | 人脸识别阈值 [70,100] |
| int | mapvalue | 人证识别阈值 [30,100] |
| int | recogSpaceTime | 识别去重时间（秒）[2,255] |
| int | delayvalue | 开门延迟 秒（默认 2S） |
| int | lightLevelPercent | 补光灯亮度  0-100 |
| int | lightType | 0 常关   1 常开  2 按时间段开启 3自动补光（自动补光规则： 检测到人脸即亮、无人脸立即关闭，当摄像头透光量低时常开 补光灯） |
| String | lightTimeStart | 补光灯开启时间 |
| String | lightTimeEnd | 补光灯关闭时间 |
| int | systemVol | 系统音量 [0,7] |
| int | detectRange | 识别距离 [0,3] [无限制,0.5米,1米,2米] |
| int | wgOutType | 韦根输出类型  0.韦根 26 1.韦根 34 |
| int | wgInOutMode | 韦根模式 0韦根输出，1 韦根输入 |
| int | livenessEnable | 活体开关  0开启 1关闭 |
| int | InfraredImaging | 黑白成像视频  0开启  1关闭 |
| int | livenessValue | 活体分数  0-10 |
| int | recogModeDB | 人脸识别 0禁用，1开启，2健康码白名单（不开启健康码时此 功能就是人脸识别） |
| int | recogModeIC | 识别模式 0禁用  1 IC卡识别  2  IC卡加人脸识别 |
| int | recogModeID | 识别模式   0 禁用  1人证识别 （身份证照片+人脸照片比对）  2 人证+白名单验证（人证比对后检验人脸在百名单库） 3 仅 刷身份证 4 验证身份证 （刷了身份证后验证身份证号码在白名单库中） |
| int | detectVoiceEnable | 陌生人识别  0禁用，1抓拍识别， 2 抓拍识别并开门 |
| int | recogRelay | 识别开门   0继电器不输出 继电器输出 1 （设置继电器不输出后需调用接口开闸 http://deviceAddress:port/setDeviceRemoteOpen ） |
| String | rebootTime | 自动重启间隔 "DDHHmm" DD说明：00 每天，01周一，02周二，03周三，04周四，05 周五，06周六，07周日 |
| int | TemperatureMode | 测温功能   0 不测温   1 测温+人脸识别  2 只测温 |
| String | TempNormalMax | 测温报警温度（默认 37.2） |
| int | TempThermography | 测温热成像视频 0 关闭 1 开启 |
| int | OutSideDetectMode | 测温使用环境 0:标准模式 1:户外模式 |
| int | EnableMasks | 口罩检测 0 关闭 1 开启 |
| int | IntelligentRecognition | 安全帽识别  0 关闭 1 开启 |
| int | wgOutType | 韦根输出类型  0-韦根 26， 1-韦根 34 |
| int | wgInOutMode | 韦根模式 0韦根输出  1韦根输入 |
| int | relayMode | 继电器类型 0:标准模式 、1:持续开门，2:持续关门 |
| String | banner | 设备标语（默认值为人脸识别门禁终端）（说明：修改此值需 bannerEnable参数 要开启 ） |
| int | bannerEnable | 待机屏保 0 关  1 打开 |
| int | bannerSpaceTime | 待机屏保图片切换时间 |
| int | VideoSwitch | RTSP视频流切换  0 RGB视频流， 1 IR视频 |
| int | voiceSetting | （语音模块接串口 2） 自定义语音功能 0禁用，1启用 |
| String | customVoice | 自定义语音内容 |
| int | resultDisplayMode | 识别结果显示 0禁用，1启用 |
| String | resultCustomization | 识别结果显示内容（自定义显示识别结果） |
| int | passwordOpendoor | 密码开门功能 0禁用，1启用 |
| String | doorPassword | 开门密码（6位纯数字） |
| int | Comparison | 比对模式 0离线比对 1在线比对 |
| String | comparisonAddress | 在线比对地址 |
| int | longitude | 经度 （仅支持获取，不支持设置） |
| int | latitude | 纬度（仅支持获取，不支持设置） |
| int | backlightType | 显示屏控制模式 0 常亮 2自动息屏 |

示例
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15 "pwd" : "123456",
16 "rebootTime" :   "000000",
17 "recogModeDB" : 1,
18 "recogModeIC" : 0,
19 "recogModeID" : 1,
20 "recogRelay" : 0,
21 "recogSpaceTime" : 3,
22 "relayMode" : 0,
23 "resultFormat" :   "",
24 "resultNameEncrypt" : 0,
25 "resultSetting" : 0,
26 "resultVoiceFormat" :   "",
27 "systemVol" : 7,
28 "upblack" : 1,
29 "uploadImage" : 1,
30 "voiceSetting" : 2,
31 "wgOutType" : 0,
32 "whitevalue" : 80
33 },
34
35
36
"message" : "Get Device   Parameter Success",
"result" : 0
}
url：http://192.168.1.135:8091/getDeviceParameter
发送：
{"password":"123456"}
返回：
{
"data" :
{
"CheckTempWorkMode" : 0,
"EnableMasks" : 0,
"EnableNonResidentTemp" : 1,
"platformEnable" : 0,
"platformIp" :   "http://192.168.1.198:8080/getrecord/",
### 2.2.2 设置设备参数
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 表示当前消息体的数据 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

http://deviceAddress:port/s Url 地址： etDeviceParameter
请求方式：post
发送消息的内容
消息内容的结构见【获取设备参数】消息体的内容
接受消息应答数据内容
示例
1
2
3
4
5
6
7
url：http://192.168.1.135:8091/setDeviceParameter
发送：
{"password":"123456","data":{"name":"test","location":"test"}}
返回：
{"message":"Set Device Parameter Success","result" : 0}
### 2.2.3  设备升级
http://deviceAddress:port/u Url 地址： ploadUpgradeAPKFile
支持 post 以表单方式的方式上传
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

接受消息应答数据内容
注意：
1), Headers,Content-Type 类型为 multipart/form-data；
2), Params,需要携带 password
3), Body 选择需要升级.img文件，文件只能从低版本升级到高版本；
### 2.2.3  设备重启
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

http://deviceAddress:port/s Url 地址： etDeviceReboot
请求方式：post
发送消息的内容
消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | type | 类型为： DelayReboot 延时重启 |
| int | value | 指定当前在多少秒后重启 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |

| String | message | 提示消息 |
| --- | --- | --- |
| String | result | 0成功;1,参数设置错误;2.密码错误 |

示例
1
2
3
4
5
6
7
url：http://192.168.1.135:8091/setDeviceReboot
发送：
{"password":"123456","data":{"type":"DelayReboot","value":"5"}}
返回：
{"message" : "Set Device   Reboot Success","result" : 0}
### 2.2.4  设备控制开闸
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

示例
http://deviceAddress:port/s Url 地址： etDeviceRemoteOpen
请求方式：post
发送消息的内容
1 2 3 4 5 6 7 8 url：http://192.168.1.135:8091/setDeviceRemoteOpen 发送： {"password":"123456"} 返回： {"message" : "Set Device   Remote Open Success","result" : 0}
### 2.2.5   获取版本信息
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

消息体的内容
http://deviceAddress:port/g Url 地址： etDeviceVersion
请求方式：post
发送消息的内容
接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | device_name | 设备名称 |
| String | firmware_version | 软件版本 |
| String | sysvertime | 编译时间 |
| String | model | 型号 |
| String | macaddr | MAC地址 |
| String | SN | SN |

示例
1
2
3
4
5
6
7
8 "data": {
9 "device\_name": "Terminal",
10 "firmware\_version": "101.10193.0000 Build-202100607",
11
12
13 "sysvertime": "20210608-093539"
14 },
15 "message": "Get Device Version Success",
16 "result": 0
17 }
url：http://192.168.1.135:8091/getDeviceVersion
发送：
{"password":"123456"}
返回：
{
"macaddr": "DA:8B:11:17:23:3B",
"model": "C108LD",
### 2.2.6  获取相机当前图片
http://deviceAddress:port/g Url 地址： etDeviceSnapPicture
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | snapPicBase64 | 相机获取的base64图片数据 |

消息体的内容
示例
![id-221.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-221.png?sign=1766572729-4e5225d805-0-154ff3f50be13479715d8ad9b1a7151a3bb7672057accdb01faaa72f4aa1af09)
### 2.2.7  获取相机当前人脸照片
http://deviceAddress:port/ Url 地址： getDeviceSnapFace
请求方式：post
说明：此接口用于抓拍相机当前的人脸照片，与 2.2.6的不同点是 2.2.6接口请求后直接抓拍当 前的照片，2.2.7接口会检测到人脸后再进行抓拍
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | snapPicBase64 | 相机获取的base64图片数据 |

### 2.2.7  设置设备 logo图片
http://deviceAddress:port/s Url 地址： etDeviceLogo
支持 post 表单方法上传文件
发送消息的内容
接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

消息体的内容
示例
1
2
3
4
5
6
7
8
返回：
{"data":{\\n\\t\\"snapPicBase64":"/9j/4AAQSkZ…………………vOO5waXgEEdM9R1os
WhFxuXn+IAntipmh44qIADgjJ7jOasRy444pNaCP/Z"},"message":"Get Device
SnapPicture Success","result":0}
url：http://192.168.1.135:8091/getDeviceSnapFace
发送：
{"password":"123456"}
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

接受消息应答数据内容
注意：
1), Headers,Content-Type 类型为 multipart/form-data；
2), Params,需要携带 password
3), Body 选择需要设置的图片文件
### 2.2.8  恢复默认设备 logo图片
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

http://deviceAddress:port/ Url 地址： restoreDeviceLogo
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

示例
接受消息应答数据内容
1 2 3 4 5 6 7 8 url：http://192.168.1.135:8091/restoreDeviceLogo 发送： {"password":"123456"} 返回： {"message":"Restore device default logo.","result":0}
### 2.2.9   设置设备的网络地址
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

http://deviceAddress:port/s Url 地址： etDeviceNetwork
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| boolean | isDhcp | 是否启动 dhcp动态 ip |
| String | ip | 静态 ip地址 |
| String | gateway | 默认网关 |
| String | mask | 子网掩码 |
| String | dns | dns |

接受消息应答数据内容
 消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0成功; 1,参数设置错误;2,密 码错误; |

示例
1
2
3 发送：
4
5
6 返回：
7 {"message":"","result":0}
url：http://192.168.1.135:8091/setDeviceNetwork
{"password":"123456","data":{"isDhcp":false,"ip":"192.168.1.136","gateway":"192.16
8.1.1","mask":"255.255.255.0","dns":"8.8.8.8"}}
### 2.2.10  恢复设备所有参数配置
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

示例
http://deviceAddress:port/ Url 地址： restoreDeviceDefaultParameter
请求方式：post
发送消息的内容
1 2 3 4 5 6 7 8 url：http://192.168.1.135:8091/restoreDeviceDefaultParameter 发送： {"password":"123456"} 返回： {"message" :"Restore Device Default Parameter Success","result" : 0}
### 2.2.12 获取设备时间
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| string | time | 时间 |

http://deviceAddress:port/getDeviceTime Url 地址：
请求方式：post
发送消息的内容
接受消息应答数据内容
示例
1 url：http://192.168.1.135:8091/getDeviceTime
2
3 发送：
4 {"password":"123456"}
5
6 返回：
7 {"data" : "{\\n\\t\\"time\\" : \\"2020-12-16 15:05:07\\"\\n}","message" : "Get Device Time Success","result" : 0}
### 2.2.13  设置设备时间
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| string | time | 2020-06-12 18:00:00 （ ） 时间 |

http://deviceAddress:port /setDeviceTime Url 地址：
请求方式：post
发送消息的内容
消息体的内容 
接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码 错误； |

示例
1 2 3 4 5 6 7 8 url：http://192.168.1.135:8091/setDeviceTime 发送： {"password":"123456","data":{"time":"2020-06-12 18:00:00"}} 返回： {"message" : "Set Device Time   Success","result" : 0}
### 2.2.14 设置设备固件升级
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

说明：设置设备固件升级和设备升级功能相同
http://deviceAddress:port/ Url 地址： setDeviceOTAUpgrade
支持 post上传表达方式
发送消息的内容
接受消息应答数据内容
注意：
1), Headers,Content-Type 类型为 multipart/form-data；
2), Params,需要携带 password
3), Body 选择需要升级 update.zip文件，从低版本升级到高版本；
### 2.2.15 设备显示提示文字
Url 地址：http://deviceAddress:port/setDeviceShowMessage
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 用户密码 默认 123456 |
| Object | data | 表示当前消息体的数据 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 最长发送14个汉字 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; |

请求方式：post
调用此接口时设备将弹窗显示接口下发内容三秒钟。
发送消息的内容
参数的 data消息体的内容：
接受消息应答数据内容
示例
1
2
3
4
5
6
7
8
url：http://192.168.1.135:8091/setDeviceShowMessage
发送：
{"password":"123456","data":{"message":"这是测试内容"}}
返回：
{"message" : "Set Device Show Message Success","result" : 0}
### 2.2.16 获取设备当前卡号
http://deviceAddress:port/g Url 地址： etDeviceIcidNumber
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | icidNumber | 设备读取到的IC卡号/ID卡号 |

接受消息应答数据内容
消息体的内容
示例
1
2
3
4
5
6
7
8
9
10
11
12
13
14
url：http://192.168.1.135:8091/getDeviceIcidNumber
发送：
{"password":"123456"}
返回：
{
"data" :
{
"icidNumber" : "2730936666"
},
"message" : "",
"result" : 0
}
注：设备刷卡输出为 10位 10进制
### 2.2.17 设置设备 UI
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

http://deviceAddress:port/ Url 地址： setDeviceUI
支持 post 表单方法上传文件
发送消息的内容
接受消息应答数据内容
注意：
1), Headers,Content-Type 类型为 multipart/form-data；
2), Params,需要携带 password
3), Body 选择需要设置的图片文件
注意设置 UI必须传 PNG图片
### 2.2.18 获取设备的网络地址
http://deviceAddress:port/g Url 地址： etDeviceNetwork
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| boolean | isDhcp | 是否启动 dhcp动态 ip |
| String | ip | 静态 ip地址 |
| String | gateway | 默认网关 |
| String | mask | 子网掩码 |
| String | dns | dns |

 消息体的内容
示例
1 2 3 4 5 6 7 8 {"password":"123456","data":{"isDhcp":false,"ip":"192.168.1.136","gateway":"192.16 8.1.1","mask":"255.255.255.0","dns":"8.8.8.8"}} 返回： {"message" :"","result" : 0} url：http://192.168.1.135:8091/setDeviceNetwork 发送：
## 2.3    名单管理接口
### 2.3.1   添加名单
http://deviceAddress:port/ Url 地址： addDeviceWhiteList
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| int | totalnum | 表示当前需要同步白名单的总条数 |
| int | currentnum | 表示当前同步白名单的编号，从 1开始 |
| Object | data | 消息体 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | usertype | Y user类型 white(白名单), black（黑名单）， visitor（访客） |
| String | employee_number | Y 人员编号 |
| String | name | N 姓名 |
| String | playname | N 播报姓名 （适用于万能语音多音字，万能语音非标配， 对接前请询问商务人员，非多音字不用下发此字段） |
| String | sex | N 男, 女 |
| String | nation | N 民族 |
| String | telephone | N 电话 |
| String | idno | N 身份证号 |
| String | idstartdate | N 身份证有效期开始 |
| String | idenddate | N 身份证有效期结束 |
| String | peoplestartdate | N 名单有效期开始时间 |
| String | peopleenddate | N 名单有效期结束时间 |
| String | idissue | N 身份证签发机关 |
| String | idaddress | N 身份证地址 |
| String | icno | N ic卡号（10位 10进制） |
| String | icno2 | N ic卡号 |
| String | icno3 | N ic卡号 |
| String | icno4 | N ic卡号 |
| String | QRcode | N 二维码 |
| String | company | N 公司 |
| String | department | N 部门 |
| boolean | passAlgo | Y true：不需要照片 false：需要照片 |
| int | TimeGroupId | 普通时间段 0:任意时间识别成功都可同行  , 1-4周一到周日指 定时间段内识别成功才能通行 |
| int | SpecialGroupId | 特殊时间段 ID （在此时间段内可以识别通行） |
| String | register_base64 | 表示名单图片的 base64字符串 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

示例
接受消息应答数据内容
1
2
3
4
5
6
7
8 "usertype":"white",
9 "idno":"431024199522140067",
10 "employee\_number":"1001",
11 "name": "李四",
12 "sex": "女",
13 "nation": "",
14 "idstartdate": "",
15 "idenddate": "",
16 "peoplestartdate":"2020-12-16",
17 "peopleenddate":"2035-12-16",
18 "idissue":"",
19 "idaddress":"",
20 "icno": "",
21 "QRcode": "",
22
23
24
25
26
27 }
28 }
29
30
31
32
"passAlgo": false,
"company": "",
"department": "",
"TimeGroupId": 0,
"register\_base64":   "/9j…………………"
返回：
{"message" :   "添加成功","result" : 0}
url：http://192.168.1.135:8091/addDeviceWhiteList
发送：
{"password":"123456",
"totalnum": 1,
"currentnum": 1,
"data": {
### 2.3.2   删除名单
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | employee_number | Y 人员编号 |
| String | usertype | Y user类型 white(白名单),black（黑名单） |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

示例
http://deviceAddress:port/d Url 地址： eleteDeviceWhiteList
请求方式：post
发送消息的内容
消息体的内容
接受消息应答数据内容
1 url：http://192.168.1.135:8091/deleteDeviceWhiteList
2
3 发送：
4
5
6
7 {"message" : "Delete WhiteList Success","result" : 0}
{"password":"123456","data":{"employee\_number":"1001","usertype":"white"}}
返回：
### 2.3.3  删除所有白名单
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

http://deviceAddress:port/d Url 地址： eleteDeviceAllWhiteList
请求方式：post
发送消息的内容
接受消息应答数据内容
示例
1
2
3
4
5
6
7
url：http://192.168.1.135:8091/deleteDeviceAllWhiteList
发送：
{"password":"123456"}
返回：
{"message" :"Delete All Device White List Success","result" : 0}
### 2.3.4  获取名单列表
http://deviceAddress:port/g Url 地址： etAllDeviceIdWhiteList
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| Stringl] | idNumList | 表示人员的唯一编号的数组 |

消息体的内容
示例
1
2
3
4
5
6
7
8
9
10
11
12
url：http://192.168.1.135:8091/getAllDeviceIdWhiteList
发送：
{"password":"123456"}
"result" : 0
}
返回：
{
"data":"{\\n\\t\\"idList\\":\\n\\t[\\n\\t\\t\\"3886\\",\\n\\t\\t\\"3887\\",\\n\\t\\t\\"3888\\"}",
"message" : "Get All Device   White List Success",
### 2.3.5  获取名单详情
http://deviceAddress:port/g Url 地址： etDeviceWhiteListDetailByIdNum
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | employee_number | 表示人员的唯一编号 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |
| Object | data | 消息体 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | usertype | user类型；white(白名单),black（黑名单） |
| String | name | 姓名 |
| String | sex | 男,女 |
| String | nation | 民族 |
| String | idno | 身份证号 |
| String | peoplestartdate | 名单有效期开始 |
| String | peopleenddate | 名单有效期结束 |
| String | idissue | 身份证签发机关 |
| String | idaddress | 身份证地址 |
| String | icno | ic卡号 20180716 |
| int | TimeGroupId | 0:任意时间识别成功都可同行   1：周一到周日指 定时间段内识别成功才能通行，时间段外提示“无 权限进入，请在指定时间段内识别” |

![id-558.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-558.png?sign=1766572729-64850a47a7-0-d6e7a2c045f79da2237a2dd170be7bc20eb90563a370212752148817e063469d)
2.3.6  验证图片质量
http://deviceAddress:port/ Url 地址： checkFacePicture
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | register_base64 | 名单的base64字符串 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 验证成功; 1 照片验证不合格； |

接受消息应答数据内容
示例
1
2
3
4
5
6
7
8
url：http://192.168.1.135:8091/checkFacePicture
{"password":"123456","data":{"register\_base64":"./9ekdjdmje...wedddsd"}}
发送：
返回：
{"message" :"","result" : 0}
## 2.4   待机图片设置接口
### 2.4.1  删除所有待机图片
http://deviceAddress:port/d Url 地址： eleteAllDeviceAdvertPicture
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

接受消息应答数据内容
注意：会一次性清空所有的设备的图片
示例
1
2
3
4
5
6
7
8
url：http://192.168.1.135:8091/deleteAllDeviceAdvertPicture
发送：
{"password":"123456"}
返回：
{"message":"Delete   all advert pictures success","result":0}
### 2.4.2  添加待机图片
http://deviceAddress:port/a Url 地址： ddDeviceAdvertPicture
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0成功;1,参数设置错误;2,密 码错误; |

注意：
1), Headers,Content-Type 类型为 multipart/form-data；
2), Params,需要携带 password
3),上传图片文件的大小限制在 400K以下；
## 2.5 日志回调接口
### 2.5.1 获取设备日志
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | devType | 描述设备日记的类型, handle表示获取操作日记; |

接受消息应答数据内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

http://deviceAddress:port/g Url 地址： etDeviceLog
请求方式：post
发送消息的内容
错误时，会相应返回错误提示；成功时，直接会将日记文件以数据方式传输；
### 2.5.2 删除设备日志
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | devType | 描述设备日记的类型, handle表示获取操作日记; |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 码错误； 0 成功; 1,参数设置错误；2,密 |

示例
http://deviceAddress:port/d Url 地址： eleteDeviceLog
请求方式：post
发送消息的内容
消息体的内容
接受消息应答数据内容
1
2
3 发送：
4
5
6
7
8
url：http://192.168.1.135:8091/deleteDeviceLog
{"password":"123456","data":{"devType":"crash"}}
返回：
{"message" :   "Delete Device Log Success","result":0}
## 2.6  获取识别记录
### 2.6.1 识别结果回调接口
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 123456 |
| int | platformEnable | 上传识别记录到平台 0关 1开 |
| String | platformIp | 平台 Url地址 |

Url 地址：http://deviceAddress:port/setIdentifyCallBck
请求方式：post
当配置了 platformEnable=1和 platformIp信息时，人脸识别后，会主动发送识别记录到该地址 （记录支持断网续传）
（设置参数和获取参数接口中也同样有 platformEnable、platformIp字段，也可通过设置参数 ） 进行配置，但是 platformEnable参数的定义不一样，platformEnable=0为开
![id-656.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-656.png?sign=1766572729-e00f633f15-0-3836630daa42dc2ab8103482a6ed553790c0154a2ddcb16b098f8250debb9427)
推送格式如下：（必须要建立 http服务器才能接收数据，且确保关闭防火墙、杀毒软件、驱动 ） 管家类软件
（注意：为了防止刷卡上传图片异常，刷卡时设备延时 3秒上传记录）
示例
| 数据类型 | 字段名称 | 必传 | 说明 |
| --- | --- | --- | --- |
| String | id | Y | uuid |
| String | Mac_addr | Y | 设备唯一标识码 |
| String | time | Y | 比对时间 yyyy -MM - dd HH:mm:ss |
| String | devicename | N | 设备名称 |
| String | location | N | 安装位置 |
| int | inout | N | 出入 0出口   1入口 |
| String | employee_num ber | Y | 人员编号 |
| String | name | N | 姓名 |
| String | sex | N | 性别 |
| String | nation | N | 民族 |
| String | idNum | N | 身份证号 |
| String | icNum | N | IC卡号 |
| String | birthday | N | 出生年月如：1997 年 4月 3日 |
| String | telephone | N | 电话 |
| String | address | N | 身份证上住址信息 |
| String | depart | N | 签发机关 |
| String | validStart | N | 有效期开始 |
| String | validEnd | N | 有效期结束 |
| int | IdentifyType | Y | 识别方式（比对类型）：0人脸识别， 1 黑名单识别（预留字段），2人证比 对， 3 IC卡识别 |
| int | resultStatus | Y | 比对结果 1 比对成功 0 比对失败 |
| String | face_base64 | Y | 比对抓拍照片 base64位字符串 |
| String | templatePhoto | N | 模板照片 |
| String | temperature | N | 体温 |
| int | healthCode | N | 健康码类型 0 未开启 1绿码 2黄码 3 红码 |
| json | rna | N | 其他检测信息 |
| Int | rna.ret | N | 核酸检测结果 （0未开启，1 阴性，2 阳性） |
| String | rna.time | N | 核酸检测时间 |
| String | rna.src | N | 核酸检测机构 |
| String | .ret antiboby | N | 是否接种 0未接种，1已接种 |
| String | . antiboby time | N | 接种时间 yyyy-mm-dd hh:mm:ss |
| String | . antiboby src | N | 接种记录 |

示例
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
"sex" : "0",
"telephone" : "",
"face\_base64":"./9eksksm......d"
"idissue" : "",
"inout" : 1,
"location" : "Location",
"name" : "张三",
"nation" : "姹夋棌",
"resultStatus" : 1,
"rna" :
{
"ret" : 0,
"src" : "",
"stopoverCountyName" : "",
"time" : ""
},
推送：
{
"IdentifyType" : "0",
"Mac\_addr" : "0A:0C:XX:XX:9A:BD",
"SN" : "T2XXXXXXXXXXS",
"address" : "",
"antiboby" :
{
"ret" : 0,
"src" : "",
"time" : ""
},
"birthday" : "",
"depart" : "",
"devicename" : "Terminal",
"employee\_number" : "165123763275",
"healthCode" : 0,
"icNum" : "0000555555",
"id" : "1736233111",
"idNum" : "534444444444",
记录主动推送上传后，服务器每接收一条数据成功后应对设备回复:
1 {"message" :" ","result" : 0} ok
![id-736.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-736.png?sign=1766572729-f1aabc338e-0-b7cf27055dd1756d776edf3356b7d17ed9dc8fd141ad391ab328ef3204ea4900)
若不进行回复，设备将持续推送同一条记录。
如下为设备打印的日志，调试过程中可自行获取设备日志进行查看
### 2.6.2 重新提取识别结果
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| long | startTime | 记录的开始时间 |
| long | endTime | 记录的结束时间 |

接受消息应答数据内容
重新提取某个时间段内的所有记录，设备收到此命令后会把此时间段内的所有记录，一条 一条的重新上传到 platformIp的地址。
http://deviceAddress:port/s Url 地址： etDeviceRecordRevert
请求方式：post
Content-Type:application/json
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0成功;1,参数设置错误;2.密 |
|  |  | 码错误; |

| 1 | url：http://192.168.1.135:8091/setDeviceNetwork |
| --- | --- |
| 2 |  |
| 3 | 发送： |
| 4 | {"password":"123456","data":{"isDhcp":false,"ip":"192.168.1.136","gateway":"192.16 |
|  | 8.1.1","mask":"255.255.255.0","dns":"8.8.8.8"}} |
| 5 |  |
| 6 | 返回： |
| 7 | {"message" :"","result" : 0} |
| 8 |  |

示例
## 2.7  获取设备实时视频流
url：http://192.168.2.127:8091/setDeviceRecordRevert
发送消息：
{"password":"123456", "data":{"startTime":"2020-10-01
00:00:00","endTime":"2021-04-05 23:59:59"}}
接受消息：{"message":"Success","result":0}
系统默认的视频流输出为关闭状态，请通过以下接口修改视频流输出
T92K、T91K设备开启视频输出功能必须要开启每天定时重启，否则可能会导致设备重启
http://deviceAddress:port/s Url 地址： etDeviceParameter
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 表示当前消息体的数据 |
| int | VideoSwitch | 视频流开关 0开启视频流 2 关闭视频流 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密码错误； |

### 消息内容的结构见【获取设备参数】消息体的内容
### 接受消息应答数据内容
示例
1
2
3 发送：
4
5
6 返回：
7 {"message" :"","result" : 0}
8
url：http://192.168.1.135:8091/setDeviceNetwork
{"password":"123456","data":{"isDhcp":false,"ip":"192.168.1.136","gateway":"192.16
8.1.1","mask":"255.255.255.0","dns":"8.8.8.8"}}
| Url：http://192.168.1.135:8091/setDeviceParameter |
| --- |
| 发送消息：{"password":"123456","data":{" ":0}} VideoSwitch |
| 接受消息：{"message" : "Set Device Parameter Success","result" : 0} |

设备支持获取 RTSP视频流，测试获取方法
rtsp://ip:554/live/mainstream
![id-782.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-782.png?sign=1766572729-361fe84eb4-0-5dbafcf2dff32a78a9744dbf5582d9900666af5768b4f612faec59c7368de4a3)
![id-783.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-783.png?sign=1766572729-204cb0aa32-0-98673d9028a65ced7bddfbfff778eed0a56c52bfc40ab58a7e700f46a5300332)
### 测试方法：
1、安装 VLC 工具
2、打开 VLC
3、媒体-打开网络串流
输入 rtsp://ip:554/live/mainstream
播放即可。
## 2.8  其它消息
### 2.8.1  设置门禁时间段
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| List<WeekTime | TimeGroup |  |

WeekTime结构的定义
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | TimeGroupId | 时间组唯一 ID |
| List<DayTimes> | TimeGroup |  |

DayTimes结构的定义
功能说明：可为星期一到星期天，每天设置最多四个时间段为允许通行的时间段；每个时间组 有一个唯一 ID，与此 ID关联的人员则受此时间组限制。
(注意：有多个时间段时需要一起下发，不能分开下发)
http://deviceAddress:port/s Url 地址： etDeviceTimeAccessGroups
请求方式：post
Content-Type:application/json
发送消息的内容
data消息体的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | WeekIndex | 对应的星期:0星期一,1星期 二,2星期三,3星期四,4星 |
|  |  | 期五,5星期六,6星期日 |
| List<TimesBean> | Times |  |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | Start | 开始时间 格式例如：09:00 |
| String | End | 结束格式 例如：18:00 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 码错误； |

TimesBean结构的定义
接受消息应答数据内容
示例
1 url：http://192.168.1.135:8091/setDeviceNetwork
2
3 发送：
4
5
6 返回：
7 {"message" :"","result" : 0}
8
{"password":"123456","data":{"isDhcp":false,"ip":"192.168.1.136","gateway":"192.16
8.1.1","mask":"255.255.255.0","dns":"8.8.8.8"}}
"WeekIndex": "1", "Times": [{ "Start": "08:00", "End": "12:00" }, { "Start": "14:00", "End": "18:00" }, { "Start": "19:00", "End": "22:00" }]  }, { "WeekIndex": "2", url：http://192.168.2.127:8091/setDeviceTimeAccessGroups 发送消息： { "password": "123456", "data": { "TimeGroup": [{ "TimeGroupId": "1", "TimeGroup": [{ "WeekIndex": "0", "Times": [{ "Start": "08:00", "End": "12:00" }, { "Start": "14:00", "End": "18:00" }, { "Start": "19:00", "End": "22:00" }] }, {
|  | "Times": [{ "Start": "08:00", "End": "12:00" |
| --- | --- |
|  | }, { |
|  | "Start": "14:00", |
|  | "End": "18:00" |
|  | }, { |
|  | "Start": "19:00", |
|  | "End": "22:00" |
|  | }] |
| }, | { |
|  | "WeekIndex": "3", |
|  | "Times": [{ |
|  | "Start": "08:00", |
|  | "End": "12:00" |
|  | }, { |
|  | "Start": "14:00", |
|  | "End": "18:00" |
|  | }, { |
|  | "Start": "19:00", |
|  | }] "End": "22:00" |
| }, | { |
|  | "WeekIndex": "4", |
|  | "Times": [{ |
|  | "Start": "08:00", |
|  | "End": "12:00" |
|  | }, { |
|  | "Start": "14:00", |
|  | "End": "18:00" |
|  | }, { |
|  | "Start": "19:00", |
|  | "End": "22:00" }] |

|  | }, { "WeekIndex": "5", |
| --- | --- |
|  | "Times": [{ "Start": "08:00", "End": "23:00" |
|  | }] }, { "WeekIndex": "6", "Times": [{ "Start": "08:00", "End": "23:00" |
| }]          } } | }] }] 接受消息：{"message":"Success","result":0} |

### 2.8.2 设置特殊时间段
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 设备用户密码 |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| List<SpecialTimeGroupData> | SpecialTimeGroup |  |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | SpecialTimeGroupId | （小于等于 0，表示没有限 制通行） 特殊时间段 ID |
| List<TimesBean> | SpecialTimeGroupData |  |

TimesBean结构的定义
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| long | Start | 表示开始时间的毫秒数 |
| long | End | 表示结束时间的毫秒数 |
| int | PassStatus | 1 禁止 |

接受消息应答数据内容
可设置多个特殊时间段，用于设定特定的日期时间禁止通行
注意：系统优先判断特殊时间，控制通行
http://deviceAddress:port/s Url 地址： etDeviceAccessSpecialTimeGroup
请求方式：post
Content-Type:application/json
发送消息的内容
data消息体的内容
SpecialTimeGroupData结构的定义
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错 误；2,密码错误；3，用 户设置 password失败 |

示例
![id-840.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-840.png?sign=1766572729-45a4a1418b-0-8b856f8a0ff257ae30f4de37a07b7b2304b9010e6e6d5bc04755c09ecb0ea3c2)
![id-841.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-841.png?sign=1766572729-77455e3b0d-0-f805e4927b7a5b0cc15a5dccace2f12d28f371909781a84c8125c2acb442a66a)
"PassStatus": 1 }, { "Start": "1573270380000", "End": "1573270380000", "PassStatus": 1 }, { "Start": "1573439520000", "End": "1573439520000", "PassStatus": 1 }] }] } }
## 2.9 设备数据传输加密
加密此接口就是为了让系统和设备独家销售，避免与市场上的同厂家的设备串货。 此接口应用后，同样会对云协议的上传记录进行加密传输。
### 2.9.1  设备校验码写入
校验码写入不可逆，请勿轻易尝试
Url 地址：http://deviceAddress:port/ SetCheckCode
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 必填 | 说明 | 注意事项 |
| --- | --- | --- | --- | --- |
| String | Crc | Y | 校验码 |  |
| String | key | Y | 秘钥 | 当写入 key、iv字段后，设 备将采用 aes cbc模式 128 位 方式加密记录上传接口 的人员编号和比对时间 |
| String | iv | Y | 偏移量（长度小于 16 字节） |  |
| String | password | Y | 用户密码 默认 |  |

| 123456 |
| --- |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; |

接受消息应答数据内容
示例
1 2 3 4 5 6 7 8 url：http://192.168.1.135:8091/setDeviceNetwork 发送： {"Crc":"888888888","key":"222222222","iv":"2222222222","password":"123456"} 返回： {"message" :"","result" : 0}
### 2.9.2   获取设备校验码
请求方式：post
发送消息的内容
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | password | 用户密码默认123456 |

接受消息应答数据内容
Url 地址：http://deviceAddress:port/ GetCheckCode
| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | message | 提示消息 |
| int | result | 0 成功; 1,参数设置错误；2,密 |
|  |  | 码错误； |
| Object | data | 消息体 |

| 数据类型 | 字段名称 | 说明 |
| --- | --- | --- |
| String | SN | 设备唯一序列号 |
| String | Crc | 校验码 |

data消息体的内容
示例
1 2 3 4 5 6 7 8 url：http://192.168.1.135:8091/setDeviceNetwork 发送： {"password":"123456","data":{"isDhcp":false,"ip":"192.168.1.136","gateway":"192.16 8.1.1","mask":"255.255.255.0","dns":"8.8.8.8"}} 返回： {"message" :"","result" : 0}
# 3.0.  在线比对识别
用途：设备识别后推送数据到平台地址，识别判断后返回信息到设备进行开门和提示
在线接口可脱离整个局域网单独在运行，有配置工具可直接切换比对模式和配置在线比对地 址。
可以理解为此接口为一套单独的协议
请求方式
http post
设备请求参数：
| 参数 | 参数类型 | 是否必须 | 释义 |
| --- | --- | --- | --- |
| deviceId | string | 是 | 设备唯一标识 （mac） |
| dev_sno | string | 是 | 设备号（设备 MAC地址） |
| deviceSN | string | 是 | 设备 SN |
| token | string | 否 | token |
| type | string | 是 | 识别方式： type= stranger，代表陌生人 type=face，代表人脸识别； type=qr，代表扫码识别； type=id，代表人证比对； type=ic，代表刷卡比对； |
| value | string | 是 | 识别内容： 当 type= stranger时，value内容为固定字符 "data.capture" 当 type=face时，value内容为人员 id号码； 当 type=qr时，value内容为识别到的二维码内容字 符串； 当 type=id时，value内容为身份证识别到的身份证 号码； |
| temperature | string | 否 | 识别温度值，只是作为后台记录使用； |
| data | object | 否 | 数据 |
| capture | string |  | 现场抓拍照片，base64编码 （扫码无照片） |
| time | string |  | 识别时间, 时间戳精确到毫秒 |

请求示例：
{
"dev\_sno": "04:0C:F4:12:68:05",
"deviceld": "0A:0C:F4:12:68:05",
"devicesN": "T02F11601FS",
"temperature": "",
"token": "dc70ace460a00a093b5lefff3588d47b",
"type": "", "value": "", "data": { "capture": "", "datetime": "" } }
| 参数 | 参数类型 | 释义 |
| --- | --- | --- |
| status | int | 0:成功；1：失败（非 0 失败）。成功时开门，失败时不开门。 |
| msg | String | 提示信息。返回内容就显示，不返回就不显示。 |
| audio | String | 语音（需使用智能语音模块，语音模块接串口 2，此字段不适用于 T12） |

平台返回参数：
{ "status": 0, "msg": "识别成功", "audio": "识别成功" }
提示：需要智能语音时，需要设置语音控制模式为 【自定义语音】 才能确保播的是后台返回 的语音，只要开启就可以，后面的自定义语音内容不重要。



示例
![id-891.png](https://space-static.coze.site/coze_space/7576239489933183966/upload/id-891.png?sign=1766572729-da20671f83-0-4da3648aca4d1fd3db2deca2356ce8e6dd9bf8c774a9e41a4191edb94c37e28c)
# 云协议
如局域网协议不适用，请查看云协议
https://docs.qq.com/doc/DRmlpeUNzbk9MbU1C 
