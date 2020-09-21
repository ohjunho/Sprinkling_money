# Sprinkling_money

<h3>문제 해결 전략</h3>
- Scale in/out에 구애 받지 않는 구조 구현 
- Non-block, 비동기 통신
- 데이터가 인스턴스 내에서 소요하는 시간을 최소화(동작 시간 단축 목표)
- vert.x를 사용해 클러스터링/HA 대응 및 서버 내 인스턴스 묶음 관리 (같은 IP로 다수 인스턴스 운용)

<h5>서비 아키텍처</h5>
![아키텍처](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/architecture.png)

<h5>시퀀스</h5>
![뿌리기](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/sprinkling.png)


![받기](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/receive.png)


![조회](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/retrieve.png)


----------------------------------------

* 시간이 되면 아래도 적용한다...
- IMDG를 통한 TTL 관
- Kafka를 통한 처리 결과 저장 (데이터 소실 방지, 데이터베이스 접근 최소화)
- SSL은 라우터에 설치하는 것으로 간주하여 추가하지 않음 (key/cert 파일 필요)

<h5>MSA 적용</h5>리
```
API GW (Kong) --- Server Cluster
     |                  |
     -------------------
             |
           consul
```  


---------------------------------
<b>hsql 실행</b> 
java -classpath lib/hsqldb-2.5.1.jar org.hsqldb.server.Server