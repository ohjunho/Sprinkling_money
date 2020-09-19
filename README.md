# Sprinkling_money

<h3>문제 해결 전략</h3>
- Scale in/out에 구애 받지 않는 구조 구현 
- 비동기 통신을 통해 데이터가 인스턴스 내에서 소요하는 시간을 최소화(동작 시간 단축 목표)
- IMDG를 내장한 인스턴스를 사용해 클러스터링/HA 대응
- Kafka를 통한 처리 결과 저장 (데이터 소실 방지, 데이터베이스 접근 최소화)
- 스케줄링은 트리거 방식을 통해 간략하게 구현 
- vert.x를 통해 서버 내 인스턴스 묶음 관리 (같은 IP로 다수 인스턴스 운용)

<h5>서비 아키텍처</h5>
![아키텍처](https://github.com/ohjunho/Sprinkling_money/blob/master/image/%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98.png?raw=true)

<h5>시퀀스</h5>
![뿌리기](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/%EB%BF%8C%EB%A6%AC%EA%B8%B0.png)


![받기](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/%EB%B0%9B%EA%B8%B0.png)


![조회](https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/%EC%A1%B0%ED%9A%8C.png)


----------------------------------------

* 시간이 되면 아래도 적용한다...

<h5>MSA 적용</h5>
```
API GW (Kong) --- Server Cluster
     |                  |
     -------------------
             |
           consul
```  
