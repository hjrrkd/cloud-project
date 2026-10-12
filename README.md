# HTCondor Workload-based AWS EC2 Auto Scaling

HTCondor의 작업 부하에 따라 AWS EC2 인스턴스를 자동으로 확장·축소하는 프로젝트입니다.

AWS Systems Manager(SSM)를 통해 HTCondor의 작업 큐와 슬롯 상태를 원격으로 수집하고,
수집한 정보를 기준으로 EC2 인스턴스의 Scale Out / Scale In을 수행하도록 구현했습니다.

## 주요 기능

### EC2 인스턴스 관리
- AWS SDK 기반 EC2 인스턴스 조회, 시작, 중지, 생성, 재부팅
- 사용 가능한 Region 및 Availability Zone 조회
- EC2 이미지(AMI) 조회

### HTCondor 상태 수집
- AWS SSM을 활용해 SSH 접속 없이 원격 명령 실행
- `condor_status`를 통한 슬롯 상태 확인
- `condor_q`를 통한 작업 큐 상태 확인
- Shell Script를 통해 슬롯 수, 작업 수, 작업이 배치된 노드 정보 수집

### Auto Scaling
HTCondor의 현재 슬롯 수와 작업 수를 비교하여 EC2 인스턴스를 자동으로 조정합니다.

- `슬롯 수 < 작업 수` → EC2 인스턴스 생성 (Scale Out)
- `슬롯 수 > 작업 수` → 불필요한 EC2 인스턴스 중지 (Scale In)
- `슬롯 수 = 작업 수` → 현재 상태 유지

### 안전한 Scale In
Scale In 과정에서 실행 중인 작업이 중단되지 않도록,
HTCondor에서 작업이 배치된 노드의 hostname을 확인하고 해당 노드를 축소 대상에서 제외했습니다.

## Tech Stack

- AWS EC2
- AWS Systems Manager (SSM)
- AWS IAM
- AWS SDK
- HTCondor
- Shell Script

## Repository

- `main` : Auto Scaling Script, HTCondor Job File
- `master` : AWS EC2 관리 및 Auto Scaling 프로그램
