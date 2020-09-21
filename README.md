# Sprinkling_money

<h3>문제 해결 전략</h3>
- Scale in/out에 구애 받지 않는 구조 구현 
- Non-block, 비동기 통신을 통해 데이터가 인스턴스 내에서 소요하는 시간을 최소화(동작 시간 단축 목표)
- IMDG를 내장한 인스턴스를 사용해 클러스터링/HA 대응 
- vert.x를 통해 서버 내 인스턴스 묶음 관리 (같은 IP로 다수 인스턴스 운용)

<h5>서비 아키텍처</h5>

<p><a target="_blank" rel="noopener noreferrer" href="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/architecture.png"><img src="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/architecture.png" alt="아키텍처" style="max-width:100%;"></a></p>

<h5>시퀀스</h5>

<p><a target="_blank" rel="noopener noreferrer" href="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/sprinkling.png"><img src="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/sprinkling.png" alt="뿌리기" style="max-width:100%;"></a></p>

<p><a target="_blank" rel="noopener noreferrer" href="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/receive.png"><img src="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/receive.png" alt="받기" style="max-width:100%;"></a></p>

<p><a target="_blank" rel="noopener noreferrer" href="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/retrieve.png"><img src="https://raw.githubusercontent.com/ohjunho/Sprinkling_money/master/image/retrieve.png" alt="조회" style="max-width:100%;"></a></p>

----------------------------------------

* 시간이 되면 아래도 적용한다...

- Kafka를 통한 처리 결과 저장 (데이터 소실 방지, 데이터베이스 접근 최소화)

<h5>MSA 적용</h5>

API GW (Kong) --- Server Cluster

     |                  |

     -------------------

             |

           consul

