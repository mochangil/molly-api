**패션을 쉽게 MollyMol**
---


### **📽️ 시연 영상**
https://drive.google.com/file/d/13Jez_t-zlCY9VO-dskcfzjaCsl4pcMxr/view

<br>

### **🔖 프로젝트 개요**

---

- 의류 전자상거래 플랫폼
- My Outfit Lifts You! Molly

<br>

### **🎯 목표**
  - **트래픽 급증에도 안정적인 애플리케이션 개발** : 대용량 트래픽 유입 상황에서 상품 등록, 구매, 리뷰 등의 핵심 기능이 문제없이 작동하도록 설계
  - **신뢰를 주는 전자상거래 플랫폼 구축** : 실제 거래가 발생하는 서비스인 만큼, 데이터 정합성과 무결성을 철저히 보장하는 시스템 개발  

<br>



<br>

### 🧑‍🤝‍🧑 팀 역할

---

![image](https://github.com/user-attachments/assets/97fbfff2-ec90-4350-952c-7050890ae2a7)

<br>

### **📚 기술 스택**

---

![image](https://github.com/user-attachments/assets/e4fbaec5-dde3-48b4-9706-fba3f6fc3f36)

<br>

### **🌏 서버 아키텍쳐**

---

![image](https://github.com/user-attachments/assets/f4ff3c43-79f9-47f4-978a-34aab6a6b43f)

<br>

### 🗺️ ERD

---

![image](https://github.com/user-attachments/assets/d9ddd0fa-e303-4643-a723-b88219edd958)

<br>

<br>

### **🔗주요 기능**

---

**1️⃣ 회원가입/ 로그인**

- 이메일을 통해 회원 가입 및 로그인을 할 수 있습니다.
- 마이페이지에서 회원 정보를 관리할 수 있습니다.

**2️⃣ 상품 관리**

- 상품 검색 및 필터링 (필터: 카테고리, 색상, 사이즈, 가격 등/정렬: 최신 순, 조회 많은 순, 구매 많은 순  등)
- 상품 상세 페이지 - 이미지, 설명, 리뷰, 재고, 배송 정보 표시
- 상품 등록 -  상품 단건 등록, 상품 다건 (100건 이상부터 1,000,000건 이하) 엑셀로 등록 가능
- 상품 수정 및 삭제

3️⃣ **장바구니**

- 사용자는 상품을 장바구니에 담을 수 있습니다.
- 장바구니에서 옵션 및 수량 변경할 수 있습니다.
- 장바구니 전체 선택, 전체 삭제 할 수 있습니다.

4️⃣ **결제**

- 토스 API를 통해 사용자는 결제를 진행할 수 있습니다.
- 포인트를 사용하여 결제 할 수 있습니다.

**5️⃣ 주문 및 배송 관리**

- 자신의 기본 배송지 설정 및 추가, 변경, 삭제 할 수 있습니다.
- 마이페이지에서 사용자의 주문 내역을 조회할 수 있습니다.

**6️⃣ 리뷰**

- 사용자는 배송완료된 상품의 리뷰를 작성할 수 있습니다.
- 사용자는 리뷰 작성, 수정, 삭제 할 수 있습니다.
- 리뷰에 대한 좋아요 기능을 제공합니다.
- 최근 7일간 누적 좋아요가 많은 인기순위 Top 12를 조회할 수 있습니다.

<br>

### **🌈 개선 사항**

---

  #### 1️⃣ 주문 API 응답속도 개선 - 응답 시간 약 75% 감소
  </br>

  **문제 상황**
  - 주문 결제시 배송, 재고, 결제 등을 순차적으로 처리하고 있는데 동시에 많은 요청이 몰릴 경우 성능이 급격하게 저하됨

  **1. 단일 트랜잭션 -> 분리 트랜잭션**
  - 단일 트랜잭션으로 진행하던 작업을 작업별로 새로운 트랜잭션(@Transactional(REQUIRES_NEW))으로 분리
  - 단일 요청에 대한 응답시간은 개선되었지만, 하나의 스레드가 여러 커넥션을 요구하며 HikariCP DeadLock 발생되는 것을 확인

  **2. 순차처리 -> 부분 비동기 병렬처리**
  - 메인 트랜잭션이 커넥션을 반환하는 시점이 다른 작업 트랜잭션에 의존하게 되면서 트랜잭션 분리의 이점이 전혀 없어짐
  - 각 트랜잭션이 완전히 독립적으로 커밋을 수행하도록 비동기 병렬 처리 구조 도입
  - 각 이벤트의 EventListner를 @Async 비동기 병렬 수행하도록 하고, 각 이벤트의 완료는 ConcurrentHashMap에서 완료여부 추적
  - Orchestration Saga 패턴의 모양을 띄고 있으며, 이벤트 발행 주체는 orderService에서 수행

  **3. 완전 비동기 병렬처리 (작업 중)**
  - 하나의 주문-결제 요청 api로 요청과 응답을 동시에 처리하려면 순차처리가 동반되어야 하기에, 주문과 응답을 2 phase로 나누어 구조 개선
  - Kafka와 같은 외부 메시지 큐를 사용하여 처리량 개선
  - Choreography Saga 패턴의 모양을 띄고 있으며, 이벤트 발행 주체는 전 이벤트리스너가 수행
  


  **결과**  
  (BlockingQueue를 통해 이벤트 소비 흐름을 제어할 때, 이벤트를 소비하는 컨슈머 스레드 갯수를 1개와 4개로 바꾸어가며 비교 진행) 
  - 응답시간 개선
  <img width="695" alt="image" src="https://github.com/user-attachments/assets/819aced4-b5f0-4064-9988-df73e30f335d" />

  - 에러율 개선 (구조적 HikariCP DeadLock 문제 해결)
  <img width="381" alt="image" src="https://github.com/user-attachments/assets/f3d961aa-5a5e-406d-8331-f0334fb7b4b1" />
  </br>

  #### 2️⃣ 재고차감 개선 - 응답속도 약 30% 개선

  </br>

  **문제 상황**
  - 동시에 같은 상품의 재고를 차감할 때, 기존에는 동시성 문제를 방지하기 위해 Jpa의 Pessimistic_Lock을 사용했으나 많은 요청이 몰리게 될 경우 응답속도와 처리량이 급격히 저하됨
  - 재고 차감이 비동기로 분리되어도 DB 내 row-level lock으로 병목이 발생

  **1. 비관적 락**
  - 트랜잭션 종료시까지 레코드를 점유하는 방식으로, 충돌을 예방하는 대신 처리량 감소
  - 요청이 몰리면 lock 대기시간이 증가하며, 전체 재고 시스템의 병목으로 이어짐

  **2. 낙관적 락**
  - 충돌이 실제로 발생한 경우에만 예외를 발생하여 동시성을 제어
  - 재시도 로직이 빈번하게 실행되며 성능 개선 효과가 미미함

  **3. 캐시 연산과 배치 업데이트**
  - 재고 연산을 RDB 대신 Redis에서 수행하고, DB 반영은 일정 주기로 일괄 처리하는 Look-aside + Write-back 캐시 전략 적용
  - 싱글 스레드로 동작하는 Redis 특성을 살려 Lua Script를 통한 원자적 연산으로 동시성 문제 해결

  **결과**  
  
  Jmeter 쓰레드 그룹 설정  
  <img width="458" alt="image" src="https://github.com/user-attachments/assets/2649c6bb-970f-4111-999f-266ff07fbbfd" />  
  
  비관적 락 vs 캐싱 전략  
  <img width="366" alt="image" src="https://github.com/user-attachments/assets/0949d903-7c83-45f4-8c83-06a518e8383e" />

  </br>

</br>

### 🚀 트러블 슈팅

---

1. AOP self-invocation
2. retryable의 custom exception
3. tossPayment의 정보 암호화
4. 주문-결제 프로세스의 재고 선차감 및 후차감 로직
5. 이벤트핸들러의 pointCut으로 공통작업 registerFuture와 completeFuture를 간단하게 리팩토링


