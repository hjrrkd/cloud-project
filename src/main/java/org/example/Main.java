package org.example;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import java.util.concurrent.TimeUnit;
import java.util.*;


public class Main {

    private static Ec2Client ec2;
    private static SsmClient ssm;

    private static void init() {/*throws Exception*/
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();

        // 리전을 ap-northeast-2로 고정
        Region region = Region.AP_NORTHEAST_2;

        // EC2 클라이언트
        ec2 = Ec2Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        // SSM 클라이언트
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

    // 인스턴스 리스트 출력
    public static void listInstances() {
        System.out.println("Listing instances...");

        try (Ec2Client ec2 = Ec2Client.create()) {
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

    // 가용 영역(Zones) 출력
    public static void availableZones() {
        System.out.println("Available zones...");

        try (Ec2Client ec2 = Ec2Client.create()) {

            DescribeAvailabilityZonesRequest request = DescribeAvailabilityZonesRequest.builder().build();

            DescribeAvailabilityZonesResponse response = ec2.describeAvailabilityZones(request);
            for (AvailabilityZone zone : response.availabilityZones()) {
                System.out.printf("[Zone] %s, [Region] %s, [Zone Name] %s\n",
                        zone.zoneId(), zone.regionName(), zone.zoneName());
            }
        } catch (Exception e) {
            System.err.println("Failed to list availability zones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 인스턴스 시작
    public static void startInstance(String instanceId) {
        System.out.printf("Starting instance: %s\n", instanceId);

        try (Ec2Client ec2 = Ec2Client.create()) {

            StartInstancesRequest request = StartInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            StartInstancesResponse response = ec2.startInstances(request);

            System.out.printf("Successfully started instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to start instance: %s\n", e.getMessage());
        }
    }

    // 인스턴스 중지
    public static void stopInstance(String instanceId) {
        System.out.printf("Stopping instance: %s\n", instanceId);

        try (Ec2Client ec2 = Ec2Client.create()) {

            StopInstancesRequest request = StopInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            StopInstancesResponse response = ec2.stopInstances(request);

            System.out.printf("Successfully stopped instance: %s\n", instanceId);
            System.out.printf("Current state: %s\n",
                    response.stoppingInstances().get(0).currentState().name());
        } catch (Exception e) {
            System.out.printf("Failed to stop instance: %s\n", e.getMessage());
        }
    }

    // 인스턴스 생성
    public static void createInstance(String amiId) {
        System.out.printf("Creating instance in region: ap-southeast-2 with AMI: %s\n", amiId);

        try (Ec2Client ec2 = Ec2Client.create()) {

            RunInstancesRequest request = RunInstancesRequest.builder()
                    .imageId(amiId)
                    .instanceType(InstanceType.T2_MICRO)
                    .minCount(1)
                    .maxCount(1)
                    .build();

            RunInstancesResponse response = ec2.runInstances(request);

            String instanceId = response.instances().get(0).instanceId();
            System.out.printf("Successfully created instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to create instance: %s\n", e.getMessage());
        }
    }

    // 인스턴스 재부팅
    public static void rebootInstance(String instanceId) {
        System.out.printf("Rebooting instance: %s\n", instanceId);

        try (Ec2Client ec2 = Ec2Client.create()) {

            RebootInstancesRequest request = RebootInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2.rebootInstances(request);

            System.out.printf("Successfully rebooted instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to reboot instance: %s\n", e.getMessage());
        }
    }

    // 가용 리전 출력
    public static void availableRegions() {
        System.out.println("Available regions...");

        DescribeRegionsRequest request = (DescribeRegionsRequest) DescribeRegionsRequest.builder().build();

        try {
            DescribeRegionsResponse result = ec2.describeRegions(request);

            for (software.amazon.awssdk.services.ec2.model.Region region : result.regions()) {
                System.out.printf("[Region] %s, [Endpoint] %s\n",
                        region.regionName(), region.endpoint());
            }
        } catch (Exception e) {
            System.out.printf("Failed to fetch regions: %s\n", e.getMessage());
        }
    }

    // EC2 이미지 출력
    public static void listImages() {
        System.out.println("Listing images....");

        try (Ec2Client ec2 = Ec2Client.create()) {
            Filter filter = Filter.builder()
                    .name("name")
                    .values("aws-htcondor-worker")
                    .build();

            DescribeImagesRequest request = DescribeImagesRequest.builder()
                    .filters(filter)
                    .build();

            DescribeImagesResponse response = ec2.describeImages(request);

            for (Image image : response.images()) {
                System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
                        image.imageId(), image.name(), image.ownerId());
            }
        } catch (Exception e) {
            System.out.printf("Failed to list images: %s\n", e.getMessage());
        }
    }

    // condor_status 실행
    public static void executeCondorStatus() {
        String instanceId = "i-0e2da5fd0cf96ff18";
        String command = "condor_status";

        try (SsmClient ssmClient = SsmClient.create()) {

            // SSM 명령 요청 설정
            Map<String, List<String>> parameters = new HashMap<>();
            parameters.put("commands", Arrays.asList(command)); // 실행 명령 등록

            SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                    .instanceIds(instanceId)
                    .documentName("AWS-RunShellScript") // ShellScript 실행을 위한 SSM 문서
                    .parameters(parameters) // 명령 전달
                    .build();

            // SSM 명령 실행
            SendCommandResponse sendCommandResponse = ssmClient.sendCommand(sendCommandRequest);
            String commandId = sendCommandResponse.command().commandId();

            Thread.sleep(1000);

            // 실행 결과 확인
            GetCommandInvocationRequest invocationRequest = GetCommandInvocationRequest.builder()
                    .commandId(commandId)
                    .instanceId(instanceId)
                    .build();

            GetCommandInvocationResponse invocationResponse = ssmClient.getCommandInvocation(invocationRequest);

            System.out.println("Command Output:");
            System.out.println(invocationResponse.standardOutputContent());

        } catch (Exception e) {
            System.out.printf("Failed to execute condor_status on instance %s: %s\n", instanceId, e.getMessage());
            e.printStackTrace();
        }
    }

    // condor_q 실행
    public static void executeCondorQ() {
        String instanceId = "i-0e2da5fd0cf96ff18";
        String command = "condor_q";

        try (SsmClient ssmClient = SsmClient.create()) {

            // SSM 명령 요청 설정
            Map<String, List<String>> parameters = new HashMap<>();
            parameters.put("commands", Arrays.asList(command)); // 실행 명령 등록

            SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                    .instanceIds(instanceId)
                    .documentName("AWS-RunShellScript") // ShellScript 실행을 위한 SSM 문서
                    .parameters(parameters) // 명령 전달
                    .build();

            // 명령 실행
            SendCommandResponse sendCommandResponse = ssmClient.sendCommand(sendCommandRequest);
            String commandId = sendCommandResponse.command().commandId();

            Thread.sleep(1000);

            // 실행 결과 확인
            GetCommandInvocationRequest invocationRequest = GetCommandInvocationRequest.builder()
                    .commandId(commandId)
                    .instanceId(instanceId)
                    .build();

            GetCommandInvocationResponse invocationResponse = ssmClient.getCommandInvocation(invocationRequest);

            String output = invocationResponse.standardOutputContent();
            System.out.println("Command Output:");
            System.out.println(output);

            if (output.contains("Total for query: 0 jobs")) {
                System.out.println("No jobs are currently in the queue.");
            }

        } catch (Exception e) {
            System.out.printf("Failed to execute condor_q on instance %s: %s\n", instanceId, e.getMessage());
            e.printStackTrace();
        }
    }

    // 오토스케일링 기능
    public static void autoScaling() {
        System.out.println("Starting auto-scaling...");

        String scriptResult = executeSSMScript("/home/ec2-user/autoscaling.sh");
        if (scriptResult == null || scriptResult.isEmpty()) {
            System.err.println("Error: Script result is empty. Check SSM command execution.");
            return;
        }

        String[] results = scriptResult.split("\n");

        try {
            int slots = Integer.parseInt(results[0].trim()); // 현재 사용 가능한 슬롯 수
            int jobs = Integer.parseInt(results[1].trim()); // 대기 중인 작업 수
            List<String> assignedNodes = Arrays.asList(results[2].trim().split(",")); // 할당된 노드 목록

            // 스케일 아웃 조건
            if (jobs > slots) {
                System.out.println("Scale out: Creating a new instance...");
                createInstance("ami-064d68c52313d6d29");
            }
            // 스케일 인 조건
            else if (jobs < slots) {
                System.out.println("Scale in: Stopping unnecessary instances...");
                scaleInInstances(assignedNodes);
            }
            // 안정 상태
            else {
                System.out.println("System stable: No scaling required.");
            }
        } catch (Exception e) {
            System.err.println("Error parsing script result: " + e.getMessage());
        }

    }

    // SSM을 통해 원격 스크립트 실행
    private static String executeSSMScript(String scriptPath) {
        String instanceId = "i-0e2da5fd0cf96ff18";

        try {
            // SSM 명령 요청 생성
            SendCommandRequest sendCommandRequest = SendCommandRequest.builder()
                    .instanceIds(instanceId)
                    .documentName("AWS-RunShellScript")
                    .parameters(Collections.singletonMap("commands", Collections.singletonList("bash " + scriptPath)))
                    .build();

            SendCommandResponse result = ssm.sendCommand(sendCommandRequest);
            String commandId = result.command().commandId(); // 명령 ID 가져오기
            System.out.println("Command ID: " + commandId);

            TimeUnit.SECONDS.sleep(2);

            return getSSMCommandResult(commandId, instanceId);

        } catch (Exception e) {
            System.err.println("Failed to execute script: " + e.getMessage());
            return "";
        }
    }

    //  SSM 명령 실행 결과를 가져옴
    private static String getSSMCommandResult(String commandId, String instanceId) {
        try {

            long startTime = System.currentTimeMillis();
            while (true) {

                GetCommandInvocationRequest request = GetCommandInvocationRequest.builder()
                        .commandId(commandId)
                        .instanceId(instanceId)
                        .build();

                GetCommandInvocationResponse response = ssm.getCommandInvocation(request);

                if (response.status() == CommandInvocationStatus.SUCCESS) {
                    return response.standardOutputContent();
                } else if (response.status() == CommandInvocationStatus.FAILED) {
                    System.err.println("Command failed: " + response.standardErrorContent());
                    return "";
                }

                if (System.currentTimeMillis() - startTime > TimeUnit.MINUTES.toMillis(5)) {
                    System.err.println("Command timeout after 5 minutes.");
                    return "";
                }

                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            System.err.println("Error while fetching command result: " + e.getMessage());
            return "";
        }
    }

    // 사용되지 않는 인스턴스 식별 및 종료
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


            if (!assignedNodes.contains(privateIP)
                    && !instanceId.equals("i-0e2da5fd0cf96ff18")  // Main 노드 제외
                    && !stoppedInstances.contains(instanceId)) {
                stopInstance(instanceId);
                stoppedInstances.add(instanceId); // 중지된 인스턴스 기록
            }
        }
    }
}