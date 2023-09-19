### logback-dendrobe-diamond

[![Build](https://github.com/CharLemAznable/logback-dendrobe-diamond/actions/workflows/build.yml/badge.svg)](https://github.com/CharLemAznable/logback-dendrobe-diamond/actions/workflows/build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.charlemaznable/logback-dendrobe-diamond/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.charlemaznable/logback-dendrobe-diamond/)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
![GitHub code size](https://img.shields.io/github/languages/code-size/CharLemAznable/logback-dendrobe-diamond)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=alert_status)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=bugs)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=security_rating)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=sqale_index)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=code_smells)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=ncloc)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=coverage)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=CharLemAznable_logback-dendrobe-diamond&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=CharLemAznable_logback-dendrobe-diamond)

使用Diamond热更新[logback-dendrobe](https://github.com/CharLemAznable/logback-dendrobe)配置.

##### Maven Dependency

```xml
<dependency>
  <groupId>com.github.charlemaznable</groupId>
  <artifactId>logback-dendrobe-diamond</artifactId>
  <version>2023.2.2</version>
</dependency>
```

##### Maven Dependency SNAPSHOT

```xml
<dependency>
  <groupId>com.github.charlemaznable</groupId>
  <artifactId>logback-dendrobe-diamond</artifactId>
  <version>2023.2.3-SNAPSHOT</version>
</dependency>
```

#### 本地配置需要读取的Diamond配置坐标

在本地类路径默认配置```logback-dendrobe.properties```文件中, 添加如下配置:

```
logback.diamond.group=XXX
logback.diamond.dataId=YYY
```

即指定使用Diamond配置```group:XXX dataId:YYY```热更新logback-dendrobe配置.

```logback.diamond.group```配置默认值: Logback
```logback.diamond.dataId```配置默认值: default

#### 使用Diamond配置logback-dendrobe数据库日志的Eql连接

当配置数据库日志为```{logger-name}[eql.connection]=XXX```时, 读取Diamond配置```group:EqlConfig dataId:XXX```作为Eql连接配置.

#### 使用Diamond配置logback-dendrobe Vert.x日志的Vert.x实例

当配置Vert.x日志为```{logger-name}[vertx.name]=XXX```时, 读取Diamond配置```group:VertxOptions dataId:XXX```作为Vert.x实例配置.

#### 使用Diamond配置logback-dendrobe ElasticSearch日志的es客户端

当配置ElasticSearch日志为```{logger-name}[es.name]=XXX```时, 读取Diamond配置```group:EsConfig dataId:XXX```作为es客户端配置.
