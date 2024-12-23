![image](https://github.com/user-attachments/assets/6255ff68-7b54-481f-a237-833588a6ab46)# 3D 멀티플레이어 FPS 게임

LibGDX와 Kryonet을 사용하여 개발된 3D 멀티플레이어 FPS 게임입니다. Counter-Strike에서 영감을 받아 팀 기반 게임플레이와 폭탄 설치/해체 메커니즘을 구현했습니다.

## 주요 기능

### 게임플레이
- 3D 일인칭 슈팅 게임플레이
- 테러리스트와 대테러리스트 팀 대전
- 라운드 기반 게임 시스템
- 폭탄 설치 및 해체 메커니즘
- 탈락한 플레이어를 위한 관전 모드

### 무기 시스템
- 다양한 무기 종류 (권총, AK47, SG553, AR15, 단검)
- 상점 시스템과 게임 내 경제 시스템
- 실제같은 무기 메커니즘
  - 반동 시스템
  - 탄약 관리
  - 신체 부위별 데미지
  - 무기 교체 시스템

### 기술적 특징
- LibGDX를 사용한 3D 그래픽 렌더링
- ODE4J를 활용한 물리 시뮬레이션
- Kryonet 기반 네트워크 통신
- 클라이언트 예측 및 보간
- 총알 궤적 및 충돌 시스템
- 애니메이션 시스템
- 사운드 시스템

## 개발 환경

### 필수 요구사항
- Java 8 이상
- LibGDX
- Kryonet
- ODE4J

### 실행 방법

#### 서버 실행
```bash
# ServerLauncher.java 실행
```

#### 클라이언트 실행
```bash
# Gradle로 빌드
./gradlew build

# Lwjgl3Launcher.java 실행
```

## 조작 방법

- WASD - 이동
- 마우스 - 시점 이동
- 좌클릭 - 발사
- R - 재장전
- C - 단발/연발 변경
- B - 상점 메뉴
- ESC - 설정 메뉴
- 숫자키 1~3 - 빠른 무기 변경
- F1 - 디버그 화면
- F2 - 시점 변경 (1인칭/3인칭)

## 네트워크 설정

기본 네트워크 설정:
- 기본 호스트: 서버 IP
- TCP 포트: 14150
- UDP 포트: 18473

## 프로젝트 구조

주요 패키지 구조:
```
src/
├── main/
│   └── java/
│       └── com/
│           └── monstrous/
│               └── tut3d/
│                   ├── client/        # 클라이언트 관련 코드
│                   ├── server/        # 서버 관련 코드
│                   ├── net/           # 네트워크 통신
│                   ├── physics/       # 물리 엔진
│                   ├── gui/           # 사용자 인터페이스
│                   └── effects/       # 시각 효과
```

## 실행 화면

![image](https://github.com/user-attachments/assets/4a18db63-8422-4c1b-b3c3-4f446bffc47c)

## 라이선스

MIT License

## 사용한 자원

- LibGDX 프레임워크
- Kryonet 네트워킹 라이브러리
- ODE4J 물리 엔진
- [기타 크레딧]

## 문의사항

lesw1012@gmail.com
