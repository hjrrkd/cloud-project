package org.example;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.SendCommandRequest;
import software.amazon.awssdk.services.ssm.model.SendCommandResponse;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationRequest;
import software.amazon.awssdk.services.ssm.model.GetCommandInvocationResponse;
import software.amazon.awssdk.services.ssm.model.CommandInvocationStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;



public class Main {


    private static Ec2Client ec2;
    private static SsmClient ssm;

    private static void init() {/*throws Exception*/
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();


        // 리전을 ap-northeast-2로 고정
        Region region = Region.AP_NORTHEAST_2;

        // EC2 클라이언트 초기화
        ec2 = Ec2Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        // SSM 클라이언트 초기화
        ssm = SsmClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

    }

    public static void main(String[] args) throws Exception {
        init();

        Scanner menu = new Scanner(System.in);
        Scanner idString = new Scanner(System.in);
        int number = 0;

        while (true) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println("           Amazon AWS Control Panel using SDK               ");
            System.out.println("------------------------------------------------------------");
            System.out.println("  1. list instance                2. available zones        ");
            System.out.println("  3. start instance               4. available regions      ");
            System.out.println("  5. stop instance                6. create instance        ");
            System.out.println("  7. reboot instance              8. list images            ");
            System.out.println("  9. condor_status               10. AutoScaling            ");
            System.out.println(" 11. condor_q                    99. quit                   ");
            System.out.println("------------------------------------------------------------");

            System.out.print("Enter an integer: ");
            if (menu.hasNextInt()) {
                number = menu.nextInt();
            } else {
                System.out.println("Invalid input!");
                break;
            }

            String instanceId = "";
            String amiId = "";

            switch (number) {
                case 1:
                    listInstances();
                    break;
                case 2:
                    availableZones();
                    break;
                case 3:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) startInstance(instanceId);
                    break;
                case 4:
                    availableRegions();
                    break;
                case 5:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) stopInstance(instanceId);
                    break;
                case 6:
                    System.out.print("Enter ami id: ");
                    amiId = idString.nextLine().trim();
                    if (!amiId.isEmpty()) createInstance(amiId);
                    break;
                case 7:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) rebootInstance(instanceId);
                    break;
                case 8:
                    listImages();
                    break;
                case 9:
                    executeCondorStatus();
                    break;
                case 10:
                    autoScaling();
                    break;
                case 11:
                    executeCondorQ();
                    break;
                case 99:
                    System.out.println("Exiting...");
                    menu.close();
                    idString.close();
                    return;

                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    public static void listInstances() {
        System.out.println("Listing instances...");

        try (Ec2Client ec2 = Ec2Client.create()) { // AWS SDK v2는 클라이언트를 자동 닫도록 try-with-resources를 사용
            String nextToken = null;

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .nextToken(nextToken) // 토큰 설정
                        .build();

                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        System.out.printf(
                                "[id] %s, [AMI] %s, [type] %s, [state] %10s, [monitoring state] %s\n",
                                instance.instanceId(),
                                instance.imageId(),
                                instance.instanceType(),
                                instance.state().name(),
                                instance.monitoring().stateAsString());
                    }
                }

                nextToken = response.nextToken();
            } while (nextToken != null); // 다음 토큰이 null이 아니면 계속 루프
        } catch (Exception e) {
            System.err.println("Failed to list instances: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void availableZones() {
        System.out.println("Available zones...");

        try (Ec2Client ec2 = Ec2Client.create()) { // 클라이언트 생성
            // 요청 객체 생성
            DescribeAvailabilityZonesRequest request = DescribeAvailabilityZonesRequest.builder().build();

            // 응답 받기
            DescribeAvailabilityZonesResponse response = ec2.describeAvailabilityZones(request);

            // 가용 영역 출력
            for (AvailabilityZone zone : response.availabilityZones()) {
                System.out.printf("[Zone] %s, [Region] %s, [Zone Name] %s\n",
                        zone.zoneId(), zone.regionName(), zone.zoneName());
            }
        } catch (Exception e) {
            System.err.println("Failed to list availability zones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void startInstance(String instanceId) {
        System.out.printf("Starting instance: %s\n", instanceId);

        try (Ec2Client ec2 = Ec2Client.create()) { // 클라이언트 생성
            // 요청 객체 생성
            StartInstancesRequest request = StartInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            // 인스턴스 시작 요청
            StartInstancesResponse response = ec2.startInstances(request);

            // 성공 메시지 출력
            System.out.printf("Successfully started instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to start instance: %s\n", e.getMessage());
        }
    }

    public static void stopInstance(String instanceId) {
        System.out.printf("Stopping instance: %s\n", instanceId);

        try (Ec2Client ec2 = Ec2Client.create()) { // 클라이언트 생성
            // 요청 객체 생성
            StopInstancesRequest request = StopInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            // 인스턴스 중지 요청
            StopInstancesResponse response = ec2.stopInstances(request);

            // 성공 메시지 출력
            System.out.printf("Successfully stopped instance: %s\n", instanceId);
            System.out.printf("Current state: %s\n",
                    response.stoppingInstances().get(0).currentState().name());
        } catch (Exception e) {
            System.out.printf("Failed to stop instance: %s\n", e.getMessage());
        }
    }

    public static void createInstance(String amiId) {
        System.out.printf("Creating instance in region: ap-southeast-2 with AMI: %s\n", amiId);

        try (Ec2Client ec2 = Ec2Client.create()) { // 클라이언트 생성
            // 요청 객체 생성
            RunInstancesRequest request = RunInstancesRequest.builder()
                    .imageId(amiId)
                    .instanceType(InstanceType.T2_MICRO) // 인스턴스 유형 설정
                    .minCount(1) // 최소 인스턴스 개수
                    .maxCount(1) // 최대 인스턴스 개수
                    .build();

            // 인스턴스 생성 요청
            RunInstancesResponse response = ec2.runInstances(request);

            // 생성된 인스턴스 ID 가져오기
            String instanceId = response.instances().get(0).instanceId();
            System.out.printf("Successfully created instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to create instance: %s\n", e.getMessage());
        }
    }

    public static void rebootInstance(String instanceId) {
        System.out.printf("Rebooting instance: %s\n", instanceId);

        try (Ec2Client ec2 = Ec2Client.create()) { // 클라이언트 생성
            // 요청 객체 생성
            RebootInstancesRequest request = RebootInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            // 인스턴스 재부팅 요청
            ec2.rebootInstances(request);

            // 성공 메시지 출력
            System.out.printf("Successfully rebooted instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to reboot instance: %s\n", e.getMessage());
        }
    }

    public static void availableRegions() {
        System.out.println("Available regions...");

        // DescribeRegionsRequest 생성
        DescribeRegionsRequest request = (DescribeRegionsRequest) DescribeRegionsRequest.builder().build();

        try {
            // EC2의 지역 정보 가져오기
            DescribeRegionsResponse result = ec2.describeRegions(request);

            // 지역 및 엔드포인트 출력
            for (software.amazon.awssdk.services.ec2.model.Region region : result.regions()) {
                System.out.printf("[Region] %s, [Endpoint] %s\n",
                        region.regionName(), region.endpoint());
            }
        } catch (Exception e) {
            // 예외 처리
            System.out.printf("Failed to fetch regions: %s\n", e.getMessage());
        }
    }


    public static void listImages() {
        System.out.println("Listing images....");

        try (Ec2Client ec2 = Ec2Client.create()) { // 클라이언트 생성
            // 필터 설정
            Filter filter = Filter.builder()
                    .name("name")
                    .values("aws-htcondor-worker")
                    .build();

            // 요청 객체 생성
            DescribeImagesRequest request = DescribeImagesRequest.builder()
                    .filters(filter)
                    .build();

            // 이미지 목록 요청
            DescribeImagesResponse response = ec2.describeImages(request);

            // 이미지 정보 출력
            for (Image image : response.images()) {
                System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
                        image.imageId(), image.name(), image.ownerId());
            }
        } catch (Exception e) {
            System.out.printf("Failed to list images: %s\n", e.getMessage());
        }
    }

    public static void executeCondorStatus() {
        String instanceId = "i-0e2da5fd0cf96ff18"; // 실행할 EC2 인스턴스 ID
        String command = "condor_status";

        try (SsmClient ssmClient = SsmClient.create()) {
            // SSM 명령 요청
            Map<String, List<String>> parameters = new HashMap<>();
            parameters.put("commands", Arrays.asList(command));

            SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                    .instanceIds(instanceId)
                    .documentName("AWS-RunShellScript") // Shell 명령 실행
                    .parameters(parameters)  // 수동으로 만든 parameters 맵 사용
                    .build();

            // 명령 실행
            SendCommandResponse sendCommandResponse = ssmClient.sendCommand(sendCommandRequest);
            String commandId = sendCommandResponse.command().commandId();

            // 결과 조회를 위한 대기
            Thread.sleep(5000); // 명령 실행 완료를 기다림 (필요에 따라 조정)

            // 명령 결과 요청
            GetCommandInvocationRequest invocationRequest = GetCommandInvocationRequest.builder()
                    .commandId(commandId)
                    .instanceId(instanceId)
                    .build();

            GetCommandInvocationResponse invocationResponse = ssmClient.getCommandInvocation(invocationRequest);

            // 명령 결과 출력
            System.out.println("Command Output:");
            System.out.println(invocationResponse.standardOutputContent());

        } catch (Exception e) {
            System.out.printf("Failed to execute condor_status on instance %s: %s\n", instanceId, e.getMessage());
            e.printStackTrace();
        }
    }

    public static void executeCondorQ() {
        String instanceId = "i-0e2da5fd0cf96ff18"; // EC2 인스턴스 ID
        String command = "condor_q";

        try (SsmClient ssmClient = SsmClient.create()) {
            // SSM 명령 요청
            Map<String, List<String>> parameters = new HashMap<>();
            parameters.put("commands", Arrays.asList(command));

            SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                    .instanceIds(instanceId)
                    .documentName("AWS-RunShellScript") // Shell 명령 실행
                    .parameters(parameters)
                    .build();

            // 명령 실행
            SendCommandResponse sendCommandResponse = ssmClient.sendCommand(sendCommandRequest);
            String commandId = sendCommandResponse.command().commandId();

            // 결과 조회를 위한 대기
            Thread.sleep(5000); // 명령 실행 완료를 기다림 (필요에 따라 조정)

            // 명령 결과 요청
            GetCommandInvocationRequest invocationRequest = GetCommandInvocationRequest.builder()
                    .commandId(commandId)
                    .instanceId(instanceId)
                    .build();

            GetCommandInvocationResponse invocationResponse = ssmClient.getCommandInvocation(invocationRequest);

            // 명령 결과 출력
            String output = invocationResponse.standardOutputContent();
            System.out.println("Command Output:");
            System.out.println(output);

            // 결과에 맞는 형태로 출력
            if (output.contains("Total for query: 0 jobs")) {
                System.out.println("No jobs are currently in the queue.");
            }

        } catch (Exception e) {
            System.out.printf("Failed to execute condor_q on instance %s: %s\n", instanceId, e.getMessage());
            e.printStackTrace();
        }
    }

    /*private static void sendSSMCommand(String instanceId, String command) throws Exception {
        System.out.println("Executing command on instance " + instanceId);

        // 명령어를 리스트로 생성
        List<String> commands = new ArrayList<>();
        commands.add(command);

        // 파라미터를 HashMap으로 설정
        HashMap<String, List<String>> parameters = new HashMap<>();
        parameters.put("commands", commands);

        // SendCommandRequest 생성
        SendCommandRequest ssmRequest = SendCommandRequest.builder()
                .instanceIds(instanceId) // EC2 인스턴스 ID
                .documentName("AWS-RunShellScript") // 사용할 SSM 문서
                .parameters(parameters) // 명령어 파라미터
                .build();

        // 명령어 실행
        SendCommandResponse ssmResponse = ssm.sendCommand(ssmRequest);

        // 실행된 명령어의 commandId 출력
        System.out.println("Command executed with command ID: " + ssmResponse.command().commandId());
    }*/


    public static void autoScaling() {
        System.out.println("Starting auto-scaling...");

        try {
            // SSM을 통해 쉘 스크립트 실행
            String scriptResult = executeSSMScript("/home/ec2-user/autoscaling.sh");  // SSM 경로로 변경
            String[] results = scriptResult.split("\n");

            int slots = Integer.parseInt(results[0].trim());
            int jobs = Integer.parseInt(results[1].trim());
            List<String> assignedNodes = Arrays.asList(results[2].trim().split(","));

            // 스케일 아웃: 잡이 슬롯보다 많으면 인스턴스 생성
            if (jobs > slots) {
                System.out.println("Scale out: Creating a new instance...");
                createInstance("ami-064d68c52313d6d29"); // worker 이미지 기반 인스턴스 생성
            }
            // 스케일 인: 슬롯보다 잡이 적으면 불필요한 인스턴스 종료
            else if (jobs < slots) {
                System.out.println("Scale in: Stopping unnecessary instances...");
                scaleInInstances(assignedNodes);  // scaleInInstances는 리스트에서 인스턴스를 종료하는 함수
            }
            // 시스템 안정: 스케일링 필요 없음
            else {
                System.out.println("System stable: No scaling required.");
            }
        } catch (Exception e) {
            System.out.println("Error in auto-scaling: " + e.getMessage());
        }
    }

    private static String executeSSMScript(String scriptPath) {
        String instanceId = "i-0e2da5fd0cf96ff18"; // EC2 인스턴스 ID
        try {
            // SSM 명령 요청
            SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                    .instanceIds(instanceId)
                    .documentName("AWS-RunShellScript")
                    .parameters(Collections.singletonMap("commands", Collections.singletonList("bash " + scriptPath)))
                    .build();

            SendCommandResponse result = ssm.sendCommand(sendCommandRequest);
            String commandId = result.command().commandId(); // 명령 ID 추출

            // 결과를 기다리고 반환
            return getSSMCommandResult(commandId, instanceId);
        } catch (Exception e) {
            System.err.println("Failed to execute script: " + e.getMessage());
            return "";
        }
    }

    private static String getSSMCommandResult(String commandId, String instanceId) {
        try {
            // 최대 5분 동안 결과를 기다림 (필요에 따라 조정 가능)
            long startTime = System.currentTimeMillis();
            while (true) {
                // 명령 실행 상태 조회
                GetCommandInvocationRequest request = GetCommandInvocationRequest.builder()
                        .commandId(commandId)
                        .instanceId(instanceId)
                        .build();

                GetCommandInvocationResponse response = ssm.getCommandInvocation(request);

                if (response.status() == CommandInvocationStatus.SUCCESS) {
                    return response.standardOutputContent(); // 성공 시 출력
                } else if (response.status() == CommandInvocationStatus.FAILED) {
                    System.err.println("Command failed: " + response.standardErrorContent());
                    return "";
                }

                // 명령이 끝날 때까지 기다림
                if (System.currentTimeMillis() - startTime > TimeUnit.MINUTES.toMillis(5)) {
                    System.err.println("Command timeout after 5 minutes.");
                    return "";
                }

                TimeUnit.SECONDS.sleep(1); // 1초 대기 후 재시도
            }
        } catch (Exception e) {
            System.err.println("Error while fetching command result: " + e.getMessage());
            return "";
        }
    }

    private static void scaleInInstances(List<String> assignedNodes) {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .filters(Filter.builder()
                        .name("instance-state-name")
                        .values("running")
                        .build())
                .build();

        DescribeInstancesResponse describeInstancesResponse = ec2.describeInstances(describeInstancesRequest);

        List<Instance> runningInstances = new ArrayList<>();
        for (Reservation reservation : describeInstancesResponse.reservations()) {
            runningInstances.addAll(reservation.instances());
        }

        // 이미 중지된 인스턴스를 추적하기 위한 Set 생성
        Set<String> stoppedInstances = new HashSet<>();

        // 인스턴스 순회
        for (Instance instance : runningInstances) {
            String instanceId = instance.instanceId();
            String privateIP = instance.privateIpAddress();

            // Main 노드와 Assigned 노드를 제외하고 처리
            if (!assignedNodes.contains(privateIP)
                    && !instanceId.equals("i-0e2da5fd0cf96ff18")  // Main 노드 제외
                    && !stoppedInstances.contains(instanceId)) {
                stopInstance(instanceId);
                stoppedInstances.add(instanceId); // 중지된 인스턴스 기록
            }
        }
    }



}